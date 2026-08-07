package com.example.mail.schedule;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 08. 定时 / 批量发送邮件场景。
 *
 * - 批量：循环构造并发送 N 封邮件，逐封 try-catch 统计成败（发送方失败不拖垮整批）
 * - 定时：@Scheduled 心跳任务 + ScheduledExecutorService 一次性延迟任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;
    private final ScheduledExecutorService mailScheduler;

    /** 延迟任务登记表：jobId -> 任务信息 */
    private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();

    /**
     * 批量发送 count 封邮件。
     */
    public Map<String, Object> batchSend(String to, String subjectPrefix, int count) {
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> detail = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (int i = 1; i <= count; i++) {
            try {
                MailRecord record = buildAndSend(to, subjectPrefix + " #" + i,
                        "第 " + i + " 封批量邮件，共 " + count + " 封。", "schedule", "批量发送");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);
                item.put("subject", record.getSubject());
                item.put("sizeBytes", record.getSizeBytes());
                item.put("ok", true);
                detail.add(item);
                success++;
            } catch (Exception e) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);
                item.put("ok", false);
                item.put("error", e.getMessage());
                detail.add(item);
                failed++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", count);
        result.put("success", success);
        result.put("failed", failed);
        result.put("costMs", System.currentTimeMillis() - start);
        result.put("detail", detail);
        result.put("simulate", deliveryService.isSimulate());
        result.put("tip", "批量发送要逐封捕获异常并统计成败；大批量建议改异步批量 + 队列削峰，避免阻塞请求线程。");
        return result;
    }

    /**
     * 注册一个延迟发送任务。
     *
     * @param delaySeconds 延迟秒数后发送
     */
    public Map<String, Object> registerDelayed(String to, String subject, String content, int delaySeconds) {
        String jobId = UUID.randomUUID().toString().replace("-", "");
        // 用并发 Map 存任务状态：调度线程会写 status，请求线程会读
        Map<String, Object> job = new ConcurrentHashMap<>();
        job.put("jobId", jobId);
        job.put("status", "PENDING");
        job.put("to", to);
        job.put("subject", subject);
        job.put("delaySeconds", delaySeconds);
        job.put("createdAt", System.currentTimeMillis());
        jobs.put(jobId, job);

        mailScheduler.schedule(() -> {
            try {
                buildAndSend(to, subject, content, "schedule", "延迟任务 " + jobId);
                job.put("status", "SENT");
                log.info("[定时发送] jobId={} 已发送", jobId);
            } catch (Exception e) {
                job.put("status", "FAILED");
                job.put("error", e.getMessage());
                log.error("[定时发送] jobId={} 失败", jobId, e);
            }
        }, Math.max(0, delaySeconds), TimeUnit.SECONDS);

        return job;
    }

    /**
     * 延迟任务列表。
     */
    public Map<String, Object> listJobs() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobs", new ArrayList<>(jobs.values()));
        result.put("tip", "用 ScheduledExecutorService 做一次性延迟任务；@Scheduled 做周期任务（见心跳说明）。");
        return result;
    }

    /**
     * 周期心跳任务：每分钟执行一次。
     *
     * 默认关闭（scheduleDemo=false）避免打扰；打开后每分钟发一封心跳邮件，
     * 用于演示 @Scheduled 周期发送（真实告警系统会这么做）。
     */
    @Scheduled(fixedDelay = 60000)
    public void heartbeat() {
        if (!props.isScheduleDemo()) {
            return;
        }
        try {
            buildAndSend(props.getFrom(), "[心跳] 服务运行正常",
                    "Spring Boot 邮件服务定时任务心跳，时间：" + System.currentTimeMillis(),
                    "schedule", "@Scheduled(fixedDelay=60s) 心跳");
            log.info("[心跳] 定时心跳邮件已发送");
        } catch (Exception e) {
            log.error("[心跳] 发送失败", e);
        }
    }

    public Map<String, Object> heartbeatInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", props.isScheduleDemo());
        result.put("howToEnable", "把 application.yml 中 mail.practice.schedule-demo 改为 true，每分钟发一封心跳邮件");
        result.put("realUsage", "真实系统常用于：服务健康巡检、报表定时推送、优惠券到期提醒、续费通知。");
        return result;
    }

    private MailRecord buildAndSend(String to, String subject, String content,
                                    String tag, String note) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(props.getFrom());
        helper.setTo(MailSupport.parseAddresses(to));
        helper.setSubject(subject);
        helper.setText(content);
        return deliveryService.send(message, tag, note);
    }
}
