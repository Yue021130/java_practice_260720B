# 03-collections-generics-practice —— Java 集合与泛型全场景实践

「java高级知识」系列第 3 个专题。系统覆盖 **Java Collections Framework** 与 **Java 泛型**，
把每个知识点包装成可运行的现实业务场景，配合 **Vue 3 + Vite** 前端面板与中文「面试八股」注释，边跑边学。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven，端口 **8083**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5176**

## 全场景一览（8 大模块，29 个实验）

| 模块 | 场景 | 对应端点 | 面试考点 |
| --- | --- | --- | --- |
| List | ArrayList vs LinkedList | `POST /api/list/arraylist-vs-linkedlist` | 数组 vs 链表、复杂度 |
| List | subList 陷阱 | `POST /api/list/sublist-trap` | 视图 vs 拷贝 |
| List | ListIterator 双向迭代 | `POST /api/list/listiterator` | 双向/set/add |
| List | fail-fast 迭代器 | `POST /api/list/iterator-failfast` | modCount、Iterator.remove |
| Set | HashSet 去重 | `POST /api/set/hashset-dedup` | equals/hashCode |
| Set | LinkedHashSet 保序 | `POST /api/set/linkedhashset-order` | 底层 LinkedHashMap |
| Set | TreeSet 排序 | `POST /api/set/treeset-sort` | 红黑树、Comparable/Comparator |
| Set | equals/hashCode 契约 | `POST /api/set/equals-hashcode-contract` | 四个性质 |
| HashMap | HashMap 内部机制 | `POST /api/maphash/hashmap-internals` | 扰动、2 的幂、负载因子 |
| HashMap | key 被修改后丢失 | `POST /api/maphash/key-mutation` | key 必须不可变 |
| HashMap | Map 对比 | `POST /api/maphash/map-compare` | HashMap/Hashtable/CHM、null |
| 有序 Map | LinkedHashMap LRU | `POST /api/mapordered/linkedhashmap-lru` | accessOrder、removeEldestEntry |
| 有序 Map | TreeMap 排序与范围 | `POST /api/mapordered/treemap-sort` | 红黑树、subMap |
| 有序 Map | WeakHashMap 弱引用 | `POST /api/mapordered/weakhashmap-cache` | key 弱引用、GC 清理 |
| Queue | PriorityQueue TopK | `POST /api/queue/priorityqueue-topk` | 堆、O(NlogK) |
| Queue | ArrayDeque 双端队列 | `POST /api/queue/arraydeque-dual` | 栈/队列、比 Stack/LinkedList 快 |
| 工具类 | sort + binarySearch | `POST /api/utils/sort-binarysearch` | 先排序、Comparator 链 |
| 工具类 | synchronized / unmodifiable | `POST /api/utils/synchronized-unmodifiable` | 包装器本质 |
| 工具类 | Arrays.asList 陷阱 | `POST /api/utils/arrays-aslist-trap` | 固定大小视图 |
| 工具类 | Collections.shuffle | `POST /api/utils/shuffle` | Random 种子可复现 |
| 泛型 | PECS 原则 | `POST /api/generics/pecs` | extends/super |
| 泛型 | 类型擦除与 raw type | `POST /api/generics/type-erasure` | 编译期特性 |
| 泛型 | 泛型方法 + 类型推断 | `POST /api/generics/generic-method` | `<T extends Comparable>` |
| 泛型 | 泛型 DAO / Service | `POST /api/generics/generic-dao` | BaseRepository<T, ID> |
| 综合实战 | LRU 缓存 | `POST /api/realworld/lru-cache` | LinkedHashMap 实现 |
| 综合实战 | TopK 热词 | `POST /api/realworld/topk-words` | HashMap + PriorityQueue |
| 综合实战 | groupBy 订单 | `POST /api/realworld/groupby` | compute/computeIfAbsent/merge |
| 综合实战 | 通用排序工具 | `POST /api/realworld/comparator-sort` | comparing/thenComparing/nullsFirst |

## 模块面试点速记

### List

- **ArrayList**：底层 `Object[]`，随机访问 O(1)、尾部追加均摊 O(1)、中间插入/删除 O(n)。
- **LinkedList**：双向链表，随机访问 O(n)、头尾操作 O(1)。多数场景优先 ArrayList。
- **subList**：返回视图，修改会联动原 List；想隔离用 `new ArrayList<>(subList(...))`。
- **Iterator / ListIterator**：Iterator 单向 remove；ListIterator 双向，支持 set/add。
- **fail-fast**：结构性修改会改变 `modCount`，迭代器检测到不一致就抛 `ConcurrentModificationException`。

### Set

- **HashSet**：底层 HashMap，去重依赖 `equals` + `hashCode`，两者必须同时重写且一致。
- **LinkedHashSet**：底层 LinkedHashMap，去重同时保持插入顺序。
- **TreeSet**：底层红黑树，元素必须可比或传入 Comparator，操作 O(logN)。
- **equals/hashCode 契约**：自反、对称、传递、一致；equals 相等则 hashCode 必须相等。

### Map

