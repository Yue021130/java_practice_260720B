# 自定义注解深度教程：注解到底能干什么？本质是什么？有哪些使用场景？

> 本教程是 `04-mybatis-plus-practice` 项目的补充阅读，配合 `annotation/AutoFillUser.java` 与 `aspect/` 下的两个切面一起看，效果最好。

---

## 1. 一句话本质

**注解（Annotation）就是“贴在代码上的元数据标签”。**

它本身不会执行任何逻辑。注解真正的价值在于：**被工具、框架或运行时程序读取后，做出相应处理**。

你可以把注解理解为「**声明式标记**」：

- 告诉编译器：“这个类我重写了父类方法”（`@Override`）
- 告诉框架：“这个类是控制器”（`@Controller`）
- 告诉 AOP：“执行到这个方法时帮我记日志 / 填字段 / 做校验”（自定义注解）

---

## 2. 自定义注解的语法

```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)              // 能用在哪里
@Retention(RetentionPolicy.RUNTIME)      // 保留到运行时
@Documented                              // 会出现在 javadoc 中
public @interface AutoFillUser {
    String value() default "system";     // 注解属性，带默认值
}
```

### 2.1 元注解一览

| 元注解 | 作用 |
| --- | --- |
| `@Target` | 注解可以贴在哪些元素上：`TYPE`（类/接口）、`METHOD`、`FIELD`、`PARAMETER`、`CONSTRUCTOR`、`LOCAL_VARIABLE`、`ANNOTATION_TYPE`、`PACKAGE`、`TYPE_PARAMETER`、`TYPE_USE`、`MODULE`、`RECORD_COMPONENT` |
| `@Retention` | 注解生命周期：`SOURCE`（源码级，编译后丢弃）、`CLASS`（默认，保留在字节码但 JVM 运行期不可见）、`RUNTIME`（运行期可通过反射读取） |
| `@Documented` | 生成 javadoc 时是否包含该注解 |
| `@Inherited` | 是否允许子类继承父类上的类级别注解 |
| `@Repeatable` | 是否允许在同一元素上重复标注（需配合容器注解） |

### 2.2 注解属性规则

- 属性类型只能是：**基本类型、String、Class、枚举、其他注解、以上类型的数组**
- 属性名一般叫 `value()`，使用时可以省略：`@AutoFillUser("admin")`
- 属性可以有默认值：`String value() default "system";`
- 数组属性赋值：`@Role({"admin", "user"})`，单元素时可省略花括号

---

## 3. 注解到底能干什么？

注解的“能力”来自**消费者**，常见消费者有三类：

### 3.1 编译器 / 源码工具

- `@Override`：编译器检查是否真的重写了方法
- `@Deprecated`：编译器给出弃用警告
- `@SuppressWarnings`：压制警告
- **Annotation Processor**：在编译时扫描注解，生成代码（Lombok、MapStruct、Dagger 都是这个思路）

### 3.2 运行时反射

程序启动后通过反射读取注解，然后决定行为。

```java
Method method = clazz.getDeclaredMethod("xxx");
AutoFillUser anno = method.getAnnotation(AutoFillUser.class);
if (anno != null) {
    String operator = anno.value();
    // 根据 operator 做填充、校验、日志……
}
```

### 3.3 框架 AOP 拦截

Spring AOP 里最常见的用法：把自定义注解当成切点。

```java
@Aspect
@Component
public class LogAspect {

    @Around("@annotation(autoFillUser)")
    public Object around(ProceedingJoinPoint pjp, AutoFillUser autoFillUser) throws Throwable {
        System.out.println("方法执行前，操作人=" + autoFillUser.value());
        Object result = pjp.proceed();
        System.out.println("方法执行后");
        return result;
    }
}
```

---

## 4. 自定义注解的典型使用场景

### 4.1 操作日志

```java
@LogOperation(module = "订单", action = "下单")
public void createOrder(Order order) { ... }
```

AOP 拦截后记录：谁、什么时候、做了什么、参数、返回值、耗时。

### 4.2 数据权限 / 接口鉴权

```java
@RequireRole("admin")
public void deleteUser(Long userId) { ... }
```

进入方法前校验当前用户角色。

### 4.3 参数校验增强

`@NotNull`、`@Size` 是 JSR-303 的标准注解；你也可以写业务校验注解：

```java
@Phone
private String phone;
```

### 4.4 自动填充字段

本项目 `AutoFillUser` 就是典型：方法上贴注解，AOP 自动把当前操作人写入 `createBy` / `updateBy`。

### 4.5 幂等 / 防重提交

```java
@Idempotent(key = "order:#{userId}", expire = 10)
public void submitOrder(Long userId) { ... }
```

AOP 先用 Redis 锁拦截重复请求。

### 4.6 限流

