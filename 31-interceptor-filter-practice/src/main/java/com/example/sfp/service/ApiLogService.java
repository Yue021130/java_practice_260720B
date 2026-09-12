package com.example.sfp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sfp.dto.ApiLogVO;
import com.example.sfp.entity.ApiLog;
import com.example.sfp.mapper.ApiLogMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 审计日志 Service：由 LogInterceptor 调用写入，由 ApiLogController 查询。
 */
@Service
public class ApiLogService {

    @Resource
    private ApiLogMapper apiLogMapper;

    public void save(ApiLog apiLog) {
        apiLogMapper.insert(apiLog);
    }

    public Page<ApiLogVO> page(long current, long size, String ip, String uri, Integer statusCode, String method) {
        LambdaQueryWrapper<ApiLog> wrapper = new LambdaQueryWrapper<ApiLog>()
                .like(ip != null && !ip.isEmpty(), ApiLog::getIp, ip)
                .like(uri != null && !uri.isEmpty(), ApiLog::getUri, uri)
                .eq(statusCode != null, ApiLog::getStatusCode, statusCode)
                .eq(method != null && !method.isEmpty(), ApiLog::getMethod, method)
                .orderByDesc(ApiLog::getCreateTime);
        Page<ApiLog> entityPage = apiLogMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ApiLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    private ApiLogVO toVO(ApiLog apiLog) {
        if (apiLog == null) {
            return null;
        }
        ApiLogVO vo = new ApiLogVO();
        BeanUtils.copyProperties(apiLog, vo);
        return vo;
    }
}
