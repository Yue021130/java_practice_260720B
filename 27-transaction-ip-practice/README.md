# 第27章：事务失效与 IP 归属地实战

## 一、业务背景

`@Transactional` 是 Spring 中最常用的注解之一，但线上事务失效的案例屡见不鲜。本章以转账业务为例，用 H2 内存数据库演示 5 种典型的事务失效场景，并给出修复方案。

同时，IP 归属地查询在日志审计、风控、运营分析中非常常见。本章使用 **ip2region** 离线库实现 IP 解析，并内置 fallback 数据保证示例可闭环运行。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、MyBatis-Plus、H2、ip2region、Knife4j |
| 前端 | Vue3 + Vite + Element Plus + Axios |
| 构建 | Maven、npm |

## 三、项目结构

```text
27-transaction-ip-practice
├── src/main/java/com/example/tip
│   ├── TransactionIpApplication.java   # 启动类（@EnableAsync）
│   ├── common/                         # 统一返回、业务异常、全局异常处理
│   ├── config/                         # 跨域、Knife4j、MyBatis-Plus
│   ├── controller/                     # TransactionDemoController / IpRegionController
│   ├── dto/                            # 请求/响应 DTO
│   ├── entity/                         # Account 实体
│   ├── mapper/                         # AccountMapper
│   ├── service/                        # TransactionDemoService / IpRegionService
│   └── util/                           # Ip2RegionUtil
├── src/main/resources
│   ├── application.yml
│   ├── schema.sql                      # H2 建表
│   ├── data.sql                        # 初始化数据
│   └── ip2region.xdb                   # 离线 IP 库（可选，未提供时启用内置 fallback）
├── src/test/java                       # 单元测试
├── web/                                # Vue3 前端
└── pom.xml
```

## 四、接口清单

启动后访问：http://localhost:8080/doc.html

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| GET | /api/tx/scenes | 运行全部事务失效场景 |
| POST | /api/tx/transfer | 正常转账 |
| GET | /api/ip/search?ip=xxx | 查询 IP 归属地 |

## 五、运行方式

### 后端

```bash
cd 27-transaction-ip-practice
mvn spring-boot:run
```

### 前端

```bash
cd 27-transaction-ip-practice/web
npm install
npm run dev
```

浏览器打开 http://localhost:5173 即可体验。

## 六、事务失效场景说明

| 场景 | 是否回滚 | 原因 | 修复方案 |
| --- | --- | --- | --- |
| 同类自调用 | ❌ | `this.method()` 不走代理，事务注解未被 Spring 处理 | 通过 `@Lazy` 注入自身代理对象调用 |
| 异常被吞掉 | ❌ | 方法内部 try-catch 吞掉异常，Spring 无法感知 | 捕获后重新抛出，或去掉 try-catch |
| rollbackFor 不匹配 | ❌ | 默认只回滚 `RuntimeException`，受检异常不会回滚 | 显式指定 `rollbackFor = Exception.class` |
| 非 public 方法 | ❌ | `@Transactional` 只能作用于 public 方法 | 改为 public 方法 |
| @Async 异步事务 | ✅（自身） | 异步方法在独立线程中执行，调用方无法同步感知 | 配合返回值/回调，或事务同步机制 |

## 七、八股速记

### 7.1 @Transactional 失效的 8 大场景

1. 同类自调用（this 调用）；
2. 方法不是 public；
3. 异常被 try-catch 吞掉；
4. `rollbackFor` 配置错误；
5. 数据库引擎不支持事务（如 MyISAM）；
6. 传播行为配置错误；
7. 类未被 Spring 管理；
8. 同一个方法上同时加 @Async 和 @Transactional。

### 7.2 ip2region 是什么？

1. 离线 IP 地址定位库，数据文件仅几 MB；
2. 支持 memory、vectorIndex、cache 多种查询算法；
3. 返回格式：`国家|区域|省份|城市|ISP`；
4. 生产环境建议下载官方最新 xdb 数据文件。

## 八、测试验证

```bash
# 后端测试
cd 27-transaction-ip-practice
mvn test

# 前端构建
cd 27-transaction-ip-practice/web
npm run build
```

## 九、作者

Yue021130 <169446203@qq.com>
