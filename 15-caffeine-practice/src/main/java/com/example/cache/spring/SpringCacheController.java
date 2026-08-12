package com.example.cache.spring;

import com.example.cache.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 08. Spring Cache 注解实验接口。
 */
@RestController
@RequestMapping("/api/spring")
@RequiredArgsConstructor
@Tag(name = "08. Spring Cache 注解", description = "@Cacheable / @CachePut / @CacheEvict / @Caching")
public class SpringCacheController {

    private final UserCacheService service;

    @GetMapping("/query")
    @Operation(summary = "@Cacheable 查询", description = "连读两次：第一次 miss 打库，第二次命中；返回打库次数")
    public ApiResponse<Map<String, Object>> query(@RequestParam(defaultValue = "1") int id) {
        int before = service.dbLoads();
        Map<String, Object> first = service.query(id);   // 走代理 → @Cacheable 生效
        int afterFirst = service.dbLoads();
        Map<String, Object> second = service.query(id);
        int afterSecond = service.dbLoads();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("firstValue", first);
        result.put("secondValue", second);
        result.put("dbLoads", afterSecond - before);
        result.put("tip", "两次 query 只打库 " + (afterSecond - before) + " 次：第一次 miss 执行方法体，"
                + "第二次命中直接返回缓存（方法体没执行，dbLoads 不再增加）。");
        return ApiResponse.success(result);
    }

    @PostMapping("/update")
    @Operation(summary = "@CachePut 更新", description = "更新 DB 并把新值写进缓存，之后 query 直接命中新值")
    public ApiResponse<Map<String, Object>> update(@RequestParam(defaultValue = "1") int id,
                                                   @RequestParam(defaultValue = "张三丰") String name) {
        Map<String, Object> updated = service.update(id, name);
        Map<String, Object> cached = service.query(id); // 应该直接命中更新后的值

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("queryAfterUpdate", cached);
        result.put("tip", "@CachePut 每次执行并回写缓存：更新后立刻 query，拿到的是新名字（无需再打库）。");
        return ApiResponse.success(result);
    }

    @PostMapping("/delete")
    @Operation(summary = "@CacheEvict 删除", description = "从缓存剔除 key，之后 query 再次 miss 打库")
    public ApiResponse<Map<String, Object>> delete(@RequestParam(defaultValue = "1") int id) {
        service.query(id);        // 先缓存上
        int before = service.dbLoads();
        service.delete(id);       // @CacheEvict 剔除缓存
        service.query(id);        // 应再次 miss
        int after = service.dbLoads();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("dbLoadsAfterEvict", after - before);
        result.put("tip", "delete() 剔除缓存后，下一次 query 又 miss 重新打库（dbLoads 增加）——"
                + "这就是 @CacheEvict 的意义：让数据重新加载而不是留在缓存里变脏。");
        return ApiResponse.success(result);
    }

    @PostMapping("/multi")
    @Operation(summary = "@Caching 组合", description = "一次操作同时剔除 users 与 userCache 两个缓存")
    public ApiResponse<Map<String, Object>> multi(@RequestParam(defaultValue = "1") int id) {
        service.evictBoth(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("tip", "@Caching 可组合多个 @CacheEvict/@CachePut/@Cacheable（多缓存、多 key 一次清）。");
        return ApiResponse.success(result);
    }

    @GetMapping("/explain")
    @Operation(summary = "注解速记（八股）", description = "Cacheable/CachePut/CacheEvict/Caching 区别与注意点")
    public ApiResponse<Map<String, Object>> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annotations", new LinkedHashMap<String, Object>() {{
            put("@Cacheable", "读：命中返回缓存；miss 执行方法并回写。unless 控制哪些结果不缓存");
            put("@CachePut", "写：每次执行方法并把返回值写缓存（更新场景用）");
            put("@CacheEvict", "删：执行后剔除指定 key（allEntries=true 清空整个缓存；beforeInvocation 提前删）");
            put("@Caching", "组合：一次操作同时命中多个注解（多缓存/多 key）");
            put("@CacheConfig", "类级别：统一定 cacheNames / keyGenerator，避免每个方法重复写");
        }});
        result.put("keys", "key 用 SpEL：#id 取参数、#result 取返回值、#p0/#a0 取位置参数；"
                + "字符串拼接 key 注意 toString 陷阱（见 10 章 key-demo）。");
        result.put("watchouts", new String[]{
                "注解靠代理生效：自调用（this.xxx()）不拦截，必须通过注入的 Bean 调",
                "condition（满足才缓存）与 unless（满足就不缓存）方向相反，别搞混",
                "@CachePut 与 @Cacheable 同 key 时，Put 会覆盖 Cacheable 的缓存",
                "Spring 默认不缓存 null：@Cacheable 返回 null 不会写入，穿透防护要自己处理"
        });
        result.put("tip", "生产上 Spring Cache 注解 + Caffeine/Redis 任选一种 CacheManager 即可切换，业务代码不变。");
        return ApiResponse.success(result);
    }
}
