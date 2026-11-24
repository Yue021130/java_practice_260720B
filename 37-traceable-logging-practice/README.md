# 第37章：日志优雅可追溯实战（traceId 全链路 + 分时分天切割 + 线程池进度可视化）

## 一、业务背景

线上排查靠日志，日志排查靠链路 ID。本章把一个单体 Spring Boot Demo 做成**日志工程化样板间**：一条 traceId 从 HTTP 入口穿过同步调用、@Async、CompletableFuture、定时任务、模拟 MQ、异常处理全程不丢；logback 按小时切文件、按天归目录、按级别分文件、落盘自动脱敏；前端动态观察线程池批次任务进度。

第05章讲过 MDC/ThreadLocal 的概念，本章是落地工程：切割策略选型、脱敏边界、动态日志级别、awk 检索手册、并发进度计数的正确姿势。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18 + Java 8 + Actuator（动态日志级别） |
| 日志 | SLF4J + Logback（TimeBasedRollingPolicy 分时分天切割、LevelFilter 级别分文件、%replace 脱敏） |
| 链路 | MDC + OncePerRequestFilter + TaskDecorator + @Scheduled + ApplicationEvent |
| 前端 | Vue3 + Vite 5 + Element Plus + Axios（轮询进度） |
| 数据库 | 无（日志主题不需要） |

## 三、项目结构

```
37-traceable-logging-practice/
├── pom.xml
├── docs/logging-guide.md                # 八股参考（切割策略/脱敏边界/awk 手册）
├── src/main/java/com/example/tlp/
│   ├── TlpApplication.java              # @EnableAsync @EnableScheduling
│   ├── trace/                           # 【核心】链路上下文
│   │   ├── TraceContext.java            # MDC 封装：generate/set/clear/runWithNewTrace
│   │   ├── TraceIdFilter.java           # 入口过滤器：X-Trace-Id 透传/生成/回写/清理
│   │   └── MdcTaskDecorator.java        # 线程池 MDC 透传（ finally clear 防污染）
│   ├── config/ThreadPoolConfig.java     # batchExecutor：有界队列+CallerRuns+TaskDecorator+优雅关闭
│   ├── task/BatchTaskService.java       # 批次任务：子任务进线程池，AtomicInteger 进度计数
│   ├── task/TaskProgress.java           # 进度快照（前端轮询）
│   ├── event/                           # 模拟 MQ：发布 + @Async 消费
│   ├── schedule/DemoScheduler.java      # 定时任务：runWithNewTrace 每次新链路
│   ├── controller/LogDemoController.java# 6 个场景接口 + 批次提交/进度
│   └── common/GlobalExceptionHandler.java
├── src/main/resources/logback-spring.xml# 【核心】分时分天切割 + error 分文件 + 脱敏
└── web/                                 # 前端（链路演示/批次进度/日志检索）
```

## 四、接口清单

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| GET | /api/demo/sync | 同步链路 |
| GET | /api/demo/async | @Async 异步（独立 Service Bean，防自调用失效） |
| GET | /api/demo/future | CompletableFuture + 自定义线程池 |
| GET | /api/demo/event | 模拟 MQ 发布 → @Async 消费 |
| GET | /api/demo/exception | 异常链路（ERROR 进 error-current.log，同 traceId） |
| GET | /api/demo/headers | 返回当前 traceId（验证 X-Trace-Id 透传） |
| POST | /api/batch/submit | 提交批次 {taskCount, perTaskMs, failRatePermille} |
| GET | /api/batch/{taskId}/progress | 轮询进度 {total, done, failed, percent, finished} |
| POST | /actuator/loggers/{logger} | 动态调整日志级别 `{"configuredLevel":"DEBUG"}` |

## 五、运行方式

```bash
mvn spring-boot:run                  # 后端
cd web && npm install && npm run dev # 前端
```

无需数据库。启动后观察 `logs/` 目录：`logs/current.log` 是当前写入文件，整点滚动为 `logs/2026-10-08/app-2026-10-08-14.log`，ERROR 单独进 `logs/error-current.log`。

## 六、核心场景验证

| 场景 | 操作 | 验证点 |
| --- | --- | --- |
| 同步链路 | 前端点「同步链路」 | 3 行日志 traceId 相同 |
| 异步线程 | 「@Async 异步」 | `batch-x` 线程日志与 http 线程同 traceId |
| CompletableFuture | 「Future」 | 同上（裸 ForkJoinPool 会断链，本章走 batchExecutor） |
| 模拟 MQ | 「MQ 事件」 | 消费日志（异步）带发布时的 traceId |
| 异常链路 | 「异常链路」 | 响应拿 traceId，grep 到 ERROR 行且同 id，文件在 error-current.log |
| 透传 | 输入自定义 id 点「Headers 透传」 | 响应 traceId == 输入值 |
| 定时任务 | 等 30 秒 | 每条新 traceId |
| 并发区分 | 快速连点 | 不同请求 traceId 不同、日志交错可区分 |
| 批次进度 | 批次页提交 30 个任务 | 进度条 0→100%，日志里一个 traceId 串起全部子任务 |
| 动态级别 | `curl -X POST localhost:8080/actuator/loggers/com.example.tlp -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'` | DEBUG 日志即时出现，无需重启 |
| 脱敏 | 日志里出现 11 位数字 | 文件里是 `***********`，控制台是原文 |

## 七、八股速记

1. **MDC 为什么能串联**：底层 ThreadLocal&lt;Map&gt;，每线程一份副本；pattern `%X{traceId:-}` 取值，空时输出默认串。
2. **线程池污染**：Tomcat/业务线程池线程复用，不 finally clear 就串号；线程池任务不装饰就断链。两个 finally 缺一不可。
3. **透传三方案**：Spring TaskDecorator（线程池）、TTL（任意线程，05 章）、MQ 消息头（跨进程）。
4. **切割策略**：TimeBased（按时间）、SizeBased（按大小）、SizeAndTimeBased（两者）；`maxHistory` 按 fileNamePattern 最细时间单位计，配天目录就是保留 N 天；`totalSizeCap` 防磁盘打满。
5. **按级别分文件**：LevelFilter ACCEPT/DENY；配合 AsyncAppender 可减少日志打印对业务的阻塞。
6. **脱敏边界**：pattern 级 `%replace` 适合演示，生产建议结构化日志 + 序列化器脱敏（只脱敏指定字段而不是全文正则）。
7. **动态日志级别**：Actuator loggers 端点即改即生效；风险是 DEBUG 放大磁盘 IO，生产配合开关审计。
8. **并发进度计数**：AtomicInteger / LongAdder，普通 int 的 read-modify-write 丢更新会让进度永远到不了 100%。

详见 **docs/logging-guide.md**。

## 八、测试验证

```bash
mvn clean test   # 10 例：TraceContext 2 + MdcTaskDecorator 3 + TraceIdFilter 2 + BatchTaskService 3（真实线程池）
cd web && npm install && npm run build
```

## 九、作者

Yue021130 <169446203@qq.com>
