package com.example.sbcore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ActuatorService {

    @Autowired
    private Environment environment;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> run() {
        Map<String, Object> data = new LinkedHashMap<>();

        String port = environment.getProperty("server.port", "8088");
        String baseUrl = "http://127.0.0.1:" + port;

        Map<String, Object> health = fetchJson(baseUrl + "/actuator/health");
        Map<String, Object> info = fetchJson(baseUrl + "/actuator/info");

        String include = environment.getProperty("management.endpoints.web.exposure.include", "health,info");

        data.put("health", health);
        data.put("info", info);
        data.put("exposedEndpoints", Arrays.asList(include.split(",")));

        List<String> sensitive = Arrays.asList("env", "heapdump", "httptrace", "threaddump", "configprops", "mappings");
        data.put("sensitiveEndpoints", sensitive);

        data.put("interviewNote",
                "Actuator 暴露运行期端点：health 健康检查、info 应用信息、metrics 指标、loggers 日志级别。" +
                "生产环境应使用 management.endpoints.web.exposure.include=health,info 最小化暴露，" +
                "并配合安全框架限制访问；env/heapdump/httptrace 等会泄露敏感信息，切勿直接对外暴露。");

        return data;
    }

    private Map<String, Object> fetchJson(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), Map.class);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.warn("获取 Actuator 端点失败：{}", msg);
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("error", msg);
            fallback.put("tip", "非真实运行环境或 Actuator 未启动时，HTTP 请求可能无法连接");
            return fallback;
        }
    }
}
