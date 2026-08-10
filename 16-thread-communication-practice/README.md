# 16-thread-communication-practice：Java 线程间通信方式实践

本模块把 **Java 线程间通信** 的完整体系——按底层机制分 **七大类 17 种方式**——转化为可运行、可交互的 Spring Boot + Vue 3 项目代码。每个通信工具都做成一个真实 JVM 上能跑出可见现象的实验（谁被唤醒、等了多久、丢了几次更新、并发峰值多少），前端面板一键运行，便于系统学习。

**开箱即用**：不需要数据库、不需要消息队列，所有多线程演示都在内存里编排（原生 `Thread` / `ExecutorService` + 各种同步器），直接 `mvn spring-boot:run` 就能玩。

> 🎯 一句话总纲：**除了共享变量轮询，其余所有线程通信的本质都是「阻塞 + 等待队列 + 唤醒」，操作系统层面又全部落到 futex 的等待队列上——一层层抽象，殊途同归。**（本模块 10 章 `unified-model` 接口直接演示这张分层图）

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 测试：JUnit 5 + MockMvc + AssertJ（全场景接口集成测试）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS（面板支持「运行实验 / 观察 JSON 结果」卡片）
- 端口：后端 **8096**，前端 **5189**

## 核心：线程间通信七大类（本模块的知识骨架）

| 大类 | 方式 | 核心一句话 |
| --- | --- | --- |
| **一、共享内存** | 共享变量 + `volatile` | 解决「看得见」（可见性 + 禁重排，不保证原子） |
| | `AtomicXxx`（CAS） | 解决「改得对」（无锁原子更新） |
| **二、锁对象/等待通知** | `Object.wait/notify/notifyAll` | 每个对象自带一个等待队列（monitor 的 WaitSet） |
| | `Condition` | 一个锁挂多个等待队列，精准唤醒某一类 |
| **三、线程协作控制** | `Thread.join()` | A 等 B 结束（底层 `wait(0)` + `isAlive()` 轮询） |
| | `interrupt()` | 打招呼不是强杀，协作式优雅退出 |
| | `LockSupport.park/unpark` | 无需持锁、指定线程、信号可预发（AQS 基石） |
| **四、JUC 同步工具** | `CountDownLatch` | 一个线程等 N 个（一次性） |
| | `CyclicBarrier` | N 个互相等齐（可复用） |
| | `Semaphore` | 限流，控制同时进入数 |
| | `Exchanger` | 两线程碰头双向交换 |
| | `Phaser` | Latch + Barrier 合体，多阶段 + 动态增减 |
| **五、阻塞队列** | `BlockingQueue` 家族 | 队列即通信载体，天然解耦 + 背压 |
| **六、异步结果传递** | `Future/FutureTask` | 跨线程传返回值，`get()` 阻塞等 |
| | `CompletableFuture` | 回调编排、allOf/anyOf、链式依赖 |
| **七、IO/其他通道** | `PipedStream` 等 | 单向管道（标准答案之一）；跨进程才用 Socket/共享内存 |

## 模块结构

```
16-thread-communication-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/comm/
│   ├── ThreadCommunicationPracticeApplication.java
│   ├── common/         # 统一响应 ApiResponse、全局异常处理
│   ├── config/         # CommPracticeProperties、OpenAPI、CORS、演示线程池
│   ├── support/        # CommLogStore（最近实验记录）
│   ├── shared/         # 01 共享内存：volatile 可见性 / 原子类 CAS
│   ├── waitnotify/     # 02 等待通知：wait/notify/notifyAll 生产者-消费者
│   ├── condition/      # 03 Condition：有界缓冲 / signal 精准唤醒
│   ├── cooperate/      # 04 线程协作控制：join / interrupt
│   ├── locksupport/    # 05 LockSupport：park/unpark / 信号预发
│   ├── sync/           # 06 JUC 同步工具：Latch/Barrier/Semaphore/Exchanger/Phaser
│   ├── queue/          # 07 阻塞队列：put/take 背压 / 队列家族
│   ├── async/          # 08 异步结果：FutureTask / CompletableFuture 编排
│   ├── pipe/           # 09 管道与其他通道：PipedStream / 跨进程思路
│   └── summary/        # 10 选型总结：七大类总览 / 选型表 / 底层统一模型
├── src/main/resources/application.yml
├── src/test/java/com/example/comm/   # 上下文测试 + 全场景接口测试
└── web/                # Vue 3 前端面板（5189）
```

