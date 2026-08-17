package com.example.os.userprofile;

import com.example.os.domain.Order;
import com.example.os.domain.User;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 01 用户画像聚合：演示 Optional 解包对象 + Stream 聚合集合的真实业务组合。
 *
 * <p>真实场景：CRM 系统根据用户 ID 拉取会员信息，只有 VIP 且邮箱不为空时才做订单聚合，
 * 否则返回降级后的空画像。这种“先安全解包、再流式聚合”是日常业务代码最高频的模式之一。</p>
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final MockDataRepository repository;

    /**
     * 聚合指定用户的画像信息。
     *
     * @param userId 用户 ID
     * @return 画像 Map，包含用户基本信息与订单聚合结果
     */
    public Map<String, Object> aggregate(Long userId) {
        // 1. Optional 安全解包用户：避免 if (user == null) 的层层嵌套。
        return repository.findUserById(userId)
                // 2. filter：只给“VIP 且邮箱不为空”的用户做深度画像，其他用户走 orElse 兜底。
                .filter(this::isQualifiedForProfile)
                // 3. map：把 User 对象转换成画像 Map。
                .map(this::buildProfile)
                // 4. orElse：不满足条件或用户不存在时，返回降级结果，而不是抛 NPE 或返回 null。
                .orElseGet(() -> fallbackProfile(userId));
    }

    /**
     * 判断用户是否适合做深度画像：VIP 且邮箱有效。
     */
    private boolean isQualifiedForProfile(User user) {
        return user.getLevel() == User.UserLevel.VIP
                && user.getEmail() != null
                && !user.getEmail().trim().isEmpty();
    }

    /**
     * 构建完整画像：Optional 解包后的 User 对象，这里一定可以安全使用。
     */
    private Map<String, Object> buildProfile(User user) {
        // 5. Stream 聚合订单：从仓库拿订单列表（可能为空，但绝不会是 null）。
        List<Order> orders = repository.findOrdersByUserId(user.getId());

        // 6. 使用 Stream 做金额汇总：mapToInt / reduce / collect 都是真实报表常用操作。
        long orderCount = orders.size();
        BigDecimal totalAmount = orders.stream()
                // 这里 amount 在 Mock 数据里不为 null，生产中建议用 ofNullable 再防御一次。
                .map(o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = orderCount == 0
                ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(orderCount), 2, BigDecimal.ROUND_HALF_UP);

        BigDecimal maxAmount = orders.stream()
                .map(o -> Optional.ofNullable(o.getAmount()).orElse(BigDecimal.ZERO))
                // max 需要 Comparator，BigDecimal 已实现了 Comparable。
                .max(BigDecimal::compareTo)
                // 没有订单时给 0，而不是抛 NoSuchElementException。
                .orElse(BigDecimal.ZERO);

        String recentOrderTime = orders.stream()
                .map(Order::getCreateTime)
                // 按时间倒序，取最新一笔。
                .max(java.time.LocalDateTime::compareTo)
                .map(t -> t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .orElse("无订单");

        // 7. Stream 处理标签：把 List<String> 转成“#高频 #数码”这种展示字符串。
        String tagText = Optional.ofNullable(user.getTags())
                .filter(tags -> !tags.isEmpty())
                .map(tags -> tags.stream()
                        .filter(tag -> tag != null && !tag.trim().isEmpty())
                        .map(tag -> "#" + tag)
                        .collect(Collectors.joining(" ")))
                .orElse("无标签");

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("userName", user.getName());
        result.put("email", user.getEmail());
        result.put("level", user.getLevel());
        result.put("isQualified", true);
        result.put("orderCount", orderCount);
        result.put("totalAmount", totalAmount);
        result.put("avgAmount", avgAmount);
        result.put("maxAmount", maxAmount);
        result.put("recentOrderTime", recentOrderTime);
        result.put("tags", tagText);
        result.put("interviewNote", "Optional 先解包对象、Stream 再聚合集合，能消灭大量 if-null；orElse/orElseGet 负责兜底。");
        return result;
    }

    /**
     * 不满足条件时的降级画像：明确表达“没有画像”，而不是返回 null。
     */
    private Map<String, Object> fallbackProfile(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("isQualified", false);
        result.put("reason", "用户不存在、非 VIP 或邮箱为空");
        result.put("orderCount", 0);
        result.put("totalAmount", BigDecimal.ZERO);
        result.put("avgAmount", BigDecimal.ZERO);
        result.put("maxAmount", BigDecimal.ZERO);
        result.put("tags", Collections.emptyList());
        result.put("interviewNote", "生产代码要用 orElse/orElseGet 提供降级值，避免调用方再判空。");
        return result;
    }

    /**
     * 八股速记接口：返回本场景的核心考点。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "用户画像聚合");
        result.put("pattern", "Optional 解包单一对象 → Stream 聚合集合");
        result.put("keyPoints", new String[]{
                "Optional.ofNullable / filter / map / orElseGet 链式处理空值",
                "Stream 对集合做 reduce / max / min / collect 汇总",
                "不要先 isPresent 再 get，能链式就链式",
                "金额计算用 BigDecimal，注意除法精度与舍入模式"
        });
        result.put("trap", "在 Stream 里直接对可能为 null 的字段做方法调用会 NPE，先用 Optional.ofNullable 包一下。");
        return result;
    }
}