- **HashMap**：
  - `hash()` 高 16 位与低 16 位异或，让高位参与桶定位。
  - 容量始终为 2 的幂，`index = (n-1) & hash` 等价于 `% n` 但更快。
  - 负载因子 0.75 是冲突与扩容的权衡。
  - JDK8：链表长度 ≥8 且容量 ≥64 时树化为红黑树，≤6 时退化回链表。
- **key 不可变**：key 改变导致 hashCode 改变，get 会去错桶，结果找不到。
- **HashMap vs Hashtable vs ConcurrentHashMap**：
  - HashMap：非线程安全，允许 null key/value。
  - Hashtable：全方法 synchronized，不允许 null。
  - CHM：分段/CAS 锁，线程安全，不允许 null（避免二义性）。
- **LinkedHashMap**：`accessOrder=true` 实现 LRU；重写 `removeEldestEntry` 做淘汰。
- **TreeMap**：红黑树，支持 `subMap` / `firstKey` / `lastKey` 范围查询。
- **WeakHashMap**：key 是弱引用，无强引用时 GC 自动清理 Entry。

### Queue / Deque

- **PriorityQueue**：底层小顶堆，offer/poll O(logN)，peek O(1)。非线程安全。
- **ArrayDeque**：双端队列首选，不允许 null；作为栈比 Stack 快，作为队列比 LinkedList 快。

### Collections / Arrays 工具

- `Collections.binarySearch` 前 List 必须先 sort。
- `Collections.synchronizedXXX` 提供方法级锁，但迭代仍需外部同步。
- `Collections.unmodifiableXXX` 只是只读视图，原集合改变视图也变。
- `Arrays.asList` 返回固定大小视图，不能 add/remove；需要可变 List 用 `new ArrayList<>(Arrays.asList(...))`。
- `Collections.shuffle` 用相同 Random 种子可复现同一乱序。

### 泛型

- **PECS**：Producer Extends（只读）、Consumer Super（只写）。`List<? extends Number>` 不能 add Integer。
- **类型擦除**：泛型在运行时擦除为 Object 或边界；Java 泛型是编译期类型检查机制。
- **raw type**：破坏类型安全，新代码应避免。
- **泛型方法**：`<T extends Comparable<T>> T findMax(List<T>)` 等。
- **泛型 DAO**：`BaseRepository<T extends BaseEntity<ID>, ID>` 复用 CRUD 模板。
- **限制**：不能创建泛型数组 `new T[]`，`List<?>[]` 合法但 `List<String>[]` 不合法。

### 综合实战

- **LRU**：LinkedHashMap accessOrder + removeEldestEntry。
- **TopK**：HashMap 词频 + 大小为 K 的 PriorityQueue 小顶堆。
- **groupBy**：`compute` 更新聚合值，`computeIfAbsent` 分组收集，`merge` 累加。
- **排序**：`Comparator.comparing().thenComparing().nullsFirst/nullsLast`。

## 接口文档（Swagger UI）

项目集成了 SpringDoc OpenAPI（`springdoc-openapi-ui:1.7.0`，对应 Spring Boot 2.7），
接口文档根据代码中的 `@Tag` / `@Operation` / `@Parameter` 注解自动生成。

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8083/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8083/v3/api-docs

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

浏览器打开 http://localhost:5176 即可看到监控面板。

> 前端在开发时通过 Vite proxy 把 `/api` 转发到 `http://localhost:8083`。

## 运行测试

后端自带集成测试（JUnit 5 + MockMvc + AssertJ）：

```bash
mvn test
```

测试覆盖：

- `CollectionsGenericsApplicationTests`：Spring 上下文加载。
- `ScenarioApiTest`：通过 MockMvc 调用全部 29 个场景接口，验证均返回 200 且 data 非空。

## 推荐实验顺序

1. **List**：ArrayList/LinkedList 性能、subList 视图、fail-fast 迭代器。
2. **Set**：HashSet 去重、LinkedHashSet 保序、TreeSet 排序、equals/hashCode 契约。
3. **HashMap**：扰动函数、容量 2 的幂、key 不可变、三种 Map 对比。
4. **有序 Map**：LRU、TreeMap 范围查询、WeakHashMap 弱引用。
5. **Queue**：PriorityQueue TopK、ArrayDeque 双端队列。
6. **工具类**：sort+binarySearch、包装器、Arrays.asList 陷阱、shuffle。
7. **泛型**：PECS、类型擦除、泛型方法、泛型 DAO。
8. **综合实战**：LRU、TopK、groupBy、多字段排序。

## 项目结构

```
03-collections-generics-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/cg/
│   ├── CollectionsGenericsApplication.java
│   ├── common/        ApiResponse、ScenarioResult、ScenarioLog、NamedThreadFactory
│   ├── config/        CorsConfig、OpenApiConfig
│   ├── list/          ListController / ListScenarioService
│   ├── set/           SetController / SetScenarioService
│   ├── maphash/       MapHashController / MapHashScenarioService
│   ├── mapordered/    MapOrderedController / MapOrderedScenarioService
│   ├── queue/         QueueController / QueueScenarioService
│   ├── utils/         UtilsController / UtilsScenarioService
│   ├── generics/      GenericsController / GenericsScenarioService
│   └── realworld/     RealWorldController / RealWorldScenarioService
├── src/test/java/com/example/cg/
└── web/               Vue 3 + Vite 前端
```
