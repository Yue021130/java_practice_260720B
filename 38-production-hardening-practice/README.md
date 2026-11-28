# 第38章：生产级 Spring Boot 加固实战

## 一、业务背景

功能能跑只是及格线，能上线才是工程。本章把一个「登录 + 订单 CRUD」的最小业务，按生产标准做全链路加固：**密钥外置、Spring Security + JWT 认证授权（含黑名单撤销）、接口限流、CORS 白名单、JSON 结构化日志与 traceId、Actuator 健康检查与 Prometheus、springdoc 按 Profile 开关、测试金字塔（Testcontainers）、OWASP 依赖扫描、混沌工程、审计落库与灾备策略**——spec 八节全部有代码或配置实证。

演示账号：**admin**（order:read+write）/ **user01**（仅 order:read），密码均 123456——用 user01 点「新增订单」可看到后端 `@PreAuthorize` 的 403。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18 + Java 8 + Spring Security + jjwt 0.11 |
| ORM | MyBatis-Plus 3.5.5 + MySQL 8.0 |
| 限流 | Bucket4j 7.x（按 IP 令牌桶） |
| 可观测 | Actuator + Micrometer(Prometheus) + logstash-logback-encoder（JSON 日志）+ MDC traceId |
| 混沌工程 | chaos-monkey-spring-boot 2.7.2（默认关闭） |
| 测试 | JUnit5 + Mockito + Testcontainers 1.19.3（无 Docker 自动跳过）+ MockMvc |
| 文档 | springdoc-openapi 1.7（/swagger-ui.html，prod profile 关闭） |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Axios |

## 三、项目结构

```
38-production-hardening-practice/
├── pom.xml                              # OWASP dependency-check 插件
├── docs/
│   ├── production-hardening-guide.md    # 八股完整展开 + 上线 Checklist
│   └── sql/schema.sql
├── src/main/java/com/example/prod/
│   ├── ProdApplication.java
│   ├── security/                        # 【核心一】安全
│   │   ├── JwtService.java              # 签发/校验，jti 声明，密钥环境变量注入
│   │   ├── JwtAuthFilter.java           # Bearer 解析 → SecurityContextHolder，finally clearContext
│   │   ├── TokenBlacklist.java          # 登出撤销（Caffeine，生产换 Redis）
│   │   ├── SecurityUserDetailsService.java
│   │   └── RateLimitFilter.java         # Bucket4j 按 IP 令牌桶，429+Retry-After
│   ├── config/
│   │   ├── SecurityConfig.java          # 无状态/白名单/CORS 域名/安全头/@PreAuthorize 开关
│   │   ├── AuditConfig + DbAuditEventRepository.java  # 审计落库（append-only）
│   │   └── MyBatisPlusConfig.java
│   ├── observability/                   # 【核心二】可观测
│   │   ├── TraceIdFilter.java + TraceContext.java     # traceId 全链路
│   │   └── DatabaseHealthIndicator.java # /actuator/health 的 db 组件
│   ├── controller/service/entity/mapper/dto/        # 登录 + 订单 + 审计查询
│   └── common/                          # BusinessException + GlobalExceptionHandler
├── src/main/resources/
│   ├── application.yml                  # 密钥 ${JWT_SECRET} 外置 + dev 默认值
│   ├── application-prod.yml             # 文档关闭/最小暴露/管理端口分离
│   └── logback-spring.xml               # 文本模式；structured profile 输出 JSON
├── src/test/java/com/example/prod/
│   ├── unit/        # JwtServiceTest / TokenBlacklistTest / RateLimitFilterTest（8 例）
│   ├── integration/ # OrderIntegrationTest（Testcontainers 真实 MySQL，无 Docker 自动跳过）
│   └── e2e/         # SecurityChainE2ETest（401/放行/黑名单/白名单）
└── web/                                 # 登录/订单/加固看板
```

## 四、接口清单

> 开发环境文档：**http://localhost:8080/swagger-ui.html**（prod profile 关闭）。
> 业务接口均需 `Authorization: Bearer <token>`，限流 10 次/分钟/IP。

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| POST | /api/auth/login | 登录（白名单），返回 JWT；失败审计 LOGIN_SUCCESS/失败 401 |
| POST | /api/auth/logout | 登出，jti 进黑名单（Token 立即失效） |
| GET | /api/auth/me | 当前用户 |
| GET/POST/PUT/DELETE | /api/order | 订单 CRUD（Service 层 @PreAuthorize，user01 写操作 403） |
| GET | /api/audit/page | 审计日志分页 |
| GET | /actuator/health | 健康检查（含自定义 db 组件，白名单） |
| GET | /actuator/prometheus | JVM/HTTP 指标（prod 暴露） |
| POST | /actuator/loggers/{logger} | 动态日志级别 |

