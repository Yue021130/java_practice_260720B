# 12-springboot-mail-practice：Spring Boot 邮件服务实践

本模块把 Spring Boot 官方邮件能力（`spring-boot-starter-mail` + `JavaMailSender`）转化为可运行、可交互的 Spring Boot + Vue 3 项目代码，覆盖**基础文本邮件、富文本 HTML、附件、内联图片、Thymeleaf 模板、异步发送、失败重试、Quartz 定时任务、@EventListener 事件监听、邮件头与编码、常见坑与调优**等完整能力，便于系统学习。

**开箱即用**：默认 `simulate` 模拟发送模式——只构造 `MimeMessage` 并记录内容，不连接任何 SMTP 服务器；把 `application.yml` 中 `mail.practice.mode` 改为 `real` 并填好邮箱授权码即可真实发送。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 邮件：spring-boot-starter-mail（JavaMailSender，javax.mail 1.6）+ Thymeleaf 模板引擎
- 定时任务：spring-boot-starter-quartz（内存 JobStore，无需数据库）
- 事件：Spring 事件机制 @EventListener（@Async / condition / @TransactionalEventListener）
- 测试：JUnit 5 + MockMvc + AssertJ + GreenMail（嵌入式 SMTP 服务器，真实发送验证）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS
- 端口：后端 **8092**，前端 **5185**

## 模块结构

```
12-springboot-mail-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/mail/
│   ├── MailPracticeApplication.java
│   ├── common/         # 统一响应 ApiResponse、全局异常处理
│   ├── config/         # MailConfig（JavaMailSender/线程池/调度器）、OpenAPI、CORS、自定义配置
│   ├── service/        # MailDeliveryService（模拟/真实发送核心）
│   ├── support/        # 邮件记录存储、地址解析、图片生成
│   ├── basic/          # 01 基础邮件
│   ├── html/           # 02 富文本 HTML
│   ├── attachment/     # 03 附件
│   ├── inline/         # 04 内联图片
│   ├── template/       # 05 Thymeleaf 模板
│   ├── async/          # 06 异步发送
│   ├── retry/          # 07 失败重试
│   ├── schedule/       # 08 定时与批量（含 Quartz 定时任务）
│   ├── event/          # 11 事件监听 @EventListener
│   ├── header/         # 09 邮件头与编码
│   └── pitfall/        # 10 常见坑与调优
├── src/main/resources/
│   ├── application.yml
│   └── templates/mail/ # welcome.html / order.html 邮件模板
├── src/test/java/com/example/mail/   # 场景接口测试 + GreenMail 真实发送测试
└── web/                # Vue 3 前端面板
```

## 快速启动

### 后端

```bash
cd 12-springboot-mail-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8092/swagger-ui/index.html

### 前端

```bash
cd 12-springboot-mail-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5185

### 运行测试

```bash
cd 12-springboot-mail-practice
mvn test
```

测试包含两套上下文：
- `ScenarioApiTest`：simulate 模式下跑全部接口（快、无外部依赖）
- `GreenMailRealSendTest`：用 GreenMail 在本机 3025 端口起嵌入式 SMTP 服务器，**真实发送**并校验收件

## 切到真实发送（以 QQ 邮箱为例）

改 `src/main/resources/application.yml`：

```yaml
mail:
  practice:
    mode: real            # simulate → real
    from: your@qq.com     # 与登录账号一致
    host: smtp.qq.com
    port: 465
    username: your@qq.com
    password: your-smtp-auth-code   # QQ 邮箱「设置→账户→开启 SMTP」生成的授权码
    ssl: true             # 465 端口用 SSL
```

163 邮箱对应：`host: smtp.163.com`、`port: 25`、`starttls: true`、`ssl: false`。

## 接口速查

### 01. 基础邮件 `/api/basic`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/basic/text` | POST | 发送纯文本邮件 |
| `/api/basic/multiple` | POST | 多收件人 + 抄送 + 密送 |
| `/api/basic/recent` | GET | 最近发送记录（主题/收件人/大小/耗时） |
| `/api/basic/mode` | GET | 当前发送模式与 SMTP 配置 |

### 02. 富文本 HTML `/api/html`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/html/send` | POST | 发送 HTML 邮件（表格 + 按钮） |
| `/api/html/example` | GET | 返回示例 HTML 源码 |

### 03. 附件 `/api/attachment`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/attachment/csv` | POST | 内存生成 CSV 文本附件 |
| `/api/attachment/image` | POST | PNG 二进制附件 |
| `/api/attachment/limitations` | GET | 各邮箱附件配额说明 |

### 04. 内联图片 `/api/inline`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/inline/send` | POST | 发送带 cid 内联图片的邮件 |
| `/api/inline/compare` | GET | 内联 vs 外链图片对比 |

