package com.example.cache.pitfall;

/**
 * SpEL key 陷阱演示用参数对象。
 *
 * 故意<b>不重写 toString</b>：Object 默认 toString 是「类名@内存地址」，
 * 两个「业务上等价」（同 id）但不同实例的对象，toString 一定不同——
 * 若缓存 key 写成 #param，就会得到两个不同 key，缓存永远不命中（见 10 章 key-demo）。
 */
public class Param {

    private final int id;
    private final String name;

    public Param(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // 不重写 toString()：默认 Object.toString() → com.example...Param@1f2e3d
}
