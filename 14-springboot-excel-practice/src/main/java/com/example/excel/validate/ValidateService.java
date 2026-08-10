package com.example.excel.validate;

import com.alibaba.excel.EasyExcel;
import com.example.excel.common.ExcelBizException;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 06. 数据校验与错误反馈：逐行校验、合法/错误分行、错误回写。
 *
 * 导入质量是 Excel 系统的生命线：坏数据进库比不导入更可怕。
 * 本模块演示「校验 → 分行 → 错误清单回写」的完整闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateService {

    private final ExcelLogStore logStore;

    /**
     * 生成一份带问题的学生样本文件（4 条坏数据 + 4 条好数据）。
     */
    public byte[] sampleBytes() {
        List<StudentHead> rows = new ArrayList<>();
        rows.add(student("S0001", "张伟", 25, "13800000001", "计算机科学"));   // 好
        rows.add(student("S0002", "李娜", 22, "13900000002", "软件工程"));     // 好
        rows.add(student("S0003", "王强", 17, "13700000003", "人工智能"));     // 年龄 < 18
        rows.add(student("S0004", null, 28, "13600000004", "网络工程"));       // 姓名空
        rows.add(student("AB12", "赵敏", 23, "13500000005", "数据科学"));       // 学号格式错
        rows.add(student("S0006", "刘洋", 30, "12345", "信息安全"));            // 手机号错
        rows.add(student("S0007", "陈静", 26, "13300000007", "计算机科学"));   // 好
        rows.add(student("S0008", "杨磊", 24, "13200000008", "软件工程"));     // 好

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, StudentHead.class)
                .sheet("学生导入模板")
                .doWrite(rows);
        return out.toByteArray();
    }

    /**
     * 导入演示：生成样本 → 监听器校验 → 返回合法行/错误行。
     */
    public Map<String, Object> importDemo() {
        long start = System.currentTimeMillis();
        StudentReadListener listener = new StudentReadListener();
        try {
            EasyExcel.read(new ByteArrayInputStream(sampleBytes()))
                    .head(StudentHead.class)
                    .registerReadListener(listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }
        long costMs = System.currentTimeMillis() - start;
        logStore.add("import", "validate", "校验导入（8 行样本）", listener.getTotal(), costMs);
        return buildImportResult(listener, costMs);
    }

    /**
     * 真实上传导入：接收前端 multipart 文件。
     */
    public Map<String, Object> importFile(InputStream in, String filename) {
        if (in == null) {
            throw new ExcelBizException("上传文件不能为空");
        }
        if (filename != null && !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelBizException("仅支持 .xlsx 格式（EasyExcel 不支持 .xls）");
        }
        long start = System.currentTimeMillis();
        StudentReadListener listener = new StudentReadListener();
        try {
            EasyExcel.read(in)
                    .head(StudentHead.class)
                    .registerReadListener(listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            throw new ExcelBizException("解析上传的 Excel 失败：" + e.getMessage(), e);
        }
        long costMs = System.currentTimeMillis() - start;
        logStore.add("import", "validate", "真实上传导入：" + filename, listener.getTotal(), costMs);
        return buildImportResult(listener, costMs);
    }

    /**
     * 错误回写：把校验失败的行导出成「问题清单.xlsx」。
     */
    public byte[] errorReportBytes() {
        // 直接用样本里的错误行生成报告，方便演示
        StudentReadListener listener = new StudentReadListener();
        try {
            EasyExcel.read(new ByteArrayInputStream(sampleBytes()))
                    .head(StudentHead.class)
                    .registerReadListener(listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }
        List<ErrorRowHead> errors = new ArrayList<>();
        for (Map<String, Object> err : listener.getErrorRows()) {
            ErrorRowHead head = new ErrorRowHead();
            head.setRowNo(((Number) err.get("row")).intValue());
            head.setStudentNo((String) err.get("studentNo"));
            head.setName((String) err.get("name"));
            head.setError((String) err.get("error"));
            errors.add(head);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, ErrorRowHead.class)
                .sheet("问题清单")
                .doWrite(errors);
        return out.toByteArray();
    }

    /**
     * 导入校验速记（八股）。
     */
    public Map<String, Object> rules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principle", "坏数据进库比不导入更可怕：先校验、再入库、坏行回写，三步缺一不可。");
        result.put("layers", new String[]{
                "1. 前端/模板校验：下拉选项、必填、正则（体验好，但可被绕过，只能当第一道）",
                "2. 后端逐行校验：行号 + 原因精确到行（本模块演示，业务规则都在这里）",
                "3. 数据库约束：唯一索引 / 非空 / 外键兜底（最后防线）"
        });
        result.put("patterns", new LinkedHashMap<String, Object>() {{
            put("校验时机", "边读边校验（监听器内）or 全部读回后统一校验——行号溯源用前者更准");
            put("错误处理", "失败行不中断整体：收集错误清单回写 Excel，成功行正常入库");
            put("重复校验", "学号/身份证号做去重，先查库再校验，避免重复导入");
            put("事务边界", "千万别把整个导入包在一个大事务里，行级失败会回滚全部；按批提交");
        }});
        result.put("tip", "「错误行回写」是最受欢迎的功能：用户拿到问题清单照着改，而不是看一堆报错弹窗。");
        return result;
    }

    private Map<String, Object> buildImportResult(StudentReadListener listener, long costMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", listener.getTotal());
        result.put("validCount", listener.getValidRows().size());
        result.put("errorCount", listener.getErrorRows().size());
        result.put("validRows", listener.getValidRows());
        result.put("errorRows", listener.getErrorRows());
        result.put("costMs", costMs);
        result.put("errorReportDownload", "/api/validate/error-download");
        result.put("tip", "errorRows 里的 row 是用户在 Excel 里看到的行号（表头占第 1 行）；"
                + "合法行可入库，错误行可下载问题清单逐条修改。");
        return result;
    }

    private StudentHead student(String no, String name, int age, String phone, String major) {
        StudentHead head = new StudentHead();
        head.setStudentNo(no);
        head.setName(name);
        head.setAge(age);
        head.setPhone(phone);
        head.setMajor(major);
        return head;
    }
}
