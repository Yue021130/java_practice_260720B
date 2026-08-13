# 17-api-signature-practice：基于 appid + appkey 的 HMAC-SHA256 接口签名鉴权实践

本模块把业界最成熟、最推荐的接口鉴权方案——**HMAC-SHA256 请求签名鉴权**（AWS、阿里云、微信支付同款）——转化为可运行、可交互的 Spring Boot + Vue 3 项目代码。每个环节（签名怎么算、篡改怎么被拒、重放怎么被拦、拦截器怎么落地）都是真实 JVM 上能跑出结果、能动手验证的实验，前端面板一键运行，便于系统学习。

**开箱即用**：不需要数据库、不需要 Redis（内存模拟）、不需要外部服务，直接 `mvn spring-boot:run` 就能玩。

> 🔑 一句话记住这套方案：**用 appkey 把「请求关键信息 + 时间戳 + nonce」做成带密钥的指纹（HMAC-SHA256），服务端重算比对——appkey 不传输、重放被拦、篡改必现。**

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 加密：JDK 自带 `javax.crypto.Mac(HmacSHA256)` + `MessageDigest`（SHA-256 / MD5 / isEqual），零额外依赖
- 测试：JUnit 5 + MockMvc + AssertJ（含「未带签名 → 401」「篡改 → 拒绝」断言）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS（面板支持「运行实验 / 观察 JSON 结果」卡片）
- 端口：后端 **8097**，前端 **5190**

## 核心：HMAC-SHA256 请求签名鉴权（本模块的知识骨架）

| 要素/环节 | 说明 |
| --- | --- |
| **appid** | 应用唯一标识，可公开传输（`X-App-Id`），用于定位 appkey |
| **appkey** | 应用密钥，**绝不传输**，仅服务端签名计算用 |
| **签名 Signature** | `HMAC-SHA256(appkey, CanonicalString)`，客户端传签名、服务端验签 |
| **Canonical String** | 9 字段按固定顺序 `\n` 拼接：HTTPMethod / Content-MD5 / Content-Type / Timestamp / Nonce / CanonicalURI / CanonicalQueryString / CanonicalHeaders / HashedPayload |
| **防重放** | 时间戳 ±5 分钟窗口（挡老请求）+ nonce 去重（Redis `SETNX`+TTL，挡窗口内重放） |
| **完整性** | uri / query / body（HashedPayload）都参与签名，篡改任意一处 → 签名失配 |
| **安全比对** | `MessageDigest.isEqual` 常量时间比较，防时序攻击 |
| **落地** | `@RequireSign` 注解 + `SignAuthInterceptor` 拦截器统一鉴权，业务接口零侵入 |

## 模块结构

```
17-api-signature-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/sign/
│   ├── ApiSignaturePracticeApplication.java
│   ├── common/         # 统一响应 ApiResponse、全局异常处理
│   ├── config/         # SignPracticeProperties、OpenAPI、CORS、@RequireSign、拦截器、WebMvc 注册
│   ├── signature/      # HmacSignService（签名引擎）+ SignVerifier（统一校验器）
│   ├── support/        # AppKeyStore（appid→appkey）、NonceStore（防重放）、SignLogStore
│   ├── principle/      # 01 核心原理：三要素 / 鉴权流程 6 步 / 与 API Key 对比
│   ├── sign/           # 02 签名计算：Canonical String 9 字段 + HMAC-SHA256
│   ├── verify/         # 03 服务端验签：篡改任一字段 → 签名失败
│   ├── timestamp/      # 04 防重放-时间戳：±窗口校验
│   ├── nonce/          # 05 防重放-nonce：重复使用拒绝
│   ├── body/           # 06 请求体完整性：Content-MD5 / HashedPayload
│   ├── canonical/      # 07 规范化：query / headers 排序规则
│   ├── simplified/     # 08 简化版方案：appid+timestamp+nonce+uri+params
│   ├── interceptor/    # 09 拦截器实战：@RequireSign + 受保护接口
│   └── summary/        # 10 选型对比：HMAC vs API Key vs JWT vs OAuth
├── src/main/resources/application.yml
├── src/test/java/com/example/sign/   # 上下文测试 + 全场景接口测试（含 401/200）
└── web/                # Vue 3 前端面板（5190）
```

## 快速启动

### 后端

```bash
cd 17-api-signature-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8097/swagger-ui/index.html

### 前端

```bash
cd 17-api-signature-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5190

### 运行测试

```bash
cd 17-api-signature-practice
mvn test
```

## 接口速查

### 01. 核心原理 `/api/principle`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/principle/elements` | GET | appid / appkey / 签名三要素速记 |
| `/api/principle/flow` | GET | 鉴权流程 6 步（组装→签名→发请求→查key→重算→比对） |
| `/api/principle/vs-apikey` | GET | 签名 vs 简单 API Key：三个优势 |
| `/api/principle/explain` | GET | 为什么大厂都选 HMAC-SHA256（八股） |

