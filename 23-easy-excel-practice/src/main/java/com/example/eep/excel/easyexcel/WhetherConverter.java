package com.example.eep.excel.easyexcel;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.util.Objects;

/**
 * EasyExcel 自定义 Converter：Integer 1/0 与 是/否 互转。
 */
public class WhetherConverter implements Converter<Integer> {

    private static final Integer YES = 1;
    private static final Integer NO = 0;
    private static final String YES_DESC = "是";
    private static final String NO_DESC = "否";

    @Override
    public Class<?> supportJavaTypeKey() {
        return Integer.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * 导入：Excel 文字 → Java Integer。
     */
    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                     GlobalConfiguration globalConfiguration) {
        String value = cellData.getStringValue();
        return YES_DESC.equals(value) ? YES : NO;
    }

    /**
     * 导出：Java Integer → Excel 文字。
     */
    @Override
    public WriteCellData<?> convertToExcelData(Integer value, ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(Objects.equals(value, YES) ? YES_DESC : NO_DESC);
    }
}
