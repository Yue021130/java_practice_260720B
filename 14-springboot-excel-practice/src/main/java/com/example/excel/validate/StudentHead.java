package com.example.excel.validate;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 学生信息 head 类（06 数据校验与错误反馈）。
 *
 * 这里演示 @ExcelProperty 的另一种用法：index 强制指定列下标（0 起），
 * 适合「模板固定、不允许改列」的场景；按名字匹配则更宽容，允许换列顺序。
 * 校验失败的 errorMsg 用 @ExcelIgnore 兜着，只在程序里用、不进出 Excel。
 */
@Data
public class StudentHead {

    @ExcelProperty(value = "学号", index = 0)
    private String studentNo;

    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ExcelProperty(value = "年龄", index = 2)
    private Integer age;

    @ExcelProperty(value = "手机号", index = 3)
    private String phone;

    @ExcelProperty(value = "专业", index = 4)
    private String major;

    /** 校验失败时的错误信息（不参与导入导出映射） */
    @ExcelIgnore
    private String errorMsg;
}
