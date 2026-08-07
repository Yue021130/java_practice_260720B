package com.example.mail.attachment;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.ImageFactory;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 03. 附件邮件场景。
 *
 * 核心 API：MimeMessageHelper.addAttachment(filename, InputStreamSource / DataSource)。
 * 附件支持任意 MIME 类型，二进制与文本一视同仁，SMTP 层会自动 Base64 编码传输。
 */
@Service
@RequiredArgsConstructor
public class AttachmentMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 发送文本附件（动态生成 CSV，无需磁盘文件）。
     */
    public Map<String, Object> sendCsv(String to, String subject, int rows) {
        try {
            String csv = buildCsv(rows);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText("订单导出数据见附件。");

            // addAttachment 重载一：InputStreamSource（字节流）——适合内存动态生成的文件
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
            helper.addAttachment("订单导出.csv", new ByteArrayResource(bytes), "text/csv;charset=UTF-8");

            MailRecord record = deliveryService.send(message, "attachment", "CSV 附件：addAttachment(InputStreamSource)");
            return buildResult(record, "CSV 附件" + rows + " 行，约 " + bytes.length + " 字节");
        } catch (Exception e) {
            throw new MailBizException("发送 CSV 附件邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 发送二进制附件（PNG 图片，演示任意 MIME 类型）。
     */
    public Map<String, Object> sendImage(String to, String subject) {
        try {
            byte[] png = createPngBytes();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText("二进制附件演示：一张 PNG 图片。");

            // addAttachment 重载二：DataSource（自带 content type）
            javax.activation.DataSource ds = new javax.mail.util.ByteArrayDataSource(png, "image/png");
            helper.addAttachment("统计图表.png", ds);

            MailRecord record = deliveryService.send(message, "attachment", "PNG 附件：addAttachment(DataSource)");
            return buildResult(record, "PNG 图片附件，约 " + png.length + " 字节");
        } catch (Exception e) {
            throw new MailBizException("发送图片附件邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 附件大小限制说明。
     */
    public Map<String, Object> limitations() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("note", "附件大小瓶颈通常不在 JavaMail 而在邮件服务商配额，常见限制如下：");
        result.put("limits", new String[]{
                "QQ 邮箱：普通附件 50MB，超大附件 2GB（需单独走超大附件通道）",
                "163 邮箱：普通附件 50MB",
                "Gmail：附件 25MB（超过会自动转成 Google Drive 链接）",
                "企业邮箱：一般 20~50MB，以管理员配置为准"
        });
        result.put("tip", "超大文件不要直接塞进 MimeMessage（会占满内存），应改为上传对象存储后发下载链接。");
        return result;
    }

    private String buildCsv(int rows) {
        StringBuilder sb = new StringBuilder("订单号,客户,金额\n");
        for (int i = 1; i <= rows; i++) {
            sb.append("ORD").append(String.format("%06d", i)).append(',')
                    .append("客户").append(i % 7).append(',')
                    .append(String.format("%.2f", i * 19.9)).append('\n');
        }
        return sb.toString();
    }

    private byte[] createPngBytes() {
        return ImageFactory.createPng("Mail Attachment Demo", 320, 120);
    }

    private Map<String, Object> buildResult(MailRecord record, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送：" + message);
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        return result;
    }
}
