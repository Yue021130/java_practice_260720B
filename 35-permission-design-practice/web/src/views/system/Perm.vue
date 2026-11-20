<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="权限名称" clearable style="width: 180px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" v-permission="'perm:create'" @click="onCreate(null)">新增权限</el-button>
      <el-button type="warning" :loading="refreshing" @click="onRefreshCache">刷新权限缓存</el-button>
    </div>

    <el-table :data="filteredTree" v-loading="loading" border row-key="id"
              :tree-props="{ children: 'children' }" default-expand-all>
      <el-table-column prop="name" label="权限名称" min-width="180" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'MENU' ? 'primary' : 'warning'">
            {{ row.type === 'MENU' ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="perm" label="权限标识" min-width="150">
        <template #default="{ row }">{{ row.perm || '-' }}</template>
      </el-table-column>
      <el-table-column prop="path" label="路由地址" min-width="130">
        <template #default="{ row }">{{ row.path || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-permission="'perm:update'" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" v-permission="'perm:delete'"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑权限' : '新增权限'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级权限">
          <el-tree-select v-model="form.parentId" :data="menuOnlyTree" check-strictly
                          :render-after-expand="false" node-key="id" :props="{ label: 'name' }"
                          placeholder="不选则为顶级" style="width: 100%" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="MENU">菜单</el-radio>
            <el-radio value="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="菜单/按钮名称" />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="路由地址" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item v-if="form.type === 'BUTTON'" label="权限标识" prop="perm">
          <el-input v-model="form.perm" placeholder="如 user:list" />
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
import {
  getPermissionTree, createPermission, updatePermission, deletePermission, refreshPermissionCache
} from '../../api/permission'
import { buildTree } from '../../utils/tree'

const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const permTree = ref([])
const keyword = ref('')

/** 上级权限只能选择 MENU 类型 */
const menuOnlyTree = computed(() => {
  const filter = (list) => list
    .filter((n) => n.type === 'MENU')
    .map((n) => ({ ...n, children: n.children ? filter(n.children) : undefined }))
  return filter(permTree.value)
})

const filteredTree = computed(() => {
  if (!keyword.value) return permTree.value
  const kw = keyword.value.trim()
  const match = (list) => list
    .map((n) => {
      const children = n.children ? match(n.children) : []
      if ((n.name && n.name.includes(kw)) || (n.perm && n.perm.includes(kw)) || children.length) {
        return { ...n, children: children.length ? children : n.children }
      }
      return null
    })
    .filter(Boolean)
  return match(permTree.value)
})

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = { id: null, parentId: null, type: 'MENU', name: '', perm: '', path: '', sortOrder: 0 }
const form = reactive({ ...emptyForm })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const list = await getPermissionTree()
    permTree.value = buildTree(list)
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
    id: row.id, parentId: row.parentId, type: row.type, name: row.name,
    perm: row.perm || '', path: row.path || '', sortOrder: row.sortOrder || 0
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updatePermission(form)
      ElMessage.success('权限已更新')
    } else {
      await createPermission(form)
      ElMessage.success('权限已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除权限 [${row.name}] 吗？`, '删除提示', { type: 'warning' })
  await deletePermission(row.id)
  ElMessage.success('已删除')
  load()
}

async function onRefreshCache() {
  refreshing.value = true
  try {
    await refreshPermissionCache()
    ElMessage.success('权限缓存已刷新')
  } finally {
    refreshing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
</style>
