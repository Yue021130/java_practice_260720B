-- =====================================================================
-- 第38章 生产级加固实战：数据库初始化脚本
-- MySQL 8.0，执行：mysql -uroot -p < docs/sql/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS prod_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE prod_db;

-- ---------------------------------------------------------------------
-- 系统用户：Spring Security 登录数据源，username 唯一索引
-- password 存 BCrypt 哈希，下方初始值的明文均为 123456
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
    nickname    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

-- ---------------------------------------------------------------------
-- 业务订单：方法级权限（order:read / order:write）与审计的演示载体
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_order (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    order_no    VARCHAR(64)   NOT NULL COMMENT '订单号（业务键）',
    amount      DECIMAL(12,2) NOT NULL COMMENT '金额',
    dept_id     BIGINT        NULL COMMENT '所属部门（演示固定 1）',
    user_id     BIGINT        NULL COMMENT '下单人 sys_user.id',
    status      INT           NOT NULL DEFAULT 1 COMMENT '1 正常 / 0 取消',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务订单';

-- ---------------------------------------------------------------------
-- 审计事件：加固看板数据源。
-- 登录成功/登出/订单增删改发的 AuditEvent 由审计落库组件写入本表。
-- timestamp 是 MySQL 非保留关键字，可直接作列名（此处仍加反引号保险）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_event (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    principal  VARCHAR(64) NOT NULL COMMENT '操作主体（用户名）',
    type       VARCHAR(64) NOT NULL COMMENT '事件类型：LOGIN_SUCCESS/LOGOUT/ORDER_CREATE/...',
    data       TEXT        NULL COMMENT '事件上下文（键值/JSON 文本）',
    `timestamp` DATETIME   NOT NULL COMMENT '事件发生时间',
    KEY idx_principal (principal),
    KEY idx_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审计事件';

-- ---------------------------------------------------------------------
-- 初始数据
-- ---------------------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, role) VALUES
('admin',  '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '管理员',   'ADMIN'),
('user01', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '普通用户', 'USER');

-- 6 条示例订单：前 3 条归属 admin，后 3 条归属 user01（自增 ID 依次 1~6）
INSERT INTO biz_order (order_no, amount, dept_id, user_id, status) VALUES
('NO202610010001', 199.00,  1, 1, 1),
('NO202610010002', 88.50,   1, 1, 1),
('NO202610010003', 1200.00, 1, 1, 0),
('NO202610020001', 35.90,   1, 2, 1),
('NO202610020002', 560.00,  1, 2, 1),
('NO202610020003', 79.99,   1, 2, 0);
