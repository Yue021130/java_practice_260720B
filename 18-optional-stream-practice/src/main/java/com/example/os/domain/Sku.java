package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SKU 实体：演示 Optional + Stream 取最优价。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sku {

    private Long id;
    private Long productId;
    private String skuCode;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private Boolean enabled;
}
