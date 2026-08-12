# 15-caffeine-practice：Spring Boot + Caffeine 缓存实践

本模块把 Java 生态最流行的高性能本地缓存 **Caffeine**（Guava Cache 的继任者）与 **Spring Cache** 注解抽象转化为可运行、可交互的 Spring Boot + Vue 3 项目代码，覆盖**快速开始、淘汰策略、刷新与异步、统计监控、缓存预热、穿透/击穿/雪崩、两级缓存、Spring Cache 注解、缓存一致性、常见坑与调优**等完整能力，每个实验都在真实 JVM 上跑给你看，便于系统学习。

**重点场景**：缓存预热——应用启动完成后自动把热门 key 装进缓存，前端可查状态、手动触发、对比预热前后命中率。

**开箱即用**：不需要 Redis、不需要数据库（都做了内存模拟），直接 `mvn spring-boot:run` 就能玩。

**为什么选 Caffeine 而不是 Guava Cache**：Caffeine 是 Guava Cache 作者写的续作，基于 Window-TinyLFU 淘汰算法，并发吞吐是 Guava 的好几倍，命中率也更高——这也是面试里「本地缓存用什么」的标准答案。

> ⚠️ Caffeine 2.9.x 兼容 Java 8~17 且无需任何 JVM 参数（3.x 要求 Java 11）。本项目用 Spring Boot 依赖管理锁定的 **2.9.3**，守住本仓库「兼容 Java 8」的承诺。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 缓存：`com.github.ben-manes.caffeine:caffeine` **2.9.3**（Spring Boot 依赖管理锁定）+ `spring-boot-starter-cache`（@EnableCaching）
- 测试：JUnit 5 + MockMvc + AssertJ（含预热状态机专项 + 单飞并发确定性断言）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS
- 端口：后端 **8095**，前端 **5188**

## 核心：Caffeine 是什么

`Caffeine` 是一个高性能 Java 进程内缓存库，核心卖点：**Window-TinyLFU 淘汰算法 + 丰富的淘汰/刷新/异步/统计能力 + Spring 原生集成**。

| 能力 | 配置 | 说明 |
| --- | --- | --- |
| 容量淘汰 | `maximumSize(N)` | 满了按 Window-TinyLFU 淘汰低频 key |
| 时间淘汰 | `expireAfterWrite` / `expireAfterAccess` | 写后固定过期 / 访问续命 |
| 自动刷新 | `refreshAfterWrite` + CacheLoader | 过期后旧值可用、后台异步刷新，读不阻塞 |
| 自动加载 | `LoadingCache` / `CacheLoader` | get 未命中自动加载回填，并发只加载一次（防击穿） |
| 异步 | `AsyncCache` / `AsyncLoadingCache` | 返回 CompletableFuture，加载在线程池 |
| 统计 | `recordStats()` | 命中率 / 淘汰数 / 加载耗时，可对接监控 |

**两条使用路径**：
1. 原生 Caffeine API（`Cache` / `LoadingCache` 直连）——本项目 01~07 章、10 章用它把原理讲透；
2. Spring Cache 抽象（`@Cacheable` 等注解 + CacheManager）——08、09 章走它，换 Redis 只需换 CacheManager。

## 模块结构

```
15-caffeine-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/cache/
│   ├── CaffeinePracticeApplication.java   # @EnableCaching
│   ├── common/         # 统一响应 ApiResponse、全局异常处理
│   ├── config/         # CaffeineConfig(CacheManager+各模块Caffeine Bean)、CachePracticeProperties、OpenAPI、CORS
│   ├── support/        # HotDataService(慢DB模拟)、SimpleRedisCache(Redis模拟)、CacheLogStore
│   ├── basic/          # 01 快速开始
│   ├── eviction/       # 02 淘汰策略
│   ├── refresh/        # 03 刷新与异步
│   ├── stats/          # 04 统计与监控
│   ├── preheat/        # 05 缓存预热（重点）
│   ├── stampede/       # 06 穿透/击穿/雪崩（含手写单飞）
│   ├── twolevel/       # 07 两级缓存（L1 Caffeine + L2 模拟Redis）
│   ├── spring/         # 08 Spring Cache 注解
│   ├── consistency/    # 09 缓存一致性
│   └── pitfall/        # 10 常见坑与调优
├── src/main/resources/application.yml
├── src/test/java/com/example/cache/   # 上下文 + 全场景 + 预热专项 + 单飞并发测试
└── web/                # Vue 3 前端面板（5188）
```

## 快速启动

### 后端

