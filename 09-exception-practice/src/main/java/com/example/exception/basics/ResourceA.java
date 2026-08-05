package com.example.exception.basics;

import lombok.extern.slf4j.Slf4j;

/**
 * 教学用 AutoCloseable 资源 A。
 */
@Slf4j
public class ResourceA implements AutoCloseable {

    private final String name;

    public ResourceA(String name) {
        this.name = name;
        log.info("[{}] 已打开", name);
    }

    public void doSomething(boolean fail) {
        if (fail) {
            throw new RuntimeException(name + " 业务执行失败");
        }
        log.info("[{}] 业务执行成功", name);
    }

    @Override
    public void close() {
        log.info("[{}] 已关闭", name);
    }
}
