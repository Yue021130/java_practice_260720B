# 04-mybatis-plus-practice —— Spring Boot + MyBatis-Plus 全注解与核心能力实践

「java高级知识」系列第 4 个专题。系统覆盖 **Spring Boot + MyBatis-Plus 的常用注解与核心能力**，
把每个知识点包装成可运行的现实业务场景，配合 **Vue 3 + Vite** 前端面板与中文「面试八股」注释，边跑边学。

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + MyBatis-Plus 3.5.7 + H2 内存数据库，端口 **8084**
- 前端：Vue 3 + Vite 5 + axios（纯手写 CSS，无 UI 库），开发端口 **5177**

## 全场景一览（10 大模块，46 个实验）

| 模块 | 场景 | 对应端点 | 面试考点 |
| --- | --- | --- | --- |
| 实体类注解 | @TableName 表名映射 | `POST /api/entity/table-name` | 类名与表名不一致 |
| 实体类注解 | @TableId 主键策略 | `POST /api/entity/table-id` | ASSIGN_ID / AUTO |
| 实体类注解 | @TableField 字段映射 | `POST /api/entity/table-field` | exist=false / fill |
| BaseMapper CRUD | insert 插入 | `POST /api/mapper/insert` | 主键回填 |
| BaseMapper CRUD | selectById 查询 | `POST /api/mapper/select-by-id` | 逻辑删除过滤 |
| BaseMapper CRUD | updateById 更新 | `POST /api/mapper/update-by-id` | 非空更新 |
| BaseMapper CRUD | deleteById 删除 | `POST /api/mapper/delete-by-id` | 物理删除 |
| IService CRUD | save 保存 | `POST /api/service/save` | boolean 返回 |
| IService CRUD | saveOrUpdate | `POST /api/service/save-or-update` | 按 id 判存 |
| IService CRUD | list 查询 | `POST /api/service/list` | list / count |
| IService CRUD | page 分页 | `POST /api/service/page` | 分页插件 |
| 条件构造器 | 等值 + 模糊 | `POST /api/wrapper/eq-like` | eq / like |
| 条件构造器 | 范围 + 排序 | `POST /api/wrapper/between-order` | between / orderBy |
| 条件构造器 | Lambda 构造器 | `POST /api/wrapper/lambda` | 类型安全 |
| 条件构造器 | 嵌套 and/or | `POST /api/wrapper/nested` | 优先级控制 |
| 分页查询 | 基础分页 | `POST /api/page/basic` | Page + selectPage |
| 分页查询 | 分页 + 条件 | `POST /api/page/custom` | Wrapper 组合 |
| 高级注解 | 逻辑删除 | `POST /api/advanced/logic-delete` | @TableLogic |
| 高级注解 | 乐观锁 | `POST /api/advanced/optimistic-lock` | @Version |
| 高级注解 | 自动填充 | `POST /api/advanced/auto-fill` | MetaObjectHandler |
| 批量操作 | saveBatch | `POST /api/batch/save-batch` | 分批大小 |
| 批量操作 | updateBatchById | `POST /api/batch/update-batch` | 按 id 批量更新 |
| 综合实战 | 用户订单统计 | `POST /api/realworld/user-order` | 自定义 SQL |
| 综合实战 | 状态分组统计 | `POST /api/realworld/status-stats` | groupBy 聚合 |
| 综合实战 | 综合搜索分页 | `POST /api/realworld/search-page` | 多条件 + 分页 |
| 更多注解 | @KeySequence 序列主键 | `POST /api/more/key-sequence` | H2/Oracle/Postgres |
| 更多注解 | @OrderBy 默认排序 | `POST /api/more/order-by` | 全局默认排序 |
| 更多注解 | @EnumValue 枚举映射 | `POST /api/more/enum-value` | code/desc |
| 更多注解 | @InterceptorIgnore | `POST /api/more/interceptor-ignore` | 关闭 blockAttack |
| 更多注解 | @TableField(select=false) | `POST /api/more/field-select` | 敏感字段不返回 |
| 更多注解 | @TableField(condition) | `POST /api/more/field-condition` | 自定义 WHERE |
| 更多注解 | @TableField(update) | `POST /api/more/field-update` | 自增 SET |
| 更多注解 | @TableField(numericScale) | `POST /api/more/field-numeric-scale` | DECIMAL 精度 |
| 更多注解 | IdType 全策略 | `POST /api/more/id-types` | 5 种主键策略 |
| 更多注解 | FieldStrategy | `POST /api/more/field-strategy` | null/空值策略 |
| 更多注解 | @Order 执行顺序 | `POST /api/more/order` | Spring Bean 顺序 |
| 更多注解 | 自定义注解 + AOP | `POST /api/more/custom-annotation` | @interface + @Aspect |
| 更多注解 | @Accessors 链式/fluent/prefix | `POST /api/more/accessors` | Lombok 链式 setter |
| 扩展实战 | TypeHandler 自定义类型转换 | `POST /api/extension/type-handler` | JSON 字段映射 |
| 扩展实战 | ActiveRecord 模式 | `POST /api/extension/active-record` | extends Model<T> |
| 扩展实战 | 动态表名 | `POST /api/extension/dynamic-table-name` | 按月分表 |
| 扩展实战 | InsertBatchSomeColumn | `POST /api/extension/insert-batch-some-column` | 批量插入 SQL 注入器 |
| 扩展实战 | 链式 Wrapper | `POST /api/extension/chain-wrapper` | lambdaQuery/lambdaUpdate |
| 扩展实战 | 复杂 Wrapper | `POST /api/extension/wrapper-advanced` | inSql / groupBy having |
| 扩展实战 | selectMaps / selectObjs | `POST /api/extension/select-maps` | Map/单列返回值 |
| 扩展实战 | Wrapper 更新 / 删除 | `POST /api/extension/wrapper-update-delete` | 条件更新删除 |

