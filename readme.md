# Java 场景模拟测试

始于 2026 年 7 月 20 日。

本仓库是「java高级知识」系列的可运行场景练习集，每个专题都用 **Spring Boot 后端 + 前端面板** 把知识点包装成可交互的实验场景，
配合中文注释与 README 中的「面试八股」速记，边跑边学。

## 已完成的专题

| 目录 | 主题 | 前端 | 后端端口 | 前端端口 |
| --- | --- | --- | --- | --- |
| [01-thread-pool-practice](01-thread-pool-practice/) | Java 线程池实践：七大参数、四种拒绝策略、动态调参 | React 18 + Vite | 8081 | 5174 |
| [02-juc-practice](02-juc-practice/) | Java 并发包（JUC）全场景：锁 / 原子类 / 并发容器 / 同步工具 / CompletableFuture / ThreadLocal / JMM | Vue 3 + Vite | 8082 | 5175 |
| [03-collections-generics-practice](03-collections-generics-practice/) | Java 集合与泛型全场景：List / Set / Map / Queue / 工具类 / 泛型 / 综合实战 | Vue 3 + Vite | 8083 | 5176 |
| [04-mybatis-plus-practice](04-mybatis-plus-practice/) | Spring Boot + MyBatis-Plus 全场景：实体注解 / BaseMapper / IService / 条件构造器 / 分页 / 逻辑删除 / 乐观锁 / 批量操作 | Vue 3 + Vite | 8084 | 5177 |
| [05-threadlocal-practice](05-threadlocal-practice/) | ThreadLocal 全场景：线程隔离 / Web 上下文 / MDC / 线程池串号 / TTL 透传 / 内存泄漏 | Vue 3 + Vite | 8085 | 5178 |
| [06-stream-lambda-practice](06-stream-lambda-practice/) | Stream / Lambda / Optional 全场景：函数式接口 / Collectors / 基本类型流 / 并行流正确使用与踩坑 | Vue 3 + Vite | 8086 | 5179 |
| [07-springboot-async-practice](07-springboot-async-practice/) | Spring Boot 异步任务与线程池：@Async / ThreadPoolTaskExecutor / 拒绝策略 / 上下文透传 / 批量聚合 / 优雅关闭 | Vue 3 + Vite | 8087 | 5180 |
| [08-springboot-core-practice](08-springboot-core-practice/) | Spring Boot 核心能力实战：条件装配 / 生命周期 / 属性绑定 / 事件监听 / 启动流程 / 常用注解 | Vue 3 + Vite | 8088 | 5181 |
| [09-exception-practice](09-exception-practice/) | Java 异常体系全场景：分类 / try-catch-finally / try-with-resources / 常见异常 / Spring 全局异常 / 并发异常 / 最佳实践 | Vue 3 + Vite | 8089 | 5182 |
| [10-sa-token-practice](10-sa-token-practice/) | Sa-Token 全功能实践：登录认证 / 权限鉴权 / Session / 踢人封禁 / SSO / OAuth2.0 / Redis / JWT / API 签名 / 网关 / RPC / Quick 登录 | Vue 3 + Vite | 8090 | 5183 |
| [11-thread-pool-advanced-practice](11-thread-pool-advanced-practice/) | Java 线程池深度实践：源码流程 / 七大参数 / 7 种阻塞队列 / 4 种拒绝策略 / Executors 工厂风险 / 生命周期 | Vue 3 + Vite | 8091 | 5184 |
| [12-springboot-mail-practice](12-springboot-mail-practice/) | Spring Boot 邮件服务实践：基础文本 / 富文本 HTML / 附件 / 内联图片 / Thymeleaf 模板 / 异步发送 / 失败重试 / Quartz 定时任务 / @EventListener 事件监听 / 编码与邮件头 / 常见坑 | Vue 3 + Vite | 8092 | 5185 |
| [13-unsafe-practice](13-unsafe-practice/) | 魔法类 Unsafe 实践：获取实例 / 堆外内存 / 绕过构造器 / CAS 原子操作 / 字段偏移与对象布局 / park-unpark / 内存屏障 / 危险与本质 | Vue 3 + Vite | 8093 | 5186 |
| [14-springboot-excel-practice](14-springboot-excel-practice/) | Spring Boot + EasyExcel 导入导出实践：注解映射 / 样式 / 复杂表头 / 大数据量导出 / 数据校验与错误回写 / 监听器增量读取 / 模板填充 / Web 下载与上传 / 常见坑与调优 | Vue 3 + Vite | 8094 | 5187 |
| [15-caffeine-practice](15-caffeine-practice/) | Spring Boot + Caffeine 缓存实践：快速开始 / 淘汰策略 / 刷新与异步 / 统计监控 / **缓存预热** / 穿透击穿雪崩与单飞 / 两级缓存 / Spring Cache 注解 / 缓存一致性 / 常见坑与调优 | Vue 3 + Vite | 8095 | 5188 |
| [16-thread-communication-practice](16-thread-communication-practice/) | Java 线程间通信方式实践：共享内存(volatile/原子类) / 等待通知(wait-notify/Condition) / 线程协作(join/interrupt/LockSupport) / JUC 同步工具 / 阻塞队列 / 异步结果传递 / 管道通道 / 选型总结 | Vue 3 + Vite | 8096 | 5189 |
| [17-api-signature-practice](17-api-signature-practice/) | 基于 appid + appkey 的 HMAC-SHA256 接口签名鉴权实践：核心原理 / 签名计算 / 服务端验签 / 防重放(时间戳+nonce) / 请求体完整性 / 规范化 / 简化版 / 拦截器实战 / 选型对比 | Vue 3 + Vite | 8097 | 5190 |
| [18-optional-stream-practice](18-optional-stream-practice/) | Java Optional + Stream 真实业务场景实践：用户画像聚合 / 订单报表统计 / 菜单权限树 / 批量数据清洗 / SKU 最优价格 / 消息通知过滤 / Excel 导入校验 / 分页再加工 / 反模式对比 | Vue 3 + Vite | 8098 | 5191 |
| [19-unified-response-practice](19-unified-response-practice/) | Spring Boot 统一返回结果封装实战：Result / ResultCode / ResultFactory / PageResult / ResponseBodyAdvice 全局自动包装 / 全局异常处理 / 前端 axios 拦截器 / Swagger 显示统一包装结构 / String 返回值两种稳妥方案 | Vue 3 + Vite | 8099 | 5192 |
| [20-custom-annotation-aop-practice](20-custom-annotation-aop-practice/) | Spring Boot 自定义注解 + AOP 高阶玩法实战：内置注解 / 元注解 / 自定义注解 + AOP / 操作日志 / 权限校验 / 接口限流 / 数据脱敏 / 耗时监控 / @Repeatable / Bean Validation / 注解组合 | Vue 3 + Vite | 8100 | 5193 |
| [21-nio2-file-practice](21-nio2-file-practice/) | Java NIO.2 文件操作实战：Path / Files / WatchService / 文件遍历 / 属性读写 / 拷贝移动删除 / 异步文件通道 | Vue 3 + Vite | 8101 | 5194 |
| [22-easyexcel-stream-practice](22-easyexcel-stream-practice/) | EasyExcel 流式导出实战：大数据量分页 / 临时文件 / 任务状态 / 内存优化 / Web 下载 / 异步生成 | Vue 3 + Vite | 8102 | 5195 |
| [23-easy-excel-practice](23-easy-excel-practice/) | Easypoi + EasyExcel 导入导出实战：注解映射 / 自定义 Converter / 组内重复校验 / 错误回写 / Web 上传下载 | Vue 3 + Vite | 8103 | 5196 |

## 通用启动方式

进入对应子目录后：

```bash
# 后端
mvn spring-boot:run

# 前端（另开终端）
cd web
npm install
npm run dev
```

然后按上表端口打开浏览器即可。

## 统一技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0
- 前端：Vite 5 + axios + 纯手写 CSS（无 UI 库）
- 测试：JUnit 5 + MockMvc + AssertJ

每个子目录都有独立的 `README.md`，详细说明该专题的场景清单、面试要点、接口文档与实验顺序。
