package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户实体：模拟 CRM / 电商系统中的会员信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserLevel level;
    private List<String> tags;

    /**
     * 用户等级：演示 Optional.filter 的过滤条件。
     */
    public enum UserLevel {
        VIP,
        NORMAL,
        GUEST
    }
}
