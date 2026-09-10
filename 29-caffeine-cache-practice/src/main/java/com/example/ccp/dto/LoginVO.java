package com.example.ccp.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功出参：JWT token + 用户基本信息。
 */
@Data
public class LoginVO implements Serializable {

    /** JWT Token，前端存入 Pinia 并随每次请求放入 Authorization 请求头 */
    private String token;

    private Long id;

    private String username;

    private String nickname;
}
