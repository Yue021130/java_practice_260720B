<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="部门名称" clearable style="width: 180px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" v-permission="'dept:create'" @click="onCreate(null)">新增部门</el-button>
    </div>

    <el-table :data="filteredTree" v-loading="loading" border row-key="id"
              :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="name" label="部门名称" min-width="220" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" v-permission="'dept:create'"
                     @click="onCreate(row)">新增子部门</el-button>
          <el-button size="small" v-permission="'dept:update'" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" v-permission="'dept:delete'"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="deptTree" check-strictly
                          :render-after-expand="false" node-key="id" :props="{ label: 'name' }"
                          placeholder="不选则为顶级部门" style="width: 100%" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="部门名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptTree, createDept, updateDept, deleteDept } from '../../api/dept'
import { buildTree } from '../../utils/tree'

const loading = ref(false)
const saving = ref(false)
const flatList = ref([])
const deptTree = ref([])
const keyword = ref('')

const filteredTree = computed(() => {
  if (!keyword.value) return deptTree.value
  const kw = keyword.value.trim()
  const match = (list) => list
    .map((n) => {
      const children = n.children ? match(n.children) : []
      if (n.name.includes(kw) || children.length) {
        return { ...n, children: children.length ? children : n.children }
      }
      return null
    })
    .filter(Boolean)
  return match(deptTree.value)
})

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = { id: null, parentId: null, name: '', sortOrder: 0 }
const form = reactive({ ...emptyForm })

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const list = await getDeptTree()
    flatList.value = list
    deptTree.value = buildTree(list)
  } finally {
    loading.value = false
  }
}

function onCreate(parent) {
  Object.assign(form, emptyForm, { parentId: parent ? parent.id : null })
  dialogVisible.value = true
}

function onEdit(row) {
  Object.assign(form, emptyForm, {
    id: row.id, parentId: row.parentId, name: row.name, sortOrder: row.sortOrder || 0
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDept(form)
      ElMessage.success('部门已更新')
    } else {
      await createDept(form)
      ElMessage.success('部门已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除部门 [${row.name}] 吗？`, '删除提示', { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
</style>
