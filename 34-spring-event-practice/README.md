# 第34章：Vue3 + SpringBoot 前后端分离实战（Spring Event 事件监听）

## 一、业务背景

企业应用里，一个动作往往要触发一串后续动作：订单支付后要发短信、加积分、写审计。把这些全写在主流程里，代码耦合成面条；这时就该用 **Spring Event 事件机制**做解耦——主流程只发布事件，谁关心谁监听。

第24章已经演示过 `@Async` + 基础 `@EventListener`，本章把事件机制讲透：依据 `docs/` 三篇文档（事件解决什么问题 / 工作原理 / 使用坑），用一套「请假审批系统」实证 **普通 POJO 事件、同步监听的异常传播坑、`@TransactionalEventListener(AFTER_COMMIT)` 事务事件、`@Async` 线程池、@Order 顺序、condition 条件 SpEL、本地消息表可靠性**。

- 后端：MyBatis-Plus + Sa-Token（JWT 无状态）+ Knife4j，事件先落 `event_record` 表再广播（本地消息表思想）；
- 前端：「请假管理」页提交/审批单据，「事件中心」页实时看到**一个事件被哪些监听器、按什么顺序、同步还是异步处理**的完整链路。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，无状态模式） |
| 事件机制 | Spring Event：@EventListener / @TransactionalEventListener / @Async / @Order / condition |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
34-spring-event-practice/
├── pom.xml
├── docs/
│   ├── spring-event-guide.md            # 八股参考（原理拆解 + 代码位置索引）
│   ├── Spring Event 别瞎用！被它坑的绩效都没了！.html   # 原始参考文档
│   ├── Spring Boot事件监听，这东西到底解决啥问题？.html
│   ├── Springboot的事件监听工作原理.html
│   └── sql/schema.sql                   # MySQL 8.0 建库建表 + 示例数据
├── src/main/java/com/example/sep/
│   ├── SepApplication.java              # @EnableAsync + @MapperScan
│   ├── common/                          # Result / BusinessException / GlobalExceptionHandler
│   ├── event/                           # 【本章核心】普通 POJO 事件（4.2+ 推荐写法）
│   │   ├── LeaveSubmittedEvent.java     # 请假提交事件
│   │   └── LeaveApprovedEvent.java      # 审批结果事件
│   ├── listener/                        # 【本章核心】4 种订阅方式各演示一处
│   │   ├── AuditLogListener.java        # 同步监听提交事件，写审计（@Order(1)）
│   │   ├── ConditionalListener.java     # condition SpEL 只对年假登记（@Order(2)）
│   │   ├── StatsListener.java           # 同步计数器，演示 @Order 顺序
│   │   └── NotifyListener.java          # AFTER_COMMIT + @Async 发通知（本章核心坑演示）
│   ├── config/AsyncConfig.java          # @Async 显式线程池（拒绝默认 SimpleAsyncTaskExecutor）
│   ├── service/
│   │   ├── EventRecordService.java      # 本地消息表：record 同事务落表 / markHandled 独立事务回写
│   │   ├── LeaveService.java            # submit/approve/reject（@Transactional 发布事务事件）
│   │   └── EventCenterService.java      # 事件流水/通知/审计分页装配
│   ├── controller/LeaveController.java  # /api/leave
│   ├── controller/EventCenterController.java  # /api/event
│   ├── dto/ entity/ mapper/ util/
└── web/                                 # 前端（请假管理 + 事件中心）
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**。除登录外均需 `Authorization: <token>`。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录 | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前用户 | 登录 |
| GET | /api/leave/page | 请假单分页 | 登录 |
| GET | /api/leave/{id} | 请假单详情 | 登录 |
| POST | /api/leave/submit | 提交申请（发布 LeaveSubmittedEvent，同步审计） | 登录 |
| POST | /api/leave/approve | 审批通过（事务提交后发布 LeaveApprovedEvent） | 登录 |
| POST | /api/leave/reject | 审批拒绝（同上） | 登录 |
| GET | /api/event/page | 事件流水分页（含消费监听器列表） | 登录 |
| GET | /api/event/{id} | 事件详情（payload + handledBy） | 登录 |
| GET | /api/event/notify/page | 站内通知分页 | 登录 |
| GET | /api/event/audit/page | 审计日志分页 | 登录 |

## 五、运行方式

前置：本机已安装 MySQL 8.0，且 8080/5173 端口未被占用。
**先修改 `src/main/resources/application.yml` 中的数据库用户名密码。**

