package com.example.cache.pitfall;

import com.example.cache.support.HotDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SpEL key 陷阱对比服务（10 章）。
 *
 * 两个方法都 @Cacheable，区别只在 key 的写法：
 * - queryBad：key="#param" —— 整个对象当 key，不同实例就是不同 key → 缓存永远 miss；
 * - queryGood：key="#param.id" —— 取业务主键当 key → 等价对象命中同一缓存。
 *
 * 这是生产上最常见的「缓存像没开一样」的根因之一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyTrapService {

    private final HotDataService hotDataService;

    private final AtomicLong badDbLoads = new AtomicLong();
    private final AtomicLong goodDbLoads = new AtomicLong();

    /** 错误写法：整个对象当 key（toString 不同 → 永远 miss） */
    @Cacheable(cacheNames = "userCache", key = "#param")
    public Map<String, Object> queryBad(Param param) {
        badDbLoads.incrementAndGet();
        return hotDataService.loadUser(param.getId());
    }

    /** 正确写法：取业务主键当 key（等价对象命中同一缓存） */
    @Cacheable(cacheNames = "userCache", key = "#param.id")
    public Map<String, Object> queryGood(Param param) {
        goodDbLoads.incrementAndGet();
        return hotDataService.loadUser(param.getId());
    }

    public int badLoads() {
        return (int) badDbLoads.get();
    }

    public int goodLoads() {
        return (int) goodDbLoads.get();
    }
}
