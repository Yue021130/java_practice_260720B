-- =====================================================================
-- 第32章：Servlet Filter 过滤器链 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fcp_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE fcp_db;

-- ---------- 系统用户表（登录用） ----------
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

-- ---------- 公告表（本章业务表，给过滤器链提供真实流量） ----------
DROP TABLE IF EXISTS biz_notice;

CREATE TABLE biz_notice (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    title       VARCHAR(100)  NOT NULL COMMENT '公告标题',
    type        INT           NOT NULL DEFAULT 1 COMMENT '类型：1 通知，2 公告，3 新闻',
    status      INT           NOT NULL DEFAULT 0 COMMENT '状态：1 发布，0 草稿',
    content     VARCHAR(2000) DEFAULT NULL COMMENT '正文内容',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '排序号',
    deleted     INT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告表';

-- ---------- 请求审计日志表（由 TimingFilter 自动写入，含鉴权失败的 401） ----------
DROP TABLE IF EXISTS sys_request_log;

CREATE TABLE sys_request_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id     VARCHAR(64)  DEFAULT NULL COMMENT '全链路追踪 ID',
    ip           VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
    method       VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
    uri          VARCHAR(255) DEFAULT NULL COMMENT '请求 URI',
    status_code  INT          DEFAULT NULL COMMENT '响应状态码',
    cost_ms      INT          DEFAULT NULL COMMENT '耗时毫秒',
    has_error    INT          NOT NULL DEFAULT 0 COMMENT '是否异常：0 否，1 是',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_trace_id (trace_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '请求审计日志表（Filter 层）';

-- 初始化用户（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '超级管理员', 'admin@example.com', 1);

-- 初始化公告（方便启动后直接体验 CRUD 与 XSS 清洗）
INSERT INTO biz_notice (title, type, status, content, sort_order)
VALUES ('系统升级通知', 1, 1, '本周六 22:00 - 24:00 系统升级，期间暂停服务。', 1),
       ('平台使用公告', 2, 1, '请妥善保管个人账号，勿泄露密码。', 2),
       ('新版本发布新闻', 3, 0, 'V2.0 版本新增过滤器链监控面板。', 3);
