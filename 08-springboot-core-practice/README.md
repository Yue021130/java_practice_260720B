# 08 · Spring Boot 核心能力实战

系统覆盖 Spring Boot 核心能力：Starters 与自动装配、配置体系与外部化配置、IOC / Bean 生命周期、缓存抽象（Caffeine 本地缓存 + Redis 分布式缓存双实现）、生产可观测与健康检查。每个知识点都被包装成可一键运行的场景接口，前端面板点击即出结果，后端返回运行数据 + `interviewNote` 面试结论。

## 项目目标

把 Spring Boot 面试中最常被问到的“核心八股”变成 12 个可运行、可对比、可观察的场景。学完本项目，你能够说清楚：

- 为什么引入一个 Starter 就能立刻拥有对应能力？自动装配到底装配了谁？
- `application.yml`、命令行参数、环境变量、Profile 的优先级是怎样的？
- `@ConfigurationProperties` 如何绑定复杂对象并校验？
- Bean 从创建到销毁经历了哪些扩展点？`@ConditionalOn...` 在什么条件下生效？
- `@Cacheable / @CachePut / @CacheEvict` 各自的行为是什么？Caffeine 和 Redis 缓存分别适合什么场景？
- Actuator 暴露了哪些端点？生产环境如何最小化暴露并注意安全？

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok 1.18.30 + springdoc-openapi 1.7.0
- 缓存：Caffeine（本地）+ Spring Data Redis（分布式）
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS）
- 测试：JUnit 5 + MockMvc + AssertJ
- 端口：后端 **8088**，前端开发端口 **5181**
- 基础包：`com.example.sbcore`

## 12 个场景

| 模块 | 场景 | 端点 | 面试考点 |
| --- | --- | --- | --- |
| Starters 与自动装配 | 常见 Starter 与能力清单 | `POST /api/core/starters` | Web / Validation / Cache / Actuator / Redis 分别引入了什么 |
| Starters 与自动装配 | 自动装配 Bean 清单 | `POST /api/core/auto-config-beans` | `META-INF/spring.factories`、`@EnableAutoConfiguration`、条件装配 |
| 配置体系 | 配置优先级与 Profile 切换 | `POST /api/core/config-priority` | 命令行 > 环境变量 > 配置文件 > 默认值；`spring.profiles.active` |
| 配置体系 | `@ConfigurationProperties` 绑定与校验 | `POST /api/core/config-props` | 宽松绑定、`@Validated`、`@NotNull` / `@Range` |
| IOC 与 Bean 生命周期 | Bean 生命周期回调 | `POST /api/core/bean-lifecycle` | `@PostConstruct / @PreDestroy`、`InitializingBean`、`DisposableBean`、执行顺序 |
| IOC 与 Bean 生命周期 | 条件装配场景 | `POST /api/core/conditional` | `@ConditionalOnProperty / OnClass / OnMissingBean` |
| 缓存抽象 | `@EnableCaching` + Caffeine 基础缓存 | `POST /api/core/cache-caffeine-basic` | `@Cacheable` key、condition、unless；Caffeine 本地缓存特性 |
| 缓存抽象 | 缓存注解行为对比 | `POST /api/core/cache-ops` | `@Cacheable / @CachePut / @CacheEvict` 语义与返回值 |
| 缓存抽象 | 缓存命中率与耗时对比 | `POST /api/core/cache-hit` | 命中率、穿透、模拟耗时、缓存失效策略 |
| 缓存抽象 | Redis 分布式缓存 | `POST /api/core/cache-redis` | RedisCacheManager、key 序列化、多实例共享 |
| 缓存抽象 | 本地缓存 vs 分布式缓存 | `POST /api/core/cache-compare` | 一致性、延迟、依赖、适用场景对比 |
| 生产可观测 | Actuator health / info 与安全暴露 | `POST /api/core/actuator` | health、info、端点最小暴露、`management.endpoints.web.exposure.include` |

## 面试八股速记

### Starters 与自动装配

- **Starter** 是“依赖 + 自动配置 + 约定默认配置”的组合包。引入 `spring-boot-starter-web` 就会自动引入 Tomcat、Spring MVC、Jackson 等依赖，并自动配置 `DispatcherServlet`。
- **自动装配入口**：`@SpringBootApplication` 包含 `@EnableAutoConfiguration`，Spring Boot 读取所有 `META-INF/spring.factories` 中 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 指定的配置类。
- **条件装配**：自动配置类上大量使用 `@ConditionalOnClass / @ConditionalOnMissingBean / @ConditionalOnProperty`，只有条件满足才会生效，避免冲突。

