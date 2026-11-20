package com.example.epc.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.epc.common.BusinessException;
import com.example.epc.datascope.DataScopeValidator;
import com.example.epc.dto.OrderVO;
import com.example.epc.entity.Dept;
import com.example.epc.entity.Order;
import com.example.epc.entity.User;
import com.example.epc.mapper.DeptMapper;
import com.example.epc.mapper.OrderMapper;
import com.example.epc.mapper.UserMapper;
import com.example.epc.oplog.OpLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单 Service：本章数据权限的正反两面教材。
 *
 * <p>正面：page 列表只写业务条件（orderNo 模糊查询），部门/本人过滤
 * 完全交给 DataPermissionInterceptor 自动注入——业务代码零侵入；
 * detail/update 因为拦截器只管 SELECT，额外用 {@link DataScopeValidator}
 * 做按 id 的归属二次校验，防 IDOR 水平越权。</p>
 *
 * <p>反面：{@link #insecureDetail} 故意不做归属校验，留着演示
 * 「有功能权限 ≠ 有数据权限」的水平越权漏洞。</p>
 */
@Slf4j
@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private DataScopeValidator dataScopeValidator;

    /**
     * 分页查询。
     *
     * <p>【重点】这里千万不要加 deptId 过滤条件！数据权限由
     * DataPermissionInterceptor 在执行时按当前登录用户的数据范围
     * 自动注入 WHERE（ALL 不注入 / SELF 注入 user_id = 本人 /
     * 其余注入 dept_id IN 可见部门）。业务层一旦手写部门条件，
     * 就是绕过/破坏数据权限体系。</p>
     */
    public Page<OrderVO> page(long current, long size, String orderNo) {
        Page<Order> page = orderMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Order>()
                        .like(StringUtils.hasText(orderNo), Order::getOrderNo, orderNo)
                        .orderByDesc(Order::getCreateTime));
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVOs(page.getRecords()));
        return voPage;
    }

    /**
     * 订单详情：先过数据归属校验（DataScopeValidator 不在范围直接抛 403），
     * 再返回数据——防「拿着别人订单 id 直接调详情」的 IDOR。
     */
    @OpLog(value = "订单详情", perm = "order:detail")
    public OrderVO detail(Long id) {
        dataScopeValidator.checkOrderVisible(id, StpUtil.getLoginIdAsLong());
        Order order = orderMapper.selectById(id);
        List<OrderVO> vos = toVOs(java.util.Collections.singletonList(order));
        return vos.get(0);
    }

    /**
     * 修改订单：同属一个 owner 的资源操作前同样要先校验归属。
     * 拦截器不拦 UPDATE 语句（见 DataScopeHandler 注释），
     * 没有这道校验，知道 id 就能改别人订单。
     */
    @OpLog(value = "修改订单", perm = "order:update")
    public void update(Order order) {
        if (order == null || order.getId() == null) {
            throw new BusinessException("订单 id 不能为空");
        }
        if (orderMapper.selectById(order.getId()) == null) {
            throw new BusinessException("订单不存在");
        }
        dataScopeValidator.checkOrderVisible(order.getId(), StpUtil.getLoginIdAsLong());
        // 只允许改金额与状态，归属（deptId/userId）不允许通过本接口变更
        Order update = new Order();
        update.setId(order.getId());
        update.setAmount(order.getAmount());
        update.setStatus(order.getStatus());
        orderMapper.updateById(update);
    }

    /**
     * ⚠️ 反面教材，故意留的漏洞！⚠️
     *
     * <p>只校验功能权限（order:list），不做任何数据归属校验——
     * zhangsan（SELF 数据范围）拿 admin 的订单 id 调本接口也能查到。
     * 用来对照 detail 演示水平越权的真实后果。【生产严禁】复制
     * 这种写法，按 id 操作单资源的接口必须过 DataScopeValidator。</p>
     */
    @SaCheckPermission("order:list")
    public OrderVO insecureDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return toVOs(java.util.Collections.singletonList(order)).get(0);
    }

    /** 批量转 VO 并填充部门名 / 用户名 */
    private List<OrderVO> toVOs(List<Order> orders) {
        List<Long> deptIds = orders.stream().map(Order::getDeptId).distinct().collect(Collectors.toList());
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
        Map<Long, String> deptNames = deptIds.isEmpty() ? java.util.Collections.emptyMap()
                : deptMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getName, (a, b) -> a));
        Map<Long, String> usernames = userIds.isEmpty() ? java.util.Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));
        return orders.stream().map(o -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(o, vo);
            vo.setDeptName(deptNames.get(o.getDeptId()));
            vo.setUsername(usernames.get(o.getUserId()));
            return vo;
        }).collect(Collectors.toList());
    }
}
