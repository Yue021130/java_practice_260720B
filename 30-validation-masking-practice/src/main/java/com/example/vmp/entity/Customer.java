package com.example.vmp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体，对应表 biz_customer：敏感字段明文存储，出参脱敏。
 */
@Data
@TableName("biz_customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 身份证号 */
    private String idCard;

    /** 邮箱 */
    private String email;

    /** 住址 */
    private String address;

    /** 性别：1 男，0 女 */
    private Integer gender;

    /** 客户等级：1~5 */
    private Integer level;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
