package com.example.sbcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

import com.example.sbcore.property.AppProperties;
import com.example.sbcore.property.CustomConfigProperties;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({AppProperties.class, CustomConfigProperties.class})
public class SbCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbCoreApplication.class, args);
    }

}
