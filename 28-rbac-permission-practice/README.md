# 第28章：RBAC 权限 + 动态路由 + 数据权限实战

## 一、业务背景

后台系统里"Controller 加了登录校验就万事大吉"是最常见的越权隐患——认证（你是谁）≠ 授权（你能做什么）。
本章依据 `docs/` 中的 RBAC 系列文章，落地一套完整的权限体系：

1. **RBAC 权限模型**：用户-角色-权限五表设计，权限细化到按钮级权限点（如 `order:delete`、`order:refund`），后台可配置，改库即生效；
2. **机制兜底**：方法级 `@SaCheckPermission` 注解校验，杜绝"漏写一行注解就越权"；
3. **动态路由**：后端按角色下发菜单树，前端登录后动态注册路由，无权限的页面根本不存在；
4. **数据权限**：MyBatis-Plus 拦截器统一注入 `dept_id` / `user_id` 条件，告别散落在 XML 里的硬编码 WHERE。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、MyBatis-Plus 3.5.5、Sa-Token 1.39.0（JWT 无状态）、Knife4j (OpenAPI3)、MySQL 8.0 |
| 前端 | Vue3 + Vite、Element Plus、Pinia、Vue Router、Axios |
| 构建 | Maven、npm |

## 三、项目结构

```text
28
├── docs/                                   # 本章参考文章（RBAC、动态路由与数据权限等）
├── src/main/java/com/example/rbac
│   ├── RbacPermissionApplication.java      # 启动类
│   ├── common/                             # 统一返回、业务异常、全局异常处理
│   ├── config/                             # 跨域、Knife4j、MyBatis-Plus（分页+数据权限拦截器）、Sa-Token
│   ├── satoken/                            # StpInterfaceImpl：鉴权数据源（实时查库）
│   ├── datapermission/                     # 数据权限：上下文加载 + WHERE 条件注入
│   ├── controller/                         # Auth / User / Role / Permission / Order
│   ├── service/                            # AuthService / UserService / RoleService / PermissionService / OrderService
│   ├── mapper/                             # MyBatis-Plus BaseMapper
│   ├── entity/                             # SysUser SysRole SysPermission 关联表 BizOrder
│   └── dto/                                # 请求/响应 DTO
├── src/main/resources
│   ├── application.yml                     # MySQL + Sa-Token JWT 配置
│   └── sql/rbac_demo.sql                   # 建库建表 + 种子数据（先执行）
├── src/test/java                           # 数据权限拦截器单元测试
├── web/                                    # Vue3 前端
│   └── src
│       ├── api/request.js                  # Axios 封装：请求/响应拦截器处理 Token
│       ├── stores/user.js                  # Pinia 用户状态
│       ├── router/index.js                 # 静态路由 + 登录后动态 addRoute
│       ├── directive/permission.js         # v-permission 按钮级权限
│       ├── layout/Layout.vue               # 动态菜单布局
│       └── views/                          # Login Dashboard OrderList UserManage RoleManage Forbidden
└── pom.xml
```

## 四、接口清单

启动后访问：http://localhost:8080/doc.html （Knife4j，右上角全局参数填登录返回的 token 即可在线调试）

| 方法 | 接口 | 所需权限 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 公开 | 登录，签发 JWT |
| GET | /api/auth/info | 登录 | 当前用户信息 + 角色 + 权限点 |
| GET | /api/auth/menus | 登录 | 当前用户菜单树（动态路由数据源） |
| POST | /api/auth/logout | 登录 | 退出登录 |
| GET | /api/order/page | order:list | 订单分页（自动按数据权限过滤） |
| POST | /api/order | order:add | 下单 |
| POST | /api/order/{id}/refund | order:refund | 订单退款（高危操作场景） |
| DELETE | /api/order/{id} | order:delete | 删除订单 |
| GET | /api/user/page | user:list | 用户分页 |
| POST/PUT | /api/user | user:add / user:edit | 新增/编辑用户（含角色分配） |
| DELETE | /api/user/{id} | user:remove | 删除用户 |
| GET | /api/role/list | role:list | 角色列表 |
| GET | /api/role/{id}/permissions | role:list | 角色已拥有的权限点 |
| POST / PUT | /api/role、/api/role/{id}/permissions | role:assign | 角色维护与权限分配 |
| GET | /api/permission/tree | role:list | 全量权限树 |

## 五、运行方式

### 1. 准备数据库（MySQL 8.0）

