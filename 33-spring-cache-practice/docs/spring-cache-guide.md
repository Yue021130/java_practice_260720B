# Spring Cache 缓存框架参考指南（第33章配套）

> 本文档是第33章的自写参考文档：八股 + 场景 + 代码位置索引。
> 所有结论都有本章代码实证，可在 `src/main/java/com/example/scp/` 中对照阅读。

## 一、Spring Cache 是什么

Spring Cache 是 Spring 提供的**缓存抽象层**：它只定义规范（`CacheManager` / `Cache` / `@Cacheable` 等注解），不管数据存在哪里。存储侧由 `CacheManager` 的实现决定——`ConcurrentMapCacheManager`（内存 Map）、`CaffeineCacheManager`（本地高性能）、`RedisCacheManager`（远程 Redis），或本章的自定义 `TwoLevelCacheManager`。

好处：业务代码只写 `@Cacheable`，换缓存方案零改动；配合 Spring AOP 自动完成「查缓存 → miss 则执行方法 → 回填缓存」的 Cache Aside 流程。

## 二、注解速查表

| 注解 | 语义 | 典型用法 |
| --- | --- | --- |
| `@Cacheable` | 先查缓存，命中直接返回；miss 执行方法并回填 | 详情查询、分页查询 |
| `@CacheEvict` | 方法执行后**删除**缓存 | 删除数据；`allEntries=true` 清空整个 cacheName |
| `@CachePut` | 方法**必执行**，结果写回缓存 | 更新数据后直接写新值，下次读就是新值 |
| `@Caching` | 多个缓存注解的组合（evict 多个 cacheName） | 更新同时清详情 + 清列表 |

关键属性（SpEL 表达式）：

- `key`：缓存键。默认 `SimpleKeyGenerator` 按参数生成 `SimpleKey` 对象，可读性差，生产建议自定义 `KeyGenerator`（本章 `productPageKeyGenerator` 生成 `"page:1:10:手机:-"`）。
- `condition`：**方法执行前**判断，false 则整个方法不走缓存（常用于「只缓存小页」）。
- `unless`：**方法执行后**判断，true 则**不回填**缓存。经典用法 `unless = "#result == null"`——但注意：不写它时空结果也能进缓存（本章故意不写，用于防穿透，见第五节）。

## 三、二级缓存结构

```
@Cacheable 方法被调用
        │
        ▼
  ┌─ TwoLevelCache.get() ─────────────┐
  │  ① L1 Caffeine（JVM 内，纳秒级）   │ 命中 → 返回
  │  ② L2 Redis（跨进程，毫秒级）      │ 命中 → 回填 L1 → 返回
  │  ③ 两级都 miss                     │ 执行方法体（查库）→ put 两级
  └────────────────────────────────────┘
```

为什么二级：

- **只用本地缓存（Caffeine）**：快，但多实例之间数据不一致，且容量受 JVM 内存限制；
- **只用远程缓存（Redis）**：容量大、一致性好，但每次都要网络往返（亚毫秒级）；
- **二级**：L1 扛住绝大部分读（本章 L1 TTL 只有二级 TTL 的 1/10，靠 evict 保一致），L2 做跨进程共享的兜底。

代价是**一致性复杂**：写操作要同时失效两级。本章用 `@CacheEvict(allEntries=true)` 清列表 + 按 key 清详情，配合 L1 短 TTL 兜底。强一致场景需引入 Canal/MQ 异步同步或缩短 TTL。

代码位置：`cache/TwoLevelCache.java`（核心）、`cache/TwoLevelCacheManager.java`、`config/CacheConfig.java`。

## 四、三大经典问题与本章落点

### 4.1 缓存穿透（查不存在的数据）

恶意用不存在的 id 反复查询，缓存永远不命中，全部打到数据库。

**本章方案**：`ProductService.getById()` 的 `@Cacheable` **故意不写** `unless="#result == null"`——查不到的商品会以 `NullValue` 占位符进缓存，TTL 只有 30 秒（`spring.cache.two-level.null-ttl`）。占位符命中时业务视角返回 null，但 metrics 计入 L1/L2 命中而非 miss——数据库被挡住了。

