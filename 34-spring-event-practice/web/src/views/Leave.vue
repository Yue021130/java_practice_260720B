<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="提交请假单会发布 LeaveSubmittedEvent（同步审计监听器写入审计日志）；审批通过/拒绝在事务提交后发布 LeaveApprovedEvent（AFTER_COMMIT 异步通知监听器写入站内信）——请到「事件中心」观察完整事件链路。" />

    <div class="toolbar">
      <el-input v-model="query.username" placeholder="申请人" clearable style="width: 160px"
                @keyup.enter="load(1)" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="待审批" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button type="success" @click="onSubmit">提交请假</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="申请人" width="110" />
      <el-table-column label="请假类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTagType(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="请假原因" show-overflow-tooltip />
      <el-table-column prop="startDate" label="开始日期" width="110" />
      <el-table-column prop="endDate" label="结束日期" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="approver" label="审批人" width="110">
        <template #default="{ row }">{{ row.approver || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="onApprove(row)">通过</el-button>
            <el-button size="small" type="danger" @click="onReject(row)">拒绝</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="query.current" :page-size="query.size"
                   @current-change="(p) => load(p)" />

    <el-dialog v-model="dialogVisible" title="提交请假" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="申请人" prop="username">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="请假类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="SICK">病假</el-radio>
            <el-radio value="CASUAL">事假</el-radio>
            <el-radio value="ANNUAL">年假</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择开始日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageLeave, submitLeave, approveLeave, rejectLeave } from '../api/leave'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, username: '', status: null })

const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = { username: '', type: '', startDate: '', endDate: '', reason: '' }
const form = reactive({ ...emptyForm })

const rules = {
  username: [{ required: true, message: '请输入申请人', trigger: 'blur' }],
  type: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

const TYPE_MAP = {
  SICK: { text: '病假', tag: 'info' },
  CASUAL: { text: '事假', tag: 'warning' },
  ANNUAL: { text: '年假', tag: 'success' }
}
const STATUS_MAP = {
  0: { text: '待审批', tag: 'warning' },
  1: { text: '已通过', tag: 'success' },
  2: { text: '已拒绝', tag: 'danger' }
}

function typeText(type) {
  return TYPE_MAP[type]?.text || type
}

function typeTagType(type) {
  return TYPE_MAP[type]?.tag || 'info'
}

function statusText(status) {
  return STATUS_MAP[status]?.text || status
}

function statusTagType(status) {
  return STATUS_MAP[status]?.tag || 'info'
}

async function load(page) {
  loading.value = true
  try {
    query.current = page
    const data = await pageLeave(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.username = ''
  query.status = null
  load(1)
}

function onSubmit() {
  Object.assign(form, emptyForm, { username: userStore.userInfo.username })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await submitLeave(form)
    ElMessage.success('请假单已提交，LeaveSubmittedEvent 已发布')
    dialogVisible.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

async function onApprove(row) {
  await ElMessageBox.confirm(`确定通过 [${row.username}] 的请假申请吗？`, '审批提示', { type: 'warning' })
  await approveLeave({ id: row.id, approver: userStore.userInfo.username })
  ElMessage.success('审批通过，LeaveApprovedEvent 已发布')
  load(query.current)
}

async function onReject(row) {
  await ElMessageBox.confirm(`确定拒绝 [${row.username}] 的请假申请吗？`, '审批提示', { type: 'warning' })
  await rejectLeave({ id: row.id, approver: userStore.userInfo.username })
  ElMessage.success('已拒绝，LeaveApprovedEvent 已发布')
  load(query.current)
}

onMounted(() => load(1))
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
</style>
