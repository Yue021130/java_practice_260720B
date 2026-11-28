package com.example.prod.e2e;
import com.example.prod.security.JwtService;
import com.example.prod.security.SecurityUserDetailsService;
import com.example.prod.security.TokenBlacklist;
import com.example.prod.service.AuthService;
import com.example.prod.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端测试（金字塔 10% 层）：完整 HTTP 安全链路。
 *
 * <p>所有 DB 依赖（业务 Service、审计仓库、健康检查）用 @MockBean 替换：
 * 本类目标是安全过滤器链而非业务/持久化——审计仓库必须 mock，
 * 否则 Spring Security 的 AuthenticationAuditListener 会把认证事件
 * 写向真实数据库。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.capacity=1000",
        "app.rate-limit.refill-per-minute=1000",
        "management.health.db.enabled=false"
})
class SecurityChainE2ETest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private TokenBlacklist tokenBlacklist;

    @MockBean
    private AuthService authService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private SecurityUserDetailsService userDetailsService;
    @MockBean
    private AuditEventRepository auditEventRepository;
    @MockBean
    private com.example.prod.observability.DatabaseHealthIndicator databaseHealthIndicator;

    private void mockUser(String username) {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(
                new User(username, "x",
                        Collections.singletonList(new SimpleGrantedAuthority("order:read"))));
        when(databaseHealthIndicator.health()).thenReturn(Health.up().build());
    }

    @Test
    @DisplayName("未带 token → 401")
    void unauthenticated() throws Exception {
        mockUser("admin");
        mockMvc.perform(get("/api/order/page")
                        .param("current", "1").param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("有效 Bearer token → 放行（200）")
    void validToken() throws Exception {
        mockUser("admin");
        String token = jwtService.generate("admin");
        mockMvc.perform(get("/api/order/page")
                        .param("current", "1").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("登出后被黑名单撤销的 token → 401")
    void blacklistedToken() throws Exception {
        mockUser("admin");
        String token = jwtService.generate("admin");
        String jti = jwtService.parse(token).getBody().getId();
        tokenBlacklist.add(jti, 3600_000);

        mockMvc.perform(get("/api/order/page")
                        .param("current", "1").param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("白名单 /actuator/health 未认证也可达（200）")
    void loginPermitted() throws Exception {
        mockUser("admin");
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
