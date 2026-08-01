# 06-stream-lambda-practice —— Stream / Lambda / Optional 全场景实践

「java高级知识」系列第 6 个专题。系统覆盖 **Java 8 Lambda 表达式、Stream API、Collectors、Optional、基本类型流、并行流的正确使用与踩坑**，
把每个知识点包装成可运行的现实业务场景，配合 **Vue 3 + Vite** 前端面板，边跑边学。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok 1.18.30 + springdoc-openapi 1.7.0，端口 **8086**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5179**

## 项目目标

1. 通过 14 个可交互场景掌握 Lambda、Stream、Collectors、Optional、IntStream 与并行流核心 API。
2. 用 2 万条内存数据（Employee / Order）演示真实聚合与分析场景。
3. 通过并行流 5 个对比实验理解：什么时候该用、什么时候不该用、为什么共享可变状态会出错、怎样正确聚合。

## 全场景一览（5 大模块，14 个实验）

| 模块 | 场景 | 对应端点 | 面试考点 |
| --- | --- | --- | --- |
| Lambda 基础 | Lambda 与函数式接口 | `POST /api/lambda/functional` | Predicate / Function / Consumer / Supplier、@FunctionalInterface |
| Lambda 基础 | 方法引用 | `POST /api/lambda/method-ref` | 静态、实例、构造方法引用；Class::method 与 lambda 等价 |
| Stream 基础 | Stream 创建方式 | `POST /api/stream/create` | collection.stream()、Stream.of、IntStream.range、iterate、generate |
| Stream 基础 | 中间操作 | `POST /api/stream/intermediate` | filter / map / flatMap / distinct / sorted / peek / limit / skip |
| Stream 基础 | 终止操作 | `POST /api/stream/terminal` | collect / reduce / forEach / findFirst / anyMatch / max |
| Collectors | 分组与分区 | `POST /api/collectors/group-partition` | groupingBy / partitioningBy / counting / averagingInt |
| Collectors | 字符串拼接与统计 | `POST /api/collectors/join-summary` | joining / summarizingInt / maxBy / reducing |
| Optional | 空值安全 | `POST /api/optional/safe` | ofNullable / map / filter / orElse / orElseThrow / ifPresent |
| 基本类型流 | IntStream / LongStream / DoubleStream | `POST /api/stream/primitive` | mapToInt / boxed / sum / average / range / rangeClosed |
| 并行流 | 并行加速场景 | `POST /api/parallel/speedup` | CPU 密集型大集合、ForkJoinPool common pool |
| 并行流 | 并行额外开销 | `POST /api/parallel/overhead` | 小集合 / 简单操作反而更慢 |
| 并行流 | 线程不安全错误示范 | `POST /api/parallel/race-condition` | 共享可变变量 + forEach 导致结果错误 |
| 并行流 | 正确聚合 | `POST /api/parallel/correct-reduce` | reduce / collect 保证结合律与无状态 |
| 并行流 | 顺序与 findAny | `POST /api/parallel/order-findany` | ordered / unordered、findFirst vs findAny |

## 面试八股速记

### Lambda & 函数式接口

- **函数式接口**：只有一个抽象方法的接口，可用 `@FunctionalInterface` 标注，如 `Runnable`、`Comparator`、`Predicate<T>`、`Function<T,R>`、`Consumer<T>`、`Supplier<T>`。
- **Lambda 本质**：函数式接口实例的语法糖，编译后生成 invokedynamic + LambdaMetafactory。
- **方法引用**：`对象::实例方法`、`类::静态方法`、`类::实例方法`、`类::new`，要求参数/返回值匹配。

### Stream

- **不会修改数据源**，操作分为中间操作（返回 Stream）和终止操作（触发计算）。
- **中间操作**：filter、map、flatMap、distinct、sorted、peek、limit、skip 都是懒执行。
- **终止操作**：collect、reduce、forEach、findFirst/anyMatch、max/min/count；Stream 只能消费一次。
- **基本类型流**：`mapToInt` / `mapToLong` / `mapToDouble` 避免装箱，提供 sum/average/max/range 等专用方法。

### Collectors

- **groupingBy**：按属性分组，可嵌套 counting/averagingInt/summingInt 等下游收集器。
- **partitioningBy**：按 boolean 二分，返回 `Map<Boolean, List<T>>`。
- **joining / summarizingInt / maxBy / reducing**：分别用于字符串拼接、一次性统计、取最大、自定义聚合。

### Optional

- 用于**明确表达可能为空**，避免 NPE，但不要作为字段或方法参数滥用。
- `ofNullable` → `map/filter` → `orElse/orElseGet/orElseThrow/ifPresent` 链式处理。
- `orElse` 立即求值，`orElseGet` 惰性求值。

### 并行流

- **适合用**：CPU 密集型、数据量大、无状态、数据源可高效拆分（如 `IntStream.range`、数组）。
- **不适合用**：数据量小、操作简单、IO 阻塞（会占满 common pool）、需要严格顺序保证。
- **线程安全**：不要在 lambda / forEach 中修改外部可变变量；应使用 `reduce` / `collect` 做分治合并。
- **顺序**：有序并行流 `findFirst` 稳定，`findAny` 可能返回任意元素；`unordered()` 可取消顺序约束提升性能。

## 启动步骤

后端（项目根目录）：

```bash
mvn spring-boot:run
```

前端（另开一个终端）：

```bash
cd web
npm install
npm run dev
```

浏览器打开 http://localhost:5179 即可看到实验面板。

## 接口文档（Swagger UI）

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8086/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8086/v3/api-docs

## 运行测试

后端自带集成测试（JUnit 5 + MockMvc + AssertJ）：

```bash
mvn test
```

- `ScenarioApiTest`：通过 MockMvc 调用全部 14 个场景接口，验证均返回 200 且 data 非空。
- `raceConditionProducesMismatch`：断言 `/api/parallel/race-condition` 的实际 size 小于预期。
- `correctReduceEqualsExpected`：断言 `/api/parallel/correct-reduce` 的 reduce 与 collect 结果均等于预期值。

## 项目结构

```
06-stream-lambda-practice/
├── pom.xml
├── README.md
├── src/main/resources/application.yml
├── src/main/java/com/example/sl/
│   ├── StreamLambdaPracticeApplication.java
│   ├── common/ApiResponse.java
│   ├── domain/          Employee.java、Order.java
│   ├── data/            MockDataGenerator.java
│   ├── basic/           LambdaController / LambdaService
│   ├── stream/          StreamController / StreamService
│   ├── collectors/      CollectorsController / CollectorsService
│   ├── optional/        OptionalController / OptionalService
│   └── parallel/        ParallelController / ParallelService
├── src/test/java/com/example/sl/ScenarioApiTest.java
└── web/                 Vue 3 + Vite 前端
```

## 并行流使用 / 禁用 checklist

| 场景 | 建议 | 原因 |
| --- | --- | --- |
| 大集合 + CPU 密集型计算 | ✅ 使用 parallelStream | 充分利用多核，ForkJoinPool 拆分任务 |
| 小集合 / 简单 map + 1 | ❌ 避免 parallelStream | 拆分/线程调度开销大于收益 |
| 涉及共享可变变量 | ❌ 禁用 | 产生竞争，结果不可预期 |
| 需要严格顺序保证 | ⚠️ 谨慎 | 可用 findFirst，但性能会受限 |
| IO 密集型 / 阻塞操作 | ⚠️ 自定义线程池 | common pool 被占满会影响其他并行流 / CompletableFuture |
| reduce / collect 操作 | ✅ 确保无状态 + 结合律 | 否则并行结果错误 |
