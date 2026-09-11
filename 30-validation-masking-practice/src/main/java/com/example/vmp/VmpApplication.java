package com.example.vmp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.vmp.mapper")
public class VmpApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmpApplication.class, args);
    }
}
