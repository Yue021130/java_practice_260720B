# 第32章：Vue3 + SpringBoot 前后端分离实战（Servlet Filter 过滤器链）

## 一、业务背景

Servlet Filter 是 Java Web 中最经典的横切扩展点：请求进入 Servlet 容器后、到达 Spring MVC 之前，会依次经过一条**过滤器链（Filter Chain）**。链上的每个 Filter 都可以对请求做前置处理、决定是否放行，并在响应返回时做后置清理。Filter 的注册方式有三种（`@WebFilter`、`@Component`、`FilterRegistrationBean`），生命周期由容器管理（`init → doFilter → destroy`），且天然支持在 Filter 层完成鉴权——这是 Interceptor 做不到的（拦截器在 DispatcherServlet 内部，未登录请求根本到不了它）。

本章以「公告管理系统」为业务载体，手工搭建一条 **5 个 Filter 组成的过滤器链**：TraceId 全链路日志 → 统一编码 → XSS 清洗 → 请求计时统计 → Filter 层登录鉴权，并把每一次请求的链路日志写入 `sys_request_log`，前端可实时查看——包括鉴权失败的 401 请求（这正是 Filter 比 Interceptor 更靠外层的直接证据）。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，无状态模式 + SaServletFilter 在 Filter 层鉴权） |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
32-filter-chain-practice/
├── pom.xml
├── docs/
│   └── sql/
│       └── schema.sql                   # MySQL 8.0 建库建表 + 初始化数据
├── src/main/java/com/example/fcp/
│   ├── FcpApplication.java              # 启动类（@MapperScan + @ServletComponentScan）
│   ├── common/
│   │   ├── Result.java                  # 统一返回结果 {code, msg, data}
│   │   ├── BusinessException.java       # 业务异常
│   │   └── GlobalExceptionHandler.java  # 全局异常
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域
│   │   ├── Knife4jConfig.java           # OpenAPI3 文档 Bean
│   │   ├── MyBatisPlusConfig.java       # 分页插件
│   │   ├── SaTokenConfig.java           # JWT 无状态 StpLogic
│   │   └── FilterConfig.java            # 【核心】集中注册 Encoding/Timing/SaServlet 三个 Filter
│   ├── filter/                          # 过滤器链主体
│   │   ├── TraceIdFilter.java           # ① @Component+@Order(1)：MDC 写入 traceId，全链路透传
│   │   ├── EncodingFilter.java          # ② FilterRegistrationBean(order=2)：统一 UTF-8 编码（演示 init-param）
│   │   ├── XssFilter.java               # ③ @WebFilter：包装 Request，做 XSS 清洗（@ServletComponentScan 扫描）
│   │   ├── XssHttpServletRequestWrapper.java  # 继承可重复读取包装器，重写取值方法做转义
│   │   ├── XssUtil.java                 # HTML 转义工具（< > & " ' ( )）
│   │   ├── RepeatedlyReadHttpServletRequestWrapper.java  # 可重复读取 body 的包装器
│   │   └── TimingFilter.java            # ④ OncePerRequestFilter(order=10)：记录 URI/IP/状态码/耗时落库
│   ├── controller/
│   │   ├── AuthController.java          # 登录/登出/当前用户
│   │   ├── NoticeController.java        # 公告 CRUD（/api/notice）
│   │   └── RequestLogController.java    # 链路日志分页查询（/api/request-log/page）
│   ├── dto/  entity/  mapper/  service/
│   └── util/PasswordUtil.java           # BCrypt 密码哈希
├── src/main/resources/application.yml   # 含 logging.pattern.console 的 %X{traceId:-} 日志格式
└── web/                                 # 前端
    ├── index.html / package.json / vite.config.js
    └── src/
        ├── main.js / App.vue
        ├── router/index.js              # 路由 + 登录守卫
        ├── stores/user.js               # Pinia：token / userInfo
        ├── api/request.js               # axios：Token 注入、Result 解包、401 跳转
        ├── api/auth.js / notice.js / requestLog.js
        ├── layout/Index.vue
        └── views/Login.vue / Notice.vue / ChainLog.vue
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**（Knife4j）。
> 除登录外均需携带 `Authorization: <token>` 请求头。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT token | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前登录用户信息 | 登录 |
| GET | /api/notice/page | 公告分页列表 | 登录 |
| GET | /api/notice/{id} | 公告详情 | 登录 |
| POST | /api/notice | 新增公告 | 登录 |
| PUT | /api/notice | 编辑公告 | 登录 |
| DELETE | /api/notice/{id} | 逻辑删除 | 登录 |
| GET | /api/request-log/page | 链路日志分页（由 TimingFilter 写入，含 401） | 登录 |