```bash
# 方式一：命令行（密码按本机实际情况）
mysql -uroot -p < src/main/resources/sql/rbac_demo.sql

# 方式二：用 Navicat / DataGrip 等工具直接执行 rbac_demo.sql
```

数据库连接配置在 `application.yml`（默认 `localhost:3306/rbac_demo`，账号 root，密码 123456，请按本机环境修改）。

### 2. 启动后端

```bash
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd web
npm install
npm run dev
```

浏览器打开 http://localhost:5173

### 演示账号（密码均为 123456）

| 账号 | 角色 | 数据权限 | 可见菜单 |
| --- | --- | --- | --- |
| admin | ADMIN | 全部数据 | 全部菜单 + 全部按钮 |
| manager | MANAGER | 本部门（研发部） | 订单（退款可用）、用户查询 |
| zhangsan | USER | 仅本人 | 订单（仅查询/下单） |

**推荐体验路径**：分别用三个账号登录看订单列表的数据差异 → 用 zhangsan 直接调删除接口（前端按钮已隐藏，可用 Knife4j 携带 token 调用）观察 403 → 用 admin 在「角色管理」给 USER 角色勾选 `order:delete`，zhangsan 重新登录后删除按钮出现。

## 六、核心实现说明

### 6.1 Sa-Token JWT 无状态登录

- `SaTokenConfig` 注册 `StpLogicJwtForSimple`，token 本身携带 loginId，服务端不存会话；
- `application.yml` 配置 `token-name: Authorization`（Axios 请求拦截器每次自动携带）、`is-concurrent: false`（新登录挤掉旧登录）；
- `StpInterfaceImpl` 每次鉴权实时查库，角色权限调整后立即生效（权限可配置化）。

### 6.2 功能权限：注解 + 全局异常处理

- Controller 方法标注 `@SaCheckPermission("order:delete")`，而非停留在类级登录校验；
- 未登录 → code 401；无角色/无权限 → code 403，统一由 `GlobalExceptionHandler` 转成 `Result`；
- 前端双重兜底：菜单/路由层只下发有权限的页面，按钮层 `v-permission` 指令直接移除 DOM。

### 6.3 动态路由

- 权限表中 `type=menu` 的记录携带 `path` / `component` / `icon`；
- 登录后 `GET /auth/menus` 返回当前用户可见菜单树 → `addDynamicRoutes` 逐个 `router.addRoute`；
- 刷新页面后动态路由丢失，`router.beforeEach` 检测 `routesLoaded` 重新拉取注册。

### 6.4 数据权限：拦截器统一注入

- `MyBatisPlusConfig` 注册 `DataPermissionInterceptor`（先于分页拦截器）；
- `DataScopeHandler` 只对 `OrderMapper` 语句生效：DEPT 范围追加 `dept_id = ?`，SELF 范围再追加 `user_id = ?`，GLOBAL 不加；
- 数据范围由 `DataScopeContext` 实时计算（取用户所有角色中最大的 `data_scope`）；
- Service 层零感知——`orderService.page()` 里没有任何数据权限代码。

## 七、八股速记

### 7.1 认证 vs 授权

1. 认证（Authentication）：确认"你是谁"，如登录校验 JWT；
2. 授权（Authorization）：确认"你能做什么"，如角色/权限点校验；
3. 只认证不授权 = 任何登录用户都能调用删除接口，这是典型的越权漏洞。

### 7.2 RBAC 五表模型

1. 用户表、角色表、权限点表三张主表；
2. 用户-角色、角色-权限两张关联表，均支持多对多；
3. 权限点比角色更细（`order:delete`），角色只是权限点的集合，业务变化只需改关联数据，不改代码。

### 7.3 三种数据权限范围

1. SELF（本人）：`WHERE dept_id = ? AND user_id = ?`；
2. DEPT（本部门）：`WHERE dept_id = ?`；
3. GLOBAL（全部）：不加条件。
用 MyBatis 拦截器统一注入，优于在 XML 里硬编码——一处配置，全局生效，杜绝漏写。

### 7.4 前端权限的三层防线

1. 路由层：无权限的菜单不下发、不注册（看不见）；
2. 按钮层：`v-permission` 隐藏操作入口（点不到）；
3. 接口层：后端 `@SaCheckPermission` 强校验（拦得住）。

## 八、测试验证

```bash
# 后端：编译 + 数据权限拦截器单元测试（5 个用例）
mvn test

# 前端：构建
cd web
npm run build
```

## 九、作者

Yue021130 <169446203@qq.com>
