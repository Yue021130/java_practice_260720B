package com.example.vmp.masking;

/**
 * 脱敏类型枚举。
 */
public enum SensitiveType {

    /** 手机号：138****8000 */
    PHONE,

    /** 身份证号：1101**********7758 */
    ID_CARD,

    /** 邮箱：z****@163.com */
    EMAIL,

    /** 姓名：张* */
    NAME,

    /** 地址：北京市朝阳区**** */
    ADDRESS
}
