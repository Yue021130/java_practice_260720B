# 10-sa-token-practice：Sa-Token 全功能实践

本模块将 [Sa-Token 官方文档](https://sa-token.cc/doc.html#/) 的核心能力转化为可运行、可交互的 Spring Boot + Vue 3 项目代码，覆盖登录认证、权限鉴权、Session、踢人封禁、SSO、OAuth2.0、Redis、JWT、API 签名、网关思路、RPC 状态传递、Quick 登录等全部主要能力，便于系统学习。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + Sa-Token 1.39.0
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS
- 文档：SpringDoc OpenAPI 1.7.0（Swagger UI）
- 可选：Redis（取消 `application.yml` 中 Redis 注释并启动本地 Redis 后自动切换）

## 模块结构

```
10-sa-token-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/satoken/
│   ├── SaTokenPracticeApplication.java
│   ├── common/         # 统一响应、全局异常处理
│   ├── config/         # Sa-Token 配置、CORS、OpenAPI、权限数据接口
│   ├── login/          # 登录认证
│   ├── loginmodel/     # 登录模型
│   ├── permission/     # 权限、角色、路由鉴权
│   ├── session/        # Session 会话
│   ├── kickout/        # 踢人下线、强制注销、账号封禁
│   ├── advanced/       # 二级认证、身份切换、多账号、加密
│   ├── listener/       # 全局侦听器、全局过滤器
│   ├── integration/    # Redis、前后端分离、Token 续签
│   ├── sso/            # SSO 单点登录模拟
│   ├── oauth2/         # OAuth2.0 模拟
│   ├── jwt/            # JWT、临时 Token
│   ├── signature/      # API 参数签名
│   ├── gateway/        # 网关鉴权思路
│   ├── rpc/            # RPC 登录态传递思路
│   └── quick/          # Quick 快速登录
├── src/main/resources/application.yml
├── src/test/java/com/example/satoken/  # JUnit 集成测试
└── web/                 # Vue 3 前端面板
```

## 快速启动

### 后端

```bash
cd 10-sa-token-practice
mvn spring-boot:run
```

启动后访问 Swagger UI：http://localhost:8090/swagger-ui/index.html

### 前端

```bash
cd 10-sa-token-practice/web
npm install
npm run dev
```

前端开发服务器端口：http://localhost:5183

### 运行测试

```bash
cd 10-sa-token-practice
mvn test
```

## 接口速查

### 01. 登录认证 `/api/login`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/login/do-login` | POST | 登录，返回 Token |
| `/api/login/logout` | POST | 注销 |
| `/api/login/is-login` | GET | 是否登录 |
| `/api/login/token-value` | GET | 当前 Token |
| `/api/login/login-id` | GET | 当前登录 ID |
| `/api/login/login-by-token` | POST | 指定 Token 恢复登录态 |

### 02. 登录模型 `/api/login-model`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/login-model/single` | POST | 单端登录 |
| `/api/login-model/multi` | POST | 多端登录 |
| `/api/login-model/mutex` | POST | 同端互斥 |
| `/api/login-model/remember` | POST | 记住我（30 天） |
| `/api/login-model/7days` | POST | 七天免登 |

### 03. 权限认证 `/api/permission`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/permission/login-with-perms` | POST | 登录并写入权限/角色 |
| `/api/permission/check-perm` | GET | 代码校验权限 |
| `/api/permission/anno-admin` | GET | 注解鉴权 admin |
| `/api/permission/anno-and` | GET | 注解 AND 模式 |
| `/api/permission/anno-or` | GET | 注解 OR 模式 |
| `/api/permission/check-role` | GET | 角色认证 |
| `/api/permission/grant` | POST | 越级授权 |

### 04. 路由拦截 `/api/route`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/route/user/info` | GET | 需 user 权限 |
| `/api/route/admin/info` | GET | 需 admin 权限 |
| `/api/route/public/info` | GET | 公开接口 |
| `/api/route/res/{id}` | GET/POST/DELETE | RESTful 方法鉴权 |

### 05. Session `/api/session`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/session/account/set` | POST | Account-Session 写 |
| `/api/session/account/get` | GET | Account-Session 读 |
| `/api/session/token/set` | POST | Token-Session 写 |
| `/api/session/token/get` | GET | Token-Session 读 |
| `/api/session/custom/set` | POST | 自定义 Session 写 |
| `/api/session/custom/get` | GET | 自定义 Session 读 |
| `/api/session/search` | GET | 查询当前账号所有 Token |
| `/api/session/login-device-count` | GET | 登录设备数 |

### 06. 踢人封禁 `/api/manage`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/manage/kickout` | POST | 按账号踢下线 |
| `/api/manage/kickout-by-token` | POST | 按 Token 踢下线 |
| `/api/manage/logout` | POST | 强制注销 |
| `/api/manage/disable` | POST | 账号封禁 300 秒 |
| `/api/manage/disable-category` | POST | 分类封禁 |
| `/api/manage/disable-level` | POST | 阶梯封禁 |
| `/api/manage/is-disable` | GET | 查询封禁状态 |

### 07. 高级认证 `/api/advanced`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/advanced/second-auth` | POST | 开启二级认证 |
| `/api/advanced/check-safe` | GET | 校验二级认证 |
| `/api/advanced/switch-to` | POST | 临时身份切换 |
| `/api/advanced/mock` | POST | 模拟他人账号 |
| `/api/advanced/login-admin` | POST | Admin 体系登录 |
| `/api/advanced/admin-is-login` | GET | Admin 是否登录 |
| `/api/advanced/encrypt` | POST | MD5/SHA256/AES |

### 08. 全局监听过滤 `/api/global`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/global/login` | POST | 登录触发监听 |
| `/api/global/logout` | POST | 注销触发监听 |
| `/api/global/filter-test` | GET | 全局过滤器测试 |

### 09. 集成扩展 `/api/integration`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/integration/dao-type` | GET | 查看当前 DAO 实现 |
| `/api/integration/header-token` | POST | 前后端分离 Token |
| `/api/integration/token-timeout` | GET | Token 有效期 |
| `/api/integration/renew` | POST | 手动续签 |
| `/api/integration/public/info` | GET | 公开接口 |

### 10. SSO `/api/sso`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/sso/do-login` | POST | SSO 服务端登录 |
| `/api/sso/is-login` | GET | 查询 SSO 登录态 |
| `/api/sso/client1/info` | GET | 客户端 1 校验 |
| `/api/sso/client2/info` | GET | 客户端 2 校验 |
| `/api/sso/logout` | POST | SSO 单点注销 |
| `/api/sso/public/modes` | GET | SSO 三种模式说明 |

### 11. OAuth2.0 `/api/oauth2`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/oauth2/authorize` | GET | 授权码模式 - 申请 code |
| `/api/oauth2/token` | POST | 授权码模式 - 换 token |
| `/api/oauth2/password-token` | POST | 密码模式 |
| `/api/oauth2/client-token` | POST | 客户端凭证模式 |
| `/api/oauth2/refresh` | POST | 刷新令牌 |
| `/api/oauth2/userinfo` | GET | 受保护资源 |
| `/api/oauth2/openid` | GET | openid 模式 |

### 12. JWT & 临时 Token `/api/jwt`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/jwt/generate` | POST | 生成 JWT |
| `/api/jwt/verify` | POST | 校验 JWT |
| `/api/jwt/temp-token` | POST | 临时 Token（60 秒） |
| `/api/jwt/modes` | GET | JWT 三种模式 |

### 13. API 签名 `/api/signature`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/signature/generate` | POST | 生成签名 |
| `/api/signature/verify` | POST | 校验签名 |

### 14. 网关 `/api/gateway`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/gateway/check` | GET | 网关登录态校验 |
| `/api/gateway/intro` | GET | 网关鉴权说明 |

### 15. RPC `/api/rpc`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/rpc/upstream` | GET | 上游获取 Token |
| `/api/rpc/downstream` | POST | 下游恢复登录态 |

### 16. Quick 登录 `/api/quick`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/quick/phone-login` | POST | 手机号一键登录 |
| `/api/quick/scan-login` | POST | 扫码登录 |
| `/api/quick/intro` | GET | Quick 登录说明 |

## 面试八股

### Sa-Token 与 Spring Security / Shiro 的区别？

- **Sa-Token**：国产轻量级，API 极简，一句话完成登录鉴权；内置登录模型、权限、Session、踢人、封禁、SSO、OAuth2、JWT、分布式网关等全家桶。
- **Spring Security**：Spring 生态重量级安全框架，功能全面但配置复杂，学习曲线陡峭。
- **Shiro**：老牌权限框架，不依赖 Spring，功能完善但社区活跃度下降，缺少现代 OAuth2/Gateway 等原生支持。

### StpUtil.login(id) 之后框架做了什么？

1. 生成 Token（默认 UUID）。
2. 将 Token 与 loginId 映射写入持久层（内存/Redis）。
3. 创建或复用 loginId 对应的 Account-Session。
4. 创建 Token-Session。
5. 将 Token 写入当前请求的 Cookie、Header（可选）。
6. 触发 `SaTokenListener.doLogin` 事件。

### Token 与 Session 的关系？

- **Token**：客户端持有的凭证，用于标识一次登录。
- **Account-Session**：以 loginId 为 key，同一账号多端共享。
- **Token-Session**：以 Token 为 key，每个 Token 独立，适合存储单次请求/设备维度的数据。

### 单端/多端/同端互斥登录的实现原理？

通过 `sa-token.is-concurrent` 和 `is-share` 控制：

- 单端：`is-concurrent=false`，新登录挤掉旧登录。
- 多端：`is-concurrent=true, is-share=false`，每次登录新建 Token。
- 同端互斥：在 `SaLoginModel` 中设置 `device`，结合自定义逻辑限制同 device 同时在线数。

### `@SaCheckPermission` 与路由拦截的区别？

- **注解鉴权**：在 Controller 方法上加注解，进入方法前校验，粒度细，适合具体接口。
- **路由拦截**：在 `SaInterceptor` 中按路径批量配置，适合模块级、RESTful 风格鉴权。

### 踢人下线与强制注销的区别？

- **踢人下线 `kickout`**：让指定账号/Token 的登录态失效，但用户再次请求时可重新登录。
- **强制注销 `logout`**：彻底清除指定账号的所有登录状态，效果更强。

### 账号封禁、分类封禁、阶梯封禁？

- **账号封禁**：`StpUtil.disable(id, time)`，登录时检查，全局禁止登录。
- **分类封禁**：`StpUtil.disableLevel(id, service, level, time)`，按业务维度封禁，例如禁止评论、禁止发帖。
- **阶梯封禁**：根据违规次数动态提升 level，level 越高封禁时间越长，实现处罚累加。

### SSO 三种模式？

- **同域模式**：所有子系统在同一主域名下，Cookie 自动共享，配置最简单。
- **跨域模式**：域名不同，通过 Sa-Token ticket 机制在跳转时传递登录态。
- **跨 Redis 模式**：服务端与客户端不共享 Redis，子系统通过 HTTP 接口向 SSO 服务端校验登录态。

### OAuth2.0 四种授权模式适用场景？

- **授权码模式**：最安全，适用于 Web 应用、移动应用，推荐首选。
- **密码模式**：用户把账号密码交给受信任客户端，适用于第一方应用。
- **客户端凭证模式**：服务端之间调用，无需用户参与。
- **隐式模式**：已逐渐被 PKCE 授权码模式替代，不推荐使用。

### Redis 集成与分布式 Session？

引入 `sa-token-redis-jackson` 并在 `application.yml` 中配置 Redis 后，Sa-Token 的 Token、Session、权限缓存全部持久化到 Redis，天然支持分布式部署和共享登录态。

## 推荐实验顺序

1. 登录认证：体验登录/注销/Token。
2. 登录模型：理解单端/多端/同端互斥。
3. 权限认证：登录后写入权限，测试注解与代码鉴权。
4. 路由拦截：未登录访问 user/admin 模块，观察 401/403。
5. Session：Account-Session 与 Token-Session 区别。
6. 踢人封禁：登录后踢下线，再登录后封禁。
7. 高级认证：二级认证、身份切换、多账号。
8. 全局监听过滤：观察控制台事件日志。
9. 集成扩展：查看 DAO 类型、前后端分离 Token。
10. SSO/OAuth2/JWT：按前端面板流程体验。
11. 签名/Gateway/RPC/Quick：扩展能力了解。
