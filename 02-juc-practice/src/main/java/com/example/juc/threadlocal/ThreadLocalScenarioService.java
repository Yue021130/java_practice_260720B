package com.example.juc.threadlocal;

import com.example.juc.common.NamedThreadFactory;
import com.example.juc.common.ScenarioLog;
import com.example.juc.common.ScenarioResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ThreadLocal 场景业务实现。
 *
 * 面试点：ThreadLocalMap 是 Thread 的属性；key 是 ThreadLocal 弱引用、value 强引用；
 * 线程池下必须 finally remove，否则串号 + 内存泄漏；InheritableThreadLocal 在线程池复用下失效。
 */
@Slf4j
@Service
public class ThreadLocalScenarioService {

    private static final ThreadLocal<String> TRACE_HOLDER = new ThreadLocal<>();

    /**
     * traceId 全链路传递。
     *
     * 八股：MDC 日志追踪底层就是 ThreadLocal；finally 里 remove 是最佳实践。
     */
    public ScenarioResult traceChain() {
        ScenarioLog log = new ScenarioLog();
        AtomicReference<String> traceA = new AtomicReference<>();
        AtomicReference<String> traceB = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread reqA = new Thread(() -> {
            try {
                start.await();
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                TRACE_HOLDER.set(traceId);
                traceA.set(traceId);
                log.log("拦截器 set traceId=%s", traceId);
                controllerLayer(log);
                serviceLayer(log);
                daoLayer(log);
                String afterRemove = TRACE_HOLDER.get();
                TRACE_HOLDER.remove();
                log.log("请求 A finally remove，remove 前=%s remove 后=null（验证=%s）", traceId, afterRemove == null ? "提前为空" : "正常");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "trace-req-A");
        reqA.setDaemon(true);

        Thread reqB = new Thread(() -> {
            try {
                start.await();
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                TRACE_HOLDER.set(traceId);
                traceB.set(traceId);
                log.log("拦截器 set traceId=%s", traceId);
                controllerLayer(log);
                serviceLayer(log);
                daoLayer(log);
                TRACE_HOLDER.remove();
                log.log("请求 B finally remove，traceId=%s 已清理", traceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "trace-req-B");
        reqB.setDaemon(true);

        reqA.start();
        reqB.start();
        start.countDown();
        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("traceA", traceA.get());
        data.put("traceB", traceB.get());
        data.put("consistent", traceA.get() != null && traceB.get() != null && !traceA.get().equals(traceB.get()));
        return ScenarioResult.of(log, "两条请求链路 traceId 互不干扰，finally remove 防止泄漏", data);
    }

    private void controllerLayer(ScenarioLog log) {
        log.log("controller 层读取 traceId=%s", TRACE_HOLDER.get());
    }

    private void serviceLayer(ScenarioLog log) {
        log.log("service 层读取 traceId=%s", TRACE_HOLDER.get());
    }

    private void daoLayer(ScenarioLog log) {
        log.log("dao 层读取 traceId=%s", TRACE_HOLDER.get());
    }

    /**
     * 泄漏与串号演示。
     *
     * 八股：value 强引用、线程不死 -> 内存泄漏；线程池复用 -> 脏数据被下一个任务读到。
     */
    public ScenarioResult leakDemo() {
        ScenarioLog log = new ScenarioLog();
        ExecutorService pool = Executors.newSingleThreadExecutor(new NamedThreadFactory("leak-"));
        AtomicBoolean dirtyRead = new AtomicBoolean(false);
        AtomicBoolean cleanAfterRemove = new AtomicBoolean(false);

        // 阶段一：不 remove，下一个任务串号
        pool.submit(() -> {
            TRACE_HOLDER.set("用户A");
            log.log("任务1 set 用户 A，但不 remove");
        });
        pool.submit(() -> {
            String v = TRACE_HOLDER.get();
            if ("用户A".equals(v)) {
                dirtyRead.set(true);
                log.log("任务2（同一线程复用）get=%s -> 串号事故！", v);
            }
        });

        // 阶段二：正确 remove
        pool.submit(() -> {
            TRACE_HOLDER.set("用户B");
            try {
                log.log("任务3 set 用户 B，业务处理…");
            } finally {
                TRACE_HOLDER.remove();
                log.log("任务3 finally remove");
            }
        });
        pool.submit(() -> {
            String v = TRACE_HOLDER.get();
            if (v == null) {
                cleanAfterRemove.set(true);
                log.log("任务4（同一线程复用）get=null -> remove 有效，未串号");
            }
        });

        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dirtyRead", dirtyRead.get());
        data.put("cleanAfterRemove", cleanAfterRemove.get());
        return ScenarioResult.of(log, "不 remove 会串号，finally remove 后下一个任务读不到脏数据", data);
    }

    /**
     * 线程池下上下文丢失与补救。
     *
     * 八股：InheritableThreadLocal 只在线程创建时拷贝，线程池复用失效；阿里 TTL 用装饰器思想解决。
     */
    public ScenarioResult poolContext() {
        ScenarioLog log = new ScenarioLog();
        ExecutorService pool = Executors.newFixedThreadPool(2, new NamedThreadFactory("pool-ctx-"));
        AtomicBoolean lostInPool = new AtomicBoolean(false);
        AtomicBoolean manualPassOk = new AtomicBoolean(false);
        AtomicBoolean decoratorOk = new AtomicBoolean(false);

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TRACE_HOLDER.set(traceId);
        log.log("父线程 set traceId=%s", traceId);

        CountDownLatch done = new CountDownLatch(3);

        // 1) 直接提交：上下文丢失
        pool.submit(() -> {
            String v = TRACE_HOLDER.get();
            if (v == null) {
                lostInPool.set(true);
                log.log("异步任务直接 get=null：父线程 ThreadLocal 不会自动传给线程池");
            }
            done.countDown();
        });

        // 2) 手动传递
        pool.submit(() -> {
            TRACE_HOLDER.set(traceId);
            try {
                String v = TRACE_HOLDER.get();
                if (traceId.equals(v)) {
                    manualPassOk.set(true);
                    log.log("手动传递：任务内 set traceId=%s 成功", v);
                }
            } finally {
                TRACE_HOLDER.remove();
            }
            done.countDown();
        });

        // 3) 装饰器模式（TransmittableThreadLocal 的核心思想）
        Runnable original = () -> {
            String v = TRACE_HOLDER.get();
            if (traceId.equals(v)) {
                decoratorOk.set(true);
                log.log("装饰器传递：任务内 get=%s 成功", v);
            }
        };
        Runnable wrapped = new TraceContextRunnable(original, traceId);
        pool.submit(wrapped);
        // 等待任务执行完再 remove 装饰器里的 remove
        pool.submit(() -> done.countDown());

        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        TRACE_HOLDER.remove();
        pool.shutdownNow();

        Map<String, Object> data = new HashMap<>();
        data.put("lostInPool", lostInPool.get());
        data.put("manualPassOk", manualPassOk.get());
        data.put("decoratorOk", decoratorOk.get());
        return ScenarioResult.of(log, "线程池下 ThreadLocal 会丢失，手动传参或装饰器封装是标准补救方案", data);
    }

    /**
     * 装饰器：提交时 capture 上下文，执行时 replay，结束后 remove。
     */
    private static class TraceContextRunnable implements Runnable {
        private final Runnable delegate;
        private final String traceId;

        TraceContextRunnable(Runnable delegate, String traceId) {
            this.delegate = delegate;
            this.traceId = traceId;
        }

        @Override
        public void run() {
            TRACE_HOLDER.set(traceId);
            try {
                delegate.run();
            } finally {
                TRACE_HOLDER.remove();
                log.info("装饰器 finally remove traceId");
            }
        }
    }
}
