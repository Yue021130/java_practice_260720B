package com.example.excel.style;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.merge.LoopMergeStrategy;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 03. 样式与格式：表头样式 / 内容样式 / 条件高亮 / 合并相同单元格。
 *
 * 三种改样式的层级（从简单到进阶，面试必问）：
 * 1. 注解：@HeadStyle / @ContentStyle / @ColumnWidth / @HeadRowHeight ...
 * 2. 内置策略：HorizontalCellStyleStrategy（表头+内容两套样式一次性套用）
 * 3. 自定义 WriteHandler：CellWriteHandler / RowWriteHandler / SheetWriteHandler，按值/按行任意改
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleService {

    private final ExcelLogStore logStore;

    /**
     * 导出：带完整样式的部门薪资报表。
     */
    public byte[] exportBytes() {
        List<SalaryHead> rows = buildRows();

        // ---- 表头样式：浅蓝底 + 白色加粗 + 居中 ----
        WriteFont headFont = new WriteFont();
        headFont.setBold(true);
        headFont.setColor(IndexedColors.WHITE.getIndex());
        headFont.setFontHeightInPoints((short) 12);
        WriteCellStyle headStyle = new WriteCellStyle();
        headStyle.setWriteFont(headFont);
        headStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // ---- 内容样式：全边框 + 居中 ----
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints((short) 11);
        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setWriteFont(contentFont);
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        // 内置策略：一次性套用表头+内容样式
        HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headStyle, contentStyle);

        // 合并相同部门单元格：每 2 行合并第 0 列（值相同才合并）
        LoopMergeStrategy mergeStrategy = new LoopMergeStrategy(2, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, SalaryHead.class)
                .registerWriteHandler(styleStrategy)   // 内置样式策略
                .registerWriteHandler(mergeStrategy)   // 合并相同部门
                .registerWriteHandler(new SalaryHighlightHandler(15000)) // 自定义高亮
                .sheet("部门薪资")
                .doWrite(rows);
        byte[] data = out.toByteArray();
        logStore.add("export", "style", "样式与格式：部门薪资报表", rows.size(), 12L);
        return data;
    }

    /**
     * 导出演示（JSON）：说明应用了哪些样式。
     */
    public Map<String, Object> exportDemo() {
        byte[] data = exportBytes();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", "部门薪资报表.xlsx");
        result.put("bytes", data.length);
        result.put("styles", new LinkedHashMap<String, Object>() {{
            put("表头", "浅蓝底 + 白色加粗 + 居中（HorizontalCellStyleStrategy）");
            put("内容", "全边框 + 居中（HorizontalCellStyleStrategy）");
            put("高亮", "月薪 > 15000 标红加粗（自定义 SalaryHighlightHandler）");
            put("合并", "部门两两相同自动合并（LoopMergeStrategy）");
            put("列宽行高", "@ColumnWidth / @HeadRowHeight / @ContentRowHeight 注解");
        }});
        result.put("tip", "下载后打开文件看效果：高亮行就是月薪过万的「重点员工」。");
        return result;
    }

    /**
     * 样式机制速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("levels", new String[]{
                "1. 注解：@HeadStyle / @ContentStyle / @ColumnWidth / @HeadRowHeight / @ContentRowHeight",
                "2. 内置策略：HorizontalCellStyleStrategy（表头+内容）、AbstractCellStyleStrategy、LoopMergeStrategy、OnceAbsoluteMergeStrategy",
                "3. 自定义 Handler：CellWriteHandler / RowWriteHandler / SheetWriteHandler，可读值、按条件改样式（本项目高亮示例）"
        });
        result.put("merge", new LinkedHashMap<String, Object>() {{
            put("LoopMergeStrategy(eachRow, columnIndex)", "纵向合并：每 N 行中值相同的单元格合并成一块");
            put("OnceAbsoluteMergeStrategy(firstRow,lastRow,firstCol,lastCol)", "绝对合并：把指定矩形区域合并（如大标题行）");
            put("@ContentLoopMerge", "注解版纵向合并，作用在字段上");
        }});
        result.put("tip", "生产上「模板 + 预设样式」往往比代码里逐像素调样式更省事：让 UI 出模板，后端只填数据。");
        return result;
    }

    /**
     * 构造 6 条数据：部门两两相同，供 LoopMergeStrategy 演示合并。
     */
    private List<SalaryHead> buildRows() {
        List<SalaryHead> rows = new ArrayList<>();
        rows.add(row("研发部", "张伟", 18000.0, 6000.0, date(2019, 2, 5)));
        rows.add(row("研发部", "李娜", 15000.0, 5000.0, date(2020, 5, 12)));
        rows.add(row("产品部", "王强", 12000.0, 3000.0, date(2021, 0, 20)));
        rows.add(row("产品部", "赵敏", 11000.0, 2500.0, date(2022, 3, 8)));
        rows.add(row("市场部", "刘洋", 9800.0, 2000.0, date(2023, 1, 14)));
        rows.add(row("市场部", "陈静", 9000.0, 1500.0, date(2023, 8, 1)));
        return rows;
    }

    private SalaryHead row(String dept, String name, double salary, double bonus, Date hireDate) {
        SalaryHead head = new SalaryHead();
        head.setDepartment(dept);
        head.setName(name);
        head.setSalary(salary);
        head.setBonus(bonus);
        head.setTotal(salary + bonus);
        head.setHireDate(hireDate);
        return head;
    }

    private Date date(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 0, 0, 0);
        return cal.getTime();
    }
}