```java
@RateLimit(qps = 100)
public String hotApi() { ... }
```

### 4.7 多数据源 / 动态路由

```java
@DataSource("slave")
public List<User> listUsers() { ... }
```

AOP 切换当前线程的数据源 key。

### 4.8 缓存

```java
@Cacheable(key = "user:#{id}")
public User getById(Long id) { ... }
```

Spring Cache 的底层就是注解 + AOP。

### 4.9 测试增强

JUnit 的 `@Test`、`@ParameterizedTest`、`@Disabled` 都是自定义注解。

### 4.10 编译时代码生成

Lombok 的 `@Data`、`@Accessors` 在编译期通过 Annotation Processor 修改 AST，帮你生成 getter/setter。

---

## 5. 实战：从零写一个“计时”注解

### 5.1 定义注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
    String name() default "";
}
```

### 5.2 AOP 切面

```java
@Aspect
@Component
public class TimedAspect {

    @Around("@annotation(timed)")
    public Object around(ProceedingJoinPoint pjp, Timed timed) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long cost = System.currentTimeMillis() - start;
        System.out.printf("[%s] 耗时 %d ms%n", timed.name(), cost);
        return result;
    }
}
```

### 5.3 使用

```java
@Service
public class DemoService {

    @Timed(name = " heavyJob ")
    public void heavyJob() {
        // 业务逻辑
    }
}
```

---

## 6. 本项目中的自定义注解实战

项目已实现的 `@AutoFillUser` 完整链路：

1. **定义注解** `annotation/AutoFillUser.java`
2. **业务方法贴注解** `service/impl/TaskServiceImpl.java`
   ```java
   @AutoFillUser
   public Task createTask(String content) { ... }
   ```
3. **AOP 拦截并处理** `aspect/AutoFillUserAspect.java`
   - `@Before("@annotation(autoFillUser)")` 进入方法前执行
   - 从请求上下文获取当前用户
   - 反射设置实体对象的 `createBy` / `updateBy`
4. **多个切面排序** `aspect/ValidationAspect.java` 使用 `@Order(10)`，`AutoFillUserAspect` 使用 `@Order(20)`，保证先校验、后填充。

---

## 7. 常见坑与面试八股

### 7.1 为什么自定义注解写了，AOP 却没生效？

- 检查 `@Retention` 是否为 `RUNTIME`
- 检查类是否交给 Spring 管理（`@Component` / `@Service` 等）
- 检查是否同类内部自调用：`this.createTask()` 不会走 Spring 代理
- 检查 `@Target` 是否匹配你贴的位置

### 7.2 `@Retention` 该选哪个？

| 策略 | 使用场景 |
| --- | --- |
| `SOURCE` | 给编译器或 IDE 看的，例如 `@Override` |
| `CLASS` | 字节码工具、APM 探针、字节码增强 |
| `RUNTIME` | 需要反射读取，例如自定义 AOP、框架扫描 |

### 7.3 `@Inherited` 的局限

- 只对**类上的注解**生效
- 对方法、字段上的注解不生效
- 只会继承直接父类，不会继承接口

### 7.4 反射读取注解的方法

```java
// 类
MyAnno anno = clazz.getAnnotation(MyAnno.class);

// 方法
Method m = clazz.getMethod("foo");
MyAnno anno = m.getAnnotation(MyAnno.class);

// 字段
Field f = clazz.getDeclaredField("name");
MyAnno anno = f.getAnnotation(MyAnno.class);

// 参数
Parameter[] params = m.getParameters();
```

### 7.5 注解是接口吗？

是的。所有注解类型都隐式继承 `java.lang.annotation.Annotation`。你不能让一个注解继承另一个注解，但可以通过**组合注解**（一个注解上贴另一个注解）达到类似效果。

---

## 8. 总结

- **注解的本质**：代码元数据标签，本身不执行逻辑。
- **注解的能力**：来自消费者——编译器、Annotation Processor、反射、AOP、框架。
- **最常见的自定义注解用法**：定义 → 贴到目标 → AOP/反射读取 → 执行横切逻辑。
- **核心记忆点**：`@Target` 决定贴哪里，`@Retention` 决定活多久，`@interface` 决定它是什么。

---

## 9. 延伸阅读

- Java 官方文档：[Annotations](https://docs.oracle.com/javase/tutorial/java/annotations/)
- Spring AOP 切点表达式：[AspectJ](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-pointcuts)
- 项目源码：
  - `04-mybatis-plus-practice/src/main/java/com/example/mp/annotation/AutoFillUser.java`
  - `04-mybatis-plus-practice/src/main/java/com/example/mp/aspect/AutoFillUserAspect.java`
  - `04-mybatis-plus-practice/src/main/java/com/example/mp/aspect/ValidationAspect.java`
