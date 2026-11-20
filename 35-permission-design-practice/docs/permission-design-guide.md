# 企业级权限设计参考指南（第35章配套）

> 依据 `docs/` 三篇文档（RBAC 动态路由数据权限 / MP 数据权限生产级落地 / 授权设计番外篇）整理，
> 所有结论有本章代码实证，附代码位置索引。

## 一、认证 vs 授权，RBAC0~RBAC3

- **认证（Authentication）**：你是谁——登录体系解决（本章 Sa-Token JWT）。
- **授权（Authorization）**：你能做什么——本章主角。
- **RBAC0**（基础）：用户-角色-权限，本章落地。
- **RBAC1**：增加角色继承（经理继承员工权限）——可用角色 parent_id 扩展，本章未展开。
- **RBAC2**：增加角色互斥/基数约束（一个用户不能同时是会计和出纳）。
- **RBAC3**：RBAC1+RBAC2 合并。

设计要点：**权限永远挂在角色上，不直接挂用户**——人变来变去，角色相对稳定；权限粒度细化到按钮级（`user:delete`），前端藏按钮、后端拦接口，两层缺一不可。

## 二、权限点三级设计

| 层级 | 载体 | 示例 | 校验位置 |
| --- | --- | --- | --- |
| 菜单 | sys_permission type=MENU | 订单管理 | 后端下发，前端动态路由 |
| 按钮 | sys_permission type=BUTTON | `order:update` | 前端 v-permission 隐藏 + 后端 @SaCheckPermission |
| 接口 | Controller 方法 | GET /api/order/{id} | 后端 @SaCheckPermission |

perm 命名规范：`资源:动作`（order:list/create/update/delete），同一资源的按钮和接口 perm 一一对应，排查时能反查。

代码位置：`entity/Permission.java`、`controller/` 各方法注解、`web/src/directives/permission.js`。

## 三、动态路由的安全意义

静态路由 + 菜单隐藏只是「看不见」，路由文件还在前端包里，直接输 URL 就能进页面（前端守卫也能绕）；**动态路由**是登录后由后端按权限下发菜单树、前端 `addRoute` 注册——无权限的页面组件根本不存在于路由表，配合后端接口鉴权形成双保险。

注意动态路由要处理**刷新 404**：刷新时路由表重建时机晚于导航守卫，需标记重注册逻辑（前端 router/index.js 已实现）。

## 四、数据权限四种方案对比

| 方案 | 优点 | 缺点 | 适用 |
| --- | --- | --- | --- |
| SQL 硬编码 WHERE | 直观 | 散落在 XML，改口径要全文替换 | 小型项目 |
| MyBatis 拦截器注入 | 对 Mapper 零侵入、统一口径 | 复杂 join/别名要适配 | **生产主流（本章方案）** |
| 数据库视图 | DB 层收口 | 视图膨胀、跨库难 | 报表库 |
| 规则引擎 | 最灵活 | 引入重、排查难 | 权限产品化 |

**MP DataPermissionInterceptor 原理**：在 Executor.query 阶段拿到 BoundSql 的 WHERE 表达式（JSqlParser AST），`DataPermissionHandler.getSqlSegment` 返回追加条件后的新表达式重新生成 SQL。代码位置：`config/MyBatisPlusConfig.java`、`datascope/DataScopeHandler.java`。

与多租户插件的区别：`TenantLineInnerInterceptor` 是给**所有表**拼固定租户列；数据权限是**按用户范围拼动态条件**，两者可同时注册（租户先执行）。

## 五、五级数据范围

| 范围 | 语义 | SQL 条件 |
| --- | --- | --- |
| ALL | 全部数据 | 不加条件 |
| CUSTOM | 自定义部门（规则表 csv） | `dept_id IN (csv)` |
| DEPT_AND_CHILD | 本部门及以下（递归部门树） | `dept_id IN (递归结果)` |
| DEPT | 仅本部门 | `dept_id = 本部门` |
| SELF | 仅本人 | `user_id = 当前用户` |

多角色取**最宽**不取交集（若依口径）：交集越配越小，管理员排查困难。代码位置：`datascope/DataScopeType.java`、`DataScopeResolver.java`、`sys_role_data_scope` 表。

## 六、垂直越权 vs 水平越权

- **垂直越权**（vertical）：普通用户用了管理员功能——用 @SaCheckPermission 挡（功能权限）。
- **水平越权**（horizontal / IDOR）：同角色用户 A 访问用户 B 的数据——SQL 注入只挡列表，挡不住「拿着别人 id 调详情/修改」。**必须**在按 id 操作资源的接口做数据归属二次校验。

代码实证：`OrderController` 的 `GET /api/order/{id}` 走 `DataScopeValidator.checkOrderVisible`（他人订单 403）；`GET /api/order/insecure/{id}` 故意不走校验，能读出他人订单——正反对照。

## 七、权限缓存一致性

热点路径（鉴权校验、数据范围解析）每次查库会把权限表打爆，标准做法：本地缓存 + **变更主动失效**。

- 缓存：Caffeine，userId → 权限点/角色/数据范围（`permission/PermissionCache.java`）。
- 失效：用户角色变更 evict(userId)；角色/权限/数据范围变更 evictAll()（影响面大按角色反查在线用户逐个失效是精细化做法）。
- 兜底：TTL 10 分钟——即使忘了 evict 也不会永久脏数据。
- 集群：本地缓存换 Redis，evict 时广播（MQ/Redis pub-sub）。

## 八、番外篇：权限可配置化之后的授权设计

权限全部存库可配置后，**授权本身**要有机制兜底：

1. **默认拒绝**：白名单之外一律要登录、业务接口一律要权限点。白名单显式维护（/api/auth/login、/doc.html、knife4j 资源），新增公开接口必须显式声明——宁漏不放。
2. **超级管理员**：ADMIN 角色的 ALL 数据范围就是「逃逸口」；注意逃逸口要最小化，普通角色永远走规则。
3. **没配数据范围的角色默认 SELF**（DataScopeResolver 最后一段），即「不配 = 最小权限」。
4. **机制兜底代替人肉**：@OpLog 审计、@SaCheckPermission 注解、拦截器注入，都是把「记得」变成「必须」。
5. **审计可查**：所有授权决策留痕（操作日志/登录日志），出事能复盘。

## 九、越权测试清单

- [ ] 低权限账号直接访问高权限接口 → 403
- [ ] 登录后改前端路由跳转无权限页面 → 页面不存在 + 接口 403
- [ ] 无权限按钮是否可见 → v-permission 已移除
- [ ] 普通用户拿他人资源 id 调详情 → 403（水平越权）
- [ ] 普通用户拿他人资源 id 调修改 → 403
- [ ] 改角色权限后立即生效 → 缓存已 evict
- [ ] 订单列表与其他角色对比行数不同 → 数据权限生效
- [ ] SQL 日志确认带了 dept_id IN / user_id 条件 → 拦截器生效
