package com.example.mp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mp.entity.Task;

/**
 * 任务 Service 接口。
 */
public interface TaskService extends IService<Task> {

    /**
     * 创建任务：方法上标注 @AutoFillUser，由 AOP 自动填充 createBy / updateBy。
     */
    Task createTask(String content);

    /**
     * 完成任务：方法上标注 @AutoFillUser，由 AOP 自动填充 updateBy。
     */
    Task finishTask(Long id);
}
