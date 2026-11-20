<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.success" placeholder="结果" clearable style="width: 120px">
        <el-option label="成功" :value="true" />
        <el-option label="失败" :value="false" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createTime" label="登录时间" width="170" />
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageLoginLog } from '../../api/log'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, username: '', success: null })

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageLoginLog(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.username = ''
  query.success = null
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
