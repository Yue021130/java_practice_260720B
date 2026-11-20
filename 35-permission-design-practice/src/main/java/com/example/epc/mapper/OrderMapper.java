package com.example.epc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.epc.entity.Order;

/**
 * 订单表 Mapper。
 *
 * <p>注意：本 Mapper 的查询会被 datascope/DataScopeHandler 自动注入
 * 数据范围条件（dept_id / user_id 过滤）——业务层不要再手写部门过滤，
 * 写死条件会导致数据权限失效（见 OrderService.page 的注释）。</p>
 */
public interface OrderMapper extends BaseMapper<Order> {
}
