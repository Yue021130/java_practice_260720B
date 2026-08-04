package com.example.sbcore.conditional;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Data
@Component
@ConditionalOnClass(name = "org.springframework.data.redis.core.RedisTemplate")
public class OnClassBean {

    private final String description = "@ConditionalOnClass 检测到 RedisTemplate 类路径时注册";
}
