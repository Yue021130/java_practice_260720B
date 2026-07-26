package com.example.mp.entity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体类注解场景服务。
 */
@Service
@RequiredArgsConstructor
public class EntityAnnotationService {

    private final UserMapper userMapper;

    /**
     * @TableName 演示：User 类通过 @TableName("t_user") 映射到 t_user 表。
     */
    public Map<String, Object> tableNameDemo() {
        Map<String, Object> result = new HashMap<>();
        result.put("entityClass", "User");
        result.put("tableName", "t_user");
        result.put("annotation", "@TableName(\"t_user\")");
        result.put("description", "类名与表名不一致时，使用 @TableName 显式指定");
        result.put("userCount", userMapper.selectCount(new QueryWrapper<User>().eq("deleted", 0)));
        return result;
    }

    /**
     * @TableId 演示：ASSIGN_ID 雪花 ID vs AUTO 数据库自增。
     */
    public Map<String, Object> tableIdDemo() {
        Map<String, Object> result = new HashMap<>();
        result.put("primaryKeyField", "id");
        result.put("currentStrategy", "ASSIGN_ID");
        result.put("snowflakeIdExample", String.valueOf(System.currentTimeMillis()));
        result.put("note", "ASSIGN_ID 生成全局唯一 Long 型 ID；AUTO 依赖数据库自增");
        result.put("sampleUser", userMapper.selectById(1001L));
        return result;
    }

    /**
     * @TableField 演示：字段映射、排除非持久化字段。
     */
    public Map<String, Object> tableFieldDemo() {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectById(1001L);
        user.setRemark("这是非持久化备注，不会写入数据库");

        result.put("sampleUser", user);
        result.put("remarkField", "remark");
        result.put("remarkPersisted", false);
        result.put("note", "@TableField(exist = false) 标记的字段不参与 SQL");
        return result;
    }
}
