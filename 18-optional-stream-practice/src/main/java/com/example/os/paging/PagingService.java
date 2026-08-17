package com.example.os.paging;

import com.example.os.domain.User;
import com.example.os.domain.User.UserLevel;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 08 分页结果再加工：演示 Optional 解包分页列表 + Stream 排序 / 过滤 / 转换 / peek。
 *
 * <p>真实场景：MyBatis-Plus / PageHelper 分页返回 Page<T>，业务层拿到 records 后还要做二次加工：
 * 过滤敏感字段、按权重排序、去重、限流展示。Optional 能安全解包“分页对象可能为 null 或 records 为空”的情况。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagingService {

    private final MockDataRepository repository;

    /**
     * 模拟分页查询后再加工。
     *
     * @param page 页码，从 1 开始，为空默认 1
     * @param size 每页大小，为空默认 5
     * @return 加工后的分页数据
     */
    public Map<String, Object> transform(Integer page, Integer size) {
        // 1. Optional 处理可空分页参数，给出默认值与边界保护。
        int finalPage = Optional.ofNullable(page)
                .filter(p -> p > 0)
                .orElse(1);
        int finalSize = Optional.ofNullable(size)
                .filter(s -> s > 0 && s <= 100)
                .orElse(5);

        // 2. 模拟 DAO 分页查询：从内存数据里截取一页。
        List<User> allUsers = repository.getUsers();
        int fromIndex = (finalPage - 1) * finalSize;
        int toIndex = Math.min(fromIndex + finalSize, allUsers.size());

        // 3. 分页结果可能越界，返回空列表而不是 null。
        List<User> pageRecords = fromIndex >= allUsers.size()
                ? Collections.emptyList()
                : allUsers.subList(fromIndex, toIndex);

        // 4. Optional 解包集合：如果 pageRecords 为空，直接返回降级结果；否则进入 Stream 加工链。
        List<Map<String, Object>> transformed = Optional.ofNullable(pageRecords)
                .filter(list -> !list.isEmpty())
                .map(list -> list.stream()
                        // 5. peek 仅用于调试，不修改元素；生产环境通常用日志框架。
                        .peek(u -> log.debug("[分页再加工] 处理用户: {}", u.getName()))
                        // 6. 过滤：只保留有邮箱的用户（演示二次过滤）。
                        .filter(u -> u.getEmail() != null && !u.getEmail().trim().isEmpty())
                        // 7. 排序：VIP > NORMAL > GUEST。
                        .sorted(Comparator.comparing(this::levelPriority).reversed())
                        // 8. 转换：只暴露需要的字段。
                        .map(this::toView)
                        .collect(Collectors.toList()))
                // 9. 如果集合为空，给空列表而不是 null，保持接口稳定。
                .orElse(Collections.emptyList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", finalPage);
        result.put("size", finalSize);
        result.put("total", allUsers.size());
        result.put("rawCount", pageRecords.size());
        result.put("transformedCount", transformed.size());
        result.put("records", transformed);
        result.put("interviewNote", "Optional.ofNullable(list).filter(非空).map(list.stream()...).orElse(emptyList()) 是安全解包集合的经典写法。");
        return result;
    }

    private int levelPriority(User user) {
        switch (user.getLevel()) {
            case VIP:
                return 3;
            case NORMAL:
                return 2;
            case GUEST:
            default:
                return 1;
        }
    }

    private Map<String, Object> toView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("name", user.getName());
        view.put("level", user.getLevel());
        view.put("email", user.getEmail());
        return view;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "分页结果再加工");
        result.put("pattern", "Optional 解包分页列表 → Stream 过滤 / 排序 / 转换 / peek 调试");
        result.put("keyPoints", new String[]{
                "分页参数用 Optional.filter(正数).orElse(默认值) 做边界保护",
                "Optional.ofNullable(list).filter(非空).map(stream...).orElse(emptyList()) 避免返回 null",
                "peek 适合调试日志，不适合修改元素或做业务逻辑",
                "sorted + map + collect 是二次加工的标准流水线"
        });
        result.put("trap", "不要在 peek 里修改对象状态或做数据库查询，它只是为了观察流中元素；过度依赖 peek 会让代码难维护。");
        return result;
    }
}
