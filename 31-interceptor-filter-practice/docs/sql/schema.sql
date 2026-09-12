-- =====================================================================
-- 第31章：Filter + Interceptor —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS ifp_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ifp_db;

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

-- ---------- 资源表（本章业务表，给 Interceptor 提供真实流量） ----------
DROP TABLE IF EXISTS biz_resource;

CREATE TABLE biz_resource (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(50)  NOT NULL COMMENT '资源名称',
    url         VARCHAR(200) NOT NULL COMMENT '资源路径/标识',
    type        INT          NOT NULL DEFAULT 1 COMMENT '类型：1 API，2 页面，3 菜单',
    status      INT          NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_url (url)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资源表';

-- ---------- 接口审计日志表（由 LogInterceptor 自动写入） ----------
DROP TABLE IF EXISTS sys_api_log;

CREATE TABLE sys_api_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ip           VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
    method       VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
    uri          VARCHAR(255) DEFAULT NULL COMMENT '请求 URI',
    user_agent   VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    username     VARCHAR(50)  DEFAULT 'anonymous' COMMENT '登录用户名（未登录为 anonymous）',
    status_code  INT          DEFAULT NULL COMMENT '响应状态码',
    cost_ms      INT          DEFAULT NULL COMMENT '耗时毫秒',
    has_error    INT          NOT NULL DEFAULT 0 COMMENT '是否异常：0 否，1 是',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_uri (uri)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '接口审计日志表';

-- 初始化用户（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '超级管理员', 'admin@example.com', 1);

-- 初始化资源（方便启动后直接体验 CRUD 与限流）
INSERT INTO biz_resource (name, url, type, status, sort_order, remark)
VALUES ('用户登录接口', '/api/auth/login', 1, 1, 1, '系统登录'),
       ('资源管理页面', '/resource', 3, 1, 2, '资源管理菜单'),
       ('审计日志页面', '/api-log', 3, 1, 3, '审计日志菜单'),
       ('限流测试接口', '/api/resource/test-rate-limit', 1, 1, 4, '演示 IP 限流');
