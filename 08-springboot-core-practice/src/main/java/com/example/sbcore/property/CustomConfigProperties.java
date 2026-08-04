package com.example.sbcore.property;

import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix = "custom")
public class CustomConfigProperties {

    @NotNull
    private String appId;

    @Range(min = 1, max = 65535)
    private Integer threadPoolSize;

    private Boolean enabled;
}
