-- =====================================================================
-- 第34章：Spring Event 事件监听实战 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS sep_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE sep_db;

-- ---------- 系统用户表（登录用，结构同第33章） ----------
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 哈希）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 禁用',
    deleted     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- ---------- 请假单表（本章业务表，事件发布的主角） ----------
DROP TABLE IF EXISTS biz_leave;

CREATE TABLE biz_leave (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50) NOT NULL COMMENT '请假人用户名',
    type        VARCHAR(20) NOT NULL COMMENT '请假类型：SICK 病假 / CASUAL 事假 / ANNUAL 年假',
    reason      VARCHAR(200) NOT NULL COMMENT '请假事由',
    start_date  DATE        NOT NULL COMMENT '开始日期',
    end_date    DATE        NOT NULL COMMENT '结束日期',
    status      INT         NOT NULL DEFAULT 0 COMMENT '审批状态：0 待审批，1 已通过，2 已拒绝',
    approver    VARCHAR(50) DEFAULT NULL COMMENT '审批人',
    deleted     INT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type (type),
    KEY idx_start_date (start_date),
    KEY idx_end_date (end_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请假单表';

-- ---------- 事件记录表（本地消息表思想：事件先落库再广播） ----------
DROP TABLE IF EXISTS event_record;

CREATE TABLE event_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_type  VARCHAR(100) NOT NULL COMMENT '事件类型（类名）',
    payload     TEXT         DEFAULT NULL COMMENT '事件负载（JSON）',
    status      INT          NOT NULL DEFAULT 0 COMMENT '消费状态：0 待消费，1 已消费',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_event_type (event_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '事件记录表';

-- ---------- 事件消费明细表（记录事件被哪些监听器处理过） ----------
DROP TABLE IF EXISTS event_handle;

CREATE TABLE event_handle (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id     BIGINT       NOT NULL COMMENT '关联事件记录 id（event_record.id）',
    listener_name VARCHAR(200) NOT NULL COMMENT '处理该事件的监听器名称',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_event_id (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '事件消费明细表';

-- ---------- 站内通知表（由异步监听器写入） ----------
DROP TABLE IF EXISTS sys_notify;

CREATE TABLE sys_notify (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    receiver    VARCHAR(50)  NOT NULL COMMENT '接收人用户名',
    title       VARCHAR(100) NOT NULL COMMENT '通知标题',
    content     VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    read_flag   INT          NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_receiver (receiver)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内通知表';

-- ---------- 审计日志表（由 AFTER_COMMIT 监听器在事务提交后写入） ----------
DROP TABLE IF EXISTS biz_audit_log;

CREATE TABLE biz_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    action      VARCHAR(50)  NOT NULL COMMENT '动作（如 LEAVE_SUBMIT / LEAVE_APPROVE）',
    detail      TEXT         DEFAULT NULL COMMENT '动作明细（JSON 或描述文本）',
    operator    VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_action (action)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审计日志表';

-- 初始化用户（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '超级管理员', 'admin@example.com', 1);

-- 初始化请假单（覆盖不同状态与类型，方便体验分页与审批）
INSERT INTO biz_leave (username, type, reason, start_date, end_date, status, approver)
VALUES ('zhangsan', 'SICK', '感冒发烧，需要休息两天', '2026-07-20', '2026-07-21', 0, NULL),
       ('lisi', 'ANNUAL', '年假出游，已提前交接工作', '2026-07-25', '2026-07-29', 1, 'admin'),
       ('wangwu', 'CASUAL', '家中有事，需请假一天处理', '2026-07-22', '2026-07-22', 2, 'admin'),
       ('zhangsan', 'ANNUAL', '回家探亲，往返共三天', '2026-08-05', '2026-08-07', 0, NULL),
       ('zhaoliu', 'SICK', '胃病复诊，上午半天检查', '2026-07-24', '2026-07-24', 1, 'admin');
