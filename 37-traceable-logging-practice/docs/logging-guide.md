# 日志工程化参考指南（第37章配套）

> 八股 + 场景 + 命令手册，所有结论有本章代码实证。

## 一、MDC 串联的原理与边界

**原理**：`org.slf4j.MDC` 底层是 `ThreadLocal<Map<String,String>>`，每个线程持有一份独立上下文。`MDC.put("traceId", x)` 只影响当前线程；日志 pattern `%X{traceId:-}` 在**打印时刻**从当前线程的 MDC 取值，取不到输出 `:-` 后的默认值。

**两个边界（踩坑高发区）**：

1. **线程切换即上下文丢失**：线程池、@Async、CompletableFuture、new Thread——新线程的 ThreadLocal 是空副本，必须透传。本章三方案：
   - Spring 线程池：`setTaskDecorator(new MdcTaskDecorator())`（`trace/MdcTaskDecorator.java`）；
   - 任意线程/装饰不了的场景：阿里 TTL（05 章已详讲）；
   - 跨进程（HTTP/RPC/MQ）：把头带过去，对端取出来 set 回 MDC（HTTP 的 `X-Trace-Id`、MQ 消息头）。
2. **线程复用即上下文污染**：处理请求的线程是池化的，不清理上一个请求的 traceId 会串给下一个请求。所有 `set` 必须有配对的 finally `clear`/`remove`——`TraceIdFilter` 和 `MdcTaskDecorator` 里各有一处，缺一不可。

**验证方法**：把 `finally { MDC.clear(); }` 注释掉再跑并发请求（`for i in {1..5}; do curl -s localhost:8080/api/demo/sync & done`），会看到第二个请求复用了第一个请求的 traceId。

## 二、logback 切割策略选型

| 策略 | 类 | 适用 |
| --- | --- | --- |
| 按时间切 | `TimeBasedRollingPolicy` | 常规选择（本章） |
| 按大小切 | `SizeBasedRollingPolicy` | 突发大流量场景 |
| 时间+大小 | `SizeAndTimeBasedRollingPolicy` | 两者都要约束 |

本章 `fileNamePattern = logs/%d{yyyy-MM-dd}/app-%d{yyyy-MM-dd-HH}.log` 的两个细节：

- **按小时切、按天归目录**：单文件可控（小时级），目录可按天打包归档/删除；`maxHistory=7` 表示保留 7 天的天目录（maxHistory 的时间单位取 pattern 里最细粒度之外的第一级——这里按天算）。
- **`totalSizeCap=1GB`**：即使 maxHistory 内的总量超限也强制删旧，防磁盘打满。

**选型权衡**：切得越细检索越快但文件越多；生产常见口径是小时切 + 天目录 + 7 天保留，ELK 场景本地文件只是兜底，保留 1~3 天即可。

## 三、按级别分文件与异步日志

- **LevelFilter**：`ACCEPT/DENY` 精确匹配单级别；要「ERROR 及以上」需 `ThresholdFilter`（`onMatch=ACCEPT` 配 `onMismatch=DENY`，接受 ERROR+ 因为其阈值语义是 ≥）。本章用 LevelFilter 正好只收 ERROR 一档。
- **AsyncAppender**：把日志写盘挪到独立线程，业务线程只入队；配 `queueSize` + `discardingThreshold` + `neverBlock`。注意 AsyncAppender 丢日志的边界：进程崩溃时队列未落盘部分丢失，审计场景慎用于「唯一证据」。

## 四、脱敏：pattern 级 %replace 的边界

```xml
%replace(%msg){'\d{11}','***********'}
```

- 生效位置是**打印时刻的正则替换**，所有 logger 打出的 11 位连续数字都会被替换——包括不是手机号的数字（误伤）和拼接在长串里的片段（漏伤）。
- 本章刻意「控制台不脱敏、文件脱敏」：开发期看原文，落盘才脱敏。
- **生产建议**：结构化日志（JSON）+ 字段级脱敏序列化器（如自定义 Jackson Serializer，参照 30 章 @Sensitive 思路），只对标注字段脱敏，而不是对全文跑正则。