```bash
cd 15-caffeine-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8095/swagger-ui/index.html

启动日志会看到：`应用启动完成，开始自动预热 50 个热门 key ...` 与 `缓存预热完成`——这就是预热场景在真实启动时的样子。

### 前端

```bash
cd 15-caffeine-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5188

### 运行测试

```bash
cd 15-caffeine-practice
mvn test
```

测试含 `PreheatTest`（预热状态机 SUCCESS / 命中率从 0 → 1.0 / 并发预热幂等）与 `StampedeSingleFlightTest`（无保护 10 并发打库 10 次 vs 单飞 20 并发只加载 1 次）。

## 接口速查

### 01. 快速开始 `/api/basic`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/basic/cache-demo?id=1` | GET | 手动 Cache 全流程：miss → 查库 → put → hit → invalidate |
| `/api/basic/loading?id=1` | GET | LoadingCache + CacheLoader 自动加载回填 |
| `/api/basic/info` | GET | Caffeine 是什么 / 与 Guava、Redis 对比 |

### 02. 淘汰策略 `/api/eviction`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/eviction/size-demo?count=12` | GET | maximumSize=5 容量淘汰，看淘汰数/存活 key |
| `/api/eviction/expire-demo?type=write&durationMs=150` | GET | expireAfterWrite vs expireAfterAccess：读不续命 vs 读续命 |
| `/api/eviction/explain` | GET | 容量 / 时间 / 引用三类策略速记 |

### 03. 刷新与异步 `/api/refresh`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/refresh/refresh-demo?waitMs=0` | GET | refreshAfterWrite 到期后读不阻塞、后台异步刷新 |
| `/api/refresh/async-demo?id=1` | GET | AsyncCache 返回 CompletableFuture，加载在线程池 |
| `/api/refresh/explain` | GET | refresh vs expire / 黄金组合 / 异步注意点 |

### 04. 统计与监控 `/api/stats`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/stats/demo?accesses=500` | GET | 跑一批访问，返回命中率/查库次数/加载耗时 |
| `/api/stats/explain` | GET | recordStats / 指标含义 / 监控告警 |

### 05. 缓存预热（重点） `/api/preheat`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/preheat/status` | GET | 预热状态 PENDING/RUNNING/SUCCESS/FAILED，含 key 数与耗时 |
| `/api/preheat/warm` | POST | 手动触发预热（幂等，可重复触发） |
| `/api/preheat/stats` | GET | 预热前后命中率对比（探测读） |
| `/api/preheat/config` | GET | 预热开关 / key 数 / 批次 / 容量 / 过期 |
| `/api/preheat/explain` | GET | 为什么预热 / 静态与动态 / 时机与兜底 |

### 06. 穿透/击穿/雪崩 `/api/stampede`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/stampede/overview` | GET | 三大问题现象与应对表 |
| `/api/stampede/null-demo?times=50` | GET | 空值缓存防穿透：不缓存 vs 缓存对比打库次数 |
| `/api/stampede/stampede-demo?threads=20` | GET | 击穿现场：热点 key 过期瞬间 N 线程全打 DB |
| `/api/stampede/singleflight?threads=20` | GET | 单飞保护：N 并发只加载 1 次 |
| `/api/stampede/explain` | GET | 单飞实现 / 逻辑过期 / 注意点 |

### 07. 两级缓存 `/api/twolevel`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/twolevel/get?id=1` | GET | 读路径 L1 → L2 → DB，命中哪级从哪级回填 |
| `/api/twolevel/put?id=1&name=&dept=` | POST | 写路径 Cache Aside：先更库再删 L1+L2 |
| `/api/twolevel/evict?id=1` | POST | 删两级缓存，强制下次读走 DB |
| `/api/twolevel/consistency` | GET | Cache Aside / 双删 / 延迟双删 / 读写穿 |
| `/api/twolevel/explain` | GET | 两级缓存速记与注意点 |

### 08. Spring Cache 注解 `/api/spring`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/spring/query?id=10` | GET | @Cacheable：连读两次看打库次数 |
| `/api/spring/update?id=10&name=` | POST | @CachePut：更新 DB 并回写缓存 |
| `/api/spring/delete?id=10` | POST | @CacheEvict：剔除缓存 key |
| `/api/spring/multi?id=10` | POST | @Caching：一次清两个缓存 |
| `/api/spring/explain` | GET | 注解速记与代理注意点 |

