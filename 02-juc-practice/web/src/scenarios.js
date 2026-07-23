// 场景目录：前端渲染的数据源，endpoint 与后端 Spring Boot 接口一一对应
export const modules = [
  {
    key: 'locks',
    name: '锁',
    desc: 'ReentrantLock / 读写锁 / StampedLock / Condition / LockSupport',
    scenarios: [
      {
        id: 'transfer-deadlock',
        title: '转账死锁与解决',
        scene: '两个账户互相转账，各持一把账户锁，两个线程加锁顺序不一致就会互相等待造成死锁；tryLock 超时放弃 + 按序加锁可以破解。',
        bagu: [
          '死锁四条件：互斥、持有并等待、不可剥夺、循环等待，破坏其一即可',
          '避免死锁常用手段：按固定顺序加锁、tryLock 超时主动放弃已有锁',
          'synchronized 由 JVM 托管简单可靠；Lock 功能更强（可中断/超时/公平/多 Condition）但需手动 unlock'
        ],
        endpoint: '/api/locks/transfer-deadlock',
        params: [
          {
            name: 'mode',
            label: '运行模式',
            type: 'select',
            default: 'trylock',
            options: [
              { value: 'deadlock', label: '演示死锁' },
              { value: 'trylock', label: 'tryLock 解决' }
            ]
          }
        ]
      },
      {
        id: 'fair-vs-nonfair',
        title: '公平锁 vs 非公平锁',
        scene: '秒杀闸机放行：公平锁严格排队先来后到，非公平锁允许新线程插队抢锁，吞吐量更高但可能饥饿。',
        bagu: [
          'ReentrantLock 底层是 AQS：state 记录锁状态，CLH 双向队列管理等待线程',
          '非公平更快的原因：省去唤醒挂起线程的上下文切换，刚释放锁的线程很可能缓存还热',
          'ReentrantLock 默认是非公平锁，new ReentrantLock(true) 才是公平锁'
        ],
        endpoint: '/api/locks/fair-vs-nonfair',
        params: [
          { name: 'threads', label: '线程数', type: 'number', default: 8, min: 1, max: 32 }
        ]
      },
      {
        id: 'interruptible',
        title: '可中断锁 lockInterruptibly',
        scene: '运维线程等资源等太久，可以被 Ctrl+C 式中断优雅退出并释放资源；synchronized 阻塞时做不到这一点。',
        bagu: [
          'synchronized 拿不到锁只能无限阻塞，不响应中断',
          'Lock 三个独有特性：可中断 lockInterruptibly、超时 tryLock、公平锁选项',
          'lockInterruptibly 在等锁期间响应中断并抛出 InterruptedException'
        ],
        endpoint: '/api/locks/interruptible',
        params: []
      },
      {
        id: 'readwrite-cache',
        title: '读写锁商品缓存',
        scene: '商品详情缓存读多写少，ReentrantReadWriteLock 让并发读互不阻塞，只有写操作时才独占。',
        bagu: [
          '语义：读读共享、读写互斥、写写互斥，适合读多写少场景',
          '锁降级：持有写锁时可获取读锁再释放写锁，保证修改对自己后续读可见',
          '不允许锁升级（读锁→写锁）：多个读线程同时升级会互相等待造成死锁'
        ],
        endpoint: '/api/locks/readwrite-cache',
        params: [
          { name: 'readers', label: '读线程数', type: 'number', default: 10, min: 1, max: 32 },
          { name: 'writers', label: '写线程数', type: 'number', default: 2, min: 1, max: 8 }
        ]
      },
      {
        id: 'stamped-optimistic',
        title: 'StampedLock 乐观读',
        scene: '物流坐标被高频读取、偶尔更新，乐观读完全无锁更快，validate 校验失败再升级为悲观读锁。',
        bagu: [
          '加锁/解锁返回 stamp 戳，解锁与校验都必须带上这个戳',
          '乐观读 tryOptimisticRead 不加锁，读完后必须 validate 确认期间没有写操作',
          'StampedLock 不可重入，重入会导致死锁，这是最常见的坑'
        ],
        endpoint: '/api/locks/stamped-optimistic',
        params: []
      },
      {
        id: 'condition-buffer',
        title: 'Condition 手写生产者消费者',
        scene: '外卖后厨出餐、骑手取餐：不用 BlockingQueue，用 ReentrantLock + 两个 Condition 手写有界缓冲区。',
        bagu: [
          'Condition 对比 wait/notify：一个锁可挂多个等待队列，能精确唤醒生产者或消费者',
          'await 必须放在 while 循环里判断条件，防止虚假唤醒和 signalAll 后的条件失效',
          'notEmpty / notFull 两个队列分离，避免 notifyAll 唤醒无关线程的性能浪费'
        ],
        endpoint: '/api/locks/condition-buffer',
        params: [
          { name: 'producers', label: '生产者数', type: 'number', default: 3, min: 1, max: 16 },
          { name: 'consumers', label: '消费者数', type: 'number', default: 3, min: 1, max: 16 },
          { name: 'items', label: '生产总量', type: 'number', default: 10, min: 1, max: 50 }
        ]
      },
      {
        id: 'locksupport',
        title: 'LockSupport 演示',
        scene: '先 unpark 后 park 不会阻塞（许可机制），对比 wait/notify 必须先 wait 后 notify 的顺序敏感问题。',
        bagu: [
          '许可只有 0 和 1 两种状态，多次 unpark 不会累积，park 消耗一个许可',
          'AQS 底层就是靠 LockSupport.park/unpark 挂起和唤醒线程',
          'park 响应中断但不抛异常，需要通过 Thread.interrupted() 自行判断'
        ],
        endpoint: '/api/locks/locksupport',
        params: []
      }
    ]
  },
  {
    key: 'atomic',
    name: '原子类',
    desc: 'CAS / AtomicLong / LongAdder / ABA',
    scenarios: [
      {
        id: 'stock-compare',
        title: '库存扣减三路对比',
        scene: '秒杀库存扣减：普通 int 会超卖、synchronized 结果正确但慢、AtomicLong 既正确又快，三路同场实测对比。',
        bagu: [
          'CAS 底层是 Unsafe.compareAndSwap，对应 CPU 的 cmpxchg 原子指令',
          '竞争激烈时 CAS 自旋空转会大量消耗 CPU，这是它的主要开销',
          'CAS 三大问题：ABA、自旋开销、只能保证单个变量的原子性'
        ],
        endpoint: '/api/atomic/stock-compare',
        params: [
          { name: 'threads', label: '线程数', type: 'number', default: 8, min: 1, max: 32 },
          { name: 'times', label: '每线程扣减次数', type: 'number', default: 10000, min: 1000, max: 100000 }
        ]
      },
      {
        id: 'longadder-vs-atomic',
        title: 'LongAdder vs AtomicLong 压测',
        scene: '全站 PV 计数：高并发下 AtomicLong 让所有线程竞争同一个变量，LongAdder 用 Cell 分段打散热点。',
        bagu: [
          'LongAdder 核心思想：base + Cell[] 分段，不同线程更新不同 Cell，降低竞争',
          'sum() 是对各段求和的弱一致快照，求和过程中不阻塞写入',
          '低并发下 AtomicLong 反而更划算，LongAdder 只在高竞争时优势明显'
        ],
        endpoint: '/api/atomic/longadder-vs-atomic',
        params: [
          { name: 'threads', label: '线程数', type: 'number', default: 32, min: 1, max: 64 },
          { name: 'times', label: '每线程累加次数', type: 'number', default: 100000, min: 10000, max: 1000000 }
        ]
      },
      {
        id: 'aba',
        title: 'ABA 问题与修复',
        scene: '余额 100 → 扣 50 → 又充 50，CAS 看值没变就以为没人动过；AtomicStampedReference 加版本号可以识破。',
        bagu: [
          'ABA 危害经典案例：无锁链表的栈，值没变但节点结构已被改动',
          '解法：AtomicStampedReference 版本号 / AtomicMarkableReference 标记位',
          '业务里也可以直接用带版本号的字段（类似乐观锁 version）规避'
        ],
        endpoint: '/api/atomic/aba',
        params: []
      },
      {
        id: 'order-state',
        title: '无锁订单状态机',
        scene: '支付回调和超时取消同时到达，CAS 保证「待支付 → 已支付」只成功一次，防止重复支付。',
        bagu: [
          '用 CAS 做状态迁移：compareAndSet(期望旧状态, 新状态) 天然保证迁移合法性',
          'AtomicReference 可以让任意对象引用具备 CAS 能力',
          '数据库层的等价做法是 update ... where status = 期望状态，利用 affected rows 判断'
        ],
        endpoint: '/api/atomic/order-state',
        params: []
      }
    ]
  },
  {
    key: 'container',
    name: '并发容器',
    desc: 'CHM / CopyOnWrite / DelayQueue / 跳表',
    scenarios: [
      {
        id: 'hashmap-accident',
        title: 'HashMap 并发事故',
        scene: '多线程往 HashMap 灌数据：size 对不上、数据丢失，甚至触发更诡异的异常，演示为什么它不能并发用。',
        bagu: [
          'JDK7 头插法扩容时可能形成环形链表，get 时死循环打满 CPU',
          'JDK8 改尾插解决了成环，但并发 put 仍会互相覆盖丢数据、size 不准',
          '并发场景必须用 ConcurrentHashMap 或 Collections.synchronizedMap'
        ],
        endpoint: '/api/container/hashmap-accident',
        params: [
          { name: 'threads', label: '线程数', type: 'number', default: 8, min: 1, max: 32 },
          { name: 'keys', label: '灌入键数', type: 'number', default: 10000, min: 1000, max: 100000 }
        ]
      },
      {
        id: 'chm-ops',
        title: 'ConcurrentHashMap 原子操作三件套',
        scene: 'putIfAbsent 做缓存防击穿、computeIfAbsent 懒加载、merge 词频聚合，一个接口串起三个高频用法。',
        bagu: [
          'JDK7 Segment 分段锁；JDK8 改为 CAS + synchronized 锁桶头节点，锁粒度更细',
          'size 统计：无竞争用 baseCount，有竞争分散到 CounterCell 求和（思想同 LongAdder）',
          'computeIfAbsent 的 mapping 函数里不要再改同一个 map，可能死锁'
        ],
        endpoint: '/api/container/chm-ops',
        params: []
      },
      {
        id: 'cow-whitelist',
        title: 'CopyOnWrite 白名单',
        scene: 'IP 白名单读多写少：读线程遍历完全不阻塞，读到的是修改那一刻的快照，写操作复制新数组。',
        bagu: [
          '写时复制：增删改先拷贝整个数组再替换引用，读操作永远无锁',
          '迭代器是弱一致的，遍历的是创建迭代器时的快照，不会抛 CME',
          '不适合写多场景：每次写都复制数组，内存翻倍且 GC 压力大'
        ],
        endpoint: '/api/container/cow-whitelist',
        params: []
      },
      {
        id: 'delayqueue-order',
        title: 'DelayQueue 订单超时取消',
        scene: '下单 30 分钟未支付自动取消：任务实现 Delayed 放入 DelayQueue，到期自动出队被消费线程处理。',
        bagu: [
          '内部是 PriorityQueue 按到期时间排序，到期元素才能被 take 出来',
          '采用 Leader-Follower 模式减少不必要的等待唤醒',
          '生产上常用替代方案：Redis ZSet 轮询、时间轮（Netty HashedWheelTimer）、MQ 延迟消息'
        ],
        endpoint: '/api/container/delayqueue-order',
        params: [
          { name: 'orders', label: '订单数', type: 'number', default: 5, min: 1, max: 20 },
          { name: 'timeoutMs', label: '超时时间(ms)', type: 'number', default: 2000, min: 500, max: 10000 }
        ]
      },
      {
        id: 'blockingqueue-family',
        title: 'BlockingQueue 家族对比',
        scene: 'Array / Linked / Synchronous / Priority 四种阻塞队列同场实测，直观看到容量、排序、直接交接的差异。',
        bagu: [
          '线程池 workQueue 选型直接影响拒绝策略与扩容行为的触发时机',
          'SynchronousQueue 不存储元素，put 必须等 take，生产消费直接交接',
          'ArrayBlockingQueue 有界一把锁；LinkedBlockingQueue 默认容量 Integer.MAX_VALUE 易 OOM'
        ],
        endpoint: '/api/container/blockingqueue-family',
        params: []
      },
      {
        id: 'skiplist-leaderboard',
        title: '跳表排行榜',
        scene: '游戏积分排行榜：多线程并发更新分数，同时实时查询 TopN，ConcurrentSkipListMap 轻松胜任。',
        bagu: [
          '跳表用多层索引实现 O(logN) 查找，空间换时间',
          '并发友好：插入用 CAS 无锁化完成，不需要全局锁',
          '对比 TreeMap/TreeSet 需要外部加锁才能并发使用，吞吐量差距明显'
        ],
        endpoint: '/api/container/skiplist-leaderboard',
        params: [
          { name: 'players', label: '玩家数', type: 'number', default: 20, min: 5, max: 100 }
        ]
      }
    ]
  },
  {
    key: 'sync',
    name: '同步工具',
    desc: 'CountDownLatch / CyclicBarrier / Semaphore / Exchanger',
    scenarios: [
      {
        id: 'latch-startup',
        title: 'CountDownLatch 并行启动自检',
        scene: '应用启动时并行检查 DB / Redis / MQ 等依赖，全部就绪后才放行流量，主线程 await 等待。',
        bagu: [
          '一次性不可复用，计数器归零后无法再重置',
          'countDown 可在任意线程调用，await 阻塞直到计数归零',
          '基于 AQS 共享模式实现：state 即计数，归零时唤醒所有等待线程'
        ],
        endpoint: '/api/sync/latch-startup',
        params: []
      },
      {
        id: 'barrier-report',
        title: 'CyclicBarrier 分片报表汇总',
        scene: '4 个线程各算一个季度财报，全员到达屏障后自动触发合并年报的回调；跑两轮证明它可复用。',
        bagu: [
          '与 CountDownLatch 三大区别：可复用、计数归各线程自己、支持到达回调 barrierAction',
          '某线程中断或超时会导致屏障 broken，其他线程收到 BrokenBarrierException',
          '基于 ReentrantLock + Condition 实现，而非 AQS 共享模式'
        ],
        endpoint: '/api/sync/barrier-report',
        params: [
          { name: 'rounds', label: '运行轮数', type: 'number', default: 2, min: 1, max: 3 }
        ]
      },
      {
        id: 'semaphore-limit',
        title: 'Semaphore 接口限流',
        scene: '10 个请求同时打向只允许 3 并发的下游短信服务，拿不到许可的排队等待或快速失败降级。',
        bagu: [
          '连接池、限流器的本质就是 Semaphore 许可发放与归还',
          'tryAcquire(超时) 拿不到可以走降级逻辑，避免线程无限堆积',
          'fair 参数控制是否按排队顺序发许可，公平模式吞吐更低'
        ],
        endpoint: '/api/sync/semaphore-limit',
        params: [
          { name: 'requests', label: '请求数', type: 'number', default: 10, min: 1, max: 50 },
          { name: 'permits', label: '许可数', type: 'number', default: 3, min: 1, max: 10 }
        ]
      },
      {
        id: 'exchanger-reconcile',
        title: 'Exchanger 双人对账',
        scene: '两个财务各核一半账单，在交换点互换数据做交叉比对，是双线程数据交换的专用工具。',
        bagu: [
          '必须成双成对：一方调用 exchange 后阻塞，直到另一方也到达才互换数据',
          '带超时的 exchange 版本可以避免一方永远干等',
          '典型应用：双缓冲区、遗传算法、流水线设计中的数据交接'
        ],
        endpoint: '/api/sync/exchanger-reconcile',
        params: []
      }
    ]
  },
  {
    key: 'future',
    name: '异步编排',
    desc: 'CompletableFuture / Future / ForkJoin / 定时调度',
    scenarios: [
      {
        id: 'cf-detail',
        title: 'CompletableFuture 电商详情页聚合',
        scene: '商品、库存、评价、推荐 4 个远程调用并行编排，allOf 汇总结果，单个服务异常时降级、整体超时兜底。',
        bagu: [
          'supplyAsync 不传线程池默认走 ForkJoinPool 公共池，生产必须换自定义池',
          'thenCompose 用于串行有依赖的任务，thenCombine 用于并行无依赖的合并',
          'exceptionally/handle 会吞掉异常，链路里漏了它异常会静默丢失'
        ],
        endpoint: '/api/future/cf-detail',
        params: []
      },
      {
        id: 'old-future',
        title: '老式 Future 对比',
        scene: '同样的聚合需求用 Future.get 串行写法实现，阻塞又难看，反衬 CompletableFuture 的编排能力。',
        bagu: [
          'Future 两大局限：get() 阻塞等待，无法声明式地做链式/组合编排',
          'isDone + 轮询的方式既浪费 CPU 代码又丑陋',
          'Future.cancel 只能中断还在运行的任务，无法表达任务间的依赖关系'
        ],
        endpoint: '/api/future/old-future',
        params: []
      },
      {
        id: 'forkjoin-sum',
        title: 'ForkJoin 分治求和',
        scene: '1 亿个元素求和：递归拆分任务利用多核并行计算，与单线程循环直接对比耗时。',
        bagu: [
          '工作窃取算法：空闲线程从其他线程队列尾部偷任务，减少竞争',
          '拆分阈值太小任务开销大于收益，太大则并行度不足，需实测调优',
          'parallelStream 底层也走公共 ForkJoinPool，多个 IO 型任务会互相拖累'
        ],
        endpoint: '/api/future/forkjoin-sum',
        params: [
          { name: 'size', label: '数组规模', type: 'number', default: 100000000, min: 1000000, max: 200000000 }
        ]
      },
      {
        id: 'scheduled-compare',
        title: '定时任务两种模式对比',
        scene: 'scheduleAtFixedRate 按固定频率追赶执行 vs scheduleWithFixedDelay 顺延执行，时间线上直观看漂移差异。',
        bagu: [
          'FixedRate 以上次开始时间为基准，任务超时会追赶补跑；FixedDelay 以上次结束时间为基准顺延',
          '任务抛异常后调度会静默停止后续执行，任务体必须 try-catch',
          '线程池大小为 1 时多个任务会互相阻塞，注意 poolSize 配置'
        ],
        endpoint: '/api/future/scheduled-compare',
        params: []
      }
    ]
  },
  {
    key: 'threadlocal',
    name: '线程上下文',
    desc: 'ThreadLocal 原理与坑',
    scenarios: [
      {
        id: 'trace-chain',
        title: 'traceId 全链路传递',
        scene: '拦截器生成 traceId 存入 ThreadLocal，业务层、DAO 层日志自动带上同一个 ID，finally 中 remove 收尾。',
        bagu: [
          '每个线程持有 ThreadLocalMap，key 是 ThreadLocal 弱引用、value 是强引用',
          'key 被 GC 后 value 成为访问不到的脏数据，线程长存即内存泄漏',
          '日志框架 MDC 的全链路追踪就是这个原理（底层也是 ThreadLocal）'
        ],
        endpoint: '/api/threadlocal/trace-chain',
        params: []
      },
      {
        id: 'leak-demo',
        title: '泄漏与串号演示',
        scene: '线程池复用线程且不 remove：下一个请求读到上一个用户的身份信息，经典串号事故；并对比正确写法。',
        bagu: [
          '内存泄漏根因：value 是强引用而线程（线程池里）长期存活，value 无法回收',
          '线程池场景用完必须 remove，否则脏数据会被下一个任务读到',
          '最佳实践：在 finally 块中 remove，或使用 try-with-resources 封装'
        ],
        endpoint: '/api/threadlocal/leak-demo',
        params: []
      },
      {
        id: 'pool-context',
        title: '线程池下上下文丢失',
        scene: '主线程的 traceId 传不进异步任务：演示手动传递、装饰器包装任务两种补救方案。',
        bagu: [
          'InheritableThreadLocal 只在线程创建时拷贝一次，线程池复用线程时拿不到新值',
          'TransmittableThreadLocal（阿里 TTL）通过装饰任务在提交时快照、执行时回放解决',
          '手动传递方案：提交任务时取出上下文，任务体内 set/remove 成对使用'
        ],
        endpoint: '/api/threadlocal/pool-context',
        params: []
      }
    ]
  },
  {
    key: 'base',
    name: 'JMM 基础',
    desc: 'volatile / DCL / 死锁检测',
    scenarios: [
      {
        id: 'volatile-visibility',
        title: 'volatile 可见性',
        scene: '工人线程空转等待 stop 信号：普通变量可能永远看不到主线程的修改，volatile 让修改立刻可见。',
        bagu: [
          'JMM 模型：主内存 + 各线程工作内存，变量读写要先经过工作内存同步',
          'happens-before 规则保证：对一个 volatile 变量的写，先行发生于后续对它的读',
          'volatile 通过内存屏障禁止指令重排，保证有序性'
        ],
        endpoint: '/api/base/volatile-visibility',
        params: []
      },
      {
        id: 'volatile-not-atomic',
        title: 'volatile 不保证原子性',
        scene: '8 个线程对同一个 volatile int 各加 1000 次，最终结果远小于 8000，实测打脸。',
        bagu: [
          'i++ 是读-改-写三步操作，volatile 只保证每步读到最新值，不保证整体原子',
          '并发下两个线程可能读到同一个旧值，各自 +1 后写回，丢失一次更新',
          '原子性要靠 synchronized / Lock / AtomicInteger 等原子类保证'
        ],
        endpoint: '/api/base/volatile-not-atomic',
        params: [
          { name: 'threads', label: '线程数', type: 'number', default: 8, min: 1, max: 32 },
          { name: 'times', label: '每线程自增次数', type: 'number', default: 1000, min: 100, max: 10000 }
        ]
      },
      {
        id: 'dcl-singleton',
        title: 'DCL 双重检查单例',
        scene: '多线程同时获取单例：没有 volatile 的 DCL 可能拿到半初始化对象，现场演示并给出正确写法。',
        bagu: [
          'instance = new 非原子：分配内存 → 初始化对象 → 引用赋值，后两步可能重排',
          '重排后其他线程可能拿到未初始化的引用（半初始化对象）',
          'volatile 禁止该重排，是 DCL 正确性的关键，两次 null 检查各有职责'
        ],
        endpoint: '/api/base/dcl-singleton',
        params: []
      },
      {
        id: 'deadlock-detect',
        title: '死锁制造与自动检测',
        scene: '故意制造一个死锁，然后用 ThreadMXBean.findDeadlockedThreads() 自动揪出死锁线程并打印栈。',
        bagu: [
          '线上排查三板斧：jps 找进程、jstack 打线程栈、ThreadMXBean 编程式检测',
          'jstack 输出里关注 BLOCKED 状态线程和 "Found one Java-level deadlock" 字样',
          '监控侧可定时调用 findDeadlockedThreads 做死锁告警'
        ],
        endpoint: '/api/base/deadlock-detect',
        params: []
      }
    ]
  }
]

// 汇总统计，供界面展示
export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
