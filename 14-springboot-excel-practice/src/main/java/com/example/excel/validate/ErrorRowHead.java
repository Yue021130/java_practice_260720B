package com.example.excel.validate;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 错误回写 head 类：把校验失败的行导出成一个「问题清单.xlsx」。
 *
 * 经典做法：不合法的行不进库，而是连同原因一起导出，让导入人照着改，
 * 比前端弹一堆错误更贴近真实生产（批量导入系统都这么做）。
 */
@Data
public class ErrorRowHead {

    /** 原文件中的行号 */
    @ExcelProperty("行号")
    private Integer rowNo;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("错误信息")
    private String error;
}