### 配置体系

- **配置优先级（从高到低）**：命令行参数 > `SPRING_APPLICATION_JSON` > Servlet 参数 / 环境变量 > `application-{profile}.yml`（指定 profile）> `application.yml` > `@PropertySource` > 默认值。
- **Profile 隔离**：通过 `spring.profiles.active=dev` 激活，`application-dev.yml` 会覆盖 `application.yml` 中的同名配置。
- **`@ConfigurationProperties`**：把前缀相同的配置批量绑定到 Java Bean，支持宽松绑定（`user-name` / `userName` / `USER_NAME`），配合 `@Validated` 做 JSR-303 校验。

### IOC / Bean 生命周期

- **生命周期扩展点（按顺序）**：
  1. 实例化（构造器 / 工厂方法）
  2. 属性赋值（依赖注入）
  3. `Aware` 接口回调（`BeanNameAware`、`ApplicationContextAware` 等）
  4. `BeanPostProcessor.postProcessBeforeInitialization`
  5. `@PostConstruct` / `InitializingBean.afterPropertiesSet()` / 自定义 `init-method`
  6. `BeanPostProcessor.postProcessAfterInitialization`
  7. Bean 就绪使用
  8. 容器关闭时：`@PreDestroy` / `DisposableBean.destroy()` / 自定义 `destroy-method`
- **条件装配**：`@ConditionalOnProperty` 根据配置项决定 Bean 是否注册；`@ConditionalOnClass` 根据类路径；`@ConditionalOnMissingBean` 防止重复注册。

### 缓存抽象

- **启用缓存**：`@EnableCaching` + 引入具体缓存实现依赖（Caffeine / Redis / EhCache）。
- **核心注解**：
  - `@Cacheable`：先查缓存，命中直接返回；未命中执行方法并放入缓存。
  - `@CachePut`：始终执行方法，并把结果更新到缓存（常用于写后更新）。
  - `@CacheEvict`：删除缓存；`allEntries = true` 清空整个 cache，`beforeInvocation` 控制时机。
- **Caffeine 本地缓存**：进程内、低延迟、容量 / 过期策略丰富，适合单机读多写少。
- **Redis 分布式缓存**：多实例共享、支持持久化，但存在网络开销和序列化问题，适合集群环境。
- **缓存穿透**：缓存与数据库都没有的 key 被高频请求。可通过缓存空值、布隆过滤器或接口校验缓解。

### 生产可观测

- **Actuator**：`spring-boot-starter-actuator` 暴露运行期端点。
- **常用端点**：`health`（健康检查）、`info`（应用信息）、`metrics`（指标）、`loggers`（日志级别）。
- **安全注意**：生产环境不要暴露所有端点，应使用 `management.endpoints.web.exposure.include=health,info` 并配合安全框架限制访问；`env`、`heapdump`、`httptrace` 等端点会泄露敏感信息。

## 启动命令

```bash
# 后端（当前项目根目录）
mvn spring-boot:run

# 前端（另开终端）
cd web
npm install
npm run dev
```

浏览器访问 `http://localhost:5181`。

Swagger UI：`http://localhost:8088/swagger-ui.html`

> 如需使用 Redis 缓存场景，请确保本地 Redis 运行在默认端口 6379；Caffeine 场景无需外部依赖。

## 测试

```bash
mvn test -q
```

`ScenarioApiTest` 覆盖全部 12 个场景端点，并对配置绑定、条件装配、缓存命中率、Actuator 端点暴露做了额外断言。

## 缓存选型 Checklist

- [ ] 单实例应用且追求低延迟 → 优先 Caffeine 本地缓存
- [ ] 多实例共享缓存或需要持久化 → 使用 Redis 分布式缓存
- [ ] 是否为热点数据设置了合理的 `key`，避免缓存穿透
- [ ] `@CacheEvict` 是否在写操作后正确清理或更新缓存
- [ ] 是否监控了缓存命中率，避免缓存形同虚设
- [ ] Redis 场景下是否配置了合适的 key / value 序列化方式（推荐 `StringRedisSerializer` + `GenericJackson2JsonRedisSerializer`）
