# 11-thread-pool-advanced-practice：Java 线程池深度实践

本模块基于 CSDN 文章《[Java 多线程：彻底搞懂线程池](https://blog.csdn.net/u013541140/article/details/95225769)》进行扩展，把文章中提到的**线程池优势、七大参数、任务队列、线程工厂、拒绝策略、Executors 四种功能线程池、源码流程**全部转化为可运行、可观测的 Spring Boot + Vue 3 代码。

与 `01-thread-pool-practice` 的关系：
- **01 偏实战面板**：动态调参、拒绝策略触发、实时指标监控。
- **11 偏源码深度**：ThreadPoolExecutor 执行流程、7 种阻塞队列对比、Executors 工厂源码风险、生命周期状态、Worker 内部类。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS
- 测试：JUnit 5 + MockMvc + AssertJ
- 端口：后端 **8091**，前端 **5184**

## 模块结构

```
11-thread-pool-advanced-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/threadpooladvanced/
│   ├── ThreadPoolAdvancedPracticeApplication.java
│   ├── common/         # 统一响应、全局异常处理
│   ├── config/         # OpenAPI、CORS、预定义线程池
│   ├── controller/     # REST API
│   ├── dto/            # 请求/响应 DTO
│   ├── service/        # 实验业务逻辑
│   └── source/         # 源码分析辅助（预留）
├── src/main/resources/application.yml
├── src/test/java/com/example/threadpooladvanced/
└── web/                # Vue 3 前端面板
```

## 快速启动

### 后端

```bash
cd 11-thread-pool-advanced-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8091/swagger-ui/index.html

### 前端

```bash
cd 11-thread-pool-advanced-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5184

### 运行测试

```bash
cd 11-thread-pool-advanced-practice
mvn test
```

## 接口速查

### 01. 线程池基础 `/api/pool`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pool/metrics` | GET | 所有线程池实时指标 |
| `/api/pool/{poolId}/metrics` | GET | 指定线程池指标 |
| `/api/pool/predefined/{poolId}/submit` | POST | 向预定义池提交任务 |
| `/api/pool/custom/create` | POST | 创建自定义线程池 |
| `/api/pool/custom/{poolId}/submit` | POST | 向自定义池提交任务 |
| `/api/pool/{poolId}/shutdown` | POST | 优雅关闭 |
| `/api/pool/{poolId}/shutdownNow` | POST | 立即关闭 |

预定义池：`cpuPool` / `ioPool` / `tinyPool`。

### 02. 阻塞队列 `/api/queue`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/queue/types` | GET | 7 种阻塞队列特性对比 |
| `/api/queue/{type}/experiment` | POST | 指定队列类型实验 |

### 03. 拒绝策略 `/api/rejection`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/rejection/types` | GET | 4 种 JDK 拒绝策略 + 自定义策略说明 |
| `/api/rejection/{policy}/experiment` | POST | 触发饱和观察策略行为 |

### 04. Executors 工厂 `/api/executors`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/executors/types` | GET | 4 种工厂源码、特点、风险 |
| `/api/executors/{type}/demo` | POST | 演示对应工厂池的行为 |

### 05. 源码分析 `/api/source`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/source/execute-flow` | GET | execute() 流程关键步骤 |
| `/api/source/lifecycle-states` | GET | RUNNING/SHUTDOWN/STOP/TIDYING/TERMINATED |
| `/api/source/worker-intro` | GET | Worker 内部类职责说明 |

## 与 CSDN 文章的映射

| 文章章节 | 本项目对应内容 |
| --- | --- |
| 1. 线程池的优势 | README / 面试八股 / 源码流程说明 |
| 2. 线程池的使用 | `/api/pool/custom/create` 动态创建 ThreadPoolExecutor |
| 3. 线程池的工作原理 | `/api/source/execute-flow` 七步流程 |
| 4. 线程池的参数 | `/api/pool` 实时指标 + `/api/queue` 队列说明 |
| 4.1 任务队列 | `/api/queue/types` 与 `/api/queue/{type}/experiment` |
| 4.2 线程工厂 | `ThreadPoolAdvancedConfig.namedThreadFactory` 自定义工厂 |
| 4.3 拒绝策略 | `/api/rejection/types` 与 `/api/rejection/{policy}/experiment` |
| 5. 功能线程池 | `/api/executors/types` 与 `/api/executors/{type}/demo` |
| 6. 总结（阿里规约） | README 面试八股：为什么不建议用 Executors |

## 面试八股

### 为什么要用线程池？

1. **降低资源消耗**：复用线程，减少创建/销毁开销。
2. **提高响应速度**：任务到达即可执行，无需等待创建线程。
3. **提高可管理性**：统一分配、调优、监控。

### ThreadPoolExecutor 七大参数

| 参数 | 作用 |
| --- | --- |
| corePoolSize | 核心线程数，默认长期存活 |
| maximumPoolSize | 最大线程数 |
| keepAliveTime | 非核心线程空闲回收时间 |
| unit | keepAliveTime 的时间单位 |
| workQueue | 任务等待队列 |
| threadFactory | 创建线程的工厂 |
| handler | 拒绝策略 |

### execute() 执行流程

1. 读 `ctl`（高 3 位状态 + 低 29 位 worker 数）。
2. workerCount < corePoolSize → `addWorker(command, true)` 创建核心线程。
3. 核心满 → `workQueue.offer(command)` 入队。
4. 入队失败且 workerCount < maximumPoolSize → `addWorker(command, false)` 创建非核心线程。
5. 仍失败 → `handler.rejectedExecution` 执行拒绝策略。
6. `Worker.runWorker` 循环从队列取任务执行。
7. 非核心线程 keepAliveTime 内未取到任务则被回收。

### 四种拒绝策略

| 策略 | 行为 |
| --- | --- |
| AbortPolicy | 抛 RejectedExecutionException |
| CallerRunsPolicy | 提交线程自己执行 |
| DiscardPolicy | 静默丢弃 |
| DiscardOldestPolicy | 丢弃最老任务再尝试 |

### 7 种阻塞队列

- **ArrayBlockingQueue**：数组有界队列，公平锁可选。
- **LinkedBlockingQueue**：链表队列，默认无界，注意 OOM。
- **SynchronousQueue**：不存储元素，高吞吐，配合大 max。
- **PriorityBlockingQueue**：按优先级出队，无界。
- **DelayQueue**：延时出队，元素需实现 Delayed。
- **LinkedBlockingDeque**：双向队列，支持 FIFO/FILO。
- **LinkedTransferQueue**：TransferQueue 实现，在 ThreadPoolExecutor 中行为接近 LinkedBlockingQueue。

### 为什么不建议用 Executors？

- `newFixedThreadPool` / `newSingleThreadExecutor`：默认使用无界 `LinkedBlockingQueue`，任务堆积可能 OOM。
- `newCachedThreadPool` / `newScheduledThreadPool`：`maximumPoolSize` 为 `Integer.MAX_VALUE`，可能创建海量线程导致 OOM。
- 阿里规约推荐直接 `new ThreadPoolExecutor`，参数透明、风险可控。

### 线程池生命周期

| 状态 | 说明 |
| --- | --- |
| RUNNING | 接收新任务并处理队列任务 |
| SHUTDOWN | 不接收新任务，但处理队列剩余任务 |
| STOP | 不接收新任务，不处理队列任务，中断正在执行的任务 |
| TIDYING | 所有任务终止，workerCount 为 0 |
| TERMINATED | `terminated()` 执行完成 |

### Worker 内部类

- 继承 AQS、实现 Runnable，封装一个线程和首次任务。
- `runWorker`：循环从队列取任务执行。
- `getTask`：从队列阻塞取任务，受 keepAliveTime 控制。
- `processWorkerExit`：Worker 退出时清理并判断是否需要补充线程。

## 推荐实验顺序

1. 启动后端和前端，打开 Swagger UI。
2. **源码流程页**：先理解 execute() 七步流程。
3. **线程池基础页**：向 `tinyPool` 提交 20 个任务，观察队列满后拒绝。
4. **阻塞队列页**：分别实验 `ArrayBlockingQueue`、`LinkedBlockingQueue`、`SynchronousQueue` 的入队行为。
5. **拒绝策略页**：依次实验 Abort / CallerRuns / Discard / DiscardOldest。
6. **Executors 风险页**：查看四种工厂源码参数，理解阿里规约。
7. **生命周期页**：调用 `shutdown` 和 `shutdownNow`，观察状态变化。

## 参考

- [Java 多线程：彻底搞懂线程池](https://blog.csdn.net/u013541140/article/details/95225769)
- [01-thread-pool-practice](../01-thread-pool-practice/)