### 09. 缓存一致性 `/api/consistency`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/consistency/aside-demo?id=2` | GET | Cache Aside 现场：只更库不删缓存 → 脏数据 |
| `/api/consistency/double-delete-demo?id=3` | GET | 双删：写前删 + 写后删压掉竞态窗口 |
| `/api/consistency/patterns` | GET | Cache Aside / 读写穿 / 写回 对比 |
| `/api/consistency/explain` | GET | 为什么删缓存不更新 / 双删与 binlog |

### 10. 常见坑与调优 `/api/pitfall`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pitfall/list` | GET | 10 个高频坑：现象 → 原因 → 解法 |
| `/api/pitfall/key-demo` | GET | SpEL key 陷阱现场：等价对象因 toString 不同缓存永远 miss |
| `/api/pitfall/tuning` | GET | 容量 / TTL / 预热 / 监控 / 大促三件套 |

## 缓存预热详解（本模块重点）

**为什么需要预热**：冷启动时缓存是空的，前 N 个请求全部 miss、全部打到数据库——这是「缓存击穿」的最初形态，秒杀/大促/每日高峰最怕这个。预热就是在流量进来之前，把最可能被访问的 key（热门商品、字典、配置）主动加载进缓存。

**本模块的实现**（`preheat/CachePreheatService`）：

- 监听 `ApplicationReadyEvent`（应用完全就绪后）自动预热，而不是在启动过程中阻塞；
- `AtomicBoolean` 保证「正在预热中」的幂等，并发触发直接忽略；
- 分批加载（batchSize），避免一次性阻塞太久；
- 记录预热前真实探测命中数（冷启动 0/20）与预热后探测命中数，量化收益；
- 状态机 `PENDING → RUNNING → SUCCESS / FAILED`，前端随时可查。

> 细节：Caffeine 的 `stats().hitRate()` 在「零请求」时返回 1.0（乐观约定），会掩盖冷启动真相，所以本模块用**真实探测读**（getIfPresent 计数 0/20 → 20/20）来对比，别被 stats 的 1.0 误导。

**验证路径**：启动看日志 → 前端「预热状态」应为 SUCCESS → 「预热收益对比」看探测命中从 0/20 提升到 20/20 → 手动再点一次 warm 观察幂等。

## 面试八股

### Caffeine 和 Guava Cache 什么关系？为什么选 Caffeine？

Caffeine 是 Guava Cache 作者的续作，核心是 **Window-TinyLFU** 淘汰算法（LRU + LFU 融合 + 窗口区），在并发吞吐和命中率上都优于 Guava。新项目直接用 Caffeine，Spring Boot 通过 `CaffeineCacheManager` 原生集成。

### Caffeine 和 Redis 怎么选？

Caffeine 是**进程内缓存**：最快（纳秒级）、每个实例各有一份、天然不一致；Redis 是**分布式缓存**：跨实例共享、一致性好，但要网络 RTT。热点数据优先本地（L1），共享数据放 Redis（L2），两级缓存组合（07 章）。

### 淘汰策略有哪些？区别？

1. **容量** `maximumSize/maximumWeight`：满了按 Window-TinyLFU 淘汰低频 key，O(1) 维护、命中率优于 LRU；
2. **时间** `expireAfterWrite`（写后固定过期，读不续命）/ `expireAfterAccess`（访问续命）/ `expireAfter`（自定义）；
3. **引用** `weakKeys/weakValues/softValues`：交给 GC，不推荐常用。
> 真实业务 TTL 几乎都是 `expireAfterWrite + refreshAfterWrite`，「访问续命」容易让冷数据赖着不走。

### expireAfterWrite 和 refreshAfterWrite 的区别？

- `expireAfterWrite`：过期后下次读必 miss、要重新加载，慢；
- `refreshAfterWrite`：过期后**旧值仍可用**，后台异步刷新新值，读不阻塞——配合较长的 expireAfterWrite 是「防击穿又保新鲜」的黄金组合。刷新只在「读已存在的 key」时触发，miss 仍走正常加载。

### Cache 和 LoadingCache 的区别？

`Cache` 只管存储，miss 后由你决定加载；`LoadingCache` 绑定 `CacheLoader`，`get(key)` 未命中自动加载并回填，且**并发下同一 key 只加载一次（自带单飞）**。

### 缓存穿透 / 击穿 / 雪崩是什么？怎么解决？

- **穿透**：查「缓存和库都没有」的 key，每次都打 DB。→ 空值缓存（短 TTL）+ 参数校验 + 布隆过滤器；
- **击穿**：热点 key 正好过期，瞬间大量请求同时 miss。→ 单飞 / 逻辑过期 / 预热（本模块 05/06 章现场复现）；
- **雪崩**：大量 key 同时过期或缓存服务宕机。→ TTL 加随机抖动 + 多级缓存 + 限流熔断 + 缓存高可用。
> 一句话：穿透是「挡」，击穿是「合并」，雪崩是「错峰 + 兜底」。

