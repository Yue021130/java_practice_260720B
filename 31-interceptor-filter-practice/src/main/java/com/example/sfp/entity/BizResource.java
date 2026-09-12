package com.example.sfp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源实体，对应表 biz_resource：给拦截器提供真实业务流量。
 */
@Data
@TableName("biz_resource")
public class BizResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源名称 */
    private String name;

    /** 资源路径/标识 */
    private String url;

    /** 类型：1 API，2 页面，3 菜单 */
    private Integer type;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
