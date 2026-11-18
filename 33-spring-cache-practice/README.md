# 第33章：Vue3 + SpringBoot 前后端分离实战（Spring Cache 二级缓存）

## 一、业务背景

前面章节大多是单点技术演示。真实企业开发的读多写少业务（商品、配置、字典、文章）都离不开缓存。第29章演示了 Spring Cache + Caffeine 本地缓存，本章更进一步：**手写 Caffeine（一级）+ Redis（二级）二级缓存组件**，把 Spring Cache 抽象的注解能力、三大经典问题（穿透/击穿/雪崩）的应对方案、命中率监控全部落到一套可运行的商品管理系统里。

- 后端：MyBatis-Plus 操作 MySQL，Sa-Token（JWT 无状态）做登录认证，**自定义 `TwoLevelCacheManager`** 接管 Spring Cache 注解，Knife4j 自动生成 API 文档；
- 前端：Vue3 + Element Plus，「缓存监控」页实时展示 L1/L2 命中、回源 MISS、命中率与缓存 key 分布，直观看到 SQL 消失的过程；
- 缓存是本章主角：`@Cacheable` / `@CacheEvict` / `@CachePut` / `@Caching` 全覆盖，自定义 `KeyGenerator`，空值占位防穿透、SETNX 互斥防击穿、TTL 抖动防雪崩。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，StpLogicJwtForStateless 无状态模式） |
| 缓存 | Spring Cache 抽象 + 自定义 Caffeine（L1）+ Redis（L2）二级缓存 |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
33-spring-cache-practice/
├── pom.xml                              # 含 spring-boot-starter-data-redis + caffeine
├── docs/
│   ├── spring-cache-guide.md            # 八股+场景参考文档（注解速查/三大问题/序列化/坑清单）
│   └── sql/
│       └── schema.sql                   # MySQL 8.0 建库建表 + 初始化数据
├── src/main/java/com/example/scp/
│   ├── ScpApplication.java              # 启动类（@EnableCaching + @MapperScan）
│   ├── common/                          # Result / BusinessException / GlobalExceptionHandler
│   ├── cache/                           # 【本章核心】二级缓存组件
│   │   ├── TwoLevelCache.java           # L1 Caffeine + L2 Redis 的 Cache 实现
│   │   ├── TwoLevelCacheManager.java    # CacheManager 实现，接管 @Cacheable 的 value
│   │   ├── TwoLevelCacheProperties.java # 各 cacheName 的 TTL/jitter 配置
│   │   ├── CacheMetrics.java            # L1/L2/MISS/驱逐 命中率统计（LongAdder）
│   │   ├── NullValue.java               # 空值占位符（防穿透）
│   │   └── RedisLockClient.java         # SETNX 分布式锁（防击穿演示）
│   ├── config/
│   │   ├── CacheConfig.java             # 装配 TwoLevelCacheManager + 自定义 KeyGenerator
│   │   ├── RedisConfig.java             # RedisTemplate JSON 序列化（GenericJackson2Json）
│   │   ├── CorsConfig / Knife4jConfig / MyBatisPlusConfig / SaTokenConfig
│   ├── controller/
│   │   ├── AuthController.java          # 登录/登出/当前用户
│   │   ├── ProductController.java       # 商品 CRUD + /hot/{id} 击穿演示接口
│   │   └── CacheMonitorController.java  # /api/cache/stats、/keys、/clear
│   ├── dto/  entity/  mapper/  service/
│   └── util/PasswordUtil.java           # BCrypt 密码哈希
├── src/main/resources/application.yml   # 数据源/Redis/二级缓存 TTL 配置
└── web/                                 # 前端
    ├── index.html / package.json / vite.config.js
    └── src/
        ├── router/index.js              # 路由 + 登录守卫
        ├── stores/user.js               # Pinia：token / userInfo
        ├── api/request.js               # axios：Token 注入、Result 解包、401 跳转
        ├── api/auth.js / product.js / cacheMonitor.js
        ├── layout/Index.vue
        └── views/Login.vue / Product.vue / CacheMonitor.vue
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**（Knife4j）。
> 除登录外均需携带 `Authorization: <token>` 请求头。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT token | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前登录用户信息 | 登录 |
| GET | /api/product/page | 商品分页（@Cacheable + condition 只缓存小页） | 登录 |
| GET | /api/product/{id} | 商品详情（@Cacheable，空值占位防穿透） | 登录 |
| GET | /api/product/hot/{id} | 热点详情（SETNX 锁防击穿演示） | 登录 |
| POST | /api/product | 新增（@CacheEvict 清分页缓存） | 登录 |
| PUT | /api/product | 修改（@CachePut + @Caching） | 登录 |
| DELETE | /api/product/{id} | 逻辑删除（@Caching 双清） | 登录 |
| GET | /api/cache/stats | 各缓存命中率统计 | 登录 |
| GET | /api/cache/keys?cache=product | 查看 L1/L2 缓存 key 分布 | 登录 |
| POST | /api/cache/clear?cache=product | 清空指定缓存 | 登录 |

## 五、运行方式

前置：本机已安装 **MySQL 8.0** 和 **Redis**（`docker run -d -p 6379:6379 redis:7` 即可），且 8080/5173 端口未被占用。
**先修改 `src/main/resources/application.yml` 中的数据库用户名密码为你本机的实际凭据。**

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
- Redis 客户端观察：`SCAN 0 MATCH product::*` 看二级缓存 key

## 六、核心场景说明

### 6.1 注解全景（ProductService）

