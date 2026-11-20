# 第35章：企业级权限中心实战（RBAC + 规则化数据权限 + 越权防护）

## 一、业务背景

「Controller 加了登录校验就万事大吉」是后台系统最常见的越权隐患——认证（你是谁）≠ 授权（你能做什么）。本章依据 `docs/` 三篇文档（RBAC/动态路由/数据权限总览、MyBatis-Plus 数据权限生产级落地、权限可配置化之后的授权设计），整合出一套**企业级权限中心**：

- **功能权限**：RBAC 五表（用户-角色-权限），权限点细化到按钮级（`order:delete`），接口层 `@SaCheckPermission` + 前端 `v-permission` 双保险；
- **动态路由**：后端按权限下发菜单树，前端登录后 `addRoute` 动态注册——无权限的页面组件根本不存在于路由表；
- **数据权限**：规则表驱动五级数据范围（全部/自定义/本部门及以下/本部门/仅本人），MyBatis-Plus 拦截器自动注入 SQL，**改库即生效**；
- **越权防护**：水平越权（IDOR）二次校验 + 一个故意不设防的反面教材接口做正反对照；
- **机制兜底**：@OpLog 操作审计、登录日志、权限缓存主动失效——把「记得」变成「必须」。

演示账号（密码均 123456）：**admin**（全部数据）/ **manager01**（研发经理，本部门及以下）/ **zhangsan**（普通员工，仅本人）——三个账号看到的订单列表完全不同。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5（含 DataPermissionInterceptor 数据权限拦截器） |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，无状态模式） |
| 权限缓存 | Caffeine（变更主动失效 + TTL 兜底） |
| 操作审计 | 自定义 @OpLog 注解 + Spring AOP |
| API 文档 | Knife4j 4.3.0（OpenAPI3/Swagger 规范，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router（动态路由）+ Axios |

## 三、项目结构

```
35-permission-design-practice/
├── pom.xml
├── docs/
│   ├── permission-design-guide.md        # 八股参考（方案对比/越权清单/代码索引）
│   ├── 一文讲透后台权限控制：RBAC、动态路由与数据权限.html
│   ├── 动态数据权限控制：Spring Boot ＋ MyBatis-Plus 生产级落地方案.html
│   ├── Controller 加了 [Authorize] 就万事大吉？（番外篇）—— 权限可配置化之后，授权该怎么设计.html
│   └── sql/schema.sql                    # 10 张表 + 演示数据
├── src/main/java/com/example/epc/
│   ├── EpcApplication.java
│   ├── permission/                       # 【核心一】功能权限
│   │   ├── PermissionCache.java          # userId→权限点/角色/数据范围 缓存 + evict
│   │   └── StpInterfaceImpl.java         # Sa-Token 权限供给（对接 @SaCheckPermission）
│   ├── datascope/                        # 【核心二】数据权限
│   │   ├── DataScopeType.java            # 五级范围枚举 + 宽窄比较
│   │   ├── DataScopeContext.java         # 解析结果快照
│   │   ├── DataScopeResolver.java        # 多角色取最宽 + 部门树递归
│   │   ├── DataScopeHandler.java         # MP DataPermissionHandler：SQL 注入
│   │   └── DataScopeValidator.java       # 水平越权二次校验（防 IDOR）
│   ├── oplog/                            # 【核心三】审计
│   │   ├── OpLog.java / OpLogAspect.java # @OpLog 注解 + AOP 操作日志
│   │   └── LoginLogService.java          # 登录日志
│   ├── config/MyBatisPlusConfig.java     # 分页 + 数据权限拦截器
│   ├── controller/（User/Role/Dept/Permission/Order/Log/Auth）
│   ├── dto/ entity/ mapper/ service/ util/ common/
├── src/main/resources/application.yml
└── web/                                  # 前端（动态路由 + v-permission）
    └── src/directives/permission.js      # 按钮级权限指令
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**。除登录外均需 `Authorization: <token>`。

| 方法 | 接口 | 权限点 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 公开（白名单） | 登录，返回 token + menus + perms |
| GET | /api/auth/info | 登录 | 当前用户 |
| GET/POST/PUT | /api/user | user:* | 用户管理（变更 evict 缓存） |
| DELETE | /api/user/{id} | user:delete | 删除用户 |
| GET/POST/PUT | /api/role | role:* | 角色管理（含权限分配 + 数据范围配置） |
| GET | /api/role/{id} | role:list | 角色详情（回显 permissionIds/customDeptIds） |
| GET | /api/dept/tree | 登录 | 部门平铺列表（前端建树） |
| GET/POST/PUT/DELETE | /api/dept | dept:* | 部门管理 |
| GET | /api/permission/tree | 登录 | 权限树 |
| POST | /api/permission/refresh-cache | perm:list | 演示缓存主动失效 |
| GET | /api/order/page | order:list | 订单分页（数据权限自动过滤） |
| GET | /api/order/{id} | order:detail | 详情（数据归属二次校验） |
| PUT | /api/order | order:update | 修改（二次校验） |
| GET | /api/order/insecure/{id} | order:list | **反面教材**：不设数据归属校验，演示水平越权 |
| GET | /api/log/op/page | log:list | 操作日志 |
| GET | /api/log/login/page | log:list | 登录日志 |

## 五、运行方式

前置：本机 MySQL 8.0，8080/5173 端口空闲。**先修改 `application.yml` 数据库密码。**

```bash
mysql -u root -p < docs/sql/schema.sql   # 1. 建库建表 + 演示数据
mvn spring-boot:run                      # 2. 后端
cd web && npm install && npm run dev     # 3. 前端
```

## 六、核心场景说明

### 6.1 授权决策链（每次请求）

```
请求 → 白名单?（/api/auth/login、/doc.html…）放行
     → Sa-Token 登录校验（JWT 无状态）
     → @SaCheckPermission 权限点校验（读 PermissionCache，不查库）
     → 查询类：DataScopeHandler 按数据范围往 SQL 注入 dept_id IN / user_id = 条件
     → 单资源操作：DataScopeValidator 二次校验归属，越权 403
     → @OpLog 切面落操作审计
