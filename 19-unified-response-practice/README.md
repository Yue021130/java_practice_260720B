# 19-unified-response-practice：Spring Boot 统一返回结果封装实战

「java高级知识」系列第 19 个专题。本模块完整落地微信公众号文章《Spring Boot 统一返回结果封装》中的全套方案：
**Result 结构体、ResultCode 枚举、ResultFactory 静态工厂、PageResult 分页封装、`ResponseBodyAdvice` 全局自动包装、
`@IgnoreResultWrap` 特例、全局异常处理、BusinessException 业务异常、参数校验、前端 axios 统一拦截器、
String 返回值陷阱、文件下载跳过包装、Swagger 文档适配**。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0 + validation，端口 **8099**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5192**
- 数据：全部内存 Mock，不需要 Redis / 数据库，`mvn spring-boot:run` 即跑

## 项目目标

1. 掌握统一返回结果封装的完整技术栈与落地细节。
2. 理解 `ResponseBodyAdvice` 的工作时机与边界条件。
3. 识别并规避 5 大生产坑点：Result 套 Result、String 变字符串 JSON、文件下载被包装、异常结构不一致、Swagger 显示裸类型。
4. 通过前后端完整闭环 + 自动化测试，验证每种场景的正确行为。

## 模块结构

```
19-unified-response-practice/
├── pom.xml
├── README.md
├── .gitignore
├── src/main/resources/application.yml
├── src/main/java/com/example/ur/
│   ├── UnifiedResponsePracticeApplication.java
│   ├── common/
│   │   ├── result/
│   │   │   ├── Result.java                 # 统一响应体：code/msg/data/timestamp
│   │   │   ├── ResultCode.java             # 业务状态码枚举
│   │   │   ├── ResultFactory.java          # 静态工厂方法
│   │   │   ├── PageResult.java             # 分页结果统一封装
│   │   │   └── BusinessException.java      # 业务异常
│   │   ├── advice/
│   │   │   ├── GlobalResponseAdvice.java   # ResponseBodyAdvice 全局自动包装
│   │   │   ├── GlobalExceptionHandler.java # 全局异常处理器
│   │   │   └── IgnoreResultWrap.java       # 跳过包装注解
│   │   └── config/
│   │       ├── OpenApiConfig.java          # Swagger 配置
│   │       ├── CorsConfig.java             # 跨域
│   │       └── WebConfig.java              # MessageConverter 顺序调整
│   ├── domain/
│   │   └── User.java                       # 用户实体（含 @Valid 校验注解）
│   ├── vo/
│   │   └── UserVO.java                     # 返回给前端的视图对象
│   ├── support/
│   │   └── MockUserRepository.java         # 内存 DAO + 分页支持
│   └── user/
│       ├── UserController.java             # 用户 CRUD、分页、校验、下载等示例接口
│       └── UserService.java                # 业务逻辑 + 主动抛 BusinessException
├── src/test/java/com/example/ur/
│   ├── ScenarioApiTest.java                # 全场景 MockMvc 接口覆盖
│   └── UnifiedResponseUnitTest.java        # Result/ResultFactory/PageResult 单元测试
└── web/                                    # Vue 3 前端面板
    ├── index.html
    ├── package.json
    ├── vite.config.js
    ├── src/main.js
    ├── src/api/request.js                  # axios 统一拦截器
    ├── src/App.vue
    └── src/style.css
```

## 快速启动

### 后端

```bash
cd 19-unified-response-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8099/swagger-ui/index.html

### 前端

```bash
cd 19-unified-response-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5192

### 运行测试

```bash
cd 19-unified-response-practice
mvn test
```

- `ScenarioApiTest`：MockMvc 覆盖全部接口，验证自动包装、校验失败、业务异常、String 返回值、文件下载等。
- `UnifiedResponseUnitTest`：验证 Result/ResultFactory/PageResult/BusinessException 行为。

