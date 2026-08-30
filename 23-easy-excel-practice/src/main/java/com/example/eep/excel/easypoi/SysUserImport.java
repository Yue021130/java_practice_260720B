package com.example.eep.excel.easypoi;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.handler.inter.IExcelDataModel;
import cn.afterturn.easypoi.handler.inter.IExcelModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Easypoi 系统用户导入 DTO。
 *
 * <p>实现 {@link IExcelModel} 与 {@link IExcelDataModel}，用于回写校验错误信息。</p>
 */
@Data
public class SysUserImport implements IExcelModel, IExcelDataModel, Serializable {

    private static final long serialVersionUID = 1L;

    /** 行号（Easypoi 自动填充）。 */
    @Excel(name = "行号")
    private Integer rowNum;

    /** 校验错误信息。 */
    @Excel(name = "错误信息", width = 40)
    private String errorMsg;

    @Excel(name = "姓名(必填)", width = 20)
    @NotBlank(message = "姓名不能为空")
    private String realname;

    @Excel(name = "部门编码(必填)", width = 25)
    @NotBlank(message = "部门编码不能为空")
    private String deptOrgCode;

    @Excel(name = "角色编码(必填)", width = 20)
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @Excel(name = "手机号码(选填)", width = 18)
    private String phone;

    @Excel(name = "电子邮件(选填)", width = 25)
    private String email;

    @Excel(name = "性别(选填)", width = 12)
    private String sexName;

    @Excel(name = "工号(选填)", width = 18)
    private String workNo;
}