## 模块面试点速记

### 实体类注解

- **@TableName**：指定实体类对应的数据库表名；可通过全局 `table-prefix` 配置统一前缀。
- **@TableId**：指定主键字段与生成策略；`ASSIGN_ID` 雪花算法、`AUTO` 数据库自增、`INPUT` 配合 `@KeySequence`。
- **@TableField**：字段别名、非持久化字段（`exist=false`）、自动填充策略（`fill`）。
- **@KeySequence**：Oracle/PostgreSQL/H2 等支持 sequence 的数据库生成主键；需注册对应 `IKeyGenerator` Bean。
- **@OrderBy**：在实体字段上标注默认排序规则，`list()` / `lambdaQuery()` 会自动追加 `ORDER BY`。
- **@EnumValue**：指定枚举中哪个字段持久化到数据库，通常配合 `@JsonValue` 控制前端展示。
- **@InterceptorIgnore**：临时关闭分页、多租户、防止全表更新/删除等插件；属性包括 `blockAttack`、`tenantLine`、`optimisticLocker`、`illegalSql` 等。
- **@Order**：Spring 控制 Bean 执行顺序，数字越小优先级越高；常用于 `CommandLineRunner`、AOP 切面、拦截器、监听器。
- **自定义注解 + AOP**：用 `@interface` 定义注解，`@Aspect` + `@Before/@Around` 实现切面，常用于操作日志、数据权限、自动填充等横切逻辑。
  - 完整教程见 [`docs/custom-annotation-tutorial.md`](docs/custom-annotation-tutorial.md)。
- **@Accessors**：Lombok 注解；`chain=true` 让 setter 返回 this 支持链式调用；`fluent=true` 省略 get/set 前缀；`prefix` 自动剥离字段前缀。

### @TableField 高级属性

- **select = false**：查询时不出现在 SELECT 列表，适合密码等敏感字段。
- **condition**：自定义 WHERE 条件模板，如 `%s LIKE CONCAT('%%',#{%s},'%%')`。
- **update**：自定义 SET 片段，如 `%s+1` 实现自增。
- **numericScale**：指定 `DECIMAL` 小数位，如 `numericScale = "2"`。
- **exist = false**：标记非持久化字段。
- **fill = FieldFill.INSERT/INSERT_UPDATE**：配合 `MetaObjectHandler` 自动填充。
- **insertStrategy / updateStrategy / whereStrategy**：字段 null/空值策略。

