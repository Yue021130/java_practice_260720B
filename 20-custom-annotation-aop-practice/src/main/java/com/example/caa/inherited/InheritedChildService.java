package com.example.caa.inherited;

import org.springframework.stereotype.Service;

/**
 * 子类：自身没有标注 @InheritedMarker，但继承自标注了该注解的父类。
 */
@Service
public class InheritedChildService extends BaseAnnotatedService {

    public String childHello() {
        return "hello from child";
    }
}
