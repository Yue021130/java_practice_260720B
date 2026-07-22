# 02-juc-practice —— Java 并发包（JUC）全场景实践

「java高级知识」系列第 2 个专题。把 `java.util.concurrent` 的核心知识点全部包装成可运行的现实业务场景，
配合 **Vue 3 + Vite** 前端面板，把多线程执行过程渲染成带线程名的时间线，直观看懂锁、原子类、并发容器、
同步工具、异步编排、ThreadLocal 与 JMM 基础。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven，端口 **8082**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5175**

## JUC 全场景一览（7 大模块，32 个实验）

| 模块 | 场景 | 对应端点 | 面试考点 |
| --- | --- | --- | --- |
| 锁 | 转账死锁与解决 | `POST /api/locks/transfer-deadlock` | 死锁四条件、tryLock、按序加锁 |
| 锁 | 公平锁 vs 非公平锁 | `POST /api/locks/fair-vs-nonfair` | AQS、CLH 队列、默认非公平 |
| 锁 | lockInterruptibly 可中断 | `POST /api/locks/interruptible` | synchronized 不可中断 |
| 锁 | 读写锁商品缓存 | `POST /api/locks/readwrite-cache` | 读读共享、锁降级、锁升级死锁 |
| 锁 | StampedLock 乐观读 | `POST /api/locks/stamped-optimistic` | stamp 校验、不可重入 |
| 锁 | Condition 手写生产者消费者 | `POST /api/locks/condition-buffer` | 多等待队列、while+await、signal |
| 锁 | LockSupport 演示 | `POST /api/locks/locksupport` | 许可不累积、park 响应中断 |
| 原子类 | 库存扣减三路对比 | `POST /api/atomic/stock-compare` | CAS、自旋、原子性 |
| 原子类 | LongAdder vs AtomicLong | `POST /api/atomic/longadder-vs-atomic` | Cell 分段、sum 弱一致 |
| 原子类 | ABA 问题与修复 | `POST /api/atomic/aba` | AtomicStampedReference |
| 原子类 | 无锁订单状态机 | `POST /api/atomic/order-state` | CAS 做状态迁移 |
| 并发容器 | HashMap 并发事故 | `POST /api/container/hashmap-accident` | JDK7 成环、JDK8 丢数据 |
| 并发容器 | CHM 原子操作三件套 | `POST /api/container/chm-ops` | putIfAbsent、computeIfAbsent、merge |
| 并发容器 | CopyOnWrite 白名单 | `POST /api/container/cow-whitelist` | 快照、CME |
| 并发容器 | DelayQueue 订单超时取消 | `POST /api/container/delayqueue-order` | Leader-Follower、延迟方案选型 |
| 并发容器 | BlockingQueue 家族对比 | `POST /api/container/blockingqueue-family` | 线程池 workQueue 选型 |
| 并发容器 | 跳表排行榜 | `POST /api/container/skiplist-leaderboard` | 跳表 O(logN)、CAS 无锁 |
| 同步工具 | CountDownLatch 并行自检 | `POST /api/sync/latch-startup` | 一次性、AQS 共享模式 |
| 同步工具 | CyclicBarrier 分片报表 | `POST /api/sync/barrier-report` | 可复用、barrierAction |
| 同步工具 | Semaphore 接口限流 | `POST /api/sync/semaphore-limit` | 连接池本质、tryAcquire 降级 |
| 同步工具 | Exchanger 双人对账 | `POST /api/sync/exchanger-reconcile` | 成双成对、超时 |
| 异步编排 | CompletableFuture 详情页聚合 | `POST /api/future/cf-detail` | 自定义池、allOf、exceptionally、applyToEither、超时兜底 |
| 异步编排 | 老式 Future 对比 | `POST /api/future/old-future` | get 阻塞、不能链式编排 |
| 异步编排 | ForkJoin 分治求和 | `POST /api/future/forkjoin-sum` | work-stealing、阈值 |
| 异步编排 | 定时任务两种模式对比 | `POST /api/future/scheduled-compare` | fixedRate/fixedDelay、异常静默停止 |
| ThreadLocal | traceId 全链路传递 | `POST /api/threadlocal/trace-chain` | ThreadLocalMap、MDC |
| ThreadLocal | 泄漏与串号演示 | `POST /api/threadlocal/leak-demo` | value 强引用、线程池必须 remove |
| ThreadLocal | 线程池下上下文丢失 | `POST /api/threadlocal/pool-context` | InheritableThreadLocal 坑、TTL |
| JMM 基础 | volatile 可见性 | `POST /api/base/volatile-visibility` | JMM、happens-before、重排序 |
| JMM 基础 | volatile 不保证原子性 | `POST /api/base/volatile-not-atomic` | i++ 三步、原子类 |
| JMM 基础 | DCL 双重检查单例 | `POST /api/base/dcl-singleton` | 指令重排、volatile |
| JMM 基础 | 死锁制造与自动检测 | `POST /api/base/deadlock-detect` | jps/jstack/ThreadMXBean |

