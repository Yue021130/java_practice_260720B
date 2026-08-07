package com.example.mail.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 一封已发送（或已模拟发送）的邮件记录。
 *
 * 用于「最近发送记录」面板展示；simulate 模式下记录的是内存中构造的消息内容，
 * 方便在不配置 SMTP 的情况下观察每条消息的结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailRecord {

    /** 记录序号 */
    private long id;

    /** 场景标签：basic / html / attachment / inline / template / async / retry / schedule / header */
    private String tag;

    /** 发件人 */
    private String from;

    /** 收件人（多个逗号分隔） */
    private String to;

    /** 主题 */
    private String subject;

    /** 消息 Content-Type */
    private String contentType;

    /** 消息大小（字节，saveChanges 后才有值） */
    private int sizeBytes;

    /** 发送耗时（毫秒） */
    private long costMs;

    /** 是否为模拟发送 */
    private boolean simulate;

    /** 发送时间戳（epoch 毫秒） */
    private long sentAt;

    /** 备注 */
    private String note;

    /**
     * 从 MimeMessage 提取展示字段。
     *
     * 注意：MimeMessage.getSize() 在内容尚未物化时返回 -1，这里用 writeTo 写入
     * 字节流得到真实的线缆大小（header + body 编码后）。
     */
    public static MailRecord of(String tag, MimeMessage message, boolean simulate, String note, long costMs)
            throws MessagingException {
        MailRecord record = new MailRecord();
        record.setTag(tag);
        record.setFrom(format(message.getFrom()));
        record.setTo(format(message.getAllRecipients()));
        record.setSubject(message.getSubject());
        record.setContentType(message.getContentType());
        record.setSizeBytes(computeSize(message));
        record.setSimulate(simulate);
        record.setCostMs(costMs);
        record.setSentAt(System.currentTimeMillis());
        record.setNote(note);
        return record;
    }

    /**
     * 计算 MimeMessage 完整编码后的字节大小。
     */
    private static int computeSize(MimeMessage message) throws MessagingException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            message.writeTo(out);
            return out.size();
        } catch (IOException e) {
            return message.getSize();
        }
    }

    /**
     * Address 数组转可读字符串。
     */
    public static String format(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        return Arrays.stream(addresses)
                .map(Address::toString)
                .collect(Collectors.joining(", "));
    }
}
