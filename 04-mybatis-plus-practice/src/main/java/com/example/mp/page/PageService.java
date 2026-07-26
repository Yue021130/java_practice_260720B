package com.example.mp.page;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mp.entity.User;
import com.example.mp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 分页场景服务。
 */
@Service
@RequiredArgsConstructor
public class PageService {

    private final UserMapper userMapper;

    /**
     * 基础分页：Page<T> + selectPage。
     */
    public Map<String, Object> basicDemo() {
        Map<String, Object> result = new HashMap<>();

        Page<User> page = new Page<>(1, 3);
        Page<User> pageResult = userMapper.selectPage(page, null);

        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("records", pageResult.getRecords());
        result.put("note", "分页插件 PaginationInnerInterceptor 必须配置，否则 total 为 0");
        return result;
    }

    /**
     * 分页 + Wrapper。
     */
    public Map<String, Object> customDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).ge("age", 20).orderByAsc("age");

        Page<User> page = new Page<>(1, 2);
        Page<User> pageResult = userMapper.selectPage(page, wrapper);

        result.put("condition", "status = 1 AND age >= 20");
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("records", pageResult.getRecords());
        result.put("note", "分页与 Wrapper 条件同时生效，SQL 会先 WHERE 再 LIMIT");
        return result;
    }
}