代码位置：`cache/NullValue.java`、`TwoLevelCache.put/get` 的 unwrap 逻辑。

### 4.2 缓存击穿（热点 key 过期瞬间的并发重建）

某个热点 key 过期，恰好大量并发同时 miss，全部涌向数据库重建缓存。

**本章方案**：`ProductService.hotDetail()` 演示 SETNX 分布式锁：只有一个线程抢到锁回源重建，其他线程自旋等待（最多 2 秒）读缓存，超时兜底直接回源保可用性。

代码位置：`cache/RedisLockClient.java`（教学版，注释里说明生产用 Redisson）、`GET /api/product/hot/{id}`。

### 4.3 缓存雪崩（大量 key 同时过期）

批量加载的 key 若 TTL 相同，会在同一秒集体过期，数据库瞬时被打爆。

**本章方案**：`TwoLevelCache.ttlWithJitter()` —— 实际 TTL = 基础 TTL + `[0, jitter)` 随机抖动。配置在 `application.yml` 的 `spring.cache.two-level.configs.<cacheName>.jitter`。单测 `ttlJitter()` 验证 TTL 落在 `[600, 660)` 区间。

## 五、序列化：为什么值用 GenericJackson2JsonRedisSerializer

| 序列化器 | 优点 | 缺点 |
| --- | --- | --- |
| `JdkSerializationRedisSerializer`（默认） | 无需配置 | 二进制不可读、类必须实现 Serializable |
| `StringRedisSerializer` | 可读 | 只能存字符串 |
| `Jackson2JsonRedisSerializer<T>` | 可读、无类型信息泄露 | 反序列化必须传入 Class，缓存范型对象麻烦 |
| `GenericJackson2JsonRedisSerializer` | 可读、自描述（写 `@class`） | 多写类名、需注册 JavaTimeModule |

本章选第四种，key 用 `StringRedisSerializer`（Redis 客户端里可直接 `scan product::*` 排查）。注意必须 `registerModule(new JavaTimeModule())`，否则 `LocalDateTime` 类型的 `createTime` 反序列化会失败。

代码位置：`config/RedisConfig.java`。

## 六、监控：命中率怎么统计

`TwoLevelCache` 内置 `CacheMetrics`（LongAdder 分段计数，高并发下累加几乎无竞争）：

- **L1 命中**：最快路径，纳秒级；
- **L2 命中**：比 DB 快，且会回填 L1；
- **MISS**：两级都没有，回源数据库——这个数越小越好；
- **命中率** = (L1+L2) / 总请求。

`GET /api/cache/stats` 暴露全部 cacheName 的指标，前端「缓存监控」页可视化。

观察方法：启动后连续刷新「商品管理」页 3~4 次，再看监控页——前几页 MISS 多，之后 L1 命中占绝对大头，控制台 SQL 消失。

## 七、常见坑清单

1. **注解不生效**：忘了 `@EnableCaching`；或方法是**自调用**（this 调用不走 AOP 代理）——必须经 Spring 容器里的代理对象调用。
2. **condition 与 unless 混淆**：condition 在方法执行前判（可阻止方法走缓存但**方法仍会执行**），unless 在方法执行后判（阻止回填）。
3. **分页缓存的 key**：忘记带查询参数导致不同条件命中同一缓存；本章用自定义 KeyGenerator 把参数拼进 key。
4. **allEntries 只清一个 cacheName**：更新操作如果影响多个缓存（详情 + 列表 + 计数），用 `@Caching` 组合多个 `@CacheEvict`。
5. **L1 与 Redis 数据不一致**：本地缓存多实例间不共享，写后读场景要么 evict 兜底、要么缩短 L1 TTL（本章两者都用）。
6. **keys 命令**：生产 Redis 禁用 `KEYS *`（O(n) 阻塞），扫描用 `SCAN` 游标（`TwoLevelCache.clear/l2Keys` 已按此实现）。
