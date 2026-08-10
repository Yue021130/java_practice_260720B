package com.example.excel.mergehead;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.merge.LoopMergeStrategy;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 04. 复杂表头：多级分组表头 + 纵向合并相同区域。
 *
 * 表头合并完全靠 @ExcelProperty(value = {"一级","二级"}) 自动完成；
 * 数据行「区域」相同值的纵向合并用 LoopMergeStrategy(eachRow=3, columnIndex=0)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MergeHeadService {

    private final ExcelLogStore logStore;

    /**
     * 导出：多级表头的年度销售业绩表。
     */
    public byte[] exportBytes() {
        List<SalesHead> rows = buildRows();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, SalesHead.class)
                // 每 3 行合并第 0 列（区域）中值相同的单元格
                .registerWriteHandler(new LoopMergeStrategy(3, 0))
                .sheet("2025年度销售")
                .doWrite(rows);
        byte[] data = out.toByteArray();
        logStore.add("export", "mergehead", "复杂表头：年度销售业绩表", rows.size(), 10L);
        return data;
    }

    /**
     * 导出演示（JSON）：描述表头层级。
     */
    public Map<String, Object> exportDemo() {
        byte[] data = exportBytes();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", "年度销售业绩表.xlsx");
        result.put("bytes", data.length);
        result.put("headLevels", "2 级：一级【区域/一季度/二季度/年度合计】→ 二级【目标/实际】分组");
        result.put("mergedRegion", true);
        result.put("tip", "value 数组的长度决定该列占几级表头；层级不够时 EasyExcel 自动跨行合并单元格。");
        return result;
    }

    /**
     * 复杂表头速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("how", "@ExcelProperty(value = {\"一级\",\"二级\",...}) 的数组长度=表头级数；"
                + "不同字段级数不同时，少的字段自动跨行合并补齐。");
        result.put("mergeTypes", new LinkedHashMap<String, Object>() {{
            put("表头合并", "多级 value 自动完成，无需额外代码");
            put("数据行合并", "LoopMergeStrategy(eachRow, columnIndex) 纵向合并相同值；@ContentLoopMerge 注解版");
            put("绝对区域合并", "OnceAbsoluteMergeStrategy(首行,末行,首列,末列) 合并任意矩形，如大标题行");
        }});
        result.put("tip", "合并单元格会影响后续筛选/排序/公式，生产上先想清楚业务是否真的需要合并。");
        return result;
    }

    /**
     * 构造 9 条数据：区域按 3 行一组，供 LoopMergeStrategy 合并。
     */
    private List<SalesHead> buildRows() {
        List<SalesHead> rows = new ArrayList<>();
        rows.add(row("华北", 100, 120, 110, 130));
        rows.add(row("华北", 90, 105, 100, 115));
        rows.add(row("华北", 80, 95, 85, 100));
        rows.add(row("华东", 120, 140, 130, 150));
        rows.add(row("华东", 110, 125, 115, 140));
        rows.add(row("华东", 100, 110, 105, 120));
        rows.add(row("华南", 90, 100, 95, 110));
        rows.add(row("华南", 85, 92, 88, 105));
        rows.add(row("华南", 70, 80, 75, 90));
        return rows;
    }

    private SalesHead row(String region, int q1t, int q1a, int q2t, int q2a) {
        SalesHead head = new SalesHead();
        head.setRegion(region);
        head.setQ1Target(q1t);
        head.setQ1Actual(q1a);
        head.setQ2Target(q2t);
        head.setQ2Actual(q2a);
        head.setTotal(q1t + q1a + q2t + q2a);
        return head;
    }
}
