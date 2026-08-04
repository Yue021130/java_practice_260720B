package com.example.sbcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class CacheRedisService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private CacheManager redisCacheManager;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        boolean connected = false;
        try {
            redisConnectionFactory.getConnection().ping();
            connected = true;
        } catch (Exception e) {
            log.warn("Redis 未连接：{}", e.getMessage());
        }

        data.put("redisConnected", connected);

        if (connected && redisTemplate != null) {
            String key = "sbcore:book:isbn-999";
            redisTemplate.opsForValue().set(key, "分布式缓存中的书");
            Object value = redisTemplate.opsForValue().get(key);
            data.put("cachedValue", value);
            data.put("keyPattern", "sbcore:book:* 可被多个实例共享");
        } else {
            data.put("cachedValue", "Redis 未启动，无法演示写入");
            data.put("tip", "请在本地启动 Redis 默认端口 6379 后重试");
        }

        data.put("interviewNote",
                "Redis 作为分布式缓存，多实例共享同一份数据，支持持久化与集群；但存在网络开销和序列化问题。" +
                "推荐 key/value 序列化：StringRedisSerializer + GenericJackson2JsonRedisSerializer。");

        return data;
    }
}
