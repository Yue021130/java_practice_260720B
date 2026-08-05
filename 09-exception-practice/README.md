# 09-exception-practice — Java 异常体系全场景实践

「java高级知识」系列第 9 个专题。把 Java 异常体系从 JVM 层级、语法糖、常见坑、Spring 全局处理、并发传播到工程最佳实践，全部包装成可运行的实验场景。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven，端口 **8089**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5182**

## 异常体系速查

```
java.lang.Throwable
├── java.lang.Error（unchecked，不应捕获）
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── NoClassDefFoundError
└── java.lang.Exception（checked-root）
    ├── RuntimeException（unchecked）
    │   ├── NullPointerException
    │   ├── ClassCastException
    │   ├── IllegalArgumentException / IllegalStateException
    │   ├── IndexOutOfBoundsException
    │   ├── ConcurrentModificationException
    │   ├── UnsupportedOperationException
    │   └── NoSuchElementException
    └── checked exception
        ├── IOException
        ├── SQLException
        └── ClassNotFoundException
```

## 全场景接口一览（7 大模块，30+ 实验）

### 01. 异常体系与分类（/api/hierarchy）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/hierarchy/family` | Throwable 家谱 |
| POST | `/api/hierarchy/checked-unchecked?checked=true` | checked vs unchecked |
| POST | `/api/hierarchy/custom-exception?throwWithCause=true` | 自定义 BusinessException |
| GET | `/api/hierarchy/when-to-use` | 选型建议 |

### 02. 异常基础语法（/api/basics）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/basics/execution-order?scenario=normal` | try-catch-finally 执行顺序 |
| POST | `/api/basics/finally-override?withReturn=true` | finally 覆盖 return/throw |
| POST | `/api/basics/try-with-resources?businessFail=false&closeFail=false` | try-with-resources |
| POST | `/api/basics/exception-chain` | 异常链 cause |
| POST | `/api/basics/mask-sensitive` | 异常脱敏 |
| GET | `/api/basics/finally-not-execute` | finally 不执行的极端情况 |

### 03. 常见异常场景（/api/common）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/common/npe` | NPE 防御 |
| POST | `/api/common/class-cast` | ClassCastException |
| POST | `/api/common/number-format` | NumberFormatException |
| POST | `/api/common/index-out-of-bounds` | IndexOutOfBoundsException |
| POST | `/api/common/cme` | ConcurrentModificationException |
| POST | `/api/common/uoe` | UnsupportedOperationException |
| POST | `/api/common/no-such-element` | NoSuchElementException |
| POST | `/api/common/stack-overflow` | StackOverflowError（受控） |
| GET | `/api/common/oom` | OutOfMemoryError 原理 |
| POST | `/api/common/class-not-found` | ClassNotFoundException vs NoClassDefFoundError |
| GET | `/api/common/assertion` | AssertionError |

### 04. 异常进阶特性（/api/advanced）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/advanced/multi-catch` | Java 7 多 catch |
| POST | `/api/advanced/rethrow` | 更精确重抛 |
| POST | `/api/advanced/lambda-checked` | Lambda 受检异常处理 |
| POST | `/api/advanced/stream-exception` | Stream 异常短路 |
| POST | `/api/advanced/suppressed` | Suppressed Exception |
| POST | `/api/advanced/exception-masking` | 异常屏蔽 |
| POST | `/api/advanced/performance` | 异常创建性能开销 |

### 05. Spring 全局异常处理（/api/spring）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/spring/business-error` | 业务异常 |
| GET | `/api/spring/error-code` | 业务错误码设计 |
| POST | `/api/spring/validation` | @RequestBody 参数校验异常 |
| GET | `/api/spring/validation-param?name=x` | @RequestParam 参数校验异常 |
| POST | `/api/spring/response-status` | ResponseStatusException |
| POST | `/api/spring/unknown-error` | 未知异常兜底 |

