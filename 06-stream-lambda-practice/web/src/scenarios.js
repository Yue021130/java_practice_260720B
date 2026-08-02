export const modules = [
  {
    key: 'lambda',
    name: 'Lambda 基础',
    desc: 'Lambda 表达式、函数式接口与方法引用',
    scenarios: [
      {
        id: 'functional',
        title: 'Lambda 与函数式接口',
        scene: '用 Predicate / Function / Consumer / Supplier 处理员工数据。',
        bagu: [
          '@FunctionalInterface 只有一个抽象方法',
          'Predicate<T> 返回 boolean，常用于 filter',
          'Function<T,R> 做映射，Consumer<T> 消费，Supplier<T> 供给'
        ],
        endpoint: '/api/lambda/functional',
        params: []
      },
      {
        id: 'method-ref',
        title: '方法引用',
        scene: '静态、实例、构造方法引用是 Lambda 的简写形式。',
        bagu: [
          '对象::实例方法、类::静态方法、类::实例方法、类::new',
          '要求函数式接口的参数/返回值与目标方法匹配',
          '可读性通常优于等价的 lambda 表达式'
        ],
        endpoint: '/api/lambda/method-ref',
        params: []
      }
    ]
  },
  {
    key: 'stream',
    name: 'Stream 基础',
    desc: 'Stream 创建、中间操作、终止操作与基本类型流',
    scenarios: [
      {
        id: 'create',
        title: 'Stream 创建方式',
        scene: 'collection.stream()、Stream.of、IntStream.range、iterate、generate。',
        bagu: [
          'Stream 不修改数据源，可来自集合/数组/生成器',
          'IntStream.range 左闭右开，rangeClosed 左闭右闭',
          'iterate / generate 用于构造无限流，需配合 limit'
        ],
        endpoint: '/api/stream/create',
        params: []
      },
      {
        id: 'intermediate',
        title: '中间操作',
        scene: 'filter / map / flatMap / distinct / sorted / peek / limit / skip。',
        bagu: [
          '中间操作返回 Stream，可链式调用',
          '中间操作是懒执行的，遇到终止操作才计算',
          'peek 仅用于调试，不要修改元素状态'
        ],
        endpoint: '/api/stream/intermediate',
        params: []
      },
      {
        id: 'terminal',
        title: '终止操作',
        scene: 'collect / reduce / forEach / findFirst / anyMatch / max 触发计算。',
        bagu: [
          '终止操作触发整条流水线执行',
          'Stream 只能被消费一次',
          'findFirst / anyMatch / max 返回 Optional'
        ],
        endpoint: '/api/stream/terminal',
        params: []
      },
      {
        id: 'primitive',
        title: '基本类型流',
        scene: 'IntStream / LongStream / DoubleStream 避免装箱拆箱。',
        bagu: [
          'mapToInt / mapToLong / mapToDouble 产生基本类型流',
          '基本类型流提供 sum / average / max 等专用聚合',
          'boxed() 可把 IntStream 转回 Stream<Integer>'
        ],
        endpoint: '/api/stream/primitive',
        params: []
      }
    ]
  },
  {
    key: 'collectors',
    name: 'Collectors',
    desc: '分组、分区、字符串拼接与统计',
    scenarios: [
      {
        id: 'group-partition',
        title: '分组与分区',
        scene: 'groupingBy 按部门分组，partitioningBy 按工资高低二分。',
        bagu: [
          'groupingBy 可配合 counting / averagingInt 等下游收集器',
          'partitioningBy 返回 Map<Boolean, List<T>>',
          '两者都可通过并行流提升吞吐量'
        ],
        endpoint: '/api/collectors/group-partition',
        params: []
      },
      {
        id: 'join-summary',
        title: '字符串拼接与统计',
        scene: 'joining、summarizingInt、maxBy、reducing 聚合结果。',
        bagu: [
          'joining 做字符串拼接，可指定分隔符/前缀/后缀',
          'summarizingInt 一次返回 count/sum/min/average/max',
          'reducing 必须满足结合律才能安全并行'
        ],
        endpoint: '/api/collectors/join-summary',
        params: []
      }
    ]
  },
  {
    key: 'optional',
    name: 'Optional',
    desc: '空值安全处理',
    scenarios: [
      {
        id: 'safe',
        title: '空值安全',
        scene: '用 ofNullable / map / filter / orElse / orElseGet / orElseThrow 避免 NPE。',
        bagu: [
          'Optional 用于明确表达可能为空',
          'orElse 立即求值，orElseGet 惰性求值',
          '不要滥用 Optional 作为字段/方法参数'
        ],
        endpoint: '/api/optional/safe',
        params: []
      }
    ]
  },
  {
    key: 'parallel',
    name: '并行流',
    desc: '并行流加速、开销、线程安全与顺序',
    scenarios: [
      {
        id: 'speedup',
        title: '并行加速场景',
        scene: '大集合 + CPU 密集型运算，parallelStream 明显更快。',
        bagu: [
          '适合 CPU 密集型、大数据量、无状态操作',
          '底层使用 ForkJoinPool.commonPool',
          '数据源最好可高效拆分，如 IntStream.range / Arrays.stream'
        ],
        endpoint: '/api/parallel/speedup',
        params: []
      },
      {
        id: 'overhead',
        title: '并行额外开销',
        scene: '小集合 + 简单操作，parallelStream 因任务拆分反而更慢。',
        bagu: [
          '小集合不要 parallelStream',
          '简单操作拆分/合并开销大于收益',
          'IO 密集型可考虑自定义线程池'
        ],
        endpoint: '/api/parallel/overhead',
        params: []
      },
      {
        id: 'race-condition',
        title: '线程不安全错误示范',
        scene: '共享可变 ArrayList + parallelStream().forEach(list::add) 出现竞争。',
        bagu: [
          'forEach 不保证执行顺序，也不保证原子性',
          '共享可变状态会导致结果不一致',
          '应使用 collect 合并局部结果'
        ],
        endpoint: '/api/parallel/race-condition',
        params: []
      },
      {
        id: 'correct-reduce',
        title: '正确聚合',
        scene: '用 reduce / collect 在并行流下得到正确结果。',
        bagu: [
          'reduce / collect 内部做分治合并',
          '操作必须无状态且满足结合律',
          '避免在 lambda 中读写外部可变变量'
        ],
        endpoint: '/api/parallel/correct-reduce',
        params: []
      },
      {
        id: 'order-findany',
        title: '顺序与 findAny',
        scene: '有序 vs unordered、findFirst vs findAny 在并行流下的差异。',
        bagu: [
          'findFirst 在有序流中稳定返回第一个',
          'findAny 可返回任意匹配元素，性能更好',
          'unordered() 可取消顺序约束提升并行效率'
        ],
        endpoint: '/api/parallel/order-findany',
        params: []
      }
    ]
  }
]

export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
