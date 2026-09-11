# 第30章：Vue3 + SpringBoot 前后端分离实战（Jakarta Validation 参数校验 + Jackson 数据脱敏）

## 一、业务背景

客户信息是敏感数据最密集的领域：姓名、手机号、身份证号、邮箱、住址。这套「客户信息管理系统」把两道生产刚需串成全链路：

- **入口防脏**：所有写操作先过 JSR-303 校验——分组校验区分「新增必填/编辑可空」，自定义 `@Phone`、`@IdCard` 注解解决内置注解覆盖不了的规则；
- **出口防泄**：出参 VO 上的 `@Sensitive` 注解让 Jackson 序列化时自动打码，前端拿到的永远是脱敏数据，数据库中仍是明文。

登录认证沿用上一章方案：Sa-Token JWT 无状态 + MyBatis-Plus + MySQL 8.0 + Knife4j，前端 Vue3 + Element Plus + Pinia + Axios 拦截器。

## 二、技术栈

| 层级 | 技术 |
| --- | --- |
| 后端框架 | Spring Boot 2.7.18 + Java 8 + Maven |
| ORM | MyBatis-Plus 3.5.5 |
| 登录认证 | Sa-Token 1.39.0（sa-token-jwt，无状态模式） |
| 参数校验 | spring-boot-starter-validation（分组校验 + 自定义 ConstraintValidator） |
| 数据脱敏 | 自定义 @Sensitive 注解 + Jackson ContextualSerializer |
| API 文档 | Knife4j 4.3.0（OpenAPI3，/doc.html） |
| 数据库 | MySQL 8.0 |
| 前端 | Vue3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios |

## 三、项目结构

```
30-validation-masking-practice/
├── pom.xml
├── docs/
│   └── sql/
│       └── schema.sql                   # MySQL 8.0 建库建表 + 初始化数据
├── src/main/java/com/example/vmp/
│   ├── VmpApplication.java              # 启动类（@MapperScan）
│   ├── common/
│   │   ├── Result.java                  # 统一返回结果 {code, msg, data}
│   │   ├── BusinessException.java       # 业务异常
│   │   └── GlobalExceptionHandler.java  # 全局异常（含三种校验异常 + NotLoginException→401）
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域
│   │   ├── Knife4jConfig.java           # OpenAPI3 文档 Bean
│   │   ├── MyBatisPlusConfig.java       # 分页插件
│   │   ├── SaTokenConfig.java           # JWT 无状态 StpLogic
│   │   └── WebMvcConfig.java            # Sa-Token 拦截器（除 /api/auth/login 均需登录）
│   ├── validation/                      # 【主题一】参数校验
│   │   ├── groups/ValidationGroups.java # Create / Update 分组标记接口
│   │   ├── annotation/Phone.java        # @Phone 自定义注解
│   │   ├── annotation/IdCard.java       # @IdCard 自定义注解
│   │   └── validator/PhoneValidator.java / IdCardValidator.java
│   ├── masking/                         # 【主题二】数据脱敏
│   │   ├── SensitiveType.java           # PHONE / ID_CARD / EMAIL / NAME / ADDRESS
│   │   ├── Sensitive.java               # @Sensitive 注解（@JacksonAnnotationsInside + @JsonSerialize）
│   │   ├── SensitiveSerializer.java     # ContextualSerializer：按字段注解选择策略打码
│   │   └── MaskingUtil.java             # 五种打码策略
│   ├── controller/
│   │   ├── AuthController.java          # 登录/登出/当前用户
│   │   ├── CustomerController.java      # 客户 CRUD（Create/Update 分组校验）
│   │   └── ValidateDemoController.java  # 校验演示（@RequestParam 校验 → ConstraintViolationException）
│   ├── dto/
│   │   ├── LoginDTO.java / LoginVO.java
│   │   ├── CustomerSaveDTO.java         # 分组校验入参
│   │   └── CustomerVO.java              # @Sensitive 脱敏出参
│   ├── entity/User.java / Customer.java # @TableLogic 逻辑删除
│   ├── mapper/UserMapper.java / CustomerMapper.java
│   ├── service/AuthService.java / CustomerService.java
│   └── util/PasswordUtil.java           # BCrypt 密码哈希
├── src/main/resources/application.yml
└── web/                                 # 前端
    ├── index.html / package.json / vite.config.js   # 端口 5173，/api 代理到 8080
    └── src/
        ├── main.js / App.vue
        ├── router/index.js              # 路由 + 登录守卫
        ├── stores/user.js               # Pinia：token / userInfo（localStorage 持久化）
        ├── api/request.js               # axios：请求注入 Token、响应解包 Result、401 跳登录
        ├── api/auth.js / customer.js
        ├── layout/Index.vue
        └── views/Login.vue / Customer.vue（脱敏列表 + 新增/编辑，规则与后端呼应）
```

## 四、接口清单

> API 文档（可在线调试）：**http://localhost:8080/doc.html**（Knife4j）。
> 除登录外均需携带 `Authorization: <token>` 请求头。