### 02. 签名计算 `/api/sign`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/sign/compute?method=&uri=&query=&body=` | GET | 完整签名计算：9 字段 Canonical String + HMAC-SHA256（前端可对照复算） |
| `/api/sign/canonical` | GET | Canonical String 9 字段拆解 |
| `/api/sign/verify-manual` | GET | 手工验签对照：给定 appkey/待签串/签名，重算比对 |
| `/api/sign/explain` | GET | 为什么固定顺序 / 空字段规则（八股） |

### 03. 服务端验签 `/api/verify`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/verify/demo?tamper=none\|body\|timestamp\|uri\|query` | GET | 正确签名通过；篡改任一字段 → 签名失败 |
| `/api/verify/explain` | GET | 验签 5 步 / 为什么最后才做 HMAC（八股） |

### 04. 防重放-时间戳 `/api/timestamp`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/timestamp/demo?timestamp=now\|-3600\|+3600` | GET | ±窗口校验：过期 / 未来请求拒绝 |
| `/api/timestamp/explain` | GET | 时间戳 vs nonce 分工（八股） |

### 05. 防重放-nonce `/api/nonce`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/nonce/demo?nonce=xxx` | GET | 第一次占用成功，同一 nonce 第二次 → 拒绝 |
| `/api/nonce/explain` | GET | Redis SETNX+TTL / 为什么必须服务端记忆（八股） |

### 06. 请求体完整性 `/api/body`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/body/demo?body=...&tamper=true\|false` | GET | body 参与签名：篡改 body（金额 20→9999）→ 完整性失配 |
| `/api/body/explain` | GET | Content-MD5 vs HashedPayload 取舍（八股） |

### 07. 规范化 `/api/canonical`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/canonical/query-sort` | GET | 乱序参数规范化后一致（为什么必须排序） |
| `/api/canonical/headers-sort` | GET | 头名小写字典序拼接 |
| `/api/canonical/uri-encoding` | GET | 路径编码规则 / + vs %20 的坑 |
| `/api/canonical/explain` | GET | 规范化意义 / 常见 bug（八股） |

### 08. 简化版方案 `/api/simplified`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/simplified/demo?uri=&params=` | GET | 简化签名：appid+timestamp+nonce+uri+排序参数 |
| `/api/simplified/explain` | GET | 什么时候能简化 / 与标准版取舍（八股） |

### 09. 拦截器实战 `/api/interceptor`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/interceptor/generate` | GET | 生成合法签名（模拟客户端），返回 4 个 X- 头 |
| `/api/interceptor/secure-demo?tamper=false\|true` | GET | 验签闭环：统一校验器通过 / 篡改拒绝（与拦截器同代码） |
| `/api/interceptor/protected` | GET | **受保护接口（@RequireSign）**：未带签名 401，携带 200 |
| `/api/interceptor/explain` | GET | 注解+拦截器机制 / 与过滤器、AOP、网关对比（八股） |

### 10. 选型总结 `/api/summary`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/summary/overview` | GET | 接口鉴权方案全景（HMAC / JWT / OAuth / API Key） |
| `/api/summary/compare` | GET | 四种方案对比表（安全性/复杂度/适用） |
| `/api/summary/principles` | GET | 三个关键原则 + 生产底线 |
| `/api/summary/pitfalls` | GET | 签名对不上的 5 大排查点 / 生产落地建议 |
| `/api/summary/explain` | GET | 完整回答「接口鉴权怎么做」的 7 步（八股） |

## 面试八股

### 接口鉴权怎么做？为什么选 HMAC-SHA256 签名？

1. **定位**：接口与接口之间（机器对机器）的身份校验 + 防篡改 + 防重放，业界通用首选（AWS SigV4、阿里云、微信支付同款）；
2. **三要素**：`appid`（公开定位应用）、`appkey`（秘密，绝不传输，仅服务端算签名）、`签名`（HMAC-SHA256 结果）；
3. **为什么不是直接传 key**：API Key 会被抓包/日志泄露、可无限重放、无法校验请求是否被改；HMAC 全程只传输签名结果，appkey 不出网。

### HMAC-SHA256 和普通 SHA256 有什么区别？

SHA256 是无密钥哈希：任何人知道内容都能算出来，改了内容自己重算即可，防不住伪造。HMAC-SHA256 是**带密钥**的哈希：`HMAC(key, msg)`，没有 appkey 就算不出合法签名——既能校验完整性，又能证明「是持有 appkey 的一方签的」。

### 待签名字符串（Canonical String）怎么拼？

9 个字段按**固定顺序**用 `\n` 拼接：
```
HTTPMethod + "\n" + Content-MD5 + "\n" + Content-Type + "\n" + Timestamp + "\n" +
Nonce + "\n" + CanonicalURI + "\n" + CanonicalQueryString + "\n" + CanonicalHeaders + "\n" + HashedPayload
```
无 body：Content-MD5/Content-Type 填空串、HashedPayload 用空串的 SHA256；无 query：CanonicalQueryString 为空串。**顺序、分隔符、空字段规则必须两端一致**，否则签名永远对不上。

