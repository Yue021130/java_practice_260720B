package com.example.mail.retry;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import com.example.mail.service.MailDeliveryService;
import com.example.mail.support.MailRecord;
import com.example.mail.support.MailSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 07. 失败重试邮件场景。
 *
 * 真实网络环境 SMTP 偶发失败（连接超时、瞬时 4xx、DNS 抖动）很常见，
 * 直接抛错给用户体验差。业界做法：失败后按指数退避重试若干次，仍失败再落库/告警。
 *
 * 本场景用 failTimes 参数模拟前 N 次“发送失败”，观察重试如何逐步退避并最终成功。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryMailService {

    private final JavaMailSenderImpl mailSender;
    private final MailPracticeProperties props;
    private final MailDeliveryService deliveryService;

    /**
     * 带重试的发送。
     *
     * @param failTimes 模拟前 failTimes 次失败（0 表示一次成功）
     * @param maxRetries 最大重试次数
     * @param backoff    退避策略：fixed 固定延迟 / exponential 指数退避
     */
    public Map<String, Object> sendWithRetry(String to, String subject, String content,
                                             int failTimes, int maxRetries, String backoff) {
        List<Map<String, Object>> attempts = new ArrayList<>();
        int attempt = 0;
        int maxAttempts = 1 + Math.max(0, maxRetries);
        boolean success = false;
        MailRecord finalRecord = null;

        while (attempt < maxAttempts) {
            attempt++;
            long start = System.currentTimeMillis();
            try {
                finalRecord = doAttempt(to, subject, content, attempt, failTimes);
                long cost = System.currentTimeMillis() - start;
                attempts.add(attemptMap(attempt, "SUCCESS", cost, 0, ""));
                success = true;
                break;
            } catch (Exception e) {
                long cost = System.currentTimeMillis() - start;
                long delay = nextDelay(attempt, maxRetries, backoff);
                attempts.add(attemptMap(attempt, "FAILED", cost, delay, e.getMessage()));
                if (attempt >= maxAttempts) {
                    log.warn("[重试] 已达最大尝试次数 {}，放弃发送", maxAttempts);
                    break;
                }
                sleep(delay);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("attempts", attempts);
        result.put("simulate", deliveryService.isSimulate());
        if (success && finalRecord != null) {
            result.put("record", finalRecord);
        }
        result.put("backoff", backoff);
        result.put("tip", "exponential 指数退避：延迟 = 基础延迟(500ms) × 2^(第几次重试-1)，失败越多次等得越久，给 SMTP 恢复时间。");
        return result;
    }

    /**
     * 重试策略说明。
     */
    public Map<String, Object> strategy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fixed", "固定延迟：每次重试间隔相同（如 1s），简单但对瞬时故障不够友好。");
        result.put("exponential", "指数退避：1s → 2s → 4s → 8s，避免对不稳定服务器造成重试风暴。推荐。");
        result.put("jitter", "退避 + 随机抖动：在退避值上加随机量，防止大量客户端同时重试（惊群）。");
        result.put("rules", new String[]{
                "必须设置最大重试次数，防止无限重试",
                "重试次数计入监控指标，多次失败要告警",
                "事务/幂等场景：收件人已收到邮件后重试会造成重复发送，需结合业务幂等键",
                "重试不可靠时（连续失败）建议降级为：落库 + 人工/定时任务补发"
        });
        return result;
    }

    private MailRecord doAttempt(String to, String subject, String content,
                                 int attempt, int failTimes) throws Exception {
        if (attempt <= failTimes) {
            // 模拟 SMTP 瞬时故障
            throw new MailBizException("模拟 SMTP 连接失败（第 " + attempt + " 次尝试）");
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(props.getFrom());
        helper.setTo(MailSupport.parseAddresses(to));
        helper.setSubject(subject);
        helper.setText(content);
        return deliveryService.send(message, "retry", "失败重试：指数退避后成功");
    }

    private long nextDelay(int attempt, int maxRetries, String backoff) {
        if ("exponential".equalsIgnoreCase(backoff)) {
            int retryIndex = attempt - 1;
            return props.getRetryBaseDelayMs() * (1L << Math.min(retryIndex, 10));
        }
        return props.getRetryBaseDelayMs();
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> attemptMap(int attempt, String status, long cost,
                                           long delayMs, String error) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attempt", attempt);
        m.put("status", status);
        m.put("costMs", cost);
        m.put("nextDelayMs", delayMs);
        m.put("error", error);
        return m;
    }
}
