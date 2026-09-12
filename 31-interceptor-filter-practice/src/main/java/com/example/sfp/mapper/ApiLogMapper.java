package com.example.sfp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.sfp.entity.ApiLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper。
 */
@Mapper
public interface ApiLogMapper extends BaseMapper<ApiLog> {
}
