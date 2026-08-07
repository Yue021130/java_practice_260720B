package com.example.mail.html;

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
 * 02. 富文本 HTML 邮件场景。
 */
@Service
@RequiredArgsConstructor
public class HtmlMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 发送一封带样式、表格、链接的 HTML 邮件。
     */
    public Map<String, Object> sendHtml(String to, String subject, String username, double amount) {
        try {
            String html = buildHtml(username, amount);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            // 第二个参数 true 表示内容为 HTML；邮件客户端若不支持 HTML 会回退到纯文本
            helper.setText(html, true);

            MailRecord record = deliveryService.send(message, "html", "富文本 HTML：helper.setText(html, true)");
            return buildResult(record, html);
        } catch (Exception e) {
            throw new MailBizException("发送 HTML 邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 返回示例 HTML，方便前端查看。
     */
    public Map<String, Object> example() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("html", buildHtml("张三", 1288.50));
        result.put("tip", "HTML 邮件用 <table> 布局兼容性最好；文字必须指定字体与颜色，避免暗色模式下不可见。");
        return result;
    }

    private String buildHtml(String username, double amount) {
        return "<!DOCTYPE html><html lang=\"zh-CN\"><body style=\"margin:0;padding:0;background:#f5f7fa;font-family:"
                + "-apple-system,'Segoe UI',Arial,sans-serif;\">"
                + "<div style=\"max-width:600px;margin:24px auto;background:#fff;border-radius:8px;overflow:hidden;\">"
                + "<div style=\"background:#2563eb;padding:24px;color:#fff;\">"
                + "<h2 style=\"margin:0;\">支付成功通知</h2></div>"
                + "<div style=\"padding:24px;color:#2c3e50;line-height:1.8;\">"
                + "<p>尊敬的 <b>" + username + "</b>：</p>"
                + "<p>您的订单已支付成功，详情如下：</p>"
                + "<table style=\"width:100%;border-collapse:collapse;margin:16px 0;\">"
                + "<tr><td style=\"padding:8px;border:1px solid #e2e8f0;\">订单编号</td>"
                + "<td style=\"padding:8px;border:1px solid #e2e8f0;\">202608060001</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #e2e8f0;\">支付金额</td>"
                + "<td style=\"padding:8px;border:1px solid #e2e8f0;color:#dc2626;font-weight:bold;\">¥ "
                + String.format("%.2f", amount) + "</td></tr>"
                + "<tr><td style=\"padding:8px;border:1px solid #e2e8f0;\">支付时间</td>"
                + "<td style=\"padding:8px;border:1px solid #e2e8f0;\">2026-08-06 10:30</td></tr></table>"
                + "<p style=\"font-size:13px;color:#64748b;\">如有疑问请联系客服（点击下方按钮打开站点）。</p>"
                + "<a href=\"https://example.com/orders/202608060001\" "
                + "style=\"display:inline-block;padding:10px 24px;background:#2563eb;color:#fff;"
                + "text-decoration:none;border-radius:6px;\">查看订单</a>"
                + "</div></div></body></html>";
    }

    private Map<String, Object> buildResult(MailRecord record, String html) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "HTML 邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送");
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        result.put("html", html);
        return result;
    }
}
