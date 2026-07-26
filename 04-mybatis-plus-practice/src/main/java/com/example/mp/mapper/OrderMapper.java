package com.example.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mp.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper：继承 BaseMapper 获得基础 CRUD。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
