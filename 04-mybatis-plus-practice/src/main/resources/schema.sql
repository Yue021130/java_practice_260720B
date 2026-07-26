-- H2 内存数据库建表脚本
-- 启动时由 spring.sql.init 自动执行

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100),
    gender INT DEFAULT 0 COMMENT '0-未知 1-男 2-女',
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    deleted INT DEFAULT 0 COMMENT '0-未删除 1-已删除',
    version INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status INT DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用于演示 @TableField 高级属性：select/condition/update/numericScale
CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    login_count INT DEFAULT 0,
    balance DECIMAL(12, 2) DEFAULT 0.00
);

-- 用于演示 @KeySequence 序列主键
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

-- H2 序列，供 @KeySequence 演示使用
CREATE SEQUENCE IF NOT EXISTS seq_product START WITH 100 INCREMENT BY 1;

-- 用于演示自定义注解 + AOP：自动填充 create_by / update_by
CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT PRIMARY KEY,
    content VARCHAR(200) NOT NULL,
    status INT DEFAULT 0 COMMENT '0-待处理 1-已完成',
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用于演示 TypeHandler + ActiveRecord：extra 以 JSON 字符串存储
CREATE TABLE IF NOT EXISTS t_article (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000),
    extra VARCHAR(1000),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用于演示动态表名（按月分表）：逻辑表名为 t_report，实际表为 t_report_202401 / t_report_202402
CREATE TABLE IF NOT EXISTS t_report_202401 (
    id BIGINT PRIMARY KEY,
    report_month VARCHAR(20),
    content VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS t_report_202402 (
    id BIGINT PRIMARY KEY,
    report_month VARCHAR(20),
    content VARCHAR(500)
);