```bash
# 1. 建库建表
mysql -u root -p < docs/sql/schema.sql

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端（另开终端）
cd web && npm install && npm run dev
```

- 前端：http://localhost:5173 ，默认账号 **admin / 123456**

## 六、核心场景说明：一个事件的完整链路

### 6.1 提交请假（同步链路）

```
POST /api/leave/submit
  → LeaveService.submit：落库 biz_leave（同事务 record() 落 event_record，事件 id 挂上事件对象）
  → publishEvent(LeaveSubmittedEvent)
      → AuditLogListener（同步，同线程）：写审计日志 → markHandled("AuditLogListener")
      → ConditionalListener（condition 命中 ANNUAL 才执行）：写年假台账 → markHandled
  → 接口返回（审计已落库，可查证）
```

### 6.2 审批通过（事务事件 + 异步链路，本章核心）

```
POST /api/leave/approve  [方法带 @Transactional]
  → doApprove：更新单据状态（事务内）
  → publishEvent(LeaveApprovedEvent)
      → StatsListener（普通 @EventListener，事务内同步执行）：计数 → markHandled
      → 事务提交成功 ✓
      → NotifyListener（AFTER_COMMIT 投递 + @Async("eventExecutor")）：
            异步线程发站内通知 → markHandled("NotifyListener")（REQUIRES_NEW 独立事务）
```

**这就是「被坑绩效」的解法**：如果 NotifyListener 用普通 `@EventListener` + `@Async`，它会在事务提交前、在另一个线程读库——用户收到「已通过」通知时点进去还是「待审批」。`AFTER_COMMIT` 让通知在数据定稿后才发出。

### 6.3 事件中心页验证

提交一条年假申请 → 审批通过 → 「事件中心」页应看到：

- 2 条事件流水：`LeaveSubmittedEvent`（handledBy：AuditLogListener、ConditionalListener）、`LeaveApprovedEvent`（handledBy：StatsListener、NotifyListener）；
- 审计日志 2 条（LEAVE_SUBMIT + ANNUAL_LEAVE_REGISTER——注意事假/病假只有 1 条，condition 过滤的效果）；
- 站内通知 1 条；
- 观察后端日志线程名：审计与接口同线程，通知在 `event-async-x` 线程。

## 七、八股速记

**7.1 原理**：观察者模式——`ApplicationEventPublisher`（Subject）→ `ApplicationEventMulticaster.multicastEvent()`（notify）→ 注解方法经 `EventListenerMethodAdapter` 注册成监听器。默认全链同步。

**7.2 POJO 事件**：4.2+ 事件不必继承 `ApplicationEvent`，监听器不必实现接口；按参数类型精确匹配。

**7.3 同步 vs 异步**：同步异常传播给发布者（能搞挂主流程/事务）；`@Async` 必须显式配线程池（默认 SimpleAsyncTaskExecutor 不池化会线程爆炸），且要开 `@EnableAsync`、防自调用。

**7.4 事务事件五阶段**：BEFORE_COMMIT / **AFTER_COMMIT（默认，对外通知）** / AFTER_ROLLBACK / AFTER_COMPLETION。发布方必须有事务（fallbackExecution=false 否则丢弃）；AFTER_COMMIT 后代码无事务上下文，写库用 REQUIRES_NEW。

**7.5 @Order 与 condition**：同步监听器按 @Order 顺序执行；condition 用 SpEL（`#event.type == 'ANNUAL'`）投递前过滤。

**7.6 可靠性**：进程内事件宕机即丢、跨不了实例。本地消息表（业务与事件同事务落库 + 消费回写 + 超时重扫）是单机兜底；跨实例直接 MQ 事务消息。

**7.7 事件 vs MQ**：单实例内解耦用事件；「另一个实例要不要知道」的答案是要，就用 MQ，不要造轮子。

更多细节见 **docs/spring-event-guide.md**。

## 八、测试验证

```bash
# 后端单元测试（全部 mock，无需 MySQL/Redis）
mvn clean test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 提交一条年假申请
curl -s -X POST http://localhost:8080/api/leave/submit \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"admin","type":"ANNUAL","reason":"回家过年","startDate":"2026-10-01","endDate":"2026-10-05"}'

# 审批通过
curl -s -X POST http://localhost:8080/api/leave/approve \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '{"id":1,"approver":"admin"}'

# 看事件流水：payload + 已消费监听器
curl -s http://localhost:8080/api/event/page -H "Authorization: $TOKEN"
```

## 九、作者

Yue021130 <169446203@qq.com>
