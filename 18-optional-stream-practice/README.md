# 18-optional-stream-practice：Java Optional + Stream 真实业务场景实践

「java高级知识」系列第 18 个专题。与第 06 章（Stream / Lambda / Optional 基础）形成互补：
**06 章讲 API 本身，18 章讲工程化落地**。

本模块把 `Optional` 与 `Stream` 的组合放到 8 个贴近真实业务的 Spring Boot 场景中：
用户画像聚合、订单报表统计、菜单权限树、批量数据清洗、SKU 最优价格、消息通知过滤、Excel 导入校验、分页结果再加工，
外加一组**反模式对比**（错误写法 vs 正确写法）。每个场景都有完整的中文注释、可交互的 Vue 3 前端面板，以及对应的面试八股。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0，端口 **8098**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5191**
- 数据：全部内存 Mock，不需要 Redis / 数据库，`mvn spring-boot:run` 即跑

## 项目目标

1. 掌握 `Optional` 与 `Stream` 在真实业务中的**组合模式**，而不是孤立使用。
2. 理解 `Optional` 的**正确打开方式**：链式解包、惰性默认值、副作用隔离。
3. 识别并规避 5 大常见反模式：`get()` 裸用、`isPresent + get`、滥用 `orElse`、Stream 里修改外部变量、把 Optional 当字段。
4. 通过 MockMvc 测试与单元测试验证行为，确保每个场景可运行、可复现。

## 全场景一览

| 模块 | 场景 | 核心端点 | Optional + Stream 结合点 | 面试考点 |
| --- | --- | --- | --- | --- |
| 01 用户画像 | 聚合用户画像 | `GET /api/userprofile/aggregate` | `Optional` 解包用户 → `Stream` 聚合订单 | 防御式编程、空值安全链 |
| 02 订单报表 | 汇总与分组 | `GET /api/report/summary` | `Optional` 默认时间范围 → `Stream` 分组汇总 | 默认值、按时间窗口聚合 |
| 03 菜单权限 | 权限树构建 | `GET /api/permission/tree` | `Optional.flatMap` 解嵌套 → `Stream` 递归建树 | flatMap、递归流 |
| 04 数据清洗 | 批量清洗 | `GET /api/dataclean/clean` | `Optional` 单字段清洗 → `Stream` 批量过滤 | 数据质量、空对象模式 |
| 05 SKU 价格 | 最优价格 | `GET /api/sku/best-price` | `Optional` 解包商品 → `Stream.min/max` | 惰性默认值、极值 |
| 06 消息通知 | 通知过滤 | `GET /api/notification/filter` | `Optional.ifPresent` 审计 → `Stream` 纯过滤 | 副作用隔离 |
| 07 Excel 导入 | 校验转换 | `GET /api/excelimport/validate` | `Optional` 链式校验 → `Stream` 错误聚合 | 校验链、错误聚合 |
| 08 分页加工 | 分页再加工 | `GET /api/paging/transform` | `Optional` 解包分页列表 → `Stream` 排序转换 | 集合解包、peek 调试 |
| 09 反模式 | 错误 vs 正确 | `GET /api/pitfall/wrong-vs-right` | 4 组经典反模式对照 | 最佳实践 |

## 模块结构

```
18-optional-stream-practice/
├── pom.xml
├── README.md
├── src/main/resources/application.yml
├── src/main/java/com/example/os/
│   ├── OptionalStreamPracticeApplication.java
│   ├── common/                    # ApiResponse、GlobalExceptionHandler
│   ├── config/                    # OpenApiConfig、CorsConfig、PracticeProperties
│   ├── domain/                    # User、Order、Product、Sku、Menu、Notification、ImportRow
│   ├── support/                   # MockDataRepository（内存 DAO）
│   ├── userprofile/               # 01 用户画像聚合
│   ├── report/                    # 02 订单报表统计
│   ├── permission/                # 03 菜单权限树
│   ├── dataclean/                 # 04 批量数据清洗
│   ├── sku/                       # 05 SKU 最优价格
│   ├── notification/              # 06 消息通知过滤
│   ├── excelimport/               # 07 Excel 导入校验
│   ├── paging/                    # 08 分页再加工
│   └── pitfall/                   # 09 反模式对比
├── src/test/java/com/example/os/  # ScenarioApiTest、OptionalStreamUnitTest
└── web/                           # Vue 3 + Vite 前端面板（5191）
```