```

### 6.2 数据权限演示

分别用三个账号登录看「订单管理」页：admin 见 8 条；manager01（DEPT_AND_CHILD）只见研发部及其子部门；zhangsan（SELF）只见自己创建的。观察后端控制台 SQL：查询自动带上了 `dept_id IN (...)` / `user_id = ...`——**业务代码里没有任何 WHERE 硬编码**。

### 6.3 水平越权对照演示

zhangsan 登录后打开订单页「越权对比演示」：对自己的订单，两个接口都返回；点他人订单，`/api/order/{id}` 返回 403（越权拦截），而 `/api/order/insecure/{id}` 能读出来——这就是只做功能权限、不做数据归属校验的后果。

### 6.4 权限缓存生效演示

给 STAFF 角色勾选 `order:detail` 保存 → 角色管理页点「刷新权限缓存」→ zhangsan 重新登录（或等 TTL）→ 详情按钮出现。若不刷新，旧权限最长存活 10 分钟（Caffeine TTL 兜底）。

## 七、八股速记

1. **RBAC0~3**：用户-角色-权限是基础；RBAC1 角色继承、RBAC2 角色互斥约束、RBAC3 合并。权限永远挂角色不挂用户。
2. **权限点三级**：菜单（动态路由载体）/ 按钮（前端隐藏）/ 接口（后端拦截）——两层校验缺一不可，前端隐藏只是体验不是安全。
3. **动态路由安全意义**：无权限页面组件不存在于路由表，而不是「存在但藏菜单」。
4. **数据权限四方案**：硬编码 WHERE / MP 拦截器注入（生产主流，本章）/ 视图 / 规则引擎。拦截器原理 = JSqlParser 改写 WHERE AST。
5. **五级数据范围**：多角色取最宽不取交集；未配置默认 SELF（默认拒绝）。
6. **垂直 vs 水平越权**：功能权限挡垂直；水平越权（IDOR）必须数据归属二次校验——SQL 注入不拦 UPDATE/DELETE。
7. **权限缓存一致性**：Caffeine + 变更主动 evict + TTL 兜底；集群换 Redis + 失效广播。
8. **番外篇授权设计**：默认拒绝、白名单显式维护、超管逃逸口最小化、机制兜底（@OpLog/@SaCheckPermission/拦截器）、审计留痕。

详见 **docs/permission-design-guide.md**（含越权测试清单）。

## 八、测试验证

```bash
mvn clean test          # 后端单测（全部 mock，无需 MySQL）：数据范围解析 7 例 + 越权校验 5 例 + 宽窄比较 2 例 + 审计切面 2 例 + 密码 2 例
cd web && npm install && npm run build
```

## 九、作者

Yue021130 <169446203@qq.com>
