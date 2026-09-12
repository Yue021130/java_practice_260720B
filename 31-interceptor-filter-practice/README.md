# 第31章：Vue3 + SpringBoot 前后端分离实战（Filter + Interceptor）

## 一、业务背景

企业级 Web 应用经常需要在请求处理的不同阶段插入通用横切逻辑：日志、鉴权、限流、审计、统一编码等。Spring Boot 提供两条经典扩展链路：

- **Servlet Filter**：运行在 Servlet 容器层，最早进入、最晚离开，能拿到最原始的 `HttpServletRequest/Response`；
- **Spring MVC Interceptor**：运行在 DispatcherServlet 内部，只在被 Spring 路由到的 Handler 前后生效，能拿到 Controller 方法上下文。

本章以「资源管理系统」为业务载体，串联 **请求日志 Filter**、**可重复读取的 RequestWrapper**、**操作审计 Interceptor** 与 **IP 令牌桶限流 Interceptor**，并把每一次 Controller 调用写入 `sys_api_log` 审计表，前端可实时查看拦截器记录的完整链路日志。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，无状态模式） |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
31-interceptor-filter-practice/
├── pom.xml
├── docs/
│   └── sql/
│       └── schema.sql                   # MySQL 8.0 建库建表 + 初始化数据
├── src/main/java/com/example/sfp/
│   ├── SfpApplication.java              # 启动类（@MapperScan）
│   ├── common/
│   │   ├── Result.java                  # 统一返回结果 {code, msg, data}
│   │   ├── BusinessException.java       # 业务异常
│   │   └── GlobalExceptionHandler.java  # 全局异常（含 NotLoginException→401）
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域
│   │   ├── Knife4jConfig.java           # OpenAPI3 文档 Bean
│   │   ├── MyBatisPlusConfig.java       # 分页插件
│   │   ├── SaTokenConfig.java           # JWT 无状态 StpLogic
│   │   ├── FilterConfig.java            # 【主题一】注册 RequestLogFilter
│   │   └── WebMvcConfig.java            # 【主题二】注册 RateLimit / Sa-Token / Log 拦截器
│   ├── filter/                          # Servlet Filter
│   │   ├── RequestLogFilter.java        # 请求进入日志 + 包装 Request
│   │   └── RepeatedlyReadHttpServletRequestWrapper.java  # 可重复读取 body 的包装器
│   ├── interceptor/                     # Spring MVC Interceptor
│   │   ├── LogInterceptor.java          # 记录 IP / 用户 / URI / 耗时 / 状态码
│   │   └── RateLimitInterceptor.java    # IP 维度令牌桶限流
│   ├── controller/
│   │   ├── AuthController.java          # 登录/登出/当前用户
│   │   ├── ResourceController.java      # 资源 CRUD + 限流测试接口
│   │   └── ApiLogController.java        # 审计日志分页查询
│   ├── dto/
│   │   ├── LoginDTO.java / LoginVO.java / UserVO.java
│   │   ├── ResourceSaveDTO.java         # 资源入参（JSR-303 校验）
│   │   ├── ResourceVO.java              # 资源出参
│   │   └── ApiLogVO.java                # 审计日志出参
│   ├── entity/
│   │   ├── User.java                    # 系统用户（sys_user）
│   │   ├── BizResource.java             # 资源（biz_resource，避 javax.annotation.Resource 同名冲突）
│   │   └── ApiLog.java                  # 审计日志（sys_api_log）
│   ├── mapper/                          # MyBatis-Plus BaseMapper
│   ├── service/
│   └── util/PasswordUtil.java           # BCrypt 密码哈希
├── src/main/resources/application.yml
└── web/                                 # 前端
    ├── index.html / package.json / vite.config.js
    └── src/
        ├── main.js / App.vue
        ├── router/index.js              # 路由 + 登录守卫
        ├── stores/user.js               # Pinia：token / userInfo
        ├── api/request.js               # axios：Token 注入、Result 解包、401 跳转
        ├── api/auth.js / resource.js / apiLog.js
        ├── layout/Index.vue
        └── views/Login.vue / Resource.vue / ApiLog.vue
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**（Knife4j）。
> 除登录外均需携带 `Authorization: <token>` 请求头。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT token | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前登录用户信息 | 登录 |
| GET | /api/resource/page | 资源分页列表 | 登录 |
| GET | /api/resource/{id} | 资源详情 | 登录 |
| POST | /api/resource | 新增资源 | 登录 |
| PUT | /api/resource | 编辑资源 | 登录 |
| DELETE | /api/resource/{id} | 逻辑删除 | 登录 |
| GET | /api/resource/test-rate-limit | 限流测试（快速连点可触发 429） | 登录 |
| GET | /api/api-log/page | 审计日志分页（由 LogInterceptor 写入） | 登录 |

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

