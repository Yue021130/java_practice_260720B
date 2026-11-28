package com.example.prod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.prod.dto.OrderSaveDTO;
import com.example.prod.dto.OrderVO;
import com.example.prod.entity.Order;
import com.example.prod.entity.User;
import com.example.prod.mapper.OrderMapper;
import com.example.prod.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单服务：方法级权限（@PreAuthorize）+ 审计的演示载体
 *
 * <p>权限注解打在 Service 方法上而非 Controller：所有调用方（Controller、
 * 定时任务、消息监听、测试代码）都必须经过 Service 代理，Controller 绕不过权限校验。</p>
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final AuditEventRepository auditEventRepository;

    public OrderService(OrderMapper orderMapper,
                        UserMapper userMapper,
                        AuditEventRepository auditEventRepository) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * 订单分页查询
     */
    @PreAuthorize("hasAuthority('order:read')")
    public Page<OrderVO> page(long current, long size, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                .orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(current, size), wrapper);

        // 批量查下单人用户名，避免在 stream 里逐条 selectById 造成 N+1
        List<Long> userIds = page.getRecords().stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        Page<OrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setUsername(usernameMap.get(order.getUserId()));
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * 订单详情
     */
    @PreAuthorize("hasAuthority('order:read')")
    public OrderVO detail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        // 单条详情，直接查一次即可，无 N+1 问题
        if (order.getUserId() != null) {
            User user = userMapper.selectById(order.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
        }
        return vo;
    }

    /**
     * 下单：userId 由当前登录用户名反查，记审计
     */
    @PreAuthorize("hasAuthority('order:write')")
    public OrderVO save(OrderSaveDTO dto, String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        Order order = new Order();
        order.setOrderNo(dto.getOrderNo());
        order.setAmount(dto.getAmount());
        order.setUserId(user.getId());
        // 演示数据：User 实体没有 deptId 字段，生产环境应取用户所属部门
        order.setDeptId(1L);
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        auditEventRepository.add(new AuditEvent(username, "ORDER_CREATE",
                Collections.<String, Object>singletonMap("orderNo", dto.getOrderNo())));

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setUsername(user.getUsername());
        return vo;
    }

    /**
     * 改单：DTO 无 id 字段，按订单号（业务键）定位订单
     */
    @PreAuthorize("hasAuthority('order:write')")
    public OrderVO update(OrderSaveDTO dto, String username) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, dto.getOrderNo()));
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        order.setAmount(dto.getAmount());
        if (dto.getStatus() != null) {
            order.setStatus(dto.getStatus());
        }
        orderMapper.updateById(order);

        auditEventRepository.add(new AuditEvent(username, "ORDER_UPDATE",
                Collections.<String, Object>singletonMap("orderNo", dto.getOrderNo())));

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        if (order.getUserId() != null) {
            User user = userMapper.selectById(order.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
        }
        return vo;
    }

    /**
     * 删单：物理删除（演示），记审计
     */
    @PreAuthorize("hasAuthority('order:write')")
    public void delete(Long id, String username) {
        orderMapper.deleteById(id);
        auditEventRepository.add(new AuditEvent(username, "ORDER_DELETE",
                Collections.<String, Object>singletonMap("id", id)));
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
