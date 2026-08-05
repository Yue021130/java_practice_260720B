package com.example.exception.basics;

import lombok.extern.slf4j.Slf4j;

/**
 * 教学用 AutoCloseable 资源：close 方法也会抛异常，用于演示 Suppressed Exception。
 */
@Slf4j
public class ResourceWithCloseException implements AutoCloseable {

    private final String name;

    public ResourceWithCloseException(String name) {
        this.name = name;
        log.info("[{}] 已打开", name);
    }

    public void doSomething() {
        throw new RuntimeException(name + " 业务执行失败");
    }

    @Override
    public void close() throws Exception {
        log.info("[{}] 关闭时抛异常", name);
        throw new Exception(name + " 关闭失败");
    }
}
