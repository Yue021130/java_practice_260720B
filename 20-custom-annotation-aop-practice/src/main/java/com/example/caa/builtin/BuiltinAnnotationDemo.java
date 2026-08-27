package com.example.caa.builtin;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Java 内置注解演示。
 *
 * <p>对应微信公众号原文第 1 节：@Override、@Deprecated、@SuppressWarnings 是每天会用到的内置注解。</p>
 */
@Component
public class BuiltinAnnotationDemo {

    /**
     * @Override 确保方法确实覆盖了父类方法，防止笔误。
     *
     * <p>如果这里写成 {@code toSting()} 而没有 @Override，编译器不会报错，但逻辑上并没有覆盖 Object.toString()；
     * 加上 @Override 后，编译器会立即提示错误。</p>
     */
    @Override
    public String toString() {
        return "BuiltinAnnotationDemo{正确覆盖了 toString()}";
    }

    /**
     * @Deprecated 标记该方法不推荐使用，调用时会产生编译警告。
     *
     * @deprecated 请使用 {@link #newMethod()} 替代
     */
    @Deprecated
    public String oldMethod() {
        return "这是老方法，不建议使用";
    }

    /**
     * 替代旧方法的新方法。
     */
    public String newMethod() {
        return "这是新方法";
    }

    /**
     * @SuppressWarnings 用于在明确知道警告无害时抑制编译器警告。
     *
     * <p>这里演示一个经典场景：从 raw type 转换为泛型列表。suppress 范围越小越好，
     * 一般放在方法或局部变量上，不要整个类都抑制。</p>
     */
    @SuppressWarnings("unchecked")
    public List<String> suppressWarningDemo() {
        // 模拟一个返回 raw type 的第三方方法
        Object rawList = new ArrayList<String>();
        ((ArrayList<String>) rawList).add("hello");
        return (List<String>) rawList;
    }
}