### 06. 并发中的异常（/api/concurrency）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/concurrency/thread-uncaught` | 子线程异常不抛主线程 |
| POST | `/api/concurrency/uncaught-handler` | UncaughtExceptionHandler |
| POST | `/api/concurrency/future-get` | Future.get 异常包装 |
| POST | `/api/concurrency/completable-exception` | CompletableFuture 异常处理 |
| POST | `/api/concurrency/async-exception` | @Async 异常 |
| POST | `/api/concurrency/pool-swallow` | 线程池 submit 吞异常 |

### 07. 最佳实践与反模式（/api/bestpractice）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/bestpractice/swallow` | 不要吞异常 |
| POST | `/api/bestpractice/flow-control` | 不要用异常做流程控制 |
| POST | `/api/bestpractice/fail-fast` | 早失败 |
| POST | `/api/bestpractice/logging` | 异常日志规范 |
| POST | `/api/bestpractice/transaction` | 事务与异常回滚 |

## 接口文档（Swagger UI）

项目集成了 SpringDoc OpenAPI（`springdoc-openapi-ui:1.7.0`，对应 Spring Boot 2.7），
接口文档根据代码中的 `@Tag` / `@Operation` 注解自动生成。

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8089/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8089/v3/api-docs

## 启动步骤

后端（进入 `09-exception-practice` 目录）：

```bash
mvn spring-boot:run
```

前端（另开一个终端）：

```bash
cd web
npm install
npm run dev
```

浏览器打开 http://localhost:5182 即可看到实验面板。

## 运行测试

```bash
mvn test
```

测试覆盖：

- `ExceptionPracticeApplicationTests`：Spring 上下文加载。
- `BasicsBehaviorTest`：finally 覆盖返回值、异常链保留 cause、try-with-resources 的 Suppressed 异常。
- `ScenarioApiTest`：MockMvc 调用全部实验接口，验证返回状态与数据结构。

## 推荐实验顺序

1. **异常体系**：先看 Throwable 家谱，建立 checked / unchecked 直觉。
2. **基础语法**：跑通 try-catch-finally 执行顺序、finally 覆盖 return、try-with-resources。
3. **常见异常**：逐个复现 NPE、CME、UOE 等高频异常，记住防御方式。
4. **进阶特性**：多 catch、Lambda checked、Stream 异常短路、Suppressed。
5. **Spring 全局处理**：理解 @ControllerAdvice、参数校验、业务错误码设计。
6. **并发异常**：重点理解子线程异常不传播、Future.get 的 ExecutionException、线程池 submit 吞异常。
7. **最佳实践**：对照反模式，形成工程化意识。

## 面试八股速记

### 异常体系

- **Error vs Exception**：Error 是 JVM 层面的严重错误（如 OOM、StackOverflow），通常不应捕获；Exception 是程序可处理的异常。
- **checked vs unchecked**：checked 编译期检查，方法签名必须声明或 try-catch（IOException/SQLException）；unchecked 继承 RuntimeException，不强制处理。
- **何时用 checked / unchecked**：可恢复场景用 checked；编程错误/无法恢复用 unchecked。Effective Java 推荐优先 unchecked，减少样板代码。

### try-catch-finally

- **finally 是否一定执行？** 正常情况下一定执行；但 `System.exit()`、JVM 崩溃、线程被强制终止时不会执行。
- **finally 中 return 会怎样？** 会覆盖 try/catch 中的 return，也会吞掉 catch 中的 throw，非常危险。
- **try-with-resources**：资源必须实现 AutoCloseable；按打开逆序关闭；close 异常被挂到业务异常的 `suppressed` 上。

### 常见异常

