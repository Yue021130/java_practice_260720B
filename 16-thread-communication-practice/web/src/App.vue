<script setup>
import { ref } from 'vue'
import axios from 'axios'

const activeModule = ref('shared')
const results = ref({})
const loading = ref({})

const modules = [
  { key: 'shared', title: '01. 共享内存', desc: 'volatile 可见性 / 原子类 CAS' },
  { key: 'waitnotify', title: '02. 等待通知 wait/notify', desc: 'monitor 等待队列 / 生产者-消费者' },
  { key: 'condition', title: '03. Condition 条件队列', desc: '一锁多队列 / signal 精准唤醒' },
  { key: 'cooperate', title: '04. 线程协作控制', desc: 'join 等待 / interrupt 优雅退出' },
  { key: 'locksupport', title: '05. LockSupport', desc: 'park/unpark / 信号预发 / AQS 基石' },
  { key: 'sync', title: '06. JUC 同步工具', desc: 'Latch / Barrier / Semaphore / Exchanger / Phaser' },
  { key: 'queue', title: '07. 阻塞队列', desc: 'BlockingQueue 家族 / put-take 背压' },
  { key: 'async', title: '08. 异步结果传递', desc: 'FutureTask / CompletableFuture 编排' },
  { key: 'pipe', title: '09. 管道与其他通道', desc: 'PipedStream 单向管道 / 跨进程思路' },
  { key: 'summary', title: '10. 选型总结', desc: '七大类总览 / 选型表 / 底层统一模型' }
]

