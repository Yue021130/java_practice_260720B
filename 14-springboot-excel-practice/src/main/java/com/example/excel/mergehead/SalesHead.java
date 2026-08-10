package com.example.excel.mergehead;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * 2025 年度销售业绩 head 类（04 复杂表头）。
 *
 * @ExcelProperty 的 value 支持 String[] 多级表头：
 *   - value = "区域"          → 只占一级表头
 *   - value = {"一季度", "目标"} → 两级表头（「一季度」在上，「目标」在下）
 * 不同字段层级不同时，EasyExcel 会自动把「区域」「年度合计」跨行合并，
 * 形成常见的复杂分组表头——这是 EasyExcel 对比手工 POI 最省事的地方之一。
 *
 * 区域字段按 3 行一组相同排列，配合 LoopMergeStrategy(3, 0) 演示纵向合并。
 */
@Data
@HeadRowHeight(22)
@ContentRowHeight(20)
public class SalesHead {

    @ExcelProperty(value = "区域")
    @ColumnWidth(12)
    private String region;

    @ExcelProperty(value = {"一季度", "目标"})
    private Integer q1Target;

    @ExcelProperty(value = {"一季度", "实际"})
    private Integer q1Actual;

    @ExcelProperty(value = {"二季度", "目标"})
    private Integer q2Target;

    @ExcelProperty(value = {"二季度", "实际"})
    private Integer q2Actual;

    @ExcelProperty(value = "年度合计")
    @ColumnWidth(12)
    private Integer total;
}
