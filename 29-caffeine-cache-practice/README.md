# 第29章：Vue3 + SpringBoot 前后端分离实战（Sa-Token JWT 无状态登录 + Caffeine 缓存）

## 一、业务背景

前面章节大多是单点技术演示。真实企业开发是**前后端分离**的：后端提供 REST API，前端独立部署，
通过 Token 认证身份。本章用一套最小但完整的「后台用户管理系统」串起全链路：

- 后端：**MyBatis-Plus** 操作 MySQL，**Sa-Token（JWT 无状态模式）** 做登录认证，**Caffeine** 做本地缓存加速查询，**Knife4j** 自动生成 API 文档；
- 前端：**Vue3 + Element Plus** 构建页面，**Pinia** 管理登录态，**Axios 拦截器** 自动携带 Token、统一处理 401；
- 缓存是本章主角：用户详情/分页列表加 `@Cacheable`，增删改 `@CacheEvict` 清空缓存，
  前端提供「缓存演示」页实时观察命中率，直观看到 SQL 消失的过程。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，StpLogicJwtForStateless 无状态模式） |
| 缓存 | Caffeine（Spring Cache 抽象 + @Cacheable/@CacheEvict） |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
29-caffeine-cache-practice/
├── pom.xml                              # 后端依赖管理
├── docs/
│   └── sql/
│       └── schema.sql                   # MySQL 8.0 建库建表 + 初始化数据
├── src/main/java/com/example/ccp/
│   ├── CcpApplication.java              # 启动类（@MapperScan）
│   ├── common/
│   │   ├── Result.java                  # 统一返回结果 {code, msg, data}
│   │   ├── BusinessException.java       # 业务异常
│   │   └── GlobalExceptionHandler.java  # 全局异常处理（含 NotLoginException → 401）
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域
│   │   ├── Knife4jConfig.java           # OpenAPI3 文档 Bean
│   │   ├── MyBatisPlusConfig.java       # 分页插件
│   │   ├── SaTokenConfig.java           # 切换 JWT 无状态 StpLogic
│   │   ├── WebMvcConfig.java            # Sa-Token 拦截器（除 /api/auth/login 均需登录）
│   │   └── CacheConfig.java             # CaffeineCacheManager + @EnableCaching
│   ├── controller/
│   │   ├── AuthController.java          # 登录/登出/当前用户
│   │   ├── UserController.java          # 用户 CRUD
│   │   └── CacheDemoController.java     # 缓存统计/对比实验/清空
│   ├── dto/                             # LoginDTO/UserSaveDTO 入参，LoginVO/UserVO 出参
│   ├── entity/User.java                 # sys_user 表实体（@TableLogic 逻辑删除）
│   ├── mapper/UserMapper.java           # extends BaseMapper<User>
│   ├── service/
│   │   ├── AuthService.java             # 登录签发 JWT
│   │   ├── UserService.java             # CRUD + @Cacheable/@CacheEvict
│   │   └── CacheDemoService.java        # 读取 Caffeine 原生统计
│   └── util/PasswordUtil.java           # BCrypt 密码哈希
├── src/main/resources/application.yml
└── web/                                 # 前端
    ├── index.html
    ├── package.json / vite.config.js    # 端口 5173，/api 代理到 8080
    └── src/
        ├── main.js / App.vue
        ├── router/index.js              # 路由 + 登录守卫
        ├── stores/user.js               # Pinia：token、userInfo（localStorage 持久化）
        ├── api/
        │   ├── request.js               # axios 封装：请求注入 Token、响应解包 Result、401 跳登录
        │   ├── auth.js / user.js / cache.js
        ├── layout/Index.vue             # 侧边栏 + 顶栏布局
        └── views/
            ├── Login.vue                # 登录页
            ├── User.vue                 # 用户管理（搜索/分页/新增/编辑/删除）
            └── Dashboard.vue            # 缓存命中率监控 + 对比实验
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**（Knife4j）。
> 除登录外均需携带 `Authorization: <token>` 请求头。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT token | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前登录用户信息 | 登录 |
| GET | /api/user/page | 用户分页列表（userPage 缓存） | 登录 |
| GET | /api/user/{id} | 用户详情（user 缓存） | 登录 |
| POST | /api/user | 新增用户（清缓存） | 登录 |
| PUT | /api/user | 编辑用户（清缓存，密码留空不改） | 登录 |
| DELETE | /api/user/{id} | 逻辑删除（清缓存） | 登录 |
| GET | /api/cache/stats | 缓存命中率统计 | 登录 |
| GET | /api/cache/compare/{id} | 缓存对比实验（连查两次） | 登录 |
| GET | /api/cache/clear | 清空全部缓存 | 登录 |

