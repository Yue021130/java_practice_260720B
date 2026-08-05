package com.example.exception.advanced;

/**
 * 支持受检异常的函数式接口。
 *
 * Java 标准 Consumer 不能抛 checked exception，这里演示如何扩展。
 *
 * @param <T> 入参类型
 */
@FunctionalInterface
public interface CheckedConsumer<T> {

    void accept(T t) throws Exception;
}
