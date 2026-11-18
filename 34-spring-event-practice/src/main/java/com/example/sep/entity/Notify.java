package com.example.sep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，对应表 sys_notify。
 *
 * <p>由异步监听器在事件触发后写入，模拟"请假提交后通知审批人"等场景。</p>
 */
@Data
@TableName("sys_notify")
public class Notify {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户名 */
    private String receiver;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读：0 未读，1 已读 */
    private Integer readFlag;

    private LocalDateTime createTime;
}