## 五、运行方式

前置：本机已安装 MySQL 8.0，且 8080/5173 端口未被占用。
**先修改 `src/main/resources/application.yml` 中的数据库用户名密码为你本机 MySQL 的实际凭据。**

```bash
# 1. 建库建表
mysql -u root -p < docs/sql/schema.sql

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端（另开终端）
cd web && npm install && npm run dev
```

- 前端：http://localhost:5173 ，默认账号 **admin / 123456**
- 接口文档：http://localhost:8080/doc.html

## 六、核心场景说明：一条 5 个 Filter 的过滤器链

本章刻意把 5 个 Filter 分别用**三种不同方式**注册，以便对比。链上执行顺序（order 越小越靠前）：

| 顺序 | Filter | 注册方式 | 职责 |
| --- | --- | --- | --- |
| ① | TraceIdFilter | `@Component` + `@Order(1)` | 生成/透传 traceId 写入 SLF4J MDC，日志 pattern 配 `%X{traceId:-}` 后每条日志自动带链路 ID |
| ② | EncodingFilter | `FilterRegistrationBean`（order=2） | 统一请求/响应 UTF-8 编码，演示 `init-param` 配置读取 |
| ③ | XssFilter | `@WebFilter` + 启动类 `@ServletComponentScan` | 包装 Request，对参数和 body 做 HTML 转义，防 XSS 注入 |
| ④ | TimingFilter | `OncePerRequestFilter`（order=10） | try/finally 统计耗时，finally 中记录状态码落库 `sys_request_log` |
| ⑤ | SaServletFilter | `Sa-Token` 提供（order=20） | Filter 层登录鉴权，未登录由 `setError` 直接写回 401 JSON |

要点：

- **包绕模型（Filter Chain）**：每个 Filter 的 `doFilter` 中 `chain.doFilter()` 之前的代码是前置逻辑、之后的是后置逻辑；请求像穿过一层层"洋葱"，进入与返回各经过一次。`TimingFilter` 用 try/finally 包住 `chain.doFilter()`，保证即使下游鉴权失败抛异常，也能记录到 401。
- **Filter 层鉴权**：`SaServletFilter` 位于 TimingFilter 之后、Controller 之前，未登录请求在这里就被拦截写回 401，**不会**进入 Controller。因此 TimingFilter 能记录鉴权失败的请求——而 Interceptor 做不到（见 7.3）。
- **XSS 清洗**：`XssFilter` 把 Request 包装成 `XssHttpServletRequestWrapper`（继承可重复读取包装器），重写 `getParameter / getParameterValues / getParameterMap / getReader` 统一走 `XssUtil.clean()` 转义。body 中 JSON 字符串值里的 `< >` 被转义为 `&lt; &gt;` 不影响 Jackson 解析，却能在渲染时防注入。
- **TraceId 透传**：`TraceIdFilter` 优先取请求头 `X-Trace-Id`（网关/上游传入则透传），否则生成 UUID 短串，写入 MDC 并回写到响应头，前端「链路日志」页可直接按 traceId 检索。

## 七、八股速记

**7.1 Filter 的三种注册方式对比**

| 注册方式 | 优点 | 缺点 | 适用场景 |
| --- | --- | --- | --- |
| `@WebFilter` + `@ServletComponentScan` | 声明式、简洁 | **无法指定 order**，顺序不可控；不能精细配置 urlPatterns 之外的行为 | 简单独立 Filter |
| `@Component` | 纳入 Spring 容器，可注入 Bean | 默认作用于所有请求；order 需配合 `@Order` 注解 | 需要依赖注入的简单 Filter |
| `FilterRegistrationBean` | 可精确配置 order、urlPatterns、init-params、是否注册 | 样板代码多 | 生产首选，需精细控制时 |

> 坑：`@Component` 与 `FilterRegistrationBean` 重复注册同一个 Filter 会导致**执行两次**，本章 TimingFilter/EncodingFilter 只通过 `FilterConfig` 注册，绝不标注 `@Component`。

