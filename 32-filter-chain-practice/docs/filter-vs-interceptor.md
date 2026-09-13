# Spring Boot 过滤器（Filter）vs 拦截器（Interceptor）对比（修订版）

> 本文档对照第 31 章（Interceptor 为主）与第 32 章（Filter 链为主）的实际代码修订，
> 所有改动处用 HTML 注释标注了原因，文末附修订摘要。

## 一、核心区别速览

| 维度 | 过滤器（Filter） | 拦截器（Interceptor） |
|------|------------------|----------------------|
| **所属规范** | Servlet 标准 | Spring MVC |
| **执行时机** | 请求进入 Servlet 容器后，到达 `DispatcherServlet` 之前 | 到达 `DispatcherServlet` 之后，Controller 方法前后 |
| **能否注入 Spring Bean** | ⚠️ 看注册方式：仅 `@WebFilter` 方式不能注入；`@Component` / `FilterRegistrationBean` 方式**可以** | ✅ 可以 |
| **处理范围** | 所有请求（包括静态资源、错误页面、被鉴权拦截的请求） | 仅到达 Handler 的 Spring MVC 请求 |
| **操作粒度** | 对 `HttpServletRequest/Response` 做底层处理，可操作原始流 | 对 `HandlerMethod` 做业务层处理，可读方法注解 |

<!-- 修改原因：原表写「Filter ❌ 不能直接注入 Spring Bean」，这是常见误区。只有 @WebFilter + @ServletComponentScan 方式注册的 Filter 由 Servlet 容器直接实例化、不在 Spring 容器里，才无法注入；@Component 方式本身就是 Bean，FilterRegistrationBean 方式由 Spring 创建，构造器注入完全可用——32 章 TimingFilter.java:37 就是构造器注入 RequestLogService 反例。DelegatingFilterProxy 是把 Spring Bean 挂进 Servlet 容器的桥，不是 Filter 注入 Bean 的唯一出路。 -->

---

## 二、两条链的衔接关系（新增）

一次请求的完整经过：

```
客户端
  │
  ▼
┌─ Filter 链（包绕模型，洋葱式）─────────────┐
│ TraceIdFilter 前置                          │
│   EncodingFilter 前置                       │
│     XssFilter 前置                          │
│       TimingFilter 前置                     │
│         SaServletFilter（鉴权，可在此中断）  │
│           ▼                                 │
│         DispatcherServlet                   │
│           ▼                                 │
│         Interceptor 链                      │
│           preHandle ① → ② → ③（顺序执行）  │
│           Controller → Service → Mapper     │
│           postHandle ③ → ② → ①（逆序）     │
│           afterCompletion ③ → ② → ①（逆序）│
│       TimingFilter 后置（finally，记录耗时）│
│     XssFilter 后置                          │
│   EncodingFilter 后置                       │
│ TraceIdFilter 后置（清理 MDC）              │
└─────────────────────────────────────────────┘
  │
  ▼
客户端
```

要点：
- **Filter 先于 Interceptor 执行**，且是包绕模型：进入和返回各穿过一次；
- Interceptor 三段式中，`preHandle` 按注册顺序执行，`postHandle`/`afterCompletion` 逆序执行。

<!-- 增加原因：这是面试高频追问「一个请求在 Spring Boot 里的完整链路」，原文档只分别描述两者，没讲衔接。图里的 5 个 Filter 就是 32 章实际的链（TraceId→Encoding→Xss→Timing→SaServlet），Interceptor 三段式对应 31 章 WebMvcConfig 的注册顺序。 -->

---

## 三、过滤器的典型场景

过滤器更偏向**底层、通用、与业务无关**的需求，适合在请求到达 Spring MVC 之前做处理。

### 1. 字符编码统一
等价于 Spring Boot 自动配置的 `CharacterEncodingFilter`，强制所有请求/响应使用 UTF-8。32 章 `EncodingFilter` 用 `FilterRegistrationBean` 手动注册并演示了 `init-param` 读取。

### 2. 跨域处理（CORS）
在 Spring Security 之前放行预检请求，或做自定义跨域头处理。

### 3. 请求日志 / 链路追踪 / 耗时统计
- 32 章 `TraceIdFilter`：生成/透传 traceId 写入 SLF4J MDC，日志 pattern 配 `%X{traceId:-}` 后每条日志自动带链路 ID；
- 32 章 `TimingFilter`：`try/finally` 包住 `chain.doFilter()`，统计耗时并落库 `sys_request_log`。

### 4. 基础安全防护
- **XSS 过滤**：`XssFilter` 把 Request 包装成 `XssHttpServletRequestWrapper`，对参数和 body 做 HTML 转义；
- **请求体可重复读**：`RepeatedlyReadHttpServletRequestWrapper` 缓存 body，Filter 读完后 Controller 的 `@RequestBody` 仍能读；
- SQL 注入检测、请求体签名校验同理。

### 5. 请求/响应压缩
对返回的 JSON/HTML 做 Gzip 压缩，减少带宽。

### 6. Filter 层鉴权（与 Spring Security 同级的能力）
32 章用 Sa-Token 的 `SaServletFilter` 在 Filter 层做登录校验：`setAuth` 中 `SaRouter.match("/**").check(r -> StpUtil.checkLogin())`，未登录由 `setError` 直接写回 401 JSON。**未登录请求根本到不了 Controller**。

> ⚠️ 重复注册陷阱：`@Component` 和 `FilterRegistrationBean` 注册同一个 Filter 会导致执行两次。32 章 `TimingFilter`/`EncodingFilter` 只通过 `FilterConfig` 注册、绝不标 `@Component`（见 `TimingFilter.java:20` 注释）。

