# 14-springboot-excel-practice：Spring Boot + EasyExcel 导入导出实践

本模块把 Spring Boot 生态下最常用的 Excel 能力（阿里巴巴 **EasyExcel**，基于 POI 封装）转化为可运行、可交互的 Spring Boot + Vue 3 项目代码，覆盖**注解映射、样式、复杂表头、大数据量导出、数据校验与错误回写、监听器增量读取、模板填充、Web 下载与上传、常见坑与调优**等完整能力，每个实验都在真实 JVM 上跑给你看，便于系统学习。

**开箱即用**：不需要数据库、不需要消息队列，导入演示都在内存里「生成样本文件 → 解析」，直接 `mvn spring-boot:run` 就能玩；导出演示会返回真正的 `.xlsx` 文件让你下载打开看效果。

**为什么用 EasyExcel 而不是直接写 POI**：API 极简（注解驱动、读写各两行）、默认流式读写内存占用低（读 1 万行 ≈ POI 的 1/100）、阿里内部验证过的社区方案——这也是面试里「Excel 导入导出怎么做」的标准答案。

> ⚠️ EasyExcel 只支持 `.xlsx`（07+ 格式），**不支持 `.xls`**——这是它最常被考到的一个边界。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- Excel：`com.alibaba:easyexcel` **3.3.4**（底层 POI 5.x；3.x 用 JDK 自身反射替换了 cglib，Java 8~17 开箱即用、无需额外 JVM 参数）
- 测试：JUnit 5 + MockMvc + AssertJ（含真实 multipart 上传导入的集成测试）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS（面板支持「运行实验 / 下载文件 / 上传文件」三种卡片）
- 端口：后端 **8094**，前端 **5187**

## 核心：EasyExcel 是什么

`EasyExcel` 是阿里巴巴开源、基于 Apache POI 封装的 Excel 导入导出框架，核心卖点就一句话：**注解驱动 + 低内存流式读写**。

| 能力 | 一行代码 | 说明 |
| --- | --- | --- |
| 导出 | `EasyExcel.write(out, Head.class).sheet("表").doWrite(list)` | head 类用注解声明列名/格式 |
| 导入 | `EasyExcel.read(in).head(Head.class).sheet().doReadSync()` | 读回 `List<T>`；大文件改用监听器 |
| 样式 | `registerWriteHandler(HorizontalCellStyleStrategy)` | 表头/内容样式，或自定义 Handler |
| 复杂表头 | `@ExcelProperty({"一级","二级"})` | 多级表头自动合并 |
| 模板填充 | `EasyExcel.write(out).withTemplate(...).sheet().doFill(数据)` | 样式交给模板，后端只填数据 |
| 大数据 | `ExcelWriter` 分页边查边写 | 内存近似恒定，几十万行不 OOM |

**head 类 = 一个类同时定义「Excel 长什么样」与「Java 对象长什么样」**，导入导出共用一个类，这是 EasyExcel 优雅的核心。

## 模块结构

```
14-springboot-excel-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/excel/
│   ├── ExcelPracticeApplication.java
│   ├── common/         # 统一响应 ApiResponse、全局异常处理（含上传超限）
│   ├── config/         # ExcelPracticeProperties、OpenAPI、CORS
│   ├── support/        # 演示数据工厂、下载响应工具（RFC 5987 文件名）、操作日志存储
│   ├── basic/          # 01 快速开始
│   ├── annotation/     # 02 注解与字段
│   ├── style/          # 03 样式与格式（内置策略 + 自定义 Handler）
│   ├── mergehead/      # 04 复杂表头
│   ├── bigdata/        # 05 大数据量导出（分页边查边写 + 内存对比）
│   ├── validate/       # 06 数据校验与错误反馈（问题清单回写）
│   ├── listener/       # 07 监听器与增量读取（按批落库）
│   ├── template/       # 08 模板导出（简单填充 + 列表填充）
│   ├── web/            # 09 Web 下载与导入实战（响应头/上传校验）
│   └── pitfall/        # 10 常见坑与调优
├── src/main/resources/application.yml
├── src/test/java/com/example/excel/   # 上下文测试 + 全场景接口测试（含真实上传）
└── web/                # Vue 3 前端面板（5187）
```

