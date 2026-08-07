package com.example.mail.basic;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 01. 基础邮件场景：纯文本 / 多收件人 / 抄送密送 / 最近发送记录。
 */
@Service
@RequiredArgsConstructor
public class BasicMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 发送一封最简单的纯文本邮件。
     */
    public Map<String, Object> sendText(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText(content);

            MailRecord record = deliveryService.send(message, "basic",
                    "最简单的纯文本邮件：MimeMessageHelper.setText(content)");
            return buildResult(record, "邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送");
        } catch (Exception e) {
            throw new MailBizException("发送基础文本邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 演示多收件人 + 抄送 + 密送。
     */
    public Map<String, Object> sendMultiple(String to, String cc, String bcc, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            if (cc != null && !cc.trim().isEmpty()) {
                helper.setCc(MailSupport.parseAddresses(cc));
            }
            if (bcc != null && !bcc.trim().isEmpty()) {
                helper.setBcc(MailSupport.parseAddresses(bcc));
            }
            helper.setSubject(subject);
            helper.setText(content);

            MailRecord record = deliveryService.send(message, "basic",
                    "多收件人 + 抄送 + 密送：setTo / setCc / setBcc");
            return buildResult(record, "含抄送/密送的邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送");
        } catch (Exception e) {
            throw new MailBizException("发送多收件人邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 最近发送记录（配合前端面板查看不同构造方式的差异）。
     */
    public Map<String, Object> recent() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", deliveryService.recentRecords());
        result.put("tip", "simulate 模式下邮件并未真正发出，仅记录构造内容；切到 real 模式才是真实发送。");
        return result;
    }

    /**
     * 当前发送模式与 SMTP 配置概览（前端面板顶部展示）。
     */
    public Map<String, Object> mode() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", props.getMode());
        result.put("simulate", deliveryService.isSimulate());
        result.put("from", props.getFrom());
        result.put("host", props.getHost());
        result.put("port", props.getPort());
        result.put("tip", "改 application.yml 中 mail.practice.mode=real 并填好 SMTP 账号/授权码即可真实发送；"
                + "simulate 模式开箱即用。");
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