<!-- 增加原因：Filter 层鉴权原文档只提了「Spring Security 之前做自定义认证」，没提 Sa-Token 这类纯 Filter 层鉴权方案——这是 32 章的核心场景。重复注册坑是三种注册方式混用时必踩的坑，32 章代码里专门留了注释。 -->

---

## 四、拦截器的典型场景

拦截器更偏向**业务层面**，方便拿到 Handler 方法上下文和使用 Spring Bean。

### 1. 登录态与权限校验（最常用）
31 章用 Sa-Token 的 `SaInterceptor` 在 `preHandle` 中校验登录态。适合需要结合方法注解（如 `@SaCheckPermission`）做细粒度权限的场景。

### 2. 接口审计日志
31 章 `LogInterceptor`：`preHandle` 记开始时间，`afterCompletion` 组装 IP/用户/URI/状态码/耗时写入 `sys_api_log`。

### 3. 接口限流
31 章 `RateLimitInterceptor`：内存 `ConcurrentHashMap<String, TokenBucket>`，IP 维度令牌桶，超限直接写回 429。分布式环境应换 Redis + Lua 或 Sentinel。

### 4. 防重复提交
基于 Token 或用户 + 接口维度，在 `preHandle` 中加分布式锁，提交后释放。

### 5. 通用模型数据注入
在 `postHandle` 中往 ModelAndView 注入公共数据（当前登录用户、系统配置）。

### 6. 数据预处理/后处理
解密请求参数、统一包装响应体（更推荐 `ResponseBodyAdvice`）。

---

## 五、关键实证差异（新增）

### 5.1 谁能记录鉴权失败的 401？

- **Filter 能**。32 章的链是 `TimingFilter(order=10) → SaServletFilter(order=20)`：鉴权失败时 `StpUtil.checkLogin()` 抛异常被 `setError` 捕获写回 401，位于**前面**的 `TimingFilter` 的 finally 块照常执行，把这条 401 记进 `sys_request_log`。
- **Interceptor 不能**。31 章的 `LogInterceptor` 注册在 `SaInterceptor` 之后，未登录请求在 `preHandle` 阶段就被拦下返回，根本到不了 `afterCompletion`，所以审计表里永远没有 401 记录。

验证方法：32 章前端「链路日志」页，清掉 token 后随便发一个请求，就能看到一条 `statusCode=401` 的记录。

<!-- 增加原因：这是 Filter 比 Interceptor「更靠外层」最有说服力的实证，也是 32 章相对 31 章的设计差异点（审计下沉到 Filter 层）。原文档只说 Filter「处理范围大」，没落到具体可复现的证据。 -->

### 5.2 请求包装（Wrapper）能力不对称

- Filter 拿到的是**原始流**，适合包装 Request：`XssHttpServletRequestWrapper` 重写 `getParameter*/getReader` 做参数清洗，`RepeatedlyReadHttpServletRequestWrapper` 让 body 可重复读（32 章）；
- Interceptor 拿到的 request **已被上游 Filter 包装过**，想读 body 必须依赖更靠前的 Filter 做过缓存，否则 `getInputStream()` 只能读一次，Controller 的 `@RequestBody` 会报错。

### 5.3 重复执行陷阱不对称

- 容器 `RequestDispatcher.forward` 会导致 Filter 被**再次触发**；继承 Spring 的 `OncePerRequestFilter` 后内部按请求属性标记保证一次请求只执行一次（32 章 `TimingFilter` 即如此），否则 traceId 会重复生成、耗时会重复统计；
- Interceptor 默认只拦 `DispatcherType.REQUEST`，forward 不会重复触发，无需特殊处理。

---

## 六、一句话总结怎么选

| 场景 | 选哪个 |
|------|--------|
| 编码、压缩、XSS、traceId、请求体包装、底层安全 | **Filter** |
| 业务权限注解、审计、防重复提交、Handler 方法级逻辑 | **Interceptor** |
| 登录校验 | **两条路都成立**：只需要「挡未登录请求」→ Filter 层（SaServletFilter / Spring Security）；需要结合 Controller 方法做细粒度权限 → Interceptor（SaInterceptor） |
| 请求耗时统计且要求覆盖鉴权失败 | **Filter**（finally 兜底，见 5.1） |

<!-- 修改原因：原文档「登录校验优先 Interceptor」在 Sa-Token / Spring Security 时代已不成立——Spring Security 本身就是一条 Filter 链，Sa-Token 也提供 Filter 层鉴权（32 章实证）。补充了 5.1 的耗时统计场景，因为「要不要记录 401」是 Filter/Interceptor 选型的真实决策点。 -->

---

## 修订摘要

| 位置 | 改动 | 原因 |
|------|------|------|
| 速览表「注入 Bean」 | ❌ → ⚠️分注册方式 | 仅 `@WebFilter` 不能注入；32 章 `TimingFilter` 构造器注入反证 |
| 第二节 | 新增两链衔接图 | 面试高频，原文档缺整体视图 |
| 三.6 / 五.1 | 新增 Filter 层鉴权与 401 实证 | 32 章核心场景，Filter「更外层」的可复现证据 |
| 三（末尾） | 新增重复注册陷阱 | `@Component` + `FilterRegistrationBean` 混用必踩坑 |
| 五.2 / 5.3 | 新增 Wrapper 能力不对称、forward 重复触发 | Interceptor 读 body 依赖前置 Filter 缓存；`OncePerRequestFilter` 的存在意义 |
| 六 | 「优先 Interceptor」改为「分场景两条路都成立」 | Spring Security / Sa-Token 均为 Filter 层鉴权，经验法则已过时 |