| 方法 | 接口 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 登录，返回 JWT token | 公开 |
| POST | /api/auth/logout | 登出 | 登录 |
| GET | /api/auth/info | 当前登录用户信息 | 登录 |
| GET | /api/customer/page | 客户分页列表（出参已脱敏） | 登录 |
| GET | /api/customer/{id} | 客户详情（出参已脱敏） | 登录 |
| POST | /api/customer | 新增客户（Create 分组校验） | 登录 |
| PUT | /api/customer | 编辑客户（Update 分组；敏感字段留空不改） | 登录 |
| DELETE | /api/customer/{id} | 逻辑删除 | 登录 |
| GET | /api/validate/phone?phone= | 方法参数 @Phone 校验演示 | 登录 |
| GET | /api/validate/keyword?keyword= | @NotBlank+@Size 叠加演示 | 登录 |

## 五、运行方式

前置：本机已安装 MySQL 8.0，且 8080/5173 端口未被占用。
**先修改 `src/main/resources/application.yml` 中的数据库用户名密码为你本机 MySQL 的实际凭据。**

```bash
# 1. 建库建表
mysql -u root -p < docs/sql/schema.sql

# 2. 启动后端
mvn spring-boot:run

# 3. 启动前端（另开终端）
cd web && npm install && npm run dev
```

- 前端：http://localhost:5173 ，默认账号 **admin / 123456**
- 接口文档：http://localhost:8080/doc.html

## 六、核心场景说明

### 6.1 分组校验流程

1. `ValidationGroups.Create/Update` 是两个空标记接口；
2. `CustomerSaveDTO` 中 `id` 只挂 Update 分组（编辑必填），`name/phone/idCard` 挂 Create 分组（新增必填）；
3. Controller 用 `@Validated(Create.class)` / `@Validated(Update.class)` 指定本次生效的规则集；不带 groups 的规则（level 范围、email 格式）属默认分组，两种场景都生效；
4. 校验失败抛 `MethodArgumentNotValidException`，全局异常处理取第一条字段错误返回 `code=400`，前端弹出中文提示。

### 6.2 自定义注解三要素

以 `@IdCard` 为例：① `@Constraint(validatedBy = IdCardValidator.class)` 指定校验器；② `@Target/@Retention` 元注解；③ `message/groups/payload` 三个规范属性。`IdCardValidator` 实现 `ConstraintValidator<IdCard, String>`，`isValid()` 中做 18 位加权因子校验（能识别任意一位写错），null/空串放行交给 `@NotBlank`。

### 6.3 Jackson 序列化打码原理

1. `@Sensitive` 组合了 `@JacksonAnnotationsInside` + `@JsonSerialize(using = SensitiveSerializer.class)`；
2. `SensitiveSerializer` 实现 `ContextualSerializer`：Jackson 为每个带注解的字段调用一次 `createContextual()`，从 `BeanProperty` 读到 `@Sensitive(SensitiveType.XXX)`；
3. `serialize()` 时按类型调用 `MaskingUtil` 打码后输出——`13800138000` → `138****8000`，`110101199003077758` → `1101**********7758`；
4. 脱敏只挂在 `CustomerVO` 上：Entity（MyBatis 读写）与日志不受影响，避免「查出来打码再入库」的灾难。

## 七、八股速记

**7.1 @Valid 与 @Validated**
`@Valid` 是 JSR-303 标准注解，支持**嵌套级联**（字段上再加 @Valid 才会校验内部对象）；`@Validated` 是 Spring 扩展，支持**分组**。常用组合：入口 @Validated(分组) + 嵌套处 @Valid。

**7.2 三种校验异常别漏接**
`@RequestBody` 失败抛 `MethodArgumentNotValidException`；表单/查询对象绑定失败抛 `BindException`；`@RequestParam`/`@PathVariable`/普通方法参数失败抛 `ConstraintViolationException`。全局异常处理三者都要捕获。

**7.3 校验器的 null 语义**
ConstraintValidator 对 null 应返回 true（「格式合法」与「是否必填」是两个职责），必填交给 @NotBlank/@NotNull——否则两个注解叠加会产生冲突信息。

**7.4 脱敏方案的边界**
注解脱敏只保护 HTTP 出参；生产完整方案 = 出参脱敏（本方案）+ 数据库敏感列加密 + 日志脱敏（logback  pattern 或自定义 Converter）+ 查询接口按权限返回明文（如本人查看本人完整手机号）。

## 八、测试验证

```bash
# 后端单元测试（不依赖数据库，含身份证/手机号校验器与五种脱敏策略断言）
mvn test

# 前端构建验证
cd web && npm install && npm run build
```

手动验证（后端启动后）：

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 出参脱敏：返回中手机号应为 138****8000
curl -s http://localhost:8080/api/customer/page -H "Authorization: $TOKEN"

# 校验演示：非法手机号返回 code=400
curl -s "http://localhost:8080/api/validate/phone?phone=123" -H "Authorization: $TOKEN"
```

## 九、作者

Yue021130 <169446203@qq.com>