## 五、运行方式

前置：本机已安装 MySQL 8.0，且 8080/5173 端口未被占用。
**先修改 `src/main/resources/application.yml` 中的数据库用户名密码为你本机 MySQL 的实际凭据。**

```bash
# 1. 建库建表（root 密码默认按 root 配置，见 application.yml）
mysql -u root -p < docs/sql/schema.sql

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端（另开终端）
cd web && npm install && npm run dev
```

- 前端：http://localhost:5173 ，默认账号 **admin / 123456**
- 接口文档：http://localhost:8080/doc.html

## 六、核心场景说明

### 6.1 Sa-Token JWT 无状态登录流程

1. 前端提交用户名密码 → 后端 BCrypt 校验；
2. `StpUtil.login(userId)`：由于切换了 `StpLogicJwtForStateless`，直接生成 JWT 字符串，**服务端不保存任何会话**；
3. 前端把 token 存入 Pinia + localStorage，axios 请求拦截器每次放入 `Authorization` 头；
4. `WebMvcConfig` 注册的 Sa-Token 拦截器校验 `/api/**`（放行 login），JWT 无效/过期抛 `NotLoginException` → 全局异常处理返回 `code=401`；
5. 前端响应拦截器识别 401 → 清空登录态并跳回登录页。

### 6.2 Caffeine 缓存一致性设计

| 操作 | 缓存行为 |
| --- | --- |
| 查询用户详情/分页列表 | `@Cacheable`，首次走库并写入缓存，后续直接命中 |
| 新增/编辑/删除用户 | `@CacheEvict(allEntries = true)` 先写库、后清缓存，避免脏读 |
| 统计与实验 | `/api/cache/stats` 读取 Caffeine 原生 `CacheStats`；`/api/cache/compare/{id}` 连查两次直观对比 |

核心配置集中在 `application.yml`：

```yaml
spring:
  cache:
    cache-names: user, userPage
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m,recordStats
```

## 七、八股速记

**7.1 Caffeine 核心配置**
- `maximumSize`：基于 LRU + Window TinyLFU 的容量上限，超过后淘汰最少使用条目；
- `expireAfterWrite` vs `expireAfterAccess`：写入后过期（适合配置类数据）vs 访问后过期（适合热点数据）；
- `recordStats`：开启统计，配合 `CacheStats` 看命中率；
- 一致性原则：**先更新数据库，再删除缓存**（而不是更新缓存），防止并发脏读。

**7.2 缓存三大问题**
- 穿透：查不存在的 key——缓存空值（本项目的 `setAllowNullValues(true)`）+ 布隆过滤器；
- 击穿：热点 key 过期瞬间被并发打穿——逻辑过期 / 互斥锁重建；
- 雪崩：大量 key 同时过期——过期时间加随机抖动。

**7.3 JWT 无状态 vs Session**
- Session：状态存服务端，扩展需共享存储（Redis）；
- JWT 无状态：状态在 Token 里，服务端零存储，水平扩展无压力；代价是无法主动失效，登出靠客户端删 Token（生产用黑名单兜底）。

**7.4 Sa-Token 拦截流程**
注册 `SaInterceptor` → 匹配 `/api/**` 排除登录接口 → `StpUtil.checkLogin()` 校验 JWT 签名与有效期 → 失败抛 `NotLoginException` → `@RestControllerAdvice` 统一返回 401。

## 八、测试验证

```bash
# 后端单元测试（不依赖数据库）
mvn test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
# 登录拿 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 带 token 访问用户列表；连续执行两次，第二次控制台不打印 SQL 即缓存命中
curl http://localhost:8080/api/user/page -H "Authorization: <token>"
```

## 九、作者

Yue021130 <169446203@qq.com>
