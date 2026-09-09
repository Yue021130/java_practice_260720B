package com.example.rbac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.rbac.mapper")
@SpringBootApplication
public class RbacPermissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacPermissionApplication.class, args);
    }
}
