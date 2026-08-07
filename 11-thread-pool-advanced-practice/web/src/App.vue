<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

const activeModule = ref('flow')
const loading = ref({})
const results = ref({})
const metrics = ref([])
let metricsTimer = null

const modules = [
  { key: 'flow', title: '01. 源码流程', desc: 'ThreadPoolExecutor.execute() 七步流程' },
  { key: 'pool', title: '02. 线程池基础', desc: '预定义池、自定义创建、实时监控、优雅关闭' },
  { key: 'queue', title: '03. 阻塞队列', desc: '7 种 BlockingQueue 特性对比与实验' },
  { key: 'rejection', title: '04. 拒绝策略', desc: '4 种 JDK 拒绝策略 + 自定义策略实验' },
  { key: 'executors', title: '05. Executors 工厂', desc: '4 种工厂源码、特点、OOM 风险' },
  { key: 'source', title: '06. 生命周期 & Worker', desc: '线程池五态与 Worker 内部类' }
]

// ========== 通用请求 ==========
async function request(key, method, url, paramsOrData = null) {
  loading.value[key] = true
  results.value[key] = ''
  try {
    const res = method === 'get'
      ? await axios.get(url, { params: paramsOrData })
      : await axios.post(url, paramsOrData)
    results.value[key] = JSON.stringify(res.data, null, 2)
  } catch (err) {
    const data = err.response ? err.response.data : { message: err.message }
    results.value[key] = '请求异常：\n' + JSON.stringify(data, null, 2)
  } finally {
    loading.value[key] = false
  }
}

function getResult(key) {
  return results.value[key] || ''
}

// ========== 实时指标 ==========
async function loadMetrics() {
  try {
    const res = await axios.get('/api/pool/metrics')
    if (res.data && res.data.code === 200) {
      metrics.value = res.data.data
    }
  } catch (e) {
    console.error('拉取指标失败', e)
  }
}

onMounted(() => {
  loadMetrics()
  metricsTimer = setInterval(loadMetrics, 2000)
})

onUnmounted(() => {
  if (metricsTimer) clearInterval(metricsTimer)
})

// ========== 02. 线程池基础 ==========
const submitForm = ref({ poolId: 'tinyPool', count: 20, taskDurationMs: 1000 })
const customForm = ref({
  poolId: 'myPool',
  corePoolSize: 2,
  maximumPoolSize: 4,
  keepAliveTime: 0,
  timeUnit: 'SECONDS',
  queueCapacity: 5,
  queueType: 'ArrayBlockingQueue',
  rejectionPolicy: 'AbortPolicy',
  threadFactoryPrefix: 'my'
})
const customSubmitForm = ref({ poolId: 'myPool', count: 10, taskDurationMs: 500 })
const shutdownPoolId = ref('myPool')

async function submitPredefined() {
  await request('submitPredefined', 'post', `/api/pool/predefined/${submitForm.value.poolId}/submit`, submitForm.value)
}

async function createCustom() {
  await request('createCustom', 'post', '/api/pool/custom/create', customForm.value)
}

async function submitCustom() {
  await request('submitCustom', 'post', `/api/pool/custom/${customSubmitForm.value.poolId}/submit`, customSubmitForm.value)
}

async function shutdown(now) {
  const url = `/api/pool/${shutdownPoolId.value}/${now ? 'shutdownNow' : 'shutdown'}`
  await request('shutdown', 'post', url)
}

// ========== 03. 阻塞队列 ==========
const queueTypes = ref([])
const queueForm = ref({ type: 'ArrayBlockingQueue', capacity: 3, submitCount: 10 })

async function loadQueueTypes() {
  const res = await axios.get('/api/queue/types')
  if (res.data && res.data.code === 200) queueTypes.value = res.data.data
}

async function experimentQueue() {
  const { type, capacity, submitCount } = queueForm.value
  await request('queueExperiment', 'post', `/api/queue/${type}/experiment?capacity=${capacity}&submitCount=${submitCount}`)
}

// ========== 04. 拒绝策略 ==========
const rejectionPolicies = ['AbortPolicy', 'CallerRunsPolicy', 'DiscardPolicy', 'DiscardOldestPolicy']
const rejectionForm = ref({ policy: 'AbortPolicy', submitCount: 10 })

async function experimentRejection() {
  const { policy, submitCount } = rejectionForm.value
  await request('rejectionExperiment', 'post', `/api/rejection/${policy}/experiment?submitCount=${submitCount}`)
}

