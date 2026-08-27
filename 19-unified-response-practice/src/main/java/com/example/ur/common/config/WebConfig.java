package com.example.ur.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 配置：调整 MessageConverter 顺序，解决 String 返回值被自动包装后的转换器匹配问题。
 *
 * <p>背景：Controller 返回 String 时，Spring 默认优先使用 StringHttpMessageConverter。
 * 当 ResponseBodyAdvice 把 String 包装成 Result 对象后，需要 JSON 转换器才能正确序列化。
 * 把 MappingJackson2HttpMessageConverter 放到前面，可以兜底处理这种情况。</p>
 *
 * <p>注意：本模块同时在 {@link com.example.ur.common.advice.GlobalResponseAdvice} 里对 String 做了
 * 手动 Jackson 序列化的防御性处理，两者互为保险。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 把 Jackson JSON 转换器放到第一位，确保对象类型优先走 JSON 序列化。
        // 这样即使 String 被包装成 Result，也能被正确识别为对象类型。
        converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
