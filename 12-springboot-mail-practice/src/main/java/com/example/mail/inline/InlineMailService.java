package com.example.mail.inline;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.ImageFactory;
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
 * 04. 内联图片邮件场景。
 *
 * 关键点：HTML 中用 &lt;img src="cid:xxx"&gt; 引用，图片通过
 * helper.addInline("xxx", dataSource, contentType) 附带。
 * 内联图片属于消息的一部分，随正文一起发送，不需要外链服务器。
 */
@Service
@RequiredArgsConstructor
public class InlineMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 发送一封带内联图片的 HTML 邮件。
     */
    public Map<String, Object> sendInline(String to, String subject) {
        try {
            byte[] logo = ImageFactory.createPng("Mail Logo", 240, 80);

            MimeMessage message = mailSender.createMimeMessage();
            // 必须开启 multipart，否则内联图片没有位置可放
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);

            String html = "<div style=\"font-family:Arial;color:#2c3e50;\">"
                    + "<img src=\"cid:mailLogo\" alt=\"logo\" width=\"240\"/><br/>"
                    + "<h3>带内联图片的邮件</h3>"
                    + "<p>这张图片嵌在邮件正文里，随消息一起发送，无需外链。</p>"
                    + "</div>";

            helper.setText(html, true);
            // addInline：第一个参数即 HTML 里 cid: 后面引用的名字
            javax.activation.DataSource ds = new javax.mail.util.ByteArrayDataSource(logo, "image/png");
            helper.addInline("mailLogo", ds);

            MailRecord record = deliveryService.send(message, "inline", "内联图片：helper.addInline(cid, DataSource)");
            return buildResult(record, html);
        } catch (Exception e) {
            throw new MailBizException("发送内联图片邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 内联图片 vs 外链图片对比说明。
     */
    public Map<String, Object> compare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inline", "内联图片：随邮件发送，收件人离线也能看，但增加邮件体积，适合小 logo / 小 banner。");
        result.put("external", "外链图片：正文只放 <img src=\"https://...\">，邮件小，但收件人客户端需联网加载，"
                + "且多数邮箱会默认拦截外链图片（需点击“显示图片”）。");
        result.put("rule", "小图用 cid 内联，大图用外链/附件；涉及敏感信息避免外链（可能泄露 URL）。");
        return result;
    }

    private Map<String, Object> buildResult(MailRecord record, String html) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "内联图片邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送");
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        result.put("html", html);
        result.put("tip", "如果收件人看不到图片：检查 cid 名是否与 src=\"cid:xxx\" 完全一致、大小写是否匹配。");
        return result;
    }
}
