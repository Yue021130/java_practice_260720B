package com.example.ccp.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户新增/编辑入参；id 为空表示新增，非空表示编辑。
 */
@Data
public class UserSaveDTO implements Serializable {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须为 3~20 位")
    private String username;

    /** 新增时必填，编辑时留空表示不修改密码 */
    @Size(min = 6, max = 20, message = "密码长度须为 6~20 位")
    private String password;

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 状态：1 正常，0 禁用 */
    private Integer status;
}
