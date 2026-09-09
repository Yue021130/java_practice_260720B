package com.example.tip.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 转账请求 DTO
 */
@Data
public class TransferDTO {

    @NotNull(message = "转出账户 ID 不能为空")
    private Long fromId;

    @NotNull(message = "转入账户 ID 不能为空")
    private Long toId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;
}
