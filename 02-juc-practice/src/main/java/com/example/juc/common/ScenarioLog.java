package com.example.juc.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 场景日志收集器：多线程安全地收集带时间戳 + 线程名的步骤日志。
 *
 * 用法：每个场景方法开头 new 一个 ScenarioLog，业务线程里调 log.log("...")，
 * 它会自动记录「距场景开始多少毫秒、哪个线程、说了什么」，
 * 最后通过 ScenarioResult.of(log, summary, data) 汇总返回给前端时间线。
 *
 * 线程安全：内部用 CopyOnWriteArrayList——写（日志追加）远少于读（结束时一次性读），
 * 且这里写并发很低，开销可忽略；这本身就是本专题讲的容器选型思想。
 */
public class ScenarioLog {

    private final long startNanos = System.nanoTime();
    private final List<ScenarioResult.Step> steps = new CopyOnWriteArrayList<>();

    /** 记录一条日志，自动带上当前耗时偏移和当前线程名 */
    public void log(String message) {
        steps.add(new ScenarioResult.Step(elapsedMs(), Thread.currentThread().getName(), message));
    }

    /** 带占位符的便捷写法：log.log("线程 %s 抢到锁", name) */
    public void log(String fmt, Object... args) {
        log(String.format(fmt, args));
    }

    /** 距场景开始的毫秒数 */
    public long elapsedMs() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** 取全部日志（场景结束时调用一次） */
    public List<ScenarioResult.Step> getSteps() {
        return steps;
    }
}
