package com.example.rbac.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.rbac.common.BusinessException;
import com.example.rbac.dto.OrderSaveDTO;
import com.example.rbac.dto.OrderVO;
import com.example.rbac.entity.BizOrder;
import com.example.rbac.entity.SysUser;
import com.example.rbac.mapper.BizOrderMapper;
import com.example.rbac.mapper.SysUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单服务：数据权限的演示载体
 *
 * <p>list 查询经过 DataPermissionInterceptor 统一注入 WHERE 条件：
 * 用户只能看到角色 data_scope 允许范围内的订单，Service 层无需手写 dept_id 条件。</p>
 */
@Service
public class OrderService {

    private final BizOrderMapper orderMapper;
    private final SysUserMapper userMapper;

    public OrderService(BizOrderMapper orderMapper, SysUserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    /**
     * 订单分页：数据权限在 MyBatis 拦截器中自动生效
     */
    public Page<OrderVO> page(long current, long size, String orderNo, String status) {
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<BizOrder>()
                .like(StringUtils.hasText(orderNo), BizOrder::getOrderNo, orderNo)
                .eq(StringUtils.hasText(status), BizOrder::getStatus, status)
                .orderByDesc(BizOrder::getId);
        Page<BizOrder> page = orderMapper.selectPage(new Page<>(current, size), wrapper);

        List<Long> userIds = page.getRecords().stream().map(BizOrder::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> nicknameMap = userIds.isEmpty() ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));

        Page<OrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setNickname(nicknameMap.get(order.getUserId()));
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 下单：归属当前登录人及其部门
     */
    public OrderVO create(OrderSaveDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        BizOrder order = new BizOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setDeptId(dto.getDeptId() != null ? dto.getDeptId() : user.getDeptId());
        order.setAmount(dto.getAmount());
        order.setStatus("PAID");
        orderMapper.insert(order);

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setNickname(user.getNickname());
        return vo;
    }

    /**
     * 订单退款：对应文档中的 order:approve-refund 高危操作场景，
     * 需要 order:refund 权限点才能调用
     */
    public void refund(Long id) {
        BizOrder order = getOrder(id);
        if ("REFUNDED".equals(order.getStatus())) {
            throw new BusinessException(400, "订单已退款，请勿重复操作");
        }
        order.setStatus("REFUNDED");
        orderMapper.updateById(order);
    }

    /**
     * 删除订单：仅 ADMIN 角色持有的 order:delete 权限点可调用
     */
    public void remove(Long id) {
        orderMapper.deleteById(id);
    }

    private BizOrder getOrder(Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "NO" + time + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
