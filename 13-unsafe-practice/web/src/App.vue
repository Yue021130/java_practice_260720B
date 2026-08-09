<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const activeModule = ref('intro')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'intro', title: '01. 初识 Unsafe', desc: '获取实例、六大能力地图、getUnsafe 为什么被堵死' },
  { key: 'memory', title: '02. 堆外内存', desc: 'allocateMemory / setMemory / copyMemory / 泄漏风险' },
  { key: 'instance', title: '03. 绕过构造器', desc: 'allocateInstance 不调构造器造对象' },
  { key: 'cas', title: '04. CAS 原子操作', desc: '自旋计数器、三种自增对比、ABA 现场复现' },
  { key: 'offset', title: '05. 字段偏移与对象布局', desc: 'objectFieldOffset / 打破封装 / 数组定位' },
  { key: 'park', title: '06. park/unpark', desc: '线程阻塞与唤醒、许可证机制' },
  { key: 'fence', title: '07. 内存屏障', desc: 'loadFence / storeFence / fullFence / volatile' },
  { key: 'essence', title: '08. 危险与本质', desc: '四大风险、本质、JDK 演变与 VarHandle' }
]

const scenarios = {
  intro: [
    { key: 'info', title: '实例与能力地图', desc: '验证反射拿到的 Unsafe 可用，输出六大能力分区', method: 'get', tip: 'Unsafe 实例来自 UnsafeConfig：反射读取内部字段 theUnsafe（getUnsafe() 对普通类会抛 SecurityException）。' },
    { key: 'getunsafe-demo', title: 'getUnsafe() 正规入口演示', desc: '普通应用直接调用必抛 SecurityException', method: 'get', tip: '只有 Bootstrap 类加载器加载的类才能拿到 Unsafe，这是 JDK 的自我保护。' },
    { key: 'why', title: '为什么叫魔法类 / 为什么禁用', desc: '魔法类名字的由来与四类禁用原因', method: 'get', tip: '生产代码绝不直接使用 Unsafe；需要底层能力走 JUC / VarHandle / 第三方库。' }
  ],
  memory: [
    { key: 'allocate', title: '堆外内存分配与读写', desc: 'allocateMemory → putInt/getInt → freeMemory', method: 'post', params: [{ name: 'count', type: 'select', options: ['5', '10', '100'] }], tip: '分配返回裸内存地址，按 地址 + i*4 偏移读写，用完必须 freeMemory。' },
    { key: 'setcopy', title: 'setMemory 填充 + copyMemory 拷贝', desc: '批量填 0x5A 再整体拷贝，十六进制验证', method: 'get', tip: 'setMemory 是批量填充，copyMemory 是 memcpy 级别，比逐字节循环快得多。' },
    { key: 'leak', title: '堆外内存泄漏风险演示', desc: '分配 N 块 1MB 堆外内存，观察堆几乎不变', method: 'post', params: [{ name: 'blocks', type: 'select', options: ['2', '5', '10'] }], tip: '堆外内存不归 GC 管：看 heapGrewMb 几乎为 0，只有 freeMemory 能释放，忘了就是泄漏。' }
  ],
  instance: [
    { key: 'create', title: 'allocateInstance 演示', desc: '不调用构造器创建对象，对比 new 的差异', method: 'get', tip: '看 countAfterUnsafe 没变：构造器完全没跑，final 字段是 0、name 是 null、连初始化 -1 都没执行。' },
    { key: 'compare', title: 'new vs allocateInstance 对比表', desc: '构造器/字段/final/校验/速度/用途逐项对比', method: 'get', tip: 'Kryo 反序列化快的原因之一就是 allocateInstance 直接造空对象。' },
    { key: 'uses', title: '真实用途与风险', desc: 'Kryo / 深拷贝 / 单例破解 / 反序列化攻击面', method: 'get', tip: '绕过构造器校验也是反序列化漏洞的土壤，现代框架会做类白名单。' }
  ],
  cas: [
    { key: 'spin', title: '自旋 CAS 计数器', desc: 'getIntVolatile + compareAndSwapInt 循环自增', method: 'post', params: [{ name: 'times', type: 'select', options: ['100000', '500000', '1000000'] }], tip: '单线程下每次 CAS 一次成功，所以 totalCasAttempts = times；多线程竞争时会明显变多。' },
    { key: 'benchmark', title: '三种自增性能对比', desc: 'synchronized / AtomicInteger / Unsafe CAS 并发自增', method: 'post', params: [{ name: 'threads', type: 'select', options: ['2', '4', '8'] }, { name: 'times', type: 'select', options: ['100000', '200000', '500000'] }], tip: '耗时随机器负载波动，看相对关系；CAS 竞争激烈时自旋空转可能反而更慢，这就是 LongAdder 分段计数的原因。' },
    { key: 'aba', title: 'ABA 问题现场复现', desc: '线程 B 折腾 100→200→100，比较两种 CAS 结局', method: 'get', tip: '无版本号：余额数值没变，A 的 CAS 成功（有隐患）；带版本号：0→1→2 变了，A 的 CAS 失败（安全）。' },
    { key: 'explain', title: 'CAS 原理八股速记', desc: '是什么 / 与锁的区别 / 三大问题 / JUC 谁在用', method: 'get', tip: '面试必问：CAS 与 synchronized 区别？三大问题？ABA 如何解决？' }
  ],
  offset: [
    { key: 'fields', title: '字段偏移量一览', desc: '打印 LayoutDemo 各字段的 objectFieldOffset', method: 'get', tip: '第一个字段偏移 ≈ 对象头大小（JDK 17 默认压缩指针为 12B）。声明顺序 ≠ 内存顺序，中间有填充。' },
    { key: 'directwrite', title: '用偏移量打破封装', desc: '绕过 getter/setter 直接读写 private 字段', method: 'get', tip: 'before=42 → putInt(offset) → 999。私有性只是语言层面的纸老虎。' },
    { key: 'array', title: '数组元素定位', desc: 'arrayBaseOffset + arrayIndexScale 直接寻址', method: 'get', tip: '第 i 个元素地址 = arrayBaseOffset + i * arrayIndexScale，ConcurrentHashMap 等就这么干。' },
    { key: 'layout', title: '对象内存布局示意图', desc: '对象头 / 字段区 / 对齐填充', method: 'get', tip: '锁状态存在 Mark Word；理解布局才懂 synchronized 升级与伪共享。' }
  ],
  park: [
    { key: 'demo', title: 'park/unpark 现场演示', desc: '正常顺序唤醒 + 提前 unpark 许可证机制', method: 'get', tip: '场景二：先 unpark 再 park 也能立即返回，这是 wait/notify 做不到的。' },
    { key: 'compare', title: 'park vs wait/notify 对比表', desc: '锁要求 / 顺序 / 精确唤醒 / 超时 / 中断', method: 'get', tip: '一句话：wait/notify 是对象级别，park/unpark 是线程级别。' },
    { key: 'explain', title: 'LockSupport 原理', desc: '许可证机制 / AQS 里怎么用 / 为什么不用 wait', method: 'get', tip: '答出“许可证”“可指定线程”“无需锁”三点就稳了。' }
  ],
  fence: [
    { key: 'demo', title: '内存屏障现场演示', desc: '普通字段 + 屏障手写 volatile 效果', method: 'get', tip: '没有 StoreStore 屏障，写方可能“先置就绪、后写数据”，读方就会看到 ready=1 但 data 是旧值。' },
    { key: 'explain', title: 'JMM 与 volatile 底层', desc: '两层语义 / 8 种内存操作 / 4 条 Happens-Before', method: 'get', tip: 'volatile 不保证原子性！i++ 还是要 CAS/锁。' }
  ],
  essence: [
    { key: 'risks', title: '四大风险', desc: '越界崩溃 / 堆外泄漏 / 破坏封装 / 不可移植', method: 'get', tip: '越界访问不抛 Java 异常，直接 SIGSEGV 让 JVM 崩溃——所以本实验只展示代码形态。' },
    { key: 'essence', title: '本质', desc: 'Unsafe 是什么、为什么存在、绕过了哪些防线', method: 'get', tip: '它绕过了类型安全、封装、内存管理三道 Java 防线——这就是“魔法”的来源。' },
    { key: 'evolution', title: 'JDK 演变与 VarHandle', desc: '从 JDK 8 到模块化到 JEP 193 VarHandle', method: 'get', tip: '新代码请直接学 VarHandle，它是官方给的方向；Unsafe 短期不会被移除。' },
    { key: 'whouses', title: '谁在用 Unsafe', desc: 'JUC / Netty / Kafka / Cassandra / 序列化框架', method: 'get', tip: '说出“ConcurrentHashMap 用 CAS 扩容、Netty 用 Unsafe 分配堆外内存”会很懂底层。' }
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

onMounted(async () => {
  try {
    await axios.get('/api/intro/info')
  } catch (e) {
    // 后端未启动时静默
  }
})
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>魔法类<br>Unsafe 实践</h1>
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
      <h2 class="module-title">{{ modules.find(m => m.key === activeModule).title }}</h2>
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
          <button class="btn" @click="callScenario(s)" :disabled="loading[`${activeModule}-${s.key}`]">
            {{ loading[`${activeModule}-${s.key}`] ? '执行中...' : '运行实验' }}
          </button>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
          <pre v-if="results[`${activeModule}-${s.key}`]" class="result">{{ results[`${activeModule}-${s.key}`] }}</pre>
        </div>
      </div>
    </main>
  </div>
</template>
