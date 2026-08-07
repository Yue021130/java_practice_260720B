package com.example.mail.async;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 06. 异步发送邮件场景。
 *
 * 为什么异步：发送邮件是 IO 操作（网络往返），若放在请求线程同步执行，
 * 一个接口可能要等几百毫秒到几秒；用 @Async 丢给独立线程池，接口立刻返回，
 * 提升用户体验与吞吐。@Async 依赖 {@link org.springframework.scheduling.annotation.EnableAsync}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;
    private final ThreadPoolTaskExecutor mailExecutor;

    /** 任务状态表：taskId -> 状态（PENDING/RUNNING/SENT/FAILED） */
    private final Map<String, String> taskStatus = new ConcurrentHashMap<>();
    /** 失败原因表 */
    private final Map<String, String> taskError = new ConcurrentHashMap<>();

    /**
     * 异步发送：方法立即返回 taskId，真正发送在 mailExecutor 线程池中执行。
     */
    public Map<String, Object> sendAsync(String to, String subject, String content) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskStatus.put(taskId, "PENDING");
        doSendAsync(taskId, to, subject, content);
        return buildResult(taskId, "任务已提交，发送在后台线程池执行");
    }

    /**
     * @Async 方法：自调用不会生效，因此从公开方法转发到这里。
     */
    @Async("mailExecutor")
    public void doSendAsync(String taskId, String to, String subject, String content) {
        taskStatus.put(taskId, "RUNNING");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(MailSupport.parseAddresses(to));
            helper.setSubject(subject);
            helper.setText(content);
            deliveryService.send(message, "async", "异步发送：@Async(\"mailExecutor\")");
            taskStatus.put(taskId, "SENT");
            log.info("[异步发送] taskId={} 完成", taskId);
        } catch (Exception e) {
            taskStatus.put(taskId, "FAILED");
            taskError.put(taskId, e.getMessage());
            log.error("[异步发送] taskId={} 失败", taskId, e);
        }
    }

    /**
     * 查询异步任务状态。
     */
    public Map<String, Object> status(String taskId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        String status = taskStatus.getOrDefault(taskId, "NOT_FOUND");
        result.put("status", status);
        if ("FAILED".equals(status)) {
            result.put("error", taskError.getOrDefault(taskId, ""));
        }
        return result;
    }

    /**
     * 异步线程池当前指标。
     */
    public Map<String, Object> poolInfo() {
        ThreadPoolExecutor pool = mailExecutor.getThreadPoolExecutor();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeCount", pool.getActiveCount());
        result.put("poolSize", pool.getPoolSize());
        result.put("corePoolSize", pool.getCorePoolSize());
        result.put("maximumPoolSize", pool.getMaximumPoolSize());
        result.put("queueSize", mailExecutor.getQueueSize());
        result.put("completedTaskCount", pool.getCompletedTaskCount());
        result.put("config", "core=4 max=8 queueCapacity=200 rejectedPolicy=CallerRunsPolicy");
        return result;
    }

    private Map<String, Object> buildResult(String taskId, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("message", message);
        result.put("simulate", deliveryService.isSimulate());
        result.put("tip", "用「查询异步状态」接口轮询 taskId 观察 PENDING→RUNNING→SENT 流转。");
        return result;
    }
}