## 快速启动

### 后端

```bash
cd 18-optional-stream-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8098/swagger-ui/index.html

### 前端

```bash
cd 18-optional-stream-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5191

### 运行测试

```bash
cd 18-optional-stream-practice
mvn test
```

- `ScenarioApiTest`：MockMvc 调用全部 19 个接口，断言返回 200 且 data 非空。
- `OptionalStreamUnitTest`：验证 `orElse` 立即求值、`orElseGet` 惰性求值、空集合安全解包、Java 8 中 Optional 展平等行为。

## 接口速查

### 01. 用户画像聚合 `/api/userprofile`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/userprofile/aggregate?userId=1` | GET | VIP + 邮箱有效才聚合订单 |
| `/api/userprofile/explain` | GET | 八股速记 |

### 02. 订单报表统计 `/api/report`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/report/summary?days=30` | GET | 最近 N 天汇总 + Top3 用户 |
| `/api/report/by-status` | GET | 按状态分组计数与求和 |
| `/api/report/explain` | GET | 八股速记 |

### 03. 菜单权限树 `/api/permission`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/permission/tree?roleCode=admin` | GET | 按角色构建菜单树 |
| `/api/permission/explain` | GET | 八股速记 |

### 04. 批量数据清洗 `/api/dataclean`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/dataclean/clean?maxRows=100` | GET | 清洗脏数据，显式丢弃错误行 |
| `/api/dataclean/explain` | GET | 八股速记 |

### 05. SKU 最优价格 `/api/sku`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/sku/best-price?productId=1` | GET | 过滤有效 SKU 后取最低价/最高价 |
| `/api/sku/explain` | GET | 八股速记 |

### 06. 消息通知过滤 `/api/notification`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/notification/filter?userId=1&type=PROMOTION` | GET | 按用户/类型/已读/时间过滤 |
| `/api/notification/explain` | GET | 八股速记 |

### 07. Excel 导入校验 `/api/excelimport`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/excelimport/validate` | GET | 逐行校验并聚合错误 |
| `/api/excelimport/explain` | GET | 八股速记 |

### 08. 分页再加工 `/api/paging`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/paging/transform?page=1&size=5` | GET | 分页后过滤、排序、字段转换 |
| `/api/paging/explain` | GET | 八股速记 |

### 09. 反模式对比 `/api/pitfall`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pitfall/wrong-vs-right` | GET | 4 组错误 vs 正确写法 |
| `/api/pitfall/explain` | GET | 反模式清单与最佳实践 |

## 场景详解

### 01. 用户画像聚合

**业务背景**：CRM 系统根据用户 ID 拉取会员信息，只有 VIP 且邮箱不为空时才做订单聚合，否则返回降级画像。

**关键代码模式**：

```java
return repository.findUserById(userId)
        .filter(this::isQualifiedForProfile)   // VIP + 邮箱有效
        .map(this::buildProfile)                // Stream 聚合订单
        .orElseGet(() -> fallbackProfile(userId));
```

**为什么这样写**：把“是否存在”、“是否满足条件”、“如何聚合”分成三个链式步骤，避免多层 if-null 嵌套。

### 02. 订单报表统计

**业务背景**：运营后台按时间范围汇总订单，并给出 Top 3 消费用户。

**关键代码模式**：

```java
int rangeDays = Optional.ofNullable(days)
        .filter(d -> d > 0)
        .orElse(properties.getDefaultDateRangeDays());
```

**为什么这样写**：前端不传时间范围时，从配置里取默认值；传了非法值时，也回到默认值，保证接口稳定。

### 03. 菜单权限树

**业务背景**：RBAC 系统根据角色编码返回前端树形菜单。

**关键代码模式**：

```java
List<Menu> allowedMenus = repository.getMenus().stream()
        .map(m -> Optional.ofNullable(m).filter(menu -> allowedMenuIds.contains(menu.getId())))
        .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
        .collect(Collectors.toList());
```

