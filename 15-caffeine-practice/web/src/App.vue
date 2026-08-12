<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('preheat')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'basic', title: '01. 快速开始', desc: 'Caffeine 概览 / 手动 Cache / LoadingCache' },
  { key: 'eviction', title: '02. 淘汰策略', desc: '容量淘汰 / 时间淘汰（write vs access）' },
  { key: 'refresh', title: '03. 刷新与异步', desc: 'refreshAfterWrite / AsyncCache' },
  { key: 'stats', title: '04. 统计与监控', desc: '命中率 / 淘汰数 / 加载耗时' },
  { key: 'preheat', title: '05. 缓存预热', desc: '启动自动预热 / 手动触发 / 命中率对比' },
  { key: 'stampede', title: '06. 穿透/击穿/雪崩', desc: '空值缓存 / 击穿现场 / 单飞' },
  { key: 'twolevel', title: '07. 两级缓存', desc: 'L1 Caffeine + L2 Redis(模拟) + DB' },
  { key: 'spring', title: '08. Spring Cache 注解', desc: '@Cacheable / @CachePut / @CacheEvict / @Caching' },
  { key: 'consistency', title: '09. 缓存一致性', desc: 'Cache Aside / 双删 / 一致性模式' },
  { key: 'pitfall', title: '10. 常见坑与调优', desc: '10 个高频坑 / SpEL key 陷阱' }
]

