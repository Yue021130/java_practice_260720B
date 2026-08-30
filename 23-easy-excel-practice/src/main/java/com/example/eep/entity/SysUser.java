package com.example.eep.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * 系统用户实体，演示导入成功后落库。
 */
@Data
@Entity
@Table(name = "t_sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String realname;

    @Column(length = 64)
    private String deptOrgCode;

    @Column(length = 64)
    private String roleCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String email;

    @Column(length = 10)
    private String sexName;

    @Column(length = 32)
    private String workNo;
}
