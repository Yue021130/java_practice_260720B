package com.example.mp.advanced;

import com.example.mp.entity.User;
import com.example.mp.mapper.UserMapper;
import com.example.mp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 高级注解场景服务。
 */
@Service
@RequiredArgsConstructor
public class AdvancedAnnotationService {

    private final UserMapper userMapper;
    private final UserService userService;

    /**
     * 逻辑删除演示：removeById 更新 deleted=1，select 自动过滤 deleted=1。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> logicDeleteDemo() {
        Map<String, Object> result = new HashMap<>();

        // 先插入一条可删除的记录
        User user = new User();
        user.setUsername("logic-delete-target");
        user.setAge(26);
        user.setEmail("logic@example.com");
        user.setStatus(1);
        userService.save(user);
        Long id = user.getId();

        // 逻辑删除
        boolean removed = userService.removeById(id);
        User afterDelete = userMapper.selectById(id);

        result.put("removed", removed);
        result.put("deletedId", id);
        result.put("afterDelete", afterDelete);
        result.put("note", "@TableLogic 让 removeById 执行 UPDATE deleted=1；select 自动加 deleted=0 条件");
        return result;
    }

    /**
     * 乐观锁演示：version 字段自动递增。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> optimisticLockDemo() {
        Map<String, Object> result = new HashMap<>();

        User before = userMapper.selectById(1004L);
        Integer oldVersion = before.getVersion();

        before.setAge(before.getAge() + 1);
        int rows = userMapper.updateById(before);
        User after = userMapper.selectById(1004L);

        result.put("updateRows", rows);
        result.put("oldVersion", oldVersion);
        result.put("newVersion", after.getVersion());
        result.put("afterUser", after);
        result.put("note", "@Version 配合乐观锁插件：update 时 WHERE version=旧值并 SET version=version+1");
        return result;
    }

    /**
     * 自动填充演示：createTime / updateTime 自动赋值。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoFillDemo() {
        Map<String, Object> result = new HashMap<>();

        User user = new User();
        user.setUsername("auto-fill-" + System.currentTimeMillis());
        user.setAge(27);
        user.setEmail("autofill@example.com");
        user.setStatus(1);
        userService.save(user);

        User inserted = userMapper.selectById(user.getId());

        result.put("insertedUser", inserted);
        result.put("createTime", inserted.getCreateTime());
        result.put("updateTime", inserted.getUpdateTime());
        result.put("note", "@TableField(fill=INSERT) 在 insert 时填充；fill=INSERT_UPDATE 在 insert/update 都填充");
        return result;
    }
}
