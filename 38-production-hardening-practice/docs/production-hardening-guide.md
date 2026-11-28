# 生产加固完整指南（第38章配套）

> spec 八节的完整展开，所有条目附本章代码/配置位置。

## 一、密钥与敏感信息

- **原则**：密码、密钥、API Key 一律环境变量注入（`${ENV:default}`），仓库零密文。本章实证：`application.yml` 的 `DB_PASSWORD`/`JWT_SECRET`（`application.yml:7-9, 20-22`）。
- **JWT_SECRET 强度**：HS256 要求 ≥ 256bit（32 字节），`JwtService` 构造器显式校验并启动即报错（`security/JwtService.java:33-35`）。
- **生产禁用的东西**：devtools（本工程未引入）、H2 控制台（未引入）、actuator 全暴露（prod 收紧）。启动参数：`DB_PASSWORD=xxx JWT_SECRET=$(openssl rand -hex 32) -Dspring-boot.run.profiles=prod`。

## 二、认证与授权（Spring Security + JWT）

三件套及落点：

1. **JWT 无状态认证**：`JwtAuthFilter` 解析 Bearer → 校验签名/过期 → 黑名单检查 → `SecurityContextHolder.setAuthentication`。**finally `clearContext()`**——SecurityContext 线程绑定，Tomcat 线程复用不清理会串号/越权（`security/JwtAuthFilter.java:70-73`）。
2. **方法级权限**：`@EnableGlobalMethodSecurity(prePostEnabled = true)`（`config/SecurityConfig.java`）+ Service 层 `@PreAuthorize("hasAuthority('order:write')")`（`service/OrderService.java`）。权限在 Service 层，Controller 绕不过。
3. **Token 撤销**：登出把 jti 写黑名单，TTL=剩余有效期（`security/TokenBlacklist.java`）。生产换 Redis：`SET blacklist:{jti} 1 EX {remaining}`，接口不变。

## 三、传输与接口安全

- **HTTPS**：网关/Nginx 443 终结 + HSTS；应用层终结在 SecurityConfig 开 `requiresChannel`（`application-prod.yml` 注释示例）。
- **限流**：`RateLimitFilter` Bucket4j 按 IP 令牌桶，前置在 JWT 之前（限流不消耗认证计算），429 + Retry-After。集群换 Redis 版 bucket4j。
- **CORS**：`allowedOrigins` 指定 `http://localhost:5173`，禁止 `*`（`config/SecurityConfig.corsSource()`）。

## 四、可观测性

- **结构化日志**：默认文本模式；`structured` profile 用 LogstashEncoder 输出 JSON，MDC 的 traceId 自动成为 JSON 字段，ELK/Loki 直接采集（`logback-spring.xml`）。
- **健康检查**：`/actuator/health` 供 K8s 探针；自定义 `DatabaseHealthIndicator`（SELECT 1）作为 db 组件（`observability/DatabaseHealthIndicator.java`）。K8s 的 liveness/readiness 应分离：liveness 轻量（进程活）、readiness 带依赖检查。
- **指标**：`micrometer-registry-prometheus` 暴露 `/actuator/prometheus`，Grafana 配 JVM/HTTP 面板。
- **动态日志级别**：`/actuator/loggers/{logger}` 即改即生效；生产不暴露 loggers 端点，排障时临时加白名单（`application-prod.yml` 注释）。

## 五、API 文档与契约

- springdoc-openapi 1.7（springfox 已停维护）生成 OpenAPI3；dev 访问 `/swagger-ui.html`。
- **prod 关闭**：`springdoc.api-docs.enabled=false` + `swagger-ui.enabled=false`（`application-prod.yml`），防接口信息泄露。
- **契约优先**：Spring REST Docs 用测试生成片段再组装，文档与代码强一致——适合对外 API；内部系统 springdoc 足够。

## 六、测试策略（金字塔）

| 层级 | 占比 | 本章实证 | 运行条件 |
| --- | --- | --- | --- |
| 单元 | 70% | `unit/`：JwtService（签发/篡改/错密钥）、TokenBlacklist（撤销/过期/TTL 边界）、RateLimitFilter（容量/429/多 IP） | 任何机器 |
| 集成 | 20% | `integration/OrderIntegrationTest`：Testcontainers 真实 MySQL 跑 登录→@PreAuthorize→黑名单 | 需 Docker，无则自动 skip（`disabledWithoutDocker=true`） |
| 端到端 | 10% | `e2e/SecurityChainE2ETest`：RANDOM_PORT + MockMvc 验安全链（401/200/黑名单/白名单） | 任何机器 |

Testcontainers 价值：SQL 方言、schema 迁移、事务、TTL 面对真实数据库验证，杜绝「本地通过、生产爆炸」。CI 里装 Docker 即全量跑。