**为什么这样写**：Java 8 没有 `Optional.stream()`，用 `flatMap` 把 `Optional<Menu>` 展平成 `Stream<Menu>`，再进入后续分组递归。

### 04. 批量数据清洗

**业务背景**：ETL / 批量导入前清洗原始数据，字段去空、类型转换、范围校验，脏数据显式丢弃。

**关键代码模式**：

```java
String name = Optional.ofNullable(raw.get("name"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .orElse(null);
if (name == null) {
    return Optional.empty();   // 整条丢弃
}
```

**为什么这样写**：每个字段的清洗逻辑都是一段独立的 Optional 链，失败时返回 `Optional.empty()`，上层 Stream 负责收集成功/失败清单。

### 05. SKU 最优价格

**业务背景**：商品详情页展示最低价 SKU 与价格区间。

**关键代码模式**：

```java
BigDecimal minPrice = validSkus.stream()
        .map(Sku::getPrice)
        .min(Comparator.naturalOrder())
        .orElse(BigDecimal.ZERO);
```

**为什么这样写**：`min/max` 返回 `Optional`，没有元素时用 `orElse` 给默认值，避免 `NoSuchElementException`。

### 06. 消息通知过滤

**业务背景**：消息中心按用户、类型、已读状态、时间窗口过滤通知，并记录审计日志。

**关键代码模式**：

```java
List<Map<String, Object>> filtered = repository.getNotifications().stream()
        .filter(n -> userId == null || userId.equals(n.getUserId()))
        // ... 更多过滤
        .map(this::toView)
        .collect(Collectors.toList());

Optional.ofNullable(userId)
        .filter(id -> id > 0)
        .ifPresent(id -> log.info("[通知过滤] 用户 {} 查询...", id, filtered.size()));
```

**为什么这样写**：Stream 只做无状态转换/过滤；副作用（日志）用 `Optional.ifPresent` 显式隔离，便于测试与并行化。

### 07. Excel 导入校验

**业务背景**：批量导入订单/用户时逐行校验，汇总错误行号与原因。

**关键代码模式**：

```java
Integer age = Optional.ofNullable(row.getAge())
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .flatMap(this::parseIntOpt)
        .filter(a -> a > 0 && a < 150)
        .orElseGet(() -> { errors.add("年龄非法"); return null; });
```

**为什么这样写**：把每个字段的“非空、格式、范围”校验写成 Optional 链，失败时通过 `orElseGet` 记录错误，最后统一返回成功/失败两份清单。

### 08. 分页再加工

**业务背景**：分页查询拿到 `Page<T>` 后，对 records 做二次加工：过滤、排序、字段裁剪。

**关键代码模式**：

```java
List<Map<String, Object>> transformed = Optional.ofNullable(pageRecords)
        .filter(list -> !list.isEmpty())
        .map(list -> list.stream()
                .peek(u -> log.debug("处理用户: {}", u.getName()))
                .filter(u -> u.getEmail() != null)
                .sorted(Comparator.comparing(this::levelPriority).reversed())
                .map(this::toView)
                .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
```

**为什么这样写**：分页结果可能越界或为空，用 Optional 解包后进入 Stream 链，最终给调用方一个稳定的空列表而不是 null。

## 面试八股

### Optional 能做什么、不能做什么？

**能**：明确表达“值可能不存在”，强迫调用方处理空值，减少 NPE。
**不能**：不能消除空值本身，也不能替代所有 null 检查。
**不能滥用为字段或方法参数**：字段会让实体类臃肿、序列化麻烦；方法参数会让调用方也不得不包 Optional，增加噪音。Optional **最适合作为返回值类型**。

### orElse 和 orElseGet 的区别？

- `orElse(T other)`：**立即求值**，无论 Optional 是否有值都会先计算 other。
- `orElseGet(Supplier<? extends T> supplier)`：**惰性求值**，只有 Optional 为空时才执行 supplier。

**生产注意**：默认值来自数据库查询、构造大对象、复杂计算时，必须用 `orElseGet`，否则每次调用都浪费资源。

