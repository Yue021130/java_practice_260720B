package com.example.prod.integration;

import com.example.prod.dto.LoginDTO;
import com.example.prod.security.JwtService;
import com.example.prod.security.TokenBlacklist;
import com.example.prod.service.AuthService;
import com.example.prod.service.OrderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试（金字塔 20% 层）：Testcontainers 拉起真实 MySQL，
 * 验证 登录(BCrypt+真实库) → 带权限调 Service → 登出黑名单撤销 完整链路。
 *
 * <p>disabledWithoutDocker=true：本机没有 Docker 时自动跳过执行
 * （CI 或装了 Docker 的机器上正常运行）。这是 Testcontainers 的关键
 * 价值——SQL 方言、schema、事务行为面对真实数据库验证，
 * 避免「本地通过、生产爆炸」。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class OrderIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("prod_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    /**
     * Testcontainers 只提供空库，schema 和种子数据由测试自己初始化
     * （与生产 schema.sql 同源维护；演示为控制规模内联 DDL）。
     */
    @BeforeAll
    static void initSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sys_user (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(100) NOT NULL, nickname VARCHAR(50), role VARCHAR(20), create_time DATETIME)");
            stmt.execute("CREATE TABLE IF NOT EXISTS biz_order (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(50) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, dept_id BIGINT, user_id BIGINT, " +
                    "status INT NOT NULL, create_time DATETIME)");
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_event (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, principal VARCHAR(50), type VARCHAR(50), " +
                    "data TEXT, timestamp TIMESTAMP)");
            // admin / 123456（BCrypt）
            stmt.execute("INSERT INTO sys_user(username, password, nickname, role, create_time) " +
                    "VALUES('admin', '$2a$10$Elov7OraQ1re/uEuZ868GOmBG8Pz0H.aJZKU3uE89OImnD4BrBfti', '管理员', 'ADMIN', NOW())");
        }
    }

    @Autowired
    private AuthService authService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private TokenBlacklist tokenBlacklist;

    /** 模拟 JWT 过滤器完成后的安全上下文（@PreAuthorize 靠它判定） */
    private void loginAsAdmin() {
        java.util.List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("order:write"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new User("admin", "x", authorities), null, authorities));
    }

    @Test
    @DisplayName("完整链路：登录拿 token → 建单分页 → 登出后 jti 进黑名单")
    void fullFlow() {
        // 1. 登录（真实 BCrypt 校验 + 真实数据库）
        LoginDTO login = new LoginDTO();
        login.setUsername("admin");
        login.setPassword("123456");
        com.example.prod.dto.LoginVO loginVO = authService.login(login, "127.0.0.1");
        assertNotNull(loginVO.getToken());

        // 2. 模拟过滤器解析 token 后建立的安全上下文
        loginAsAdmin();

        // 3. 业务写操作（真实 MySQL + @PreAuthorize 代理）
        com.example.prod.dto.OrderSaveDTO saveDTO = new com.example.prod.dto.OrderSaveDTO();
        saveDTO.setOrderNo("IT-ORDER-001");
        saveDTO.setAmount(new BigDecimal("99.50"));
        saveDTO.setStatus(1);
        orderService.save(saveDTO, "admin");

        // 4. 读操作
        com.baomidou.mybatisplus.core.metadata.IPage<com.example.prod.dto.OrderVO> page = orderService.page(1, 10, "IT-ORDER-001");
        assertTrue(page.getTotal() >= 1);

        // 5. 登出 → jti 进黑名单，旧 token 立即失效
        authService.logout("Bearer " + loginVO.getToken());
        String jti = jwtService.parse(loginVO.getToken()).getBody().getId();
        assertTrue(tokenBlacklist.contains(jti), "登出后 jti 必须在黑名单中");

        SecurityContextHolder.clearContext();
    }
}