## 快速启动

### 后端

```bash
cd 16-thread-communication-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8096/swagger-ui/index.html

### 前端

```bash
cd 16-thread-communication-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5189

### 运行测试

```bash
cd 16-thread-communication-practice
mvn test
```

## 接口速查

### 01. 共享内存 `/api/shared`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/shared/volatile-demo?workers=4&flagDelayMs=200` | GET | 主线程置标志，N 个 worker 轮询感知，统计感知延迟（可见性） |
| `/api/shared/atomic-demo?threads=4&increments=1000` | GET | 普通 int++ 丢更新 vs AtomicInteger 精确（CAS 原子性） |
| `/api/shared/explain` | GET | volatile 三性 / CAS 原理 / ABA / 适用场景（八股） |

### 02. 等待通知 wait/notify `/api/waitnotify`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/waitnotify/producer-consumer?productions=20&capacity=3` | GET | 有界缓冲，满/空时 wait，放/取后 notifyAll |
| `/api/waitnotify/explain` | GET | 为什么必须在 synchronized / 为什么 while 不用 if / notify vs notifyAll |

### 03. Condition 条件队列 `/api/condition`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/condition/bounded-buffer?productions=20&capacity=3` | GET | notFull/notEmpty 两个条件队列的有界缓冲 |
| `/api/condition/signal-demo?waiters=4` | GET | signal 只唤醒偶数组、奇数组继续沉睡（精准唤醒现场） |
| `/api/condition/explain` | GET | 与 wait/notify 区别 / signal vs signalAll / 为什么绑定 Lock |

### 04. 线程协作控制 `/api/cooperate`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/cooperate/join-demo?tasks=3&taskMs=100` | GET | 主线程等 N 个子任务全部完成，验证总耗时 ≈ taskMs |
| `/api/cooperate/interrupt-demo?mode=sleep` | GET | sleep 被打断抛异常退出 |
| `/api/cooperate/interrupt-demo?mode=loop` | GET | 循环里 isInterrupted 感知退出 |
| `/api/cooperate/explain` | GET | join 底层 / interrupt 是协作不是强杀 / 中断状态清理 |

### 05. LockSupport `/api/locksupport`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/locksupport/park-unpark?delayMs=200` | GET | 线程 park 挂起，主线程延迟 unpark，记录等待耗时 |
| `/api/locksupport/unpark-first` | GET | 先 unpark 后 park 立即通过（信号预发；wait 这么做会死锁） |
| `/api/locksupport/explain` | GET | 三优势 / permit 机制 / AQS 基石 |

### 06. JUC 同步工具 `/api/sync`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/sync/latch-demo?workers=3` | GET | 主线程 await 等 N 个 worker countDown（一次性） |
| `/api/sync/barrier-demo?parties=3&rounds=3` | GET | N 线程每轮到齐才放行，可循环复用 |
| `/api/sync/semaphore-demo?permits=2&threads=8` | GET | 限流：并发峰值 ≤ permits |
| `/api/sync/exchanger-demo` | GET | 两线程碰头双向交换数据 |
| `/api/sync/phaser-demo?parties=3` | GET | 多阶段 + 中途 register/deregister 动态增减 |
| `/api/sync/explain` | GET | 五件套对比表 / Latch 一次性 vs Barrier 循环 |

### 07. 阻塞队列 `/api/queue`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/queue/blocking-demo?productions=20&capacity=3` | GET | 满则阻塞生产者（背压）、空则阻塞消费者 |
| `/api/queue/family` | GET | Array/Linked/Synchronous/Priority/Delay 家族 + 四种行为分组 |
| `/api/queue/explain` | GET | 原理（Condition）/ put-take vs offer-poll / 背压 / 选型 |

