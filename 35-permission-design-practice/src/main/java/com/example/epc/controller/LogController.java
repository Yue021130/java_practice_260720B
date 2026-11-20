package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.Result;
import com.example.epc.entity.LoginLog;
import com.example.epc.entity.OpLog;
import com.example.epc.mapper.LoginLogMapper;
import com.example.epc.mapper.OpLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志查询接口：操作审计日志 + 登录日志。
 * 写入由机制兜底（@OpLog AOP / 登录流程内记录），这里只读。
 */
@Tag(name = "日志管理", description = "操作日志与登录日志查询")
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final OpLogMapper opLogMapper;
    private final LoginLogMapper loginLogMapper;

    public LogController(OpLogMapper opLogMapper, LoginLogMapper loginLogMapper) {
        this.opLogMapper = opLogMapper;
        this.loginLogMapper = loginLogMapper;
    }

    @SaCheckPermission("log:list")
    @Operation(summary = "操作日志分页", description = "@OpLog AOP 自动落库的审计记录")
    @GetMapping("/op/page")
    public Result<Page<OpLog>> opPage(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(opLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<OpLog>().orderByDesc(OpLog::getCreateTime)));
    }

    @SaCheckPermission("log:list")
    @Operation(summary = "登录日志分页", description = "登录成功/失败都会记录（含失败原因）")
    @GetMapping("/login/page")
    public Result<Page<LoginLog>> loginPage(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(loginLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getCreateTime)));
    }
}
