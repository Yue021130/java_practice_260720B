package com.example.sep.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.sep.common.Result;
import com.example.sep.dto.AuditLogVO;
import com.example.sep.dto.EventRecordVO;
import com.example.sep.dto.NotifyVO;
import com.example.sep.service.EventCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 事件中心接口：事件流水、站内通知、审计日志。
 *
 * <p>配合事件监听器演示"事件可追溯"：每条事件记录都能看到被哪些监听器消费过。</p>
 */
@Tag(name = "事件中心", description = "事件流水、站内通知、审计日志查询")
@RestController
@RequestMapping("/api/event")
public class EventCenterController {

    private final EventCenterService eventCenterService;

    public EventCenterController(EventCenterService eventCenterService) {
        this.eventCenterService = eventCenterService;
    }

    @Operation(summary = "事件流水分页", description = "查 event_record，含每条事件的消费状态")
    @GetMapping("/page")
    public Result<Page<EventRecordVO>> eventPage(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(eventCenterService.eventPage(current, size));
    }

    @Operation(summary = "事件详情", description = "单条事件记录，含 handledBy（消费它的监听器名列表）")
    @GetMapping("/{id}")
    public Result<EventRecordVO> eventDetail(@PathVariable Long id) {
        return Result.ok(eventCenterService.eventDetail(id));
    }

    @Operation(summary = "站内通知分页", description = "可按接收人过滤")
    @GetMapping("/notify/page")
    public Result<Page<NotifyVO>> notifyPage(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String receiver) {
        return Result.ok(eventCenterService.notifyPage(current, size, receiver));
    }

    @Operation(summary = "审计日志分页", description = "由事务提交后的监听器写入的动作流水")
    @GetMapping("/audit/page")
    public Result<Page<AuditLogVO>> auditPage(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(eventCenterService.auditPage(current, size));
    }
}
