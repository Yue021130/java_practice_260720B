<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="本页数据由 LogInterceptor 自动写入 sys_api_log 表，展示 Filter + Interceptor 的链路审计效果。" />

    <div class="toolbar">
      <el-input v-model="query.ip" placeholder="IP" clearable style="width: 150px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.uri" placeholder="URI" clearable style="width: 200px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.method" placeholder="方法" clearable style="width: 100px">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <el-input v-model.number="query.statusCode" placeholder="状态码" clearable style="width: 110px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="uri" label="URI" show-overflow-tooltip />
      <el-table-column prop="username" label="用户" width="110" />
      <el-table-column prop="statusCode" label="状态码" width="85" />
      <el-table-column prop="costMs" label="耗时(ms)" width="90" />
      <el-table-column label="异常" width="80">
        <template #default="{ row }">
          <el-tag :type="row.hasError === 1 ? 'danger' : 'success'">
            {{ row.hasError === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageApiLogs } from '../api/apiLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, ip: '', uri: '', method: '', statusCode: null })

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageApiLogs(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.ip = ''
  query.uri = ''
  query.method = ''
  query.statusCode = null
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
