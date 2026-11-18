package com.example.scp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品出参：从 Product 拷贝而来，与数据库实体解耦，避免泄露 deleted 等内部字段。
 */
@Data
public class ProductVO {

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    /** 状态：1 上架，0 下架 */
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
