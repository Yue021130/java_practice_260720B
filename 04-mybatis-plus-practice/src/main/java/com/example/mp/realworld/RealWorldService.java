package com.example.mp.realworld;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mp.entity.User;
import com.example.mp.mapper.OrderMapper;
import com.example.mp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合实战场景服务。
 */
@Service
@RequiredArgsConstructor
public class RealWorldService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    /**
     * 用户订单统计：自定义 SQL 关联查询。
     */
    public Map<String, Object> userOrderStats() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> stats = userMapper.selectUserOrderStats();
        result.put("stats", stats);
        result.put("note", "复杂关联统计可以写自定义 SQL，简单单表统计也可以用 Wrapper + groupBy");
        return result;
    }

    /**
     * 按状态分组统计人数与平均年龄。
     */
    public Map<String, Object> statusStats() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> stats = userMapper.selectStatusStats();
        result.put("stats", stats);
        result.put("note", "@Select 自定义 SQL 适合聚合查询；生产上复杂统计也可用 XML 或 @Select");
        return result;
    }

    /**
     * 综合搜索分页：条件 + 分页 + 排序。
     */
    public Map<String, Object> searchPage() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("username", "张")
                .eq("status", 1)
                .between("age", 18, 40)
                .orderByDesc("create_time");

        Page<User> page = new Page<>(1, 5);
        Page<User> pageResult = userMapper.selectPage(page, wrapper);

        result.put("condition", "username LIKE '%张%' AND status=1 AND age BETWEEN 18 AND 40");
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("records", pageResult.getRecords());
        result.put("note", "生产常见列表接口：多个筛选条件 + 分页 + 排序");
        return result;
    }
}
