package com.example.excel.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户演示数据（各模块共享的数据源）。
 *
 * 真实工程里这通常是数据库实体，导入导出时再映射到各自带注解的 head 类；
 * 这里用普通 POJO 承载演示数据，各模块 Service 按需转成自己的 head 对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRow {

    /** 用户编号 */
    private Integer id;

    /** 姓名 */
    private String name;

    /** 部门 */
    private String department;

    /** 月薪 */
    private Double salary;

    /** 入职日期 */
    private Date hireDate;

    /** 是否在职 */
    private Boolean active;
}
