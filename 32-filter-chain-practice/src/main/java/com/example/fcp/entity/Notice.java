package com.example.fcp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体，对应表 biz_notice：给过滤器链提供真实业务流量。
 */
@Data
@TableName("biz_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 类型：1 通知，2 公告，3 新闻 */
    private Integer type;

    /** 状态：1 发布，0 草稿 */
    private Integer status;

    /** 正文内容 */
    private String content;

    /** 排序号 */
    private Integer sortOrder;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
