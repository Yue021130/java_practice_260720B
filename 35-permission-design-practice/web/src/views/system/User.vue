<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.nickname" placeholder="昵称" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" v-permission="'user:create'" @click="onCreate">新增用户</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column prop="deptName" label="部门" width="150">
        <template #default="{ row }">{{ row.deptName || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">{{ formatRoles(row.roleNames) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" v-permission="'user:update'" @click="onEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" v-permission="'user:delete'"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select v-model="form.deptId" :data="deptTree" check-strictly :render-after-expand="false"
                          node-key="id" :props="{ label: 'name' }" placeholder="选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
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
import { pageUser, createUser, updateUser, deleteUser } from '../../api/user'
import { getDeptTree } from '../../api/dept'
import { pageRole } from '../../api/role'
import { buildTree } from '../../utils/tree'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, username: '', nickname: '' })

const dialogVisible = ref(false)
const formRef = ref()
const deptTree = ref([])
const roleOptions = ref([])
const emptyForm = { id: null, username: '', nickname: '', password: '', deptId: null, roleIds: [] }
const form = reactive({ ...emptyForm })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }]
}

function formatRoles(roleNames) {
  if (!roleNames || !roleNames.length) return '-'
  return Array.isArray(roleNames) ? roleNames.join('、') : roleNames
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageUser(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.username = ''
  query.nickname = ''
  load(1)
}

async function loadDeptTree() {
  const list = await getDeptTree()
  deptTree.value = buildTree(list)
}

async function loadRoleOptions() {
  const data = await pageRole({ current: 1, size: 100 })
  roleOptions.value = data.records
}

async function onCreate() {
  Object.assign(form, emptyForm)
  await Promise.all([loadDeptTree(), loadRoleOptions()])
  dialogVisible.value = true
}

async function onEdit(row) {
  Object.assign(form, emptyForm, {
    id: row.id,
    username: row.username,
    nickname: row.nickname,
    deptId: row.deptId,
    roleIds: row.roleIds || []
  })
  await Promise.all([loadDeptTree(), loadRoleOptions()])
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form)
      ElMessage.success('用户已更新')
    } else {
      await createUser(form)
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    load(form.id ? query.current : 1)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除用户 [${row.username}] 吗？`, '删除提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load(query.current)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
