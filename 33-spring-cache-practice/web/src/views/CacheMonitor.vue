<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="统计来自自定义二级缓存 CacheMetrics；查找顺序 L1(Caffeine) → L2(Redis) → 数据库，L2 命中会回填 L1。" />

    <div class="toolbar">
      <el-select v-model="cacheName" placeholder="选择缓存" style="width: 160px">
        <el-option label="product" value="product" />
        <el-option label="productPage" value="productPage" />
      </el-select>
      <el-button type="primary" :loading="keysLoading" @click="loadKeys">查询 Keys</el-button>
      <el-button type="danger" @click="onClear">清空缓存</el-button>
      <el-button @click="loadStats">刷新统计</el-button>
    </div>

    <div v-loading="statsLoading" class="stats">
      <div v-for="item in stats" :key="item.name" class="stats-group">
        <h4>{{ item.name }}</h4>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-value">{{ item.l1Hit }}</div>
            <div class="stat-label">L1 命中</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ item.l2Hit }}</div>
            <div class="stat-label">L2 命中</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ item.miss }}</div>
            <div class="stat-label">回源 MISS</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ (item.hitRate * 100).toFixed(1) }}%</div>
            <div class="stat-label">命中率</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ item.evict }}</div>
            <div class="stat-label">驱逐次数</div>
          </div>
        </div>
      </div>
      <el-empty v-if="!statsLoading && stats.length === 0" description="暂无统计数据" />
    </div>

    <el-table :data="keyRows" v-loading="keysLoading" border>
      <el-table-column prop="key" label="Key" show-overflow-tooltip />
      <el-table-column label="层级" width="100">
        <template #default="{ row }">
          <el-tag :type="row.level === 'L1' ? 'success' : 'warning'">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCacheStats, getCacheKeys, clearCache } from '../api/cacheMonitor'

const stats = ref([])
const statsLoading = ref(false)
const keyRows = ref([])
const keysLoading = ref(false)
const cacheName = ref('product')

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await getCacheStats()
  } finally {
    statsLoading.value = false
  }
}

async function loadKeys() {
  keysLoading.value = true
  try {
    const data = await getCacheKeys(cacheName.value)
    keyRows.value = [
      ...(data.l1Keys || []).map((k) => ({ key: k, level: 'L1' })),
      ...(data.l2Keys || []).map((k) => ({ key: k, level: 'L2' }))
    ]
  } finally {
    keysLoading.value = false
  }
}

async function onClear() {
  await ElMessageBox.confirm(`确定清空缓存 [${cacheName.value}] 吗？`, '警告', { type: 'warning' })
  await clearCache(cacheName.value)
  ElMessage.success('缓存已清空')
  loadStats()
  keyRows.value = []
}

onMounted(loadStats)
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.stats { margin-bottom: 20px; }
.stats-group { margin-bottom: 15px; }
.stats-group h4 { margin: 0 0 8px; }
.stats-cards { display: flex; gap: 12px; flex-wrap: wrap; }
.stat-card {
  min-width: 120px; padding: 12px 16px; border: 1px solid #ebeef5; border-radius: 6px;
  background: #f8f9fb; text-align: center;
}
.stat-value { font-size: 20px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
