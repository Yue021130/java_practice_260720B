<script setup>
import { reactive, ref } from 'vue'
import { runScenario } from '../api'
import LogTimeline from './LogTimeline.vue'

const props = defineProps({
  scenario: { type: Object, required: true }
})

// 参数表单初值
const form = reactive({})
for (const p of props.scenario.params || []) {
  form[p.name] = p.default
}

const showBagu = ref(false)
const running = ref(false)
const result = ref(null)
const error = ref('')

function buildParams() {
  const params = {}
  for (const p of props.scenario.params || []) {
    let v = form[p.name]
    if (p.type === 'number') {
      v = Number(v)
      if (Number.isNaN(v)) v = p.default
      if (p.min != null && v < p.min) v = p.min
      if (p.max != null && v > p.max) v = p.max
    }
    params[p.name] = v
  }
  return params
}

async function run() {
  if (running.value) return
  running.value = true
  error.value = ''
  result.value = null
  try {
    const res = await runScenario(props.scenario.endpoint, buildParams())
    if (res && res.code === 200) {
      result.value = res.data || {}
    } else {
      error.value = `后端返回异常：code=${res?.code}，${res?.message || '未知错误'}`
    }
  } catch (e) {
    error.value = '请求失败：' + (e.response?.data?.message || e.message || '网络错误，请确认后端已启动（:8082）')
  } finally {
    running.value = false
  }
}
</script>

<template>
  <div class="scenario-card">
    <div class="card-head">
      <h3 class="card-title">{{ scenario.title }}</h3>
      <code class="endpoint">{{ scenario.endpoint }}</code>
    </div>
    <p class="scene">{{ scenario.scene }}</p>

    <div class="bagu">
      <button class="bagu-toggle" type="button" @click="showBagu = !showBagu">
        <span class="arrow" :class="{ open: showBagu }">▸</span>
        面试八股（{{ scenario.bagu.length }} 条）
      </button>
      <ul v-if="showBagu" class="bagu-list">
        <li v-for="(b, i) in scenario.bagu" :key="i">{{ b }}</li>
      </ul>
    </div>

    <div v-if="(scenario.params || []).length" class="params">
      <label v-for="p in scenario.params" :key="p.name" class="param-field">
        <span class="param-label">{{ p.label }}</span>
        <input
          v-if="p.type === 'number'"
          type="number"
          v-model="form[p.name]"
          :min="p.min"
          :max="p.max"
          :disabled="running"
        />
        <select v-else v-model="form[p.name]" :disabled="running">
          <option v-for="opt in p.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </label>
    </div>

    <button class="run-btn" :class="{ loading: running }" :disabled="running" @click="run">
      <span v-if="running" class="spinner"></span>
      {{ running ? '运行中…' : '运行场景' }}
    </button>

    <div v-if="error" class="error-box">{{ error }}</div>
    <LogTimeline v-if="result" :result="result" />
  </div>
</template>
