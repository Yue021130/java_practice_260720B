<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>Caffeine 缓存实时监控</span>
        <el-button @click="loadStats" :loading="loading">刷新统计</el-button>
      </div>
    </template>

    <el-row :gutter="20">
      <el-col :span="12" v-for="(item, name) in stats" :key="name">
        <el-card shadow="never" class="stat-card">
          <template #header>{{ name === 'user' ? 'user 缓存（用户详情）' : 'userPage 缓存（分页列表）' }}</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="缓存条目数">{{ item.estimatedSize ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="命中率">{{ item.hitRate ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="命中次数">{{ item.hitCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="未命中次数">{{ item.missCount ?? '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />

    <h3>缓存对比实验</h3>
    <p class="tip">输入用户 ID 连续查询两次：第一次未命中走数据库，第二次命中直接返回。
      观察命中率变化；也可先到「用户管理」页编辑该用户，验证缓存被清空后重新加载。</p>
    <div class="toolbar">
      <el-input-number v-model="compareId" :min="1" placeholder="用户 ID" />
      <el-button type="primary" :loading="comparing" @click="onCompare">运行实验</el-button>
      <el-button type="warning" @click="onClear">清空缓存</el-button>
    </div>
    <el-alert v-if="compareResult" class="result" type="success" :closable="false"
              :title="`第一次耗时 ${compareResult.firstCostMs}ms（走数据库），第二次耗时 ${compareResult.secondCostMs}ms（走缓存）`"
              :description="compareResult.tip" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cacheStats, cacheCompare, cacheClear } from '../api/cache'

const loading = ref(false)
const comparing = ref(false)
const stats = ref({})
const compareId = ref(1)
const compareResult = ref(null)

async function loadStats() {
  loading.value = true
  try {
    stats.value = await cacheStats()
  } finally {
    loading.value = false
  }
}

async function onCompare() {
  comparing.value = true
  try {
    compareResult.value = await cacheCompare(compareId.value)
    await loadStats()
  } finally {
    comparing.value = false
  }
}

async function onClear() {
  await cacheClear()
  ElMessage.success('缓存已清空')
  compareResult.value = null
  await loadStats()
}

onMounted(loadStats)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stat-card { margin-bottom: 20px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin: 10px 0; }
.tip { color: #909399; font-size: 13px; }
.result { margin-top: 10px; }
</style>
