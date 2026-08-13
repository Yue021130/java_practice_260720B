package com.example.sign.support;

import com.example.sign.config.SignPracticeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * nonce 去重存储（内存版，模拟 Redis：SETNX + TTL）。
 *
 * 真实工程用 Redis {@code SET nonce:{nonce} 1 EX 300 NX} 原子去重；
 * 这里用内存 Map 记录「nonce → 记录时间」，查询时清理超期的，等价实现。
 * nonce 的作用：配合时间戳防重放——同一请求被重放时 nonce 已存在 → 拒绝。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NonceStore {

    private final SignPracticeProperties props;

    /** nonce → 首次记录时间戳（毫秒） */
    private final Map<String, Long> store = new LinkedHashMap<>();

    /**
     * 尝试占用 nonce。
     *
     * @return true = 首次出现，占用成功；false = 已存在（重放，应拒绝）
     */
    public synchronized boolean tryAcquire(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long ttlMs = props.getNonceTtlSeconds() * 1000L;
        // 先清理过期的（模拟 TTL 到期自动删除）
        Iterator<Map.Entry<String, Long>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > ttlMs) {
                it.remove();
            }
        }
        if (store.containsKey(nonce)) {
            return false;   // 已被用过 → 重放
        }
        store.put(nonce, now);
        return true;
    }

    public int size() {
        return store.size();
    }
}
