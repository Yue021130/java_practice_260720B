<script setup>
import { ref, computed } from 'vue'
import ScenarioCard from './components/ScenarioCard.vue'

const modules = ref([
  {
    title: 'Starters 与自动装配',
    desc: '理解 Starter 依赖组合与自动装配原理',
    scenarios: [
      {
        title: '常见 Starter 与能力清单',
        scene: '列出项目引入的 Starter 及其自动装配能力',
        endpoint: 'http://localhost:8088/api/core/starters',
        bagu: [
          'Starter 是“依赖 + 自动配置 + 默认约定”的组合包',
          'spring-boot-starter-web 自动引入 Tomcat、Spring MVC、Jackson',
          '引入 Starter 后，Maven 拉取依赖，Spring Boot 读取 spring.factories 完成自动配置'
        ]
      },
      {
        title: '自动装配 Bean 清单',
        scene: '从容器中查看自动配置的关键 Bean',
        endpoint: 'http://localhost:8088/api/core/auto-config-beans',
        bagu: [
          '@SpringBootApplication 包含 @EnableAutoConfiguration',
          'Spring Boot 读取 META-INF/spring.factories 中 EnableAutoConfiguration 键值',
          '@ConditionalOnClass / @ConditionalOnMissingBean / @ConditionalOnProperty 过滤候选配置类'
        ]
      }
    ]
  },
  {
    title: '配置体系',
    desc: '配置优先级、Profile 与 @ConfigurationProperties',
    scenarios: [
      {
        title: '配置优先级与 Profile 切换',
        scene: '展示命令行、环境变量、配置文件与默认值的优先级',
        endpoint: 'http://localhost:8088/api/core/config-priority',
        bagu: [
          '优先级：命令行参数 > SPRING_APPLICATION_JSON > 环境变量 > application-{profile}.yml > application.yml > 默认值',
          'spring.profiles.active=dev 激活 dev 配置',
          '同名配置后者覆盖前者'
        ]
      },
      {
        title: '@ConfigurationProperties 绑定与校验',
        scene: '将配置前缀批量绑定到 Java Bean 并校验',
        endpoint: 'http://localhost:8088/api/core/config-props',
        bagu: [
          '@ConfigurationProperties 支持宽松绑定：user-name / userName / USER_NAME',
          '可配合 @Validated 做 JSR-303 校验，如 @NotNull、@Range',
          '复杂对象可嵌套绑定'
        ]
      }
    ]
  },
  {
    title: 'IOC 与 Bean 生命周期',
    desc: '生命周期扩展点与条件装配',
    scenarios: [
      {
        title: 'Bean 生命周期回调',
        scene: '按顺序展示 Bean 从创建到销毁的扩展点',
        endpoint: 'http://localhost:8088/api/core/bean-lifecycle',
        bagu: [
          '1) 构造器实例化 -> 2) 属性赋值 -> 3) Aware 接口回调',
          '4) BeanPostProcessor.before -> 5) @PostConstruct',
          '6) InitializingBean.afterPropertiesSet -> 7) init-method',
          '8) BeanPostProcessor.after；销毁：@PreDestroy -> DisposableBean.destroy -> destroy-method'
        ]
      },
      {
        title: '条件装配场景',
        scene: '演示 @ConditionalOnProperty / OnClass / OnMissingBean',
        endpoint: 'http://localhost:8088/api/core/conditional',
        bagu: [
          '@ConditionalOnProperty 根据配置项决定是否注册 Bean',
          '@ConditionalOnClass 根据类路径是否存在决定是否注册',
          '@ConditionalOnMissingBean 防止重复注册'
        ]
      }
    ]
  },
  {
    title: '缓存抽象',
    desc: 'Caffeine、Redis、缓存注解与命中率',
    scenarios: [
      {
        title: '@EnableCaching + Caffeine 基础缓存',
        scene: '演示 @Cacheable 的 key、condition、unless',
        endpoint: 'http://localhost:8088/api/core/cache-caffeine-basic',
        bagu: [
          '@Cacheable 先查缓存，命中直接返回，未命中执行方法并缓存',
          'condition 决定哪些参数进入缓存逻辑',
          'unless 可过滤空结果',
          'Caffeine 是进程内本地缓存，低延迟，适合单机读多写少'
        ]
      },
      {
        title: '缓存注解行为对比',
        scene: '对比 @Cacheable / @CachePut / @CacheEvict',
        endpoint: 'http://localhost:8088/api/core/cache-ops',
        bagu: [
          '@Cacheable：查缓存，命中则返回',
          '@CachePut：始终执行方法，并更新缓存',
          '@CacheEvict：删除缓存；allEntries=true 清空整个 cache'
        ]
      },
      {
        title: '缓存命中率与耗时对比',
        scene: '模拟随机请求，统计命中率和平均耗时',
        endpoint: 'http://localhost:8088/api/core/cache-hit',
        params: [
          { name: 'totalRequests', label: '请求次数', type: 'number', default: 50, min: 10, max: 1000 }
        ],
        bagu: [
          '命中率 = 命中次数 / 总请求',
          '命中率越高，DB 压力越小、响应越快',
          '缓存穿透：缓存与 DB 都不存在的 key 被高频请求，可缓存空值或使用布隆过滤器'
        ]
      },
      {
        title: 'Redis 分布式缓存',
        scene: '使用 RedisCacheManager 写入并读取，验证多实例共享',
        endpoint: 'http://localhost:8088/api/core/cache-redis',
        bagu: [
          'Redis 多实例共享，支持持久化',
          '推荐序列化：StringRedisSerializer + GenericJackson2JsonRedisSerializer',
          '存在网络开销，适合集群环境'
        ]
      },
      {
        title: '本地缓存 vs 分布式缓存',
        scene: '从一致性、延迟、依赖、适用场景对比 Caffeine 与 Redis',
        endpoint: 'http://localhost:8088/api/core/cache-compare',
        bagu: [
          'Caffeine：JVM 内、低延迟、容量受限、多实例不一致',
          'Redis：远程共享、可扩展、支持持久化、有网络开销',
          '常见方案：Caffeine L1 + Redis L2 两级缓存'
        ]
      }
    ]
  },
  {
    title: '生产可观测',
    desc: 'Actuator 端点与安全暴露',
    scenarios: [
      {
        title: 'Actuator health / info 与安全暴露',
        scene: '访问 health/info，查看暴露端点与安全建议',
        endpoint: 'http://localhost:8088/api/core/actuator',
        bagu: [
          'Actuator 暴露 health、info、metrics、loggers 等端点',
          '生产环境应最小化暴露：management.endpoints.web.exposure.include=health,info',
          'env/heapdump/httptrace 会泄露敏感信息，切勿直接对外暴露'
        ]
      }
    ]
  }
])

const activeModule = ref(0)
const activeScenarios = computed(() => modules.value[activeModule.value]?.scenarios || [])
</script>

<template>
  <div class="page">
    <header class="site-header">
      <div class="title-row">
        <h1>Spring Boot 核心能力实战</h1>
        <span class="badge">12 个场景</span>
      </div>
      <p class="subtitle">把面试八股变成可运行的代码</p>
      <p class="backend-hint">后端运行在 <code>http://localhost:8088</code>，Swagger UI <code>/swagger-ui.html</code></p>
    </header>

    <div class="layout">
      <nav class="side-nav">
        <button
          v-for="(m, i) in modules"
          :key="m.title"
          class="nav-item"
          :class="{ active: activeModule === i }"
          @click="activeModule = i"
        >
          <span>{{ m.title }}</span>
          <span class="nav-count">{{ m.scenarios.length }}</span>
        </button>
      </nav>

      <main class="content">
        <div class="module-group">
          <div class="module-head">
            <h2>{{ modules[activeModule].title }}</h2>
            <p class="module-desc">{{ modules[activeModule].desc }}</p>
          </div>
          <div class="card-grid">
            <ScenarioCard
              v-for="s in activeScenarios"
              :key="s.endpoint"
              :scenario="s"
            />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>