## 模块面试点速记

### 锁（Lock / AQS）

- **AQS** 是 JUC 锁的基石：一个 int state + 一个 FIFO（CLH 变体）等待队列。
- `synchronized` vs `Lock`：前者 JVM 托管、自动释放；后者支持可中断、超时、公平、多 Condition，但必须手动 `unlock()`。
- **死锁四条件**：互斥、持有并等待、不可剥夺、循环等待。破解常用：按固定顺序加锁、`tryLock(timeout)` 超时放弃。
- **读写锁**：读读共享、读写互斥、写写互斥。允许锁降级（写锁内获取读锁再释放写锁），不允许锁升级（会死锁）。
- **StampedLock**：乐观读 `tryOptimisticRead` 不加锁，读完后必须 `validate(stamp)`；失败升级为 `readLock`。不可重入。
- **Condition**：一个 `Lock` 可挂多个 Condition（`notFull` / `notEmpty`），比 `wait/notify` 更精确；`await()` 必须放在 `while` 循环里。

### 原子类（CAS）

- **CAS** 底层是 `Unsafe.compareAndSwap`，对应 CPU `cmpxchg` 原子指令。
- `AtomicLong` 高并发下所有线程竞争同一变量，自旋开销大；`LongAdder` 用 `base + Cell[]` 分段打散热点，`sum()` 是弱一致快照。
- **ABA** 问题：`AtomicReference` 只看值，中间被改回原值发现不了；用 `AtomicStampedReference` 加版本号。
- **无锁状态机**：`compareAndSet(旧状态, 新状态)` 天然保证迁移只发生一次，数据库对应 `update where status=旧状态`。

### 并发容器

- **ConcurrentHashMap**：JDK7 用 Segment 分段锁；JDK8 改为 CAS + `synchronized` 锁桶头节点，锁粒度更细。`size()` 用 `baseCount + CounterCell`（和 LongAdder 同源）。
- **HashMap**：JDK7 头插法扩容可能成环导致死循环；JDK8 改尾插但仍会丢数据/覆盖，**不能并发使用**。
- **CopyOnWriteArrayList**：写时复制数组，读无锁、迭代器是快照不会抛 `ConcurrentModificationException`；写多时内存翻倍。
- **DelayQueue**：内部 `PriorityQueue` 按到期时间排序，`Leader-Follower` 减少空转；生产替代有 Redis ZSet 轮询、时间轮、MQ 延迟消息。
- **BlockingQueue**：线程池 `workQueue` 选型关键。`SynchronousQueue` 不存储直接交接；`LinkedBlockingQueue` 默认容量 `Integer.MAX_VALUE`，是阿里规约点名 `newFixedThreadPool` 的坑。
- **ConcurrentSkipListMap**：跳表多层索引 O(logN)，插入用 CAS 无锁化，并发吞吐远高于 `TreeMap` + 外部锁。

### 同步工具

- **CountDownLatch**：一次性不可复用；`countDown` 可在任意线程调用；基于 AQS 共享模式。
- **CyclicBarrier**：可复用；计数归参与线程自己；支持 `barrierAction` 到达回调。某线程中断/超时会导致 barrier broken。
- **Semaphore**：限流器/连接池的本质就是许可发放与归还；`tryAcquire(timeout)` 拿不到可走降级。
- **Exchanger**：必须成双成对；单边调用会阻塞，应使用带超时的版本。

### 异步编排

