# 22-easyexcel-stream-practice：EasyExcel 流式导出实战

基于微信公众号原文《100万行导出内存从2G降到50M，EasyExcel这样用才叫"流式"》的 Spring Boot + Vue3 实战复现。

---

## 一、项目定位

导出接口是后台系统最常见的功能之一，但同样是 100 万行导出：

- 有人接口内存飙到 2G，直接 OOM；
- 有人全程内存不到 50M，导出完 GC 都懒得跑。

差别不在 EasyExcel 本身，而在是否真正理解 **"流式写入"** 四个字。

本章节提供一个 **可运行、可对比、可观测** 的完整示例：

- 错误示范：全量加载导出（小数据量可用，大数据量 OOM）。
- 正确示范：分页查询 + `ExcelWriter` 分批写入。
- 异步导出：大文件后台生成，前端轮询进度后下载。
- 八股速记：把生产环境踩过的坑一次性讲清楚。

---

## 二、技术栈

| 层 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 后端 | Spring Boot | 2.7.18 | Java 8 兼容 |
| 后端 | Java | 1.8 | |
| 后端 | Spring Data JPA | 由父工程管理 | 数据访问 |
| 后端 | H2 | 由父工程管理 | 内存数据库，开箱即用 |
| 后端 | EasyExcel | 3.3.4 | 阿里 Excel 工具 |
| 后端 | Lombok | 由父工程管理 | 减少样板代码 |
| 后端 | springdoc-openapi-ui | 1.7.0 | Swagger 文档 |
| 前端 | Vue | 3.4.21 | Composition API |
| 前端 | Vite | 5.2.8 | 构建工具 |
| 前端 | axios | 1.6.8 | HTTP 请求 |

---

## 三、快速启动

### 3.1 启动后端

```bash
cd 22-easyexcel-stream-practice
mvn spring-boot:run
```

后端默认端口 `8102`。

接口文档：http://localhost:8102/swagger-ui.html

### 3.2 启动前端

```bash
cd 22-easyexcel-stream-practice/web
npm install
npm run dev
```

前端默认端口 `5195`，已代理到后端 8102。

---

## 四、核心八股

### 4.1 为什么用了 EasyExcel 还会 OOM？

EasyExcel 只是把 Excel 写入过程做了流式封装。如果你一次性把 100 万行数据全部查出来装进 `List`，那么：

```
内存 ≈ 100万行 × 每行对象大小（含字符串、BigDecimal 等）
```

几个 G 很正常，GC 回收不掉就直接 OOM。

**真正要做的是：查询也要流式化。**

### 4.2 流式导出正确姿势

```java
ExcelWriter writer = EasyExcel.write(response.getOutputStream(), OrderExportVO.class).build();
WriteSheet sheet = EasyExcel.writerSheet("订单").build();

while (true) {
    Page<Order> page = orderRepository.findAllByOrderByIdAsc(PageRequest.of(pageNo, 5000));
    if (!page.hasContent()) break;
    writer.write(page.getContent().stream().map(OrderExportVO::from).collect(Collectors.toList()), sheet);
    pageNo++;
}
writer.finish();
```

核心要点：

1. **ExcelWriter 只创建一次**，不要每批都 new。
2. **每批查询后立刻写入**，写入后释放当前批次引用。
3. **最后必须 `finish()`**，否则最后一批数据可能丢失。
4. **数据库分页查询**，不要 `select *`。

### 4.3 五个真金白银的坑

| 坑 | 后果 | 解决方案 |
|---|---|---|
| 一次性加载全表 | OOM | LIMIT/OFFSET 分页、MyBatis Cursor、流式查询 |
| 每行设置单元格样式 | 样式对象爆炸，内存 leak | 使用注解统一设置，或使用默认样式 |
| 自动列宽 | 缓存整列数据算宽度，占内存 | 固定 `@ColumnWidth` |
| 单 Sheet 超过 1048576 行 | 文件损坏或抛异常 | 超过后拆分 Sheet |
| 同步导出大文件 | HTTP 超时、线程阻塞 | 异步任务 + 轮询 + 下载 |

