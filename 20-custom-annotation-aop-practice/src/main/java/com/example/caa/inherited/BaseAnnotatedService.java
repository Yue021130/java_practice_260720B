package com.example.caa.inherited;

import com.example.caa.annotation.InheritedMarker;

/**
 * 父类：标注了 @InheritedMarker。
 *
 * <p>由于 @InheritedMarker 声明了 @Inherited，子类 {@link InheritedChildService}
 * 在运行期可以通过反射获取到该注解。</p>
 */
@InheritedMarker("from-parent")
public abstract class BaseAnnotatedService {

    public String hello() {
        return "hello from base";
    }
}
