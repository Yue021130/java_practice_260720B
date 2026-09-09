-- =============================================================
-- 第28章：RBAC 权限 + 动态路由 + 数据权限实战
-- MySQL 8.0 建库建表 + 种子数据
-- 执行方式：mysql -uroot -p < rbac_demo.sql
-- =============================================================

DROP DATABASE IF EXISTS rbac_demo;
CREATE DATABASE rbac_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rbac_demo;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE sys_user (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64) NOT NULL COMMENT '登录名',
    password    VARCHAR(128) NOT NULL COMMENT '密码（MD5，仅演示用途）',
    nickname    VARCHAR(64) NOT NULL COMMENT '昵称',
    dept_id     BIGINT      NOT NULL COMMENT '所属部门（1=研发部 2=运营部 3=财务部）',
    status      INT         NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    deleted     INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删 0正常',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- ----------------------------
-- 2. 角色表（data_scope：1=本人 2=本部门 3=全部数据）
-- ----------------------------
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(64) NOT NULL COMMENT '角色编码',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    data_scope  INT         NOT NULL DEFAULT 1 COMMENT '数据权限范围：1本人 2本部门 3全部',
    deleted     INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ----------------------------
-- 3. 权限点表（type：menu=菜单 button=按钮权限点）
--    menu 类型的 path/component 供前端生成动态路由
-- ----------------------------
CREATE TABLE sys_permission (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父权限 ID',
    code        VARCHAR(128) NOT NULL COMMENT '权限编码：order:list / order:delete 等',
    name        VARCHAR(64) NOT NULL COMMENT '权限名称',
    type        VARCHAR(16) NOT NULL COMMENT '类型：menu 菜单 / button 按钮',
    path        VARCHAR(128) DEFAULT NULL COMMENT '前端路由路径',
    component   VARCHAR(128) DEFAULT NULL COMMENT '前端组件路径',
    icon        VARCHAR(64)  DEFAULT NULL COMMENT '菜单图标',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限点表';

-- ----------------------------
-- 4. 用户-角色关联表
-- ----------------------------
CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

-- ----------------------------
-- 5. 角色-权限关联表
-- ----------------------------
CREATE TABLE sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

-- ----------------------------
-- 6. 订单表（数据权限演示载体，dept_id/user_id 是过滤维度）
-- ----------------------------
CREATE TABLE biz_order (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no    VARCHAR(64)   NOT NULL COMMENT '订单号',
    user_id     BIGINT        NOT NULL COMMENT '下单人',
    dept_id     BIGINT        NOT NULL COMMENT '所属部门',
    amount      DECIMAL(12,2) NOT NULL COMMENT '金额',
    status      VARCHAR(16)   NOT NULL DEFAULT 'PAID' COMMENT '状态：PAID 已支付 / REFUNDED 已退款',
    deleted     INT           NOT NULL DEFAULT 0,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- =============================================================
-- 种子数据
-- =============================================================

-- 密码均为 123456 的 MD5：e10adc3949ba59abbe56e057f20f883e
INSERT INTO sys_user (id, username, password, nickname, dept_id) VALUES
(1, 'admin',   'e10adc3949ba59abbe56e057f20f883e', '超级管理员', 1),
(2, 'manager', 'e10adc3949ba59abbe56e057f20f883e', '研发经理',   1),
(3, 'zhangsan','e10adc3949ba59abbe56e057f20f883e', '张三',       1),
(4, 'lisi',    'e10adc3949ba59abbe56e057f20f883e', '李四',       2);

INSERT INTO sys_role (id, code, name, data_scope) VALUES
(1, 'ADMIN',   '超级管理员', 3),
(2, 'MANAGER', '部门经理',   2),
(3, 'USER',    '普通用户',   1);

-- 菜单权限（parent_id=0 为一级菜单，携带前端路由信息）
INSERT INTO sys_permission (id, parent_id, code, name, type, path, component, icon, sort) VALUES
(1,  0, 'menu:dashboard', '工作台',   'menu', '/dashboard',  'Dashboard',  'Odometer', 1),
(2,  0, 'menu:order',     '订单管理', 'menu', '/order',      'OrderList',  'Tickets',  2),
(3,  0, 'menu:user',      '用户管理', 'menu', '/user',       'UserManage', 'User',     3),
(4,  0, 'menu:role',      '角色管理', 'menu', '/role',       'RoleManage', 'Lock',     4);

-- 按钮权限点（文档中 order:approve-refund 越权场景）
INSERT INTO sys_permission (id, parent_id, code, name, type) VALUES
(100, 2, 'order:list',     '订单查询',   'button'),
(101, 2, 'order:add',      '下单',       'button'),
(102, 2, 'order:refund',   '订单退款',   'button'),
(103, 2, 'order:delete',   '订单删除',   'button'),
(200, 3, 'user:list',      '用户查询',   'button'),
(201, 3, 'user:add',       '新增用户',   'button'),
(202, 3, 'user:edit',      '编辑用户',   'button'),
(203, 3, 'user:remove',    '删除用户',   'button'),
(300, 4, 'role:list',      '角色查询',   'button'),
(301, 4, 'role:assign',    '分配权限',   'button');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 3);

-- ADMIN：全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- MANAGER：订单全部 + 用户查询
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2),
(2, 100), (2, 101), (2, 102),
(2, 200);

-- USER：工作台 + 订单查询/下单
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1), (3, 2),
(3, 100), (3, 101);

-- 订单数据：研发部(1) 3 单 + 运营部(2) 2 单，用于验证数据权限过滤效果
INSERT INTO biz_order (order_no, user_id, dept_id, amount, status) VALUES
('NO20260101001', 2, 1, 1999.00, 'PAID'),
('NO20260101002', 3, 1,  299.50, 'PAID'),
('NO20260101003', 3, 1,   88.00, 'REFUNDED'),
('NO20260102001', 4, 2, 1500.00, 'PAID'),
('NO20260102002', 4, 2,   66.00, 'PAID');
