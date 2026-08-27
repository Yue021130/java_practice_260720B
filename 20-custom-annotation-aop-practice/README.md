# 20-custom-annotation-aop-practice：自定义注解 + AOP 高阶玩法实战

「java高级知识」系列第 20 个专题。本模块完整落地微信公众号文章《自定义注解 + AOP 才是高阶玩法，别再只会 Ctrl+C Ctrl+V 了》中的知识点：
内置注解、元注解、自定义注解 + AOP、常见框架注解（Lombok / Bean Validation）、重复注解 @Repeatable、注解生命周期与 APT 说明，
以及操作日志、权限校验、接口限流、数据脱敏、耗时监控等 5 个 AOP 实战场景。

本地已存放原文 HTML：`Java 开发 ＞ 自定义注解 ＋ AOP 才是高阶玩法，别再只会 Ctrl＋C Ctrl＋V 了.html`。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0 + `spring-boot-starter-aop`，端口 **8100**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5193**
- 数据：全部内存 Mock，不需要 Redis / 数据库，`mvn spring-boot:run` 即跑

## 项目目标

1. 掌握 Java 内置注解（@Override / @Deprecated / @SuppressWarnings）的正确用法。
2. 掌握元注解（@Target / @Retention / @Documented / @Inherited）与自定义注解定义。
3. 掌握 AOP 五种通知类型与使用场景，理解注解形参注入写法。
4. 理解 `@Aspect`、`@Component`、`@EnableAspectJAutoProxy` 的作用。
5. 识别 AOP 常见坑点：注解需要处理器、RUNTIME 反射性能、参数语义不清、自调用失效、`@Around` 忘记 `proceed()` 等。
6. 掌握 Java 8 `@Repeatable` 重复注解与容器注解的写法。
7. 通过前后端完整闭环 + 自动化测试验证每种切面行为。

## 模块结构

```
20-custom-annotation-aop-practice/
├── pom.xml
├── README.md
├── .gitignore
├── src/main/resources/application.yml
├── src/main/java/com/example/caa/
│   ├── CustomAnnotationAopPracticeApplication.java
│   ├── common/
│   │   ├── ApiResponse.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── OpenApiConfig.java
│   │   └── CorsConfig.java
│   ├── annotation/                  # 自定义注解定义
│   │   ├── LogOperation.java
│   │   ├── RequirePermission.java
│   │   ├── RateLimit.java
│   │   ├── DataMasking.java
│   │   ├── Timing.java
│   │   ├── InheritedMarker.java     # @Inherited 演示
│   │   ├── Audit.java               # @Repeatable 可重复注解
│   │   └── Audits.java              # @Repeatable 容器注解
│   ├── aspect/                      # AOP 切面实现
│   │   ├── LogOperationAspect.java
│   │   ├── PermissionAspect.java
│   │   ├── RateLimitAspect.java
│   │   ├── DataMaskingAspect.java
│   │   └── TimingAspect.java
│   ├── builtin/                     # 内置注解演示
│   │   ├── BuiltinAnnotationDemo.java
│   │   └── BuiltinAnnotationController.java
│   ├── inherited/                   # @Inherited 演示
│   │   ├── BaseAnnotatedService.java
│   │   ├── InheritedChildService.java
│   │   └── InheritedAnnotationController.java
│   ├── domain/
│   │   └── User.java                # 含 Bean Validation 约束
│   ├── support/
│   │   └── MockDataRepository.java
│   └── demo/
│       ├── DemoController.java
│       └── DemoService.java
├── src/test/java/com/example/caa/
│   ├── ScenarioApiTest.java
│   ├── AopBehaviorUnitTest.java
│   ├── BuiltinAnnotationUnitTest.java
│   ├── InheritedAnnotationUnitTest.java
│   ├── RepeatableAnnotationUnitTest.java
│   └── ValidationApiTest.java
└── web/                             # Vue 3 前端面板
    ├── index.html
    ├── package.json
    ├── vite.config.js
    ├── src/main.js
    ├── src/App.vue
    └── src/style.css
```

## 快速启动

### 后端

```bash
cd 20-custom-annotation-aop-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8100/swagger-ui/index.html

### 前端

```bash
cd 20-custom-annotation-aop-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5193

### 运行测试

```bash
cd 20-custom-annotation-aop-practice
mvn test
```

- `ScenarioApiTest`：MockMvc 覆盖全部接口，验证日志、权限、限流、脱敏、耗时、组合注解。
- `AopBehaviorUnitTest`：验证 DemoService 是否被 AOP 代理、注解元信息是否正确。

