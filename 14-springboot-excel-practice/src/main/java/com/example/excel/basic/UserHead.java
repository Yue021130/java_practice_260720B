package com.example.excel.basic;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 用户导出/导入的 head 类（01 快速开始）。
 *
 * @ExcelProperty("列名")：声明表头名称，导出时按字段声明顺序生成列，
 * 导入时按表头名称匹配字段。这就是 EasyExcel「注解驱动」的核心：
 * 一个类同时定义「Excel 长什么样」与「Java 对象长什么样」。
 */
@Data
public class UserHead {

    /** 员工编号 */
    @ExcelProperty("员工编号")
    private Integer id;

    /** 姓名 */
    @ExcelProperty("姓名")
    private String name;

    /** 部门 */
    @ExcelProperty("部门")
    private String department;

    /** 月薪 */
    @ExcelProperty("月薪")
    private Double salary;

    /** 入职日期 */
    @ExcelProperty("入职日期")
    private Date hireDate;

    /** 是否在职 */
    @ExcelProperty("在职状态")
    private Boolean active;
}
