<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="商品详情与分页结果走 Caffeine + Redis 二级缓存：重复查询观察控制台 SQL 消失、「缓存监控」页命中率变化；热点查询演示缓存击穿防护。" />

    <div class="toolbar">
      <el-input v-model="query.name" placeholder="商品名称" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.category" placeholder="分类" clearable style="width: 140px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" @click="onAdd">新增商品</el-button>
      <el-button type="warning" @click="hotDialogVisible = true">热点查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商品名称" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="hotDialogVisible" title="热点商品查询" width="480px">
      <el-alert type="warning" :closable="false" class="tip"
                title="演示缓存击穿防护：热点 key 逻辑过期后由单线程重建缓存，其余请求返回旧值。" />
      <el-input v-model="hotId" placeholder="输入商品 ID" style="width: 200px" />
      <el-button type="primary" :loading="hotLoading" @click="onHotQuery">查询</el-button>
      <div v-if="hotResult" class="hot-result">
        <p>ID：{{ hotResult.id }}</p>
        <p>名称：{{ hotResult.name }}</p>
        <p>分类：{{ hotResult.category }}</p>
        <p>价格：{{ hotResult.price }}</p>
        <p>库存：{{ hotResult.stock }}</p>
      </div>
      <template #footer>
        <el-button @click="hotDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageProduct, saveProduct, updateProduct, deleteProduct, getHotProduct } from '../api/product'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, name: '', category: '', status: null })

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = {
  id: null, name: '', category: '', price: 0.01, stock: 0, status: 1, sortOrder: 0
}
const form = reactive({ ...emptyForm })

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }]
}

const hotDialogVisible = ref(false)
const hotId = ref('')
const hotLoading = ref(false)
const hotResult = ref(null)

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageProduct(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.name = ''
  query.category = ''
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
      await updateProduct(form)
      ElMessage.success('修改成功')
    } else {
      await saveProduct(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load(query.current)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除商品 [${row.name}] 吗？`, '警告', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  load(1)
}

async function onHotQuery() {
  if (!hotId.value) {
    ElMessage.warning('请输入商品 ID')
    return
  }
  hotLoading.value = true
  hotResult.value = null
  try {
    hotResult.value = await getHotProduct(hotId.value)
    ElMessage.success('查询成功')
  } finally {
    hotLoading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
.hot-result { margin-top: 15px; }
.hot-result p { margin: 4px 0; }
</style>