### BaseMapper / IService

- **BaseMapper<T>**：提供 insert、deleteById、updateById、selectById、selectList、selectPage 等方法。
- **IService<T> / ServiceImpl<M, T>**：在 Service 层提供 save、saveOrUpdate、saveBatch、remove、update、list、page 等。
- **save vs insert**：save 返回 boolean，属于 Service 层；insert 返回 int，属于 Mapper 层。
- **主键回填**：insert/save 执行后，雪花 ID 会自动写入实体对象的 id 字段。

### 条件构造器

- **QueryWrapper**：通过字符串字段名构造条件，灵活但字段改名不感知。
- **LambdaQueryWrapper**：通过方法引用构造条件，类型安全，推荐生产使用。
- **eq / like / between / orderByAsc / orderByDesc**：常用条件与排序方法。
- **nested**：显式控制 and/or 优先级。

### 分页

- 必须配置 `PaginationInnerInterceptor`，否则 `Page.getTotal()` 为 0。
- `Page<T>` 既是入参也是返回值，包含 current、size、total、pages、records。
- 分页可与 Wrapper 组合使用：先 WHERE 再 LIMIT。

### 高级注解

- **@TableLogic**：逻辑删除字段；`removeById` 更新 `deleted=1`，查询自动过滤。
- **@Version**：乐观锁字段；更新时 `WHERE version=旧值 SET version=version+1`。
- **@TableField(fill=...)**：配合 `MetaObjectHandler` 自动填充 createTime / updateTime。

### 批量操作

- **saveBatch(list, batchSize)**：按批次插入，降低数据库往返。
- **updateBatchById**：按主键批量更新，null 字段默认不更新。

### IdType 与 FieldStrategy

- **IdType**：`AUTO` / `INPUT` / `ASSIGN_ID` / `ASSIGN_UUID` / `NONE`。
- **FieldStrategy**：`DEFAULT` / `NOT_NULL` / `NOT_EMPTY` / `IGNORED` / `NEVER`。

### 扩展实战

- **TypeHandler**：`@TableField(typeHandler = JacksonTypeHandler.class)` 把 Map/List/JSON 对象与数据库字符串互转；配合 `@TableName(autoResultMap = true)` 使用。
- **ActiveRecord**：实体 `extends Model<T>` 后可直接 `article.insert()` / `Article.selectById(id)`，无需注入 Service/Mapper。
- **动态表名**：`DynamicTableNameInnerInterceptor` + `TableNameHandler` 实现日志/订单按年月分表；需提前建好物理表。
- **InsertBatchSomeColumn**：自定义 `DefaultSqlInjector` 注入 `InsertBatchSomeColumn` 方法，生成 `INSERT ... VALUES (...),(...)`，性能远高于循环单条 insert。
- **链式 Wrapper**：`IService.lambdaQuery()` / `lambdaUpdate()` 一行链式完成查询/更新，无需手动 `new QueryWrapper`。
- **复杂 Wrapper**：`inSql` 子查询、`groupBy` + `having` 聚合、`apply` 自定义 SQL 片段。
- **selectMaps / selectObjs**：返回 `List<Map<String,Object>>` 或单列 `List<Object>`，适合报表、下拉框。
- **Wrapper 更新 / 删除**：`update(null, updateWrapper)` 按条件批量更新，`delete(wrapper)` 按条件删除。

### 综合实战

- 复杂关联统计可写自定义 `@Select` SQL。
- 单表聚合可用 `Wrapper.groupBy`。
- 生产常见列表接口：多条件 + 分页 + 排序。

## 数据模型