## 七、依赖治理与混沌工程

- **OWASP**：`mvn dependency-check:check` 生成 HTML/JSON 报告（`failBuildOnCVSS=7`），挂 CI 定时跑；漏洞处置：升级版本 > 换组件 > 隔离。
- **混沌工程**：`chaos-monkey-spring-boot` 默认 `enabled=false`，演练环境 `CHAOS_MONKEY_ENABLED=true` 开启，注入延迟（1~3s）/异常。演练场景清单：DB 不可用、Redis 故障、下游 HTTP 超时、线程阻塞、连接池耗尽——逐项演练并记录系统的熔断/降级/告警表现。

## 八、审计与合规

- 审计框架：Actuator `AuditEventRepository` 接口，本章 `DbAuditEventRepository` 落 MySQL（`config/DbAuditEventRepository.java`）；Spring Security 认证成功/失败事件由 `AuthenticationAuditListener` 自动入库，业务事件（登录/登出/订单增删改）手动 `add()`。
- **append-only**：audit_event 只 INSERT/SELECT，无 UPDATE/DELETE 接口；定期归档 + 完整性校验防篡改（等保要求）。
- 合规基线：敏感数据脱敏（30 章 @Sensitive）、操作审计、加密存储、权限审计。

## 九、灾难恢复与备份

策略表：

| 类型 | 频率 | 保留 | 工具 |
| --- | --- | --- | --- |
| 全量 | 每天 | 30 天 | mysqldump / pg_dump |
| 增量 | 每小时 | 7 天 | binlog / WAL |
| 配置 | 每次变更 | 永久 | Git |
| 镜像 | 每次发布 | 最近 5 版 | 镜像仓库 |

`docs/scripts/backup-mysql.sh` 示例（本章附）：

```bash
#!/usr/bin/env bash
# 生产备份脚本：全量 + binlog 增量，保留策略用 find 清理
set -euo pipefail
BACKUP_DIR=/data/backup/mysql
DB=prod_db
TS=$(date +%F-%H%M)
mysqldump -u backup -p"$DB_PASSWORD" --single-transaction --routines "$DB" \
  | gzip > "$BACKUP_DIR/full-$DB-$TS.sql.gz"
# 每小时增量：binlog 由 mysqlbinlog 归档（略）
# 保留 30 天
find "$BACKUP_DIR" -name "full-*.sql.gz" -mtime +30 -delete
echo "backup done: full-$DB-$TS.sql.gz"
```

**恢复演练清单**（每季度一次）：挑最近备份 → 在隔离环境恢复 → 核对表数量/校验和 → 抽查业务数据 → 记录恢复耗时 RTO 与数据丢失 RPO 实际值。**备份不是目的，能恢复才是。**

## 十、生产上线 Checklist（30 项）

**密钥与配置**
- [ ] 仓库 `git secrets` 扫描无密文残留
- [ ] 所有密钥走环境变量/密钥管理（Vault/KMS）
- [ ] devtools、H2 控制台不在生产 classpath
- [ ] 配置外部化（Nacos/Apollo），本地配置文件不带生产值

**安全**
- [ ] JWT_SECRET ≥ 32 字节且随机生成
- [ ] SecurityContextHolder finally clearContext
- [ ] 方法级 @PreAuthorize 覆盖全部敏感操作
- [ ] 登出黑名单（jti）生效
- [ ] CORS 白名单具体域名，无 `*`
- [ ] 安全响应头：X-Frame-Options/nosniff/HSTS
- [ ] HTTPS 全链路，HTTP 重定向 443
- [ ] 限流按接口分级（登录更严）
- [ ]  actuator 端点最小暴露且管理端口分离
- [ ] springdoc prod 关闭

**可观测**
- [ ] 结构化 JSON 日志输出
- [ ] traceId 全链路（同步/异步/MQ）
- [ ] health 探针区分 liveness/readiness
- [ ] Prometheus 抓取 + Grafana 告警规则
- [ ] 错误率/延迟/P99 告警接入值班

**质量与演练**
- [ ] 单测覆盖率 ≥ 70% 核心逻辑
- [ ] Testcontainers 集成测试进 CI
- [ ] OWASP dependency-check 无高危（CVSS≥7）
- [ ] 混沌演练报告归档
- [ ] 审计 append-only + 定期归档

**数据与发布**
- [ ] 备份策略生效且监控备份任务成败
- [ ] 恢复演练每季度一次，RTO/RPO 达标
- [ ] 发布回滚方案（最近 5 版镜像）
- [ ] 数据库变更脚本可回滚
- [ ] 灰度/金丝雀策略（先 5% 流量）
- [ ] 上线后观察期 24h 值班
