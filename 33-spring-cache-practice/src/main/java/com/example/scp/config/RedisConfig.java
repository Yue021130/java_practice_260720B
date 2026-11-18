package com.example.scp.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置（本章缓存的二级存储）。
 *
 * <p>默认 JdkSerializationRedisSerializer 的问题是：① 二进制不可读，
 * Redis 客户端里是一堆乱码，排查困难；② 要求缓存类实现 Serializable，
 * 强耦合。所以生产统一换成 JSON。</p>
 *
 * <p>键用 StringRedisSerializer（可读、便于 scan "product::*"），
 * 值用 GenericJackson2JsonRedisSerializer：序列化时写入全限定类名
 * （type info），反序列化不用传 Class 也能还原对象。
 * 代价是比 Jackson2JsonRedisSerializer&lt;T&gt; 多写类名、有安全面，
 * 因此必须显式注册 JavaTimeModule 支持 LocalDateTime 等 JSR-310 类型。</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 激活默认类型推断：序列化结果带 @class 字段，反序列化自描述
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        // LocalDateTime / LocalDate 等 JDK8 时间类型的序列化支持
        om.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(om);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
