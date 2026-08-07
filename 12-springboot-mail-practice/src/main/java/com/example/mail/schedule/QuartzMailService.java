package com.example.mail.schedule;

import com.example.mail.common.MailBizException;
import com.example.mail.config.MailPracticeProperties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quartz 定时任务管理服务。
 *
 * 演示两种使用方式：
 * 1. 动态注册：通过接口用 Cron 表达式注册任务（本模块主要方式，便于实验）；
 * 2. 启动注册：quartz-demo=true 时启动即注册一个每 30 秒的任务（见 registerDemoIfEnabled）。
 *
 * 默认内存 JobStore（RAMJobStore），无需数据库；生产可切换 spring.quartz.job-store-type=jdbc。
 */
@Slf4j
@Service
public class QuartzMailService {

    /** 任务组名，统一归到一组便于管理 */
    private static final String GROUP = "mail";

    private final Scheduler scheduler;
    private final MailPracticeProperties props;

    public QuartzMailService(Scheduler scheduler, MailPracticeProperties props) {
        this.scheduler = scheduler;
        this.props = props;
    }

    /**
     * 启动时注册演示任务（仅当 quartz-demo=true）。
     */
    @PostConstruct
    public void registerDemoIfEnabled() {
        if (!props.isQuartzDemo()) {
            return;
        }
        try {
            Map<String, Object> job = registerCronJob("quartz-demo-job", "0/30 * * * * ?",
                    props.getFrom(), "[Quartz] 每30秒演示任务", "Quartz 定时任务演示：每 30 秒发送一封邮件。");
            log.info("[Quartz] 演示任务已注册：{}", job);
        } catch (Exception e) {
            log.warn("[Quartz] 演示任务注册失败：{}", e.getMessage());
        }
    }

    /**
     * 用 Cron 表达式注册一个定时邮件任务。
     */
    public Map<String, Object> registerCronJob(String jobName, String cron,
                                               String to, String subject, String content) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new MailBizException("非法的 Cron 表达式：" + cron);
        }
        JobKey jobKey = JobKey.jobKey(jobName, GROUP);
        try {
            if (scheduler.checkExists(jobKey)) {
                throw new MailBizException("任务已存在：" + jobName + "，可先删除或换名字");
            }
            JobDetail detail = org.quartz.JobBuilder.newJob(MailCronJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("to", to)
                    .usingJobData("subject", subject)
                    .usingJobData("content", content)
                    .storeDurably(true)
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "-trigger", GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .forJob(jobKey)
                    .build();

            scheduler.scheduleJob(detail, trigger);
            log.info("[Quartz] 已注册任务 {}，cron={}", jobName, cron);
            return jobInfo(jobKey);
        } catch (SchedulerException e) {
            throw new MailBizException("注册 Quartz 任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 列出所有定时任务与触发器状态。
     */
    public Map<String, Object> listJobs() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        try {
            for (String group : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
                    jobs.add(jobInfo(jobKey));
                }
            }
        } catch (SchedulerException e) {
            throw new MailBizException("查询 Quartz 任务失败：" + e.getMessage(), e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", jobs.size());
        result.put("jobs", jobs);
        result.put("tip", "PAUSED=已暂停，NORMAL=运行中；nextFireTime 为下次触发时间。");
        return result;
    }

    /**
     * 暂停任务（不再触发，但保留定义）。
     */
    public Map<String, Object> pause(String jobName) {
        try {
            scheduler.pauseJob(JobKey.jobKey(jobName, GROUP));
            return jobInfo(JobKey.jobKey(jobName, GROUP));
        } catch (SchedulerException e) {
            throw new MailBizException("暂停任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 恢复暂停的任务。
     */
    public Map<String, Object> resume(String jobName) {
        try {
            scheduler.resumeJob(JobKey.jobKey(jobName, GROUP));
            return jobInfo(JobKey.jobKey(jobName, GROUP));
        } catch (SchedulerException e) {
            throw new MailBizException("恢复任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除任务（连同触发器一起删除）。
     */
    public Map<String, Object> delete(String jobName) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, GROUP);
            boolean deleted = scheduler.deleteJob(jobKey);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobName", jobName);
            result.put("deleted", deleted);
            return result;
        } catch (SchedulerException e) {
            throw new MailBizException("删除任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * Quartz 核心概念速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("core", new String[][]{
                {"Job", "要执行的任务逻辑（本模块 MailCronJob 实现 org.quartz.Job.execute）"},
                {"JobDetail", "任务定义：绑定 Job 实现类 + JobDataMap 参数"},
                {"Trigger", "触发规则：CronTrigger（Cron 表达式）/ SimpleTrigger（固定间隔）"},
                {"Scheduler", "调度器：调度 JobDetail 与 Trigger，负责线程管理"},
                {"JobDataMap", "任务参数传递：注册时 usingJobData 写入，执行时 getMergedJobDataMap 读取"},
                {"@DisallowConcurrentExecution", "同 Job 不并发执行，避免任务叠加"},
                {"JobStore", "任务持久化：RAMJobStore（内存，默认）/ JDBCJobStore（数据库，集群用）"}
        });
        result.put("cronExample", new String[][]{
                {"0 30 9 * * ?", "每天 09:30 触发"},
                {"0 0/5 * * * ?", "每 5 分钟触发"},
                {"0 0 12 * * MON-FRI", "工作日中午 12:00 触发"},
                {"0/30 * * * * ?", "每 30 秒触发（演示常用）"}
        });
        result.put("vs", new String[][]{
                {"@Scheduled", "Spring 内置注解，简单、够用；固定延迟/固定速率/Cron 均可"},
                {"Quartz", "功能强：持久化、集群、错过补偿(misfire)、暂停/恢复、灵活触发器"},
                {"选型", "单机简单场景用 @Scheduled；多实例/需要不丢任务/复杂调度用 Quartz"}
        });
        return result;
    }

    private Map<String, Object> jobInfo(JobKey jobKey) throws SchedulerException {
        JobDetail detail = scheduler.getJobDetail(jobKey);
        JobDataMap data = detail.getJobDataMap();
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobName", jobKey.getName());
        job.put("group", jobKey.getGroup());
        job.put("jobClass", detail.getJobClass().getSimpleName());
        job.put("to", data.getString("to"));
        job.put("subject", data.getString("subject"));

        List<Map<String, Object>> triggers = new ArrayList<>();
        for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("triggerName", trigger.getKey().getName());
            t.put("type", trigger instanceof CronTrigger ? "cron" : "simple");
            if (trigger instanceof CronTrigger) {
                t.put("cron", ((CronTrigger) trigger).getCronExpression());
            }
            t.put("state", scheduler.getTriggerState(trigger.getKey()).name());
            t.put("nextFireTime", format(trigger.getNextFireTime()));
            t.put("previousFireTime", format(trigger.getPreviousFireTime()));
            triggers.add(t);
        }
        job.put("triggers", triggers);
        return job;
    }

    private String format(Date date) {
        if (date == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
