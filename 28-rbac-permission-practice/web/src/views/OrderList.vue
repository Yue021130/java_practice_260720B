<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageOrders, createOrder, refundOrder, removeOrder } from '../api/order'
import { useUserStore } from '../stores/user'

const store = useUserStore()

const query = reactive({ current: 1, size: 10, orderNo: '', status: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({ amount: null })

async function load() {
  loading.value = true
  try {
    const data = await pageOrders(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.current = 1
  load()
}

function openCreate() {
  form.amount = null
  dialogVisible.value = true
}

async function handleCreate() {
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入正确的金额')
    return
  }
  submitting.value = true
  try {
    await createOrder(form)
    ElMessage.success('下单成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleRefund(row) {
  await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 发起退款吗？`, '高危操作', { type: 'warning' })
  await refundOrder(row.id)
  ElMessage.success('退款成功')
  load()
}

async function handleRemove(row) {
  await ElMessageBox.confirm(`确认删除订单 ${row.orderNo} 吗？删除后不可恢复！`, '删除确认',
    { type: 'error', confirmButtonText: '删除' })
  await removeOrder(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>订单列表</span>
        <el-tag type="info">当前数据范围：{{ store.dataScopeText }}</el-tag>
      </div>
    </template>

    <el-form inline @submit.prevent>
      <el-form-item label="订单号">
        <el-input v-model="query.orderNo" placeholder="模糊查询" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="已支付" value="PAID" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button v-permission="'order:add'" type="success" @click="openCreate">下单</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="nickname" label="下单人" width="120" />
      <el-table-column prop="deptId" label="部门ID" width="90" align="center" />
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="{ row }">￥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : 'info'">
            {{ row.status === 'PAID' ? '已支付' : '已退款' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'order:refund'" size="small" type="warning"
                     :disabled="row.status === 'REFUNDED'" @click="handleRefund(row)">
            退款
          </el-button>
          <el-button v-permission="'order:delete'" size="small" type="danger" @click="handleRemove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="query.current" v-model:page-size="query.size"
                   :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
                   style="margin-top: 16px; justify-content: flex-end" @change="load" />

    <el-dialog v-model="dialogVisible" title="下单" width="360px">
      <el-form label-width="70px">
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
