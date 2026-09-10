package com.example.ccp.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户出参：剔除 password 字段，保证敏感信息不外泄。
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
