package com.example.mp.batch;

import com.example.mp.entity.User;
import com.example.mp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量操作场景服务。
 */
@Service
@RequiredArgsConstructor
public class BatchOpsService {

    private final UserService userService;

    /**
     * saveBatch 演示。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveBatchDemo() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setUsername("batch-" + i + "-" + System.currentTimeMillis());
            user.setAge(20 + i);
            user.setEmail("batch" + i + "@example.com");
            user.setStatus(1);
            users.add(user);
        }

        boolean success = userService.saveBatch(users, 2);

        List<Long> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.getId());
        }

        result.put("saveBatchSuccess", success);
        result.put("batchSize", 2);
        result.put("insertCount", users.size());
        result.put("filledIds", ids);
        result.put("note", "saveBatch 第二个参数 batchSize 控制每批提交数量，降低数据库往返");
        return result;
    }

    /**
     * updateBatchById 演示。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateBatchDemo() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = new ArrayList<>();
        for (long id = 1001L; id <= 1003L; id++) {
            User user = new User();
            user.setId(id);
            user.setEmail("batch-update-" + id + "@example.com");
            users.add(user);
        }

        boolean success = userService.updateBatchById(users, 2);

        result.put("updateBatchSuccess", success);
        result.put("batchSize", 2);
        result.put("updateCount", users.size());
        result.put("note", "updateBatchById 按 id 批量更新；null 字段默认不更新");
        return result;
    }
}
