package com.example.os.dataclean;

import com.example.os.config.PracticeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 04 批量数据清洗：演示 Optional 逐字段清洗 + Stream 批量过滤空值与脏数据。
 *
 * <p>真实场景：ETL / 数据同步 / 批量导入前，需要对原始记录做标准化：去空、转类型、兜底默认值。
 * Optional 的 map/filter/orElse 组合非常适合“单字段清洗”，Stream 则负责“批量记录清洗”。</p>
 */
@Service
@RequiredArgsConstructor
public class DataCleanService {

    private final PracticeProperties properties;

    /**
     * 批量清洗原始数据。
     *
     * @param maxRows 最大处理条数，为空使用默认值
     * @return 清洗结果统计与样本
     */
    public Map<String, Object> clean(Integer maxRows) {
        int limit = Optional.ofNullable(maxRows)
                .filter(m -> m > 0 && m <= properties.getMaxCleanBatchSize())
                .orElse(properties.getMaxCleanBatchSize());

        // 1. 模拟从上游系统拉到的原始脏数据：字段可能为空、带前后空格、类型错误。
        List<Map<String, String>> rawList = buildRawData().stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 2. Stream 逐条清洗：每条记录返回 Optional<Map<String, Object>>，合法则保留，不合法丢弃。
        List<Map<String, Object>> cleanedList = new ArrayList<>();
        List<Map<String, Object>> discardedList = new ArrayList<>();

        rawList.forEach(raw -> {
            Optional<Map<String, Object>> cleaned = cleanSingleRecord(raw);
            if (cleaned.isPresent()) {
                cleanedList.add(cleaned.get());
            } else {
                // 脏数据样本需要转成 Map<String, Object> 以统一返回类型。
                Map<String, Object> discarded = new LinkedHashMap<>();
                discarded.putAll(raw);
                discardedList.add(discarded);
            }
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputCount", rawList.size());
        result.put("cleanedCount", cleanedList.size());
        result.put("discardedCount", discardedList.size());
        result.put("cleanedSample", cleanedList.stream().limit(3).collect(Collectors.toList()));
        result.put("discardedSample", discardedList.stream().limit(3).collect(Collectors.toList()));
        result.put("interviewNote", "Optional 负责单字段清洗兜底，Stream 负责批量记录流转；把脏数据显式丢弃而不是默默跳过，便于审计。");
        return result;
    }

    /**
     * 单条记录清洗：每个字段都用 Optional 处理可空。
     */
    private Optional<Map<String, Object>> cleanSingleRecord(Map<String, String> raw) {
        // 3. name：去空、非空校验，空则整条丢弃。
        String name = Optional.ofNullable(raw.get("name"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        if (name == null) {
            return Optional.empty();
        }

        // 4. age：转 Integer，失败则丢弃。
        Integer age = Optional.ofNullable(raw.get("age"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(this::parseIntOpt)
                .filter(a -> a > 0 && a < 150)
                .orElse(null);
        if (age == null) {
            return Optional.empty();
        }

        // 5. amount：转 BigDecimal，失败或负数则丢弃。
        BigDecimal amount = Optional.ofNullable(raw.get("amount"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(this::parseDecimalOpt)
                .filter(a -> a.compareTo(BigDecimal.ZERO) >= 0)
                .orElse(null);
        if (amount == null) {
            return Optional.empty();
        }

        // 6. phone：可选字段，为空时给默认值 UNKNOWN，而不是丢弃。
        String phone = Optional.ofNullable(raw.get("phone"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse("UNKNOWN");

        Map<String, Object> cleaned = new LinkedHashMap<>();
        cleaned.put("name", name);
        cleaned.put("age", age);
        cleaned.put("amount", amount);
        cleaned.put("phone", phone);
        return Optional.of(cleaned);
    }

    private Optional<Integer> parseIntOpt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parseDecimalOpt(String s) {
        try {
            return Optional.of(new BigDecimal(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 构造模拟脏数据。
     */
    private List<Map<String, String>> buildRawData() {
        return Arrays.asList(
                createRaw("  张三  ", "28", "199.50", "13800138001"),
                createRaw(null, "28", "199.50", "13800138001"),
                createRaw("李四", "abc", "2999.00", "13900139002"),
                createRaw("王五", "35", "-100", "13700137004"),
                createRaw("赵六", "200", "100.00", null),
                createRaw("孙七", "30", "", "13600136005"),
                createRaw("  周八  ", "  25  ", "  88.80  ", "  ")
        );
    }

    private Map<String, String> createRaw(String name, String age, String amount, String phone) {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("name", name);
        raw.put("age", age);
        raw.put("amount", amount);
        raw.put("phone", phone);
        return raw;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "批量数据清洗");
        result.put("pattern", "Optional 单字段清洗 → Stream 批量过滤 → 脏数据显式丢弃");
        result.put("keyPoints", new String[]{
                "字段级清洗：ofNullable(value).map(trim).filter(非空).orElse(null/default)",
                "类型转换失败用 Optional.empty() 表达，而不是返回 null 或抛异常",
                "Stream + Optional 的组合能把 ETL 流程写得很像流水线",
                "不要把 orElse(null) 滥用回 NPE 老路；要明确是丢弃还是给默认值"
        });
        result.put("trap", "Optional.ofNullable(...).map(Integer::parseInt) 遇到非法字符串会抛异常，应放在 try-catch 里返回 Optional.empty()。");
        return result;
    }
}
