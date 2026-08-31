# 24. 异步任务与线程池实战（@Async + Spring Event）

## 项目定位

Spring Boot 2.7 + `@Async` 自定义线程池 + `Spring Event` 解耦实战，覆盖：

- 自定义 `ThreadPoolTaskExecutor` 替代默认线程池
- `ApplicationEventPublisher` 发布事件
- 多监听器 `@EventListener` 异步并行处理
- 同步 vs 异步性能对比
- Vue3 前端闭环 + 八股速记

> 真实业务场景：电商订单支付成功后，需要同时发送短信、邮件、发放积分，使用事件机制解耦主流程。

---

## 技术栈

| 技术 | 版本 | 作用 |
|------|------|------|
| Spring Boot | 2.7.18 | Web 容器、JPA、异步支持 |
| H2 | 2.x | 内存数据库 |
| Vue3 + Vite | 3.4 + 5.2 | 前端演示 |

---

## 快速启动

### 后端

```bash
cd 24-async-event-practice
mvn spring-boot:run
```

服务端口：`8104`

### 前端

```bash
cd 24-async-event-practice/web
npm install
npm run dev
```

前端端口：`5197`，代理到后端 `8104`。

### Swagger

访问：http://localhost:8104/swagger-ui.html

---

## 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/create` | 创建订单 |
| POST | `/api/order/pay` | 支付（异步事件） |
| POST | `/api/order/pay-sync` | 支付（同步对比） |
| GET | `/api/order/notify-logs` | 查询通知日志 |
| GET | `/api/order/explain` | 八股速记 |

---

## 核心代码解读

### 1. 开启异步支持

```java
@EnableAsync
@SpringBootApplication
public class AsyncEventPracticeApplication { }
```

### 2. 自定义线程池

```java
@Bean("taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("async-task-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### 3. 发布事件

```java
OrderPaidEvent event = new OrderPaidEvent(this, orderNo, userId, amount);
eventPublisher.publishEvent(event);
```

### 4. 监听事件

```java
@Async("taskExecutor")
@EventListener
public void onOrderPaid(OrderPaidEvent event) {
    // 发送短信
}
```

---

## 八股速记

1. **为什么需要 @EnableAsync？**
   - 只有开启后，Spring 才会为 @Async 方法生成异步代理。

2. **默认线程池有什么坑？**
   - `SimpleAsyncExecutor` 每次创建新线程，不控制并发，生产环境必须替换。

3. **线程池核心参数怎么配？**
   - CPU 密集型：core ≈ CPU 核数 + 1
   - IO 密集型：core ≈ CPU 核数 * 2，或更大

4. **CallerRunsPolicy 是什么？**
   - 拒绝策略之一：让调用线程自己执行任务，保证不丢任务但会降低吞吐量。

5. **Spring Event 如何感知事务？**
   - 默认 `@EventListener` 不感知事务；使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 可在事务提交后触发。

6. **@Async 方法异常怎么处理？**
   - 异常不会抛给调用方，需实现 `AsyncUncaughtExceptionHandler` 统一捕获。

---

## 测试

```bash
mvn test
```

- `OrderServiceTest`：验证异步事件产生 3 条通知日志
- `OrderControllerTest`：验证接口链路

---

## 作者

我
