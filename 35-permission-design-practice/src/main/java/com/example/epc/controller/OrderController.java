package com.example.epc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.Result;
import com.example.epc.dto.OrderVO;
import com.example.epc.entity.Order;
import com.example.epc.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口：数据权限的演示场。
 *
 * <p>列表由拦截器自动按数据范围过滤；详情/修改在业务层做归属二次校验；
 * /insecure/{id} 是故意留的水平越权反面教材，见 OrderService.insecureDetail。</p>
 */
@Tag(name = "订单管理", description = "订单分页/详情/修改（数据权限演示）")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @SaCheckPermission("order:list")
    @Operation(summary = "订单分页", description = "数据范围由拦截器自动注入过滤条件")
    @GetMapping("/page")
    public Result<Page<OrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String orderNo) {
        return Result.ok(orderService.page(current, size, orderNo));
    }

    @SaCheckPermission("order:detail")
    @Operation(summary = "订单详情", description = "先校验数据归属（DataScopeValidator），防 IDOR")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @SaCheckPermission("order:update")
    @Operation(summary = "修改订单", description = "改金额/状态；改前同样校验数据归属")
    @PutMapping
    public Result<Void> update(@RequestBody Order order) {
        orderService.update(order);
        return Result.ok();
    }

    @Operation(summary = "⚠️ 不安全详情（反面教材）", description = "不做数据归属校验，演示水平越权，生产严禁")
    @GetMapping("/insecure/{id}")
    public Result<OrderVO> insecureDetail(@PathVariable Long id) {
        return Result.ok(orderService.insecureDetail(id));
    }
}
