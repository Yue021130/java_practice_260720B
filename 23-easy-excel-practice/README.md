# 23. Excel 导入导出实战（Easypoi + EasyExcel）

## 项目定位

Spring Boot 2.7 + Easypoi 4.4 + EasyExcel 3.3 混用实战，覆盖：

- Easypoi 基础导入、带校验导入、Map 导入、组内重复校验
- EasyExcel 流式导出 + 自定义 `Converter`
- 统一返回、全局异常、Swagger 文档、Vue3 前端闭环

> 真实业务场景：后台管理系统批量导入用户，导入失败回写错误日志 Excel；商品列表低内存导出。

---

## 技术栈

| 技术 | 版本 | 作用 |
|------|------|------|
| Spring Boot | 2.7.18 | Web 容器 + JPA |
| Easypoi | 4.4.0 | 复杂导入校验、错误日志回写 |
| EasyExcel | 3.3.4 | 大文件流式导出 |
| POI | 5.2.3 | 统一版本，避免依赖冲突 |
| H2 | 2.x | 内存数据库，开箱即用 |
| Vue3 + Vite | 3.4 + 5.2 | 前端演示页面 |

---

## 快速启动

### 后端

```bash
cd 23-easy-excel-practice
mvn spring-boot:run
```

服务端口：`8103`

### 前端

```bash
cd 23-easy-excel-practice/web
npm install
npm run dev
```

前端端口：`5196`，已配置代理到 `http://localhost:8103`。

### Swagger

访问：http://localhost:8103/swagger-ui.html

---

## 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/excel/easypoi/template` | 下载导入模板 |
| POST | `/api/excel/easypoi/import/basic` | 基础导入 |
| POST | `/api/excel/easypoi/import/verify` | 带校验导入 |
| POST | `/api/excel/easypoi/import/map` | Map 方式导入 |
| POST | `/api/excel/easypoi/import/duplicate` | 组内重复校验导入 |
| POST | `/api/excel/easyexcel/init` | 初始化商品数据 |
| GET | `/api/excel/easyexcel/export` | 导出商品 Excel |
| GET | `/api/excel/explain` | 八股速记 |

---

## 核心代码解读

### 1. Easypoi 导入实体 `SysUserImport`

```java
@Data
public class SysUserImport implements IExcelModel, IExcelDataModel, Serializable {
    private Integer rowNum;      // 行号，IExcelDataModel 自动填充
    private String errorMsg;     // 错误信息，IExcelModel 回写

    @Excel(name = "姓名(必填)", width = 20)
    @NotBlank(message = "姓名不能为空")
    private String realname;
    // ...
}
```

- `IExcelModel`：承载校验错误信息。
- `IExcelDataModel`：Easypoi 自动填充物理行号。

### 2. 业务校验处理器 `SysUserVerifyHandler`

```java
@Component
public class SysUserVerifyHandler implements IExcelVerifyHandler<SysUserImport> {
    @Override
    public ExcelVerifyHandlerResult verifyHandler(SysUserImport user) {
        // 必填、正则、数据库唯一性校验
        // 返回 new ExcelVerifyHandlerResult(false, "错误原因")
    }
}
```

### 3. 组内重复校验 `GroupDuplicateVerifyHandler`

```java
public class GroupDuplicateVerifyHandler implements IExcelVerifyHandler<SysUserImport> {
    private final ThreadLocal<List<SysUserImport>> threadLocal = new ThreadLocal<>();
    // 同一批次内出现重复 realname 即失败
}
```

### 4. EasyExcel 自定义 Converter `WhetherConverter`

```java
public class WhetherConverter implements Converter<Integer> {
    // 1 -> 是，0 -> 否；导入时反向转换
}
```

用于字段 `isDefault` 在数据库（1/0）与 Excel（是/否）之间转换。

### 5. 错误日志导出注意点

Easypoi 的 `ExcelExportUtil.exportExcel` 在导出失败列表时会**清空原始 List**，因此调用前需传入副本：

```java
errorExcelBytes = exportErrorLog(new ArrayList<>(failList));
```

---

## 八股速记

1. **Easypoi vs EasyExcel 怎么选？**
   - Easypoi：复杂导入校验、错误日志回写、无注解 Map 导入、多 Sheet/图片。
   - EasyExcel：大文件、低内存、流式读写、自定义 Converter。

2. **POI 版本冲突怎么解决？**
   - 在 `dependencyManagement` 中统一 `poi`、`poi-ooxml`、`poi-ooxml-lite` 版本。

3. **`IExcelVerifyHandler` 返回值含义？**
   - `ExcelVerifyHandlerResult(true)` 通过；`false` 进入 `failList`，错误信息写入 `errorMsg`。

4. **ThreadLocal 在导入校验中注意什么？**
   - 用完清理，避免线程池复用导致脏数据。

5. **EasyExcel Converter 作用域？**
   - 字段级；可实现 `Converter` 接口处理枚举、布尔、字典等转换。

---

## 测试

```bash
mvn test
```

覆盖：

- `EasypoiImportServiceTest`：基础导入、校验导入、重复校验
- `EasyExcelExportServiceTest`：初始化数据、导出二进制
- `ExcelApiTest`：Controller 层接口测试

---

## 作者

我