const scenarios = {
  shared: [
    { key: 'volatile-demo', title: 'volatile 可见性', desc: '主线程置标志，N 个 worker 轮询感知', method: 'get', params: [{ name: 'workers', type: 'select', options: ['4', '8', '16'] }, { name: 'flagDelayMs', type: 'select', options: ['200', '500', '1000'] }], tip: '所有 worker 都能感知到标志：volatile 保证可见性。' },
    { key: 'atomic-demo', title: '原子类 CAS 原子性', desc: '普通 int++ 丢更新 vs AtomicInteger 不丢', method: 'get', params: [{ name: 'threads', type: 'select', options: ['4', '8', '16'] }, { name: 'increments', type: 'select', options: ['1000', '5000', '10000'] }], tip: '线程越多、次数越多，普通 int 丢得越明显。' },
    { key: 'explain', title: 'volatile / CAS 速记（八股）', desc: '三性 / 原子性边界 / ABA / 适用场景', method: 'get', tip: 'volatile 解决看得见，CAS 解决改得对。' }
  ],
  waitnotify: [
    { key: 'producer-consumer', title: 'wait/notify 生产者-消费者', desc: '满/空时 wait，放/取后 notifyAll', method: 'get', params: [{ name: 'productions', type: 'select', options: ['20', '50', '100'] }, { name: 'capacity', type: 'select', options: ['3', '5', '10'] }], tip: '缓冲峰值占用永远 ≤ 容量。' },
    { key: 'explain', title: 'wait/notify 速记（八股）', desc: '为什么 synchronized / 为什么 while 不用 if', method: 'get', tip: 'wait 释放锁进 WaitSet，唤醒后 while 重新检查。' }
  ],
  condition: [
    { key: 'bounded-buffer', title: 'Condition 有界缓冲', desc: 'notFull / notEmpty 两个条件队列', method: 'get', params: [{ name: 'productions', type: 'select', options: ['20', '50', '100'] }, { name: 'capacity', type: 'select', options: ['3', '5', '10'] }], tip: '生产者只等 notFull、消费者只等 notEmpty，不惊群。' },
    { key: 'signal-demo', title: 'signal 精准唤醒', desc: '只唤醒偶数组，奇数组继续沉睡', method: 'get', params: [{ name: 'waiters', type: 'select', options: ['4', '6', '8'] }], tip: '同样的场景 wait/notify 只能广播全部唤醒。' },
    { key: 'explain', title: 'Condition 速记（八股）', desc: '与 wait/notify 区别 / signal vs signalAll', method: 'get', tip: '一个锁、多个队列、点对点。' }
  ],
  cooperate: [
    { key: 'join-demo', title: 'join 等待完成', desc: '主线程等 N 个子任务全部干完', method: 'get', params: [{ name: 'tasks', type: 'select', options: ['3', '5', '8'] }, { name: 'taskMs', type: 'select', options: ['100', '200', '500'] }], tip: '总耗时 ≈ taskMs 而非 tasks×taskMs：join 只等齐不串行化。' },
    { key: 'interrupt-demo', title: 'interrupt 优雅退出', desc: 'sleep 打断抛异常 / 循环里感知退出', method: 'get', params: [{ name: 'mode', type: 'select', options: ['sleep', 'loop'] }], tip: 'interrupt 是打招呼不是强杀：线程自己决定何时退出。' },
    { key: 'explain', title: 'join / interrupt 速记（八股）', desc: 'join 底层 / 中断状态清理', method: 'get', tip: 'catch InterruptedException 后要恢复中断标志。' }
  ],
  locksupport: [
    { key: 'park-unpark', title: 'park 后 unpark', desc: '线程 park 挂起，主线程延迟唤醒', method: 'get', params: [{ name: 'delayMs', type: 'select', options: ['200', '500', '1000'] }], tip: '无需持锁、精确到线程：wait/notify 做不到。' },
    { key: 'unpark-first', title: '先 unpark 后 park（信号预发）', desc: 'permit 提前发，park 立即通过', method: 'get', tip: '换成 wait/notify 的同样顺序会永久死锁。' },
    { key: 'explain', title: 'LockSupport 速记（八股）', desc: '三优势 / permit 机制 / AQS 基石', method: 'get', tip: 'permit 最多累计 1 个，不能当计数器。' }
  ],
  sync: [
    { key: 'latch-demo', title: 'CountDownLatch 倒计时门闩', desc: '主线程等 N 个 worker（一次性）', method: 'get', params: [{ name: 'workers', type: 'select', options: ['3', '5', '8'] }], tip: '一等多：计数归零后不可复用。' },
    { key: 'barrier-demo', title: 'CyclicBarrier 循环栅栏', desc: 'N 线程到齐才放行，可循环多轮', method: 'get', params: [{ name: 'parties', type: 'select', options: ['3', '5'] }, { name: 'rounds', type: 'select', options: ['2', '3', '5'] }], tip: '多等多 + 可复用：与 Latch 的核心区别。' },
    { key: 'semaphore-demo', title: 'Semaphore 信号量限流', desc: '同时最多 permits 个进入', method: 'get', params: [{ name: 'permits', type: 'select', options: ['2', '3', '5'] }, { name: 'threads', type: 'select', options: ['8', '16', '32'] }], tip: '并发峰值永远 ≤ 许可数。' },
    { key: 'exchanger-demo', title: 'Exchanger 数据交换', desc: '两线程碰头双向交换', method: 'get', tip: '两个线程必须同时到达才会完成交换。' },
    { key: 'phaser-demo', title: 'Phaser 阶段器', desc: '多阶段 + 中途 register/deregister', method: 'get', params: [{ name: 'parties', type: 'select', options: ['3', '5'] }], tip: 'Latch + Barrier 合体，支持动态增减。' },
    { key: 'explain', title: '同步工具速记（八股）', desc: '五件套对比表 / 适用场景', method: 'get', tip: 'Latch 一等多一次性，Barrier 多等多可循环。' }
  ],
  queue: [
    { key: 'blocking-demo', title: 'put/take 阻塞（背压）', desc: '满则阻塞生产者、空则阻塞消费者', method: 'get', params: [{ name: 'productions', type: 'select', options: ['20', '50', '100'] }, { name: 'capacity', type: 'select', options: ['3', '5', '10'] }], tip: '队列满时生产者放慢，不会 OOM：这就是背压。' },
    { key: 'family', title: '阻塞队列家族速览', desc: 'Array / Linked / Synchronous / Priority / Delay', method: 'get', tip: '四种行为按「抛异常-特殊值-阻塞-超时」记忆。' },
    { key: 'explain', title: '阻塞队列速记（八股）', desc: '原理(Condition) / 背压 / 选型', method: 'get', tip: '阻塞队列 = 锁 + 条件队列 + 数组/链表。' }
  ],
  async: [
    { key: 'future-demo', title: 'FutureTask 拿结果', desc: '跨线程传返回值，get() 阻塞等', method: 'get', params: [{ name: 'taskMs', type: 'select', options: ['80', '200', '500'] }], tip: 'Future 是「异步计算 + 阻塞取结果」的最小形态。' },
    { key: 'cf-demo', title: 'CompletableFuture 链式编排', desc: 'supplyAsync → thenApply → thenApplyAsync', method: 'get', params: [{ name: 'taskMs', type: 'select', options: ['50', '100', '200'] }], tip: '每步自动衔接上一步结果，无需手写线程同步。' },
    { key: 'cf-combine', title: 'allOf / anyOf / 异常兜底', desc: '等全部 / 任一先完成 / exceptionally', method: 'get', params: [{ name: 'tasks', type: 'select', options: ['3', '5'] }], tip: '组合与容错一次到位。' },
    { key: 'explain', title: '异步结果速记（八股）', desc: 'Future 三痛点 vs CompletableFuture 四板斧', method: 'get', tip: '生产必须传独立线程池，别用 commonPool。' }
  ],
  pipe: [
    { key: 'piped-demo', title: 'PipedStream 单向管道', desc: '写线程写、读线程读', method: 'get', params: [{ name: 'messages', type: 'select', options: ['5', '10', '20'] }], tip: '官方不建议单线程两端都用，可能死锁。' },
    { key: 'cross-process', title: '跨进程通道思路', desc: 'Socket 回环 / 共享内存 / 文件', method: 'get', tip: '线程间用这些属于杀鸡用牛刀。' },
    { key: 'explain', title: '管道速记（八股）', desc: '本质 / 死锁警告 / 线程内 vs 跨进程', method: 'get', tip: '知道有这回事即可，真用还是队列和锁。' }
  ],
  summary: [
    { key: 'overview', title: '七大类总览', desc: '线程间通信方式全景图', method: 'get', tip: '由轻到重、由线程内到跨进程记忆。' },
    { key: 'decision-table', title: '选型表', desc: '需求场景 → 首选方案', method: 'get', tip: '先想清楚需求再选工具。' },
    { key: 'unified-model', title: '底层统一模型', desc: '业务层 → AQS → LockSupport → futex', method: 'get', tip: '除了轮询，都是「阻塞 + 等待队列 + 唤醒」。' },
    { key: 'explain', title: '总结速记（八股）', desc: '七大类一句话记忆 + 面试回答流程', method: 'get', tip: '把「殊途同归」讲出来就是加分项。' }
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
      <h1>Java 线程间<br>通信方式实践</h1>
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
