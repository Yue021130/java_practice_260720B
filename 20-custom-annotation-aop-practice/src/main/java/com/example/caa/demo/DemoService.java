package com.example.caa.demo;

import com.example.caa.annotation.DataMasking;
import com.example.caa.annotation.LogOperation;
import com.example.caa.annotation.RateLimit;
import com.example.caa.annotation.RequirePermission;
import com.example.caa.annotation.Timing;
import com.example.caa.domain.User;
import com.example.caa.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 演示业务服务：各种自定义注解标注在方法上，供 AOP 切面拦截。
 */
@Service
@RequiredArgsConstructor
public class DemoService {

    private final MockDataRepository repository;

    /**
     * 操作日志示例。
     */
    @LogOperation(value = "查询用户详情", logParams = true, logResult = true)
    public User getUser(Long id) {
        return repository.findById(id);
    }

    /**
     * 权限校验示例。
     */
    @RequirePermission("admin")
    public Map<String, Object> adminOnly() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "只有 admin 能看到这条数据");
        result.put("secret", "admin-secret-001");
        return result;
    }

    /**
     * 权限校验示例（普通用户权限）。
     */
    @RequirePermission("user:view")
    public Map<String, Object> userView() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "拥有 user:view 权限即可查看");
        return result;
    }

    /**
     * 接口限流示例：1 秒内最多 2 次请求。
     */
    @RateLimit(qps = 2, window = 1, timeUnit = TimeUnit.SECONDS, message = "点击太快了，请慢一点")
    public Map<String, Object> rateLimit() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "请求成功");
        return result;
    }

    /**
     * 数据脱敏示例。
     */
    @DataMasking(fields = {"phone", "email", "idCard"}, maskType = DataMasking.MaskType.DEFAULT)
    public User maskingUser() {
        return repository.findById(1L);
    }

    /**
     * 数据脱敏列表示例。
     */
    @DataMasking(fields = {"phone", "email"}, maskType = DataMasking.MaskType.DEFAULT)
    public List<User> maskingUserList() {
        return repository.findAll();
    }

    /**
     * 耗时监控示例。
     */
    @Timing("模拟耗时操作")
    public Map<String, Object> timing() {
        try {
            // 模拟 50~150ms 的业务耗时
            Thread.sleep(50 + (long) (Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "耗时操作执行完成");
        return result;
    }

    /**
     * 注解组合示例：同时记录日志、校验权限、限流、耗时。
     */
    @LogOperation(value = "组合注解演示")
    @RequirePermission("admin")
    @RateLimit(qps = 5, window = 1, timeUnit = TimeUnit.SECONDS)
    @Timing("组合操作")
    public Map<String, Object> combine() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "组合注解全部通过");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 演示 AOP 异常日志：方法抛出异常，LogOperationAspect 会记录失败日志。
     */
    @LogOperation(value = "异常日志演示")
    public Map<String, Object> errorLog() {
        throw new RuntimeException("模拟业务异常");
    }
}
