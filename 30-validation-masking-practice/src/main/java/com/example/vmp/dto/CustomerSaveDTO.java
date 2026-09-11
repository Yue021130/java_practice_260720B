package com.example.vmp.dto;

import com.example.vmp.validation.annotation.IdCard;
import com.example.vmp.validation.annotation.Phone;
import com.example.vmp.validation.groups.ValidationGroups;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 客户新增/编辑入参：分组校验区分新增必填与编辑可空。
 */
@Data
public class CustomerSaveDTO implements Serializable {

    /** 编辑必填（Update 分组），新增不传 */
    @NotBlank(message = "ID 不能为空", groups = ValidationGroups.Update.class)
    private String id;

    /** 新增必填（Create 分组） */
    @NotBlank(message = "姓名不能为空", groups = ValidationGroups.Create.class)
    @Size(max = 30, message = "姓名不能超过 30 个字符")
    private String name;

    /** 新增必填 */
    @NotBlank(message = "手机号不能为空", groups = ValidationGroups.Create.class)
    @Phone
    private String phone;

    /** 新增必填 */
    @NotBlank(message = "身份证号不能为空", groups = ValidationGroups.Create.class)
    @IdCard
    private String idCard;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

    @Size(max = 200, message = "住址不能超过 200 个字符")
    private String address;

    @Min(value = 0, message = "性别只能为 0/1")
    @Max(value = 1, message = "性别只能为 0/1")
    private Integer gender;

    @Min(value = 1, message = "等级范围为 1~5")
    @Max(value = 5, message = "等级范围为 1~5")
    private Integer level;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
