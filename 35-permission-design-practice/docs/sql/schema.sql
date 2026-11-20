-- =====================================================================
-- 第35章：企业级权限中心 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号（密码均为 123456）：
--   admin      → ADMIN 超级管理员（数据范围 ALL）
--   manager01  → RD_MANAGER 研发经理（数据范围 DEPT_AND_CHILD，研发部及子部门）
--   zhangsan   → STAFF 普通员工（数据范围 SELF，仅本人数据）
-- 越权演示：zhangsan 登录后调 GET /api/order/insecure/{admin的订单id} 可越权，
--           调 GET /api/order/{同id} 会被 DataScopeValidator 拦截（403）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS epc_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE epc_db;

-- ---------- 系统用户表（对齐第33章结构 + dept_id 归属部门） ----------
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 哈希）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    dept_id     BIGINT       DEFAULT NULL COMMENT '所属部门 id（数据权限解析依据）',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 禁用',
    deleted     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- ---------- 角色表 ----------
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码（唯一），如 ADMIN',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    deleted     INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ---------- 用户-角色关联表 ----------
DROP TABLE IF EXISTS sys_user_role;

CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户 id',
    role_id BIGINT NOT NULL COMMENT '角色 id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户-角色关联表';

-- ---------- 权限表（菜单 + 按钮同表，靠 type 区分） ----------
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE sys_permission (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限 id，顶级为 0',
    name        VARCHAR(50)  NOT NULL COMMENT '名称，如「用户管理」',
    type        VARCHAR(10)  NOT NULL COMMENT '类型：MENU 菜单 / BUTTON 按钮',
    perm        VARCHAR(100) DEFAULT NULL COMMENT '权限点标识（BUTTON 用），如 user:list',
    path        VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径（MENU 用）',
    icon        VARCHAR(50)  DEFAULT NULL COMMENT '图标（MENU 用）',
    sort_order  INT          DEFAULT 0 COMMENT '排序号，越小越靠前',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限表（菜单/按钮）';

-- ---------- 角色-权限关联表 ----------
DROP TABLE IF EXISTS sys_role_permission;

CREATE TABLE sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id       BIGINT NOT NULL COMMENT '角色 id',
    permission_id BIGINT NOT NULL COMMENT '权限 id',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-权限关联表';

-- ---------- 部门表（自关联树） ----------
DROP TABLE IF EXISTS sys_dept;

CREATE TABLE sys_dept (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父部门 id，顶级为 0',
    name        VARCHAR(50) NOT NULL COMMENT '部门名称',
    sort_order  INT         DEFAULT 0 COMMENT '排序号，越小越靠前',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

-- ---------- 角色数据范围表（一个角色一条规则） ----------
DROP TABLE IF EXISTS sys_role_data_scope;

CREATE TABLE sys_role_data_scope (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id          BIGINT      NOT NULL COMMENT '角色 id',
    scope_type       VARCHAR(20) NOT NULL COMMENT '数据范围：ALL/CUSTOM/DEPT_AND_CHILD/DEPT/SELF',
    custom_dept_ids  VARCHAR(500) DEFAULT NULL COMMENT 'CUSTOM 时的自定义部门 id（逗号分隔）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色数据范围表';

-- ---------- 订单表（数据权限演练业务表） ----------
DROP TABLE IF EXISTS biz_order;

CREATE TABLE biz_order (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(50)   NOT NULL COMMENT '订单号（唯一）',
    amount      DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    dept_id     BIGINT        NOT NULL COMMENT '归属部门 id（数据权限过滤维度）',
    user_id     BIGINT        NOT NULL COMMENT '归属用户 id（SELF 数据权限过滤维度）',
    status      INT           NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 已取消',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_dept_id (dept_id),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- ---------- 操作日志表（对齐 entity/OpLog，由 @OpLog AOP 落库） ----------
DROP TABLE IF EXISTS sys_op_log;

CREATE TABLE sys_op_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    title       VARCHAR(200)  NOT NULL COMMENT '操作标题',
    perm        VARCHAR(100)  DEFAULT NULL COMMENT '关联权限点标识',
    operator    VARCHAR(50)   DEFAULT NULL COMMENT '操作人用户名',
    method      VARCHAR(200)  DEFAULT NULL COMMENT '方法签名（类名#方法名）',
    ip          VARCHAR(50)   DEFAULT NULL COMMENT '请求 IP',
    params      TEXT          COMMENT '参数摘要（JSON，超长截断）',
    cost_ms     BIGINT        DEFAULT NULL COMMENT '耗时毫秒',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ---------- 登录日志表 ----------
DROP TABLE IF EXISTS sys_login_log;

CREATE TABLE sys_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '登录 IP',
    success     INT          NOT NULL COMMENT '是否成功：1 成功，0 失败',
    message     VARCHAR(500) DEFAULT NULL COMMENT '失败原因（成功为 null）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

-- =====================================================================
-- 初始数据
-- =====================================================================

-- 部门：总公司下挂研发部/运营部/财务部
INSERT INTO sys_dept (id, parent_id, name, sort_order) VALUES
(1, 0, '总公司', 1),
(2, 1, '研发部', 1),
(3, 1, '运营部', 2),
(4, 1, '财务部', 3);

-- 角色
INSERT INTO sys_role (id, role_code, role_name) VALUES
(1, 'ADMIN', '超级管理员'),
(2, 'RD_MANAGER', '研发经理'),
(3, 'STAFF', '普通员工');

-- 角色数据范围：ADMIN=全部 / RD_MANAGER=本部门及以下 / STAFF=仅本人
INSERT INTO sys_role_data_scope (role_id, scope_type, custom_dept_ids) VALUES
(1, 'ALL', NULL),
(2, 'DEPT_AND_CHILD', NULL),
(3, 'SELF', NULL);

-- 用户（密码均为 123456 的 BCrypt 哈希）：
-- admin 在研发部挂 ADMIN；manager01 在研发部挂 RD_MANAGER；zhangsan 在研发部挂 STAFF
INSERT INTO sys_user (id, username, password, nickname, dept_id, email, status) VALUES
(1, 'admin', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '超级管理员', 2, 'admin@example.com', 1),
(2, 'manager01', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '研发经理', 2, 'manager01@example.com', 1),
(3, 'zhangsan', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '张三', 2, 'zhangsan@example.com', 1);

-- 用户-角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- 权限：6 个菜单（1~6）+ 20 个按钮（7~26）
INSERT INTO sys_permission (id, parent_id, name, type, perm, path, icon, sort_order) VALUES
(1, 0, '系统管理', 'MENU', NULL, '/system', 'setting', 1),
(2, 1, '用户管理', 'MENU', NULL, '/system/user', 'user', 1),
(3, 1, '角色管理', 'MENU', NULL, '/system/role', 'peoples', 2),
(4, 1, '部门管理', 'MENU', NULL, '/system/dept', 'tree', 3),
(5, 1, '权限管理', 'MENU', NULL, '/system/perm', 'lock', 4),
(6, 0, '订单管理', 'MENU', NULL, '/order', 'list', 2),
(7, 2, '用户查询', 'BUTTON', 'user:list', NULL, NULL, 1),
(8, 2, '用户新增', 'BUTTON', 'user:create', NULL, NULL, 2),
(9, 2, '用户修改', 'BUTTON', 'user:update', NULL, NULL, 3),
(10, 2, '用户删除', 'BUTTON', 'user:delete', NULL, NULL, 4),
(11, 3, '角色查询', 'BUTTON', 'role:list', NULL, NULL, 1),
(12, 3, '角色新增', 'BUTTON', 'role:create', NULL, NULL, 2),
(13, 3, '角色修改', 'BUTTON', 'role:update', NULL, NULL, 3),
(14, 3, '角色删除', 'BUTTON', 'role:delete', NULL, NULL, 4),
(15, 4, '部门查询', 'BUTTON', 'dept:list', NULL, NULL, 1),
(16, 4, '部门新增', 'BUTTON', 'dept:create', NULL, NULL, 2),
(17, 4, '部门修改', 'BUTTON', 'dept:update', NULL, NULL, 3),
(18, 4, '部门删除', 'BUTTON', 'dept:delete', NULL, NULL, 4),
(19, 5, '权限查询', 'BUTTON', 'perm:list', NULL, NULL, 1),
(20, 5, '权限新增', 'BUTTON', 'perm:create', NULL, NULL, 2),
(21, 5, '权限修改', 'BUTTON', 'perm:update', NULL, NULL, 3),
(22, 5, '权限删除', 'BUTTON', 'perm:delete', NULL, NULL, 4),
(23, 6, '订单查询', 'BUTTON', 'order:list', NULL, NULL, 1),
(24, 6, '订单详情', 'BUTTON', 'order:detail', NULL, NULL, 2),
(25, 6, '订单修改', 'BUTTON', 'order:update', NULL, NULL, 3),
(26, 1, '日志查询', 'BUTTON', 'log:list', NULL, NULL, 5);

-- 角色-权限：ADMIN 全量；RD_MANAGER 订单全功能 + 用户查看；STAFF 只能看订单列表
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 2), (2, 6), (2, 7), (2, 23), (2, 24), (2, 25);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 6), (3, 23);

-- 订单（部门/用户分散，方便观察数据范围差异）：
-- zhangsan(SELF) 只能看到 ORDER003/ORDER008；manager01(DEPT_AND_CHILD 研发部) 看到研发部 4 条；
-- admin(ALL) 看到全部 8 条
INSERT INTO biz_order (id, order_no, amount, dept_id, user_id, status) VALUES
(1, 'ORDER001', 12999.00, 2, 1, 1),
(2, 'ORDER002', 899.50, 2, 2, 1),
(3, 'ORDER003', 199.00, 2, 3, 1),
(4, 'ORDER004', 5600.00, 3, 1, 1),
(5, 'ORDER005', 320.00, 3, 2, 1),
(6, 'ORDER006', 7800.00, 4, 1, 1),
(7, 'ORDER007', 99999.00, 1, 1, 1),
(8, 'ORDER008', 66.00, 2, 3, 0);
