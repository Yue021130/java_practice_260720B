<script setup>
import { computed } from 'vue'

const props = defineProps({
  result: { type: Object, required: true }
})

const note = computed(() => props.result?.note || '')
const entries = computed(() => {
  return Object.entries(props.result || {})
    .filter(([key]) => key !== 'note')
    .map(([key, value]) => ({ key, value }))
})

function display(value) {
  if (value === null) return 'null'
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  return String(value)
}
</script>

<template>
  <div class="result-view">
    <div v-if="note" class="result-note">
      <span class="note-label">结论</span>
      <span class="note-text">{{ note }}</span>
    </div>
    <div v-if="entries.length" class="metrics-row">
      <div v-for="e in entries" :key="e.key" class="metric-card">
        <div class="metric-key">{{ e.key }}</div>
        <pre class="metric-value">{{ display(e.value) }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result-view {
  margin-top: 10px;
  background: #0b0f19;
  border: 1px solid #1f2937;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.result-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: #0f172a;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #1e293b;
}
.note-label {
  background: #22d3ee;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}
.note-text {
  color: #f1f5f9;
  font-size: 14px;
  line-height: 1.5;
}
.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.metric-card {
  background: #111827;
  border: 1px solid #1f2937;
  border-radius: 8px;
  padding: 10px 14px;
}
.metric-key {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.metric-value {
  margin: 0;
  font-size: 13px;
  color: #22d3ee;
  font-weight: 600;
  word-break: break-all;
  white-space: pre-wrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
