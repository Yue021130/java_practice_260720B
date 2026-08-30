package com.example.eep.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * 商品实体，演示 EasyExcel 自定义 Converter 导出。
 */
@Data
@Entity
@Table(name = "t_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String typeName;

    /** 是否需要额外扣费：1 是 / 0 否。 */
    private Integer needPay;

    private BigDecimal price;

    /** 是否默认项：1 是 / 0 否。 */
    private Integer isDefault;

    @Column(length = 64)
    private String loungeCode;
}