const scenarios = {
  basic: [
    { key: 'cache-demo', title: '手动 Cache 全流程', desc: 'miss → 查库 → put → hit → invalidate', method: 'get', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }], tip: '第一次 miss 查库（30ms+），第二次 hit 纯内存（<1ms）。' },
    { key: 'loading', title: 'LoadingCache 自动加载', desc: 'get 未命中自动走 CacheLoader', method: 'get', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }], tip: 'LoadingCache 并发下同一 key 只加载一次（自带单飞）。' },
    { key: 'info', title: '核心概念速记', desc: 'Caffeine 是什么 / 与 Guava、Redis 对比', method: 'get', tip: '本地缓存适合：读多写少、可接受短暂不一致的热点数据。' }
  ],
  eviction: [
    { key: 'size-demo', title: '容量淘汰', desc: 'maximumSize=5，放 N 个看淘汰', method: 'get', params: [{ name: 'count', type: 'select', options: ['6', '12', '20'] }], tip: '不是严格 LRU（Window-TinyLFU），高频的活下来。' },
    { key: 'expire-demo', title: '时间淘汰 write vs access', desc: '一个读不续命、一个读续命', method: 'get', params: [{ name: 'type', type: 'select', options: ['write', 'access'] }, { name: 'durationMs', type: 'select', options: ['60', '150', '300'] }], tip: 'expireAfterWrite 固定过期；expireAfterAccess 每次读取都续命。' },
    { key: 'explain', title: '淘汰策略速记', desc: '容量 / 时间 / 引用三类', method: 'get', tip: '真实业务 TTL 几乎都是 expireAfterWrite + refreshAfterWrite。' }
  ],
  refresh: [
    { key: 'refresh-demo', title: '定时刷新演示', desc: '超过刷新间隔后读不阻塞、后台异步刷新', method: 'get', params: [{ name: 'waitMs', type: 'select', options: ['0', '500', '3000'] }], tip: 'waitMs 超过刷新间隔(默认2s)才能看到「读秒回+后台刷新」。' },
    { key: 'async-demo', title: '异步加载演示', desc: 'AsyncCache 返回 CompletableFuture', method: 'get', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }], tip: '加载在线程池完成，不阻塞请求线程。' },
    { key: 'explain', title: '刷新与异步速记', desc: 'refresh vs expire / 黄金组合', method: 'get', tip: 'refreshAfterWrite + 较长 expireAfterWrite = 防击穿又保新鲜。' }
  ],
  stats: [
    { key: 'demo', title: '统计采样', desc: '跑 N 次访问，看命中率/查库次数', method: 'get', params: [{ name: 'accesses', type: 'select', options: ['100', '500', '1000'] }], tip: '命中率骤降 = 缓存有问题，要告警。' },
    { key: 'explain', title: '统计指标速记', desc: 'recordStats / 各指标含义 / 监控', method: 'get', tip: '生产接 Micrometer + Prometheus 看命中率/淘汰率看板。' }
  ],
  preheat: [
    { key: 'warm', title: '手动触发预热', desc: '把热门 key 提前装进缓存', method: 'post', btnClass: 'btn-warm', tip: '幂等：预热中再次触发会被忽略；成功后再次触发重新预热。' },
    { key: 'status', title: '预热状态', desc: 'PENDING / RUNNING / SUCCESS / FAILED', method: 'get', tip: '启动完成会自动预热（开关在 yml），这里查状态。' },
    { key: 'stats', title: '预热收益对比', desc: '预热前后命中率 / 探测读', method: 'get', tip: '冷启动命中率≈0 → 预热后热门 key 基本全命中。' },
    { key: 'config', title: '预热配置', desc: '开关 / key 数 / 批次 / 容量', method: 'get', tip: '预热数量别超过 maximumSize，否则白热。' },
    { key: 'explain', title: '预热速记（八股）', desc: '为什么预热 / 静态与动态 / 时机', method: 'get', tip: '预热的本质：把 miss 从流量高峰提前到空闲期。' }
  ],
  stampede: [
    { key: 'overview', title: '三大问题速记', desc: '穿透 / 击穿 / 雪崩 现象与应对', method: 'get', tip: '穿透是「挡」、击穿是「合并」、雪崩是「错峰+兜底」。' },
    { key: 'null-demo', title: '空值缓存防穿透', desc: '不存在的 key：不缓存 vs 空值缓存', method: 'get', params: [{ name: 'times', type: 'select', options: ['20', '50'] }], tip: '空值缓存 TTL 要比正常数据短。' },
    { key: 'stampede-demo', title: '击穿现场（无保护）', desc: '热点 key 过期瞬间 N 线程打 DB', method: 'get', params: [{ name: 'threads', type: 'select', options: ['5', '20', '50'] }], tip: '没有保护：N 个线程全打到 DB。' },
    { key: 'singleflight', title: '单飞保护（只查一次）', desc: 'N 并发合并成 1 次加载', method: 'get', params: [{ name: 'threads', type: 'select', options: ['5', '20', '50'] }], tip: 'actualLoads=1：这就是击穿的标准解法。' },
    { key: 'explain', title: '单飞与逻辑过期速记', desc: '单飞实现 / 逻辑过期 / 注意点', method: 'get', tip: '加载过程里别再回调同一个 key；失败要能重试。' }
  ],
  twolevel: [
    { key: 'get', title: '读路径 L1→L2→DB', desc: '命中哪一级从哪一级回填', method: 'get', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }], tip: '连续点几次，来源从 DB → L2 → L1 逐级变快。' },
    { key: 'put', title: '写路径（Cache Aside）', desc: '先更库再删 L1+L2', method: 'post', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }, { name: 'name' }, { name: 'dept' }], tip: '删缓存而不是更新缓存，避免并发写顺序错乱。' },
    { key: 'evict', title: '删两级缓存', desc: '强制下次读走 DB 回填', method: 'post', params: [{ name: 'id', type: 'select', options: ['1', '2', '5'] }], tip: '改配置/修 Bug 后手动清缓存用。' },
    { key: 'consistency', title: '一致性策略说明', desc: 'Cache Aside / 双删 / 读写穿', method: 'get', tip: 'L1 TTL 10s + L2 TTL 30s，漏删也 TTL 兜底自愈。' },
    { key: 'explain', title: '两级缓存速记', desc: '为什么两级 / 各层配置 / 注意点', method: 'get', tip: '秒杀/商品详情是两级缓存最佳舞台。' }
  ],
  spring: [
    { key: 'query', title: '@Cacheable 查询', desc: '连读两次，看打库次数', method: 'get', params: [{ name: 'id', type: 'select', options: ['10', '11', '12'] }], tip: '第一次 miss 打库，第二次命中（dbLoads 不再增加）。' },
    { key: 'update', title: '@CachePut 更新', desc: '更新 DB 并把新值写缓存', method: 'post', params: [{ name: 'id', type: 'select', options: ['10', '11', '12'] }, { name: 'name' }], tip: '@CachePut 每次都执行并回写缓存。' },
    { key: 'delete', title: '@CacheEvict 删除', desc: '剔除缓存 key，下次读重新打库', method: 'post', params: [{ name: 'id', type: 'select', options: ['10', '11', '12'] }], tip: '@CacheEvict 让数据重新加载而不是留脏缓存。' },
    { key: 'multi', title: '@Caching 组合', desc: '一次操作清两个缓存', method: 'post', params: [{ name: 'id', type: 'select', options: ['10', '11', '12'] }], tip: '@Caching 可组合多缓存/多 key 一次清。' },
    { key: 'explain', title: '注解速记（八股）', desc: 'Cacheable/CachePut/CacheEvict 区别', method: 'get', tip: '注解靠代理生效：自调用 this.xxx() 不拦截。' }
  ],
  consistency: [
    { key: 'aside-demo', title: 'Cache Aside 现场', desc: '只更库不删缓存 → 脏数据', method: 'get', params: [{ name: 'id', type: 'select', options: ['2', '4', '6'] }], tip: '更库后删缓存，下次读自动加载新值。' },
    { key: 'double-delete-demo', title: '双删演示', desc: '写前删 + 写后删压掉竞态窗口', method: 'get', params: [{ name: 'id', type: 'select', options: ['3', '5', '7'] }], tip: '延迟双删把第二删再延后几百 ms。' },
    { key: 'patterns', title: '一致性模式对比', desc: 'Cache Aside / 读写穿 / 写回', method: 'get', tip: '没有银弹：能接受最终一致就用 Cache Aside + 短 TTL。' },
    { key: 'explain', title: '一致性速记（八股）', desc: '为什么删缓存 / 双删 / binlog', method: 'get', tip: '先说「永远存在窗口」，再讲 Cache Aside + 双删 + binlog。' }
  ],
  pitfall: [
    { key: 'list', title: '10 个高频坑清单', desc: '现象 → 原因 → 解法', method: 'get', tip: '先看清单再动手，少走弯路。' },
    { key: 'key-demo', title: 'SpEL key 陷阱现场', desc: '等价对象因 toString 不同缓存永远 miss', method: 'get', tip: 'key 永远用业务主键（id），别用对象本身。' },
    { key: 'tuning', title: '调优要点（八股）', desc: '容量 / TTL / 预热 / 监控 / 大促三件套', method: 'get', tip: '先看命中率 → 再看淘汰率 → 最后才动容量/TTL。' }
  ]
}