- **NPE**：自动拆箱 null、链式调用、Map.get 返回值、equals 反写。防御：Objects.requireNonNull、Optional、常量放前。
- **ClassCastException**：泛型擦除、强转前未 instanceof。
- **ConcurrentModificationException**：fail-fast 机制；for-each 中删除应使用 Iterator.remove()、removeIf 或倒序索引。
- **UnsupportedOperationException**：Arrays.asList、Collections.singletonList、unmodifiableList 不可修改。
- **StackOverflowError**：递归无终止、循环依赖初始化。
- **OutOfMemoryError**：堆 OOM、Metaspace OOM、堆外内存泄漏；是 Error，不要 catch 后假装正常。
- **ClassNotFoundException vs NoClassDefFoundError**：前者 checked，通常反射/Class.forName 触发；后者 Error，编译期存在运行期缺失。

### 进阶

- **多 catch**：Java 7 语法糖，catch 中的异常不能有继承关系。
- **精确重抛**：`catch(Exception e) { throw e; }` Java 7 后可保持原异常类型。
- **Lambda checked**：标准函数式接口不支持 checked exception，需内部 try-catch、包装为 unchecked 或自定义函数式接口。
- **Stream 异常**：中间操作异常会短路整个流水线；建议在 forEach 内单独捕获或提前过滤。
- **异常屏蔽**：catch 中抛新异常时务必传入 cause，否则原始异常丢失。
- **异常性能**：创建异常的主要开销在 `fillInStackTrace()`；高吞吐场景可重写，但会丢失堆栈。

### Spring

- **@ControllerAdvice + @ExceptionHandler**：统一处理 Controller 异常，返回统一结构。
- **ExceptionHandler 优先级**：精确匹配优先于父类匹配。
- **参数校验异常**：`@RequestBody` 触发 `MethodArgumentNotValidException`；`@RequestParam`/`@PathVariable` 触发 `ConstraintViolationException`。
- **事务回滚**：Spring 默认只对 RuntimeException / Error 回滚；checked exception 需 `@Transactional(rollbackFor = Exception.class)`。
- **业务错误码**：建议按“系统+模块+序号”分段，并映射到国际化 message key。

### 并发

- **子线程异常**：默认不会抛给主线程；需 `Thread.setDefaultUncaughtExceptionHandler` 统一处理。
- **Future.get**：任务异常被包装为 `ExecutionException`，实际异常在 cause 中；`InterruptedException` 需恢复中断标志。
- **CompletableFuture**：`exceptionally` 返回默认值；`handle` 统一处理两种结果；`whenComplete` 不吞异常。
- **线程池 submit vs execute**：submit 吞异常（异常在 Future 中），execute 会打印；submit 后务必处理 Future.get()。
- **@Async**：void 方法异常由 `AsyncUncaughtExceptionHandler` 捕获；有返回值时通过 Future 获取。

### 最佳实践

- **不要吞异常**：空 catch 会让问题无法定位。
- **不要用异常做流程控制**：异常创建开销大，应使用 if/for/return。
- **fail-fast**：入参校验前置，避免错误扩散。
- **日志规范**：`log.error("msg, params", exception)` 同时记录上下文与堆栈；不要只打 message，不要重复打印。
- **异常转换**：跨层调用建议把低层异常转换为业务异常并保留 cause；同层避免无意义 catch。
- **对外脱敏**：对外接口只返回错误码与提示，内部日志保留完整堆栈。

## 项目结构

```
09-exception-practice/
├── pom.xml
├── README.md
├── .gitignore
├── src/main/java/com/example/exception/
│   ├── ExceptionPracticeApplication.java
│   ├── common/        ApiResponse
│   ├── config/        CorsConfig、OpenApiConfig
│   ├── hierarchy/     异常体系与自定义异常
│   ├── basics/        try-catch-finally、try-with-resources、异常链
│   ├── commonex/      常见异常场景
│   ├── advanced/      多 catch、Lambda、Stream、Suppressed、性能
│   ├── spring/        全局异常处理、参数校验、业务错误码
│   ├── concurrency/   子线程异常、Future、CompletableFuture、线程池
│   └── bestpractice/  最佳实践与反模式
├── src/test/java/com/example/exception/
└── web/               Vue 3 + Vite 前端
```
