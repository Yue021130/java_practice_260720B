package com.example.scp.cache;

import java.io.Serializable;

/**
 * 空值占位符：用于「缓存穿透」防护。
 *
 * <p>查询不存在的数据时，如果直接把 null 写进缓存，Redis 存不了 null
 * （或存进去反序列化歧义），导致每次请求都打到数据库。本章约定：
 * 业务值为 null 时写入本占位符，TTL 只有 30 秒（远短于正常值），
 * 短时间内的重复穿透查询会被占位符挡住，同时不会长期污染缓存。</p>
 *
 * <p>枚举单例，天然 Serializable，JSON 序列化为 "NullValue" 字符串，
 * 反序列化回来 equals 判定即可识别。</p>
 */
public enum NullValue implements Serializable {

    INSTANCE;

    @Override
    public String toString() {
        return "NullValue";
    }
}
