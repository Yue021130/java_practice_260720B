package com.example.fcp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fcp.common.Result;
import com.example.fcp.dto.NoticeSaveDTO;
import com.example.fcp.dto.NoticeVO;
import com.example.fcp.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告管理接口：CRUD，给过滤器链提供真实业务流量。
 *
 * <p>公告正文的写操作会触发 XssFilter 对请求体的转义清洗，
 * 所有请求都会经过 TimingFilter 落审计日志。</p>
 */
@Tag(name = "公告管理", description = "公告 CRUD")
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "分页列表")
    @GetMapping("/page")
    public Result<Page<NoticeVO>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String title,
                                       @RequestParam(required = false) Integer type,
                                       @RequestParam(required = false) Integer status) {
        return Result.ok(noticeService.page(current, size, title, type, status));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<NoticeVO> detail(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    public Result<NoticeVO> save(@Validated @RequestBody NoticeSaveDTO dto) {
        return Result.ok(noticeService.save(dto));
    }

    @Operation(summary = "编辑公告")
    @PutMapping
    public Result<NoticeVO> update(@Validated @RequestBody NoticeSaveDTO dto) {
        return Result.ok(noticeService.update(dto));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