## 接口速查

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/user/{id}` | GET | 查询单个用户，自动包装成 `Result<UserVO>` |
| `/api/user/list` | GET | 查询用户列表，自动包装成 `Result<List<UserVO>>` |
| `/api/user/page` | GET | 分页查询，自动包装成 `Result<PageResult<UserVO>>` |
| `/api/user/create` | POST | 创建用户，`@Valid` 校验失败返回 `code=400` |
| `/api/user/update` | POST | 更新用户，用户不存在抛 `BusinessException` |
| `/api/user/{id}` | DELETE | 删除用户 |
| `/api/user/manual-wrap/{id}` | GET | 手动返回 `Result`，验证不重复包装 |
| `/api/user/not-found` | GET | 主动抛业务异常，验证异常统一处理 |
| `/api/user/raw-string` | GET | 返回裸 String，验证自动包装后仍是 JSON |
| `/api/user/download` | GET | 文件下载，`@IgnoreResultWrap` 跳过包装 |
| `/api/user/explain` | GET | 八股速记 |

## 核心组件详解

### 1. Result 结构体

```java
public class Result<T> {
    private int code;        // 业务状态码：0 成功，非 0 失败
    private String msg;      // 提示信息
    private T data;          // 业务数据
    private long timestamp;  // 服务端响应时间戳
}
```

为什么用 `code=0` 表示成功而不是 HTTP 200？因为 HTTP 200 只表示"网络传输层没问题"，业务是否成功由 code 表达。
比如"登录过期"，HTTP 返回 200，业务 code 是 401，前端看到 code 就跳转登录页。

### 2. ResultCode 枚举

集中管理状态码，禁止在业务代码里散落魔法数字：

```java
public enum ResultCode {
    SUCCESS(0, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "暂未登录或登录已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");
}
```

### 3. ResultFactory 静态工厂

业务代码不需要自己 new Result：

```java
ResultFactory.success(data)
ResultFactory.failed(ResultCode.NOT_FOUND)
ResultFactory.failed("用户名已存在")
ResultFactory.failed(40001, "自定义错误")
```

### 4. PageResult 分页封装

字段名从一开始就和前端约定死：

```java
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;  // 自动计算
}
```

### 5. GlobalResponseAdvice 全局自动包装

实现 `ResponseBodyAdvice<Object>`，在 `beforeBodyWrite` 中：

1. 检查 `@IgnoreResultWrap`，跳过包装；
2. 检查 body 是否已经是 `Result` 或 `PageResult`，避免重复包装；
3. `void` / null 返回成功的空 Result；
4. String 类型手动 Jackson 序列化，解决 StringHttpMessageConverter 陷阱；
5. 其余统一包装成 `ResultFactory.success(body)`。

### 6. 全局异常处理

`@RestControllerAdvice` + `@ExceptionHandler`：

- `BusinessException`：返回携带 code 的 Result；
- `MethodArgumentNotValidException` / `BindException`：返回 `code=400`；
- `Exception`：兜底，返回 `code=50000`，并记录 error 日志。

## 前端 axios 拦截器

```javascript
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) return res.data
    if (res.code === 401) { window.location.href = '/login'; return Promise.reject(...) }
    alert(`请求失败 [${res.code}]: ${res.msg}`)
    return Promise.reject(new Error(res.msg))
  },
  (error) => { alert('网络异常'); return Promise.reject(error) }
)
```

拦截后业务代码只需：`const userList = await api.getUserList()`，不再需要 `res.data.data`。

## 面试八股

### 为什么要统一返回结果？

前后端协作的地基。统一返回体后，前端写一个拦截器就能处理所有接口的成功/失败/登录过期，
不需要为每个接口写不同的判断逻辑。

### Result 四个字段分别代表什么？

- code：业务状态码
- msg：提示信息
- data：业务数据
- timestamp：服务端响应时间戳，排查问题用

### 为什么用 code=0 表示成功，而不是 HTTP 200？

HTTP 200 表示网络层成功，业务层是否成功需要用 code 表达。这样网关、监控、浏览器都能保持 HTTP 语义，
前端又能在 body 里拿到细分的业务状态。

### ResponseBodyAdvice 的 supports / beforeBodyWrite 分别在什么时候执行？

- supports：判断当前 Controller 方法是否要走这个 Advice；
- beforeBodyWrite：在响应体写出前调用，返回值将作为真正写出的对象。

### 怎么避免 Result 套 Result？

在 `beforeBodyWrite` 里先判断 `body instanceof Result || body instanceof PageResult`，
是就直接返回。这个判断一定要放在最前面。

### String 返回值被自动包装后为什么会出问题？怎么解决？

Controller 返回 String 时，Spring 默认使用 `StringHttpMessageConverter`。
如果 ResponseBodyAdvice 把 String 包装成 Result 对象返回，StringHttpMessageConverter 会调用 toString()，
导致前端收到 `"Result(code=0, ...")` 这种字符串。

解决方案：
1. 在 `beforeBodyWrite` 中对 String 手动用 Jackson 序列化成 JSON 字符串返回；
2. 调整 MessageConverter 顺序，让 `MappingJackson2HttpMessageConverter` 优先。

### 文件下载接口为什么要跳过自动包装？怎么做？

下载接口返回 `byte[]` 或 `ResponseEntity<byte[]>`，如果被包装成 Result，前端拿到的就是 JSON 而不是文件流，
下载会损坏。用自定义注解 `@IgnoreResultWrap` 标注下载方法，GlobalResponseAdvice 检测到后直接跳过包装。

### 全局异常处理为什么要兜底 Exception？

保证任何异常都以统一的 Result 结构返回给前端，不暴露原始堆栈。兜底文案给前端，堆栈留给自己查日志。

### 分页接口返回字段为什么要和前端约定死？

前端分页组件（如 Element Plus）对 `total / current / size` 等字段名有默认要求，
后端字段名对不上，前端每次都要手动映射，容易出错。一开始就固化字段名。

### VO 隔离的意义是什么？

防止把实体里的敏感字段（密码、手机号）直接暴露给前端。Controller 返回 VO 而不是裸实体。

## 生产最佳实践清单

| 实践项 | 建议 |
| --- | --- |
| 状态码统一 | 用枚举管理，禁止魔法数字 |
| 成功码约定 | 与前端确认唯一成功值（如 0） |
| 时间戳字段 | 保留 timestamp，便于排查 |
| 包装开关 | 提供 `@IgnoreResultWrap` 特例 |
| 异常兜底 | 全局异常处理器返回 Result，不抛原始堆栈 |
| VO 隔离 | 分页/列表返回 VO，不直接返回实体 |
| 日志 | 异常必须记 error 日志（含堆栈） |
| 文档 | 接口文档展示包装后的结构 |
| 约定成文 | 把 code 语义表写进接口规范文档 |

## 推荐实验顺序

1. **01 基础返回**：观察单个 / 列表 / 分页接口如何被自动包装。
2. **02 参数校验**：提交非法表单，看 `code=400` 与 msg。
3. **03 业务异常**：触发 NOT_FOUND，看异常也走 Result。
4. **04 自动包装特例**：看 String 返回值是否仍是 JSON；看手动包装是否不套 Result。
5. **05 文件下载**：点击下载，验证 ResponseBodyAdvice 不拦截。
6. **06 八股速记**：通读核心考点与坑点清单。

## 参考

- [微信公众号原文：Spring Boot 统一返回结果封装](https://mp.weixin.qq.com/s/-LLYCtwzfwcYwyJOg46a5g)
- [Spring Boot 2.7 官方文档](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [Spring ResponseBodyAdvice 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-advice)
