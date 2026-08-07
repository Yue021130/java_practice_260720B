package com.example.threadpooladvanced.service;

import com.example.threadpooladvanced.config.ThreadPoolAdvancedConfig;
import com.example.threadpooladvanced.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池深度实验服务：管理池、运行实验、收集指标。
 */
@Slf4j
@Service
public class ThreadPoolExperimentService {

    @Autowired
    @Qualifier("cpuPool")
    private ThreadPoolExecutor cpuPool;

    @Autowired
    @Qualifier("ioPool")
    private ThreadPoolExecutor ioPool;

    @Autowired
    @Qualifier("tinyPool")
    private ThreadPoolExecutor tinyPool;

    private final Map<String, ThreadPoolExecutor> predefinedPools = new LinkedHashMap<>();
    private final Map<String, ThreadPoolExecutor> customPools = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> customRejectedCounters = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        predefinedPools.put("cpuPool", cpuPool);
        predefinedPools.put("ioPool", ioPool);
        predefinedPools.put("tinyPool", tinyPool);
    }

    @PreDestroy
    public void destroy() {
        shutdownAll(predefinedPools);
        shutdownAll(customPools);
    }

    private void shutdownAll(Map<String, ThreadPoolExecutor> pools) {
        pools.values().forEach(pool -> {
            try {
                pool.shutdown();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    public List<PoolMetricsDto> getAllMetrics() {
        List<PoolMetricsDto> list = new ArrayList<>();
        predefinedPools.forEach((id, pool) -> list.add(toMetrics(id, id, pool)));
        customPools.forEach((id, pool) -> list.add(toMetrics(id, id, pool)));
        return list;
    }

    public PoolMetricsDto getMetrics(String poolId) {
        ThreadPoolExecutor pool = findPool(poolId);
        return toMetrics(poolId, poolId, pool);
    }

    private PoolMetricsDto toMetrics(String poolId, String poolName, ThreadPoolExecutor pool) {
        PoolMetricsDto dto = new PoolMetricsDto();
        dto.setPoolId(poolId);
        dto.setPoolName(poolName);
        dto.setCorePoolSize(pool.getCorePoolSize());
        dto.setMaximumPoolSize(pool.getMaximumPoolSize());
        dto.setPoolSize(pool.getPoolSize());
        dto.setActiveCount(pool.getActiveCount());
        dto.setQueueSize(pool.getQueue().size());
        dto.setQueueRemainingCapacity(pool.getQueue().remainingCapacity());
        dto.setCompletedTaskCount(pool.getCompletedTaskCount());
        dto.setTaskCount(pool.getTaskCount());
        dto.setKeepAliveTimeSeconds(pool.getKeepAliveTime(TimeUnit.SECONDS));
        dto.setShutdown(pool.isShutdown());
        dto.setTerminated(pool.isTerminated());
        if (pool.getRejectedExecutionHandler() instanceof ThreadPoolAdvancedConfig.CountingRejectedHandler) {
            dto.setRejectedCount(((ThreadPoolAdvancedConfig.CountingRejectedHandler) pool.getRejectedExecutionHandler()).getRejectedCount());
        }
        return dto;
    }

    public PoolMetricsDto submitToPredefined(String poolId, SubmitTaskRequest request) {
        ThreadPoolExecutor pool = findPool(poolId);
        doSubmit(pool, request.getCount(), request.getTaskDurationMs());
        return toMetrics(poolId, poolId, pool);
    }

    public PoolMetricsDto createCustomPool(CustomPoolRequest request) {
        if (customPools.containsKey(request.getPoolId())) {
            throw new IllegalArgumentException("poolId 已存在: " + request.getPoolId());
        }
        TimeUnit unit = TimeUnit.valueOf(request.getTimeUnit().toUpperCase());
        BlockingQueue<Runnable> queue = createQueue(request.getQueueType(), request.getQueueCapacity());
        RejectedExecutionHandler handler = createHandler(request.getRejectionPolicy(), request.getPoolId());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                request.getCorePoolSize(),
                request.getMaximumPoolSize(),
                request.getKeepAliveTime(),
                unit,
                queue,
                ThreadPoolAdvancedConfig.namedThreadFactory(request.getThreadFactoryPrefix()),
                handler
        );
        customPools.put(request.getPoolId(), pool);
        return toMetrics(request.getPoolId(), request.getPoolId(), pool);
    }

    public PoolMetricsDto submitToCustom(String poolId, SubmitTaskRequest request) {
        ThreadPoolExecutor pool = customPools.get(poolId);
        if (pool == null) {
            throw new IllegalArgumentException("自定义线程池不存在: " + poolId);
        }
        doSubmit(pool, request.getCount(), request.getTaskDurationMs());
        return toMetrics(poolId, poolId, pool);
    }

    public ShutdownResultDto shutdownPool(String poolId, boolean now) {
        ThreadPoolExecutor pool = findPool(poolId);
        List<String> pending = new ArrayList<>();
        if (now) {
            List<Runnable> tasks = pool.shutdownNow();
            tasks.forEach(t -> pending.add(t.getClass().getSimpleName()));
        } else {
            pool.shutdown();
        }
        ShutdownResultDto dto = new ShutdownResultDto();
        dto.setPoolId(poolId);
        dto.setShutdown(pool.isShutdown());
        dto.setTerminated(pool.isTerminated());
        dto.setPendingTasks(pending);
        dto.setMessage(now ? "已调用 shutdownNow()" : "已调用 shutdown()，等待队列任务执行完毕");
        return dto;
    }

    private void doSubmit(ThreadPoolExecutor pool, int count, long durationMs) {
        for (int i = 0; i < count; i++) {
            final int taskNo = i + 1;
            pool.submit(() -> {
                try {
                    Thread.sleep(durationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.debug("任务 {} 执行完成", taskNo);
            });
        }
    }

    private ThreadPoolExecutor findPool(String poolId) {
        ThreadPoolExecutor pool = predefinedPools.get(poolId);
        if (pool != null) {
            return pool;
        }
        pool = customPools.get(poolId);
        if (pool != null) {
            return pool;
        }
        throw new IllegalArgumentException("线程池不存在: " + poolId);
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<Runnable> createQueue(String type, int capacity) {
        switch (type) {
            case "ArrayBlockingQueue":
                return new ArrayBlockingQueue<>(capacity);
            case "LinkedBlockingQueue":
                return new LinkedBlockingQueue<>(capacity);
            case "SynchronousQueue":
                return new SynchronousQueue<>();
            case "PriorityBlockingQueue":
                return new PriorityBlockingQueue<>(capacity);
            case "DelayQueue":
                // DelayQueue 要求元素实现 Delayed 接口，教学中使用原始类型演示
                return new DelayQueue();
            case "LinkedBlockingDeque":
                return new LinkedBlockingDeque<>(capacity);
            case "LinkedTransferQueue":
                return new LinkedTransferQueue<>();
            default:
                throw new IllegalArgumentException("不支持的队列类型: " + type);
        }
    }

    private RejectedExecutionHandler createHandler(String policy, String poolId) {
        switch (policy) {
            case "AbortPolicy":
                return new ThreadPoolExecutor.AbortPolicy();
            case "CallerRunsPolicy":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "DiscardPolicy":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DiscardOldestPolicy":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            case "CountingPolicy":
                customRejectedCounters.put(poolId, new AtomicInteger(0));
                return new ThreadPoolAdvancedConfig.CountingRejectedHandler();
            default:
                throw new IllegalArgumentException("不支持的拒绝策略: " + policy);
        }
    }

    // ==================== 队列实验 ====================

    public List<QueueTypeDto> listQueueTypes() {
        List<QueueTypeDto> list = new ArrayList<>();
        list.add(buildQueueType("ArrayBlockingQueue", "数组 + 双指针环形队列", true, "指定容量",
                "有界队列，公平锁可选，适合固定容量场景", "线程池默认推荐，队列满后触发扩容/拒绝", "容量过小容易拒绝，过大可能堆积 OOM"));
        list.add(buildQueueType("LinkedBlockingQueue", "单向链表", true, "未指定则为 Integer.MAX_VALUE",
                "有界/无界两种，默认无界，生产消费可并行", "FixedThreadPool / SingleThreadExecutor 默认队列", "默认无界会导致 maxPoolSize 失效，有 OOM 风险"));
        list.add(buildQueueType("SynchronousQueue", "不存储元素", true, "0",
                "每个插入操作必须等待移除，吞吐量高", "CachedThreadPool 默认队列", "需要配合较大 maxPoolSize，否则容易拒绝"));
        list.add(buildQueueType("PriorityBlockingQueue", "平衡二叉堆", false, "Integer.MAX_VALUE",
                "按优先级出队，非 FIFO", "需要任务有优先级时", "无界，可能 OOM；需实现 Comparable 或 Comparator"));
        list.add(buildQueueType("DelayQueue", "PriorityQueue + Delayed", false, "Integer.MAX_VALUE",
                "只有 delay 到期的元素才能出队", "定时/延迟任务", "无界，元素必须实现 Delayed 接口"));
        list.add(buildQueueType("LinkedBlockingDeque", "双向链表", true, "未指定则为 Integer.MAX_VALUE",
                "支持 FIFO / FILO，双端操作", "需要双端控制的场景", "默认无界同样有 OOM 风险"));
        list.add(buildQueueType("LinkedTransferQueue", "CAS + 链表", false, "Integer.MAX_VALUE",
                "transfer 方法可同步传递元素", "高并发零拷贝传递", "无界，使用在 ThreadPoolExecutor 中与 LinkedBlockingQueue 行为接近"));
        return list;
    }

    private QueueTypeDto buildQueueType(String type, String underlying, boolean bounded, String defaultCapacity,
                                        String feature, String useCase, String risk) {
        QueueTypeDto dto = new QueueTypeDto();
        dto.setType(type);
        dto.setUnderlying(underlying);
        dto.setBounded(bounded);
        dto.setDefaultCapacity(defaultCapacity);
        dto.setFeature(feature);
        dto.setUseCase(useCase);
        dto.setRisk(risk);
        return dto;
    }

    public QueueExperimentResult experimentQueue(String type, int capacity, int submitCount) {
        BlockingQueue<Runnable> queue = createQueue(type, capacity);
        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < submitCount; i++) {
            try {
                boolean ok = queue.offer(() -> {}, 100, TimeUnit.MILLISECONDS);
                if (ok) {
                    accepted++;
                } else {
                    rejected++;
                }
            } catch (InterruptedException e) {
                rejected++;
                Thread.currentThread().interrupt();
            }
        }
        QueueExperimentResult result = new QueueExperimentResult();
        result.setQueueType(type);
        result.setSubmitted(submitCount);
        result.setAccepted(accepted);
        result.setRejected(rejected);
        result.setNote(String.format("%s 在容量 %d 时接受 %d 个、拒绝 %d 个", type, capacity, accepted, rejected));
        return result;
    }

    // ==================== 拒绝策略实验 ====================

    public List<RejectionTypeDto> listRejectionTypes() {
        List<RejectionTypeDto> list = new ArrayList<>();
        list.add(buildRejectionType("AbortPolicy", "丢弃任务并抛出 RejectedExecutionException", "需要调用方感知失败", "默认策略，调用方必须处理异常"));
        list.add(buildRejectionType("CallerRunsPolicy", "由提交任务的线程（调用者）自己执行", "变相限流、反压上游", "会降低主线程/接口吞吐量"));
        list.add(buildRejectionType("DiscardPolicy", "静默丢弃任务，不抛异常", "允许部分任务丢失", "数据可能丢失，排查困难，慎用"));
        list.add(buildRejectionType("DiscardOldestPolicy", "丢弃最老任务，再尝试提交", "重视新数据", "可能丢弃关键老任务"));
        list.add(buildRejectionType("CountingPolicy(自定义)", "计数 + 抛异常", "教学观察", "生产通常配合日志/告警"));
        return list;
    }

    private RejectionTypeDto buildRejectionType(String policy, String behavior, String useCase, String risk) {
        RejectionTypeDto dto = new RejectionTypeDto();
        dto.setPolicy(policy);
        dto.setBehavior(behavior);
        dto.setUseCase(useCase);
        dto.setRisk(risk);
        return dto;
    }

    public RejectionExperimentResult experimentRejection(String policy, int submitCount) {
        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger rejected = new AtomicInteger(0);
        RejectedExecutionHandler handler;
        switch (policy) {
            case "AbortPolicy":
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case "CallerRunsPolicy":
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            case "DiscardPolicy":
                handler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case "DiscardOldestPolicy":
                handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            default:
                handler = new ThreadPoolExecutor.AbortPolicy();
        }

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                ThreadPoolAdvancedConfig.namedThreadFactory("rejection"),
                handler
        );

        for (int i = 0; i < submitCount; i++) {
            final int taskNo = i;
            try {
                pool.submit(() -> {
                    executed.incrementAndGet();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RejectionExperimentResult result = new RejectionExperimentResult();
        result.setPolicy(policy);
        result.setSubmitted(submitCount);
        result.setExecuted(executed.get());
        result.setRejected(rejected.get());
        result.setBehavior(getPolicyBehavior(policy));
        result.setMessage(String.format("提交 %d 个，执行 %d 个，拒绝/异常 %d 个", submitCount, executed.get(), rejected.get()));
        return result;
    }

    private String getPolicyBehavior(String policy) {
        switch (policy) {
            case "AbortPolicy":
                return "抛出 RejectedExecutionException";
            case "CallerRunsPolicy":
                return "提交线程（调用者）自己执行";
            case "DiscardPolicy":
                return "静默丢弃";
            case "DiscardOldestPolicy":
                return "丢弃最老任务再尝试";
            default:
                return "未知";
        }
    }

    // ==================== Executors 工厂 ====================

    public List<ExecutorsTypeDto> listExecutorsTypes() {
        List<ExecutorsTypeDto> list = new ArrayList<>();
        list.add(buildExecutorsType("FixedThreadPool", "Executors.newFixedThreadPool(n)",
                "n", "n", "LinkedBlockingQueue（无界）",
                "只有核心线程，无超时回收，队列无界", "控制并发线程数", "任务堆积可能导致 OOM"));
        list.add(buildExecutorsType("SingleThreadExecutor", "Executors.newSingleThreadExecutor()",
                "1", "1", "LinkedBlockingQueue（无界）",
                "单核心线程，任务顺序执行", "需要顺序执行的任务", "同样使用无界队列，可能 OOM"));
        list.add(buildExecutorsType("CachedThreadPool", "Executors.newCachedThreadPool()",
                "0", "Integer.MAX_VALUE", "SynchronousQueue",
                "无核心线程，非核心线程 60s 回收", "大量短生命周期异步任务", "可能创建海量线程，导致 OOM / 系统崩溃"));
        list.add(buildExecutorsType("ScheduledThreadPool", "Executors.newScheduledThreadPool(n)",
                "n", "Integer.MAX_VALUE", "DelayedWorkQueue",
                "支持定时和周期任务", "定时任务、心跳、调度", "max 无界，同样存在线程数爆炸风险"));
        return list;
    }

    private ExecutorsTypeDto buildExecutorsType(String type, String methodCall, String core, String max,
                                                String queue, String feature, String useCase, String risk) {
        ExecutorsTypeDto dto = new ExecutorsTypeDto();
        dto.setType(type);
        dto.setMethodCall(methodCall);
        dto.setCorePoolSize(core);
        dto.setMaximumPoolSize(max);
        dto.setQueue(queue);
        dto.setFeature(feature);
        dto.setUseCase(useCase);
        dto.setRisk(risk);
        return dto;
    }

    public Map<String, Object> demoExecutors(String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        ExecutorService service;
        switch (type) {
            case "FixedThreadPool":
                service = Executors.newFixedThreadPool(3);
                map.put("corePoolSize", 3);
                map.put("maximumPoolSize", 3);
                map.put("queueType", "LinkedBlockingQueue（无界）");
                break;
            case "SingleThreadExecutor":
                service = Executors.newSingleThreadExecutor();
                map.put("corePoolSize", 1);
                map.put("maximumPoolSize", 1);
                map.put("queueType", "LinkedBlockingQueue（无界）");
                break;
            case "CachedThreadPool":
                service = Executors.newCachedThreadPool();
                map.put("corePoolSize", 0);
                map.put("maximumPoolSize", "Integer.MAX_VALUE");
                map.put("queueType", "SynchronousQueue");
                break;
            case "ScheduledThreadPool":
                service = Executors.newScheduledThreadPool(2);
                map.put("corePoolSize", 2);
                map.put("maximumPoolSize", "Integer.MAX_VALUE");
                map.put("queueType", "DelayedWorkQueue");
                break;
            default:
                throw new IllegalArgumentException("不支持的工厂类型: " + type);
        }
        service.shutdown();
        map.put("message", "已创建并立即关闭演示池，生产中建议直接使用 ThreadPoolExecutor");
        return map;
    }

    // ==================== 源码流程与生命周期 ====================

    public List<SourceFlowStepDto> getExecuteFlow() {
        List<SourceFlowStepDto> list = new ArrayList<>();
        list.add(buildFlowStep(1, "ctl 读状态 & 工作线程数", "ThreadPoolExecutor 用 int ctl 的高 3 位存运行状态，低 29 位存 worker 数量", "execute(Runnable)"));
        list.add(buildFlowStep(2, "核心线程数判断", "若 workerCount < corePoolSize，addWorker(command, true) 新增核心线程", "addWorker"));
        list.add(buildFlowStep(3, "入队尝试", "核心线程满后，任务进入 workQueue.offer(command)", "workQueue.offer"));
        list.add(buildFlowStep(4, "最大线程数判断", "若入队失败（有界队列满），且 workerCount < maximumPoolSize，addWorker(command, false) 新增非核心线程", "addWorker"));
        list.add(buildFlowStep(5, "执行拒绝策略", "队列满且线程数达上限，调用 handler.rejectedExecution", "RejectedExecutionHandler"));
        list.add(buildFlowStep(6, "Worker 取任务执行", "Worker 是内部类，继承 AQS 并实现 Runnable，runWorker 循环从队列 getTask 并执行", "runWorker / getTask"));
        list.add(buildFlowStep(7, "线程回收", "非核心线程 keepAliveTime 内没取到任务则被回收；allowCoreThreadTimeOut=true 时核心线程也回收", "processWorkerExit"));
        return list;
    }

    private SourceFlowStepDto buildFlowStep(int step, String title, String description, String keyMethod) {
        SourceFlowStepDto dto = new SourceFlowStepDto();
        dto.setStep(step);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setKeyMethod(keyMethod);
        return dto;
    }

    public List<LifecycleStateDto> getLifecycleStates() {
        List<LifecycleStateDto> list = new ArrayList<>();
        list.add(buildLifecycleState("RUNNING", -1, "接收新任务并处理队列任务", "是", "是", "否"));
        list.add(buildLifecycleState("SHUTDOWN", 0, "不接收新任务，但处理队列中剩余任务", "否", "是", "否"));
        list.add(buildLifecycleState("STOP", 1, "不接收新任务，不处理队列任务，中断正在执行的任务", "否", "否", "是"));
        list.add(buildLifecycleState("TIDYING", 2, "所有任务已终止，workerCount 为 0，准备调用 terminated()", "否", "否", "否"));
        list.add(buildLifecycleState("TERMINATED", 3, "terminated() 执行完成", "否", "否", "否"));
        return list;
    }

    private LifecycleStateDto buildLifecycleState(String state, int stateCode, String description,
                                                  String acceptNewTasks, String processQueueTasks, String interruptWorkers) {
        LifecycleStateDto dto = new LifecycleStateDto();
        dto.setState(state);
        dto.setStateCode(stateCode);
        dto.setDescription(description);
        dto.setAcceptNewTasks(acceptNewTasks);
        dto.setProcessQueueTasks(processQueueTasks);
        dto.setInterruptWorkers(interruptWorkers);
        return dto;
    }

    public List<WorkerIntroDto> getWorkerIntro() {
        List<WorkerIntroDto> list = new ArrayList<>();
        list.add(buildWorkerIntro("Worker 类", "继承 AQS，实现 Runnable，封装一个线程和首次任务",
                "thread、firstTask、completedTasks", "创建 → addWorker → runWorker → getTask → processWorkerExit"));
        list.add(buildWorkerIntro("runWorker", "循环从队列取任务并执行", "beforeExecute / afterExecute", "执行 firstTask 后再从队列取任务"));
        list.add(buildWorkerIntro("getTask", "从 workQueue 阻塞取任务，受 keepAliveTime 控制", "workQueue.poll / take", "超时未取到则返回 null，线程退出"));
        list.add(buildWorkerIntro("processWorkerExit", "Worker 退出时清理并判断是否需要补充线程", "completedAbruptly", "维护 workerCount 并尝试 addWorker"));
        return list;
    }

    private WorkerIntroDto buildWorkerIntro(String component, String role, String keyFields, String lifecycle) {
        WorkerIntroDto dto = new WorkerIntroDto();
        dto.setComponent(component);
        dto.setRole(role);
        dto.setKeyFields(keyFields);
        dto.setLifecycle(lifecycle);
        return dto;
    }
}
