package com.example.os.report;

import com.example.os.config.PracticeProperties;
import com.example.os.domain.Order;
import com.example.os.domain.Order.OrderStatus;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 02 订单报表统计：演示 Optional 处理“可能为空的时间范围参数” + Stream 分组聚合。
 *
 * <p>真实场景：运营后台的订单看板，前端可能传时间范围也可能不传，后端需要给出默认值，
 * 再对订单做分组、汇总、TopN 分析。Optional 非常适合封装“参数是否为空”的默认值逻辑。</p>
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final MockDataRepository repository;
    private final PracticeProperties properties;

    /**
     * 订单汇总报表。
     *
     * @param days 统计最近 N 天，为空时使用配置默认值
     * @return 汇总指标
     */
    public Map<String, Object> summary(Integer days) {
        // 1. Optional.ofNullable 封装参数：如果前端没传，就取配置文件里的默认值。
        int rangeDays = Optional.ofNullable(days)
                .filter(d -> d > 0)
                .orElse(properties.getDefaultDateRangeDays());

        LocalDateTime startTime = LocalDateTime.now().minusDays(rangeDays);

        // 2. Stream 过滤时间范围内的订单。
        List<Order> filtered = repository.getOrders().stream()
                .filter(o -> o.getCreateTime() != null)
                .filter(o -> o.getCreateTime().isAfter(startTime))
                .collect(Collectors.toList());

        // 3. 汇总指标：总单数、总金额、已完成金额、平均客单价。
        long totalCount = filtered.size();
        BigDecimal totalAmount = filtered.stream()
                .map(o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal completedAmount = filtered.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .map(o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = totalCount == 0
                ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(totalCount), 2, BigDecimal.ROUND_HALF_UP);

        // 4. Top 3 消费用户：按 userId 分组求和，再按金额排序取前 3。
        List<Map<String, Object>> topUsers = filtered.stream()
                .collect(Collectors.groupingBy(
                        Order::getUserId,
                        Collectors.mapping(
                                o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", e.getKey());
                    m.put("amount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rangeDays", rangeDays);
        result.put("startTime", startTime.toLocalDate().toString());
        result.put("totalCount", totalCount);
        result.put("totalAmount", totalAmount);
        result.put("completedAmount", completedAmount);
        result.put("avgAmount", avgAmount);
        result.put("topUsers", topUsers);
        result.put("interviewNote", "Optional 处理可空参数默认值，Stream 做过滤 + 分组 + 排序 + TopN，是后台报表的标准组合。");
        return result;
    }

    /**
     * 按订单状态分组统计。
     */
    public Map<String, Object> byStatus() {
        // 5. groupingBy 分组计数：真实业务中最常见的统计方式。
        Map<String, Long> statusCount = repository.getOrders().stream()
                .collect(Collectors.groupingBy(
                        o -> Optional.ofNullable(o.getStatus())
                                .map(Enum::name)
                                .orElse("UNKNOWN"),
                        Collectors.counting()
                ));

        // 6. groupingBy 分组求和：按状态汇总金额。
        Map<String, BigDecimal> statusAmount = repository.getOrders().stream()
                .collect(Collectors.groupingBy(
                        o -> Optional.ofNullable(o.getStatus())
                                .map(Enum::name)
                                .orElse("UNKNOWN"),
                        Collectors.mapping(
                                o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statusCount", statusCount);
        result.put("statusAmount", statusAmount);
        result.put("interviewNote", "groupingBy + mapping + reducing 是 Stream 分组聚合的黄金三角，比手写 for 循环更 declarative。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "订单报表统计");
        result.put("pattern", "Optional 处理可空参数默认值 → Stream 过滤 / 分组 / 汇总 / TopN");
        result.put("keyPoints", new String[]{
                "Optional.ofNullable(param).filter(正数).orElse(默认值) 是处理可空参数的优雅写法",
                "Stream.filter 做范围过滤，注意时间字段也要判空",
                "Collectors.groupingBy + reducing / summingInt 做分组聚合",
                "TopN 用 sorted + limit，比全部排序再 subList 更高效"
        });
        result.put("trap", "分组 key 如果可能为 null，会抛 NullPointerException；用 Optional 包装或自定义 key 提取器兜底。");
        return result;
    }
}