**7.2 Filter 生命周期与过滤器链**

- 生命周期：`init()`（容器启动时调用一次）→ 每次请求 `doFilter()` → `destroy()`（容器销毁时调用一次）。
- 过滤器链：多个 Filter 按 order 顺序串成链，`doFilter(request, response, chain)` 中调用 `chain.doFilter()` 表示放行给下一个 Filter 或目标 Servlet；不调用即拦截（可自行写响应）。
- 执行模型是**包绕（洋葱）模型**：Filter1 前置 → Filter2 前置 → Servlet → Filter2 后置 → Filter1 后置。

**7.3 Filter vs Interceptor（本章实证差异）**

| 维度 | Filter | Interceptor |
| --- | --- | --- |
| 规范 | Servlet 规范 | Spring MVC 规范 |
| 执行位置 | Servlet 容器最前端 | DispatcherServlet 内部 |
| 能否拦截未登录请求 | **能**，可在 Filter 层鉴权 | 不能——请求未到达 Handler |
| 能否拿到 Controller 方法 | 不能 | 能，`handler` 参数是 HandlerMethod |
| 典型用途 | 编码、XSS、TraceId、Filter 层鉴权 | 登录校验、权限注解、审计、限流 |

本章的实证：`SaServletFilter` 在 Filter 层把未登录请求挡在 Controller 外返回 401，位于它前面的 `TimingFilter` 用 finally 记录下了这些 401 到 `sys_request_log`；而第31章的 `LogInterceptor` 只能记录通过登录校验的请求。打开前端「链路日志」页，清掉 token 后随便发一个请求，就能看到一条 401 记录——这是 Filter 比 Interceptor 更靠外层的直接证据。

**7.4 OncePerRequestFilter 解决什么问题**

Servlet 容器在一次外部请求中只会调用 Filter 一次，但**请求转发（RequestDispatcher.forward）**会导致 Filter 被再次触发。继承 `OncePerRequestFilter`（Spring 提供）后，它内部根据请求属性标记保证同一请求只执行一次 `doFilterInternal`，避免 TraceId 重复生成、耗时重复统计等问题。

**7.5 RequestWrapper 的两种典型用途**

1. **可重复读取 body**：默认 `getInputStream()` 只能读一次，前置 Filter 读完 Controller 就拿不到了。缓存 body 到字节数组，重写 `getInputStream()/getReader()` 返回新流（本章 `RepeatedlyReadHttpServletRequestWrapper`）。
2. **改写请求内容**：重写 `getParameter*`、`getHeader` 等返回值，在不改 Controller 的情况下实现参数清洗/脱敏（本章 `XssHttpServletRequestWrapper` 继承前者，同时获得两种能力）。

**7.6 SaServletFilter 在 Filter 层鉴权**

Sa-Token 提供 `SaServletFilter`，`setAuth()` 中可以用 `SaRouter.match("/**").check(r -> StpUtil.checkLogin())` 声明拦截规则，`setError()` 中统一返回未登录 JSON。相比在 Interceptor 中鉴权，它更靠前、覆盖所有请求（含静态资源），且能配合 Filter 链完成"先计时统计、后鉴权拦截"的编排。

## 八、测试验证

```bash
# 后端单元测试（XssUtil 转义断言 + PasswordUtil）
mvn test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
# 1. 不带 token 访问受保护接口：返回 401，且这条 401 已被 TimingFilter 记录
curl -s -i http://localhost:8080/api/notice/page
# 前端「链路日志」页刷新，可看到 statusCode=401 的记录

# 2. 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. 新增一条带 XSS 载荷的公告，观察后端日志与链路日志
curl -s -X POST http://localhost:8080/api/notice \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"<b>测试</b>","type":1,"status":1,"content":"<script>alert(1)</script>"}'

# 4. 查看链路日志（traceId 列与控制台日志中的 [traceId] 一致）
curl -s "http://localhost:8080/api/request-log/page" -H "Authorization: $TOKEN"
```

同时观察后端控制台：每条日志都带有 `[traceId]`，新增公告入库后 content 已被转义为 `&lt;script&gt;` 形式。

## 九、作者

Yue021130 <169446203@qq.com>
