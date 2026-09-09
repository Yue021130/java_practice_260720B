package com.example.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rbac.common.Result;
import com.example.rbac.dto.OrderSaveDTO;
import com.example.rbac.dto.OrderVO;
import com.example.rbac.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口：功能权限（权限点）+ 数据权限（拦截器过滤）双重演示
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单分页", description = "自动按当前用户的数据权限范围过滤：本人/本部门/全部")
    @SaCheckPermission("order:list")
    @GetMapping("/page")
    public Result<Page<OrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String orderNo,
                                      @RequestParam(required = false) String status) {
        return Result.ok(orderService.page(current, size, orderNo, status));
    }

    @Operation(summary = "下单")
    @SaCheckPermission("order:add")
    @PostMapping
    public Result<OrderVO> create(@Validated @RequestBody OrderSaveDTO dto) {
        return Result.ok(orderService.create(dto));
    }

    @Operation(summary = "订单退款", description = "高危操作，对应文档 order:approve-refund 场景")
    @SaCheckPermission("order:refund")
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        orderService.refund(id);
        return Result.ok();
    }

    @Operation(summary = "删除订单", description = "仅持有 order:delete 权限点的角色可用")
    @SaCheckPermission("order:delete")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        orderService.remove(id);
        return Result.ok();
    }
}