| 方法 | 注解 | 设计意图 |
| --- | --- | --- |
| `page()` | `@Cacheable(value="productPage", keyGenerator="productPageKeyGenerator", condition="#size <= 20")` | 分页结果变化频繁，只缓存小页；自定义 KeyGenerator 生成 `page:1:10:手机:-` 可读 key |
| `getById()` | `@Cacheable(value="product", key="#id")` | **故意不写 `unless`**——查不到的商品以 NullValue 占位（30s TTL）挡穿透 |
| `hotDetail()` | 无注解 | 击穿演示：先查缓存，miss 走 `RedisLockClient` SETNX 互斥回源 + 双重检查 |
| `save()` | `@CacheEvict(value="productPage", allEntries=true)` | 新增影响列表，清整个分页缓存 |
| `update()` | `@CachePut(value="product", key="#dto.id")` + `@Caching(evict 分页)` | 方法必执行，结果直接写回详情缓存；同时清分页 |
| `delete()` | `@Caching(evict={@CacheEvict("product"), @CacheEvict(value="productPage", allEntries=true)})` | 详情按 key 清 + 分页整清 |

### 6.2 查找链路

`@Cacheable` 方法被调用 → `TwoLevelCache.get()`：① L1 Caffeine（纳秒级）→ ② L2 Redis（命中后回填 L1）→ ③ 两级 miss 才执行方法体查库，结果 put 两级。写缓存时 TTL = 基础 TTL + `[0, jitter)` 随机抖动防雪崩。

### 6.3 监控验证

1. 登录后反复刷新「商品管理」页 3~4 次；
2. 打开「缓存监控」页：前几次 MISS 多，之后 L1 命中占绝对大头，命中率趋近 100%；
3. 后端控制台 SQL 逐渐消失（MyBatis-Plus 的 StdOutImpl 日志）；
4. 「缓存监控」页可查 L1/L2 key 分布、手动清空缓存，配合 Redis 客户端 `SCAN` 对照观察。

## 七、八股速记

**7.1 Spring Cache 原理**

`@EnableCaching` 后，Spring 通过 AOP 代理拦截带缓存注解的方法：查缓存 → 命中返回 → miss 执行方法 → 回填。`CacheAutoConfiguration` 按 classpath 自动选型 CacheManager（ConcurrentMap/Caffeine/Redis...）；声明自定义 `CacheManager` Bean 后自动选型整体让位。

**7.2 condition vs unless**

`condition` 在**方法执行前**判，false 则不走缓存（方法仍会执行）；`unless` 在**方法执行后**判，true 则**不回填**。经典用法 `unless="#result == null"` 不缓存空结果——本章故意在 `getById` 不用它，让空值以占位符进缓存防穿透。

**7.3 三种提供者对比**

| 提供者 | 优点 | 缺点 | 适用 |
| --- | --- | --- | --- |
| ConcurrentMap | 零依赖 | 无淘汰策略、无容量控制 | 测试 |
| Caffeine（29章） | 纳秒级命中、Window-TinyLFU 淘汰 | 多实例不一致、受 JVM 内存限制 | 单机读多写少 |
| Redis（本章 L2） | 跨进程共享、容量大 | 网络往返亚毫秒级 | 分布式共享 |
| 二级（本章） | L1 扛读 + L2 共享 | 一致性复杂 | 生产主流 |

**7.4 三大经典问题**

- **穿透**（查不存在的数据）：NullValue 空值占位 + 短 TTL（30s），占位命中计入 L1/L2 命中而非 MISS，数据库被挡；
- **击穿**（热点 key 过期瞬间并发重建）：SETNX 只允许一个线程回源，其他自旋等待读缓存；
- **雪崩**（大量 key 同刻过期）：TTL 加 `[0, jitter)` 随机抖动打散过期时刻。
- 落点：`cache/NullValue.java`、`cache/RedisLockClient.java`、`TwoLevelCache.ttlWithJitter()`。

**7.5 序列化选型**

默认 Jdk 序列化不可读且强耦合 Serializable，生产统一 JSON。本章值用 `GenericJackson2JsonRedisSerializer`（自描述带 `@class`，反序列化不用传 Class），key 用 `StringRedisSerializer`（可读、便于 SCAN）。必须注册 `JavaTimeModule`，否则 `LocalDateTime` 反序列化失败。

**7.6 双写一致性**

先更新数据库再删缓存（Cache Aside）。删除而非更新缓存，避免并发读写产生脏数据窗口。本章 L1 靠 evict 主动失效 + 短 TTL 兜底；强一致场景需 Canal/MQ 同步或牺牲性能走单级缓存。

**7.7 注解不生效的常见原因**

没开 `@EnableCaching`；方法**自调用**（this 调用不走 AOP 代理）；`condition/unless` 里引用了方法参数名但编译没加 `-parameters`。

更系统的八股（含坑清单）见 **docs/spring-cache-guide.md**。

## 八、测试验证

```bash
# 后端单元测试（全部 Mockito mock，无需 Redis/MySQL）
mvn clean test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 同一商品连查 3 次：仅第 1 次有 SQL，后两次命中缓存
for i in 1 2 3; do
  curl -s http://localhost:8080/api/product/1 -H "Authorization: $TOKEN" > /dev/null
done

# 查一个不存在的 id：返回 null 数据，但已被 NullValue 占位缓存（穿透防护）
curl -s http://localhost:8080/api/product/99999 -H "Authorization: $TOKEN"

# 击穿演示接口（SETNX 互斥重建）
curl -s http://localhost:8080/api/product/hot/1 -H "Authorization: $TOKEN"

# 查看命中率：hitRate 应随查询次数上升
curl -s http://localhost:8080/api/cache/stats -H "Authorization: $TOKEN"

# Redis 里看二级缓存 key（TTL 带随机抖动，每个 key 不同）
redis-cli scan 0 match "product::*"
```

## 九、作者

Yue021130 <169446203@qq.com>
