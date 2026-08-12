package com.example.cache.spring;

import com.example.cache.common.CacheBizException;
import com.example.cache.support.HotDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 08. Spring Cache 注解实战：@Cacheable / @CachePut / @CacheEvict / @Caching。
 *
 * 注意：注解必须由 Spring 代理拦截才生效——
 * - 方法要 public，且通过注入的 Bean 调用（本项目由 Controller 调用，走代理）；
 * - 同一类内部 this.xxx() 是自调用，不走代理、注解不生效（经典坑）。
 *
 * 这些方法对应 CaffeineCacheManager 里的命名缓存 users / userCache。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final HotDataService hotDataService;

    /** 查询真正打到 DB 的次数（@Cacheable 命中时方法体不执行，计数不会增加） */
    private final AtomicLong dbLoads = new AtomicLong();

    /**
     * 查询：@Cacheable —— 命中直接返回缓存，miss 才执行方法体并把返回值写进缓存。
     * key="#id"：SpEL 取参数 id 当 key；unless="#result==null"：null 不缓存（防穿透要另行处理）。
     */
    @Cacheable(cacheNames = "users", key = "#id", unless = "#result == null")
    public Map<String, Object> query(Integer id) {
        dbLoads.incrementAndGet();
        return hotDataService.loadUser(id);
    }

    /**
     * 更新：@CachePut —— 方法执行完把返回值写进缓存（不回库时也保持缓存与库一致）。
     * 与 @Cacheable 的区别：@CachePut 每次都执行方法体并更新缓存。
     */
    @CachePut(cacheNames = "users", key = "#id")
    public Map<String, Object> update(Integer id, String name) {
        Map<String, Object> current = hotDataService.userDb().get(id);
        if (current == null) {
            throw new CacheBizException("用户不存在: " + id);
        }
        Map<String, Object> updated = new LinkedHashMap<>(current);
        updated.put("name", name);
        hotDataService.saveUser(id, updated);
        return updated; // 返回值会写入缓存
    }

    /**
     * 删除：@CacheEvict —— 从缓存删除对应 key（真实场景先删库再删缓存，这里只演示删缓存）。
     */
    @CacheEvict(cacheNames = "users", key = "#id")
    public void delete(Integer id) {
        log.info("用户 {} 已从 users 缓存剔除", id);
    }

    /**
     * 组合：@Caching —— 一次操作同时影响多个缓存/多个 key。
     * 这里演示同时剔除 users 与 userCache 两个缓存里的同 key。
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", key = "#id"),
            @CacheEvict(cacheNames = "userCache", key = "#id")
    })
    public void evictBoth(Integer id) {
        log.info("用户 {} 已从 users 与 userCache 两个缓存剔除", id);
    }

    /**
     * 演示辅助：查询真实打库次数。
     */
    public int dbLoads() {
        return (int) dbLoads.get();
    }
}
