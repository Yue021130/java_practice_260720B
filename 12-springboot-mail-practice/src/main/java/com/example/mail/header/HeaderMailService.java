package com.example.mail.header;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 09. 邮件头与编码场景。
 *
 * 核心知识点：
 * 1. 非 ASCII 的邮件头（主题、发件人显示名）必须按 RFC 2047 编码成
 *    =?UTF-8?B?<base64>?= 形式，JavaMail 的 setSubject(subject, "UTF-8") 会自动处理；
 * 2. 业务常用自定义头：X-Priority（优先级）、Reply-To（回复地址）、X-Mailer（发送端标识）。
 */
@Service
@RequiredArgsConstructor
public class HeaderMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 发送一封带自定义头与回复地址的邮件。
     */
    public Map<String, Object> send(String to, String subject, String replyTo, String priority) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            if (replyTo != null && !replyTo.trim().isEmpty()) {
                // setReplyTo 只接收单个地址（不像 setTo/setCc 支持数组）
                InternetAddress[] replyToAddresses = MailSupport.parseAddresses(replyTo);
                if (replyToAddresses.length > 0) {
                    helper.setReplyTo(replyToAddresses[0]);
                }
            }
            // 中文主题：直接在 MimeMessage 上指定字符集，JavaMail 自动做 RFC 2047 编码
            message.setSubject(subject, "UTF-8");
            helper.setText("本邮件演示自定义邮件头：优先级、回复地址、发送端标识。");

            // 自定义邮件头
            message.setHeader("X-Mailer", "springboot-mail-practice");
            message.setHeader("X-Priority", priority);      // 1 最高，3 普通，5 最低
            message.setHeader("Importance", "High");

            MailRecord record = deliveryService.send(message, "header", "自定义邮件头 + RFC2047 编码主题");
            return buildResult(record, "已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送，含 X-Priority=" + priority);
        } catch (Exception e) {
            throw new MailBizException("发送带自定义头的邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 演示 RFC 2047 主题编码：原始 vs 编码后的 Header 值。
     */
    public Map<String, Object> encoding(String subject) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // 方式一：让 JavaMail 自动编码
            MimeMessage auto = mailSender.createMimeMessage();
            auto.setSubject(subject, "UTF-8");
            auto.saveChanges();
            String autoHeader = auto.getHeader("Subject")[0];

            // 方式二：手动编码（MimeUtility / Base64）
            byte[] bytes = subject.getBytes(StandardCharsets.UTF_8);
            String manual = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(bytes) + "?=";

            result.put("original", subject);
            result.put("encodedByJavaMail", autoHeader);
            result.put("encodedManually", manual);
            result.put("same", autoHeader.equals(manual));
            result.put("rule", "邮件头只要包含非 ASCII 字符就必须 RFC 2047 编码；setSubject(subject, \"UTF-8\") 自动完成。");
        } catch (Exception e) {
            throw new MailBizException("编码演示失败：" + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 常用邮件头速查。
     */
    public Map<String, Object> rules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("headers", new String[][]{
                {"From", "发件人（可带显示名，如 张三 <zs@x.com>）"},
                {"To / Cc / Bcc", "收件人 / 抄送 / 密送"},
                {"Subject", "主题，非 ASCII 需 RFC 2047 编码"},
                {"Reply-To", "回复地址，默认回复给发件人，这里可指定其他邮箱"},
                {"X-Priority", "1(最高)~5(最低)，多数客户端会高亮显示高优先级邮件"},
                {"Importance", "High / Normal / Low，与 X-Priority 配套"},
                {"X-Mailer", "发送端软件标识"}
        });
        result.put("tip", "反垃圾邮件视角：真实发件域名应配置 SPF/DKIM/DMARC，且避免大段全大写、敏感词，否则易进垃圾箱。");
        return result;
    }

    private Map<String, Object> buildResult(MailRecord record, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", message);
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        return result;
    }
}
