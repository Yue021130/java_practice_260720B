package com.example.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实发送集成测试。
 *
 * 用 GreenMail 在本机 127.0.0.1:3025 起一个嵌入式 SMTP 服务器，
 * 把模式切到 real，验证邮件真的从 JavaMailSender 发出并被服务器接收。
 * 全程离线，不需要任何外部邮箱账号。
 */
@SpringBootTest(properties = {
        "mail.practice.mode=real",
        "mail.practice.host=127.0.0.1",
        "mail.practice.port=3025",
        "mail.practice.username=zsx@example.com",
        "mail.practice.password=zsx123",
        "mail.practice.ssl=false",
        "mail.practice.starttls=false"
})
@AutoConfigureMockMvc
class GreenMailRealSendTest {

    private static GreenMail greenMail;

    @BeforeAll
    static void startServer() {
        ServerSetup setup = new ServerSetup(3025, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(setup);
        // 注册 SMTP 登录账号，验证带认证的真实发送流程（与 QQ/163 一致）
        greenMail.setUser("zsx@example.com", "zsx123");
        greenMail.start();
    }

    @AfterAll
    static void stopServer() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void textMailIsReallyDelivered() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();

        mockMvc.perform(post("/api/basic/text")
                        .param("to", "zsx-receiver@example.com")
                        .param("subject", "真实发送测试")
                        .param("content", "这封邮件通过 GreenMail 真实发送并被接收。"))
                .andExpect(status().isOk());

        // 发送是同步的：HTTP 返回时邮件应已到达 GreenMail
        Message[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        MimeMessage received = (MimeMessage) messages[0];
        assertThat(received.getSubject()).isEqualTo("真实发送测试");
        assertThat(received.getAllRecipients()[0].toString()).isEqualTo("zsx-receiver@example.com");
    }

    @Test
    void htmlMailWithAttachmentIsDelivered() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();

        mockMvc.perform(post("/api/html/send")
                        .param("subject", "真实 HTML 测试")
                        .param("username", "李四")
                        .param("amount", "99.90"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/attachment/csv")
                        .param("rows", "3"))
                .andExpect(status().isOk());

        Message[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(2);
        assertThat(messages[0].getSubject()).isEqualTo("真实 HTML 测试");
        // 附件邮件应是 multipart 结构
        assertThat(messages[1].getContentType()).contains("multipart");
    }
}
