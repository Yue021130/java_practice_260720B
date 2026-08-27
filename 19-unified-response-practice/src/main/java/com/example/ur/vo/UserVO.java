package com.example.ur.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户视图对象：返回给前端的脱敏对象。
 *
 * <p>去掉了 password 等敏感字段，体现 VO 隔离的意义。
 * 真实项目中还应做字段脱敏（如手机号 138****8001）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String name;
    private Integer age;
    private String email;
    private String phone;
}
