package com.example.mp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mp.annotation.AutoFillUser;
import com.example.mp.entity.Task;
import com.example.mp.mapper.TaskMapper;
import com.example.mp.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务 Service 实现。
 */
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Override
    @AutoFillUser("admin")
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(String content) {
        Task task = new Task();
        task.setContent(content);
        task.setStatus(0);
        // createBy / updateBy / createTime / updateTime 由 AOP 自动填充
        this.save(task);
        return task;
    }

    @Override
    @AutoFillUser("admin")
    @Transactional(rollbackFor = Exception.class)
    public Task finishTask(Long id) {
        Task task = new Task();
        task.setId(id);
        task.setStatus(1);
        // updateBy / updateTime 由 AOP 自动填充
        this.updateById(task);
        return this.getById(id);
    }
}
