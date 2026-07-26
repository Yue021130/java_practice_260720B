package com.example.mp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot + MyBatis-Plus 全场景实践学习项目启动类。
 *
 * 本专题把 MyBatis-Plus 常用注解与核心能力包装成可运行的现实业务场景，
 * 配合前端面板观察实体映射、BaseMapper、IService、条件构造器、分页、逻辑删除、乐观锁等行为与面试考点。
 */
@SpringBootApplication
public class MybatisPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisPlusApplication.class, args);
    }
}
