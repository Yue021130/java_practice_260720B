package com.example.comm.support;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 最近线程通信实验记录存储（内存版）。
 *
 * 记录每次实验的模块、动作、参与线程数、结果，供「最近实验」面板查看，
 * 类似前几章的 ExcelLogStore / CacheLogStore。真实工程里是监控平台/MQ。
 */
@Component
public class CommLogStore {

    /** 最多保留条数 */
    private static final int MAX = 50;

    private final List<Map<String, Object>> records =
            Collections.synchronizedList(new LinkedList<>());

    /**
     * 追加一条实验记录。
     *
     * @param module   模块名（shared / waitnotify / sync ...）
     * @param action   动作（volatile-demo / producer-consumer / latch-demo ...）
     * @param workers  参与线程数
     * @param ok       是否成功
     * @param note     备注
     */
    public void add(String module, String action, int workers, boolean ok, String note) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("module", module);
        record.put("action", action);
        record.put("workers", workers);
        record.put("ok", ok);
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
     * 最近实验记录（最新的在前）。
     */
    public List<Map<String, Object>> recent() {
        synchronized (records) {
            return new LinkedList<>(records);
        }
    }
}
