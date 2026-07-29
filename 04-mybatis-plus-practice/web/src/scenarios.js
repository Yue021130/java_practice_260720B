// 场景目录：前端渲染的数据源，endpoint 与后端 Spring Boot 接口一一对应
export const modules = [
  {
    key: 'entity',
    name: '实体类注解',
    desc: '@TableName / @TableId / @TableField',
    scenarios: [
      {
        id: 'table-name',
        title: '@TableName 表名映射',
        scene: '实体类名 User 与数据库表名 t_user 不一致时，用 @TableName 显式指定。',
        bagu: [
          '@TableName("t_user") 指定实体类对应的表名',
          '全局可通过 mybatis-plus.global-config.db-config.table-prefix 配置表前缀',
          '类名转表名默认采用驼峰转下划线'
        ],
        endpoint: '/api/entity/table-name',
        params: []
      },
      {
        id: 'table-id',
        title: '@TableId 主键策略',
        scene: '演示 ASSIGN_ID 雪花 ID 与 AUTO 数据库自增两种主键生成策略。',
        bagu: [
          '@TableId 指定主键字段与生成策略',
          'IdType.ASSIGN_ID：雪花算法生成 Long 型 ID',
          'IdType.AUTO：依赖数据库自增；NONE/INPUT/ASSIGN_UUID 等按需选择'
        ],
        endpoint: '/api/entity/table-id',
        params: []
      },
      {
        id: 'table-field',
        title: '@TableField 字段映射',
        scene: '处理字段别名、排除非持久化字段，如确认密码、临时备注等。',
        bagu: [
          '@TableField("user_name") 指定数据库字段名',
          '@TableField(exist = false) 标记非持久化字段',
          'fill = FieldFill.INSERT/INSERT_UPDATE 配合 MetaObjectHandler 自动填充'
        ],
        endpoint: '/api/entity/table-field',
        params: []
      }
    ]
  },
  {
    key: 'mapper',
    name: 'BaseMapper CRUD',
    desc: 'insert / selectById / updateById / deleteById',
    scenarios: [
      {
        id: 'insert',
        title: 'insert 插入',
        scene: 'BaseMapper.insert 执行后，雪花 ID 会自动回填到实体对象中。',
        bagu: [
          'BaseMapper 提供 insert、deleteById、updateById、selectById 等基础方法',
          'insert 成功后，实体对象的 id 字段会被自动填充',
          '与 Service.save 的区别：Mapper 层方法，返回 int 影响行数'
        ],
        endpoint: '/api/mapper/insert',
        params: []
      },
      {
        id: 'select-by-id',
        title: 'selectById 查询',
        scene: '根据主键查询单条记录，逻辑删除会自动过滤已删除数据。',
        bagu: [
          'selectById 根据主键查询',
          '配置了 @TableLogic 后，查询会自动拼接 deleted=0',
          '主键类型要与实体中 @TableId 字段类型一致'
        ],
        endpoint: '/api/mapper/select-by-id',
        params: []
      },
      {
        id: 'update-by-id',
        title: 'updateById 更新',
        scene: '根据主键更新非空字段，避免覆盖不想修改的列。',
        bagu: [
          'updateById 默认只更新非空字段',
          '可通过 Wrapper 设置某些字段为 null',
          '配合 @Version 会自动进行乐观锁校验'
        ],
        endpoint: '/api/mapper/update-by-id',
        params: []
      },
      {
        id: 'delete-by-id',
        title: 'deleteById 删除',
        scene: 'deleteById 是物理删除；逻辑删除需要用 @TableLogic + removeById。',
        bagu: [
          'deleteById 执行 DELETE 语句，物理删除',
          '@TableLogic 让 removeById 执行 UPDATE deleted=1',
          '逻辑删除后 selectById 会返回 null'
        ],
        endpoint: '/api/mapper/delete-by-id',
        params: []
      }
    ]
  },
  {
    key: 'service',
    name: 'IService CRUD',
    desc: 'save / saveOrUpdate / list / page',
    scenarios: [
      {
        id: 'save',
        title: 'save 保存',
        scene: 'IService.save 返回 boolean，主键同样会回填到实体。',
        bagu: [
          'IService 是对 BaseMapper 的 Service 层封装',
          'save 返回 boolean，saveBatch 支持批量',
          'Service 层适合封装业务逻辑，Mapper 层专注数据访问'
        ],
        endpoint: '/api/service/save',
        params: []
      },
      {
        id: 'save-or-update',
        title: 'saveOrUpdate 保存或更新',
        scene: '根据实体是否有主键决定 insert 还是 update。',
        bagu: [
          '实体 id 为空时执行 insert',
          '实体 id 不为空时执行 update',
          '适合前端传主键可空的保存接口'
        ],
        endpoint: '/api/service/save-or-update',
        params: []
      },
      {
        id: 'list',
        title: 'list 查询',
        scene: 'IService.list() 查询全表，IService.list(wrapper) 按条件查询。',
        bagu: [
          'list() 等价于 selectList(null)',
          'count() 返回总数',
          '逻辑删除数据会自动被过滤'
        ],
        endpoint: '/api/service/list',
        params: []
      },
      {
        id: 'page',
        title: 'page 分页',
        scene: 'IService.page(Page<T>) 分页查询，需要配置分页插件。',
        bagu: [
          'Page<T> 传入 current 和 size',
          '必须配置 PaginationInnerInterceptor，否则 total=0',
          '可与 Wrapper 组合实现条件分页'
        ],
        endpoint: '/api/service/page',
        params: []
      }
    ]
  },
  {
    key: 'wrapper',
    name: '条件构造器',
    desc: 'QueryWrapper / LambdaQueryWrapper / 嵌套条件',
    scenarios: [
      {
        id: 'eq-like',
        title: '等值 + 模糊查询',
        scene: 'QueryWrapper.eq 等值匹配，like 模糊匹配。',
        bagu: [
          'eq 用于等值条件，如 status = 1',
          'like 默认两侧加 %，likeRight/likeLeft 控制 % 位置',
          '多个条件默认用 AND 连接'
        ],
        endpoint: '/api/wrapper/eq-like',
        params: []
      },
      {
        id: 'between-order',
        title: '范围 + 排序',
        scene: 'between 范围查询，orderByAsc/orderByDesc 多字段排序。',
        bagu: [
          'between 包含边界值',
          'orderByAsc / orderByDesc 支持链式调用',
          'last("LIMIT 10") 可追加原生 SQL（慎用，防止注入）'
        ],
        endpoint: '/api/wrapper/between-order',
        params: []
      },
      {
        id: 'lambda',
        title: 'Lambda 条件构造器',
        scene: 'LambdaQueryWrapper 通过方法引用避免硬编码字段名字符串。',
        bagu: [
          'LambdaQueryWrapper 使用方法引用 User::getAge',
          '字段改名时编译期即可发现错误',
          '功能与 QueryWrapper 一致，但类型更安全'
        ],
        endpoint: '/api/wrapper/lambda',
        params: []
      },
      {
        id: 'nested',
        title: '嵌套 and / or',
        scene: '用 nested 显式控制 WHERE 条件的优先级。',
        bagu: [
          '默认 and 优先级高于 or',
          'nested 方法显式包裹一组条件',
          '复杂查询建议用 Lambda 表达式避免字符串拼接'
        ],
        endpoint: '/api/wrapper/nested',
        params: []
      }
    ]
  },
  {
    key: 'page',
    name: '分页查询',
    desc: 'Page<T> + 分页插件 + Wrapper 组合',
    scenarios: [
      {
        id: 'basic',
        title: '基础分页',
        scene: 'Page<T> + BaseMapper.selectPage 实现无筛选分页。',
        bagu: [
          '分页插件必须注册 MybatisPlusInterceptor',
          'Page 对象既是入参也是返回值，会被填充 total/pages/records',
          '数据库类型 DbType 要与实际数据库一致'
        ],
        endpoint: '/api/page/basic',
        params: []
      },
      {
        id: 'custom',
        title: '分页 + 条件',
        scene: '分页与 QueryWrapper 同时生效，先 WHERE 再 LIMIT。',
        bagu: [
          'selectPage(page, wrapper) 同时传分页和条件',
          'total 是满足条件的总记录数',
          '适合带筛选的列表页'
        ],
        endpoint: '/api/page/custom',
        params: []
      }
    ]
  },
  {
    key: 'advanced',
    name: '高级注解',
    desc: '@TableLogic / @Version / @TableField(fill)',
    scenarios: [
      {
        id: 'logic-delete',
        title: '逻辑删除 @TableLogic',
        scene: 'removeById 更新 deleted=1，查询自动过滤已删除记录。',
        bagu: [
          '@TableLogic 标记逻辑删除字段',
          '全局配置 logic-delete-value / logic-not-delete-value',
          '物理删除需使用 deleteById 或自定义 SQL'
        ],
        endpoint: '/api/advanced/logic-delete',
        params: []
      },
      {
        id: 'optimistic-lock',
        title: '乐观锁 @Version',
        scene: '更新时 version 自动自增，防止并发覆盖。',
        bagu: [
          '@Version 标记乐观锁字段',
          '必须注册 OptimisticLockerInnerInterceptor',
          '更新失败说明数据已被其他事务修改'
        ],
        endpoint: '/api/advanced/optimistic-lock',
        params: []
      },
      {
        id: 'auto-fill',
        title: '自动填充 @TableField(fill)',
        scene: 'MetaObjectHandler 自动填充 createTime / updateTime。',
        bagu: [
          'FieldFill.INSERT：只在插入时填充',
          'FieldFill.INSERT_UPDATE：插入和更新都填充',
          '统一处理创建/更新时间，避免业务代码重复赋值'
        ],
        endpoint: '/api/advanced/auto-fill',
        params: []
      }
    ]
  },
  {
    key: 'batch',
    name: '批量操作',
    desc: 'saveBatch / updateBatchById',
    scenarios: [
      {
        id: 'save-batch',
        title: 'saveBatch 批量插入',
        scene: '按 batchSize 分批插入，减少数据库往返次数。',
        bagu: [
          'saveBatch(Collection<T>) 默认一批 1000 条',
          'saveBatch(list, batchSize) 可自定义分批大小',
          '主键同样会回填到每个实体'
        ],
        endpoint: '/api/batch/save-batch',
        params: []
      },
      {
        id: 'update-batch',
        title: 'updateBatchById 批量更新',
        scene: '按主键批量更新多条记录。',
        bagu: [
          'updateBatchById 按 id 批量执行 UPDATE',
          'null 字段默认不更新',
          '大批量建议分批提交，避免 SQL 过长'
        ],
        endpoint: '/api/batch/update-batch',
        params: []
      }
    ]
  },
  {
    key: 'realworld',
    name: '综合实战',
    desc: '用户订单 / 分组统计 / 搜索分页',
    scenarios: [
      {
        id: 'user-order',
        title: '用户订单统计',
        scene: '自定义 SQL 关联用户与订单表，统计每个用户的金额与订单数。',
        bagu: [
          '复杂关联统计可写 @Select 自定义 SQL',
          '简单单表聚合可用 Wrapper.groupBy',
          '复杂场景推荐 XML 或 @Select 分表统计'
        ],
        endpoint: '/api/realworld/user-order',
        params: []
      },
      {
        id: 'status-stats',
        title: '状态分组统计',
        scene: '按用户状态分组，统计人数与平均年龄。',
        bagu: [
          '聚合查询适合用自定义 SQL',
          'Map<String, Object> 接收动态列',
          '生产上可用 VO 对象替代 Map'
        ],
        endpoint: '/api/realworld/status-stats',
        params: []
      },
      {
        id: 'search-page',
        title: '综合搜索分页',
        scene: '用户名模糊 + 状态等值 + 年龄范围 + 分页 + 排序。',
        bagu: [
          '生产常见列表接口：多个筛选条件 + 分页 + 排序',
          'Wrapper 链式构建条件',
          '分页插件与 Wrapper 组合使用'
        ],
        endpoint: '/api/realworld/search-page',
        params: []
      }
    ]
  },
  {
    key: 'more',
    name: '更多注解',
    desc: '@KeySequence / @OrderBy / @EnumValue / @InterceptorIgnore / @TableField 高级属性 / IdType / FieldStrategy / @Order / 自定义注解 / @Accessors',
    scenarios: [
      {
        id: 'key-sequence',
        title: '@KeySequence 序列主键',
        scene: 'Oracle/PostgreSQL/H2 等支持 sequence 的数据库，用 @KeySequence 生成主键。',
        bagu: [
          '@KeySequence(value=\"seq_name\", dbType=DbType.H2) 指定序列与数据库类型',
          '实体 @TableId 需使用 IdType.INPUT',
          'insert 后序列值自动回填到实体 id'
        ],
        endpoint: '/api/more/key-sequence',
        params: []
      },
      {
        id: 'order-by',
        title: '@OrderBy 默认排序',
        scene: '在实体字段上标注默认排序规则，list/lambdaQuery 自动追加 ORDER BY。',
        bagu: [
          '@OrderBy(asc=false) 表示倒序',
          '适用于需要全局默认排序的字段，如 create_time',
          'Wrapper 中显式 orderBy 会覆盖默认排序'
        ],
        endpoint: '/api/more/order-by',
        params: []
      },
      {
        id: 'enum-value',
        title: '@EnumValue 枚举映射',
        scene: '枚举按 code 持久化到数据库，按 desc 展示给前端。',
        bagu: [
          '@EnumValue 标记持久化字段',
          '@JsonValue 控制序列化返回值',
          '不加注解时默认按枚举名 name() 持久化'
        ],
        endpoint: '/api/more/enum-value',
        params: []
      },
      {
        id: 'interceptor-ignore',
        title: '@InterceptorIgnore 忽略插件',
        scene: '临时关闭防止全表更新/删除、多租户、乐观锁等插件拦截。',
        bagu: [
          '@InterceptorIgnore(blockAttack=\"true\") 关闭防止全表更新/删除',
          '也支持 tenantLine、optimisticLocker、illegalSql 等',
          '常用于特殊运维操作或管理后台全量导出'
        ],
        endpoint: '/api/more/interceptor-ignore',
        params: []
      },
      {
        id: 'field-select',
        title: '@TableField(select=false)',
        scene: '查询时不返回敏感字段，如密码。',
        bagu: [
          'select=false 的字段不会出现在 SELECT 列表',
          '与 exist=false 区别：exist=false 完全不是表字段',
          '密码、 salt 等敏感字段常用'
        ],
        endpoint: '/api/more/field-select',
        params: []
      },
      {
        id: 'field-condition',
        title: '@TableField(condition)',
        scene: '自定义 WHERE 条件模板，例如 eq 也走 LIKE。',
        bagu: [
          'condition=\"%s LIKE CONCAT(\\\'%%\\\',#{%s},\\\'%%\\\')\"',
          '%s 表示字段名，#{%s} 表示参数占位',
          '适合前后模糊匹配等统一查询规则'
        ],
        endpoint: '/api/more/field-condition',
        params: []
      },
      {
        id: 'field-update',
        title: '@TableField(update)',
        scene: '自定义 SET 片段，例如登录次数每次自动 +1。',
        bagu: [
          'update=\"%s+1\" 生成 login_count = login_count + 1',
          '可实现自增、自减、拼接等原子操作',
          '注意转义与 SQL 注入风险'
        ],
        endpoint: '/api/more/field-update',
        params: []
      },
      {
        id: 'field-numeric-scale',
        title: '@TableField(numericScale)',
        scene: '指定 DECIMAL 字段的小数位数。',
        bagu: [
          'numericScale=\"2\" 表示保留两位小数',
          '常用于金额字段',
          '数据库 DECIMAL(12,2) 与注解配合使用'
        ],
        endpoint: '/api/more/field-numeric-scale',
        params: []
      },
      {
        id: 'id-types',
        title: 'IdType 全策略',
        scene: 'MyBatis-Plus 提供的五种主键生成策略一览。',
        bagu: [
          'AUTO：数据库自增',
          'INPUT：外部传入，常与 @KeySequence 配合',
          'ASSIGN_ID：雪花算法 Long',
          'ASSIGN_UUID：UUID 字符串',
          'NONE：不生成'
        ],
        endpoint: '/api/more/id-types',
        params: []
      },
      {
        id: 'field-strategy',
        title: 'FieldStrategy',
        scene: '控制字段在 insert/update 时是否参与 SQL。',
        bagu: [
          'DEFAULT：继承全局配置',
          'NOT_NULL：非 null 才参与',
          'NOT_EMPTY：非 null 且非空字符串才参与',
          'IGNORED：始终参与，即使为 null'
        ],
        endpoint: '/api/more/field-strategy',
        params: []
      },
      {
        id: 'order',
        title: '@Order 执行顺序',
        scene: 'Spring 的 @Order 控制多个同类 Bean 的执行顺序。',
        bagu: [
          '@Order 数字越小优先级越高',
          '常用于 CommandLineRunner、Aspect、Interceptor、EventListener',
          '未标注 @Order 默认 Integer.MAX_VALUE，最后执行'
        ],
        endpoint: '/api/more/order',
        params: []
      },
      {
        id: 'custom-annotation',
        title: '自定义注解 + AOP',
        scene: '自定义 @AutoFillUser 注解，配合 AOP 自动填充 createBy / updateBy。',
        bagu: [
          '@interface 定义自定义注解',
          '@Target / @Retention 指定作用位置与保留策略',
          '@Aspect + @Before("@annotation(xxx)") 实现切面逻辑',
          '多个切面用 @Order 控制执行顺序'
        ],
        endpoint: '/api/more/custom-annotation',
        params: []
      },
      {
        id: 'accessors',
        title: '@Accessors 链式/fluent/prefix',
        scene: 'Lombok @Accessors 让 POJO 支持链式 setter、fluent 风格或自动剥离字段前缀。',
        bagu: [
          'chain=true：setter 返回 this，支持链式调用',
          'fluent=true：省略 get/set 前缀，生成 name()/name(String)',
          'prefix={"f","m"}：生成 getter/setter 时自动剥离字段前缀'
        ],
        endpoint: '/api/more/accessors',
        params: []
      }
    ]
  },
  {
    key: 'extension',
    name: '扩展实战',
    desc: 'TypeHandler / ActiveRecord / 动态表名 / InsertBatchSomeColumn / 链式 Wrapper / 复杂 Wrapper / selectMaps / Wrapper 更新删除',
    scenarios: [
      {
        id: 'type-handler',
        title: 'TypeHandler 自定义类型转换',
        scene: '把 Map/List 等复杂对象以 JSON 字符串形式持久化到数据库。',
        bagu: [
          '@TableField(typeHandler = JacksonTypeHandler.class)',
          '使用 typeHandler 时建议开启 @TableName(autoResultMap = true)',
          '适用于 JSON 字段、数组、枚举集合等复杂类型'
        ],
        endpoint: '/api/extension/type-handler',
        params: []
      },
      {
        id: 'active-record',
        title: 'ActiveRecord 模式',
        scene: '实体继承 Model<T> 后，不注入 Mapper/Service 也能做单表 CRUD。',
        bagu: [
          'extends Model<T>',
          'article.insert() / Article.selectById(id)',
          '适合简单单表操作，减少样板代码'
        ],
        endpoint: '/api/extension/active-record',
        params: []
      },
      {
        id: 'dynamic-table-name',
        title: '动态表名',
        scene: '按年月分表：同一张逻辑表根据上下文路由到不同物理表。',
        bagu: [
          'DynamicTableNameInnerInterceptor + TableNameHandler',
          '常用于日志、订单按时间分表',
          '需要提前建好物理表'
        ],
        endpoint: '/api/extension/dynamic-table-name',
        params: []
      },
      {
        id: 'insert-batch-some-column',
        title: 'InsertBatchSomeColumn 批量插入',
        scene: '自定义 SQL 注入器，让 BaseMapper 拥有真正的批量插入方法。',
        bagu: [
          '继承 DefaultSqlInjector 添加 InsertBatchSomeColumn',
          '生成 INSERT ... VALUES (...),(...) 一条 SQL',
          '性能远高于循环单条 insert'
        ],
        endpoint: '/api/extension/insert-batch-some-column',
        params: []
      },
      {
        id: 'chain-wrapper',
        title: '链式 Wrapper',
        scene: 'IService 直接链式调用 lambdaQuery / lambdaUpdate。',
        bagu: [
          'userService.lambdaQuery().eq(...).list()',
          'userService.lambdaUpdate().set(...).eq(...).update()',
          '无需手动 new Wrapper'
        ],
        endpoint: '/api/extension/chain-wrapper',
        params: []
      },
      {
        id: 'wrapper-advanced',
        title: '复杂 Wrapper',
        scene: 'inSql 子查询、groupBy + having 聚合统计。',
        bagu: [
          'inSql 适合 where id in (select ...)',
          'groupBy + having 用于分组过滤',
          'selectMaps 配合聚合字段返回'
        ],
        endpoint: '/api/extension/wrapper-advanced',
        params: []
      },
      {
        id: 'select-maps',
        title: 'selectMaps / selectObjs',
        scene: '返回 Map 集合或单列集合，用于报表和下拉框。',
        bagu: [
          'selectMaps 返回 List<Map<String,Object>>',
          'selectObjs 返回单列 List<Object>',
          '灵活指定 select 字段'
        ],
        endpoint: '/api/extension/select-maps',
        params: []
      },
      {
        id: 'wrapper-update-delete',
        title: 'Wrapper 更新 / 删除',
        scene: '按条件批量更新、按条件删除。',
        bagu: [
          'update(null, updateWrapper) 只按 Wrapper 生成 SET',
          'delete(wrapper) 按条件删除',
          '生产上务必加好 WHERE 条件'
        ],
        endpoint: '/api/extension/wrapper-update-delete',
        params: []
      }
    ]
  }
]

// 汇总统计，供界面展示
export const totalScenarios = modules.reduce((sum, m) => sum + m.scenarios.length, 0)
