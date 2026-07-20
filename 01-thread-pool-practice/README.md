# 01-thread-pool-practice — Java 线程池实践

「java高级知识」系列第 1 个专题。通过一个 Spring Boot 后端 + React 前端的实时监控面板，直观演示线程池的参数配置、运行指标与拒绝策略。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven，端口 **8081**
- 前端：React 18 + Vite 5（纯手写 CSS，无 UI 库），开发端口 **5174**

## 线程池七大参数

`ThreadPoolExecutor` 构造方法的七个参数，面试必问：

| 参数 | 含义 | 说明 |
| --- | --- | --- |
| corePoolSize | 核心线程数 | 池中长期保留的线程数，默认空闲也不回收 |
| maximumPoolSize | 最大线程数 | 队列满后允许扩容到的线程上限 |
| keepAliveTime | 空闲存活时间 | 非核心线程空闲多久后被回收 |
| unit | 时间单位 | keepAliveTime 的单位（秒/毫秒等） |
| workQueue | 任务队列 | 存放等待执行的任务（ArrayBlockingQueue / LinkedBlockingQueue / SynchronousQueue 等） |
| threadFactory | 线程工厂 | 创建线程的方式，建议自定义线程名前缀方便排查问题 |
| handler | 拒绝策略 | 队列满且线程数达上限时，如何处理新任务 |

**任务入池的执行流程**（高频考点）：提交任务 → 线程数 < core 则新建线程执行 → 否则进队列排队 → 队列满且线程数 < max 则扩容线程 → 线程数也达 max 则触发拒绝策略。

## 四种 JDK 内置拒绝策略

| 策略 | 行为 | 适用场景 |
| --- | --- | --- |
| AbortPolicy（默认） | 直接抛 `RejectedExecutionException` | 希望调用方显式感知“放不下” |
| CallerRunsPolicy | 由提交任务的线程自己执行 | 变相限流，给上游反压 |
| DiscardPolicy | 静默丢弃，不抛异常 | 允许丢失的任务（慎用） |
| DiscardOldestPolicy | 丢弃队列中最老的任务，再尝试入队 | 重视新数据的场景（如监控采样） |

本项目还演示了**自定义拒绝策略** `CountingRejectedHandler`：计数 + 打 WARN 日志。

## 三个演示线程池

| 池名 | 配置 | 演示目的 |
| --- | --- | --- |
| cpuPool | core = max = CPU 核数，队列 200 | CPU 密集型推荐配置：线程数 ≈ 核数，core == max 不扩容 |
| ioPool | core = 2×核数，max = 4×核数，队列 500 | IO 密集型配置：线程大量时间等 IO 不占 CPU，可多配线程 |
| customPool | core = 2，max = 4，队列 = 5，自定义拒绝策略 | 故意配小，最多同时容纳 9 个任务，用来观察拒绝策略触发 |

经验法则：

- CPU 密集型：线程数 ≈ 核数（+1），多了只增加上下文切换；
- IO 密集型：线程数 ≈ 2×核数 起步，精细公式为 `核数 × (1 + 平均等待时间 / 平均计算时间)`；
- **不要用无界队列**：队列无限长时 maxPoolSize 永远不生效，任务堆积最终 OOM。

## 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/pool/{poolName}/submit?count=N&taskDurationMs=500` | 提交 N 个模拟任务（sleep 模拟耗时） |
| GET | `/api/pool/metrics` | 返回所有池的实时指标（活跃线程、队列积压、已完成、拒绝数等） |
| POST | `/api/pool/{poolName}/resize?corePoolSize=&maxPoolSize=` | 运行期动态调整核心/最大线程数 |

## 接口文档（Swagger UI）

项目集成了 SpringDoc OpenAPI（`springdoc-openapi-ui:1.7.0`，对应 Spring Boot 2.7），
接口文档根据代码中的 `@Tag` / `@Operation` / `@Parameter` / `@Schema` 注解自动生成，无需手写。

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8081/swagger-ui/index.html （可在线调试每个接口）
- OpenAPI JSON 描述：http://localhost:8081/v3/api-docs

文档元信息（标题、描述、版本）集中在 `config/OpenApiConfig.java` 中配置。

## 启动步骤

后端（项目根目录）：

```bash
mvn spring-boot:run
```

前端（另开一个终端）：

```bash
cd web
npm install
npm run dev
```

浏览器打开 http://localhost:5174 即可看到监控面板（每 2 秒自动刷新）。

## 运行测试

后端自带单元测试（JUnit 5 + MockMvc + AssertJ，由 spring-boot-starter-test 提供）：

```bash
mvn test
```

测试类说明（`src/test/java/com/example/threadpool/`）：

| 测试类 | 验证内容 |
| --- | --- |
| ThreadPoolPracticeApplicationTests | Spring 上下文加载冒烟测试 |
| ThreadPoolControllerTest | MockMvc 测三个接口：metrics 返回三池指标、submit 合法/非法、resize 非法参数 |
| CustomPoolRejectionTest | 核心教学点：打满 customPool 后拒绝计数上涨，且 metrics 接口可见 |
| CountingRejectedHandlerTest | 纯 JUnit：直接触发 rejectedExecution，验证计数递增 |

## 推荐实验流程

1. **观察默认状态**：启动后先看三个池的 core/max 配置与空闲指标（活跃线程 0、池大小 0——线程是懒创建的，来任务才建）。
2. **触发拒绝策略**：向 `customPool` 提交 20 个耗时 2000ms 的任务。它最多同时容纳 4 运行 + 5 排队 = 9 个，其余 11 个被拒绝，观察面板上「被拒绝任务」红色数字上涨，控制台同时打印 WARN 日志。
3. **观察队列积压**：向 `ioPool` 提交 100 个耗时 1000ms 的任务，看活跃线程打满 core 后队列进度条上涨，再逐步消化。
4. **动态调参**：任务积压时，用调参面板把 `ioPool` 的 core/max 调大（如 32/64），观察池大小立即扩容、积压加速消化——这就是「线程池参数动态化」的底层能力。
5. **对比默认拒绝策略**：向 `cpuPool` 提交远超 200 个任务，观察 AbortPolicy 的效果——接口返回里 `rejected` 不为 0，但卡片上的拒绝计数不变（因为只有 customPool 用了计数策略）。

## 常见面试点速记

- 为什么线程数不是越多越好？—— 上下文切换成本、内存占用（每线程默认约 1MB 栈）。
- core 与 max 相等意味着什么？—— 固定大小线程池，队列成为唯一缓冲。
- 如何动态调整线程池参数？—— `setCorePoolSize` / `setMaximumPoolSize` 支持运行期调用（注意顺序：调大先设 max，调小先设 core）。
- 如何监控线程池？—— `getActiveCount()` / `getPoolSize()` / `getQueue().size()` / `getCompletedTaskCount()`，生产上常配合 Micrometer 上报。
- 为什么阿里规约推荐手动 new ThreadPoolExecutor 而不是 `Executors` 工厂方法？—— `newFixedThreadPool` / `newSingleThreadExecutor` 用无界队列可能 OOM，`newCachedThreadPool` 的 max 是 `Integer.MAX_VALUE` 可能创建海量线程。
