package com.example.excel.annotation;

import com.alibaba.excel.EasyExcel;
import com.example.excel.common.ExcelBizException;
import com.example.excel.support.DemoData;
import com.example.excel.support.ExcelLogStore;
import com.example.excel.support.UserRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 02. 注解与字段：列顺序 / 忽略字段 / 列宽行高 / 数字日期格式。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final ExcelLogStore logStore;

    /**
     * 导出：用带全套注解的 head 类写出 xlsx。
     */
    public byte[] exportBytes() {
        List<UserAnnoHead> heads = new ArrayList<>();
        for (UserRow row : DemoData.users()) {
            heads.add(toHead(row));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, UserAnnoHead.class)
                .sheet("注解演示")
                .doWrite(heads);
        byte[] data = out.toByteArray();
        logStore.add("export", "annotation", "注解与字段：导出 8 条用户", heads.size(), 10L);
        return data;
    }

    /**
     * 导出演示（JSON）：说明输出列顺序与注解作用。
     */
    public Map<String, Object> exportDemo() {
        byte[] data = exportBytes();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filename", "注解演示.xlsx");
        result.put("bytes", data.length);
        result.put("outputColumns", new String[]{"编号", "姓名", "部门", "月薪", "入职日期", "在职"});
        result.put("declaredOrder", new String[]{"编号", "姓名", "月薪", "入职日期", "部门", "在职"});
        result.put("remarkColumnExported", false);
        result.put("tip", "输出列顺序由 @ExcelProperty(order) 决定而非字段声明顺序；"
                + "@ExcelIgnore 的 remark 字段没有出现在 Excel 里。");
        return result;
    }

    /**
     * 导入演示：读回刚才导出的文件，验证注解在「读」方向同样生效。
     */
    public Map<String, Object> importDemo() {
        byte[] data = exportBytes();
        List<UserAnnoHead> parsed;
        try {
            parsed = EasyExcel.read(new ByteArrayInputStream(data))
                    .head(UserAnnoHead.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new ExcelBizException("解析 Excel 失败：" + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", parsed.size());
        result.put("firstRows", parsed.subList(0, Math.min(3, parsed.size())));
        result.put("remarkAllNull", parsed.stream().allMatch(u -> u.getRemark() == null));
        result.put("salaryParsedAsNumber", parsed.get(0).getSalary() != null);
        result.put("dateParsedAsDate", parsed.get(0).getHireDate() != null);
        result.put("tip", "导入时按表头名匹配字段，@NumberFormat / @DateTimeFormat 负责把字符串转成 Double / Date，"
                + "@ExcelIgnore 的字段不会被赋值（remark 全是 null）。");
        return result;
    }

    /**
     * 注解总表（八股）。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annotations", new LinkedHashMap<String, Object>() {{
            put("@ExcelProperty", "最核心：value 表头名（可用 String[] 实现多级表头）、index 强制列下标、order 排序列顺序");
            put("@ExcelIgnore", "忽略字段：导出不写、导入不赋值");
            put("@ExcelIgnoreUnannotated", "类级别：只有带注解的字段才参与读写");
            put("@ColumnWidth / @HeadRowHeight / @ContentRowHeight", "列宽 / 表头行高 / 内容行高");
            put("@NumberFormat / @DateTimeFormat", "数字与日期格式：导出写格式、导入按格式解析");
            put("@ContentLoopMerge / @HeadStyle / @ContentStyle", "合并相同单元格 / 表头与内容样式（见 03 章）");
        }});
        result.put("converter", "字段类型与 Excel 单元格类型的转换由 Converter 完成，"
                + "@ExcelProperty(converter = XxxConverter.class) 可自定义（如枚举/字典转中文）。");
        result.put("tip", "写一个 head 类 = 同时定义「列怎么显示」与「对象怎么映射」，导入导出共用一个类，是 EasyExcel 优雅的核心。");
        return result;
    }

    private UserAnnoHead toHead(UserRow row) {
        UserAnnoHead head = new UserAnnoHead();
        head.setId(row.getId());
        head.setName(row.getName());
        head.setDepartment(row.getDepartment());
        head.setSalary(row.getSalary());
        head.setHireDate(row.getHireDate());
        head.setActive(row.getActive());
        head.setRemark("内部备注-" + row.getId());
        return head;
    }
}
