-- =====================================================================
-- 第33章：Spring Cache 二级缓存实战 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS scp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE scp_db;

-- ---------- 系统用户表（登录用，结构同第32章） ----------
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

-- ---------- 商品表（本章业务表，缓存三问的演练对象） ----------
DROP TABLE IF EXISTS biz_product;

CREATE TABLE biz_product (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    category    VARCHAR(50)   NOT NULL COMMENT '商品分类（手机/电脑/耳机等）',
    price       DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock       INT           NOT NULL DEFAULT 0 COMMENT '库存',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1 上架，0 下架',
    sort_order  INT           DEFAULT 0 COMMENT '排序号，越小越靠前',
    deleted     TINYINT       DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- 初始化用户（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '超级管理员', 'admin@example.com', 1);

-- 初始化商品（覆盖手机 / 电脑 / 耳机 / 平板 / 手表分类，方便体验分页与详情缓存）
INSERT INTO biz_product (name, category, price, stock, status, sort_order)
VALUES ('华为 Mate 60 Pro 智能手机', '手机', 6999.00, 100, 1, 1),
       ('小米 14 智能手机', '手机', 3999.00, 200, 1, 2),
       ('iPhone 15 Pro 智能手机', '手机', 8999.00, 50, 1, 3),
       ('联想 ThinkPad X1 笔记本电脑', '电脑', 9999.00, 30, 1, 4),
       ('苹果 MacBook Air 13 英寸', '电脑', 9499.00, 40, 1, 5),
       ('索尼 WH-1000XM5 头戴式降噪耳机', '耳机', 2899.00, 80, 1, 6),
       ('苹果 AirPods Pro 2 真无线耳机', '耳机', 1899.00, 150, 1, 7),
       ('华为 MatePad Pro 平板电脑', '平板', 4299.00, 0, 0, 8);
