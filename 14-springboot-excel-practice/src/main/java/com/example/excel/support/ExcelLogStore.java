package com.example.excel.support;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 最近读写操作记录存储（内存版）。
 *
 * 记录每次导出/导入的类型、模块、行数、耗时，供「最近操作」面板查看，
 * 类似上一章邮件的 MailRecordStore。真实工程里这层通常是数据库或 MQ。
 */
@Component
public class ExcelLogStore {

    /** 最多保留条数 */
    private static final int MAX = 50;

    private final List<Map<String, Object>> records =
            Collections.synchronizedList(new LinkedList<>());

    /**
     * 追加一条操作记录。
     *
     * @param type   export（导出）/ import（导入）
     * @param module 模块名（如 basic / validate）
     * @param note   备注，面板用
     * @param rows   处理行数
     * @param costMs 耗时毫秒
     */
    public void add(String type, String module, String note, int rows, long costMs) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("type", type);
        record.put("module", module);
        record.put("note", note);
        record.put("rows", rows);
        record.put("costMs", costMs);
        record.put("time", System.currentTimeMillis());
        records.add(0, record);
        synchronized (records) {
            while (records.size() > MAX) {
                records.remove(records.size() - 1);
            }
        }
    }

    /**
     * 最近操作记录（最新的在前）。
     */
    public List<Map<String, Object>> recent() {
        synchronized (records) {
            return new LinkedList<>(records);
        }
    }
}
