<template>
  <div>
    <div class="page-card">
      <el-alert type="info" :closable="false" show-icon
        title="每个场景都会调一次后端接口，拿到返回的 traceId 后，去 logs/ 目录检索："
        description="grep <traceId> logs/**/app-*.log —— 重点观察异步 / 定时 / 事件场景：日志里的线程名变了（http-nio 变成 batch-x），但 traceId 始终一致，这就是 MDC 全链路追踪的意义。" />
    </div>

    <div class="page-card">
      <div class="toolbar">
        <el-button v-for="s in scenarios" :key="s.key" :type="s.type" :loading="results[s.key]?.loading"
                   @click="runScenario(s.key)">
          {{ s.label }}
        </el-button>
      </div>

      <el-table :data="scenarioRows" border stripe>
        <el-table-column prop="label" label="场景" width="160" />
        <el-table-column label="traceId" min-width="380">
          <template #default="{ row }">
            <el-tag v-if="row.traceId" class="trace-tag" @click="copyTrace(row.traceId)">
              {{ row.traceId }}
            </el-tag>
            <span v-else class="muted">点击上方按钮发起调用</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="280">
          <template #default="{ row }">
            <span>{{ results[row.key]?.text || row.tip }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <h3>Headers 透传</h3>
      <p class="muted">在请求头里携带自定义 X-Trace-Id，后端会优先使用请求头中的值；响应头与响应体的 traceId 都应与输入一致。</p>
      <div class="toolbar">
        <el-input v-model="customTraceId" placeholder="如 my-trace-001" style="width: 260px" />
        <el-button type="warning" :loading="headerLoading" @click="runHeaders">发送透传请求</el-button>
      </div>
      <div v-if="headerResult" class="result-box">
        <p>请求头 X-Trace-Id：<el-tag class="trace-tag">{{ customTraceId }}</el-tag></p>
        <p>响应头 X-Trace-Id：<el-tag class="trace-tag" type="success"
             @click="copyTrace(headerResult.headerTraceId)">{{ headerResult.headerTraceId }}</el-tag></p>
        <p>响应体 traceId：<el-tag class="trace-tag" type="success"
             @click="copyTrace(headerResult.bodyTraceId)">{{ headerResult.bodyTraceId }}</el-tag></p>
        <el-alert v-if="headerResult.match" type="success" :closable="false" show-icon title="透传成功：三者一致" />
        <el-alert v-else type="error" :closable="false" show-icon title="透传失败：traceId 不一致，请检查后端过滤器优先级" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request, { getWithTraceId } from '../api/request'

const scenarios = [
  { key: 'sync', label: '同步链路', tip: 'controller → service → mapper 同线程，traceId 自然一致' },
  { key: 'async', label: '@Async 异步', tip: '线程名变为 batch-x，但 traceId 依然一致（ThreadPoolTaskExecutor 包装 MDC）' },
  { key: 'future', label: 'CompletableFuture', tip: '线程池 + MDC 手动拷贝，traceId 一致' },
  { key: 'event', label: '模拟MQ事件', tip: 'Spring 事件监听为异步线程，消费日志与生产日志 traceId 一致' },
  { key: 'exception', label: '异常链路', type: 'danger', tip: '返回纯文本，错误日志进了 logs/error-current.log，traceId 与 start 日志一致' }
]

const results = reactive({})
const customTraceId = ref('my-trace-001')
const headerLoading = ref(false)
const headerResult = ref(null)

const scenarioRows = computed(() =>
  scenarios.map(s => ({ ...s, traceId: results[s.key]?.traceId }))
)

async function runScenario(key) {
  results[key] = { loading: true }
  try {
    if (key === 'exception') {
      // 该接口返回纯文本（HTTP 200），需按 text 解析
      const res = await request.get('/demo/exception', { responseType: 'text' })
      results[key] = { traceId: '', text: String(res.data) }
    } else {
      const res = await request.get(`/demo/${key}`)
      results[key] = { traceId: res.data.traceId }
    }
  } catch (e) {
    ElMessage.error(`调用失败：${e.message}`)
    results[key] = {}
  }
}

async function runHeaders() {
  if (!customTraceId.value.trim()) {
    ElMessage.warning('请先输入自定义 traceId')
    return
  }
  headerLoading.value = true
  try {
    const res = await getWithTraceId('/demo/headers', customTraceId.value.trim())
    const headerTraceId = res.headers['x-trace-id'] || '(无)'
    const bodyTraceId = res.data.traceId
    headerResult.value = {
      headerTraceId,
      bodyTraceId,
      match: headerTraceId === customTraceId.value.trim() && bodyTraceId === customTraceId.value.trim()
    }
  } catch (e) {
    ElMessage.error(`调用失败：${e.message}`)
  } finally {
    headerLoading.value = false
  }
}

async function copyTrace(traceId) {
  if (!traceId) return
  try {
    await navigator.clipboard.writeText(traceId)
    ElMessage.success('traceId 已复制')
  } catch {
    ElMessage.info(traceId)
  }
}
</script>

<style scoped>
h3 { margin-bottom: 8px; }
.muted { color: #909399; font-size: 13px; }
.result-box p { margin: 6px 0; }
</style>