## 五、运行方式

```bash
mysql -u root -p < docs/sql/schema.sql     # 1. 建库（prod_db）
mvn spring-boot:run                        # 2. 后端（开发默认值密钥）
cd web && npm install && npm run dev       # 3. 前端
```

**模拟生产启动**（密钥全部环境变量注入，不带本地默认值）：

```bash
DB_PASSWORD='真实密码' JWT_SECRET="$(openssl rand -hex 32)" \
  mvn spring-boot:run -Dspring-boot.run.profiles=prod
# prod 下 /swagger-ui.html 404、actuator 只留 health/info/metrics/prometheus（9090 端口）
```

**JSON 结构化日志**（对接 ELK/Loki）：`mvn spring-boot:run -Dspring-boot.run.profiles=structured,dev`

## 六、核心场景说明

### 6.1 认证授权链路

```
请求 → RateLimitFilter（Bucket4j，超限 429）
     → TraceIdFilter（MDC）
     → JwtAuthFilter（Bearer 解析、签名校验、黑名单检查 → SecurityContextHolder；finally clearContext）
     → @PreAuthorize("hasAuthority('order:write')")（Service 层，Controller 绕不过）
     → 业务方法 → 审计事件落 audit_event
```

### 6.2 登出即失效

JWT 无状态的代价是「签发后管不了」——本章用 **jti 黑名单**折中：登出时把 jti 写入黑名单，TTL=Token 剩余有效期。`SecurityChainE2ETest.blacklistedToken` 验证了被撤销的 token 再访问返回 401。生产把 Caffeine 换 Redis（`SET blacklist:{jti} 1 EX {剩余秒数}`），接口不变。

### 6.3 加固看板验证（前端 /hardening）

- 限流演练：连发 15 次请求，观察 200→429 的切换与 Retry-After；
- Actuator 探测：health（含 db 组件 DOWN/UP）、metrics、prometheus；
- 审计日志：登录/登出/订单增删改全部落库，append-only。

## 七、八股速记

1. **密钥管理**：密码/密钥一律 `${ENV:}` 注入；生产禁 devtools/H2 控制台；JWT_SECRET ≥ 32 字节。
2. **JWT 三件套**：Filter 解析+SecurityContextHolder+finally clearContext（线程池污染）；@PreAuthorize 方法级权限；jti 黑名单实现撤销。
3. **传输与接口**：HTTPS 网关终结+requiresSecure 兜底；Bucket4j 限流（集群换 Redis 版）；CORS 禁止 `*`。
4. **可观测**：JSON 日志（LogstashEncoder 自动提升 MDC 字段）+ traceId；health 探针 + 自定义 HealthIndicator；Prometheus 抓 Micrometer 指标；loggers 动态级别。
5. **API 文档**：springdoc 生成 OpenAPI3；prod 通过 Profile 关闭；契约优先可用 Spring REST Docs（测试驱动文档）。
6. **测试金字塔**：单元 70%（Mockito）/ 集成 20%（Testcontainers 真实依赖）/ 端到端 10%（RANDOM_PORT + MockMvc）。
7. **依赖治理与混沌**：OWASP dependency-check 扫 CVE；Chaos Monkey 注入延迟/异常做故障演练（仅演练环境开启）。
8. **审计与灾备**：审计 append-only 落库；敏感数据脱敏（30 章）；备份策略「全量每天+增量每小时」，定期恢复演练——**能恢复才是备份**。

详见 **docs/production-hardening-guide.md**（含 30 项上线 Checklist）。

## 八、测试验证

```bash
mvn clean test
# 必跑 12 例：unit 8（JWT/黑名单/限流）+ e2e 4（401/放行/黑名单/白名单）
# integration 1 例：本机无 Docker 自动 skip（@Testcontainers disabledWithoutDocker）
cd web && npm install && npm run build
```

## 九、作者

Yue021130 <169446203@qq.com>