### 08. 异步结果传递 `/api/async`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/async/future-demo?taskMs=80` | GET | FutureTask + new Thread，get() 阻塞拿结果 |
| `/api/async/cf-demo?taskMs=50` | GET | supplyAsync → thenApply → thenApplyAsync 链式编排 |
| `/api/async/cf-combine?tasks=3` | GET | allOf 等全部 / anyOf 任一先完成 / exceptionally 兜底 |
| `/api/async/explain` | GET | Future 三个痛点 vs CompletableFuture 四板斧 |

### 09. 管道与其他通道 `/api/pipe`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pipe/piped-demo?messages=5` | GET | 写线程写、读线程读，Piped 单向管道传数据 |
| `/api/pipe/cross-process` | GET | Socket 回环 / 共享内存 / 文件跨进程思路 |
| `/api/pipe/explain` | GET | 管道流本质 / 单线程死锁警告 / 线程内 vs 跨进程 |

### 10. 选型总结 `/api/summary`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/summary/overview` | GET | 七大类 17 种方式全景图 |
| `/api/summary/decision-table` | GET | 需求场景 → 首选方案选型表 |
| `/api/summary/unified-model` | GET | 底层统一模型分层图（业务层 → AQS → LockSupport → futex） |
| `/api/summary/explain` | GET | 七大类一句话记忆 + 面试回答流程 |

## 面试八股

### Java 线程间通信有哪些方式？

按底层机制分七大类 17 种（本模块逐一有演示）：
1. **共享内存**：`volatile`（可见性，不保证原子）、`AtomicXxx`（CAS 无锁原子）；
2. **等待通知**：`Object.wait/notify/notifyAll`、`Condition`（Lock 的多条件队列）；
3. **线程协作**：`join()`、`interrupt()`、`LockSupport.park/unpark`；
4. **JUC 同步工具**：`CountDownLatch` / `CyclicBarrier` / `Semaphore` / `Exchanger` / `Phaser`；
5. **阻塞队列**：`BlockingQueue` 家族（队列即通信 + 背压）；
6. **异步结果**：`Future/FutureTask`、`CompletableFuture`；
7. **IO/通道**：`PipedStream`；跨进程用 Socket / 共享内存 / MQ。

### wait/notify 为什么必须在 synchronized 里？为什么用 while 不用 if？

- 必须持有该对象的 monitor 才能调用，否则抛 `IllegalMonitorStateException`——wait 要「持锁 → 释放锁 → 进等待队列」，notify 要「持锁 → 唤醒」，锁是安全协作的前提；
- `while (条件不满足) lock.wait();` 是因为唤醒后条件未必满足：**虚假唤醒**（平台层可能莫名唤醒）+ **多线程竞争**（notifyAll 唤醒的都抢锁，先抢到的可能把资源又耗光），必须回到 while 重新判断。

### notify 和 notifyAll 的区别？

`notify` 只唤醒等待队列里的一个（随机/不指定），唤醒错了类型可能没人补位 → 假死；`notifyAll` 唤醒全部让它们自行竞争与重新判断，安全但可能惊群。教科书都用 `notifyAll`；只有一个等待者时 `notify` 等价且更省。

### Condition 比 wait/notify 高级在哪？

一个 Lock 可以 `newCondition()` 多个条件队列（如 notFull/notEmpty），`signal` 精准唤醒某一类等待者（wait/notify 每个对象只有一个 WaitSet，只能广播）；还支持 `await(ms)` 超时与中断。本质是 AQS 的条件队列。

### LockSupport.park/unpark 与 wait/notify 的区别？

1. **无需持锁**（wait 必须 synchronized 内）；
2. **可指定线程**（`unpark(thread)` 精确到人，notify 只能广播）；
3. **信号可预发**：先 unpark 再 park 不丢信号（permit 最多累计 1 个），先 notify 再 wait 必死锁。
AQS、FutureTask、线程池阻塞全靠它，是 JUC 的基石。

