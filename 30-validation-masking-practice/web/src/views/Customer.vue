<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="列表中的姓名/手机号/身份证/邮箱/住址均为后端 @Sensitive 注解自动脱敏后的数据，数据库中保存的是明文。" />

    <div class="toolbar">
      <el-input v-model="query.name" placeholder="按姓名搜索" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-input v-model="query.phone" placeholder="按手机号搜索" clearable style="width: 180px"
                @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" @click="onAdd">新增客户</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="idCard" label="身份证号" width="190" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="address" label="住址" show-overflow-tooltip />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">
          <el-tag :type="row.gender === 1 ? 'primary' : 'danger'">
            {{ row.gender === 1 ? '男' : '女' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="70" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑客户' : '新增客户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" :placeholder="form.id ? '留空表示不修改' : ''" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" :placeholder="form.id ? '留空表示不修改' : '11 位手机号'" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" :placeholder="form.id ? '留空表示不修改' : '18 位身份证号'" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="客户等级">
          <el-rate v-model="form.level" :max="5" />
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
import { pageCustomers, saveCustomer, updateCustomer, deleteCustomer } from '../api/customer'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, name: '', phone: '' })

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = {
  id: null, name: '', phone: '', idCard: '', email: '',
  address: '', gender: 1, level: 1, remark: ''
}
const form = reactive({ ...emptyForm })

// 与后端校验规则呼应；编辑时敏感字段留空表示不修改，故不做必填
const PHONE_RULE = { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
const ID_CARD_RULE = { pattern: /^\d{17}[\dXx]$/, message: '身份证号须为 18 位（末位可为 X）', trigger: 'blur' }
const rules = ref({})

function buildRules(isEdit) {
  rules.value = {
    name: isEdit ? [] : [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    phone: isEdit ? [PHONE_RULE] : [{ required: true, message: '请输入手机号', trigger: 'blur' }, PHONE_RULE],
    idCard: isEdit ? [ID_CARD_RULE] : [{ required: true, message: '请输入身份证号', trigger: 'blur' }, ID_CARD_RULE],
    email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
  }
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageCustomers(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.name = ''
  query.phone = ''
  load(1)
}

function onAdd() {
  Object.assign(form, emptyForm)
  buildRules(false)
  dialogVisible.value = true
}

function onEdit(row) {
  Object.assign(form, { ...row, name: '', phone: '', idCard: '' })
  buildRules(true)
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    // 编辑时把未填写的敏感字段剔除（留空 = 不修改，对应后端 Update 分组语义）
    const payload = { ...form }
    if (payload.id) {
      for (const key of ['name', 'phone', 'idCard']) {
        if (!payload[key]) delete payload[key]
      }
    }
    if (payload.id) {
      await updateCustomer(payload)
      ElMessage.success('修改成功')
    } else {
      await saveCustomer(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load(query.current)
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除客户 [${row.name}] 吗？`, '警告', { type: 'warning' })
  await deleteCustomer(row.id)
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