### 为什么查询参数和请求头要排序？

签名比对的是字符串。同一组参数顺序不同（`size=20&page=1` vs `page=1&size=20`）拼出的串就不同。**排序（字典序）后，无论客户端以什么顺序发来，服务端都能算出同一个串**。不排序是签名对不上的第一大原因。

### 防重放为什么是「时间戳 + nonce」双保险？

- **时间戳**：挡「很久以前被截获的请求重放」——超出 ±5 分钟窗口直接拒绝，成本极低；
- **nonce**：挡「窗口内立刻重放」——服务端记录用过的 nonce（Redis `SET nonce:x 1 EX 300 NX`），第二次用同一 nonce 直接拒绝。
- 只靠时间戳：窗口内可无限重放；只靠 nonce：没有时间上限、存储无限膨胀。**两者配合**。

### 请求体怎么防篡改？

GET 的 uri/query 参与签名后，改它们会被发现；但 POST 的 body 若不参与，攻击者把转账金额 20 改成 9999 服务端毫无察觉。两种做法：**Content-MD5**（请求头带 body 的 MD5，服务端重算比对，无密钥可被伪造）+ **HashedPayload**（body 的 SHA256 拼进 Canonical String 参与 HMAC，与 appkey 绑定，更强）。生产建议 HashedPayload 纳入签名。

### 签名比对为什么不能用 == 或 equals？

`==`/`equals` 逐字符比较，一旦某位不同立即返回——攻击者可通过**响应耗时**逐位猜出签名（时序攻击）。必须用**常量时间比较**（Java 的 `MessageDigest.isEqual`、Python 的 `hmac.compare_digest`）：无论差几位，耗时都相同。

### 服务端校验顺序怎么安排？

提取四要素 → 时间戳校验 → nonce 去重 → 查 appkey → 重算签名比对。**先做廉价的拒绝**（时间戳/nonce/查key），最后才做昂贵的 HMAC 计算；全部通过才放行。

### 校验逻辑放在哪？拦截器 vs 过滤器 vs 网关？

- **拦截器（Interceptor）**：能拿到 `HandlerMethod` 读注解（本模块用 `@RequireSign`），适合单机业务鉴权；
- **过滤器（Filter）**：更早介入（Servlet 层），能拦静态资源；
- **AOP**：注解 + 切面也可行，但拿 request/response 不如拦截器方便；
- **网关（Gateway）**：微服务下的统一入口，一次鉴权服务全部下游，签名逻辑下沉到网关。**单机用拦截器、微服务用网关**，业务代码零侵入。

### 和 JWT、OAuth 怎么区分？

- **HMAC 签名**：接口与接口之间的鉴权（机器对机器），通用首选；
- **JWT**：用户登录态（人），携带身份声明，无状态可扩展；
- **OAuth 2.0**：第三方开放平台授权，流程重但规范；
- **简单 API Key**：内部低敏接口。**一句话：机器对机器用 HMAC，人对机器用 JWT，第三方用 OAuth。**

## 推荐实验顺序

1. 启动后端与前端，先点 **01 核心原理**：三要素 + 鉴权流程 6 步，建立整体认知。
2. **02 签名计算**：跑 compute 拿到完整 Canonical String 和签名，可以拿任意在线 HMAC-SHA256 工具对着复算验证——签名真的是这么算的。
3. **03 服务端验签**：依次点 `tamper=none / body / timestamp / uri / query`，亲眼看到篡改任一字段签名立刻失配。
4. **04 / 05 防重放**：时间戳窗口（过期/未来拒绝）→ nonce 去重（同一 nonce 第二次被拒），理解双保险。
5. **06 请求体完整性**：跑 body 演示，看金额 20→9999 时 Content-MD5 与 HashedPayload 双双失配。
6. **07 规范化**：看乱序参数排序后一致、头排序规则，理解「为什么必须排序」。
7. **08 简化版**：对比 5 要素 vs 9 字段，理解取舍。
8. **09 拦截器实战（重点！）**：先点 generate 拿签名 → 点 protected 自动携带签名调用（200）→ 对比 secure-demo 篡改被拒；理解 `@RequireSign` 注解 + 拦截器这套生产落地方式。
9. **10 选型总结**：对比表 + 三个关键原则 + 排查清单，串起全模块。

## 参考

- [HMAC（RFC 2104）规范](https://datatracker.ietf.org/doc/html/rfc2104)
- [AWS Signature Version 4 签名流程（HMAC 系参考实现）](https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html)
- [Java 密码学：javax.crypto.Mac（HmacSHA256）](https://docs.oracle.com/javase/8/docs/api/javax/crypto/Mac.html)
- [MessageDigest.isEqual（常量时间比较）](https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html#isEqual-byte:A-byte:A-)
- [RFC 3986：URI 编码规范](https://datatracker.ietf.org/doc/html/rfc3986)
