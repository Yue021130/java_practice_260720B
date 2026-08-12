package com.example.cache.support;

import com.example.cache.config.CachePracticeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 「数据库」模拟器：真正的数据源。
 *
 * 真实工程里这里是一次 JDBC/MyBatis 查询；本专题用内存 Map 当表，
 * 每次查询都 sleep(loadCostMs) 模拟慢 SQL，缓存命中与否的差距一眼可见。
 *
 * 同时维护两个计数器：
 * - userLoadCount：普通 load（快速开始/统计/预热用）
 * - stampedeLoadCount：击穿演示专用（见 06 章，便于断言「只查了一次库」）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotDataService {

    private static final String[] DEPTS = {"研发部", "产品部", "市场部", "运营部", "人事部"};

    private final CachePracticeProperties props;

    /** 模拟数据库表：userId -> 用户数据 */
    private final Map<Integer, Map<String, Object>> userDb = new ConcurrentHashMap<>();

    /** 普通 load 次数 */
    private final AtomicLong userLoadCount = new AtomicLong();

    /** 击穿演示的 load 次数 */
    private final AtomicLong stampedeLoadCount = new AtomicLong();

    /** 用户 key 前缀（各模块统一用 "user:{id}"） */
    public static final String KEY_PREFIX = "user:";

    @PostConstruct
    public void init() {
        for (int i = 1; i <= 100; i++) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id", i);
            user.put("name", "员工" + i);
            user.put("dept", DEPTS[i % DEPTS.length]);
            user.put("level", i % 5 + 1);
            user.put("salary", 8000 + i * 100);
            userDb.put(i, user);
        }
        log.info("模拟数据库就绪：100 个用户");
    }

    /**
     * 按缓存 key（"user:1"）加载用户。
     */
    public Map<String, Object> loadUser(String cacheKey) {
        int id = Integer.parseInt(cacheKey.substring(KEY_PREFIX.length()));
        return loadUser(id);
    }

    /**
     * 按 id 加载用户：sleep 模拟慢 SQL，返回深拷贝（避免调用方改坏「表」）。
     */
    public Map<String, Object> loadUser(Integer id) {
        userLoadCount.incrementAndGet();
        sleep();
        Map<String, Object> user = userDb.get(id);
        return user == null ? null : new LinkedHashMap<>(user);
    }

    /**
     * 击穿演示用：加载前把 key 从缓存清掉，模拟「热点 key 过期瞬间」。
     */
    public Map<String, Object> loadUserForStampede(Integer id) {
        stampedeLoadCount.incrementAndGet();
        sleep();
        Map<String, Object> user = userDb.get(id);
        return user == null ? null : new LinkedHashMap<>(user);
    }

    /**
     * 写入用户（模拟 UPDATE 数据库）。
     */
    public void saveUser(Integer id, Map<String, Object> data) {
        userDb.put(id, new LinkedHashMap<>(data));
    }

    /**
     * 热门用户列表（缓存预热的数据源）。
     */
    public List<Map<String, Object>> hotUsers(int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> user = userDb.get(i);
            if (user != null) {
                list.add(new LinkedHashMap<>(user));
            }
        }
        return list;
    }

    public int userLoadCount() {
        return (int) userLoadCount.get();
    }

    public int stampedeLoadCount() {
        return (int) stampedeLoadCount.get();
    }

    public int dbSize() {
        return userDb.size();
    }

    public Map<Integer, Map<String, Object>> userDb() {
        return userDb;
    }

    private void sleep() {
        try {
            Thread.sleep(props.getLoadCostMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
