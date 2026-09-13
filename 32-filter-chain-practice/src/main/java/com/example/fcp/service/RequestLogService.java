package com.example.fcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fcp.dto.RequestLogVO;
import com.example.fcp.entity.RequestLog;
import com.example.fcp.mapper.RequestLogMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 请求审计日志 Service：由 TimingFilter 调用写入，由 RequestLogController 查询。
 */
@Service
public class RequestLogService {

    @Resource
    private RequestLogMapper requestLogMapper;

    public void save(RequestLog requestLog) {
        requestLogMapper.insert(requestLog);
    }

    public Page<RequestLogVO> page(long current, long size, String traceId, String uri, Integer statusCode) {
        LambdaQueryWrapper<RequestLog> wrapper = new LambdaQueryWrapper<RequestLog>()
                .like(traceId != null && !traceId.isEmpty(), RequestLog::getTraceId, traceId)
                .like(uri != null && !uri.isEmpty(), RequestLog::getUri, uri)
                .eq(statusCode != null, RequestLog::getStatusCode, statusCode)
                .orderByDesc(RequestLog::getCreateTime);
        Page<RequestLog> entityPage = requestLogMapper.selectPage(new Page<>(current, size), wrapper);
        Page<RequestLogVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    private RequestLogVO toVO(RequestLog requestLog) {
        if (requestLog == null) {
            return null;
        }
        RequestLogVO vo = new RequestLogVO();
        BeanUtils.copyProperties(requestLog, vo);
        return vo;
    }
}
