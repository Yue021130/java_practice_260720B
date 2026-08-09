package com.example.unsafe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * Unsafe 单例装配：把 {@code sun.misc.Unsafe} 变成 Spring 容器里的一个 Bean。
 *
 * <h3>为什么不能直接调用 Unsafe.getUnsafe()？</h3>
 * <pre>
 *   private static final Unsafe theUnsafe = new Unsafe();
 *   public static Unsafe getUnsafe() {
 *       ClassLoader cl = Unsafe.class.getClassLoader();      // Bootstrap，为 null
 *       if (cl != null) {                                     // 普通应用类的类加载器 != null
 *           throw new SecurityException(“Unsafe”);
 *       }
 *       return theUnsafe;
 *   }
 * </pre>
 * 即：只有 BootstrapClassLoader（启动类加载器，取值为 null）加载的类才能拿到 Unsafe，
 * JDK 内部（rt.jar 里的类）调用没问题，我们普通应用直接调会抛 {@code SecurityException}。
 *
 * <h3>标准破解姿势（教学用）</h3>
 * 反射读取 Unsafe 内部唯一实例字段 {@code theUnsafe}：
 * <pre>
 *   Field f = Unsafe.class.getDeclaredField(“theUnsafe”);
 *   f.setAccessible(true);
 *   Unsafe unsafe = (Unsafe) f.get(null);
 * </pre>
 * JDK 17 及以下 `sun.misc` 包由 `jdk.unsupported` 模块无条件导出，因此可以正常反射；
 * JDK 9+ 需要模块 `jdk.unsupported` 可见（默认可见），本方法在 JDK 8 / 11 / 17 均可用。
 *
 * <p>这也是面试高频题：<b>如何获取 Unsafe 实例？</b>——答案就是上面这段反射。
 */
@Slf4j
@Configuration
public class UnsafeConfig {

    /** 反射拿到 Unsafe 内部单例，注册为 Spring Bean，供所有 Service 注入使用 */
    @Bean
    public sun.misc.Unsafe unsafe() {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
            log.info("Unsafe 实例获取成功（反射 theUnsafe）：{}", unsafe);
            return unsafe;
        } catch (Exception e) {
            throw new IllegalStateException("反射获取 sun.misc.Unsafe 失败：" + e.getMessage(), e);
        }
    }
}
