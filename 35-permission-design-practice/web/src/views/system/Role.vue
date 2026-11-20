<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" v-permission="'role:create'" @click="onCreate">新增角色</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleCode" label="角色编码" width="140" />
      <el-table-column prop="roleName" label="角色名称" width="160" />
      <el-table-column label="数据范围" min-width="140">
        <template #default="{ row }">
          <el-tag :type="scopeTagType(row.scopeType)">{{ scopeText(row.scopeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-permission="'role:update'" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" v-permission="'role:delete'"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 ROLE_ADMIN / MANAGER" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色显示名称" />
        </el-form-item>
        <el-form-item label="功能权限" prop="permissionIds">
          <el-tree ref="permTreeRef" :data="permTree" show-checkbox node-key="id"
                   :props="{ label: 'name', children: 'children' }" class="perm-tree"
                   :default-checked-keys="form.permissionIds" />
        </el-form-item>
        <el-form-item label="数据范围" prop="scopeType">
          <el-radio-group v-model="form.scopeType">
            <el-radio value="ALL">全部数据</el-radio>
            <el-radio value="CUSTOM">自定义</el-radio>
            <el-radio value="DEPT_AND_CHILD">本部门及以下</el-radio>
            <el-radio value="DEPT">本部门</el-radio>
            <el-radio value="SELF">仅本人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scopeType === 'CUSTOM'" label="自定义部门" prop="customDeptIds">
          <el-tree ref="deptTreeRef" :data="deptTree" show-checkbox check-strictly node-key="id"
                   :props="{ label: 'name', children: 'children' }" class="perm-tree"
                   :default-checked-keys="form.customDeptIds" />
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
import { pageRole, getRole, createRole, updateRole, deleteRole } from '../../api/role'
import { getPermissionTree } from '../../api/permission'
import { getDeptTree } from '../../api/dept'
import { buildTree } from '../../utils/tree'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, roleName: '' })

const dialogVisible = ref(false)
const formRef = ref()
const permTreeRef = ref()
const deptTreeRef = ref()
const permTree = ref([])
const deptTree = ref([])
const emptyForm = {
  id: null, roleCode: '', roleName: '',
  permissionIds: [], scopeType: 'SELF', customDeptIds: []
}
const form = reactive({ ...emptyForm })

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const SCOPE_MAP = {
  ALL: { text: '全部数据', tag: 'danger' },
  CUSTOM: { text: '自定义数据', tag: 'warning' },
  DEPT_AND_CHILD: { text: '本部门及以下', tag: 'primary' },
  DEPT: { text: '本部门', tag: 'success' },
  SELF: { text: '仅本人', tag: 'info' }
}

function scopeText(type) {
  return SCOPE_MAP[type]?.text || type || '-'
}

function scopeTagType(type) {
  return SCOPE_MAP[type]?.tag || 'info'
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageRole(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.roleName = ''
  load(1)
}

async function loadTrees() {
  const [perms, depts] = await Promise.all([getPermissionTree(), getDeptTree()])
  permTree.value = buildTree(perms)
  deptTree.value = buildTree(depts)
}

async function onCreate() {
  Object.assign(form, emptyForm)
  await loadTrees()
  dialogVisible.value = true
}

async function onEdit(row) {
  await loadTrees()
  const detail = await getRole(row.id)
  Object.assign(form, emptyForm, {
    id: detail.id,
    roleCode: detail.roleCode,
    roleName: detail.roleName,
    scopeType: detail.scopeType || 'SELF',
    permissionIds: detail.permissionIds || [],
    customDeptIds: detail.customDeptIds || []
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  // el-tree 半选父节点不提交，只保留叶子勾选（父级由后端按树结构推导）
  form.permissionIds = permTreeRef.value.getCheckedKeys()
  form.customDeptIds = form.scopeType === 'CUSTOM' ? deptTreeRef.value?.getCheckedKeys() || [] : []
  saving.value = true
  try {
    if (form.id) {
      await updateRole(form)
      ElMessage.success('角色已更新')
    } else {
      await createRole(form)
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    load(form.id ? query.current : 1)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除角色 [${row.roleName}] 吗？`, '删除提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load(query.current)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
.perm-tree { width: 100%; max-height: 240px; overflow: auto; border: 1px solid #e6e6e6; border-radius: 4px; }
</style>
