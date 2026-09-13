<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="公告 CRUD；提交含 <script> 等内容可观察 XssFilter 对参数与请求体的转义清洗效果。" />

    <div class="toolbar">
      <el-input v-model="query.title" placeholder="按标题搜索" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
        <el-option label="通知" :value="1" />
        <el-option label="公告" :value="2" />
        <el-option label="新闻" :value="3" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" @click="onAdd">新增公告</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">通知</el-radio>
            <el-radio :value="2">公告</el-radio>
            <el-radio :value="3">新闻</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5"
                    placeholder="试试提交 <script>alert(1)</script> 观察 XssFilter 转义" />
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
import { pageNotice, saveNotice, updateNotice, deleteNotice } from '../api/notice'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, title: '', type: null, status: null })

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = {
  id: null, title: '', type: 1, status: 1, sortOrder: 0, content: ''
}
const form = reactive({ ...emptyForm })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const typeMap = { 1: '通知', 2: '公告', 3: '新闻' }
const typeTagMap = { 1: 'primary', 2: 'warning', 3: 'success' }

function typeText(type) {
  return typeMap[type] || type
}

function typeTag(type) {
  return typeTagMap[type] || 'info'
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageNotice(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.title = ''
  query.type = null
  query.status = null
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
      await updateNotice(form)
      ElMessage.success('修改成功')
    } else {
      await saveNotice(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load(query.current)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除公告 [${row.title}] 吗？`, '警告', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  load(1)
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
