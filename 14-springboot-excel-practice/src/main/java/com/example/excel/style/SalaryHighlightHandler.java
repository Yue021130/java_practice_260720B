package com.example.excel.style;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * 自定义写处理器：月薪列（第 3 列）超过阈值标红加粗。
 *
 * 演示 EasyExcel 的底层扩展点 CellWriteHandler：
 * afterCellDispose(context) 在每个单元格「内容已就位、样式即将落盘」时回调，
 * 在这里可以按行/列/值任意改样式，是官方内置策略之外的万能扩展口。
 *
 * 注：3.x 的 afterCellDispose 参数是 CellWriteHandlerContext，
 * 通过 context.getOriginalValue() 拿原始值、context.getHead() 判断是否表头、
 * context.getCell() 拿 POI 单元格；2.x 是老式多参签名，换版本时注意差异（常见坑）。
 */
public class SalaryHighlightHandler implements CellWriteHandler {

    /** 高亮阈值 */
    private final double threshold;

    /** 月薪列下标（0-based） */
    private static final int SALARY_COLUMN = 2;

    public SalaryHighlightHandler(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        // 表头不处理
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        Integer columnIndex = context.getColumnIndex();
        if (columnIndex == null || columnIndex != SALARY_COLUMN) {
            return;
        }
        // 原始值是 Double 类型的月薪
        Object raw = context.getOriginalValue();
        if (!(raw instanceof Double) || (Double) raw <= threshold) {
            return;
        }
        // 在现有样式基础上克隆一份，把字体改成红色加粗（保留边框/对齐）
        Cell cell = context.getCell();
        CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
        style.cloneStyleFrom(cell.getCellStyle());
        Font font = cell.getSheet().getWorkbook().createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        cell.setCellStyle(style);
    }
}
