package com.example.mp.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mp.entity.User;
import com.example.mp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件构造器场景服务。
 */
@Service
@RequiredArgsConstructor
public class WrapperQueryService {

    private final UserMapper userMapper;

    /**
     * eq + like 演示。
     */
    public Map<String, Object> eqLikeDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> eqWrapper = new QueryWrapper<>();
        eqWrapper.eq("status", 1).like("email", "example.com");
        List<User> activeUsers = userMapper.selectList(eqWrapper);

        result.put("condition", "status = 1 AND email LIKE '%example.com%'");
        result.put("activeUsers", activeUsers);
        result.put("note", "eq 等值匹配；like 模糊匹配（% 可加在两侧 / 前 / 后）");
        return result;
    }

    /**
     * between + orderBy 演示。
     */
    public Map<String, Object> betweenOrderDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.between("age", 20, 30)
                .orderByAsc("age")
                .orderByDesc("create_time");
        List<User> users = userMapper.selectList(wrapper);

        result.put("condition", "age BETWEEN 20 AND 30，按 age 升序、create_time 降序");
        result.put("users", users);
        result.put("note", "between 包含边界；orderByAsc / orderByDesc 支持多字段排序");
        return result;
    }

    /**
     * LambdaQueryWrapper 演示：通过方法引用避免字段名字符串。
     */
    public Map<String, Object> lambdaDemo() {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<User> lambda = new LambdaQueryWrapper<>();
        lambda.ge(User::getAge, 25)
                .likeRight(User::getEmail, "li")
                .or()
                .eq(User::getStatus, 0);
        List<User> users = userMapper.selectList(lambda);

        result.put("condition", "age >= 25 AND email LIKE 'li%' OR status = 0");
        result.put("users", users);
        result.put("note", "LambdaQueryWrapper 使用方法引用，字段改名时编译期即可发现");
        return result;
    }

    /**
     * 嵌套 and / or 条件演示。
     */
    public Map<String, Object> nestedDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.nested(w -> w.eq("status", 1).between("age", 20, 35))
                .like("username", "张")
                .or()
                .gt("age", 40);
        List<User> users = userMapper.selectList(wrapper);

        result.put("sqlWhere", "(status = 1 AND age BETWEEN 20 AND 35) AND username LIKE '%张%' OR age > 40");
        result.put("users", users);
        result.put("note", "nested 用于显式控制 and / or 优先级；默认 and 优先级高于 or");
        return result;
    }
}