## 五、动态日志级别（Actuator）

```bash
# 查看当前级别
curl localhost:8080/actuator/loggers/com.example.tlp
# 临时开 DEBUG（即时生效，无需重启）
curl -X POST localhost:8080/actuator/loggers/com.example.tlp \
  -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'
# 恢复
curl -X POST localhost:8080/actuator/loggers/com.example.tlp \
  -H 'Content-Type: application/json' -d '{"configuredLevel":null}'
```

风险：DEBUG 放大磁盘 IO 与敏感信息输出面。生产要点：暴露端点加访问控制、改动留审计、临时开启设闹钟还原。

## 六、awk/grep 检索手册（固定列序的价值）

本章 console pattern 关键列恒定：`时间 | 级别 | [线程] | [traceId] | logger... | - | 消息`。`%-5level` 定宽 5 字符、`[traceId]` 定宽 34 字符（方括号+32 位 id）——这两列按空格分列位置不漂移；logger 名用 `%logger{36}` 截断但不定宽，取消息应使用 `- ` 分隔符而不是数空格列。

```bash
# 1. 按 traceId 全量检索（跨天目录）
grep -r "a1b2c3d4" logs/

# 2. 只翻今天某个小时的文件
less logs/$(date +%F)/app-$(date +%F-%H).log

# 3. awk 按列过滤 traceId（第 4 列 [traceId] 定宽 34 字符），
#    消息用 "- " 做分隔符取（logger 名长短不一，按空格分列会漂移）
awk -F' - ' '$4 ~ /\[a1b2c3d4\]/ {print $2}' logs/*/app-*.log

# 4. 按线程过滤（异步链路对照）
awk '$3 == "[batch-2]"' logs/*/app-*.log

# 5. 统计某 traceId 的各级别条数
grep "a1b2c3d4" logs/*/app-*.log | awk '{print $2}' | sort | uniq -c

# 6. 只看错误（error 文件本就只有 ERROR）
cat logs/*/error-*.log

# 7. 验证脱敏：文件里不应出现完整 11 位手机号
grep -E "\b1[0-9]{10}\b" logs/*/app-*.log && echo "脱敏失效" || echo "脱敏生效"
```

## 七、并发进度计数：为什么必须 AtomicInteger

`TaskProgress.done++` 语义是「读-改-写」三步，多线程并发时两个线程读到相同值各自加一，其中一个更新丢失。几十上百个子任务时丢几次更新，进度就永远停在 99%。

本章用 `AtomicInteger.incrementAndGet()`（CAS）保证原子性；更高并发场景（单机百万级计数）换 `LongAdder`（分段思想，几乎无竞争）。单测 `BatchTaskServiceTest.allSuccess/allFailed` 用真实线程池验证计数精确——这类逻辑靠肉眼并发测试必漏，必须自动化。

## 八、场景对照速查

| 场景 | 透传机制 | 代码位置 |
| --- | --- | --- |
| HTTP 入口 | Filter set，finally remove | `trace/TraceIdFilter.java` |
| 同步调用 | 同线程，天然共享 | — |
| @Async / 线程池 | TaskDecorator 快照恢复 | `trace/MdcTaskDecorator.java` + `config/ThreadPoolConfig.java` |
| CompletableFuture | 走带 Decorator 的 executor | `controller/LogDemoController.future` |
| 定时任务 | 无上游，runWithNewTrace 开新链 | `schedule/DemoScheduler.java` |
| 模拟 MQ | 事件 + @Async（真实 MQ 用消息头） | `event/DemoEventListener.java` |
| 异常链路 | 同线程 MDC，全局处理器的 ERROR 天然同 id | `common/GlobalExceptionHandler.java` |
