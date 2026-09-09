<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUsers, saveUser, removeUser } from '../api/user'
import { listRoles } from '../api/role'

const DEPT_NAMES = { 1: '研发部', 2: '运营部', 3: '财务部' }

const query = reactive({ current: 1, size: 10, username: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])

const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({ id: null, username: '', password: '', nickname: '', deptId: 1, roleIds: [] })

async function load() {
  loading.value = true
  try {
    const data = await pageUsers(query)
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
  Object.assign(form, { id: null, username: '', password: '', nickname: '', deptId: 1, roleIds: [] })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    deptId: row.deptId,
    roleIds: [...(row.roleIds || [])]
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.username || !form.nickname) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (!form.id && !form.password) {
    ElMessage.warning('新增用户必须设置密码')
    return
  }
  submitting.value = true
  try {
    await saveUser({ ...form })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleRemove(row) {
  await ElMessageBox.confirm(`确认删除用户 ${row.nickname}（${row.username}）吗？`, '删除确认',
    { type: 'error', confirmButtonText: '删除' })
  await removeUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  load()
  roles.value = await listRoles()
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button v-permission="'user:add'" type="success" @click="openCreate">新增用户</el-button>
      </div>
    </template>

    <el-form inline @submit.prevent>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="模糊查询" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="登录名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column label="部门" width="100" align="center">
        <template #default="{ row }">{{ DEPT_NAMES[row.deptId] || row.deptId }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">
          <el-tag v-for="name in row.roleNames" :key="name" size="small" style="margin-right: 4px">
            {{ name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'user:edit'" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'user:remove'" size="small" type="danger" @click="handleRemove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="query.current" v-model:page-size="query.size"
                   :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
                   style="margin-top: 16px; justify-content: flex-end" @change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="登录名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'">
          <el-input v-model="form.password" type="password" show-password
                    :placeholder="form.id ? '留空表示不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" style="width: 100%">
            <el-option v-for="(name, id) in DEPT_NAMES" :key="id" :label="name" :value="Number(id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
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
