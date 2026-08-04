package com.example.sbcore.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotNull
    private String name;

    private String version;

    @Valid
    private User user;

    @Data
    public static class User {

        @NotNull
        private String userName;

        private Integer age;

        private String email;
    }
}
