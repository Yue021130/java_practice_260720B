package com.example.sbcore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class CacheOpsService {

    @Autowired
    private CacheOpsHelperService helper;

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        helper.resetCallCount();

        String v1 = helper.cacheable("key1");
        String v2 = helper.cacheable("key1");

        String v3 = helper.cachePut("key1", "更新后");
        String v4 = helper.cacheable("key1");

        helper.cacheEvict("key1");
        String v5 = helper.cacheable("key1");

        data.put("cacheableFirst", v1);
        data.put("cacheableSecond", v2);
        data.put("cachePutResult", v3);
        data.put("afterCachePut", v4);
        data.put("afterEvict", v5);
        data.put("actualMethodInvocations", helper.getCallCount());

        data.put("interviewNote",
                "@Cacheable 先查缓存再执行；@CachePut 始终执行方法并更新缓存（写后更新）；" +
                "@CacheEvict 删除缓存，allEntries=true 清空整个 cache，beforeInvocation 控制方法执行前后删除。");

        return data;
    }
}