### 05. Thymeleaf 模板 `/api/template`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/template/welcome` | POST | 渲染 welcome.html 欢迎邮件 |
| `/api/template/order` | POST | 渲染 order.html 订单邮件（th:each） |
| `/api/template/variables` | GET | 模板变量说明 |

### 06. 异步发送 `/api/async`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/async/send` | POST | 异步发送，立即返回 taskId |
| `/api/async/status` | GET | 查询任务状态 PENDING→RUNNING→SENT/FAILED |
| `/api/async/pool` | GET | mailExecutor 线程池实时指标 |

### 07. 失败重试 `/api/retry`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/retry/send` | POST | 带重试发送（failTimes 模拟失败） |
| `/api/retry/strategy` | GET | 固定 / 指数退避 / 抖动策略说明 |

### 08. 定时与批量 `/api/schedule`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/schedule/batch` | POST | 批量发送 N 封并统计成败 |
| `/api/schedule/register` | POST | 注册延迟发送任务 |
| `/api/schedule/list` | GET | 延迟任务列表 |
| `/api/schedule/heartbeat` | GET | @Scheduled 心跳说明 |
| `/api/schedule/quartz/register` | POST | Quartz：Cron 表达式注册定时任务 |
| `/api/schedule/quartz/list` | GET | Quartz：任务/触发器状态与下次触发时间 |
| `/api/schedule/quartz/pause` | POST | Quartz：暂停任务 |
| `/api/schedule/quartz/resume` | POST | Quartz：恢复任务 |
| `/api/schedule/quartz/delete` | POST | Quartz：删除任务 |
| `/api/schedule/quartz/explain` | GET | Quartz 概念速记 |

Quartz 演示：注册 `0/30 * * * * ?` 的任务，simulate 模式下每 30 秒触发一次并在「最近发送记录」里看到邮件；或把 `mail.practice.quartz-demo` 设为 `true` 启动即注册演示任务。

### 11. 事件监听 @EventListener `/api/event`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/event/send` | POST | 发送邮件触发完整事件链路 |
| `/api/event/publish-demo` | POST | 手动发布成功/失败事件 |
| `/api/event/stats` | GET | 监听器聚合的发送统计 |
| `/api/event/listeners` | GET | 扫描容器内所有 @EventListener 方法 |
| `/api/event/explain` | GET | 事件监听知识点速记 |

### 09. 邮件头与编码 `/api/header`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/header/send` | POST | 自定义头 + Reply-To + 中文主题 |
| `/api/header/encoding` | POST | RFC 2047 主题编码演示 |
| `/api/header/rules` | GET | 常用邮件头速查 |

### 10. 常见坑与调优 `/api/pitfall`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pitfall/list` | GET | 10 个高频问题清单 |
| `/api/pitfall/plain-vs-html` | GET | HTML 当纯文本发送演示 |
| `/api/pitfall/tuning` | GET | SMTP 超时 / TLS / 工程化要点 |

## 面试八股

### Spring Boot 发送邮件的核心类？

- **`JavaMailSender` / `JavaMailSenderImpl`**：Spring 对 JavaMail 的封装，负责创建 `MimeMessage` 与发送。
- **`MimeMessageHelper`**：构造邮件的助手类，`setFrom/setTo/setCc/setBcc/setSubject/setText`，附件与内联图片都靠它。
- **`MimeMessage`**：一封邮件的完整表示（header + multipart body），`saveChanges()` 后才有字节大小。

### `new MimeMessageHelper(msg, true, "UTF-8")` 三个参数分别是什么？

- `true`：开启 multipart，附件、内联图片必须开启，否则没有位置放；
- `"UTF-8"`：正文与邮件头字符集，**不指定中文会乱码**。

### HTML 与纯文本怎么区分？

`helper.setText(content, true)` 第二个参数为 true 表示内容是 HTML；false（默认）按纯文本处理，会把 `<h3>` 等标签原样展示。

### 附件与内联图片的区别？

- **附件 `addAttachment`**：独立于正文的文件，收件人需下载/打开；
- **内联图片 `addInline`**：`<img src="cid:xxx">` 引用、随正文一起显示，是消息的一部分。

### 中文主题为什么不会乱码？

邮件头只允许 ASCII，非 ASCII 内容按 **RFC 2047** 编码成 `=?UTF-8?B?<base64>?=` 形式。`message.setSubject(subject, "UTF-8")` 会自动完成；手动可 `MimeUtility.encodeText` 或 Base64 拼接。

### 如何提升异步发送的可靠性？

- 用独立线程池（本项目 `mailExecutor`：core=4 / max=8 / 有界队列 / CallerRuns），不占用请求线程；
- `@Async` 返回 `void` 时异常只打日志，要感知失败用 `Future/CompletableFuture` 或注册 `AsyncUncaughtExceptionHandler`；
- 注意 `@Async` **自调用不生效**，必须通过 Spring Bean 注入后调用。

