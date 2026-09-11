-- =====================================================================
-- 第30章：Jakarta Validation + Jackson 数据脱敏 —— MySQL 8.0 建库建表脚本
-- 使用方法：登录 MySQL 后执行本脚本（或 source 本文件），再启动后端
-- 默认账号：admin / 123456（密码为 BCrypt 哈希，由 PasswordUtil 生成）
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vmp_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE vmp_db;

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

-- ---------- 客户信息表（本章业务表，敏感字段明文存储，出参脱敏） ----------
DROP TABLE IF EXISTS biz_customer;

CREATE TABLE biz_customer (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(30)  NOT NULL COMMENT '姓名',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    id_card     VARCHAR(18)  NOT NULL COMMENT '身份证号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    address     VARCHAR(200) DEFAULT NULL COMMENT '住址',
    gender      INT          NOT NULL DEFAULT 1 COMMENT '性别：1 男，0 女',
    level       INT          NOT NULL DEFAULT 1 COMMENT '客户等级：1~5',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    deleted     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删，1 已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户信息表';

-- 初始化用户（admin 密码为 123456 的 BCrypt 哈希）
INSERT INTO sys_user (username, password, nickname, email, status)
VALUES ('admin', '$2a$10$GmqsPHxsNvI6AADStwcmIexhUx.crgfbZOZR5ypkmHz99/psC0Wi6', '超级管理员', 'admin@example.com', 1);

-- 初始化客户（身份证号为通过加权因子校验的合法号码，用于演示脱敏效果）
INSERT INTO biz_customer (name, phone, id_card, email, address, gender, level, remark)
VALUES ('张三', '13800138000', '110101199003077758', 'zhangsan@163.com', '北京市朝阳区建国路 88 号', 1, 3, '重点客户'),
       ('欧阳娜娜', '13912345678', '440305199207118833', 'ouyangnana@qq.com', '广东省深圳市南山区科技园', 0, 5, 'VIP 客户'),
       ('王小明', '18600001234', '11010119871122331X', 'wangxm@gmail.com', '上海市浦东新区张江高科技园区', 1, 1, NULL);
