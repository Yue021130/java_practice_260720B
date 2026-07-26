package com.example.mp.mapper;

import com.example.mp.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BaseMapper CRUD 场景服务。
 */
@Service
@RequiredArgsConstructor
public class MapperCrudService {

    private final UserMapper userMapper;

    /**
     * insert 演示：MP 会把生成的主键回填到实体对象中。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertDemo() {
        Map<String, Object> result = new HashMap<>();

        User user = new User();
        user.setUsername("mapper-insert-" + UUID.randomUUID().toString().substring(0, 6));
        user.setAge(20);
        user.setEmail("mapper@example.com");
        user.setStatus(1);

        int rows = userMapper.insert(user);

        result.put("insertRows", rows);
        result.put("filledId", user.getId());
        result.put("note", "BaseMapper.insert 执行后，雪花 ID 会自动回填到实体 id 字段");
        return result;
    }

    /**
     * selectById 演示。
     */
    public Map<String, Object> selectByIdDemo() {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectById(1001L);
        result.put("queryById", 1001L);
        result.put("user", user);
        result.put("note", "BaseMapper.selectById 根据主键查询单条");
        return result;
    }

    /**
     * updateById 演示：只更新非空字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateByIdDemo() {
        Map<String, Object> result = new HashMap<>();

        // 先查询再修改，避免覆盖其他字段
        User user = userMapper.selectById(1002L);
        String originalEmail = user.getEmail();
        user.setEmail("updated-" + System.currentTimeMillis() + "@example.com");

        int rows = userMapper.updateById(user);
        User updated = userMapper.selectById(1002L);

        result.put("updateRows", rows);
        result.put("originalEmail", originalEmail);
        result.put("updatedEmail", updated.getEmail());
        result.put("note", "updateById 根据 id 更新非空字段");
        return result;
    }

    /**
     * deleteById 演示：物理删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteByIdDemo() {
        Map<String, Object> result = new HashMap<>();

        // 先插入一条用于删除的临时数据
        User user = new User();
        user.setUsername("to-delete-" + System.currentTimeMillis());
        user.setAge(25);
        user.setEmail("delete@example.com");
        user.setStatus(1);
        userMapper.insert(user);

        Long id = user.getId();
        int rows = userMapper.deleteById(id);
        User afterDelete = userMapper.selectById(id);

        result.put("deleteRows", rows);
        result.put("deletedId", id);
        result.put("afterDelete", afterDelete);
        result.put("note", "deleteById 是物理删除；逻辑删除需要 @TableLogic + removeById");
        return result;
    }
}