### map 和 flatMap 在 Optional 里的区别？

- `map(Function)`：把值映射成新值，返回 `Optional<U>`。
- `flatMap(Function)`：把值映射成 `Optional<U>`，然后“拍平”成一层 Optional。

**使用场景**：如果映射函数本身返回 Optional，用 flatMap 避免 `Optional<Optional<U>>` 的嵌套。

### Optional.isPresent() + get() 为什么是不推荐写法？

因为这就回到了 `if (x != null)` 的老路，失去了 Optional 的链式表达能力。能写成 `map/filter/orElse` 链式的，就不要拆成 `isPresent + get`。

### Optional 和 Stream 结合处理空集合时，怎样才能既安全又优雅？

```java
List<X> result = Optional.ofNullable(list)
        .filter(l -> !l.isEmpty())
        .map(l -> l.stream().map(...).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
```

**要点**：始终返回非 null 集合，调用方不需要再判空。

### Stream 里怎么安全地过滤/转换可能为空的对象？Java 8 怎么实现 flatMap(Optional::stream)？

Java 9+ 可以写：

```java
list.stream()
    .map(Optional::ofNullable)
    .flatMap(Optional::stream)
    .collect(Collectors.toList());
```

Java 8 等价实现：

```java
list.stream()
    .map(Optional::ofNullable)
    .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
    .collect(Collectors.toList());
```

### 真实业务中如何用 Optional + Stream 避免多层 if-null 嵌套？

把“对象是否存在”、“字段是否有效”、“集合如何聚合”拆成 Optional/Stream 链：

```java
return findUser(userId)
        .filter(u -> u.getLevel() == VIP)
        .map(u -> buildProfile(u, ordersOf(u)))
        .orElseGet(() -> fallbackProfile(userId));
```

### peek 能不能修改元素？它适合做什么、不适合做什么？

`peek` 可以修改元素（因为拿到的是对象引用），但**不应该**修改元素。它只适合调试、日志、观察流中间状态。

**不适合**：做业务计算、修改对象状态、数据库查询、发送消息。这些应放在终端操作（forEach、collect）或 Stream 外部。

### 分组统计时，如果 key 可能为空，怎么用 Optional 兜底？

```java
Map<String, Long> count = list.stream()
        .collect(Collectors.groupingBy(
                item -> Optional.ofNullable(item.getCategory()).orElse("未分类"),
                Collectors.counting()
        ));
```

直接用可能为 null 的字段做 groupingBy key 会抛 NPE，先用 Optional 包装转换。

### 为什么说“不要返回 null，返回 Optional.empty()”是现代 Java 的推荐做法？

返回 null 会让调用方承担“要不要判空”的决策负担，容易遗漏。返回 `Optional<T>` 后，调用方必须显式选择 `orElse / orElseGet / orElseThrow / ifPresent`，把空值处理从“约定”变成“编译期可见的契约”。

## 推荐实验顺序

1. **09 反模式对比**：先看 `wrong-vs-right`，建立正确意识，特别注意 orElse 与 orElseGet 的调用次数差异。
2. **01 用户画像聚合**：观察 VIP 用户 vs 非 VIP 用户的降级行为。
3. **02 订单报表统计**：尝试 days=7 / 30 / 365，看汇总与 Top3 变化。
4. **03 菜单权限树**：切换 admin / user / guest，看树形结构变化。
5. **04 批量数据清洗**：看哪些脏数据被丢弃，以及清洗后的样本。
6. **05 SKU 最优价格**：观察 productId=999 时 orElseGet 的兜底结果。
7. **06 消息通知过滤**：userId/type 都为空时看全量过滤，再看日志区审计输出。
8. **07 Excel 导入校验**：看成功行、失败行、空值率与 allowPersist 判断。
9. **08 分页再加工**：page=10 越界，看 Optional 如何安全返回空列表。

## 参考

- [Java 8 Optional 官方文档](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html)
- [Java 8 Stream 官方文档](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)
- [Oracle: Optional 最佳实践](https://www.oracle.com/technical-resources/articles/java/java8-optional.html)
- [Spring Boot 2.7 官方文档](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
