package com.example.prod.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.prod.dto.OrderSaveDTO;
import com.example.prod.dto.OrderVO;
import com.example.prod.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口：REST 风格，直接返回 VO / "ok"，不包 Result；
 * 统一错误格式（400/401/403）由 GlobalExceptionHandler 负责。
 *
 * <p>注意：这里故意不加权限注解——权限在 Service 方法上（@PreAuthorize），
 * Controller 只是薄薄一层转发，绕不过 Service 代理。</p>
 */
@Tag(name = "订单接口")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单分页")
    @GetMapping("/page")
    public Page<OrderVO> page(@RequestParam long current,
                              @RequestParam long size,
                              @RequestParam(required = false) String orderNo) {
        return orderService.page(current, size, orderNo);
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public OrderVO detail(@PathVariable Long id) {
        return orderService.detail(id);
    }

    @Operation(summary = "下单")
    @PostMapping
    public OrderVO save(@Validated @RequestBody OrderSaveDTO dto) {
        return orderService.save(dto, currentUsername());
    }

    @Operation(summary = "改单", description = "按订单号定位，更新金额/状态")
    @PutMapping
    public OrderVO update(@Validated @RequestBody OrderSaveDTO dto) {
        return orderService.update(dto, currentUsername());
    }

    @Operation(summary = "删单")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        orderService.delete(id, currentUsername());
        return "ok";
    }

    /**
     * 当前登录用户名：JWT 过滤器已在 SecurityContext 中放好 Authentication
     */
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
