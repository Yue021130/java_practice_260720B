<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import { useUserStore } from '../stores/user'

const store = useUserStore()

// 查询 / 分页
const query = reactive({ orderNo: '' })
const list = ref([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })
const loading = ref(false)

// 写按钮权限：仅 ADMIN 拥有 order:write。为演示后端 403，按钮保持可点击。
const isAdmin = computed(() => store.userInfo.role === 'ADMIN')
const writeTip = '无 order:write 权限（可点击验证后端 403）'

async function fetchList() {
  loading.value = true
  try {
    const data = await request.get('/order/page', {
      params: {
        current: page.current,
        size: page.size,
        orderNo: query.orderNo || undefined
      }
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  fetchList()
}

function handleReset() {
  query.orderNo = ''
  handleSearch()
}

function handlePageChange(current) {
  page.current = current
  fetchList()
}

function handleSizeChange(size) {
  page.size = size
  page.current = 1
  fetchList()
}

// 新增 / 编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增订单')
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, orderNo: '', amount: 0.01, status: '正常' })

const rules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
}

function openCreate() {
  dialogTitle.value = '新增订单'
  Object.assign(form, { id: null, orderNo: '', amount: 0.01, status: '正常' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogTitle.value = '编辑订单'
  Object.assign(form, {
    id: row.id,
    orderNo: row.orderNo,
    amount: row.amount,
    status: row.status
  })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await request.put('/order', form)
    } else {
      await request.post('/order', form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除订单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/order/${row.id}`)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="page-card">
    <div class="filter-bar">
      <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 240px"
                @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-toolbar">
      <el-tooltip v-if="!isAdmin" :content="writeTip" placement="left">
        <span>
          <el-button type="primary" @click="openCreate">新增订单</el-button>
        </span>
      </el-tooltip>
      <el-button v-else type="primary" @click="openCreate">新增订单</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="orderNo" label="订单号" min-width="160" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '取消' ? 'info' : 'success'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="createTime" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="!isAdmin">
            <el-tooltip :content="writeTip" placement="top">
              <span style="margin-right: 8px">
                <el-button size="small" @click="openEdit(row)">编辑</el-button>
              </span>
            </el-tooltip>
            <el-tooltip :content="writeTip" placement="top">
              <span>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </span>
            </el-tooltip>
          </template>
          <template v-else>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                     v-model:current-page="page.current" v-model:page-size="page.size"
                     :page-sizes="[10, 20, 50]"
                     @current-change="handlePageChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1"
                           style="width: 100%" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="正常">正常</el-radio>
            <el-radio value="取消">取消</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
