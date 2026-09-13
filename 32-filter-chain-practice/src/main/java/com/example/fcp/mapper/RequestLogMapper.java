package com.example.fcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fcp.entity.RequestLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请求审计日志 Mapper。
 */
@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLog> {
}
