export const modules = [
  {
    key: 'quickstart',
    name: '快速上手',
    desc: 'fire-and-forget、CompletableFuture、Future 超时获取',
    scenarios: [
      {
        id: 'fire-forget',
        title: '无返回值 fire-and-forget',
        scene: '调用 @Async void 方法后立即返回，任务在后台线程执行。',
        bagu: [
          '@Async 默认使用 SimpleAsyncTaskExecutor，每次新建线程',
          'void 方法无法把异常抛给调用方，需 AsyncUncaughtExceptionHandler',
          '生产环境必须自定义 ThreadPoolTaskExecutor'
        ],
        endpoint: '/api/async/fire-forget',
        params: []
      },
      {
        id: 'completable-future',
        title: 'CompletableFuture 返回值',
        scene: '用 supplyAsync / thenApply / join 做异步编排。',
        bagu: [
          'CompletableFuture 支持 thenApply / thenCompose / allOf / anyOf',
          'join() 不抛受检异常，get() 抛 InterruptedException / ExecutionException',
          '避免在 then 链里执行阻塞 IO'
        ],
        endpoint: '/api/async/completable-future',
        params: []
      },
      {
        id: 'future-timeout',
        title: 'Future + 超时获取',
        scene: '用 Future.get(timeout, TimeUnit) 避免永久阻塞。',
        bagu: [
          'Future.get() 无参版本会永久阻塞',
          '生产环境必须带超时，超时后可用 cancel(true) 中断任务',
          'CompletableFuture.get(timeout, TimeUnit) 同样适用'
        ],
        endpoint: '/api/async/future-timeout',
        params: []
      }
    ]
  },
  {
    key: 'pool',
    name: '线程池配置',
    desc: 'ThreadPoolTaskExecutor 参数、多线程池隔离、拒绝策略',
    scenarios: [
      {
        id: 'pool-config',
        title: 'ThreadPoolTaskExecutor 参数',
        scene: '查看 default/cpu/io 三池的核心参数配置。',
        bagu: [
          'corePoolSize：常驻线程数',
          'maxPoolSize：队列满后最大扩容线程数',
          'queueCapacity：任务队列大小，满后才会扩容到 max',
          'keepAliveTime：非核心线程空闲回收时间'
        ],
        endpoint: '/api/async/pool-config',
        params: []
      },
      {
        id: 'custom-executor',
        title: '多线程池与 @Async("name")',
        scene: 'CPU 密集与 IO 密集任务使用不同线程池，避免互相挤占。',
        bagu: [
          'CPU 密集型：core = max = CPU 核心数，避免上下文切换',
          'IO 密集型：max 可更大，因为线程多数时间在等待',
          '@Async("beanName") 指定自定义线程池'
        ],
        endpoint: '/api/async/custom-executor',
        params: []
      },
      {
        id: 'rejected',
        title: '队列打满与拒绝策略',
        scene: '向 core=max=2、queue=5 的小池提交 20 个任务，观察拒绝量。',
        bagu: [
          'AbortPolicy：抛 RejectedExecutionException',
          'CallerRunsPolicy：让调用线程执行，起到削峰作用',
          'DiscardPolicy / DiscardOldestPolicy：静默丢弃',
          '建议自定义计数器监控拒绝量'
        ],
        endpoint: '/api/async/rejected',
        params: []
      }
    ]
  },
  {
    key: 'exception-proxy',
    name: '异常与代理',
    desc: '异步异常处理与 Spring AOP 同类调用代理坑',
    scenarios: [
      {
        id: 'exception',
        title: '异步异常处理',
        scene: 'void @Async 异常走 AsyncUncaughtExceptionHandler；有返回值异常由调用方处理。',
        bagu: [
          'void 方法使用 AsyncUncaughtExceptionHandler 统一捕获',
          'Future/CompletableFuture 用 exceptionally / handle 处理',
          '不要忽略异步任务的异常，否则问题难排查'
        ],
        endpoint: '/api/async/exception',
        params: []
      },
      {
        id: 'self-invocation',
        title: '同类内部调用不生效',
        scene: 'outer() 内部 this.inner() 调用的是目标对象，不是代理对象。',
        bagu: [
          'Spring AOP 代理在 Bean 外部方法调用时才生效',
          'this.inner() 绕过代理，导致 @Async 不生效',
          '解决：注入自身代理，或把 inner 拆到另一个 Bean'
        ],
        endpoint: '/api/async/self-invocation',
        params: []
      }
    ]
  },
  {
    key: 'context',
    name: '上下文透传',
    desc: 'ThreadLocal / MDC 在线程池场景下的透传',
    scenarios: [
      {
        id: 'context-propagation',
        title: 'ThreadLocal / MDC 透传',
        scene: '通过 TaskDecorator 把 traceId 从主线程复制到异步线程。',
        bagu: [
          'ThreadLocal 默认不会跨线程',
          'Spring TaskDecorator 包装 Runnable，在 run 前后 set/remove',
          '任务结束后要 remove，防止线程复用导致串号'
        ],
        endpoint: '/api/async/context-propagation',
        params: []
      }
    ]
  },
  {
    key: 'production',
    name: '生产场景',
    desc: '批量聚合、异步 Controller、线程池监控、优雅关闭、同步异步对比',
    scenarios: [
      {
        id: 'batch-aggregate',
        title: '批量异步 + 结果聚合',
        scene: '提交 5 个异步任务，用 CompletableFuture.allOf 汇总结果。',
        bagu: [
          'allOf 等待全部完成，anyOf 等待任一完成',
          'thenApply 汇总结果，避免循环里逐个 get()',
          '注意 allOf 自身返回 CompletableFuture<Void>'
        ],
        endpoint: '/api/async/batch-aggregate',
        params: []
      },
      {
        id: 'controller-async',
        title: '异步 Controller',
        scene: '返回 Callable / CompletableFuture，释放 Tomcat 线程。',
        bagu: [
          'Spring MVC 支持 Callable / DeferredResult / CompletableFuture',
          'Tomcat 线程立即释放，提升并发连接数',
          '业务线程执行完成后再写回响应'
        ],
        endpoint: '/api/async/controller-async',
        params: []
      },
      {
        id: 'metrics',
        title: '线程池实时指标',
        scene: '读取 activeCount、queueSize、completedTaskCount、poolSize。',
        bagu: [
          'activeCount：当前活跃线程数',
          'queueSize：等待队列长度',
          'completedTaskCount：已完成任务数',
          'rejectedCount：拒绝任务数，需自定义实现'
        ],
        endpoint: '/api/async/metrics',
        params: []
      },
      {
        id: 'graceful-shutdown',
        title: '优雅关闭配置',
        scene: '验证所有线程池都开启了 waitForTasksToCompleteOnShutdown。',
        bagu: [
          'setWaitForTasksToCompleteOnShutdown(true)：先消费队列任务',
          'setAwaitTerminationSeconds(N)：最多等待 N 秒',
          '两者缺一不可，否则关闭时直接丢弃任务'
        ],
        endpoint: '/api/async/graceful-shutdown',
        params: []
      },
      {
        id: 'sync-vs-async',
        title: '异步 vs 同步对比',
        scene: '3 个 200ms 任务串行约 600ms，并行约 200ms。',
        bagu: [
          'IO 密集型任务并行收益明显',
          'CPU 密集型任务并行受核心数限制',
          '线程池过大反而增加上下文切换开销'
        ],
        endpoint: '/api/async/sync-vs-async',
        params: []
      }
    ]
  }
]

export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
