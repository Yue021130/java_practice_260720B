<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoles, listRolePermissions, saveRole, assignRolePermissions, removeRole } from '../api/role'
import { permissionTree } from '../api/permission'

const SCOPE_NAMES = { 1: '仅本人', 2: '本部门', 3: '全部数据' }
const SCOPE_TYPES = { 1: 'warning', 2: 'primary', 3: 'success' }

const list = ref([])
const loading = ref(false)

const treeData = ref([])
const treeRef = ref()
const assignVisible = ref(false)
const assignLoading = ref(false)
const currentRole = ref(null)

const editVisible = ref(false)
const submitting = ref(false)
const form = ref({ id: null, code: '', name: '', dataScope: 1 })

async function load() {
  loading.value = true
  try {
    list.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { id: null, code: '', name: '', dataScope: 1 }
  editVisible.value = true
}

function openEdit(row) {
  form.value = { id: row.id, code: row.code, name: row.name, dataScope: row.dataScope }
  editVisible.value = true
}

async function handleSave() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await saveRole({ ...form.value })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function openAssign(row) {
  currentRole.value = row
  assignVisible.value = true
  assignLoading.value = true
  try {
    if (!treeData.value.length) {
      treeData.value = await permissionTree()
    }
    const checked = await listRolePermissions(row.id)
    // 仅回填叶子节点，避免父节点半选状态被覆盖
    const leafIds = new Set()
    const collectLeaves = nodes => {
      nodes.forEach(n => {
        if (!n.children?.length) {
          leafIds.add(n.id)
        } else {
          collectLeaves(n.children)
        }
      })
    }
    collectLeaves(treeData.value)
    const leafChecked = checked.filter(id => leafIds.has(id))
    // 等树渲染完成后再设置勾选
    setTimeout(() => treeRef.value?.setCheckedKeys(leafChecked), 0)
  } finally {
    assignLoading.value = false
  }
}

async function handleAssign() {
  const checked = treeRef.value.getCheckedKeys()
  const half = treeRef.value.getHalfCheckedKeys()
  await assignRolePermissions(currentRole.value.id, [...half, ...checked])
  ElMessage.success(`已更新角色【${currentRole.value.name}】的权限，下一次请求立即生效`)
  assignVisible.value = false
}

async function handleRemove(row) {
  await ElMessageBox.confirm(`确认删除角色 ${row.name}（${row.code}）吗？`, '删除确认',
    { type: 'error', confirmButtonText: '删除' })
  await removeRole(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>角色管理</span>
        <el-button v-permission="'role:assign'" type="success" @click="openCreate">新增角色</el-button>
      </div>
    </template>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="code" label="角色编码" width="140" />
      <el-table-column prop="name" label="角色名称" width="160" />
      <el-table-column label="数据权限范围" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="SCOPE_TYPES[row.dataScope]">{{ SCOPE_NAMES[row.dataScope] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'role:assign'" size="small" type="primary"
                     @click="openAssign(row)">分配权限</el-button>
          <el-button v-permission="'role:assign'" size="small" type="danger" @click="handleRemove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="assignVisible" :title="`为【${currentRole?.name}】分配权限`" width="440px">
      <div v-loading="assignLoading" style="min-height: 200px">
        <el-tree ref="treeRef" :data="treeData" show-checkbox node-key="id"
                 :props="{ label: 'name', children: 'children' }" default-expand-all>
          <template #default="{ data }">
            <span>{{ data.name }} <el-tag size="small" type="info">{{ data.code }}</el-tag></span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑角色' : '新增角色'" width="400px">
      <el-form label-width="90px">
        <el-form-item label="角色编码">
          <el-input v-model="form.code" placeholder="如 MANAGER" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="数据权限范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="(name, scope) in SCOPE_NAMES" :key="scope" :label="name" :value="Number(scope)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
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
