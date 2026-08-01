export const modules = [
  {
    key: 'basic',
    name: '基础原理',
    desc: 'ThreadLocal 线程隔离、initialValue / withInitial',
    scenarios: [
      {
        id: 'isolation',
        title: '线程隔离',
        scene: '两个线程对同一个 ThreadLocal 写不同值，互不影响。',
        bagu: [
          'ThreadLocal 不是线程，而是线程的局部变量',
          '底层是 Thread 对象里的 ThreadLocalMap',
          '每个线程独立维护自己的 Entry 数组'
        ],
        endpoint: '/api/basic/isolation',
        params: []
      },
      {
        id: 'initial',
        title: 'initialValue / withInitial',
        scene: '未 set 时返回默认值，remove 后恢复默认值。',
        bagu: [
          'initialValue() 在首次 get 且未 set 时触发',
          'ThreadLocal.withInitial(Supplier) 是 JDK 8 推荐写法',
          'remove 后再 get 会重新调用 initialValue'
        ],
        endpoint: '/api/basic/initial',
        params: []
      }
    ]
  },
  {
    key: 'web',
    name: 'Web 上下文',
    desc: 'Filter + ThreadLocal / MDC traceId / SimpleDateFormat 线程安全',
    scenarios: [
      {
        id: 'user-context',
        title: 'Filter + ThreadLocal 传递用户',
        scene: '从请求 Header 解析用户，在线程内无感传递。',
        bagu: [
          'Filter 在请求进入时 set，finally 中 remove',
          'Controller/Service 直接 ThreadLocalHolder.get()',
          '忘记 remove 会导致线程池复用时串号'
        ],
        endpoint: '/api/web/user-context',
        params: []
      },
      {
        id: 'mdc-trace',
        title: 'MDC 全链路 traceId',
        scene: '通过 SLF4J MDC 在日志中输出 traceId，方便排查问题。',
        bagu: [
          'MDC 底层也是 ThreadLocal',
          '日志 pattern 中 %X{traceId} 输出当前线程 traceId',
          '请求结束后必须 MDC.clear()'
        ],
        endpoint: '/api/web/mdc-trace',
        params: []
      },
      {
        id: 'dateformat-safe',
        title: 'SimpleDateFormat 线程安全',
        scene: '共享 SimpleDateFormat 并发 parse 会异常，ThreadLocal 可解决。',
        bagu: [
          'SimpleDateFormat 内部状态可变，非线程安全',
          '高并发下会抛异常或结果错乱',
          '推荐方案：ThreadLocal<DateFormat> 或 DateTimeFormatter'
        ],
        endpoint: '/api/web/dateformat-safe',
        params: []
      }
    ]
  },
  {
    key: 'cross',
    name: '跨线程',
    desc: 'InheritableThreadLocal / 线程池污染 / TTL 透传',
    scenarios: [
      {
        id: 'inheritable',
        title: 'InheritableThreadLocal',
        scene: '父子线程值传递；new Thread 可继承，线程池不继承。',
        bagu: [
          'InheritableThreadLocal 在创建子线程时拷贝父线程值',
          '只适用于 new Thread()，不适用于线程池',
          '线程池复用线程不会重新拷贝'
        ],
        endpoint: '/api/cross/inheritable',
        params: []
      },
      {
        id: 'pool-hazard',
        title: '线程池串号 / 污染',
        scene: '任务 A 设置 ThreadLocal 未 remove，任务 B 复用同一线程读到残留值。',
        bagu: [
          '线程池会复用线程',
          'ThreadLocalMap 随线程生命周期存在',
          '未 remove 的值会被后续任务读到，造成串号'
        ],
        endpoint: '/api/cross/pool-hazard',
        params: []
      },
      {
        id: 'pool-remove',
        title: '线程池正确使用',
        scene: '任务 finally 中 remove，避免残留。',
        bagu: [
          '每个任务 try { ... } finally { remove() }',
          'remove 后再 get 返回 null 或 initialValue',
          '是线程池 + ThreadLocal 的黄金法则'
        ],
        endpoint: '/api/cross/pool-remove',
        params: []
      },
      {
        id: 'async-context',
        title: 'CompletableFuture 上下文丢失',
        scene: '默认 ForkJoinPool 不继承 ThreadLocal，异步任务读不到主线程上下文。',
        bagu: [
          'supplyAsync 默认使用 ForkJoinPool.commonPool()',
          '异步线程与主线程不同，ThreadLocal 不共享',
          '需要手动拷贝上下文或使用 TTL'
        ],
        endpoint: '/api/cross/async-context',
        params: []
      },
      {
        id: 'ttl-propagation',
        title: 'TTL 线程池透传',
        scene: 'Alibaba TransmittableThreadLocal + TtlExecutors 实现线程池上下文自动透传。',
        bagu: [
          'TransmittableThreadLocal 是 TTL 提供的增强版 ThreadLocal',
          'TtlExecutors.getTtlExecutorService() 包装线程池',
          '提交任务时自动捕获/回放上下文'
        ],
        endpoint: '/api/cross/ttl-propagation',
        params: []
      }
    ]
  },
  {
    key: 'advance',
    name: '进阶',
    desc: '内存泄漏原理 / 最佳实践',
    scenarios: [
      {
        id: 'leak-analysis',
        title: '内存泄漏原理',
        scene: 'ThreadLocalMap 的 key 是弱引用、value 是强引用，线程池长生命周期下可能 OOM。',
        bagu: [
          'Entry extends WeakReference<ThreadLocal<?>>',
          'key 是 ThreadLocal 的弱引用，可被 GC',
          'value 是强引用，必须 remove 才能释放'
        ],
        endpoint: '/api/advance/leak-analysis',
        params: []
      },
      {
        id: 'best-practice',
        title: '最佳实践',
        scene: 'static final + try-finally remove 等使用规范。',
        bagu: [
          '声明为 private static final',
          'try-finally 中 remove',
          '线程池任务必须 remove',
          'value 避免大对象',
          '跨线程池优先用 TTL'
        ],
        endpoint: '/api/advance/best-practice',
        params: []
      }
    ]
  }
]

export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