## 快速启动

### 后端

```bash
cd 14-springboot-excel-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8094/swagger-ui/index.html

### 前端

```bash
cd 14-springboot-excel-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5187

### 运行测试

```bash
cd 14-springboot-excel-practice
mvn test
```

测试含 `realMultipartImportWorks`：用 MockMultipartFile 走真实上传链路（生成样本 → multipart 上传 → 校验解析），不依赖任何外部服务。

## 接口速查

### 01. 快速开始 `/api/basic`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/basic/download` | GET | 下载员工名单.xlsx（真实导出二进制） |
| `/api/basic/export-demo` | GET | 导出演示（JSON）：列数/行数/文件大小 |
| `/api/basic/import-demo` | POST | 导入演示：内存生成样本再解析回 List |
| `/api/basic/overview` | GET | EasyExcel 是什么 / 为什么用它 / 核心 API 两行 |

### 02. 注解与字段 `/api/annotation`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/annotation/download` | GET | 下载注解演示.xlsx（列重排 + 千分位 + 日期格式） |
| `/api/annotation/export-demo` | GET | 输出列顺序 vs 声明顺序 / 忽略字段验证 |
| `/api/annotation/import-demo` | POST | 读回文件，验证格式注解反向解析、@ExcelIgnore 不赋值 |
| `/api/annotation/explain` | GET | 常用注解总表（八股） |

### 03. 样式与格式 `/api/style`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/style/download` | GET | 下载部门薪资报表.xlsx（浅蓝表头 + 高亮 + 合并） |
| `/api/style/export-demo` | GET | 说明应用了哪些样式 |
| `/api/style/explain` | GET | 三层样式机制：注解 / 内置策略 / 自定义 Handler |

### 04. 复杂表头 `/api/mergehead`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/mergehead/download` | GET | 下载年度销售业绩表.xlsx（2 级分组表头 + 区域合并） |
| `/api/mergehead/export-demo` | GET | 表头层级与合并说明 |
| `/api/mergehead/explain` | GET | 多级 value / 三种合并手段速记 |

### 05. 大数据量导出 `/api/bigdata`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/bigdata/download?rows=50000` | GET | 分页边查边写导出 N 行 |
| `/api/bigdata/export-demo?rows=50000` | POST | 行数/页大小/耗时/文件大小 |
| `/api/bigdata/compare?rows=50000` | POST | 全量 List vs 分页写的堆占用对比 |
| `/api/bigdata/strategy` | GET | 边查边写 / POI 内存对比 / 优化要点 |

### 06. 数据校验与错误反馈 `/api/validate`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/validate/import-demo` | POST | 8 行样本（4 好 4 坏）校验，返回行号与原因 |
| `/api/validate/sample-download` | GET | 下载含坏数据的样本文件（可手动上传试） |
| `/api/validate/import` | POST | 真实 multipart 上传导入 |
| `/api/validate/error-download` | GET | 下载问题清单.xlsx（错误回写） |
| `/api/validate/rules` | GET | 三层校验 / 校验时机 / 事务边界（八股） |

### 07. 监听器与增量读取 `/api/listener`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/listener/import-demo?rows=250` | POST | 流式逐行回调，每批 100 行落库，返回批次统计 |
| `/api/listener/explain` | GET | 为何用监听器 / 回调方法 / 批量与断点续传 |

### 08. 模板导出 `/api/template`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/template/template-download` | GET | 下载空白模板.xlsx（含 {占位符}） |
| `/api/template/fill-download` | GET | 下载填充后的订单.xlsx |
| `/api/template/fill-demo` | POST | 模板填充演示（JSON） |
| `/api/template/explain` | GET | withTemplate / FillWrapper / forceNewRow（八股） |

