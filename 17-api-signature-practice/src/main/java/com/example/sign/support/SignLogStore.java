package com.example.sign.support;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 最近签名校验操作记录存储（内存版）。
 *
 * 记录每次验签的模块、appid、校验结果与原因，供「最近校验」面板查看，
 * 类似前几章的 ExcelLogStore / CommLogStore。真实工程里是监控平台/MQ。
 */
@Component
public class SignLogStore {

    /** 最多保留条数 */
    private static final int MAX = 50;

    private final List<Map<String, Object>> records =
            Collections.synchronizedList(new LinkedList<>());

    /**
     * 追加一条验签记录。
     *
     * @param module   模块名（verify / timestamp / nonce / interceptor ...）
     * @param appId    appid
     * @param passed   是否通过校验
     * @param reason   通过/拒绝原因
     */
    public void add(String module, String appId, boolean passed, String reason) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("module", module);
        record.put("appId", appId);
        record.put("passed", passed);
        record.put("reason", reason);
        record.put("time", System.currentTimeMillis());
        records.add(0, record);
        synchronized (records) {
            while (records.size() > MAX) {
                records.remove(records.size() - 1);
            }
        }
    }

    /**
     * 最近验签记录（最新的在前）。
     */
    public List<Map<String, Object>> recent() {
        synchronized (records) {
            return new LinkedList<>(records);
        }
    }
}
