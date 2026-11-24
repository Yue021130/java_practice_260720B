<template>
  <div>
    <div class="page-card">
      <el-alert type="info" :closable="false" show-icon
        title="整个批次共享提交时的 traceId：去日志里 grep 这一个 id，就能看到批次内所有子任务的日志。"
        description="进度条由前端每 1 秒轮询 GET /api/batch/{taskId}/progress 驱动；finished=true 时停止轮询。试着开两个页面同时提交，观察两个批次 traceId 互不干扰。" />
    </div>

    <div class="page-card">
      <div class="toolbar">
        <span class="label">任务数</span>
        <el-input-number v-model="form.taskCount" :min="1" :max="500" />
        <span class="label">单任务耗时 ms</span>
        <el-input-number v-model="form.perTaskMs" :min="50" :max="5000" :step="50" />
        <span class="label">失败率 ‰</span>
        <el-input-number v-model="form.failRatePermille" :min="0" :max="200" />
        <el-button type="primary" :loading="submitting" @click="submitBatch">提交批次</el-button>
      </div>
    </div>

    <div v-if="batches.length === 0" class="page-card muted">
      还没有批次，调整参数后点击「提交批次」。
    </div>

    <el-card v-for="b in batches" :key="b.taskId" class="page-card batch-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span>批次 {{ b.taskId }}</span>
          <el-tag :type="b.finished ? 'info' : 'success'">{{ b.finished ? '已完成' : '进行中' }}</el-tag>
        </div>
      </template>
      <p class="row">
        批次 traceId（grep 这一个 id 即可看到全部子任务日志）：
        <el-tag class="trace-tag" @click="copyTrace(b.traceId)">{{ b.traceId }}</el-tag>
      </p>
      <el-progress :percentage="b.percent" :status="b.finished ? 'success' : ''" />
      <p class="row muted">
        已完成 {{ b.done }} / {{ b.total }}
        <template v-if="b.failed > 0">，失败 <span class="fail">{{ b.failed }}</span></template>
        ，开始时间 {{ b.startTime || '-' }}
      </p>
      <el-alert v-if="b.error" type="error" :closable="false" show-icon :title="b.error" />
    </el-card>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const form = reactive({ taskCount: 30, perTaskMs: 200, failRatePermille: 0 })
const submitting = ref(false)
const batches = ref([])

async function submitBatch() {
  submitting.value = true
  try {
    const res = await request.post('/batch/submit', {
      taskCount: form.taskCount,
      perTaskMs: form.perTaskMs,
      failRatePermille: form.failRatePermille
    })
    const batch = reactive({
      taskId: res.data.taskId,
      traceId: res.data.traceId,
      total: form.taskCount,
      done: 0,
      failed: 0,
      percent: 0,
      finished: false,
      startTime: '',
      error: '',
      timer: null
    })
    batches.value.unshift(batch)
    // 最多同时展示最近 3 个批次，超出的停止轮询
    while (batches.value.length > 3) {
      const removed = batches.value.pop()
      if (removed.timer) clearInterval(removed.timer)
    }
    startPolling(batch)
    ElMessage.success(`批次已提交：${batch.taskId}`)
  } catch (e) {
    ElMessage.error(`提交失败：${e.message}`)
  } finally {
    submitting.value = false
  }
}

function startPolling(batch) {
  batch.timer = setInterval(async () => {
    try {
      const res = await request.get(`/batch/${batch.taskId}/progress`)
      const data = res.data
      if (data == null) {
        // taskId 不存在，停止轮询并提示
        clearInterval(batch.timer)
        batch.timer = null
        batch.error = `查询批次进度失败：taskId ${batch.taskId} 不存在`
        ElMessage.error(batch.error)
        return
      }
      batch.traceId = data.traceId
      batch.total = data.total
      batch.done = data.done
      batch.failed = data.failed
      batch.percent = data.percent
      batch.finished = data.finished
      batch.startTime = data.startTime
      if (data.finished) {
        clearInterval(batch.timer)
        batch.timer = null
        ElMessage.success(`批次 ${batch.taskId} 已完成：成功 ${data.done}，失败 ${data.failed}`)
      }
    } catch (e) {
      clearInterval(batch.timer)
      batch.timer = null
      batch.error = `轮询中断：${e.message}`
      ElMessage.error(batch.error)
    }
  }, 1000)
}

async function copyTrace(traceId) {
  try {
    await navigator.clipboard.writeText(traceId)
    ElMessage.success('traceId 已复制')
  } catch {
    ElMessage.info(traceId)
  }
}

onBeforeUnmount(() => {
  batches.value.forEach(b => {
    if (b.timer) clearInterval(b.timer)
  })
})
</script>

<style scoped>
.label { color: #606266; font-size: 14px; }
.muted { color: #909399; font-size: 13px; }
.fail { color: #f56c6c; font-weight: bold; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.row { margin: 10px 0; font-size: 14px; }
.batch-card { margin-bottom: 16px; }
</style>
