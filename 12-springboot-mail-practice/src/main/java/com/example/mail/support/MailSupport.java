package com.example.mail.support;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 邮件地址解析工具。
 *
 * 前端面板传入的收件人可能是 "a@x.com,b@x.com" 或 "a@x.com;b@x.com"，
 * 统一在这里切分并转换为 InternetAddress 数组。
 */
public final class MailSupport {

    private MailSupport() {
    }

    /**
     * 把逗号/分号分隔的地址串解析为 InternetAddress 数组。
     *
     * @param csv 如 "a@example.com,b@example.com"
     */
    public static InternetAddress[] parseAddresses(String csv) throws AddressException {
        if (csv == null || csv.trim().isEmpty()) {
            return new InternetAddress[0];
        }
        String[] parts = csv.split("[,;，；]");
        List<InternetAddress> addresses = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                addresses.add(new InternetAddress(trimmed));
            }
        }
        return addresses.toArray(new InternetAddress[0]);
    }
}
