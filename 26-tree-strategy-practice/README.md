# 第26章：树形结构与策略模式实战

## 一、业务背景

后台管理系统的左侧菜单、权限树、部门树等，数据库中通常以**扁平结构**存储（`id` / `parent_id`），但前端组件（Element Tree、Ant Design Tree）需要**嵌套 children** 的树形结构。

电商/支付系统中的**支付方式**、**促销折扣**等业务规则经常变化，使用**策略模式**可以把变化点封装成可插拔的算法族，避免大面积 `if-else`。

本章使用 **Spring Boot + Hutool TreeUtil + 策略模式 + Vue3 + Element Plus** 完整实战这两个高频场景。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7.18、Hutool 5.8.x、Lombok、Validation、Knife4j |
| 前端 | Vue3 + Vite + Element Plus + Axios |
| 构建 | Maven、npm |

## 三、项目结构

```text
26-tree-strategy-practice
├── src/main/java/com/example/ts
│   ├── TreeStrategyApplication.java      # 启动类
│   ├── common/                           # 统一返回、业务异常、全局异常处理
│   ├── config/                           # 跨域、Knife4j 文档
│   ├── controller/                       # MenuController / PaymentController / DiscountController
│   ├── dto/                              # 请求/响应 DTO
│   ├── entity/                           # Menu 实体
│   ├── service/                          # MenuService / PaymentService / DiscountService
│   └── strategy/                         # 策略接口与实现
│       ├── discount/                     # 折扣策略族
│       └── payment/                      # 支付策略族
├── src/test/java/com/example/ts/service  # 单元测试
├── web/                                  # Vue3 前端
└── pom.xml
```

## 四、接口清单

启动后访问：http://localhost:8080/doc.html

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| POST | /api/menu | 新增菜单 |
| GET | /api/menu/tree | 获取菜单树 |
| GET | /api/menu/list | 获取扁平菜单列表 |
| DELETE | /api/menu/{id} | 删除菜单（含子菜单） |
| POST | /api/pay | 发起支付 |
| POST | /api/discount | 计算折扣后金额 |

## 五、运行方式

### 后端

```bash
cd 26-tree-strategy-practice
mvn spring-boot:run
```

### 前端

```bash
cd 26-tree-strategy-practice/web
npm install
npm run dev
```

浏览器打开 http://localhost:5173 即可体验。

## 六、八股速记

### 6.1 Hutool TreeUtil 构建树的核心思路

1. 准备扁平节点列表，每个节点必须有 `id`、`parentId`；
2. 通过 `TreeNodeConfig` 配置字段映射（默认 `id` / `parentId` / `weight` / `name`）；
3. 调用 `TreeUtil.build(list, rootParentId, config, nodeParser)` 得到嵌套 `Tree` 列表；
4. `weight` 字段用于同级排序，值越小越靠前。

### 6.2 策略模式（Strategy Pattern）

1. 定义算法族，分别封装起来，让它们可以互相替换；
2. 算法的变化独立于使用算法的客户；
3. 在 Spring 中，把所有实现类注入到 `Map<String, Strategy>`，运行时根据类型标识动态选择；
4. 新增策略只需新增实现类，符合**开闭原则**。

### 6.3 什么时候用策略模式？

- 支付方式（支付宝/微信/余额/银联）；
- 促销折扣（普通/VIP/满减/秒杀）；
- 消息推送（短信/邮件/APP 推送/微信模板消息）；
- 文件存储（本地/阿里云 OSS/MinIO）。

## 七、测试验证

```bash
# 后端测试
cd 26-tree-strategy-practice
mvn test

# 前端构建
cd 26-tree-strategy-practice/web
npm run build
```

## 八、作者

Yue021130 <169446203@qq.com>
