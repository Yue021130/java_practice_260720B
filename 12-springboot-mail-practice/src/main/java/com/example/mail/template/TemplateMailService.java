package com.example.mail.template;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 05. Thymeleaf 模板邮件场景。
 *
 * 为什么用模板：业务邮件（欢迎信/通知/报表）正文结构固定、只有变量变化，
 * 手写字符串拼接难维护且易出错。Thymeleaf 是 Spring Boot 默认模板引擎，
 * 模板放在 src/main/resources/templates/mail/ 下，用 th:text 绑定变量。
 */
@Service
@RequiredArgsConstructor
public class TemplateMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;
    private final SpringTemplateEngine templateEngine;

    /**
     * 用 welcome.html 模板发送欢迎邮件。
     */
    public Map<String, Object> sendWelcome(String to, String username, String platform) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("platform", platform);
            context.setVariable("year", 2026);
            context.setVariable("link", "https://example.com/verify?code=" + String.format("%08d", username.hashCode() % 100000000));

            // 模板渲染：把变量填充进 templates/mail/welcome.html
            String html = templateEngine.process("mail/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject("【" + platform + "】欢迎加入");
            helper.setText(html, true);

            MailRecord record = deliveryService.send(message, "template", "Thymeleaf 模板：welcome.html");
            return buildResult(record, html);
        } catch (Exception e) {
            throw new MailBizException("发送模板邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 用 order.html 模板发送订单通知（演示列表迭代 th:each）。
     */
    public Map<String, Object> sendOrder(String to, String customer) {
        try {
            Context context = new Context();
            context.setVariable("orderNo", "202608060001");
            context.setVariable("customer", customer);
            context.setVariable("total", "¥1,288.50");
            context.setVariable("items", new String[]{
                    "Java 并发编程实战 × 1  ￥99.00",
                    "Spring Boot 邮件服务实践课程 × 1  ￥1,099.00",
                    "快递运费  ￥0.00"
            });

            String html = templateEngine.process("mail/order", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject("【订单通知】您的订单已生成");
            helper.setText(html, true);

            MailRecord record = deliveryService.send(message, "template", "Thymeleaf 模板：order.html + th:each");
            return buildResult(record, html);
        } catch (Exception e) {
            throw new MailBizException("发送订单模板邮件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 模板变量说明。
     */
    public Map<String, Object> variables() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("welcome", "模板 mail/welcome.html：username、platform、year、link");
        result.put("order", "模板 mail/order.html：orderNo、customer、total、items(数组，th:each 渲染)");
        result.put("tip", "生产环境模板建议做成可配置（如存数据库），并支持国际化多语言模板。");
        return result;
    }

    private Map<String, Object> buildResult(MailRecord record, String html) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "模板邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送");
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        result.put("html", html);
        return result;
    }
}