- **t_user**：id、username、age、email、gender（枚举）、status、deleted（逻辑删除）、version（乐观锁）、create_time、update_time。
- **t_order**：id、user_id、amount、status、create_time。
- **t_account**：id、username、password（select=false）、email（condition）、login_count（update 自增）、balance（numericScale）。
- **t_product**：id（@KeySequence H2 序列）、name、price。
- **t_task**：id、content、status、create_by、update_by、create_time、update_time。
- **t_article**：id、title、content、extra（JSON TypeHandler）、create_time。
- **t_report_202401 / t_report_202402**：id、report_month、content（动态表名分表）。

启动时 H2 内存数据库自动执行 `schema.sql` 建表、`data.sql` 灌入示例数据。

## 接口文档（Swagger UI）

项目集成了 SpringDoc OpenAPI（`springdoc-openapi-ui:1.7.0`，对应 Spring Boot 2.7），
接口文档根据代码中的 `@Tag` / `@Operation` 注解自动生成。

启动后端后访问：

- Swagger UI 可视化页面：http://localhost:8084/swagger-ui/index.html
- OpenAPI JSON 描述：http://localhost:8084/v3/api-docs

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

浏览器打开 http://localhost:5177 即可看到实验面板。

> 前端在开发时通过 Vite proxy 把 `/api` 转发到 `http://localhost:8084`。

## 运行测试

后端自带集成测试（JUnit 5 + MockMvc + AssertJ）：

```bash
mvn test
```

测试覆盖：

- `MybatisPlusApplicationTests`：Spring 上下文加载。
- `ScenarioApiTest`：通过 MockMvc 调用全部 46 个场景接口，验证均返回 200 且 data 非空。

## 推荐实验顺序

1. **实体类注解**：@TableName、@TableId、@TableField。
2. **BaseMapper CRUD**：insert、selectById、updateById、deleteById。
3. **IService CRUD**：save、saveOrUpdate、list、page。
4. **条件构造器**：QueryWrapper、LambdaQueryWrapper、嵌套条件。
5. **分页查询**：基础分页、分页 + 条件。
6. **高级注解**：逻辑删除、乐观锁、自动填充。
7. **批量操作**：saveBatch、updateBatchById。
8. **综合实战**：用户订单统计、状态分组统计、搜索分页。
9. **更多注解**：@KeySequence、@OrderBy、@EnumValue、@InterceptorIgnore、@TableField 高级属性、IdType、FieldStrategy、@Order、自定义注解 + AOP、@Accessors。
10. **扩展实战**：TypeHandler、ActiveRecord、动态表名、InsertBatchSomeColumn、链式 Wrapper、复杂 Wrapper、selectMaps、Wrapper 更新删除。

## 项目结构

```
04-mybatis-plus-practice/
├── pom.xml
├── README.md
├── src/main/resources/schema.sql
├── src/main/resources/data.sql
├── src/main/java/com/example/mp/
│   ├── MybatisPlusApplication.java
│   ├── annotation/    自定义注解 AutoFillUser
│   ├── aspect/        ValidationAspect、AutoFillUserAspect
│   ├── common/        ApiResponse
│   ├── config/        CorsConfig、OpenApiConfig、MybatisPlusConfig
│   ├── entity/        User、Order、Account、Product、Task、Article、Report、enums/UserGender、accessors/*
│   ├── handler/       MyMetaObjectHandler
│   ├── mapper/        UserMapper、OrderMapper、AccountMapper、ProductMapper、TaskMapper、ArticleMapper、ReportMapper
│   ├── order/         StartupOrderRecorder、FirstRunner/SecondRunner/ThirdRunner
│   ├── service/       UserService、UserServiceImpl、TaskService、TaskServiceImpl、ServiceCrudController/Service
│   ├── entity/        EntityAnnotationController/Service
│   ├── mapper/        MapperCrudController/Service
│   ├── wrapper/       WrapperQueryController/Service
│   ├── page/          PageController/Service
│   ├── advanced/      AdvancedAnnotationController/Service
│   ├── batch/         BatchOpsController/Service
│   ├── realworld/     RealWorldController/Service
│   ├── more/          MoreAnnotationController/Service
│   └── extension/     ExtensionController/Service、TableNameContext
├── src/test/java/com/example/mp/
└── web/               Vue 3 + Vite 前端
```
