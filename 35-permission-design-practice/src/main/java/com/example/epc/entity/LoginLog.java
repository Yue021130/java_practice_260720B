package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志：谁、什么时候、从哪个 IP、成功还是失败。
 */
@Data
@TableName("sys_login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String ip;

    /** 0 失败 / 1 成功 */
    private Integer success;

    /** 失败原因（成功为 null） */
    private String message;

    private LocalDateTime createTime;
}
