package com.example.excel.bigdata;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.excel.basic.UserHead;
import com.example.excel.config.ExcelPracticeProperties;
import com.example.excel.support.DemoData;
import com.example.excel.support.ExcelLogStore;
import com.example.excel.support.UserRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 05. 大数据量导出：分页查询 + 边查边写，内存恒定。
 *
 * 核心思路（也是面试必答）：
 *   ExcelWriter + WriteSheet 手动管理，每查一页（pageSize 行）就 write 一页，
 *   写完 clear 掉，内存里始终只有一页数据；最后必须 finish() 收尾。
 *
 * 对比：把几十万行全部 new 进一个 List 再 doWrite，JVM 堆会瞬间被占满甚至 OOM。
 * EasyExcel 默认把内容写到临时文件（inMemory(false)），进一步压低常驻内存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BigDataService {

    /** 单次最多允许导出的行数（保护演示服务） */
    private static final int MAX_ROWS = 200_000;

    private final ExcelLogStore logStore;
    private final ExcelPracticeProperties props;

    /**
     * 分页边查边写导出 N 行。
     */
    public byte[] exportBytes(int rows) {
        int safeRows = clamp(rows);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out, UserHead.class)
                .inMemory(false) // 写临时文件而非常驻内存，大文件更省堆
                .build();
        WriteSheet sheet = EasyExcel.writerSheet("员工大数据").build();

        int pageSize = props.getBigdataPageSize();
        List<UserHead> page = new ArrayList<>(pageSize);
        for (int i = 1; i <= safeRows; i++) {
            // 模拟「分页查询」：一次只取一条，攒满一页就写出并清空
            page.add(toHead(DemoData.user(i)));
            if (page.size() >= pageSize) {
                writer.write(page, sheet);
                page.clear();
            }
        }
        if (!page.isEmpty()) {
            writer.write(page, sheet);
        }
        writer.finish(); // 必须调用，否则临时文件不落盘、文件不完整

        byte[] data = out.toByteArray();
        logStore.add("export", "bigdata", "大数据导出 " + safeRows + " 行（分页边查边写）", safeRows, Math.max(1L, safeRows / 1000L));
        return data;
    }

    /**
     * 导出演示（JSON）：行数/页大小/文件大小/耗时/内存策略。
     */
    public Map<String, Object> exportDemo(int rows) {
        int safeRows = clamp(rows);
        long start = System.currentTimeMillis();
        byte[] data = exportBytes(safeRows);
        long costMs = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", safeRows);
        result.put("pageSize", props.getBigdataPageSize());
        result.put("pages", (safeRows + props.getBigdataPageSize() - 1) / props.getBigdataPageSize());
        result.put("bytes", data.length);
        result.put("costMs", costMs);
        result.put("inMemory", false);
        result.put("download", "/api/bigdata/download?rows=" + safeRows);
        result.put("tip", "内存中始终只有一页（" + props.getBigdataPageSize() + " 行）数据：查一页、写一页、清一页，"
                + "这是大数据导出的标准姿势。");
        return result;
    }

    /**
     * 内存对比：全量 List 一把梭 vs 分页边查边写（JVM 堆采样，趋势胜于绝对值）。
     */
    public Map<String, Object> compare(int rows) {
        int safeRows = clamp(Math.min(rows, 100_000));
        int pageSize = props.getBigdataPageSize();

        // ---- 方案 A：全量 List + doWrite ----
        System.gc();
        long memBeforeA = usedMemory();
        List<UserHead> all = new ArrayList<>(safeRows);
        for (int i = 1; i <= safeRows; i++) {
            all.add(toHead(DemoData.user(i)));
        }
        ByteArrayOutputStream outA = new ByteArrayOutputStream();
        EasyExcel.write(outA, UserHead.class).sheet("全量").doWrite(all);
        long usedA = Math.max(0, usedMemory() - memBeforeA);

        // ---- 方案 B：分页边查边写 ----
        System.gc();
        long memBeforeB = usedMemory();
        byte[] outB = exportBytes(safeRows);
        long usedB = Math.max(0, usedMemory() - memBeforeB);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", safeRows);
        result.put("planA", new LinkedHashMap<String, Object>() {{
            put("desc", "全量 List + doWrite（内存峰值高）");
            put("heapUsed", usedA);
        }});
        result.put("planB", new LinkedHashMap<String, Object>() {{
            put("desc", "分页边查边写 + ExcelWriter（内存近似恒定）");
            put("heapUsed", usedB);
        }});
        result.put("bBytes", outB.length);
        result.put("tip", "JVM 堆采样有波动，看趋势：A 方案堆里躺着整个 List，B 方案永远只有一页。"
                + "几十万行时 A 可能直接 OOM，B 稳如泰山。");
        return result;
    }

    /**
     * 大数据导出速记（八股）。
     */
    public Map<String, Object> strategy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("steps", new String[]{
                "1. ExcelWriter + WriteSheet 手动管理生命周期",
                "2. 数据库分页查询（每页 5000~20000 行），查一页 write 一页",
                "3. 每页写完清空 List，内存恒定",
                "4. 全部写完调用 finish()，文件才完整",
                "5. 超大文件（几十 MB+）建议落磁盘/对象存储 + 异步下载，别走 HTTP 内存"
        });
        result.put("whyEasyExcel", "默认 SAX 流式读 + 写临时文件（inMemory(false)），内存占用远小于 POI 的 XSSFWorkbook 全量加载。");
        result.put("vsPoi", new LinkedHashMap<String, Object>() {{
            put("HSSF", ".xls 老格式，65536 行上限，一次全量进内存");
            put("XSSF", ".xlsx，整本 Workbook 常驻内存，几十万行必 OOM");
            put("SXSSF", "POI 官方流式版，思路同「边写边刷」，但 API 繁琐（要手动 flush 临时行）");
            put("EasyExcel", "对 SXSSF 的封装，注解驱动 + 自动管理临时文件，社区方案里最省心");
        }});
        result.put("tip", "导出慢的瓶颈往往在数据库查询而不在写 Excel：先优化 SQL，分页别 offset 深翻页。");
        return result;
    }

    private int clamp(int rows) {
        return Math.max(1, Math.min(rows, MAX_ROWS));
    }

    private long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
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
