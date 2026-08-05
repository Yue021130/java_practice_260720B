package com.example.exception.bestpractice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 异常最佳实践与反模式场景服务。
 */
@Slf4j
@Service
public class BestPracticeScenarioService {

    /**
     * 演示吞异常的危害。
     */
    public Map<String, Object> swallowException() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        // 反模式
        try {
            int i = 1 / 0;
        } catch (ArithmeticException e) {
            // 空 catch：异常被吞掉，排障困难
        }
        tips.add("反模式：空 catch 会吞掉异常，问题无法定位");

        // 正确做法
        try {
            int i = 1 / 0;
        } catch (ArithmeticException e) {
            log.error("计算失败", e);
            tips.add("正确：至少记录日志，最好继续抛或返回错误");
        }

        result.put("tips", tips);
        result.put("rule", "不要 catch 了你处理不了的异常却不做任何记录");
        return result;
    }

    /**
     * 演示不要用异常做流程控制。
     */
    public Map<String, Object> flowControl() {
        Map<String, Object> result = new LinkedHashMap<>();

        int count = 100000;

        // 错误方式：用异常控制循环结束
        long start1 = System.nanoTime();
        int found1 = 0;
        for (int i = 0; i < count; i++) {
            try {
                if (i > count / 2) {
                    throw new RuntimeException("break");
                }
                found1 = i;
            } catch (RuntimeException e) {
                break;
            }
        }
        long badCost = System.nanoTime() - start1;

        // 正确方式：正常条件判断
        long start2 = System.nanoTime();
        int found2 = 0;
        for (int i = 0; i < count / 2; i++) {
            found2 = i;
        }
        long goodCost = System.nanoTime() - start2;

        result.put("badCostNs", badCost);
        result.put("goodCostNs", goodCost);
        result.put("tip", "异常创建开销大、破坏可读性，不要用异常替代 if/for/return");
        return result;
    }

    /**
     * 演示 fail-fast：入参校验前置。
     */
    public Map<String, Object> failFast() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        String input = null;
        // 方式 1：Objects.requireNonNull
        try {
            Objects.requireNonNull(input, "input 不能为空");
        } catch (NullPointerException e) {
            tips.add("Objects.requireNonNull 立即失败：" + e.getMessage());
        }

        // 方式 2：Guava 风格 Preconditions（本项目不引入 Guava，用 if 演示）
        int age = -1;
        if (age < 0 || age > 150) {
            tips.add("参数校验失败：age 范围非法，应使用 0-150");
        }

        result.put("tips", tips);
        result.put("rule", "早失败、早暴露，避免把错误带到下游引发更难排查的问题");
        return result;
    }

    /**
     * 异常日志规范。
     */
    public Map<String, Object> logging() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        try {
            throw new RuntimeException("业务错误");
        } catch (RuntimeException e) {
            log.error("订单创建失败，订单号={}", "ORDER_123", e);
            tips.add("正确：log.error(msg, params, exception) 同时记录上下文与堆栈");
        }

        tips.add("错误：log.error(e.getMessage()) 丢失堆栈");
        tips.add("错误：异常被连续打印两次（catch 里打一次，抛到外层又打一次）");
        tips.add("正确：对外接口只返回脱敏信息，内部日志保留完整异常");

        result.put("tips", tips);
        return result;
    }

    /**
     * 事务与异常：演示回滚规则。
     *
     * 这里只做理论说明，不真正连接数据库。
     */
    public Map<String, Object> transaction() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("defaultRule", "Spring 默认只对 RuntimeException / Error 回滚，checked exception 不回滚");
        result.put("rollbackFor", "@Transactional(rollbackFor = Exception.class) 可指定 checked exception 也回滚");
        result.put("noRollbackFor", "@Transactional(noRollbackFor = IllegalArgumentException.class) 可指定某些异常不回滚");
        result.put("tip", "业务异常继承 RuntimeException 通常最简单；需要 checked 回滚时显式配置 rollbackFor");
        return result;
    }

    /**
     * 异常转换 vs 透传。
     */
    public Map<String, Object> translateOrPass() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> tips = new ArrayList<>();

        try {
            lowLevelCall();
        } catch (java.io.IOException e) {
            // 跨层转换：保留 cause
            RuntimeException wrapped = new RuntimeException("服务调用失败", e);
            tips.add("跨层转换：" + wrapped.getMessage() + "，cause=" + wrapped.getCause().getClass().getSimpleName());
        }

        tips.add("跨层调用建议转换异常：DAO SQLException → Service 业务异常");
        tips.add("同层内可透传，不要无意义地 catch 再 throw 相同类型");
        result.put("tips", tips);
        return result;
    }

    private void lowLevelCall() throws java.io.IOException {
        throw new java.io.IOException("连接超时");
    }
}
