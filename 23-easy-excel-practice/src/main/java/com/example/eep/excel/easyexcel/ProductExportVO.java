package com.example.eep.excel.easyexcel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * EasyExcel 商品导出视图对象。
 */
@Data
public class ProductExportVO {

    @ExcelProperty("类型名称")
    @ColumnWidth(16)
    private String typeName;

    @ExcelProperty(value = "额外扣费", converter = WhetherConverter.class)
    @ColumnWidth(14)
    private Integer needPay;

    @ExcelProperty("扣费金额")
    @ColumnWidth(14)
    private BigDecimal price;

    @ExcelProperty(value = "是否默认", converter = WhetherConverter.class)
    @ColumnWidth(14)
    private Integer isDefault;

    @ExcelProperty("Ncode")
    @ColumnWidth(16)
    private String loungeCode;
}
