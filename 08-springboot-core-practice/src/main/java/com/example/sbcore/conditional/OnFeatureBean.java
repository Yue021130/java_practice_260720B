package com.example.sbcore.conditional;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Data
@Component
@ConditionalOnProperty(prefix = "feature", name = "demo", havingValue = "true", matchIfMissing = true)
public class OnFeatureBean {

    private final String description = "@ConditionalOnProperty 条件满足时注册";

    @PostConstruct
    public void init() {
        System.out.println("OnFeatureBean 已注册，feature.demo=true 或缺省");
    }
}
