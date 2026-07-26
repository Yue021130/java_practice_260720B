package com.example.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mp.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务 Mapper。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
