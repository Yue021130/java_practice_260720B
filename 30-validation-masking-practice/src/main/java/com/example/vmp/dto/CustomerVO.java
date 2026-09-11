package com.example.vmp.dto;

import com.example.vmp.masking.Sensitive;
import com.example.vmp.masking.SensitiveType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户出参 VO：敏感字段通过 @Sensitive 自动脱敏。
 */
@Data
public class CustomerVO implements Serializable {

    private Long id;

    @Sensitive(SensitiveType.NAME)
    private String name;

    @Sensitive(SensitiveType.PHONE)
    private String phone;

    @Sensitive(SensitiveType.ID_CARD)
    private String idCard;

    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.ADDRESS)
    private String address;

    private Integer gender;

    private Integer level;

    private String remark;

    private LocalDateTime createTime;
}
