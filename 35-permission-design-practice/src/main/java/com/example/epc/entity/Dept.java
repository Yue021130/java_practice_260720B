package com.example.epc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门实体，对应表 sys_dept。
 *
 * <p>parentId 自关联成树，是数据权限（DEPT_AND_CHILD 递归收集子孙部门）
 * 的维度来源，见 datascope/DataScopeResolver。</p>
 */
@Data
@TableName("sys_dept")
public class Dept {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门 id，顶级为 0 */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 排序号，越小越靠前 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
