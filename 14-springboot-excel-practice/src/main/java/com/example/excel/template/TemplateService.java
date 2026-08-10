package com.example.excel.template;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import com.example.excel.support.ExcelLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 08. 模板导出：先出模板、再填数据。
 *
 * 生产最佳实践：报表的样式/列宽/Logo 由 UI 用 Excel 排好版，
 * 后端只负责把 {占位符} 替换成真实数据，样式零代码。
 *
 * 本项目没有真实模板文件，用 POI 在内存里「造」一个模板（含 {占位符}），
 * 再走 EasyExcel 的模板填充 API：
 *   EasyExcel.write(out).withTemplate(template).sheet().doFill(数据)
 * 复杂填充（列表）：new FillWrapper("item", list) + forceNewRow(true)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ExcelLogStore logStore;

    /**
     * 生成空白模板（含 {占位符}）。
     */
    public byte[] templateBytes() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("销售订单");

            XSSFRow title = sheet.createRow(0);
            title.createCell(0).setCellValue("销售订单");

            sheet.createRow(2).createCell(0).setCellValue("客户：");
            sheet.createRow(2).createCell(1).setCellValue("{customer}");
            sheet.createRow(3).createCell(0).setCellValue("日期：");
            sheet.createRow(3).createCell(1).setCellValue("{date}");
            sheet.createRow(4).createCell(0).setCellValue("联系电话：");
            sheet.createRow(4).createCell(1).setCellValue("{phone}");

            XSSFRow head = sheet.createRow(6);
            head.createCell(0).setCellValue("商品");
            head.createCell(1).setCellValue("单价");
            head.createCell(2).setCellValue("数量");
            head.createCell(3).setCellValue("小计");

            // 列表占位行：{item.xxx} 会被 FillWrapper("item", list) 逐行填充
            XSSFRow placeholder = sheet.createRow(7);
            placeholder.createCell(0).setCellValue("{item.name}");
            placeholder.createCell(1).setCellValue("{item.price}");
            placeholder.createCell(2).setCellValue("{item.count}");
            placeholder.createCell(3).setCellValue("{item.subtotal}");

            sheet.createRow(9).createCell(0).setCellValue("合计：");
            sheet.createRow(9).createCell(1).setCellValue("{total}");
            sheet.createRow(11).createCell(0).setCellValue("备注：");
            sheet.createRow(11).createCell(1).setCellValue("{remark}");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成模板失败：" + e.getMessage(), e);
        }
    }

    /**
     * 填充模板：简单填充（Map）+ 列表填充（FillWrapper）。
     */
    public byte[] fillBytes() {
        Map<String, Object> simple = new LinkedHashMap<>();
        simple.put("customer", "北京云启科技");
        simple.put("date", "2026-08-09");
        simple.put("phone", "010-88886666");
        simple.put("total", "¥ 12,600.00");
        simple.put("remark", "本合同金额含 13% 增值税，请于 7 日内付款。");

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item("AI 算法服务器", "¥ 8,000.00", 1));
        items.add(item("深度学习工作站", "¥ 2,200.00", 2));
        items.add(item("企业级 NAS 存储", "¥ 200.00", 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out)
                .withTemplate(new ByteArrayInputStream(templateBytes()))
                .build();
        WriteSheet sheet = EasyExcel.writerSheet().build();
        writer.fill(simple, sheet);   // 简单填充：{customer} {date} ...
        // 列表填充：forceNewRow(true) 保证每个元素新起一行
        FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
        writer.fill(new FillWrapper("item", items), fillConfig, sheet);
        writer.finish();
        byte[] data = out.toByteArray();
        logStore.add("export", "template", "模板填充销售订单", items.size() + 1, 15L);
        return data;
    }

    /**
     * 填充演示（JSON）。
     */
    public Map<String, Object> fillDemo() {
        byte[] data = fillBytes();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templatePlaceholders", new String[]{
                "简单填充：{customer} {date} {phone} {total} {remark}",
                "列表填充：{item.name} {item.price} {item.count} {item.subtotal}"
        });
        result.put("itemCount", 3);
        result.put("bytes", data.length);
        result.put("templateDownload", "/api/template/template-download");
        result.put("fillDownload", "/api/template/fill-download");
        result.put("tip", "样式/列宽全部由模板文件决定，后端一行代码不写样式——这是「模板导出」的核心价值。");
        return result;
    }

    /**
     * 模板填充速记（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", "报表样式交给模板（UI 排好版），后端只填数据：样式零代码、改版不动 Java、前后端分工清晰。");
        result.put("api", new String[]{
                "入口：EasyExcel.write(out).withTemplate(模板流).build()",
                "简单填充：writer.fill(Map 或 POJO, sheet)，替换 {字段}",
                "列表填充：writer.fill(new FillWrapper(\"item\", List), sheet, forceNewRow(true))，替换 {item.字段}",
                "收尾：writer.finish() 必须调用"
        });
        result.put("tips", new String[]{
                "占位符不要用中文/带空格：{item.name} 而非 { 商品 }",
                "列表占位行必须是一整行，EasyExcel 依此生成后续行",
                "多列表并存：多个 FillWrapper(\"名称\", list)，互不干扰",
                "模板里可以预置公式/合并单元格，填充后依然保留"
        });
        result.put("tip", "复杂报表（合同/对账单/报关单）首选模板填充；纯数据导出（列表下载）用 head 类直接写更省事。");
        return result;
    }

    private Map<String, Object> item(String name, String price, int count) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("count", count);
        item.put("subtotal", price);
        return item;
    }
}
