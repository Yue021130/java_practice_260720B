<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="订单数据受「数据范围」约束：列表只返回当前角色可见的订单；详情接口带越权校验，访问他人订单返回 403。可用「越权对比」观察不设防接口的差异。" />

    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 200px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column label="金额" width="110" align="right">
        <template #default="{ row }">￥{{ formatAmount(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="deptName" label="所属部门" width="140">
        <template #default="{ row }">{{ row.deptName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="username" label="下单人" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onDetail(row)">详情</el-button>
          <el-button size="small" type="warning" @click="onInsecureCompare(row)">越权对比</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />

    <el-dialog v-model="detailVisible" title="订单详情" width="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">￥{{ formatAmount(detail.amount) }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单人">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="compareVisible" title="越权对比演示" width="620px">
      <el-alert type="warning" :closable="false" class="tip"
                title="同一个订单，分别调用「带数据权限校验」与「不设防」两个接口：前者校验数据归属，非本人/本部门订单返回 403；后者只校验登录态，任何登录用户都能查看——这正是越权漏洞的样子。" />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="防护接口 GET /order/{id}">
          <el-tag :type="compare.secureOk ? 'success' : 'danger'">
            {{ compare.secureOk ? '放行（数据在权限范围内）' : '拦截 403（越权访问他人订单）' }}
          </el-tag>
          <pre v-if="compare.secureData" class="result">{{ JSON.stringify(compare.secureData, null, 2) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="不设防接口 GET /order/insecure/{id}">
          <el-tag type="warning">放行（只校验登录，不看数据归属）</el-tag>
          <pre v-if="compare.insecureData" class="result">{{ JSON.stringify(compare.insecureData, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageOrder, getOrder, getOrderInsecure } from '../../api/order'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '' })

const detailVisible = ref(false)
const detail = ref({})

const compareVisible = ref(false)
const compare = reactive({ secureOk: false, secureData: null, insecureData: null })

const STATUS_MAP = {
  0: { text: '待支付', tag: 'warning' },
  1: { text: '已支付', tag: 'primary' },
  2: { text: '已发货', tag: 'success' },
  3: { text: '已完成', tag: 'info' },
  CREATED: { text: '待支付', tag: 'warning' },
  PAID: { text: '已支付', tag: 'primary' },
  SHIPPED: { text: '已发货', tag: 'success' },
  DONE: { text: '已完成', tag: 'info' }
}

function statusText(status) {
  return STATUS_MAP[status]?.text || (status ?? '-')
}

function statusTagType(status) {
  return STATUS_MAP[status]?.tag || 'info'
}

function formatAmount(amount) {
  return amount === null || amount === undefined ? '-' : Number(amount).toFixed(2)
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageOrder(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.orderNo = ''
  load(1)
}

async function onDetail(row) {
  detail.value = await getOrder(row.id)
  detailVisible.value = true
}

async function onInsecureCompare(row) {
  compare.secureOk = false
  compare.secureData = null
  compare.insecureData = null
  compareVisible.value = true
  try {
    compare.secureData = await getOrder(row.id)
    compare.secureOk = true
  } catch {
    // 403：request.js 已弹出错误提示，这里把结果标记为拦截
    compare.secureOk = false
  }
  compare.insecureData = await getOrderInsecure(row.id)
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
.result { background: #f5f7fa; padding: 8px; border-radius: 4px; margin: 6px 0 0; font-size: 12px; }
</style>
