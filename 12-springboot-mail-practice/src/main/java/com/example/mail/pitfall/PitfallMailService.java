package com.example.mail.pitfall;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10. 常见坑与调优场景。
 *
 * 本模块不做真实发送，而是把工程里最容易踩的坑整理成可对照的清单与演示，
 * 帮助在写邮件代码前先避开它们。
 */
@Service
public class PitfallMailService {

    /**
     * 常见坑清单。
     */
    public Map<String, Object> list() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pitfalls", new String[][]{
                {"中文乱码", "未指定 UTF-8：helper 构造或 setSubject 未带 charset。解决：new MimeMessageHelper(msg, true, \"UTF-8\")。"},
                {"附件名乱码", "文件名含中文要传编码或使用 MimeUtility.encodeWord；部分客户端对 RFC2231 支持不一致。"},
                {"内联图片不显示", "cid 名字与 src=\"cid:xxx\" 大小写不一致，或忘了开 multipart(true)。"},
                {"HTML 当纯文本发出", "setText(html, false) 会把标签原样展示；要传 true。"},
                {"发件人被判定为垃圾邮件", "域名未配 SPF/DKIM/DMARC，或发件地址与登录账号不一致。"},
                {"@Async 方法不生效", "自调用 this.send() 不会走代理；需通过其他 Bean 注入后调用，或拆出独立组件。"},
                {"异步发送异常丢失", "void @Async 方法异常只会打日志；要感知失败请返回 Future/CompletableFuture 或注册 AsyncUncaughtExceptionHandler。"},
                {"线程池任务堆积 OOM", "用了无界队列；邮件线程池要用有界队列 + 拒绝策略。"},
                {"发送超时无感知", "未配置 connectiontimeout/timeout，SMTP 不可达时会卡很久。要设超时 + 重试。"},
                {"重复发送", "网络超时后重发可能造成用户收到多封；要基于业务幂等键去重。"}
        });
        return result;
    }

    /**
     * 演示：HTML 内容被当成纯文本发送会怎样。
     */
    public Map<String, Object> demoPlainVsHtml() {
        String html = "<h3>你好</h3><p style=\"color:red\">这是一段<b>富文本</b></p>";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("htmlSource", html);
        result.put("asPlainText", "客户端看到的是：<h3>你好</h3><p style=\"color:red\">这是一段<b>富文本</b></p>（标签原样展示）");
        result.put("asHtml", "客户端看到的是：你好（标题）/ 这是一段富文本（红色加粗）");
        result.put("tip", "setText(content, true) 才是 HTML；false 时正文里所有标签都会被当普通文本显示。");
        return result;
    }

    /**
     * 调优参数说明。
     */
    public Map<String, Object> tuning() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timeouts", new String[][]{
                {"mail.smtp.connectiontimeout", "连接 SMTP 超时（毫秒），默认无穷，必须设置"},
                {"mail.smtp.timeout", "读取响应超时（毫秒）"},
                {"mail.smtp.writetimeout", "写出超时（毫秒），JavaMail 1.6+ 支持"},
                {"mail.smtp.auth", "是否需要登录认证，服务商一般都要 true"}
        });
        result.put("tls", new String[][]{
                {"mail.smtp.ssl.enable", "SSL 直连（QQ 465 端口）"},
                {"mail.smtp.starttls.enable", "STARTTLS 升级（163 的 25 端口）"}
        });
        result.put("engineering", new String[]{
                "发送走独立线程池（mailExecutor），请求线程不阻塞",
                "失败重试 + 指数退避（见 07 模块），多次失败转人工/告警",
                "发送记录落库/打点，方便对账与排查",
                "真实验收：给测试邮箱发一封，检查正文、附件、图片、反垃圾评分"
        });
        return result;
    }
}
