package com.example.os.pitfall;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 09 反模式对比：演示 Optional + Stream 的常见错误写法与正确写法。
 *
 * <p>真实业务中，这些反模式会导致 NPE、性能浪费、并发错误。把错误与正确写法放一起对比，
 * 是面试和 Code Review 中最容易被问到的点。</p>
 */
@Service
public class PitfallService {

    /**
     *  wrong-vs-right 对比演示。
     */
    public Map<String, Object> wrongVsRight() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("case1_get", case1Get());
        result.put("case2_isPresent", case2IsPresent());
        result.put("case3_orElse", case3OrElse());
        result.put("case4_forEachSideEffect", case4ForEachSideEffect());

        return result;
    }

    /**
     * 案例 1：Optional.get() 裸用 vs orElse/orElseThrow。
     */
    private Map<String, Object> case1Get() {
        Optional<String> emptyOpt = Optional.empty();

        // ❌ 错误写法：裸 get，空 Optional 会抛 NoSuchElementException。
        String wrongResult;
        try {
            wrongResult = emptyOpt.get();
        } catch (Exception e) {
            wrongResult = "❌ 异常：" + e.getClass().getSimpleName();
        }

        // ✅ 正确写法 1：orElse 给默认值。
        String rightResult1 = emptyOpt.orElse("默认值");

        // ✅ 正确写法 2：orElseThrow 在业务不允许为空时抛自定义异常。
        String rightResult2;
        try {
            rightResult2 = emptyOpt.orElseThrow(() -> new IllegalArgumentException("name 不能为空"));
        } catch (Exception e) {
            rightResult2 = "✅ 预期异常：" + e.getMessage();
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wrong", wrongResult);
        m.put("right_orElse", rightResult1);
        m.put("right_orElseThrow", rightResult2);
        m.put("note", "永远不要用 Optional.get()，除非 100% 确定有值；用 orElse / orElseGet / orElseThrow / ifPresent 之一。");
        return m;
    }

    /**
     * 案例 2：isPresent + get vs 链式 map/filter。
     */
    private Map<String, Object> case2IsPresent() {
        String rawName = "  Alice  ";

        // ❌ 错误写法：isPresent + get，又回到了 if-null 的老路。
        String wrongResult;
        Optional<String> opt = Optional.ofNullable(rawName);
        if (opt.isPresent()) {
            String trimmed = opt.get().trim();
            if (trimmed.length() > 3) {
                wrongResult = "Hello, " + trimmed.toUpperCase();
            } else {
                wrongResult = "too short";
            }
        } else {
            wrongResult = " Guest";
        }

        // ✅ 正确写法：链式表达，一个 Optional 流水线搞定。
        String rightResult = Optional.ofNullable(rawName)
                .map(String::trim)
                .filter(s -> s.length() > 3)
                .map(String::toUpperCase)
                .map(s -> "Hello, " + s)
                .orElse("Guest");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wrong", wrongResult);
        m.put("right", rightResult);
        m.put("note", "isPresent + get 是 Optional 的反模式；能链式就链式，代码更短、意图更清晰。");
        return m;
    }

    /**
     * 案例 3：orElse(expensive) 立即求值 vs orElseGet 惰性求值。
     */
    private Map<String, Object> case3OrElse() {
        Optional<String> emptyOpt = Optional.empty();
        AtomicInteger counter = new AtomicInteger(0);

        // ❌ 错误写法：orElse 会立即执行 expensive()，即使 Optional 有值也会执行。
        String wrongResult = emptyOpt.orElse(expensiveDefault(counter));
        int wrongCounter = counter.get();

        counter.set(0);
        // ✅ 正确写法：orElseGet 只在 Optional 为空时才执行。
        String rightResult = emptyOpt.orElseGet(() -> expensiveDefault(counter));
        int rightCounter = counter.get();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wrong_result", wrongResult);
        m.put("wrong_invokeCount", wrongCounter);
        m.put("right_result", rightResult);
        m.put("right_invokeCount", rightCounter);
        m.put("note", "默认值需要计算时（查库、构造大对象），必须用 orElseGet；orElse 每次都会先算一遍。");
        return m;
    }

    private String expensiveDefault(AtomicInteger counter) {
        counter.incrementAndGet();
        return "expensive-default";
    }

    /**
     * 案例 4：Stream.forEach 修改外部变量 vs collect/reduce。
     */
    private Map<String, Object> case4ForEachSideEffect() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        // ❌ 错误写法：forEach 里修改外部 sum，并行时结果错误，单线程也不推荐。
        int[] wrongSum = {0};
        numbers.forEach(n -> wrongSum[0] += n);

        // ✅ 正确写法：用 reduce / collect 做无状态聚合。
        int rightSum = numbers.stream()
                .reduce(0, Integer::sum);
        int rightSum2 = numbers.stream()
                .collect(Collectors.summingInt(Integer::intValue));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wrong_forEach_sum", wrongSum[0]);
        m.put("right_reduce_sum", rightSum);
        m.put("right_collect_sum", rightSum2);
        m.put("note", "Stream 中间/终端操作应保持无状态；求和用 reduce / collect，不要修改外部变量。");
        return m;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "Optional + Stream 反模式对比");
        result.put("antiPatterns", new String[]{
                "裸用 Optional.get()",
                "isPresent() + get() 回到 if-null",
                "orElse(expensive) 浪费计算",
                "Stream.forEach 里修改外部可变变量",
                "把 Optional 当字段或方法参数滥用"
        });
        result.put("bestPractices", new String[]{
                "用 orElse / orElseGet / orElseThrow / ifPresent 消费 Optional",
                "优先链式：map / filter / flatMap",
                "orElseGet 处理需要计算的默认值",
                "Stream 聚合用 reduce / collect，保持无状态",
                "Optional 只作为返回值类型，不要作为字段"
        });
        result.put("trap", "Optional 不是银弹，它解决的是‘语义不明确’，不是‘所有 null’；滥用 Optional 会让代码更臃肿。");
        return result;
    }
}
