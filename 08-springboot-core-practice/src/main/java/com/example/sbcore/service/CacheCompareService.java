package com.example.sbcore.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CacheCompareService {

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, String>> table = new ArrayList<>();
        table.add(buildRow("存储位置", "JVM 堆内存", "远程 Redis 服务器"));
        table.add(buildRow("一致性", "多实例不一致，各进程独立", "多实例共享，一致性好"));
        table.add(buildRow("延迟", "微秒级，无网络开销", "毫秒级，受网络影响"));
        table.add(buildRow("容量", "受单机内存限制", "可集群扩展"));
        table.add(buildRow("持久化", "不支持，重启丢失", "支持 RDB/AOF"));
        table.add(buildRow("序列化", "Java 对象直接引用", "需配置 key/value 序列化"));
        table.add(buildRow("适用场景", "单实例、读多写少、热点数据", "分布式系统、会话共享、需要持久化"));

        data.put("comparison", table);

        List<String> checklist = new ArrayList<>();
        checklist.add("单实例且追求低延迟 → 优先 Caffeine 本地缓存");
        checklist.add("多实例共享或需要持久化 → 使用 Redis 分布式缓存");
        checklist.add("是否为热点数据设置了合理的 key，避免缓存穿透");
        checklist.add("@CacheEvict 是否在写操作后正确清理或更新缓存");
        checklist.add("是否监控了缓存命中率，避免缓存形同虚设");
        checklist.add("Redis 场景下是否配置了合适的 key/value 序列化方式");

        data.put("checklist", checklist);

        data.put("interviewNote",
                "本地缓存（Caffeine）适合单机低延迟读多写少；分布式缓存（Redis）适合集群共享、需要一致性或可持久化的场景。" +
                "实际项目中常采用两级缓存：Caffeine 做 L1，Redis 做 L2，兼顾速度与一致性。");

        return data;
    }

    private Map<String, String> buildRow(String dimension, String caffeine, String redis) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("dimension", dimension);
        row.put("caffeine", caffeine);
        row.put("redis", redis);
        return row;
    }
}
