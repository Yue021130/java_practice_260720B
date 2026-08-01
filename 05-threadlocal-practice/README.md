# 05-threadlocal-practice —— ThreadLocal 全场景实践

「java高级知识」系列第 5 个专题。系统覆盖 **ThreadLocal 的使用场景、底层原理、生产踩坑与面试八股**，
把每个知识点包装成可运行的现实业务场景，配合 **Vue 3 + Vite** 前端面板，边跑边学。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0 + Alibaba TTL，端口 **8085**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5178**

## 全场景一览（4 大模块，12 个实验）

| 模块 | 场景 | 对应端点 | 面试考点 |
| --- | --- | --- | --- |
| 基础原理 | 线程隔离 | `POST /api/basic/isolation` | ThreadLocalMap 每个线程独立 |
| 基础原理 | initialValue / withInitial | `POST /api/basic/initial` | 懒加载、默认值 |
| Web 上下文 | Filter + ThreadLocal 传递用户 | `POST /api/web/user-context` | 请求级上下文、必须 remove |
| Web 上下文 | MDC 全链路 traceId | `POST /api/web/mdc-trace` | %X{traceId}、日志排查 |
| Web 上下文 | SimpleDateFormat 线程安全 | `POST /api/web/dateformat-safe` | 共享变量并发异常、ThreadLocal 解决 |
| 跨线程 | InheritableThreadLocal | `POST /api/cross/inheritable` | 子线程继承、线程池失效 |
| 跨线程 | 线程池串号 / 污染 | `POST /api/cross/pool-hazard` | 线程复用导致 value 残留 |
| 跨线程 | 线程池正确使用（remove） | `POST /api/cross/pool-remove` | try-finally remove |
| 跨线程 | CompletableFuture 上下文丢失 | `POST /api/cross/async-context` | 异步线程不继承 ThreadLocal |
| 跨线程 | TTL 线程池透传 | `POST /api/cross/ttl-propagation` | TtlExecutors、生产跨线程方案 |
| 进阶 | 内存泄漏原理 | `POST /api/advance/leak-analysis` | key 弱引用、value 强引用 |
| 进阶 | 最佳实践 | `POST /api/advance/best-practice` | static final、remove、避免大对象 |

## 模块面试点速记

### 基础原理

- **ThreadLocal 不是线程**：它是线程的局部变量，每个线程内部有一个 `ThreadLocalMap`。
- **ThreadLocalMap**：`Thread` 对象的成员变量，key 是 `ThreadLocal` 的弱引用，value 是实际存储的对象。
- **线程隔离**：同一个 `ThreadLocal` 实例在不同线程中对应不同的 value，互不干扰。
- **initialValue()**：首次 `get()` 且未 `set()` 时触发；`ThreadLocal.withInitial(Supplier)` 是 JDK 8 推荐写法。

### Web 上下文

- **Filter + ThreadLocal**：请求进入时 `set`，`finally` 中 `remove`，Service 层直接 `get()`。
- **必须 remove**：Web 容器使用线程池，忘记 remove 会导致后续请求读到上一个请求的数据（串号）。
- **MDC**：SLF4J 提供的日志上下文，底层也是 ThreadLocal；日志 pattern 用 `%X{traceId}` 输出 traceId。
- **SimpleDateFormat**：非线程安全，高并发下会抛异常或结果错乱；可用 `ThreadLocal<DateFormat>` 或 `DateTimeFormatter`。

### 跨线程

- **InheritableThreadLocal**：创建 `new Thread()` 时，子线程会拷贝父线程的值；**线程池不生效**。
- **线程池污染**：线程池复用线程，如果任务 A set 后未 remove，任务 B 可能读到 A 的残留值。
- **CompletableFuture**：默认 `ForkJoinPool` 不会继承主线程的 ThreadLocal。
- **TTL（TransmittableThreadLocal）**：Alibaba 提供的增强 ThreadLocal，`TtlExecutors.getTtlExecutorService()` 包装线程池后可自动跨线程池透传上下文。

### 进阶

- **内存泄漏**：`ThreadLocalMap.Entry` 的 key 是 `ThreadLocal` 的弱引用，value 是强引用；线程池线程长期存活时，若未 remove，value 无法回收。
- **最佳实践**：
  1. 声明为 `private static final`
  2. 在 `try ... finally` 中 `remove()`
  3. 线程池任务必须 remove
  4. value 避免大对象
  5. 跨线程池优先使用 TTL

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

浏览器打开 http://localhost:5178 即可看到实验面板。

## 接口文档（Swagger UI）

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8085/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8085/v3/api-docs

## 运行测试

后端自带集成测试（JUnit 5 + MockMvc + AssertJ）：

```bash
mvn test
```

- `ScenarioApiTest`：通过 MockMvc 调用全部 12 个场景接口，验证均返回 200 且 data 非空。

## 推荐实验顺序

1. **基础原理**：线程隔离、initialValue。
2. **Web 上下文**：Filter 传用户、MDC traceId、SimpleDateFormat 线程安全。
3. **跨线程**：InheritableThreadLocal、线程池串号、正确使用 remove、CompletableFuture 丢失、TTL 透传。
4. **进阶**：内存泄漏原理、最佳实践。

## 项目结构

```
05-threadlocal-practice/
├── pom.xml
├── README.md
├── src/main/resources/application.yml
├── src/main/java/com/example/tl/
│   ├── ThreadLocalPracticeApplication.java
│   ├── common/ApiResponse
│   ├── context/       UserContext、TraceContext、InheritableContext、TtlContext、DateFormatHolder
│   ├── filter/        UserContextFilter
│   ├── config/        WebConfig（ServletComponentScan）、ThreadPoolConfig
│   ├── basic/         BasicController/Service
│   ├── webctx/        WebContextController/Service
│   ├── cross/         CrossThreadController/Service
│   └── advance/       AdvanceController/Service
├── src/test/java/com/example/tl/ScenarioApiTest.java
└── web/               Vue 3 + Vite 前端
```
