package com.example.tip.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * IP 归属地 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpRegionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ip;

    /** 国家 */
    private String country;

    /** 省/州 */
    private String province;

    /** 市 */
    private String city;

    /** 运营商/组织 */
    private String isp;

    /** 完整地址 */
    private String fullAddress;
}
