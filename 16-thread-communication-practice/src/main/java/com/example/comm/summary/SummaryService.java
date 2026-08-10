package com.example.comm.summary;

import com.example.comm.support.CommLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 10. 选型总结与底层统一模型。
 *
 * 把前 09 章串起来：七大类 17 种方式各有适用场景，但底层殊途同归——
 * 除了共享变量轮询，其余本质都是「阻塞 + 等待队列 + 唤醒」，
 * 操作系统层面又全部落到 futex（Linux）的等待队列上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final CommLogStore logStore;

    /**
     * 七大类总览（对应用户给的分类）。
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", new LinkedHashMap<String, Object>() {{
            put("一、共享内存", "共享变量+volatile / AtomicXxx(CAS)：无锁、最轻，解决「看得见、改得对」");
            put("二、锁对象/等待通知", "Object wait/notify/notifyAll / Condition：每个对象/每把锁自带等待队列");
            put("三、线程协作控制", "join / interrupt / LockSupport：等结束、优雅打断、park 唤醒");
            put("四、JUC 同步工具", "CountDownLatch / CyclicBarrier / Semaphore / Exchanger / Phaser：AQS 封装的高层语义");
            put("五、阻塞队列", "BlockingQueue 家族：队列即载体，天然解耦 + 背压");
            put("六、异步结果传递", "Future/FutureTask / CompletableFuture：跨线程传返回值，支持编排");
            put("七、IO/其他通道", "PipedStream / Socket / 共享内存：管道流是标准答案，跨进程才用重的");
        }});
        result.put("count", "7 大类 17 种方式");
        result.put("tip", "七大类按「由轻到重、由线程内到跨进程」记忆：共享变量 → 锁 → 协作 → 同步工具 → 队列 → 异步 → 通道。");
        return result;
    }

    /**
     * 选型表：需求场景 → 首选方案。
     */
    public Map<String, Object> decisionTable() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", new LinkedHashMap<String, Object>() {{
            put("状态标志通知", "volatile / AtomicBoolean");
            put("精准等待-唤醒", "LockSupport / Condition");
            put("生产者-消费者", "BlockingQueue");
            put("等 N 个线程完成", "CountDownLatch / join");
            put("N 个线程齐头并进", "CyclicBarrier");
            put("限流", "Semaphore");
            put("拿异步计算结果", "CompletableFuture");
            put("两线程交换数据", "Exchanger");
            put("延迟任务/定时", "DelayQueue");
            put("优先级调度", "PriorityBlockingQueue");
            put("优雅停止线程", "interrupt + 协作退出");
        }});
        result.put("tip", "先想清楚需求再选：是「等结果」「等齐」「限流」还是「解耦」，对应工具一目了然。");
        return result;
    }

    /**
     * 底层统一模型：一切线程通信的殊途同归。
     */
    public Map<String, Object> unifiedModel() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", new LinkedHashMap<String, Object>() {{
            put("共享变量轮询", "唯一例外：不阻塞，靠轮询 + volatile 可见性（CPU 空转，适合极短等待）");
            put("wait/notify", "对象 monitor 的等待队列（WaitSet）");
            put("Condition / AQS", "CLH 变体队列 / 条件队列（ConditionObject）");
            put("BlockingQueue", "队列本身就是等待载体（数组/链表 + Condition）");
            put("LockSupport", "permit 信号（每线程一个 0/1 许可）");
        }});
        result.put("layers", new String[]{
                "业务层：CountDownLatch / CompletableFuture / BlockingQueue ... 管「语义」",
                "框架层：AQS（AbstractQueuedSynchronizer）管「排队 + 唤醒」",
                "原语层：LockSupport（park/unpark）管「线程挂起/唤醒」",
                "系统层：futex（Linux fast user-space mutex）—— 用户态快路径 + 内核等待队列，同一套模型"
        });
        result.put("oneSentence", "除了共享变量轮询，其余所有方式的本质都是「阻塞 + 等待队列 + 唤醒」；"
                + "操作系统层面又全部落到 futex 的等待队列上，一层层抽象，殊途同归。");
        result.put("tip", "面试收尾金句：把这句「殊途同归」讲出来，说明你不是背 API 而是理解了本质。");
        return result;
    }

    /**
     * 总结速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("oneLiner", new LinkedHashMap<String, Object>() {{
            put("共享内存", "volatile 解决看得见、CAS 解决改得对");
            put("等待通知", "wait/notify 一个队列广播，Condition 一锁多队列点对点");
            put("协作控制", "join 等结束、interrupt 打招呼、LockSupport 精准唤醒");
            put("同步工具", "Latch 一等多、Barrier 多等多、Semaphore 限流、Exchanger 交换、Phaser 合体");
            put("阻塞队列", "队列即通信，put/take 阻塞即背压");
            put("异步结果", "Future 拿单个结果，CompletableFuture 编排多个");
            put("其他通道", "PipedStream 是知识点，跨进程才用 Socket/共享内存");
        }});
        result.put("interviewFlow", "被问「线程间通信方式」：先按七大类铺开 → 每类一句话核心 → 最后抛「底层都是阻塞+等待队列+唤醒，"
                + "落到 futex」——层次感拉满。");
        result.put("tip", "本模块 01~09 章每个工具都有可运行的演示，配合这张选型表就是完整的学习闭环。");
        return result;
    }
}