## 接口速查

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/demo/log?id=1` | GET | 操作日志示例 |
| `/api/demo/permission/admin` | GET | admin 权限校验，需 `X-Role=admin` |
| `/api/demo/permission/user` | GET | user:view 权限校验，需 `X-Role=admin/user` |
| `/api/demo/rate-limit` | GET | 接口限流，1 秒内最多 2 次 |
| `/api/demo/masking` | GET | 数据脱敏（单对象） |
| `/api/demo/masking-list` | GET | 数据脱敏（列表） |
| `/api/demo/timing` | GET | 耗时监控 |
| `/api/demo/combine` | GET | 组合注解：日志 + 权限 + 限流 + 耗时 |
| `/api/demo/error-log` | GET | 异常日志示例 |
| `/api/demo/builtin` | GET | 内置注解 @Override/@Deprecated/@SuppressWarnings 演示 |
| `/api/demo/inherited` | GET | @Inherited 元注解演示 |
| `/api/demo/audit` | GET | @Repeatable 重复注解演示 |
| `/api/demo/validate` | POST | @Valid 参数校验演示 |
| `/api/demo/explain` | GET | 八股速记 |

## 自定义注解一览

| 注解 | 作用 | 切面通知类型 |
| --- | --- | --- |
| `@LogOperation` | 记录方法名、参数、返回值、耗时、异常 | `@Around` |
| `@RequirePermission` | 方法执行前校验权限 | `@Before` |
| `@RateLimit` | 方法执行前做 QPS 限流 | `@Before` |
| `@DataMasking` | 方法返回后对敏感字段脱敏 | `@AfterReturning` |
| `@Timing` | 计算方法执行耗时并输出日志 | `@Around` |

## AOP 通知类型速查

| 通知类型 | 执行时机 | 特点 |
| --- | --- | --- |
| `@Before` | 目标方法执行前 | 无法阻止方法执行（除非抛异常） |
| `@After` | 目标方法执行后（无论是否异常） | 类似 finally |
| `@AfterReturning` | 目标方法正常返回后 | 可获取返回值 |
| `@AfterThrowing` | 目标方法抛出异常后 | 可获取异常对象 |
| `@Around` | 环绕目标方法 | 最强，可控制是否执行、修改返回值 |

## 面试八股

### 什么是自定义注解？如何定义？

自定义注解是 Java 的元数据机制，使用 `@interface` 定义。常用元注解：

- `@Target`：注解可以标注在哪些位置（类、方法、字段等）
- `@Retention`：注解保留到什么阶段（SOURCE / CLASS / RUNTIME）
- `@Documented`：是否包含在 Javadoc 中
- `@Inherited`：是否允许子类继承

AOP 场景必须声明 `@Retention(RetentionPolicy.RUNTIME)`，否则运行期反射拿不到。

### AOP 的五种通知类型有什么区别？

- `@Before`：方法执行前，适合做权限校验、参数校验；
- `@After`：方法执行后，无论是否异常，适合做资源清理；
- `@AfterReturning`：方法正常返回后，可处理返回值，适合做脱敏、日志；
- `@AfterThrowing`：方法抛异常后，适合做异常监控；
- `@Around`：环绕通知，最强，可控制方法是否执行、修改入参和返回值，适合做事务、日志、耗时。

### @Aspect / @Component / @EnableAspectJAutoProxy 的作用？

- `@Aspect`：声明这是一个切面类。
- `@Component`：把切面类交给 Spring 管理。
- `@EnableAspectJAutoProxy`：开启 AOP 自动代理。Spring Boot 引入 `spring-boot-starter-aop` 后会自动开启，无需手动标注。

### JoinPoint 和 ProceedingJoinPoint 的区别？

- `JoinPoint`：通用连接点，所有通知都能用，可获取方法签名、参数、目标对象等。
- `ProceedingJoinPoint`：继承自 JoinPoint，**只有 `@Around` 通知能用**，提供 `proceed()` 方法用于执行目标方法。

### JDK 动态代理 vs CGLIB 有什么区别？

- **JDK 动态代理**：基于接口生成代理，要求目标类实现接口；
- **CGLIB**：基于继承生成代理，可代理没有接口的类；不能代理 final 类和方法。

Spring Boot 2.x 默认 `spring.aop.proxy-target-class=true`，即优先使用 CGLIB。

### 同一个类中方法 A 调用方法 B，B 上的切面为什么不生效？

因为 AOP 代理对象外部调用才会走切面。类内部 `this.B()` 调用的是原对象，不是代理对象，所以 B 上的切面失效。

**解决方案**：
1. 把 B 抽到另一个 Bean；
2. 通过 `AopContext.currentProxy()` 获取当前代理对象再调用（需开启 `expose-proxy=true`）；
3. 注入自身代理对象调用。

### @Around 忘记调用 proceed() 会怎样？

目标方法不会执行，相当于把方法目标方法不会执行，相当于把方法"短路"了。这是 `@Around` 最常见的坑。

### 自定义注解 + AOP 适合做哪些事？不适合做哪些事？

**适合**：
- 日志审计、操作日志
- 权限校验、鉴权
- 接口限流（简单场景）
- 数据脱敏、字段加密
- 方法耗时监控
- 缓存开关、分布式锁开关

**不适合**：
- 复杂业务流程（切面逻辑过多会让代码难追踪）
- 需要强一致性的限流（单机内存限流不适合集群）
- 跨线程场景（AOP 代理不会传递到子线程）

### 接口限流用 AOP 实现有什么优缺点？

**优点**：
- 无侵入，加个注解即可；
- 逻辑集中，便于统一调整。

**缺点**：
- 单机内存限流无法跨进程共享；
- 重启后计数丢失；
- 集群部署时每个实例独立计数，限流失效。

**生产建议**：用 Redis + Lua 做分布式限流，或接入 Sentinel。

### 数据脱敏用 AOP 做有什么坑？

- 反射处理字段性能较低，大数据量时慎用；
- 嵌套对象、Map、泛型集合需要递归处理；
- 脱敏逻辑写在切面里，调用方可能不知道字段被改了，调试困难；
- 某些场景需要"不脱敏"（如导出），要提供开关。

### 操作日志用 AOP 做如何保证不阻塞主流程？

- 日志打印本身很快，但如果涉及数据库写入，建议异步处理；
- 可用 `@Async` + 线程池；
- 或发送 MQ，由消费者落库；
- 切面里只做"收集"，不做"持久化"。

## 推荐实验顺序

1. **01 操作日志**：触发 `/api/demo/log`，观察后端控制台 JSON 日志。
2. **02 权限校验**：切换 admin/user/anonymous 角色，看 200 vs 403。
3. **03 接口限流**：连续点击，看第 3 次是否触发 429。
4. **04 数据脱敏**：对比原始数据与返回数据中的 phone/email/idCard。
5. **05 耗时监控**：触发接口，看后端日志中的耗时输出。
6. **06 注解组合**：一个接口同时走多个切面。
7. **07 八股速记**：通读核心考点与坑点清单。

## 生产最佳实践清单

| 实践项 | 建议 |
| --- | --- |
| 注解设计 | 只做声明，逻辑交给切面 |
| 切面职责 | 单一职责，不要一个切面干所有事 |
| 代理问题 | 避免同类内部自调用导致切面失效 |
| 异常处理 | 切面抛异常要配合全局异常处理 |
| 限流 | 生产用 Redis/Sentinel，不用内存计数器 |
| 日志 | 不要阻塞主流程，异步或 MQ 落库 |
| 脱敏 | 提供开关，大数据量时考虑性能 |
| 测试 | 验证 Bean 是否被代理、注解是否正确 |

## 与微信公众号原文对照

本地已存放原文 HTML：`Java 开发 ＞ 自定义注解 ＋ AOP 才是高阶玩法，别再只会 Ctrl＋C Ctrl＋V 了.html`。

| 原文章节 | 核心观点 | 本地落地 |
| --- | --- | --- |
| 0. 注解到底是什么 | 注解是代码的元数据，把“做什么”和“怎么做”分离 | 5 个自定义注解只做声明，逻辑交给 5 个 AOP 切面 |
| 1. 三个内置注解 | @Override / @Deprecated / @SuppressWarnings 每天都要用 | `BuiltinAnnotationDemo.java`、`BuiltinAnnotationController.java` |
| 2. 元注解 | @Target / @Retention / @Documented / @Inherited 是自定义注解的基础 | 所有自定义注解都使用了前三个；`InheritedMarker.java` 演示 @Inherited |
| 3. 自定义注解 + 切面 | 推荐注解形参注入，避免全类名字符串和手动反射 | 所有切面都通过方法形参注入注解对象 |
| 4. 常见框架注解 | Spring Boot 核心注解、Lombok（APT）、Jakarta Bean Validation | 使用 Lombok；新增 `spring-boot-starter-validation`，`User.java` 加约束 |
| 5. 注解的三大坑 | 注解需要处理器；RUNTIME 反射有性能开销；参数语义要清晰 | 切面 Javadoc 补充提醒；`RateLimit` 用 TimeUnit 枚举明确语义 |
| 加餐一：@Repeatable | Java 8 允许同一注解多次使用，需定义容器注解 | `Audit.java` + `Audits.java` + `RepeatableAnnotationUnitTest.java` |
| 加餐二：APT 与生命周期 | SOURCE / CLASS / RUNTIME 区别；Lombok 用 APT | README 说明 + 代码注释 |
| 加餐三：注解与接口的区别 | 注解是描述，接口是行为契约 | README 面试八股补充 |
| 小结 | 自定义注解 + AOP 的核心要点清单 | 本 README 面试八股与生产最佳实践清单 |

## 参考

- 本地原文：`Java 开发 ＞ 自定义注解 ＋ AOP 才是高阶玩法，别再只会 Ctrl＋C Ctrl＋V 了.html`
- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [AspectJ 注解速查](https://www.eclipse.org/aspectj/doc/released/progguide/semantics-aspectj.html)
- [Spring Boot AOP Starter](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/io.html#io.aop)
