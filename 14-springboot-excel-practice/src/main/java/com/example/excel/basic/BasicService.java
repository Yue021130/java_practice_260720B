package com.example.excel.basic;

import com.alibaba.excel.EasyExcel;
import com.example.excel.common.ExcelBizException;
import com.example.excel.support.DemoData;
import com.example.excel.support.ExcelLogStore;
import com.example.excel.support.UserRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 01. 快速开始：最简单的「列表 → Excel」「Excel → 列表」。
 *
 * 核心两行 API：
 *   导出：EasyExcel.write(输出流, head类).sheet(表名).doWrite(数据List)
 *   导入：EasyExcel.read(输入流).head(head类).sheet().doReadSync() → List
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicService {

    private final ExcelLogStore logStore;

    /**
     * 导出：把 8 条用户数据写成一个 xlsx 字节流。
     */
    public byte[] exportBytes() {
        List<UserHead> heads = new ArrayList<>();
        for (UserRow row : DemoData.users()) {
            heads.add(toHead(row));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 一行代码导出：指定 head 类 + 表名 + 数据
        EasyExcel.write(out, UserHead.class)
                .sheet("员工名单")
                .doWrite(heads);
        byte[] data = out.toByteArray();
        logStore.add("export", "basic", "快速开始：导出 8 条用户", heads.size(), heads.size() * 10L);
        return data;
    }

    /**
     * 导出演示（JSON）：返回列/行数/文件大小等信息，前端面板查看；
     * 真正的下载走 /api/basic/download。
     */
    public Map<String, Object> exportDemo() {
        long start = System.currentTimeMillis();
        byte[] data = exportBytes();
        long costMs = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", "员工名单.xlsx");
        result.put("sheetName", "员工名单");
        result.put("rows", DemoData.users().size());
        result.put("columns", 6);
        result.put("bytes", data.length);
        result.put("costMs", costMs);
        result.put("download", "/api/basic/download");
        result.put("tip", "一行 EasyExcel.write(...).sheet(...).doWrite(list) 完成导出；"
                + "列名来自 @ExcelProperty 注解，字段顺序即列顺序。");
        return result;
    }

    /**
     * 导入演示：先在内存里生成一个 xlsx，再解析回 List（模拟「用户上传 → 后端解析」）。
     */
    public Map<String, Object> importDemo() {
        byte[] data = exportBytes();
        long start = System.currentTimeMillis();
        List<UserHead> parsed;
        try {
            // 同步读取：整个文件读成一个 List，简单直观；大数据量用监听器（见 07 章）
            parsed = EasyExcel.read(new ByteArrayInputStream(data))
                    .head(UserHead.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }
        long costMs = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", parsed.size());
        result.put("rows", parsed);
        result.put("costMs", costMs);
        result.put("roundtrip", "导出的 8 条数据原样读回，字段值一致 → 证明注解双向映射成立");
        result.put("tip", "导入本质 = 读文件 + 按 @ExcelProperty 匹配列名 + 类型转换（String→Integer/Double/Date/Boolean）。");
        return result;
    }

    /**
     * 快速开始概念速记（八股）。
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("what", "EasyExcel 是阿里巴巴开源的 Excel 导入导出框架，基于 Apache POI 封装，"
                + "主打「注解驱动 + 低内存流式读写」");
        result.put("why", new String[]{
                "API 极简：注解声明列名/格式，一个 write/read 搞定，不用手写 Workbook/Cell 循环",
                "内存占用低：默认流式（SAX 模式）读，1 万行 ≈ POI 的 1/100 内存",
                "社区成熟：阿里内部与开源生态验证，中文文档完善"
        });
        result.put("coreApi", new LinkedHashMap<String, Object>() {{
            put("export", "EasyExcel.write(OutputStream, headClass).sheet(name).doWrite(List)");
            put("import", "EasyExcel.read(InputStream).head(headClass).sheet().doReadSync()");
            put("annotation", "@ExcelProperty(列名) / @ExcelIgnore / @ColumnWidth / @DateTimeFormat ...");
        }});
        result.put("tip", "EasyExcel 只支持 .xlsx（07+ 格式），不支持老的 .xls——面试常考这个边界。");
        return result;
    }

    private UserHead toHead(UserRow row) {
        UserHead head = new UserHead();
        head.setId(row.getId());
        head.setName(row.getName());
        head.setDepartment(row.getDepartment());
        head.setSalary(row.getSalary());
        head.setHireDate(row.getHireDate());
        head.setActive(row.getActive());
        return head;
    }
}
