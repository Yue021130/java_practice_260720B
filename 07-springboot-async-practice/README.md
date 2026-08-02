# 07 · Spring Boot 异步任务与线程池

系统覆盖 Spring Boot `@Async` 与线程池整合：线程池配置、拒绝策略、异常处理、Future / CompletableFuture、同类调用代理坑、ThreadLocal 上下文透传、批量异步聚合、异步 Controller、线程池监控与优雅关闭。

## 项目目标

把 Spring Boot 异步编程的常用面试点包装成 14 个可运行场景，每个场景都能通过前端面板一键调用，后端返回运行结果 + `interviewNote` 面试八股，边跑边学。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok 1.18.30 + springdoc-openapi 1.7.0
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS）
- 测试：JUnit 5 + MockMvc + AssertJ
- 端口：后端 **8087**，前端开发端口 **5180**
- 基础包：`com.example.async`

## 14 个场景

| 模块 | 场景 | 端点 | 面试考点 |
| --- | --- | --- | --- |
| 快速上手 | 无返回值 fire-and-forget | `POST /api/async/fire-forget` | @Async 默认线程池 SimpleAsyncTaskExecutor 的问题 |
| 快速上手 | CompletableFuture 返回值 | `POST /api/async/completable-future` | supplyAsync / thenApply / join；异步编排 |
| 快速上手 | Future + 超时获取 | `POST /api/async/future-timeout` | Future.get(timeout, TimeUnit) 避免永久阻塞 |
| 线程池配置 | ThreadPoolTaskExecutor 参数 | `POST /api/async/pool-config` | core/max/queue/keepAlive/rejection/shutdown 含义 |
| 线程池配置 | 多线程池与 @Async("name") | `POST /api/async/custom-executor` | 按业务隔离线程池，避免相互挤占 |
| 线程池配置 | 队列打满与拒绝策略 | `POST /api/async/rejected` | AbortPolicy / CallerRunsPolicy / DiscardPolicy / 自定义计数 |
| 异常与代理 | 异步异常处理 | `POST /api/async/exception` | AsyncUncaughtExceptionHandler、Future.exceptionally |
| 异常与代理 | 同类内部调用不生效 | `POST /api/async/self-invocation` | Spring AOP 代理机制，this 调用绕过代理 |
| 上下文透传 | ThreadLocal / MDC 透传 | `POST /api/async/context-propagation` | TaskDecorator 包装 Runnable，复制上下文到异步线程 |
| 生产场景 | 批量异步 + 结果聚合 | `POST /api/async/batch-aggregate` | CompletableFuture.allOf / join / 汇总结果 |
| 生产场景 | 异步 Controller | `POST /api/async/controller-async` | Callable / CompletableFuture 释放 Tomcat 线程 |
| 生产场景 | 线程池实时指标 | `POST /api/async/metrics` | activeCount / queueSize / completedTaskCount / rejectedCount |
| 生产场景 | 优雅关闭配置 | `POST /api/async/graceful-shutdown` | setWaitForTasksToCompleteOnShutdown + awaitTerminationSeconds |
| 生产场景 | 异步 vs 同步对比 | `POST /api/async/sync-vs-async` | 同接口串行 vs 并行执行耗时对比 |

## 面试八股速记

### @Async 原理

- `@EnableAsync` 开启异步代理，`@Async` 方法会被 Spring AOP 包装成 `AsyncTaskExecutor.submit(...)`。
- 默认线程池是 `SimpleAsyncTaskExecutor`（每任务新建线程），生产环境必须自定义 `ThreadPoolTaskExecutor`。
- 同类内部 `this.method()` 调用不走代理，@Async 不生效。

### 线程池参数

| 参数 | 含义 |
| --- | --- |
| corePoolSize | 常驻线程数 |
| maxPoolSize | 队列满后最大扩容线程数 |
| queueCapacity | 任务队列长度 |
| keepAliveTime | 非核心线程空闲回收时间 |
| rejectionPolicy | 队列和线程都满时的拒绝策略 |

### 拒绝策略

- `AbortPolicy`：抛异常（默认）。
- `CallerRunsPolicy`：调用线程执行，起到削峰作用。
- `DiscardPolicy`：静默丢弃。
- `DiscardOldestPolicy`：丢弃最老任务。
- 建议自定义计数器监控 `rejectedCount`。

### 上下文透传

- `ThreadLocal` 不会自动跨线程。
- 使用 Spring `TaskDecorator` 包装 `Runnable`，在 `run()` 前后 `set/remove`。
- 任务结束后必须 `remove()`，避免线程复用导致串号。

### 优雅关闭

- `setWaitForTasksToCompleteOnShutdown(true)`：关闭前先消费队列任务。
- `setAwaitTerminationSeconds(30)`：最多等待 30 秒。
- 两者缺一不可，否则关闭时直接丢弃任务。

## 启动命令

```bash
# 后端
mvn spring-boot:run

# 前端（另开终端）
cd web
npm install
npm run dev
```

浏览器访问 `http://localhost:5180`。

Swagger UI：`http://localhost:8087/swagger-ui.html`

## 线程池使用 Checklist

- [ ] 是否自定义 `ThreadPoolTaskExecutor`，而非依赖 SimpleAsyncTaskExecutor
- [ ] core/max/queue 是否与业务类型匹配（CPU 密集 vs IO 密集）
- [ ] 是否配置拒绝策略并监控 rejectedCount
- [ ] void @Async 是否通过 `AsyncUncaughtExceptionHandler` 捕获异常
- [ ] 有返回值任务是否带超时 `get(timeout, TimeUnit)`
- [ ] ThreadLocal / MDC 是否通过 `TaskDecorator` 透传并在结束后清理
- [ ] 是否开启 `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds(N)`
- [ ] 是否避免同类内部 this 调用 @Async 方法

## 测试

```bash
mvn test -q
```

`ScenarioApiTest` 覆盖全部 14 个端点，并对拒绝策略、同类调用、自定义线程池、上下文透传、批量聚合做了额外断言。