// ========== 05. Executors 工厂 ==========
const executorsTypes = ['FixedThreadPool', 'SingleThreadExecutor', 'CachedThreadPool', 'ScheduledThreadPool']
const executorsResult = ref('')

async function demoExecutors(type) {
  await request(`executors-${type}`, 'post', `/api/executors/${type}/demo`)
}

// ========== 06. 生命周期 & Worker ==========
const lifecycleStates = ref([])
const workerIntros = ref([])

async function loadLifecycle() {
  const res = await axios.get('/api/source/lifecycle-states')
  if (res.data && res.data.code === 200) lifecycleStates.value = res.data.data
}

async function loadWorker() {
  const res = await axios.get('/api/source/worker-intro')
  if (res.data && res.data.code === 200) workerIntros.value = res.data.data
}

onMounted(() => {
  loadQueueTypes()
  loadLifecycle()
  loadWorker()
})
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <h1>Java 线程池<br>深度实践</h1>
      <p class="subtitle">基于 CSDN 文章扩展</p>
      <div
        v-for="m in modules"
        :key="m.key"
        :class="['menu-item', { active: activeModule === m.key }]"
        @click="activeModule = m.key"
      >
        {{ m.title }}
      </div>
    </aside>

    <main class="content">
      <h2 class="module-title">{{ modules.find(m => m.key === activeModule).title }}</h2>
      <p class="module-desc">{{ modules.find(m => m.key === activeModule).desc }}</p>

      <!-- 01. 源码流程 -->
      <div v-if="activeModule === 'flow'" class="cards">
        <div class="card full">
          <h3>ThreadPoolExecutor.execute() 执行流程</h3>
          <div class="flow">
            <div class="flow-step">
              <div class="step-num">1</div>
              <div>
                <strong>读取 ctl</strong>
                <p>高 3 位存运行状态，低 29 位存 worker 数量</p>
              </div>
            </div>
            <div class="arrow">↓</div>
            <div class="flow-step">
              <div class="step-num">2</div>
              <div>
                <strong>workerCount &lt; corePoolSize</strong>
                <p>addWorker(command, true) 创建核心线程</p>
              </div>
            </div>
            <div class="arrow">↓ 否</div>
            <div class="flow-step">
              <div class="step-num">3</div>
              <div>
                <strong>任务入队</strong>
                <p>workQueue.offer(command)</p>
              </div>
            </div>
            <div class="arrow">↓ 失败</div>
            <div class="flow-step">
              <div class="step-num">4</div>
              <div>
                <strong>workerCount &lt; maximumPoolSize</strong>
                <p>addWorker(command, false) 创建非核心线程</p>
              </div>
            </div>
            <div class="arrow">↓ 否</div>
            <div class="flow-step warn">
              <div class="step-num">5</div>
              <div>
                <strong>执行拒绝策略</strong>
                <p>handler.rejectedExecution()</p>
              </div>
            </div>
            <div class="arrow">↓</div>
            <div class="flow-step">
              <div class="step-num">6</div>
              <div>
                <strong>Worker 取任务执行</strong>
                <p>runWorker → getTask → 执行任务</p>
              </div>
            </div>
            <div class="arrow">↓</div>
            <div class="flow-step">
              <div class="step-num">7</div>
              <div>
                <strong>线程回收</strong>
                <p>keepAliveTime 内无任务则非核心线程退出</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 02. 线程池基础 -->
      <div v-if="activeModule === 'pool'" class="cards">
        <div class="card">
          <h3>向预定义池提交任务</h3>
          <div class="form-row">
            <label>池：<select v-model="submitForm.poolId"><option>cpuPool</option><option>ioPool</option><option>tinyPool</option></select></label>
            <label>任务数：<input type="number" v-model.number="submitForm.count" /></label>
            <label>任务耗时(ms)：<input type="number" v-model.number="submitForm.taskDurationMs" /></label>
          </div>
          <button class="btn" @click="submitPredefined" :disabled="loading.submitPredefined">{{ loading.submitPredefined ? '提交中...' : '提交' }}</button>
          <pre v-if="getResult('submitPredefined')" class="result">{{ getResult('submitPredefined') }}</pre>
        </div>

        <div class="card">
          <h3>创建自定义线程池</h3>
          <div class="form-grid">
            <label>poolId：<input v-model="customForm.poolId" /></label>
            <label>core：<input type="number" v-model.number="customForm.corePoolSize" /></label>
            <label>max：<input type="number" v-model.number="customForm.maximumPoolSize" /></label>
            <label>keepAliveTime：<input type="number" v-model.number="customForm.keepAliveTime" /></label>
            <label>unit：<input v-model="customForm.timeUnit" /></label>
            <label>queueCapacity：<input type="number" v-model.number="customForm.queueCapacity" /></label>
            <label>queueType：
              <select v-model="customForm.queueType">
                <option>ArrayBlockingQueue</option>
                <option>LinkedBlockingQueue</option>
                <option>SynchronousQueue</option>
                <option>PriorityBlockingQueue</option>
                <option>DelayQueue</option>
                <option>LinkedBlockingDeque</option>
                <option>LinkedTransferQueue</option>
              </select>
            </label>
            <label>rejectionPolicy：
              <select v-model="customForm.rejectionPolicy">
                <option>AbortPolicy</option>
                <option>CallerRunsPolicy</option>
                <option>DiscardPolicy</option>
                <option>DiscardOldestPolicy</option>
                <option>CountingPolicy</option>
              </select>
            </label>
            <label>threadPrefix：<input v-model="customForm.threadFactoryPrefix" /></label>
          </div>
          <button class="btn" @click="createCustom" :disabled="loading.createCustom">{{ loading.createCustom ? '创建中...' : '创建' }}</button>
          <pre v-if="getResult('createCustom')" class="result">{{ getResult('createCustom') }}</pre>
        </div>

        <div class="card">
          <h3>向自定义池提交任务</h3>
          <div class="form-row">
            <label>poolId：<input v-model="customSubmitForm.poolId" /></label>
            <label>任务数：<input type="number" v-model.number="customSubmitForm.count" /></label>
            <label>任务耗时(ms)：<input type="number" v-model.number="customSubmitForm.taskDurationMs" /></label>
          </div>
          <button class="btn" @click="submitCustom" :disabled="loading.submitCustom">{{ loading.submitCustom ? '提交中...' : '提交' }}</button>
          <pre v-if="getResult('submitCustom')" class="result">{{ getResult('submitCustom') }}</pre>
        </div>

        <div class="card">
          <h3>关闭线程池</h3>
          <div class="form-row">
            <label>poolId：<input v-model="shutdownPoolId" /></label>
          </div>
          <button class="btn" @click="shutdown(false)" :disabled="loading.shutdown">优雅关闭</button>
          <button class="btn warn" @click="shutdown(true)" :disabled="loading.shutdown">立即关闭</button>
          <pre v-if="getResult('shutdown')" class="result">{{ getResult('shutdown') }}</pre>
        </div>

        <div class="card full">
          <h3>实时指标（每 2 秒刷新）</h3>
          <div class="metrics">
            <div v-for="m in metrics" :key="m.poolId" class="metric-card">
              <h4>{{ m.poolId }}</h4>
              <div class="metric-grid">
                <span>core/max</span><span>{{ m.corePoolSize }} / {{ m.maximumPoolSize }}</span>
                <span>poolSize</span><span>{{ m.poolSize }}</span>
                <span>active</span><span>{{ m.activeCount }}</span>
                <span>queue</span><span>{{ m.queueSize }} / {{ m.queueSize + m.queueRemainingCapacity }}</span>
                <span>completed</span><span>{{ m.completedTaskCount }}</span>
                <span>shutdown</span><span>{{ m.shutdown }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 03. 阻塞队列 -->
      <div v-if="activeModule === 'queue'" class="cards">
        <div class="card full">
          <h3>7 种阻塞队列对比</h3>
          <table class="data-table">
            <thead><tr><th>类型</th><th>底层结构</th><th>有界</th><th>默认容量</th><th>特点</th><th>风险</th></tr></thead>
            <tbody>
              <tr v-for="q in queueTypes" :key="q.type">
                <td>{{ q.type }}</td>
                <td>{{ q.underlying }}</td>
                <td>{{ q.bounded ? '是' : '否' }}</td>
                <td>{{ q.defaultCapacity }}</td>
                <td>{{ q.feature }}</td>
                <td>{{ q.risk }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="card">
          <h3>队列独立实验</h3>
          <div class="form-row">
            <label>队列类型：
              <select v-model="queueForm.type">
                <option>ArrayBlockingQueue</option>
                <option>LinkedBlockingQueue</option>
                <option>SynchronousQueue</option>
                <option>PriorityBlockingQueue</option>
                <option>DelayQueue</option>
                <option>LinkedBlockingDeque</option>
                <option>LinkedTransferQueue</option>
              </select>
            </label>
            <label>容量：<input type="number" v-model.number="queueForm.capacity" /></label>
            <label>提交数：<input type="number" v-model.number="queueForm.submitCount" /></label>
          </div>
          <button class="btn" @click="experimentQueue" :disabled="loading.queueExperiment">{{ loading.queueExperiment ? '实验中...' : '实验' }}</button>
          <pre v-if="getResult('queueExperiment')" class="result">{{ getResult('queueExperiment') }}</pre>
        </div>
      </div>

      <!-- 04. 拒绝策略 -->
      <div v-if="activeModule === 'rejection'" class="cards">
        <div class="card">
          <h3>拒绝策略实验</h3>
          <p>构造 core=1, max=1, queue=2 的线程池，提交多个任务观察不同策略行为。</p>
          <div class="form-row">
            <label>策略：
              <select v-model="rejectionForm.policy">
                <option v-for="p in rejectionPolicies" :key="p" :value="p">{{ p }}</option>
              </select>
            </label>
            <label>提交数：<input type="number" v-model.number="rejectionForm.submitCount" /></label>
          </div>
          <button class="btn" @click="experimentRejection" :disabled="loading.rejectionExperiment">{{ loading.rejectionExperiment ? '实验中...' : '实验' }}</button>
          <pre v-if="getResult('rejectionExperiment')" class="result">{{ getResult('rejectionExperiment') }}</pre>
        </div>

        <div class="card full">
          <h3>四种拒绝策略说明</h3>
          <table class="data-table">
            <thead><tr><th>策略</th><th>行为</th><th>适用场景</th><th>风险</th></tr></thead>
            <tbody>
              <tr><td>AbortPolicy</td><td>抛 RejectedExecutionException</td><td>调用方需要感知失败</td><td>调用方必须处理异常</td></tr>
              <tr><td>CallerRunsPolicy</td><td>提交线程自己执行</td><td>反压、限流</td><td>降低主线程/接口吞吐</td></tr>
              <tr><td>DiscardPolicy</td><td>静默丢弃</td><td>允许部分丢失</td><td>数据可能丢失</td></tr>
              <tr><td>DiscardOldestPolicy</td><td>丢弃最老任务再尝试</td><td>重视新数据</td><td>可能丢关键老任务</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 05. Executors 工厂 -->
      <div v-if="activeModule === 'executors'" class="cards">
        <div class="card full">
          <h3>Executors 4 种工厂风险</h3>
          <table class="data-table">
            <thead><tr><th>工厂</th><th>core</th><th>max</th><th>队列</th><th>风险</th></tr></thead>
            <tbody>
              <tr><td>newFixedThreadPool</td><td>n</td><td>n</td><td>LinkedBlockingQueue（无界）</td><td>任务堆积 OOM</td></tr>
              <tr><td>newSingleThreadExecutor</td><td>1</td><td>1</td><td>LinkedBlockingQueue（无界）</td><td>任务堆积 OOM</td></tr>
              <tr><td>newCachedThreadPool</td><td>0</td><td>Integer.MAX_VALUE</td><td>SynchronousQueue</td><td>线程数爆炸 OOM</td></tr>
              <tr><td>newScheduledThreadPool</td><td>n</td><td>Integer.MAX_VALUE</td><td>DelayedWorkQueue</td><td>线程数爆炸 OOM</td></tr>
            </tbody>
          </table>
        </div>

        <div class="card">
          <h3>工厂演示</h3>
          <p>点击创建并立即关闭对应工厂池，观察其参数配置。</p>
          <div class="btn-group">
            <button v-for="t in executorsTypes" :key="t" class="btn" @click="demoExecutors(t)" :disabled="loading[`executors-${t}`]">{{ t }}</button>
          </div>
          <pre v-for="t in executorsTypes" v-if="getResult(`executors-${t}`)" :key="t" class="result">{{ t }}:\n{{ getResult(`executors-${t}`) }}</pre>
        </div>
      </div>

      <!-- 06. 生命周期 & Worker -->
      <div v-if="activeModule === 'source'" class="cards">
        <div class="card full">
          <h3>线程池生命周期状态</h3>
          <table class="data-table">
            <thead><tr><th>状态</th><th>接收新任务</th><th>处理队列任务</th><th>中断 Worker</th><th>说明</th></tr></thead>
            <tbody>
              <tr v-for="s in lifecycleStates" :key="s.state">
                <td><strong>{{ s.state }}</strong></td>
                <td>{{ s.acceptNewTasks }}</td>
                <td>{{ s.processQueueTasks }}</td>
                <td>{{ s.interruptWorkers }}</td>
                <td>{{ s.description }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="card full">
          <h3>Worker 内部类</h3>
          <div class="worker-cards">
            <div v-for="w in workerIntros" :key="w.component" class="worker-card">
              <h4>{{ w.component }}</h4>
              <p><strong>职责：</strong>{{ w.role }}</p>
              <p><strong>关键字段：</strong>{{ w.keyFields }}</p>
              <p><strong>生命周期：</strong>{{ w.lifecycle }}</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f7fa; }
.app { display: flex; min-height: 100vh; }
.sidebar { width: 260px; background: #1e293b; color: #fff; padding: 20px; position: fixed; height: 100vh; overflow-y: auto; }
.sidebar h1 { font-size: 20px; margin-bottom: 4px; line-height: 1.4; }
.sidebar .subtitle { font-size: 12px; color: #94a3b8; margin-bottom: 20px; }
.menu-item { padding: 10px 12px; border-radius: 6px; cursor: pointer; margin-bottom: 6px; font-size: 14px; transition: background .2s; }
.menu-item:hover { background: #334155; }
.menu-item.active { background: #0ea5e9; }
.content { margin-left: 260px; flex: 1; padding: 24px; }
.module-title { margin: 0 0 8px; font-size: 24px; color: #1e293b; }
.module-desc { color: #64748b; margin-bottom: 20px; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 20px; }
.card { background: #fff; border-radius: 12px; padding: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card.full { grid-column: 1 / -1; }
.card h3 { margin: 0 0 12px; font-size: 16px; color: #0f172a; }
.card h4 { margin: 0 0 8px; font-size: 14px; color: #0f172a; }
.form-row { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; align-items: center; }
.form-row label { font-size: 13px; color: #334155; }
.form-row input, .form-row select { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 6px; margin-left: 4px; }
.form-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; margin-bottom: 12px; }
.form-grid label { font-size: 13px; color: #334155; }
.form-grid input, .form-grid select { display: block; width: 100%; padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 6px; margin-top: 4px; }
.btn { background: #0ea5e9; color: #fff; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-right: 8px; }
.btn:hover { background: #0284c7; }
.btn:disabled { background: #94a3b8; cursor: not-allowed; }
.btn.warn { background: #ef4444; }
.btn.warn:hover { background: #dc2626; }
.btn-group { display: flex; flex-wrap: wrap; gap: 8px; }
.result { margin-top: 12px; background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 8px; font-size: 12px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }

.flow { display: flex; flex-direction: column; gap: 8px; }
.flow-step { display: flex; align-items: center; gap: 12px; background: #f1f5f9; padding: 12px; border-radius: 8px; border-left: 4px solid #0ea5e9; }
.flow-step.warn { border-left-color: #ef4444; background: #fef2f2; }
.step-num { width: 28px; height: 28px; background: #0ea5e9; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; flex-shrink: 0; }
.flow-step.warn .step-num { background: #ef4444; }
.flow-step p { margin: 4px 0 0; font-size: 13px; color: #475569; }
.arrow { text-align: center; color: #64748b; font-size: 14px; }

.metrics { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.metric-card { background: #f8fafc; border-radius: 8px; padding: 12px; border: 1px solid #e2e8f0; }
.metric-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; font-size: 13px; }
.metric-grid span:nth-child(odd) { color: #64748b; }
.metric-grid span:nth-child(even) { color: #0f172a; font-weight: 500; }

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; }
.data-table th { background: #f1f5f9; color: #334155; }
.data-table tr:nth-child(even) { background: #f8fafc; }

.worker-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.worker-card { background: #f8fafc; border-radius: 8px; padding: 12px; border: 1px solid #e2e8f0; }
.worker-card p { margin: 6px 0; font-size: 13px; color: #475569; }
</style>
