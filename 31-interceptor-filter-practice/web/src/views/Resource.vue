<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="本页面提供资源 CRUD；调用 /api/resource/test-rate-limit 可观察 Interceptor IP 限流效果。" />

    <div class="toolbar">
      <el-input v-model="query.name" placeholder="按名称搜索" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
        <el-option label="API" :value="1" />
        <el-option label="页面" :value="2" />
        <el-option label="菜单" :value="3" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" @click="onAdd">新增资源</el-button>
      <el-button type="warning" @click="onTestRateLimit">限流测试</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="url" label="路径/标识" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag>{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资源' : '新增资源'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="路径" prop="url">
          <el-input v-model="form.url" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">API</el-radio>
            <el-radio :value="2">页面</el-radio>
            <el-radio :value="3">菜单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageResources, saveResource, updateResource, deleteResource, testRateLimit } from '../api/resource'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, name: '', type: null })

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = {
  id: null, name: '', url: '', type: 1, status: 1, sortOrder: 0, remark: ''
}
const form = reactive({ ...emptyForm })

const rules = {
  name: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入资源路径', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function typeText(type) {
  const map = { 1: 'API', 2: '页面', 3: '菜单' }
  return map[type] || type
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageResources(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.name = ''
  query.type = null
  load(1)
}

function onAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function onEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateResource(form)
      ElMessage.success('修改成功')
    } else {
      await saveResource(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load(query.current)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除资源 [${row.name}] 吗？`, '警告', { type: 'warning' })
  await deleteResource(row.id)
  ElMessage.success('删除成功')
  load(1)
}

async function onTestRateLimit() {
  try {
    const msg = await testRateLimit()
    ElMessage.success(msg)
  } catch (e) {
    // 限流时 request.js 会弹出错误提示
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