## 六、核心场景说明

### 6.1 Filter 层：请求日志 + RequestWrapper

`RequestLogFilter` 通过 `FilterRegistrationBean` 注册，`order=1` 保证它处于最前端。它把原始 `HttpServletRequest` 包装成 `RepeatedlyReadHttpServletRequestWrapper`，将 body 一次性读入字节数组，后续 `getInputStream()/getReader()` 都从这个缓存返回——这样 Filter 打印完请求体后，Controller 的 `@RequestBody` 仍能正常读取。

### 6.2 Interceptor 层：限流 → 登录校验 → 审计

`WebMvcConfig` 按顺序注册三个拦截器：

1. `RateLimitInterceptor`：基于内存 `ConcurrentHashMap<String, TokenBucket>`，每个 IP 一个桶。容量 10，每 6 秒补充 1 个令牌，即约 10 次/分钟；触发限流直接通过 `response` 写回 `429` JSON，不进入 Controller。
2. `SaInterceptor`：Sa-Token 登录校验，放行 `/api/auth/login`。
3. `LogInterceptor`：`preHandle` 记录开始时间；`afterCompletion` 组装 `ApiLog`（IP、方法、URI、User-Agent、登录用户、状态码、耗时、是否异常）并异步？写入 `sys_api_log`。

> 注意：当前 `LogInterceptor` 采用同步 `apiLogService.save()`，演示环境足够；生产可改用 Spring 事件异步落库。

### 6.3 审计日志查询

前端「审计日志」页面调用 `/api/api-log/page`，数据全部来自 `LogInterceptor` 的自动记录。操作资源管理（新增/编辑/删除/限流测试）后刷新审计日志，可看到对应 URI 的耗时与状态码。

## 七、八股速记

**7.1 Filter 与 Interceptor 的区别**

| 维度 | Filter | Interceptor |
| --- | --- | --- |
| 规范 | Servlet 规范 | Spring MVC 规范 |
| 执行时机 | Servlet 容器最前端 | DispatcherServlet 内部 |
| 作用范围 | 所有请求（含静态资源、错误页） | 仅被 Spring 路由到的 Handler |
| 能否中断 | 可以，直接写响应 | 可以，`preHandle` 返回 false |
| 获取 Controller 信息 | 不能 | 可以，`handler` 参数可拿到 Method |
| 配置方式 | `@WebFilter` / `FilterRegistrationBean` | `WebMvcConfigurer.addInterceptors` |

**7.2 为什么需要 RequestWrapper**

默认 `HttpServletRequest.getInputStream()` 只能读取一次。Filter 或前置拦截器一旦读取 body，后续 `@RequestBody` 会报 `stream closed`。解决思路：在 Filter 链最前端把 body 缓存到 `HttpServletRequestWrapper` 子类，重写 `getInputStream()` 和 `getReader()` 返回基于缓存的新流。

**7.3 Interceptor 执行顺序**

`preHandle` 按注册顺序执行；`postHandle` 和 `afterCompletion` 按注册逆序执行。因此登录校验放在限流之后、审计之前，既能先挡住非法流量，又能让审计记录落在登录校验之后拿到用户身份。

**7.4 生产限流建议**

本章使用内存令牌桶，仅适合单机演示。分布式环境应使用 Redis + Lua 脚本实现原子漏桶/令牌桶，或接入 Sentinel、Bucket4j 等专业限流框架。

## 八、测试验证

```bash
# 后端单元测试（含 TokenBucket 限流断言）
mvn test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 资源分页（同时触发 Filter 日志 + Interceptor 审计）
curl -s http://localhost:8080/api/resource/page -H "Authorization: $TOKEN"

# 连续触发限流测试，第 11 次左右返回 429
for i in {1..15}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/resource/test-rate-limit -H "Authorization: $TOKEN"
done

# 查看审计日志
curl -s "http://localhost:8080/api/api-log/page" -H "Authorization: $TOKEN"
```

## 九、作者

Yue021130 <169446203@qq.com>