### 09. Web 下载与导入实战 `/api/web`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/web/download` | GET | 带规范响应头下载（RFC 5987 中文文件名） |
| `/api/web/import` | POST | 真实上传导入（校验大小/类型/空文件） |
| `/api/web/download-rule` | GET | 响应头 / 中文文件名 / 权限 / 防盗链（八股） |
| `/api/web/upload-limit` | GET | multipart 配置 / 超大文件方案 |

### 10. 常见坑与调优 `/api/pitfall`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/pitfall/list` | GET | 10 个高频坑：现象 → 原因 → 解法 |
| `/api/pitfall/poi-vs-easyexcel` | GET | EasyExcel vs POI 对比表 |
| `/api/pitfall/head-mismatch-demo` | GET | 表头不匹配现场：列名差一字 id 静默丢失 |
| `/api/pitfall/tuning` | GET | 读 / 写 / 线程池 / 限流 / 超大文件方案 |

## 面试八股

### EasyExcel 和 POI 的关系？

EasyExcel 基于 POI 二次封装。POI 是底层库（直接操作 Workbook/Cell），XSSF 会把整本 Excel 常驻内存、几十万行必 OOM，代码量大；EasyExcel 用注解驱动、默认 SAX 流式读 + 写临时文件，内存占用低一个量级。**结论：业务导入导出直接用 EasyExcel，需要底层操控才用 POI。**

### EasyExcel 支持哪些格式？

**只支持 `.xlsx`（OOXML，07+）**，不支持 `.xls`。底层是 poi-ooxml。这也是上传接口要校验扩展名的原因。

### 导出/导入的核心 API？

```java
// 导出：一行搞定
EasyExcel.write(outputStream, UserHead.class).sheet("员工").doWrite(userList);
// 导入：同步读回 List
List<UserHead> list = EasyExcel.read(inputStream).head(UserHead.class).sheet().doReadSync();
```

### @ExcelProperty 有哪些参数？

- `value`：表头名（`String[]` 可实现多级表头，数组长度=表头级数）；
- `index`：强制列下标（0 起），模板固定时用；
- `order`：排序（**优先级高于字段声明顺序**）；
- `converter`：自定义类型转换器（枚举/字典转中文等）。

### 为什么导入时某些字段读出来是 null？

最常见原因是**表头名对不上**：导入按表头名匹配字段，模板改列名后字段静默丢失（本模块 `/api/pitfall/head-mismatch-demo` 现场复现）。解法：模板受控 + 监听器 `invokeHeadMap` 校验表头，或用 `index` 锁死列。

### 样式怎么改？有哪几个层级？

1. 注解：`@HeadStyle` / `@ContentStyle` / `@ColumnWidth` / `@HeadRowHeight` / `@ContentRowHeight`；
2. 内置策略：`HorizontalCellStyleStrategy`（表头+内容）、`LoopMergeStrategy`（纵向合并相同值）、`OnceAbsoluteMergeStrategy`（绝对区域合并）；
3. 自定义 `CellWriteHandler`：`afterCellDispose(context)` 里按行/列/值任意改样式（本项目月薪>15000 标红就是它）。

生产上「模板 + 预设样式」比代码逐像素调样式更省事：让 UI 出模板，后端只填数据。

### 大数据量导出怎么避免 OOM？

分页边查边写：`ExcelWriter` + `WriteSheet` 手动管理，每查一页（5000~20000 行）`write` 一页、清空，最后 `finish()`。内存里始终只有一页数据。对比：几十万行 `new` 进一个 List 再 `doWrite` 必 OOM。超大文件（几十 MB+）建议落磁盘/对象存储 + 异步下载。

### 导入大数据量为什么要用监听器？

`doReadSync()` 会把整个文件读成一个 List，几十万行直接撑爆内存。`AnalysisEventListener` 是 SAX 式流式逐行回调：`invoke()` 每行回调、攒满 batchSize 批量落库一次、`doAfterAllAnalysed()` 收尾补最后一批，内存恒定。

