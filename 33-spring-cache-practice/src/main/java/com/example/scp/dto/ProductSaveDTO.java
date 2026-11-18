package com.example.scp.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 商品新增 / 修改入参：id 为空走新增，非空走修改。
 *
 * <p>校验注解在 Controller 层 @Validated 触发，失败由全局异常处理
 * 统一转成 Result.fail(400, ...) 返回。</p>
 */
@Data
public class ProductSaveDTO {

    /** 商品 id：新增时为空，修改时必填（路径与缓存 key 都依赖它） */
    private Long id;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 商品分类 */
    @NotBlank(message = "商品分类不能为空")
    private String category;

    /** 价格：最低 0.01，不允许 0 元或负数商品 */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格不能低于 0.01")
    private BigDecimal price;

    /** 库存：不允许负数 */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /** 状态：1 上架，0 下架，缺省由后端补默认值 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}