### 4.4 高频面试题

**Q1：EasyExcel 为什么比 POI 省内存？**

EasyExcel 底层基于 POI 的 SXSSF（Streaming Usermodel API），只保留滑动窗口内的行在内存，窗口外的行刷入磁盘临时文件，因此内存占用与数据总量无关，只与窗口大小有关。

**Q2：SXSSF 的窗口大小能调吗？**

可以。`SXSSFWorkbook` 构造时可以传入 `rowAccessWindowSize`，默认 100。EasyExcel 也提供相应参数，但一般不需要改。

**Q3：分页查询深分页慢怎么办？**

- 使用游标/流式查询（MyBatis Cursor、JDBC `setFetchSize`）。
- 使用上次查询的最大 ID 作为条件（`WHERE id > ? LIMIT 5000`）。
- 避免深分页 OFFSET 过大。

**Q4：异步导出任务状态怎么存？**

演示项目用内存 Map；生产环境请用 Redis + 任务表，支持多实例共享、断点续传、失败重试。

**Q5：导出时服务器 CPU 高怎么办？**

- 限制并发导出任务数（线程池/队列）。
- 大数据量导出放到夜间或离线集群。
- 复杂计算在 SQL 层或缓存层完成，不要在导出时实时计算。

---

## 五、接口清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/excel/generate?count=` | 生成模拟订单数据 |
| GET | `/api/excel/export/in-memory` | 错误示范：全量加载导出 |
| GET | `/api/excel/export/stream` | 正确示范：流式导出 |
| POST | `/api/excel/export/async?totalRows=` | 提交异步导出任务 |
| GET | `/api/excel/export/async/{taskId}/status` | 查询异步任务状态 |
| GET | `/api/excel/export/async/{taskId}/download` | 下载异步导出文件 |
| GET | `/api/excel/explain` | 八股速记 |

---

## 六、目录结构

```
22-easyexcel-stream-practice/
├── src/main/java/com/example/ee/
│   ├── EasyExcelStreamPracticeApplication.java
│   ├── common/                          # 统一响应、异常、CORS、OpenAPI
│   ├── config/
│   │   └── ExportProperties.java
│   ├── entity/
│   │   └── Order.java
│   ├── repository/
│   │   └── OrderRepository.java
│   ├── excel/
│   │   ├── OrderExportVO.java
│   │   └── ExportTaskStatus.java
│   ├── service/
│   │   ├── DataGenerator.java           # 模拟数据生成
│   │   ├── ExportTaskExecutor.java      # 异步导出执行器
│   │   └── ExcelExportService.java      # 核心导出服务
│   └── web/
│       └── ExcelExportController.java
├── src/test/java/com/example/ee/
│   ├── service/ExcelExportServiceTest.java
│   └── web/ExcelExportApiTest.java
├── src/main/resources/application.yml
├── web/                                 # Vue3 前端
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue
│   │   └── api/excel.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── pom.xml
└── README.md
```

---

## 七、与公众号原文对照

原文本地备份：`../22/100万行导出内存从2G降到50M，EasyExcel这样用才叫流式.html`

| 原文主题 | 本项目对应实现 |
|---|---|
| 流式写入 vs 全量加载 | `/api/excel/export/stream` 与 `/api/excel/export/in-memory` 对比 |
| 数据查询与写入配合 | `OrderRepository` 分页 + `ExcelWriter` 分批 write |
| 生产级完整方案 | 异步导出 + 任务状态 + 文件下载 |
| 5 个坑 | `README` 与 `/api/excel/explain` 八股速记 |

---

## 八、测试

```bash
# 后端测试
cd 22-easyexcel-stream-practice
mvn test

# 前端构建
cd 22-easyexcel-stream-practice/web
npm install
npm run build
```

---

## 九、作者

由 Kimi Code CLI 协助生成，作者：我。
