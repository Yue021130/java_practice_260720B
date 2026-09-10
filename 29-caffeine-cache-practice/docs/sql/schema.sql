-- =====================================================================
-- 第29章：Caffeine + SpringBoot 缓存实战 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS ccp_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ccp_db;

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

-- 初始化数据（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '超级管理员', 'admin@example.com', 1),
       ('zhangsan', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '张三', 'zhangsan@example.com', 1),
       ('lisi', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '李四', 'lisi@example.com', 1);
