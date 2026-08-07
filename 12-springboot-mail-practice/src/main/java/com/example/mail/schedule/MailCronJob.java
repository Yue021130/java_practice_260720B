package com.example.mail.schedule;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz 定时邮件任务。
 *
 * Quartz 每次触发都会通过自己的 JobFactory new 一个新实例（无状态），
 * 参数通过 JobDataMap 传入（注册时 usingJobData 写入，这里 getMergedJobDataMap 读取）。
 * 真正发邮件委托给 Spring 托管的 {@link QuartzJobDelegate}。
 *
 * @DisallowConcurrentExecution：同一种 Job 在上一次还没执行完时不允许并发执行，
 * 防止任务积压时线程互相叠加。
 */
@DisallowConcurrentExecution
public class MailCronJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String to = context.getMergedJobDataMap().getString("to");
        String subject = context.getMergedJobDataMap().getString("subject");
        String content = context.getMergedJobDataMap().getString("content");
        QuartzJobDelegate.get().sendFromJob(to, subject, content);
    }
}
