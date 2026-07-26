package com.example.mp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mp.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IService CRUD 场景服务。
 */
@Service
@RequiredArgsConstructor
public class ServiceCrudService {

    private final UserService userService;

    /**
     * save 演示：IService.save 与 BaseMapper.insert 类似，但属于 Service 层。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveDemo() {
        Map<String, Object> result = new HashMap<>();

        User user = new User();
        user.setUsername("service-save-" + UUID.randomUUID().toString().substring(0, 6));
        user.setAge(23);
        user.setEmail("service@example.com");
        user.setStatus(1);

        boolean success = userService.save(user);

        result.put("saveSuccess", success);
        result.put("filledId", user.getId());
        result.put("note", "IService.save 返回 boolean；主键同样会回填到实体");
        return result;
    }

    /**
     * saveOrUpdate 演示：有主键则 update，无主键则 insert。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveOrUpdateDemo() {
        Map<String, Object> result = new HashMap<>();

        // 1) 无主键 -> insert
        User newUser = new User();
        newUser.setUsername("save-or-update-new");
        newUser.setAge(24);
        newUser.setEmail("new@example.com");
        newUser.setStatus(1);
        boolean inserted = userService.saveOrUpdate(newUser);

        // 2) 有主键 -> update
        User existUser = new User();
        existUser.setId(1003L);
        existUser.setUsername("save-or-update-exist");
        existUser.setAge(36);
        boolean updated = userService.saveOrUpdate(existUser);

        result.put("inserted", inserted);
        result.put("newUserId", newUser.getId());
        result.put("updated", updated);
        result.put("updatedUser", userService.getById(1003L));
        result.put("note", "saveOrUpdate 根据实体是否有 id 判存：无 id insert，有 id update");
        return result;
    }

    /**
     * list 演示：查询全部未逻辑删除的用户。
     */
    public Map<String, Object> listDemo() {
        Map<String, Object> result = new HashMap<>();
        result.put("userList", userService.list());
        result.put("count", userService.count());
        result.put("note", "IService.list() 查询全表；IService.count() 返回总数；逻辑删除会自动过滤");
        return result;
    }

    /**
     * page 演示：分页查询。
     */
    public Map<String, Object> pageDemo() {
        Map<String, Object> result = new HashMap<>();
        Page<User> page = new Page<>(1, 3);
        Page<User> pageResult = userService.page(page);

        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("records", pageResult.getRecords());
        result.put("note", "IService.page(Page<T>) 需要配置分页插件才生效");
        return result;
    }
}
