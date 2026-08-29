<template>
  <div class="app">
    <header>
      <h1>EasyExcel 流式导出实战</h1>
      <p>100万行导出内存从 2G 降到 50M 的完整复现</p>
    </header>

    <main>
      <section class="card">
        <h2>1. 生成测试数据</h2>
        <label>数据量 <input v-model.number="count" type="number"/></label>
        <button @click="generate">生成订单数据</button>
        <p class="tip">数据写入 H2 内存数据库，大数量导出建议先生成 10万 ~ 100万 条。</p>
      </section>

      <section class="card">
        <h2>2. 错误示范：全量加载导出</h2>
        <button class="danger" @click="exportInMemory">导出（小数据量可用）</button>
        <p class="tip">一次性把所有数据读到 List，再调用 doWrite；数据量大时直接 OOM。</p>
      </section>

      <section class="card">
        <h2>3. 正确示范：流式导出</h2>
        <button @click="exportStream">流式导出</button>
        <p class="tip">分页查询 + ExcelWriter 多次 write + finish；内存稳定在几十 MB。</p>
      </section>

      <section class="card">
        <h2>4. 异步导出</h2>
        <label>目标行数 <input v-model.number="asyncRows" type="number"/></label>
        <button @click="submitAsync">提交异步任务</button>
        <button @click="pollStatus" :disabled="!taskId">查询进度</button>
        <button @click="downloadAsync" :disabled="!canDownload">下载文件</button>
        <p v-if="taskId" class="info">任务 ID: {{ taskId }} | 状态: {{ taskStatus.status }} | 进度: {{ taskStatus.processedRows }} / {{ taskStatus.totalRows }}</p>
      </section>

      <section class="card">
        <h2>5. 八股速记</h2>
        <button @click="loadExplain">加载核心考点</button>
      </section>

      <div class="result">
        <h3>结果</h3>
        <pre v-if="result">{{ JSON.stringify(result, null, 2) }}</pre>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import api from './api/excel.js'

const count = ref(10000)
const asyncRows = ref(10000)
const taskId = ref('')
const taskStatus = reactive({ status: '', processedRows: 0, totalRows: 0, fileUrl: '' })
const result = ref(null)
const error = ref('')

const canDownload = computed(() => taskStatus.status === 'SUCCESS')

function reset() {
  result.value = null
  error.value = ''
}

async function call(name, fn) {
  reset()
  try {
    const res = await fn()
    if (res && res.code !== undefined && res.code !== 200) {
      throw new Error(res.message || '业务失败')
    }
    result.value = res && res.data !== undefined ? res.data : res
  } catch (e) {
    error.value = e.message
  }
}

function generate() {
  call('generate', () => api.generate(count.value))
}

function exportInMemory() {
  reset()
  api.exportInMemory().catch(e => { error.value = e.message })
}

function exportStream() {
  reset()
  api.exportStream().catch(e => { error.value = e.message })
}

async function submitAsync() {
  reset()
  try {
    const res = await api.submitAsync(asyncRows.value)
    taskId.value = res.taskId
    Object.assign(taskStatus, res)
    result.value = res
  } catch (e) {
    error.value = e.message
  }
}

async function pollStatus() {
  if (!taskId.value) return
  try {
    const res = await api.getAsyncStatus(taskId.value)
    Object.assign(taskStatus, res)
    result.value = res
  } catch (e) {
    error.value = e.message
  }
}

function downloadAsync() {
  if (!taskId.value) return
  api.downloadAsync(taskId.value).catch(e => { error.value = e.message })
}

function loadExplain() {
  call('explain', () => api.explain())
}
</script>

<style>
* { box-sizing: border-box; }
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; background: #f5f7fa; color: #333; }
.app { max-width: 800px; margin: 0 auto; padding: 20px; }
header { text-align: center; margin-bottom: 20px; }
header h1 { margin: 0; font-size: 28px; }
header p { color: #666; margin-top: 6px; }
.card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.card h2 { font-size: 18px; margin-bottom: 12px; border-left: 4px solid #409eff; padding-left: 10px; }
.card label { display: inline-block; margin-right: 12px; }
.card input { padding: 6px 10px; border: 1px solid #d9d9d9; border-radius: 4px; margin-left: 6px; width: 140px; }
.card button { padding: 6px 16px; border: none; background: #409eff; color: #fff; border-radius: 4px; cursor: pointer; margin-right: 8px; }
.card button.danger { background: #f56c6c; }
.card button:disabled { background: #c0c4cc; cursor: not-allowed; }
.tip { color: #999; font-size: 13px; margin-top: 10px; }
.info { color: #333; font-size: 14px; margin-top: 10px; }
.result { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.result pre { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; overflow-x: auto; }
.error { color: #f56c6c; }
</style>
