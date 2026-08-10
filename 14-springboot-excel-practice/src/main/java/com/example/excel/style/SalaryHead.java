package com.example.excel.style;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.util.Date;

/**
 * 部门薪资 head 类（03 样式与格式）。
 *
 * 列宽/行高用注解声明；颜色、边框、对齐、合并单元格由 Service 里的
 * 样式策略（HorizontalCellStyleStrategy）+ 自定义 WriteHandler 实现。
 * 部门字段特意按「两两相同」排列，配合 LoopMergeStrategy 合并相同部门单元格。
 */
@Data
@HeadRowHeight(28)
@ContentRowHeight(22)
public class SalaryHead {

    @ExcelProperty(value = "部门", order = 0)
    @ColumnWidth(16)
    private String department;

    @ExcelProperty(value = "姓名", order = 1)
    @ColumnWidth(12)
    private String name;

    @ExcelProperty(value = "月薪", order = 2)
    @ColumnWidth(12)
    private Double salary;

    @ExcelProperty(value = "季度奖金", order = 3)
    @ColumnWidth(12)
    private Double bonus;

    @ExcelProperty(value = "合计", order = 4)
    @ColumnWidth(12)
    private Double total;

    @ExcelProperty(value = "入职日期", order = 5)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(16)
    private Date hireDate;
}
