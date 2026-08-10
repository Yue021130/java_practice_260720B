package com.example.excel.pitfall;

import com.alibaba.excel.EasyExcel;
import com.example.excel.basic.UserHead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 10. 常见坑与调优：把生产上踩过的坑摆出来讲明白。
 *
 * 这一章没有「能不能跑」的问题，全是「跑得对不对」的问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PitfallService {

    /**
     * 10 个高频坑（原因 → 解法）。
     */
    public Map<String, Object> list() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pitfalls", new Object[]{
                pit("只支持 .xlsx", "EasyExcel 不支持 .xls：传入 .xls 直接报错", "统一转 .xlsx / 用 EasyExcel 转换器 / 提醒用户另存为 .xlsx"),
                pit("表头名对不上", "导入时按表头名匹配字段，模板改列名后字段全为 null", "模板受控 + 监听器校验表头 invokeHeadMap；用 index 锁死列"),
                pit("忘记 finish()", "ExcelWriter 手动模式不调 finish() 文件不完整", "writer.finish() 放 finally"),
                pit("大数据量一把梭", "几十万行 new 进 List 再 doWrite → OOM", "分页边查边写（见 05 章）"),
                pit("日期/数字格式", "不配 @DateTimeFormat/@NumberFormat，导出一串时间戳/科学计数法", "head 字段加格式注解；读入也按注解解析"),
                pit("中文文件名乱码", "Content-Disposition 只写 filename=\"中文\"，老浏览器乱码", "filename + filename*=UTF-8'' 双写（见 ExcelWebSupport）"),
                pit("上传默认 1MB", "Spring multipart 默认 1MB，Excel 经常超限 413", "yml 配 max-file-size / max-request-size"),
                pit("导入不校验直接入库", "坏数据进库，后患无穷", "三层校验 + 错误回写（见 06 章）"),
                pit("大事务包整批导入", "一行失败回滚全部，几十万行回滚很惨", "按批提交，批内失败仅回滚该批"),
                pit("用 POI 手撸 Excel", "Workbook 全量常驻内存 + 大量样板代码", "能用 EasyExcel 用 EasyExcel，样式用模板填充"),
        });
        return result;
    }

    /**
     * EasyExcel vs POI（面试高频对比）。
     */
    public Map<String, Object> poiVsEasyexcel() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compare", new Object[]{
                row("定位", "底层库：操作单元格/样式/公式", "基于 POI 的封装：注解驱动、开箱即用"),
                row("读内存", "XSSF 整本 Workbook 常驻内存，大文件 OOM", "SAX 流式逐行回调，内存恒定"),
                row("写大文件", "SXSSF 也能流式，但要手动管理临时行/API 繁琐", "一行 doWrite / 分页边查边写"),
                row("代码量", "读写 10 列要几十行样板", "一个 head 类 + 两行 API"),
                row("样式/复杂表头", "全手动，灵活但累", "注解 + 策略 + 模板填充，够用且快"),
                row("格式", ".xls 与 .xlsx 都支持", "仅 .xlsx"),
                row("生态", "POI 是所有 Java Excel 库的地基", "社区方案里最流行，阿里内部验证")
        });
        result.put("conclusion", "自己造轮子用 POI；业务导入导出直接用 EasyExcel。面试说清这个取舍就够了。");
        return result;
    }

    /**
     * 现场演示「表头名对不上」：动态表头写「员工工号」，UserHead 期望「员工编号」。
     */
    public Map<String, Object> headMismatchDemo() {
        // 1. 用动态表头造一个「列名与 head 类不一致」的文件
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("员工工号"));   // 应为「员工编号」
        head.add(Collections.singletonList("姓名"));
        head.add(Collections.singletonList("部门"));
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList(1, "张伟", "研发部"));
        data.add(Arrays.asList(2, "李娜", "产品部"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).sheet().doWrite(data);
        byte[] file = out.toByteArray();

        // 2. 按 UserHead 读回，看哪些字段读不到
        List<UserHead> parsed = EasyExcel.read(new ByteArrayInputStream(file))
                .head(UserHead.class)
                .sheet()
                .doReadSync();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileHeader", new String[]{"员工工号", "姓名", "部门"});
        result.put("expectedHeader", new String[]{"员工编号", "姓名", "部门"});
        result.put("parsedRows", parsed);
        result.put("idNull", parsed.stream().anyMatch(u -> u.getId() == null));
        result.put("nameFilled", parsed.stream().allMatch(u -> u.getName() != null));
        result.put("conclusion", "「员工工号」≠「员工编号」，id 字段读不到（null），而姓名/部门正常 → "
                + "导入数据静默丢失，比直接报错更危险。");
        result.put("fix", new String[]{
                "表头用 index 锁死列（模板受控时），不依赖名字匹配",
                "监听器 invokeHeadMap 校验表头：不一致直接拒绝导入",
                "或把 head 类列名改成与模板一致"
        });
        return result;
    }

    /**
     * 调优要点。
     */
    public Map<String, Object> tuning() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", new String[]{
                "读：大文件必须用监听器/同步读留 batchSize 上限，禁止 doReadSync 读超大文件",
                "写：大数据导出分页边查边写 + inMemory(false)；超大文件落磁盘/对象存储 + 异步",
                "线程池：导出/导入异步化，用独立线程池，避免占用请求线程",
                "限流防刷：导出接口加频控，超大导出转任务单",
                "内存参数：Xss 视情况调大；监听器里及时 clear 批次",
                "数据量真的很大的导入：先落临时表 → 异步分批 → 校验 → 回写结果，别在请求里一把做完"
        });
        return result;
    }

    private Map<String, Object> pit(String name, String cause, String fix) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("cause", cause);
        m.put("fix", fix);
        return m;
    }

    private Map<String, Object> row(String dim, String poi, String easyexcel) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dimension", dim);
        m.put("poi", poi);
        m.put("easyexcel", easyexcel);
        return m;
    }
}