const paramValues = ref({})

function defaultParams(key) {
  const defs = {}
  for (const s of scenarios[activeModule.value]) {
    if (s.key === key) {
      for (const p of (s.params || [])) {
        defs[p.name] = p.options ? p.options[0] : ''
      }
    }
  }
  return defs
}

async function callScenario(scenario) {
  const cacheKey = `${activeModule.value}-${scenario.key}`
  loading.value[cacheKey] = true
  results.value[cacheKey] = ''

  try {
    const params = paramValues.value[cacheKey] || defaultParams(scenario.key)
    const url = `/api/${activeModule.value}/${scenario.key}`
    let res
    if (scenario.method === 'get') {
      res = await axios.get(url, { params })
    } else {
      res = await axios.post(url, null, { params })
    }
    results.value[cacheKey] = JSON.stringify(res.data, null, 2)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[cacheKey] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[cacheKey] = false
  }
}

function setDefaultParams() {
  for (const s of scenarios[activeModule.value]) {
    const cacheKey = `${activeModule.value}-${s.key}`
    if (!paramValues.value[cacheKey]) {
      paramValues.value[cacheKey] = defaultParams(s.key)
    }
  }
}

setDefaultParams()
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Spring Boot +<br>Caffeine 缓存实践</h1>
      <div
        v-for="m in modules"
        :key="m.key"
        :class="['menu-item', { active: activeModule === m.key }]"
        @click="activeModule = m.key; setDefaultParams()"
      >
        {{ m.title }}
      </div>
    </aside>
    <main class="content">
      <h2 class="module-title">
        {{ modules.find(m => m.key === activeModule).title }}
      </h2>
      <p class="module-desc">{{ modules.find(m => m.key === activeModule).desc }}</p>
      <div class="cards">
        <div v-for="s in scenarios[activeModule]" :key="s.key" class="card">
          <h3>{{ s.title }}</h3>
          <p>{{ s.desc }}</p>
          <div v-if="s.params" class="params">
            <label v-for="p in s.params" :key="p.name">
              {{ p.name }}:
              <select v-if="p.type === 'select'" v-model="paramValues[`${activeModule}-${s.key}`][p.name]">
                <option v-for="opt in p.options" :key="opt" :value="opt">{{ opt }}</option>
              </select>
              <input v-else v-model="paramValues[`${activeModule}-${s.key}`][p.name]" />
            </label>
          </div>
          <button
            class="btn"
            :class="s.btnClass || ''"
            @click="callScenario(s)"
            :disabled="loading[`${activeModule}-${s.key}`]"
          >
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : '运行实验' }}
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>
