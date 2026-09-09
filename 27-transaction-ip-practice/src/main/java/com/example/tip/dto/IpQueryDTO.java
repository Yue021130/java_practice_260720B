package com.example.tip.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * IP 归属地查询 DTO
 */
@Data
public class IpQueryDTO {

    @NotBlank(message = "IP 不能为空")
    private String ip;
}
