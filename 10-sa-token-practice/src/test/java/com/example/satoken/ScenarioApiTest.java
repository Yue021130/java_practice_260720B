package com.example.satoken;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sa-Token 全场景接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginScenarios() throws Exception {
        okPost("/api/login/do-login?id=10001");
        ok("/api/login/is-login");
        ok("/api/login/token-value");
        ok("/api/login/login-id");
        okPost("/api/login/logout");
    }

    @Test
    void loginModelScenarios() throws Exception {
        okPost("/api/login-model/single?id=10001");
        okPost("/api/login-model/multi?id=10001");
        okPost("/api/login-model/mutex?id=10001&device=phone");
        okPost("/api/login-model/remember?id=10001");
        okPost("/api/login-model/7days?id=10001");
    }

    @Test
    void unauthorizedAccessShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/session/account/get?key=name"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permissionScenarios() throws Exception {
        // 登录并写入权限，保存 cookie
        MvcResult loginResult = mockMvc.perform(post("/api/permission/login-with-perms?id=10001&perms=user:add,user:edit&roles=user"))
                .andExpect(status().isOk()).andReturn();
        MockCookie cookie = extractCookie(loginResult);

        // 具备 user:add 权限
        okWithCookie("/api/permission/check-perm?perm=user:add", cookie);

        // 不具备 admin 权限
        mockMvc.perform(get("/api/permission/check-perm?perm=admin").cookie(cookie))
                .andExpect(status().isForbidden());

        // 注解鉴权：没有 admin 权限，返回 403
        mockMvc.perform(get("/api/permission/anno-admin").cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void routeInterceptorScenarios() throws Exception {
        // 公开接口放行
        ok("/api/route/public/info");

        // 未登录访问 user 模块被拦截
        mockMvc.perform(get("/api/route/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionScenarios() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login/do-login?id=10001"))
                .andExpect(status().isOk()).andReturn();
        MockCookie cookie = extractCookie(loginResult);

        mockMvc.perform(post("/api/session/account/set?key=name&value=alice").cookie(cookie))
                .andExpect(status().isOk());
        okWithCookie("/api/session/account/get?key=name", cookie);
        mockMvc.perform(post("/api/session/token/set?key=traceId&value=t-123").cookie(cookie))
                .andExpect(status().isOk());
        okWithCookie("/api/session/token/get?key=traceId", cookie);
        okWithCookie("/api/session/login-device-count", cookie);
    }

    @Test
    void kickoutAndDisableScenarios() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login/do-login?id=10001"))
                .andExpect(status().isOk()).andReturn();
        MockCookie cookie = extractCookie(loginResult);

        // 踢下线后再访问需要登录的接口应返回 401
        mockMvc.perform(post("/api/manage/kickout?id=10001").cookie(cookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/session/account/get?key=name").cookie(cookie))
                .andExpect(status().isUnauthorized());

        // 重新登录并封禁（使用唯一 id 避免测试间状态污染）
        long uniqueId = System.currentTimeMillis();
        MvcResult reLogin = mockMvc.perform(post("/api/login/do-login?id=" + uniqueId))
                .andExpect(status().isOk()).andReturn();
        MockCookie cookie2 = extractCookie(reLogin);
        mockMvc.perform(post("/api/manage/disable?id=" + uniqueId).cookie(cookie2))
                .andExpect(status().isOk());

        // 被封禁后再次登录应失败
        MvcResult disableResult = mockMvc.perform(post("/api/login/do-login?id=" + uniqueId))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(body(disableResult)).contains("封禁");
    }

    @Test
    void advancedScenarios() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login/do-login?id=10001"))
                .andExpect(status().isOk()).andReturn();
        MockCookie cookie = extractCookie(loginResult);

        mockMvc.perform(post("/api/advanced/second-auth?service=pay").cookie(cookie))
                .andExpect(status().isOk());
        okWithCookie("/api/advanced/check-safe?service=pay", cookie);
        mockMvc.perform(post("/api/advanced/encrypt?password=123456").cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    void integrationScenarios() throws Exception {
        ok("/api/integration/public/info");
        ok("/api/integration/dao-type");
        okPost("/api/integration/header-token?id=10001");
    }

    @Test
    void ssoScenarios() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/sso/do-login?id=10001"))
                .andExpect(status().isOk()).andReturn();
        String ssoToken = extractField(loginResult, "ssoToken");
        String client1Token = extractField(loginResult, "client1Token");

        ok("/api/sso/is-login?ssoToken=" + ssoToken);
        ok("/api/sso/client1/info?client1Token=" + client1Token);
        okPost("/api/sso/logout?ssoToken=" + ssoToken);
    }

    @Test
    void oauth2Scenarios() throws Exception {
        MvcResult authResult = mockMvc.perform(get("/api/oauth2/authorize?id=10001&scope=read"))
                .andExpect(status().isOk()).andReturn();
        String code = extractField(authResult, "code");

        MvcResult tokenResult = mockMvc.perform(post("/api/oauth2/token?code=" + code))
                .andExpect(status().isOk()).andReturn();
        String accessToken = extractField(tokenResult, "accessToken");

        ok("/api/oauth2/userinfo?accessToken=" + accessToken);
        ok("/api/oauth2/openid?accessToken=" + accessToken);
    }

    @Test
    void jwtAndSignatureScenarios() throws Exception {
        MvcResult jwtResult = mockMvc.perform(post("/api/jwt/generate?loginId=10001"))
                .andExpect(status().isOk()).andReturn();
        String jwt = extractField(jwtResult, "jwt");

        mockMvc.perform(post("/api/jwt/verify?jwt=" + jwt))
                .andExpect(status().isOk());

        MvcResult signResult = mockMvc.perform(post("/api/signature/generate?userId=10001"))
                .andExpect(status().isOk()).andReturn();
        String signature = extractField(signResult, "signature");
        String timestamp = extractField(signResult, "timestamp");

        mockMvc.perform(post("/api/signature/verify?userId=10001&timestamp=" + timestamp + "&signature=" + signature))
                .andExpect(status().isOk());
    }

    @Test
    void quickLoginScenarios() throws Exception {
        okPost("/api/quick/phone-login?phone=13800138000");
        okPost("/api/quick/scan-login?scanCode=SCAN-OK");
        ok("/api/quick/intro");
    }

    private void ok(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        assertThat(body(result)).contains("\"code\":200");
    }

    private void okPost(String url) throws Exception {
        MvcResult result = mockMvc.perform(post(url)).andExpect(status().isOk()).andReturn();
        assertThat(body(result)).contains("\"code\":200");
    }

    private void okWithCookie(String url, MockCookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get(url).cookie(cookie))
                .andExpect(status().isOk()).andReturn();
        assertThat(body(result)).contains("\"code\":200");
    }

    private String body(MvcResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private MockCookie extractCookie(MvcResult result) {
        MockHttpServletResponse response = result.getResponse();
        javax.servlet.http.Cookie[] cookies = response.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie c : cookies) {
                if ("satoken".equals(c.getName())) {
                    return new MockCookie(c.getName(), c.getValue());
                }
            }
        }
        throw new AssertionError("未找到 satoken Cookie");
    }

    private String extractField(MvcResult result, String field) {
        String content = body(result);
        int dataStart = content.indexOf("\"data\":");
        assertThat(dataStart).isGreaterThan(-1);
        String key = "\"" + field + "\"";
        int idx = content.indexOf(key, dataStart);
        assertThat(idx).isGreaterThan(-1);
        int colon = content.indexOf(":", idx);
        int start = content.indexOf("\"", colon + 1);
        int end = content.indexOf("\"", start + 1);
        return content.substring(start + 1, end);
    }
}
