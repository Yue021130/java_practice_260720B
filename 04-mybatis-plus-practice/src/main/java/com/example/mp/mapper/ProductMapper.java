package com.example.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mp.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper：继承 BaseMapper 获得基础 CRUD。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
