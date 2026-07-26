// 场景目录：前端渲染的数据源，endpoint 与后端 Spring Boot 接口一一对应
export const modules = [
  {
    key: 'list',
    name: 'List',
    desc: 'ArrayList / LinkedList / subList / ListIterator / fail-fast',
    scenarios: [
      {
        id: 'arraylist-vs-linkedlist',
        title: 'ArrayList vs LinkedList',
        scene: '对比同样数据量下随机访问、中间插入的耗时差异：ArrayList 基于数组随机访问快，LinkedList 基于链表中间插入占优。',
        bagu: [
          'ArrayList 底层是 Object 数组，随机访问 O(1)、中间插入/删除 O(n)',
          'LinkedList 是双向链表，随机访问 O(n)、头尾操作 O(1)',
          '多数场景优先用 ArrayList，只有频繁头尾插入才考虑 LinkedList'
        ],
        endpoint: '/api/list/arraylist-vs-linkedlist',
        params: [
          { name: 'size', label: '数据规模', type: 'number', default: 100000, min: 1000, max: 1000000 }
        ]
      },
      {
        id: 'sublist-trap',
        title: 'subList 陷阱',
        scene: 'subList 返回的是原 List 的视图而非拷贝，修改会联动影响原集合。',
        bagu: [
          'subList 与原 List 共享底层数组/链表',
          '通过 subList 做批量删除后原 List 也会被删',
          '想隔离必须用 new ArrayList<>(list.subList(from, to))'
        ],
        endpoint: '/api/list/sublist-trap',
        params: []
      },
      {
        id: 'listiterator',
        title: 'ListIterator 双向迭代',
        scene: 'ListIterator 支持双向遍历，并且可以在遍历中 set/add，普通 Iterator 只能 remove。',
        bagu: [
          'Iterator 单向：hasNext / next / remove',
          'ListIterator 双向：hasPrevious / previous，支持 set 和 add',
          '遍历时修改集合，使用迭代器的方法比直接改集合更安全'
        ],
        endpoint: '/api/list/listiterator',
        params: []
      },
      {
        id: 'iterator-failfast',
        title: 'fail-fast 迭代器',
        scene: '迭代过程中对集合做结构性修改会触发 ConcurrentModificationException。',
        bagu: [
          'modCount 记录结构性修改次数，expectedModCount 在迭代器创建时快照',
          '单线程里用增强 for 删除元素也会触发 fail-fast',
          '需要安全删除时应用 Iterator.remove()'
        ],
        endpoint: '/api/list/iterator-failfast',
        params: []
      }
    ]
  },
  {
    key: 'set',
    name: 'Set',
    desc: 'HashSet / LinkedHashSet / TreeSet / equals-hashCode',
    scenarios: [
      {
        id: 'hashset-dedup',
        title: 'HashSet 去重',
        scene: '自定义对象未重写 equals/hashCode 时，HashSet 无法按业务字段去重。',
        bagu: [
          'HashSet 底层是 HashMap，去重依赖对象的 equals 和 hashCode',
          '只重写 equals 不重写 hashCode 仍然去重失败',
          'IDE 生成 equals/hashCode 时要保证一致性'
        ],
        endpoint: '/api/set/hashset-dedup',
        params: []
      },
      {
        id: 'linkedhashset-order',
        title: 'LinkedHashSet 保序',
        scene: '需要既去重又保持插入顺序时，LinkedHashSet 是正确选择。',
        bagu: [
          'LinkedHashSet 底层是 LinkedHashMap，维护双向链表记录插入顺序',
          'HashSet 不保证任何顺序',
          'TreeSet 按比较器排序，不是插入顺序'
        ],
        endpoint: '/api/set/linkedhashset-order',
        params: []
      },
      {
        id: 'treeset-sort',
        title: 'TreeSet 排序',
        scene: 'TreeSet 按自然顺序或自定义 Comparator 排序，放入不可比对象会抛 ClassCastException。',
        bagu: [
          'TreeSet 底层是红黑树，操作复杂度 O(logN)',
          '元素必须实现 Comparable，或构造时传入 Comparator',
          'Comparator 返回 0 会被视为相等元素而只保留一个'
        ],
        endpoint: '/api/set/treeset-sort',
        params: []
      },
      {
        id: 'equals-hashcode-contract',
        title: 'equals/hashCode 契约',
        scene: 'equals 相等但 hashCode 不相等时，HashSet/HashMap 会出现定位失败、去重异常。',
        bagu: [
          '契约一：自反性 x.equals(x) == true',
          '契约二：对称性、传递性、一致性',
          '契约三：equals 相等的对象 hashCode 必须相等（但 hashCode 相等不一定 equals 相等）'
        ],
        endpoint: '/api/set/equals-hashcode-contract',
        params: []
      }
    ]
  },
  {
    key: 'maphash',
    name: 'HashMap',
    desc: '扰动函数 / 树化 / 扩容 / key 不可变',
    scenarios: [
      {
        id: 'hashmap-internals',
        title: 'HashMap 内部机制',
        scene: '演示 hash 扰动、容量为 2 的幂、index 计算与扩容时机。',
        bagu: [
          'hashCode 高 16 位与低 16 位异或，让高位也参与桶定位',
          '容量为 2 的幂时，index = (n-1) & hash 等价于 hash % n 但更快',
          '负载因子 0.75 是时间（冲突）与空间（扩容）的权衡'
        ],
        endpoint: '/api/maphash/hashmap-internals',
        params: []
      },
      {
        id: 'key-mutation',
        title: 'key 被修改后丢失',
        scene: '把可变对象作为 HashMap 的 key，put 后修改字段，再 get 会找不到。',
        bagu: [
          'key 的 hashCode 在 put 时决定桶位置',
          'key 改变后 hashCode 改变，get 去错桶查找',
          '最佳实践：用 String、Integer 等不可变对象做 key'
        ],
        endpoint: '/api/maphash/key-mutation',
        params: []
      },
      {
        id: 'map-compare',
        title: 'HashMap vs Hashtable vs CHM',
        scene: '三种 Map 在线程安全性和 null 支持上的区别。',
        bagu: [
          'HashMap 非线程安全，允许一个 null key 和多个 null value',
          'Hashtable 全方法 synchronized，不允许 null',
          'ConcurrentHashMap 线程安全，不允许 null（避免二义性）'
        ],
        endpoint: '/api/maphash/map-compare',
        params: []
      }
    ]
  },
  {
    key: 'mapordered',
    name: '有序 Map',
    desc: 'LinkedHashMap / TreeMap / WeakHashMap',
    scenarios: [
      {
        id: 'linkedhashmap-lru',
        title: 'LinkedHashMap LRU 缓存',
        scene: 'accessOrder=true 让最近访问的元素移到尾部，超容时淘汰头部最久未使用。',
        bagu: [
          'LinkedHashMap 维护双向链表记录访问/插入顺序',
          'accessOrder=true 时 get 会改变元素位置',
          '重写 removeEldestEntry 实现 LRU 淘汰'
        ],
        endpoint: '/api/mapordered/linkedhashmap-lru',
        params: [
          { name: 'capacity', label: '缓存容量', type: 'number', default: 5, min: 2, max: 20 }
        ]
      },
      {
        id: 'treemap-sort',
        title: 'TreeMap 排序与范围查询',
        scene: '按分数排名、取第一/最后、subMap 范围查询。',
        bagu: [
          'TreeMap 底层是红黑树，key 必须可比或提供 Comparator',
          'firstKey / lastKey / subMap 天然支持范围查询',
          '适合排行榜、区间检索等场景'
        ],
        endpoint: '/api/mapordered/treemap-sort',
        params: []
      },
      {
        id: 'weakhashmap-cache',
        title: 'WeakHashMap 弱引用缓存',
        scene: 'key 是弱引用，当没有强引用指向 key 时 GC 会自动清理对应 Entry。',
        bagu: [
          'WeakHashMap 的 Entry 在 key 被 GC 后会被回收',
          '适合做内存敏感的辅助缓存',
          '不要把强引用保留在别处，否则无法回收'
        ],
        endpoint: '/api/mapordered/weakhashmap-cache',
        params: []
      }
    ]
  },
  {
    key: 'queue',
    name: 'Queue',
    desc: 'PriorityQueue / ArrayDeque',
    scenarios: [
      {
        id: 'priorityqueue-topk',
        title: 'PriorityQueue TopK',
        scene: '维护大小为 K 的最小堆，从海量词频中快速取出 TopK 热词。',
        bagu: [
          'PriorityQueue 底层是完全二叉树（小顶堆）',
          'offer/poll 复杂度 O(logN)，peek O(1)',
          'TopK 场景维护 K 个元素的小顶堆，时间 O(NlogK)'
        ],
        endpoint: '/api/queue/priorityqueue-topk',
        params: [
          { name: 'k', label: '前 K 个', type: 'number', default: 10, min: 1, max: 50 }
        ]
      },
      {
        id: 'arraydeque-dual',
        title: 'ArrayDeque 双端队列',
        scene: 'ArrayDeque 既可当栈（push/pop）又可当队列（offer/poll）。',
        bagu: [
          'ArrayDeque 是双端队列首选，不允许 null',
          '作为栈比 Stack 快，作为队列比 LinkedList 快',
          '无容量限制，按需扩容'
        ],
        endpoint: '/api/queue/arraydeque-dual',
        params: []
      }
    ]
  },
  {
    key: 'utils',
    name: '工具类',
    desc: 'Collections / Arrays 常用工具与陷阱',
    scenarios: [
      {
        id: 'sort-binarysearch',
        title: 'sort + binarySearch',
        scene: 'binarySearch 前必须先排序；Comparator.comparing 支持链式多字段排序。',
        bagu: [
          'Collections.binarySearch 要求 List 已按同样 Comparator 排序',
          '返回负数表示不存在，插入点 = -(return+1)',
          'Comparator.comparing(...).thenComparing(...) 实现多字段排序'
        ],
        endpoint: '/api/utils/sort-binarysearch',
        params: []
      },
      {
        id: 'synchronized-unmodifiable',
        title: 'synchronized / unmodifiable 包装器',
        scene: 'synchronizedList 提供线程安全 add，unmodifiableList 只是只读视图。',
        bagu: [
          'Collections.synchronizedXXX 包装后方法级加锁',
          '迭代时仍需外部同步，否则可能 ConcurrentModificationException',
          'unmodifiableXXX 只是视图，原集合修改视图也会变'
        ],
        endpoint: '/api/utils/synchronized-unmodifiable',
        params: []
      },
      {
        id: 'arrays-aslist-trap',
        title: 'Arrays.asList 陷阱',
        scene: 'Arrays.asList 返回固定大小视图，add/remove 会抛异常。',
        bagu: [
          'Arrays.asList 底层仍是原数组的包装',
          'set 可以修改元素，但 add/remove 不行',
          '需要可变 List 时用 new ArrayList<>(Arrays.asList(...))'
        ],
        endpoint: '/api/utils/arrays-aslist-trap',
        params: []
      },
      {
        id: 'shuffle',
        title: 'Collections.shuffle',
        scene: '用 Random 种子可以复现同一乱序结果。',
        bagu: [
          'shuffle 使用 Fisher-Yates 洗牌算法',
          '相同 Random 种子会产生相同的排列',
          '测试需要可复现时显式传入 Random'
        ],
        endpoint: '/api/utils/shuffle',
        params: []
      }
    ]
  },
  {
    key: 'generics',
    name: '泛型',
    desc: 'PECS / 类型擦除 / 泛型方法 / 泛型 DAO',
    scenarios: [
      {
        id: 'pecs',
        title: 'PECS 原则',
        scene: '<? extends E> 是只读生产者，<? super E> 是只写消费者。',
        bagu: [
          'Producer Extends：从集合里读，得到的是 E 或其子类型',
          'Consumer Super：往集合里写，只能写 E 或其子类型',
          'List<? extends Number> 不能 add Integer，因为具体类型未知'
        ],
        endpoint: '/api/generics/pecs',
        params: []
      },
      {
        id: 'type-erasure',
        title: '类型擦除与 raw type',
        scene: 'Java 泛型在运行时会被擦除为 Object 或边界类型；raw type 破坏类型安全。',
        bagu: [
          '编译后泛型类型参数被擦除，运行期看不到 T',
          'raw type 是为了兼容 Java 5 之前代码，不应在新代码中使用',
          '泛型信息保留在签名中，可通过反射的 Type 获取'
        ],
        endpoint: '/api/generics/type-erasure',
        params: []
      },
      {
        id: 'generic-method',
        title: '泛型方法 + 类型推断',
        scene: '定义 <T extends Comparable<T>> T findMax 方法，同时处理 Integer、String 等可比较类型。',
        bagu: [
          '泛型方法类型参数在方法签名前声明',
          '调用时通常无需显式指定，编译器可推断',
          '泛型方法 vs 泛型类：方法级泛型更灵活'
        ],
        endpoint: '/api/generics/generic-method',
        params: []
      },
      {
        id: 'generic-dao',
        title: '泛型 DAO / Service',
        scene: 'BaseRepository<T, ID> 让 User、Order 等实体复用 CRUD 模板。',
        bagu: [
          '泛型约束 <T extends BaseEntity> 保证 T 具备 getId 能力',
          '分层架构中 DAO/Service 常用泛型减少重复代码',
          '类型安全：UserRepository 返回的就是 User，不用强转'
        ],
        endpoint: '/api/generics/generic-dao',
        params: []
      }
    ]
  },
  {
    key: 'realworld',
    name: '综合实战',
    desc: 'LRU / TopK / groupBy / 多字段排序',
    scenarios: [
      {
        id: 'lru-cache',
        title: 'LRU 缓存',
        scene: '用 LinkedHashMap 实现带容量限制的 LRU 缓存，最近访问保留、最久未使用淘汰。',
        bagu: [
          'LRU = 双向链表维护访问顺序 + HashMap 保证 O(1) 查找',
          'LinkedHashMap 是实现 LRU 最简洁的方式',
          '生产级可用 Caffeine 等成熟库'
        ],
        endpoint: '/api/realworld/lru-cache',
        params: [
          { name: 'capacity', label: '缓存容量', type: 'number', default: 3, min: 2, max: 10 }
        ]
      },
      {
        id: 'topk-words',
        title: 'TopK 热词统计',
        scene: 'HashMap 统计词频 + PriorityQueue 小顶堆取 TopK。',
        bagu: [
          '统计阶段 O(N)，维护 K 个元素堆 O(NlogK)',
          '比全排序后取前 K 更省内存',
          'Map.merge 是词频统计的利器'
        ],
        endpoint: '/api/realworld/topk-words',
        params: [
          { name: 'k', label: '前 K 个', type: 'number', default: 5, min: 1, max: 20 }
        ]
      },
      {
        id: 'groupby',
        title: 'groupBy 订单',
        scene: '按用户聚合总金额与订单数，按状态分组订单列表。',
        bagu: [
          'Map.compute 适合更新聚合值',
          'Map.computeIfAbsent 适合分组收集',
          'Map.merge 适合累加、拼接等二元操作'
        ],
        endpoint: '/api/realworld/groupby',
        params: []
      },
      {
        id: 'comparator-sort',
        title: '通用排序工具',
        scene: '用 Comparator.comparing、thenComparing、nullsFirst/nullsLast 实现多字段排序。',
        bagu: [
          'Comparator.comparing 提取关键字段',
          'thenComparing 添加次要字段',
          'nullsFirst / nullsLast 显式处理 null'
        ],
        endpoint: '/api/realworld/comparator-sort',
        params: []
      }
    ]
  }
]

// 汇总统计，供界面展示
export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
