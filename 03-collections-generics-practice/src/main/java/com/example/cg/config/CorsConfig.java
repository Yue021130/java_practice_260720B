package com.example.cg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置。
 *
 * 说明：
 * - 前端 Vue 3 开发服务器运行在 http://localhost:5176。
 * - 后端运行在 http://localhost:8083。
 * - 浏览器同源策略会阻止跨域请求，因此需要配置 CORS。
 *   （vite.config.js 里也配了 /api 代理，开发时二者任选其一即可。）
 *
 * 注意：生产环境请把 allowedOrigins 改为真实域名，不建议用 *。
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // 允许前端开发服务器访问
                        .allowedOrigins("http://localhost:5176", "http://127.0.0.1:5176")
                        // 允许的 HTTP 方法
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // 允许所有请求头
                        .allowedHeaders("*")
                        // 允许携带 cookie（本项目暂时不用，但开启无坏处）
                        .allowCredentials(true)
                        // 预检请求缓存 1 小时
                        .maxAge(3600);
            }
        };
    }
}