- **CompletableFuture**：`supplyAsync` 不传线程池默认走 `ForkJoinPool` 公共池，生产必须换自定义池，否则一个任务阻塞会拖累所有并行流。
- `thenCompose` 用于串行有依赖；`thenCombine` / `applyToEither` 用于并行；`exceptionally` / `handle` 会吞异常，链路里漏了它异常会静默丢失。
- **Future**：`get()` 阻塞、无法链式组合，Java 8 引入 CF 解决。
- **ForkJoinPool**：工作窃取（work-stealing）；拆分阈值太小分治开销反超；`parallelStream` 共用公共池。
- **ScheduledThreadPoolExecutor**：`scheduleAtFixedRate` 以上次开始为基准，任务超时会追赶；`scheduleWithFixedDelay` 以上次结束顺延。任务体抛异常且未捕获，后续调度会**静默停止**。

### ThreadLocal

- 每个线程持有 `ThreadLocalMap`：key 是 `ThreadLocal` 的弱引用，value 是强引用。
- 内存泄漏根因：key 被 GC 后 value 成为访问不到的脏数据，线程（尤其线程池）长期存活则 value 无法回收。
- **必须 `finally remove`**，否则下一个任务可能读到上一个请求的上下文（串号）。MDC 全链路追踪就是这个原理。
- `InheritableThreadLocal` 只在线程创建时拷贝一次，**线程池复用下会拿到旧值**；阿里 `TransmittableThreadLocal` 用装饰任务在提交时快照、执行时回放解决。

### JMM 基础

- **JMM**：主内存 + 各线程工作内存；`volatile` 写 happens-before 后续读，并禁止指令重排。
- `volatile` **不保证原子性**：`i++` 是读-改-写三步，要原子性用锁或原子类。
- **DCL**：`instance = new` 三步（分配→初始化→赋值）可能重排成 1→3→2，导致其他线程拿到半初始化对象；`volatile` 禁止该重排。
- 死锁排查三板斧：`jps` 找进程 → `jstack <pid>` 看 "Found one Java-level deadlock" → 或用 `ThreadMXBean.findDeadlockedThreads()` 编程式巡检。

## 接口文档（Swagger UI）

项目集成了 SpringDoc OpenAPI（`springdoc-openapi-ui:1.7.0`，对应 Spring Boot 2.7），
接口文档根据代码中的 `@Tag` / `@Operation` / `@Parameter` 注解自动生成。

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8082/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8082/v3/api-docs

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

浏览器打开 http://localhost:5175 即可看到监控面板。

> 前端在开发时通过 Vite proxy 把 `/api` 转发到 `http://localhost:8082`，
> 因此直接访问 `http://localhost:5175` 即可，无需单独处理跨域。

## 运行测试

后端自带集成测试（JUnit 5 + MockMvc + AssertJ）：

```bash
mvn test
```

测试覆盖：

- `JucPracticeApplicationTests`：Spring 上下文加载。
- `ScenarioApiTest`：通过 MockMvc 调用全部 32 个场景接口，验证均返回 200 且 data 非空。

## 推荐实验顺序

1. **JMM 基础**：先把 `volatile` 可见性/原子性、`DCL` 跑通，建立内存模型直觉。
2. **锁**：从转账死锁、公平非公平、读写锁，到 Condition 手写生产者消费者，理解 AQS 与锁语义。
3. **原子类**：看库存三路对比、LongAdder 压测、ABA 修复，理解 CAS 的能力与边界。
4. **并发容器**：HashMap 事故 vs CHM、DelayQueue 订单超时、跳表排行榜，理解容器选型。
5. **同步工具**：CountDownLatch/CyclicBarrier/Semaphore/Exchanger，理解线程协作模型。
6. **异步编排**：CompletableFuture 详情页聚合是核心，体会并行、编排、降级、超时。
7. **ThreadLocal**：traceId 链路、串号事故、线程池丢失，理解上下文传递与泄漏根因。

## 项目结构

```
02-juc-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/juc/
│   ├── JucPracticeApplication.java
│   ├── common/        ApiResponse、ScenarioResult、ScenarioLog、NamedThreadFactory
│   ├── config/        CorsConfig、OpenApiConfig
│   ├── locks/         LockController / LockScenarioService
│   ├── atomic/        AtomicController / AtomicScenarioService
│   ├── container/     ContainerController / ContainerScenarioService
│   ├── sync/          SyncController / SyncScenarioService
│   ├── future/        FutureController / FutureScenarioService
│   ├── threadlocal/   ThreadLocalController / ThreadLocalScenarioService
│   └── base/          BaseController / BaseScenarioService
├── src/test/java/com/example/juc/
└── web/               Vue 3 + Vite 前端
```