### 导入的数据怎么校验？

三层：前端/模板（体验）→ 后端逐行校验（业务规则，行号溯源到具体行，本项目 `/api/validate`）→ 数据库约束（唯一索引兜底）。坏行不中断整体：**错误行回写问题清单**，成功行正常入库。**千万别把整个导入包在一个大事务里**，按批提交。

### 模板填充怎么做？

`EasyExcel.write(out).withTemplate(模板流).build()`，简单填充 `{customer}` 用 `writer.fill(map, sheet)`，列表填充 `{item.name}` 用 `writer.fill(new FillWrapper("item", list), fillConfig, sheet)`（`forceNewRow(true)` 保证每个元素新起一行），最后 `finish()`。样式/公式/合并单元格在模板里预置，填充后保留。

### 下载 Excel 中文文件名乱码怎么办？

Content-Disposition 同时写两套：`filename="<urlencoded>"`（老浏览器）与 `filename*=UTF-8''<urlencoded>`（RFC 5987 标准），文件名先 URLEncoder（空格转 `%20`）。Content-Type 用 xlsx 官方 MIME：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`。本项目 `ExcelWebSupport` 就是标准实现。

### 上传 Excel 要注意什么？

- `spring.servlet.multipart.max-file-size` 默认只有 1MB，要调大（本项目 50MB）；
- 后端校验：非空 → 扩展名 `.xlsx` → 大小；
- 超大文件：先落对象存储，再做异步导入，返回任务 ID 让前端轮询；
- 下载/导出接口必须鉴权 + 限流，超大导出转异步任务 + 带有效期的下载链接。

### EasyExcel 3.x 和 2.x 有什么区别？

2.x 在 Java 9+ 需要 `--add-opens` JVM 参数（底层 cglib 在强封装下初始化失败），3.x 用 JDK 自身反射替换了 cglib，**Java 8~17 开箱即用**；同时 `CellWriteHandler` 的 `afterCellDispose` 从老式多参签名改成 `CellWriteHandlerContext`、`CellData` 改包为 `WriteCellData`。本项目用的 3.3.4。

## 推荐实验顺序

1. 启动后端与前端，先点 **01 快速开始**：导出演示 → 下载文件 → 导入演示，建立「注解驱动」的整体认知。
2. **02 注解与字段**：下载文件对比输出列顺序与声明顺序，看 remark 被忽略、月薪千分位、日期格式化。
3. **03 样式与格式**：下载部门薪资报表，看浅蓝表头、全边框、月薪>15000 标红、部门自动合并。
4. **04 复杂表头**：下载年度销售业绩表，看「一季度/二季度」分组表头与区域合并。
5. **05 大数据量导出**：跑「内存对比」（重点！），体会全量 List vs 分页边查边写的差距；再下载 50000 行试试。
6. **06 数据校验**：导入演示看 4 好 4 坏的行号与原因 → 下载问题清单 → 下载样本文件后「真实上传导入」走完整链路。
7. **07 监听器**：导入 250 行看 100/100/50 三批落库，理解流式读为什么省内存。
8. **08 模板导出**：下载空白模板 → 下载填充后的订单，对比占位符与真实数据。
9. **09 Web 实战**：看响应头规范与上传限制说明；用 06 的样本做一次真实上传。
10. **10 常见坑**：通读清单；务必亲手跑一次「表头不匹配演示」，理解「静默丢失比报错更危险」。

## 参考

- [EasyExcel 官方文档](https://easyexcel.opensource.alibaba.com/)
- [EasyExcel GitHub](https://github.com/alibaba/easyexcel)
- [Apache POI 官方文档](https://poi.apache.org/components/spreadsheet/index.html)
- [RFC 5987：Content-Disposition filename*（中文文件名规范）](https://datatracker.ietf.org/doc/html/rfc5987)
- [Spring Boot 文件上传（Multipart）文档](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/features.html#features.developing-web-applications.spring-mvc.multipart)
