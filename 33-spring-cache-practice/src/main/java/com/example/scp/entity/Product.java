package com.example.scp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，对应表 biz_product。
 *
 * <p>本章缓存演示的业务载体：详情缓存（product）、分页缓存（productPage）
 * 都以它为数据来源，配合逻辑删除字段演示 @CacheEvict 的失效策略。</p>
 */
@Data
@TableName("biz_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品分类（手机 / 电脑 / 耳机 等） */
    private String category;

    /** 价格 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 状态：1 上架，0 下架 */
    private Integer status;

    /** 排序号，越小越靠前 */
    private Integer sortOrder;

    /** 逻辑删除：0 未删，1 已删（MyBatis-Plus 自动过滤已删数据） */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
