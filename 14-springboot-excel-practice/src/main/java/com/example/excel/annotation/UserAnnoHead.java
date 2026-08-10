package com.example.excel.annotation;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.util.Date;

/**
 * 用户 head 类（02 注解与字段）。
 *
 * 这一章集中展示 EasyExcel 的核心注解：
 * - {@link ExcelProperty}：value 表头名；order 控制列顺序（默认按字段声明顺序）
 * - {@link ExcelIgnore}：该字段既不导出也不导入
 * - {@link ColumnWidth} / {@link HeadRowHeight} / {@link ContentRowHeight}：列宽 / 表头行高 / 内容行高
 * - {@link NumberFormat}：导出时把数字格式化成 "#,##0.00"；导入时按该格式解析
 * - {@link DateTimeFormat}：导出时把 Date 格式化成 yyyy-MM-dd；导入时按该格式解析
 *
 * 注意：字段声明顺序是 id,name,salary,hireDate,department,active，
 * 但 order 强制输出 编号→姓名→部门→月薪→入职日期→在职，
 * 说明 <b>order 优先级高于声明顺序</b>（面试常问）。
 */
@Data
@HeadRowHeight(24)
@ContentRowHeight(20)
public class UserAnnoHead {

    @ExcelProperty(value = "编号", order = 0)
    private Integer id;

    @ExcelProperty(value = "姓名", order = 1)
    private String name;

    @ExcelProperty(value = "部门", order = 2)
    @ColumnWidth(20)
    private String department;

    @ExcelProperty(value = "月薪", order = 3)
    @NumberFormat("#,##0.00")
    private Double salary;

    @ExcelProperty(value = "入职日期", order = 4)
    @DateTimeFormat("yyyy-MM-dd")
    private Date hireDate;

    @ExcelProperty(value = "在职", order = 5)
    private Boolean active;

    /** 内部备注：@ExcelIgnore 后 Excel 里看不到它，导入时也不会给它赋值 */
    @ExcelIgnore
    private String remark;
}
