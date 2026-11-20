<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.title" placeholder="操作名称" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.operator" placeholder="操作人" clearable style="width: 140px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="title" label="操作名称" min-width="140" />
      <el-table-column prop="perm" label="权限点" min-width="130">
        <template #default="{ row }">{{ row.perm || '-' }}</template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="110" />
      <el-table-column prop="method" label="请求方式" width="90" />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="params" label="请求参数" min-width="200" show-overflow-tooltip />
      <el-table-column prop="costMs" label="耗时(ms)" width="90" align="right" />
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageOpLog } from '../../api/log'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, title: '', operator: '' })

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageOpLog(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.title = ''
  query.operator = ''
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
