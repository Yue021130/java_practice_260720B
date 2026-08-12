package com.example.cache.support;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 最近缓存操作记录存储（内存版）。
 *
 * 记录每次缓存读写的模块、key、结果，供「最近操作」面板查看，
 * 类似前几章的 MailRecordStore / ExcelLogStore。真实工程里是监控平台/MQ。
 */
@Component
public class CacheLogStore {

    /** 最多保留条数 */
    private static final int MAX = 50;

    private final List<Map<String, Object>> records =
            Collections.synchronizedList(new LinkedList<>());

    /**
     * 追加一条操作记录。
     *
     * @param module  模块名（basic / preheat / twolevel ...）
     * @param action  操作（get / put / invalidate / warm ...）
     * @param key     缓存 key
     * @param hit     是否命中
     * @param note    备注
     */
    public void add(String module, String action, String key, Boolean hit, String note) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("module", module);
        record.put("action", action);
        record.put("key", key);
        record.put("hit", hit);
        record.put("note", note);
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
