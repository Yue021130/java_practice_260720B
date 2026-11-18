package com.example.sep.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sep.common.Result;
import com.example.sep.dto.LeaveApproveDTO;
import com.example.sep.dto.LeaveSaveDTO;
import com.example.sep.dto.LeaveVO;
import com.example.sep.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假管理接口：提交、审批（通过/拒绝）、分页、详情。
 *
 * <p>提交与审批都会发布 Spring 事件，由监听器异步写通知与审计日志。</p>
 */
@Tag(name = "请假管理", description = "请假单提交、审批与查询")
@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @Operation(summary = "请假单分页", description = "按请假人、审批状态过滤，按 id 倒序")
    @GetMapping("/page")
    public Result<Page<LeaveVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String username,
                                      @RequestParam(required = false) Integer status) {
        return Result.ok(leaveService.page(current, size, username, status));
    }

    @Operation(summary = "请假单详情", description = "根据 id 查询单条请假单")
    @GetMapping("/{id}")
    public Result<LeaveVO> detail(@PathVariable Long id) {
        return Result.ok(leaveService.detail(id));
    }

    @Operation(summary = "提交请假单", description = "落库后发布 LeaveSubmittedEvent，同步监听器执行完才返回")
    @PostMapping("/submit")
    public Result<LeaveVO> submit(@Validated @RequestBody LeaveSaveDTO dto) {
        return Result.ok(leaveService.submit(dto));
    }

    @Operation(summary = "审批通过", description = "事务内更新状态并发布 LeaveApprovedEvent（approved=true）")
    @PostMapping("/approve")
    public Result<LeaveVO> approve(@Validated @RequestBody LeaveApproveDTO dto) {
        return Result.ok(leaveService.approve(dto.getId(), dto.getApprover()));
    }

    @Operation(summary = "审批拒绝", description = "事务内更新状态并发布 LeaveApprovedEvent（approved=false）")
    @PostMapping("/reject")
    public Result<LeaveVO> reject(@Validated @RequestBody LeaveApproveDTO dto) {
        return Result.ok(leaveService.reject(dto.getId(), dto.getApprover()));
    }
}
