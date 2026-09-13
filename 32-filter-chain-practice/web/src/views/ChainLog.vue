<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="本页数据由 TimingFilter 记录到 sys_request_log；过滤器位于鉴权之前，因此能记录鉴权失败的 401 请求（对比 31 章 Interceptor 只能记录登录后的请求）。" />

    <div class="toolbar">
      <el-input v-model="query.traceId" placeholder="TraceId" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.uri" placeholder="URI" clearable style="width: 200px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.ip" placeholder="IP" clearable style="width: 150px"
                @keyup.enter="load(1)" />
      <el-input v-model.number="query.statusCode" placeholder="状态码" clearable style="width: 110px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="traceId" label="TraceId" width="170" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="method" label="方法" width="80" />
      <el-table-column prop="uri" label="URI" show-overflow-tooltip />
      <el-table-column label="状态码" width="85">
        <template #default="{ row }">
          <el-tag :type="row.statusCode === 200 ? 'success' : 'danger'">{{ row.statusCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时(ms)" width="90" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageRequestLog } from '../api/requestLog'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, traceId: '', uri: '', ip: '', statusCode: null })

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageRequestLog(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.traceId = ''
  query.uri = ''
  query.ip = ''
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
