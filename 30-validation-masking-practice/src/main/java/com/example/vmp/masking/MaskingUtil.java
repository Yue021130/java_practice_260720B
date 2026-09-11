package com.example.vmp.masking;

/**
 * 脱敏工具：提供五种常见打码策略。
 */
public class MaskingUtil {

    private MaskingUtil() {
    }

    /** 手机号：保留前 3 后 4，中间 4 位打码 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证号：保留前 4 后 4，中间打码 */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /** 邮箱：保留首字符与 @ 后域名 */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email.charAt(0) + "****" + email.substring(at);
        }
        return email.charAt(0) + "****" + email.substring(at);
    }

    /** 姓名：保留姓，名打码 */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(Math.max(0, name.length() - 1));
    }

    /** 地址：保留前 6 位，后续打码 */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }
}
