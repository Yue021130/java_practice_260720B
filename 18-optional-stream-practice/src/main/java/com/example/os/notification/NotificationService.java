package com.example.os.notification;

import com.example.os.config.PracticeProperties;
import com.example.os.domain.Notification;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 06 消息通知过滤：演示 Optional.ifPresent 做副作用 + Stream 做纯过滤。
 *
 * <p>真实场景：消息中心按用户、类型、已读状态、时间窗口过滤通知，并记录审计日志。
 * 关键原则：Stream 中间操作保持无副作用，副作用（日志、计数、发送）用 Optional.ifPresent 或 forEach 显式隔离。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MockDataRepository repository;
    private final PracticeProperties properties;

    /**
     * 过滤通知列表。
     *
     * @param userId 用户 ID，可为空
     * @param type   通知类型，可为空
     * @return 过滤结果与审计信息
     */
    public Map<String, Object> filter(Long userId, String type) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(properties.getNotificationKeepDays());

        // 1. Stream 纯过滤：不产生任何副作用，只返回新的列表。
        List<Map<String, Object>> filtered = repository.getNotifications().stream()
                // userId 为空时不按用户过滤（查全部）
                .filter(n -> userId == null || userId.equals(n.getUserId()))
                // type 为空时不按类型过滤
                .filter(n -> type == null || type.equalsIgnoreCase(n.getType()))
                // 只保留未读通知
                .filter(n -> !Boolean.TRUE.equals(n.getRead()))
                // 只保留保留天数内的通知
                .filter(n -> n.getCreateTime() != null && n.getCreateTime().isAfter(deadline))
                // 标题为空的通知降级显示
                .map(this::toView)
                .collect(Collectors.toList());

        // 2. Optional.ifPresent：只有当 userId 有效时才记录审计日志，避免无效日志污染。
        Optional.ofNullable(userId)
                .filter(id -> id > 0)
                .ifPresent(id -> log.info("[通知过滤] 用户 {} 查询类型 {}，命中 {} 条", id, type, filtered.size()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("type", type);
        result.put("count", filtered.size());
        result.put("deadline", deadline.toLocalDate().toString());
        result.put("notifications", filtered);
        result.put("interviewNote", "Stream 只做转换/过滤，副作用（日志、统计、发送）用 Optional.ifPresent 或终端操作显式隔离。");
        return result;
    }

    /**
     * 把通知实体转成前端展示对象，标题为空时给默认值。
     */
    private Map<String, Object> toView(Notification n) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", n.getId());
        view.put("type", Optional.ofNullable(n.getType()).orElse("UNKNOWN"));
        view.put("title", Optional.ofNullable(n.getTitle()).filter(s -> !s.isEmpty()).orElse("（无标题）"));
        view.put("createTime", n.getCreateTime().toString());
        return view;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "消息通知过滤");
        result.put("pattern", "Stream 纯过滤 → Optional.ifPresent 做副作用（审计日志）");
        result.put("keyPoints", new String[]{
                "Stream 中间操作（filter/map/sorted）应保持无副作用，方便并行与测试",
                "Optional.ifPresent 是显式副作用的安全入口：只有当值存在时才执行",
                "Boolean.TRUE.equals(flag) 能安全处理 Boolean 包装类的 null",
                "过滤条件可空时，用 condition == null || condition.equals(field) 实现可选过滤"
        });
        result.put("trap", "在 Stream.map 里打印日志或修改外部变量，会让流既难测试又难并行；应把副作用留在终端操作或 ifPresent。");
        return result;
    }
}