### 失败重试怎么设计？

- **指数退避**：1s → 2s → 4s…，避免重试风暴；可加随机抖动；
- 必须设**最大重试次数**并打点告警；
- 超时参数要显式配置：`mail.smtp.connectiontimeout / timeout / writetimeout`，否则 SMTP 不可达时会卡死线程。

### Quartz 的核心概念与 @Scheduled 怎么选？

- **Job**：任务逻辑（实现 `org.quartz.Job.execute`）；**JobDetail**：任务定义（绑定 Job 类 + JobDataMap 参数）；**Trigger**：触发规则（CronTrigger / SimpleTrigger）；**Scheduler**：调度器。
- **JobDataMap**：任务参数，注册时 `usingJobData` 写入、执行时 `getMergedJobDataMap` 读取；Quartz 每次触发 new 一个新 Job 实例，不经过 Spring 容器，所以要拿到 Spring Bean 需自定义 `SpringBeanJobFactory` 或用静态委托（本项目演示后者）。
- **@DisallowConcurrentExecution**：同 Job 不并发执行，防止任务叠加。
- **@Scheduled vs Quartz**：单机简单场景用 `@Scheduled`（固定速率/固定延迟/Cron 都行）；需要持久化、集群调度、错过补偿（misfire）、暂停/恢复、动态增删时用 Quartz。
- Quartz 默认内存 JobStore；集群/不丢任务要切 `spring.quartz.job-store-type=jdbc`（需数据库表）。

### @EventListener 事件监听怎么用？

- 发布方注入 `ApplicationEventPublisher`，`publishEvent(event)`；Spring 4.2 起事件可以是任意 POJO，不必继承 ApplicationEvent。
- 监听方用 `@EventListener`，方法参数类型即监听的事件类型；`condition = "#event.success"` 可用 SpEL 条件过滤。
- **@Async + @EventListener**：慢副作用（短信/推送/webhook）异步执行，不拖慢发布线程。
- **@TransactionalEventListener(phase = AFTER_COMMIT)**：事务提交后才执行，避免读到未提交数据。
- **@Order**：多个监听器同时匹配时控制执行顺序。
- 事件解耦「发送」与「后续处理」：加日志/统计/通知/告警只需新增监听器，发送方零改动。

### SMTP 服务器配置差异？

| 邮箱 | host | 端口 | 加密 |
| --- | --- | --- | --- |
| QQ | smtp.qq.com | 465 | SSL |
| 163 | smtp.163.com | 25 | STARTTLS |
| Gmail | smtp.gmail.com | 587 | STARTTLS |
| Outlook | smtp.office365.com | 587 | STARTTLS |

注意：`username` 是邮箱地址，`password` 是 **SMTP 授权码**（不是邮箱登录密码）。

### 为什么会被判为垃圾邮件？

- 发件域名未配置 **SPF / DKIM / DMARC**；
- 发件人地址与 SMTP 登录账号不一致；
- 内容含大量敏感词、全大写、异常链接。真实业务用企业邮箱/邮件服务商（如腾讯云 SES）更稳妥。

## 推荐实验顺序

1. 启动后端与前端，先点「发送模式与配置」确认 simulate 模式。
2. **基础邮件**：发送纯文本 → 多收件人 → 查看最近发送记录。
3. **富文本 HTML**：对照示例 HTML 理解富文本结构。
4. **附件 / 内联图片**：分别看 addAttachment 与 addInline 的差异。
5. **Thymeleaf 模板**：welcome / order，理解模板与代码分离。
6. **异步发送**：提交后查询状态流转，看线程池指标。
7. **失败重试**：把 failTimes 设为 2，观察指数退避的过程与耗时。
8. **定时与批量**：批量 10 封 + 注册一个 3 秒延迟任务。
9. **Quartz**：注册一个 `0/30 * * * * ?` 的任务，在「最近发送记录」里看它周期性产生邮件；再试暂停/恢复/删除。
10. **事件监听**：发一封邮件触发事件链路，看统计与监听器清单；手动发布失败事件看监听器响应。
11. **邮件头与编码**：看 RFC 2047 编码演示。
12. **常见坑**：通读清单，动手前避坑。
13. （进阶）配好真实邮箱，把 mode 改为 real 真实发一封，验证 SPF/DKIM 后的进箱率。

## 参考

- [Spring Boot 官方文档：Sending Email](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/features.html#features.email)
- [Spring Framework JavaMailSender / MimeMessageHelper](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#mail)
- [Spring Boot Quartz Scheduler](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/features.html#features.quartz)
- [Spring Framework Event Listener / @EventListener](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-functionality-events)
- [Thymeleaf 官方文档](https://www.thymeleaf.org/documentation.html)
- [GreenMail（嵌入式 SMTP 测试服务器）](https://greenmail-mail-test.github.io/greenmail/)