### CountDownLatch 和 CyclicBarrier 的区别？

- **方向**：Latch 是「一个等 N 个」（主线程 await，worker countDown）；Barrier 是「N 个互相等齐」（每个都 await）；
- **复用**：Latch 一次性（计数归零报废）；Barrier 可循环复用（每轮到齐放行）；
- **典型**：Latch 等所有服务就绪 / 并发压测启动信号；Barrier 分页抓取到齐合并 / 多线程计算到齐归约。

### 为什么说阻塞队列能「背压」？

`put` 满则阻塞、`take` 空则阻塞：生产者速度 > 消费者速度时，队列不会无限堆积而 OOM，只会让生产者放慢等消费者——这就是背压，也是消息中间件削峰填谷的同款思想。内部就是 ReentrantLock + 两个 Condition（第 03 章有界缓冲的现成封装）。

### CompletableFuture 解决了 Future 的哪些痛点？

Future 三个痛点：`get()` 阻塞（等结果期间干不了别的）、**不能编排**（结果不能直接喂给下个任务）、**不能组合**（allOf/anyOf 只能自己拼）。CompletableFuture 补上：`thenApply/thenAccept/thenCompose` 链式编排、`allOf/anyOf` 组合、`exceptionally/handle` 容错。注意生产必须传独立线程池，别用默认的 ForkJoinPool.commonPool。

### 为什么说所有线程通信「殊途同归」？

除了共享变量轮询（唯一不阻塞的例外），其余方式的本质都是 **「阻塞 + 等待队列 + 唤醒」**：wait/notify 靠对象 monitor 的 WaitSet，Condition/AQS 靠 CLH/条件队列，BlockingQueue 直接就是队列，LockSupport 靠 permit 信号；操作系统层面又全部落到 **futex**（Linux 快速用户态互斥量）的等待队列上。一层层抽象，底层同一套模型。

## 推荐实验顺序

1. 启动后端与前端，先点 **01 共享内存**：volatile 可见性（感知延迟）→ 原子类（丢更新 vs 不丢），建立「看得见、改得对」的直觉。
2. **02 等待通知**：跑生产者-消费者，理解满/空时的 wait 与 notifyAll。
3. **03 Condition**：跑有界缓冲，再跑「signal 精准唤醒」——对比 02 的广播式，体会多条件队列的价值。
4. **04 协作控制**：join 等全部完成（看总耗时 ≈ taskMs 而非串行）；interrupt 两种模式对比「优雅退出 vs 强杀」。
5. **05 LockSupport**：park→unpark 与「先 unpark 后 park」，亲手验证「信号预发」这个最反直觉的点。
6. **06 同步工具**：逐个跑 Latch（一等多）→ Barrier（多等多/复用）→ Semaphore（限流峰值）→ Exchanger（交换）→ Phaser（多阶段），最后看对比表。
7. **07 阻塞队列**：跑背压演示（看生产者因满放慢几次），再看家族速览与四种行为分组。
8. **08 异步**：FutureTask 拿结果 → CompletableFuture 链式 → allOf/anyOf/exceptionally 组合。
9. **09 管道**：跑 Piped 单向管道，看跨进程思路（知道有这回事即可）。
10. **10 选型总结**：七大类总览 → 选型表 → **底层统一模型分层图**（点睛之笔），串起全模块。

## 参考

- [Oracle Java 教程：并发（Thread 通信）](https://docs.oracle.com/javase/tutorial/essential/concurrency/index.html)
- [JUC 源码：AQS（AbstractQueuedSynchronizer）与 ConditionObject](https://github.com/openjdk/jdk8u/blob/master/jdk/src/share/classes/java/util/concurrent/locks/AbstractQueuedSynchronizer.java)
- [LockSupport 文档](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/LockSupport.html)
- [CompletableFuture 文档](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- [futex（Linux 快速用户态互斥量）维基](https://en.wikipedia.org/wiki/Futex)
