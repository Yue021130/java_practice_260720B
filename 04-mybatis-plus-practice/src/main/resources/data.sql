-- H2 内存数据库初始化数据
-- 每次启动先清空，再重新灌入，避免多个测试上下文共享同一内存库时主键冲突
DELETE FROM t_order;
DELETE FROM t_task;
DELETE FROM t_account;
DELETE FROM t_product;
DELETE FROM t_user;
DELETE FROM t_article;
DELETE FROM t_report_202401;
DELETE FROM t_report_202402;

INSERT INTO t_user (id, username, age, email, gender, status, deleted, version, create_time, update_time) VALUES
(1001, '张三', 22, 'zhangsan@example.com', 1, 1, 0, 1, '2024-01-10 10:00:00', '2024-01-10 10:00:00'),
(1002, '李四', 28, 'lisi@example.com', 1, 1, 0, 1, '2024-01-11 11:00:00', '2024-01-11 11:00:00'),
(1003, '王五', 35, 'wangwu@example.com', 1, 0, 0, 1, '2024-01-12 12:00:00', '2024-01-12 12:00:00'),
(1004, '赵六', 19, 'zhaoliu@example.com', 2, 1, 0, 1, '2024-01-13 13:00:00', '2024-01-13 13:00:00'),
(1005, '孙七', 42, 'sunqi@example.com', 2, 1, 0, 1, '2024-01-14 14:00:00', '2024-01-14 14:00:00'),
(1006, '周八', 30, 'zhouba@example.com', 0, 0, 0, 1, '2024-01-15 15:00:00', '2024-01-15 15:00:00'),
(9999, '已删除用户', 25, 'deleted@example.com', 0, 1, 1, 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00');

INSERT INTO t_order (id, user_id, amount, status, create_time) VALUES
(2001, 1001, 199.50, 1, '2024-01-10 10:30:00'),
(2002, 1001,  59.90, 1, '2024-01-11 09:30:00'),
(2003, 1002, 299.00, 1, '2024-01-12 08:30:00'),
(2004, 1002,  88.00, 0, '2024-01-13 07:30:00'),
(2005, 1003, 159.00, 2, '2024-01-14 06:30:00'),
(2006, 1004, 399.00, 1, '2024-01-15 05:30:00'),
(2007, 1005,  19.90, 1, '2024-01-16 04:30:00'),
(2008, 1005, 520.00, 1, '2024-01-17 03:30:00'),
(2009, 1005,  66.60, 0, '2024-01-18 02:30:00');

INSERT INTO t_account (id, username, password, email, login_count, balance) VALUES
(3001, 'account_a', 'secret_a', 'a@example.com', 5, 1234.56),
(3002, 'account_b', 'secret_b', 'b@example.com', 8, 9876.54),
(3003, 'account_c', 'secret_c', 'c@example.com', 12, 500.00);

INSERT INTO t_product (id, name, price) VALUES
(9001, '键盘', 299.00),
(9002, '鼠标', 99.00);
