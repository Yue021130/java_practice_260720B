# Spring Event 事件监听参考指南（第34章配套）

> 本文档是第34章自写参考文档，配合 `34/docs` 三篇原文档使用：
> 原文档讲「是什么、解决什么问题、坑在哪」，本文档给「原理拆解 +
> 代码位置索引」，所有结论都有本章代码实证。

## 一、观察者模式与 Spring Event 的对应关系

Spring Event 是教科书级**观察者模式**实现：

| 观察者模式角色 | Spring Event 对应物 |
| --- | --- |
| Subject（被观察者） | `ApplicationEventPublisher`（发布者，Spring 上下文默认实现它） |
| Observer（观察者） | `@EventListener` / `@EventListener` 标注的方法 |
| notify() | `ApplicationEventMulticaster.multicastEvent()` |
| 注册观察者 | `EventListenerMethodAdapter`（容器启动时把注解方法适配成监听器注册进 multicaster） |

发布链：`publishEvent(event)` → `AbstractApplicationContext.publishEvent` → `ApplicationEventMulticaster.multicastEvent` → 遍历匹配的监听器逐个 invoke。**默认整条链都在发布线程同步执行**——这是理解一切坑的基础。

代码实证：`LeaveService` 里 `publisher.publishEvent(event)` 后，同步监听器（`AuditLogListener`）的日志打印在同一个线程（线程名相同）。

## 二、ApplicationEvent vs 普通 POJO 事件

- **Spring 4.2 之前**：事件必须继承 `ApplicationEvent`，监听器必须实现 `ApplicationListener<E>` 接口——事件类携带框架依赖。
- **Spring 4.2+**：`publishEvent(Object)` 支持任意对象，`@EventListener` 按参数类型匹配监听器。

本章两种事件的写法（`event/LeaveSubmittedEvent.java` 等）都是 4.2+ 推荐写法：纯业务 POJO + Lombok，零框架依赖。**注意类型匹配按运行时 class 精确匹配**，父类事件不会被子类参数监听器收到（除非监听父类本身）。

## 三、同步默认与 @Async 的坑

1. **默认同步**：监听器在发布线程执行，慢监听器直接拖慢接口；监听器异常会**传播给发布者**（在事务里会导致回滚）——`AuditLogListener` 注释里的坑演示。
2. **@Async 必须配线程池**：不显式配置时 Spring 用 `SimpleAsyncTaskExecutor`，**不池化、每次 new 线程**，高并发直接爆炸。本章 `config/AsyncConfig.java` 声明 `eventExecutor`（有界队列 + CallerRunsPolicy 兜底），监听器用 `@Async("eventExecutor")` 显式指定。
3. **忘了 @EnableAsync**：注解静默失效，方法退回同步执行——最常见的「异步没异步」。
4. **@Async 方法自调用不生效**：走的是 AOP 代理，this 调用绕过代理（与 @Transactional 同理）。

## 四、@TransactionalEventListener：事务事件的五个阶段

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

| 阶段 | 触发时机 | 典型用途 |
| --- | --- | --- |
| `BEFORE_COMMIT` | 事务提交前（flush 之后） | 提交前最后的校验/补充写 |
| `AFTER_COMMIT`（默认） | **事务提交成功后** | 对外通知、发消息（本章 NotifyListener） |
| `AFTER_ROLLBACK` | 事务回滚后 | 失败告警、补偿 |
| `AFTER_COMPLETION` | 提交或回滚之后（无论成败） | 清理 ThreadLocal 类资源 |

**「被坑绩效」场景复盘**（对应 `34/docs` 第二篇）：审批接口在事务里更新单据 → 用普通 `@EventListener` + `@Async` 发通知 → 监听器在另一个线程**事务提交前**读库，读到旧状态「待审批」→ 用户收到「已通过」通知，点进去却是待审批。改法就是把监听器换成 `@TransactionalEventListener(phase = AFTER_COMMIT)`：Spring 把事件暂存进事务同步器，提交成功后才投递。

两个配套坑：

- **发布方必须有事务**：`fallbackExecution` 默认 false，无事务环境事件直接丢弃。`LeaveService.approve` 的 `@Transactional` 不能删。
- **AFTER_COMMIT 后的代码无事务上下文**：监听器里写库要用 `REQUIRES_NEW` 独立事务（`EventRecordService.markHandled` 即此用法）。

## 五、@Order 与 condition

- **@Order**：同一事件的多个**同步**监听器按 @Order 值从小到大顺序执行；异步监听器各跑各的线程，@Order 只影响提交顺序不保证完成顺序。本章 `AuditLogListener(@Order(1))` → `ConditionalListener(@Order(2))` 确定性顺序。
- **condition**：SpEL 在投递前求值，false 直接跳过，事件对象固定叫 `#event`。本章 `condition = "#event.type == 'ANNUAL'"` 只对年假做台账登记——比监听器里写 if 更声明式，比按类型拆事件类更轻量。

## 六、可靠性：进程内事件会丢，怎么救

`publishEvent` 是纯内存调用：发布后、消费前宕机，事件丢失。而且进程内事件**跨不了实例**（多实例部署时 A 实例发布的事件 B 实例收不到）。

本章落地「本地消息表」最小版（`service/EventRecordService.java`）：

1. `record()` 与业务写库**同一个本地事务**（REQUIRED），保证业务数据和事件记录同生共死；
2. 广播后各监听器 `markHandled()` 回写消费明细（REQUIRES_NEW 独立提交）；
3. 生产再加一步：定时任务扫 `status=0` 且超时的事件重投。

跨实例/高可靠场景直接上 MQ：RocketMQ 事务消息（本地事务 + 消息发送半事务）或 Kafka outbox 模式；进程内事件只留做单实例内的轻量解耦。

## 七、事件 vs MQ 的边界

| 维度 | Spring Event | MQ |
| --- | --- | --- |
| 范围 | 单 JVM 进程内 | 跨进程跨实例 |
| 可靠性 | 内存调用，宕机即丢 | 持久化、重试、死信 |
| 顺序 | 同线程内有序 | 需分区保证 |
| 复杂度 | 零依赖 | 引入 Broker |
| 适用 | 模块解耦、事务后动作、单实例 | 分布式事务最终一致、削峰、跨服务 |

经验法则：**先问「另一个实例要不要知道这件事」**——不要，用事件；要，直接 MQ，不要试图用事件表自己造轮子（本章 event_record 是教学演示，生产请用现成方案）。

## 八、坑清单（速查）

1. 监听器没触发：方法参数类型与事件实际类型不匹配（父类事件 ≠ 子类参数）。
2. @TransactionalEventListener 不执行：发布方没事务（fallbackExecution=false）。
3. 异步没异步：没开 @EnableAsync，或 @Async 自调用。
4. 线程爆炸：@Async 用了默认 SimpleAsyncTaskExecutor。
5. 主流程被监听器拖挂：同步监听器异常传播/耗时过长——非关键监听改 @Async + 线程池隔离。
6. 事务内读旧数据：通知类监听器没用 AFTER_COMMIT。
7. 内存统计重启丢：进程内计数器（StatsListener 的 AtomicLong）只适合瞬态指标，持久指标落库。
