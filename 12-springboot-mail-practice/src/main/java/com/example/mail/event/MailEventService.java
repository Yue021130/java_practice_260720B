package com.example.mail.event;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 11. 事件监听场景。
 *
 * 核心链路：发送成功/失败 → 发布 MailSentEvent → 各 @EventListener 同步/异步响应。
 * 事件解耦了「发送邮件」与「后续处理」（日志、统计、通知、告警），
 * 新增一种后续处理只需新增一个监听器，不改发送方代码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailEventService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;
    private final MailStatsEventListener statsListener;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    /**
     * 发送一封邮件，触发完整事件链路（发送→发布→监听）。
     */
    public Map<String, Object> send(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText(content);

            MailRecord record = deliveryService.send(message, "event", "触发 @EventListener 事件链路");
            return buildResult(record);
        } catch (Exception e) {
            throw new MailBizException("事件链路邮件发送失败：" + e.getMessage(), e);
        }
    }

    /**
     * 手动发布成功/失败事件，直观看到哪些监听器响应（控制台看日志）。
     */
    public Map<String, Object> publishDemo() {
        // 发布一个成功事件（record 可空，仅演示）
        eventPublisher.publishEvent(new MailSentEvent(null, true, null));
        // 发布一个失败事件
        eventPublisher.publishEvent(new MailSentEvent(null, false, "模拟 SMTP 连接超时"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("published", new String[]{"MailSentEvent(success=true)", "MailSentEvent(success=false)"});
        result.put("statsAfterPublish", statsListener.snapshot());
        result.put("tip", "看控制台：同步监听器（日志/统计）立刻执行；异步监听器（站内通知）在线程池中延迟执行。");
        return result;
    }

    /**
     * 当前事件统计快照。
     */
    public Map<String, Object> stats() {
        return statsListener.snapshot();
    }

    /**
     * 扫描容器里所有 @EventListener 方法并返回清单（学习用：看事件监听器的注册情况）。
     */
    public Map<String, Object> listeners() {
        List<Map<String, Object>> found = new ArrayList<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            // 带 @Async 的 Bean 会被 CGLIB 代理，代理类上看不到方法上的 @EventListener，
            // 用 AopUtils.getTargetClass 解包回目标类再扫描
            Class<?> type = applicationContext.getType(beanName);
            if (type == null) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            type = AopUtils.getTargetClass(bean);
            for (Method method : type.getMethods()) {
                EventListener annotation = method.getAnnotation(EventListener.class);
                if (annotation != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("bean", beanName);
                    item.put("method", method.getName());
                    item.put("eventType", method.getParameterTypes().length > 0
                            ? method.getParameterTypes()[0].getSimpleName() : "(none)");
                    item.put("async", method.getAnnotation(org.springframework.scheduling.annotation.Async.class) != null);
                    item.put("condition", annotation.condition());
                    found.add(item);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", found.size());
        result.put("listeners", found);
        return result;
    }

    /**
     * 事件监听相关知识速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("core", new String[][]{
                {"@EventListener", "Spring 4.2 起可监听任意 POJO；方法参数类型即事件类型，同步执行"},
                {"condition", "SpEL 条件过滤，如 condition = \"#event.success\"，只对满足条件的事件响应"},
                {"@Async 监听", "@EventListener 上再加 @Async 可异步执行，适合慢副作用"},
                {"@TransactionalEventListener", "事务监听：phase=AFTER_COMMIT 在事务提交后执行，避免未提交数据被读到"},
                {"@Order", "多个监听器同时匹配时控制执行顺序，数字越小越先执行"},
                {"发布方", "注入 ApplicationEventPublisher，publishEvent(event) 即发布"}
        });
        result.put("why", "事件解耦发送与后续处理：加日志/统计/通知/告警都只需新增监听器，发送方零改动；"
                + "配合异步监听可大幅降低发送接口的响应时间。");
        return result;
    }

    private Map<String, Object> buildResult(MailRecord record) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "邮件已" + (deliveryService.isSimulate() ? "模拟" : "") + "发送，事件链路已触发");
        result.put("simulate", deliveryService.isSimulate());
        result.put("record", record);
        result.put("statsAfterSend", statsListener.snapshot());
        result.put("tip", "看控制台日志：发送完成后日志监听器/统计监听器同步响应，站内通知异步响应。");
        return result;
    }
}