### 单飞（single-flight）怎么实现？

同一 key 的并发加载只放一个去执行，其余线程等待同一个结果。经典实现：`ConcurrentHashMap<String, CompletableFuture>` + `putIfAbsent`，加载者完成后用 `remove(key, future)`（带值校验）清理占位。**注意：mapping function 内绝不能修改 map**（computeIfAbsent 的坑，本模块踩过并修复）。Caffeine LoadingCache.get 底层就是这个思路；跨进程用分布式锁 + 双检。

### 两级缓存怎么设计？一致性怎么保证？

读路径 L1(Caffeine) → L2(Redis) → DB 逐级回填；写路径 **Cache Aside**：先更库、再删 L1 和 L2（删比更新安全——更新有顺序竞态，删了最多多查一次库）。再叠加**双删**（写前删 + 写后延迟删）压掉「读旧值回填」的竞态窗口；最强方案是 **binlog 订阅**（Canal/CDC）监听 DB 变更同步删缓存。记住：缓存与 DB 永远存在不一致窗口，能做的是收窄它 + 短 TTL 兜底。

### 缓存预热怎么做？有什么坑？

- 静态：启动时把固定清单（字典/配置/热榜）put 进缓存；动态：启动时查 DB 拿热门 key 清单再逐批加载；定时：周期任务在流量高峰前刷新。
- 时机：用 `ApplicationReadyEvent`（应用就绪后）而不是启动过程中硬等，别阻塞就绪探针；异步/分批执行；
- 坑：预热数量别超过 `maximumSize`（热一轮又被淘汰=白干）；失败要记录告警（miss 时加载兜底）；配合 `refreshAfterWrite` 让热门数据长期新鲜。
- 本质：把 miss 从流量高峰期提前到空闲期——换时间，不换总工作量。

### @Cacheable / @CachePut / @CacheEvict 的区别？

- `@Cacheable`：读，命中返回缓存，miss 执行并回写；
- `@CachePut`：写，每次都执行并回写缓存；
- `@CacheEvict`：删，执行后剔除指定 key（allEntries 清空整缓存）；
- `@Caching`：组合多个注解；`@CacheConfig`：类级别统一定缓存名/key。
> **坑**：注解靠 Spring 代理拦截，**自调用 `this.xxx()` 不生效**，必须通过注入的 Bean 调用。

### Caffeine 内存怎么预估/调优？

每 key 平均几十字节 × 容量 = 常驻堆内存，多实例各一份。调优顺序：**先看命中率 → 再看淘汰率 → 最后动容量/TTL**。命中率 <90% 通常意味着 TTL 太短 / 键太散 / 容量不足。生产用 `recordStats()` + Micrometer 接 Prometheus/Grafana，命中率骤降要告警。

## 推荐实验顺序

1. 启动后端与前端，看启动日志里**自动预热**输出，先点 **05 缓存预热**：状态 → 收益对比 → 手动再 warm。
2. **01 快速开始**：cache-demo 看 miss/hit 耗时差 → loading 看自动加载与 DB 只查一次。
3. **02 淘汰策略**：容量淘汰放 12 个 → 时间淘汰对比 write/access。
4. **03 刷新与异步**：refresh-demo（waitMs 选 3000 看刷新）→ async-demo。
5. **04 统计**：跑 500 次访问看命中率与查库次数。
6. **06 穿透击穿雪崩**（重点）：null-demo → stampede-demo（N 线程全打库）→ singleflight（只查 1 次）。
7. **07 两级缓存**：get 连点几次看来源从 DB→L2→L1 → put/evict 看写路径。
8. **08 Spring Cache 注解**：query → update → delete → multi，看打库次数变化。
9. **09 缓存一致性**：aside-demo 看脏数据 → double-delete-demo 看双删时序。
10. **10 常见坑**：通读清单；亲手跑 key-demo 看 SpEL key 陷阱。

## 参考

- [Caffeine 官方文档（GitHub）](https://github.com/ben-manes/caffeine)
- [Caffeine Cache 设计（Window-TinyLFU 论文）](https://github.com/ben-manes/caffeine/wiki/Design)
- [Spring Boot Caching 官方文档](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/features.html#features.caching)
- [Spring Framework @Cacheable 注解文档](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
