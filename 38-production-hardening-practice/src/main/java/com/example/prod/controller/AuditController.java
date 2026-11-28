package com.example.prod.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.prod.entity.AuditRecord;
import com.example.prod.mapper.AuditRecordMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计接口：供前端「加固看板」分页展示审计流水。
 *
 * <p>只需登录即可查看（不在 Service 层加 @PreAuthorize 权限点）；
 * 接口本身已被 SecurityConfig 的认证过滤器保护，未带/带失效 token 的请求进不来。</p>
 */
@Tag(name = "审计接口")
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRecordMapper auditRecordMapper;

    public AuditController(AuditRecordMapper auditRecordMapper) {
        this.auditRecordMapper = auditRecordMapper;
    }

    @Operation(summary = "审计事件分页", description = "加固看板数据源：登录/登出/订单增删改流水")
    @GetMapping("/page")
    public Page<AuditRecord> page(@RequestParam long current, @RequestParam long size) {
        return auditRecordMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<AuditRecord>().orderByDesc(AuditRecord::getId));
    }
}
