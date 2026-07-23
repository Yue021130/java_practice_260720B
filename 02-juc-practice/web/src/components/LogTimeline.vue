<script setup>
import { computed } from 'vue'

const props = defineProps({
  result: { type: Object, required: true }
})

// 高对比度配色池，同一线程名稳定取同一颜色
const PALETTE = [
  '#22d3ee', '#60a5fa', '#a78bfa', '#f472b6',
  '#fb923c', '#a3e635', '#34d399', '#facc15',
  '#f87171', '#38bdf8', '#c084fc', '#4ade80'
]

function hashCode(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h * 31 + str.charCodeAt(i)) | 0
  }
  return Math.abs(h)
}

function threadColor(name) {
  return PALETTE[hashCode(name || 'main') % PALETTE.length]
}

const steps = computed(() => props.result?.steps || [])
const metrics = computed(() => {
  const d = props.result?.data
  if (!d || typeof d !== 'object') return []
  return Object.entries(d).map(([key, value]) => ({ key, value }))
})
</script>

<template>
  <div class="log-timeline">
    <div class="summary-banner">
      <span class="summary-label">结论</span>
      <span class="summary-text">{{ result.summary || '（无结论）' }}</span>
      <span v-if="result.elapsedMs != null" class="elapsed">耗时 {{ result.elapsedMs }} ms</span>
    </div>

    <div v-if="metrics.length" class="metrics-row">
      <div v-for="m in metrics" :key="m.key" class="metric-card">
        <div class="metric-key">{{ m.key }}</div>
        <div class="metric-value">{{ m.value }}</div>
      </div>
    </div>

    <div v-if="steps.length" class="timeline">
      <div v-for="(s, i) in steps" :key="i" class="timeline-item">
        <span class="t">+{{ s.t }}ms</span>
        <span
          class="thread-badge"
          :style="{ backgroundColor: threadColor(s.thread) + '22', color: threadColor(s.thread), borderColor: threadColor(s.thread) + '66' }"
        >{{ s.thread }}</span>
        <span class="msg">{{ s.message }}</span>
      </div>
    </div>
    <div v-else class="no-steps">该场景无时间线日志</div>
  </div>
</template>
