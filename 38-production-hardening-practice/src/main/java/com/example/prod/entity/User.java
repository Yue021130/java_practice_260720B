package com.example.prod.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户：Spring Security 登录认证的数据源（SecurityUserDetailsService 按 username 加载）
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，数据库唯一索引 */
    private String username;

    /** BCrypt 哈希，绝不明文存储/返回 */
    private String password;

    private String nickname;

    /** 角色：ADMIN 或 USER（演示只用单角色，权限点由 SecurityConfig 按角色映射） */
    private String role;

    private LocalDateTime createTime;
}
