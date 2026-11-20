package com.example.epc.datascope;

import com.example.epc.common.BusinessException;
import com.example.epc.entity.Order;
import com.example.epc.mapper.OrderMapper;
import com.example.epc.permission.PermissionCache;
import org.springframework.stereotype.Component;

/**
 * 水平越权二次校验（防 IDOR）。
 *
 * <p>SQL 注入只挡得住「列表查询」的越权，挡不住「知道 id 直接点详情/
 * 改别人数据」——{@link DataScopeHandler} 不拦截 UPDATE，即使列表
 * 过滤了，攻击者拿着别人的订单 id 调 GET /api/order/{id} 照样能读。
 * 所以凡是按 id 操作单个资源的接口，必须在业务层再校验一次归属：
 * 资源不在当前用户数据范围内 → 403。</p>
 *
 * <p>代价是每次详情多一次回表查询，可接受；生产可用「先注入条件再
 * 按 id 查」（MyBatis 参数透传数据范围）省掉这次查询。</p>
 */
@Component
public class DataScopeValidator {

    private final OrderMapper orderMapper;
    private final PermissionCache permissionCache;

    public DataScopeValidator(OrderMapper orderMapper, PermissionCache permissionCache) {
        this.orderMapper = orderMapper;
        this.permissionCache = permissionCache;
    }

    /**
     * 校验订单对指定用户可见，不可见抛 403。
     *
     * <p>currentUserId 由调用方传入（Controller/Service 层从
     * StpUtil 取）而不是在内部调 StpUtil：静态上下文让单元测试
     * 无法运行，传参是本类可测的前提。</p>
     */
    public void checkOrderVisible(Long orderId, Long currentUserId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        DataScopeContext ctx = permissionCache.getDataScope(currentUserId);
        switch (ctx.getType()) {
            case ALL:
                return;
            case SELF:
                if (!order.getUserId().equals(ctx.getUserId())) {
                    throw new BusinessException("无权限访问该订单（水平越权已被拦截）");
                }
                return;
            default:
                if (ctx.getDeptIds() == null || !ctx.getDeptIds().contains(order.getDeptId())) {
                    throw new BusinessException("无权限访问该订单（水平越权已被拦截）");
                }
        }
    }
}
