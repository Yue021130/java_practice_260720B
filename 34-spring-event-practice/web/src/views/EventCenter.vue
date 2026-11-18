<template>
  <el-card>
    <el-alert type="info" :closable="false" class="tip"
              title="每个事件发布前先落 event_record 表再广播（本地消息表思想），监听器消费后回写 event_handle——下表可以看到一个事件被哪些监听器、以同步还是异步方式处理。" />

    <el-tabs v-model="activeTab">
      <!-- 事件流水 -->
      <el-tab-pane label="事件流水" name="events">
        <div class="toolbar">
          <el-select v-model="eventQuery.eventType" placeholder="事件类型" clearable style="width: 220px">
            <el-option label="LeaveSubmittedEvent" value="LeaveSubmittedEvent" />
            <el-option label="LeaveApprovedEvent" value="LeaveApprovedEvent" />
          </el-select>
          <el-select v-model="eventQuery.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="待消费" :value="0" />
            <el-option label="已消费" :value="1" />
          </el-select>
          <el-button type="primary" @click="loadEvents(1)">查询</el-button>
          <el-button @click="onResetEvents">重置</el-button>
        </div>

        <el-table :data="eventData" v-loading="eventLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="eventType" label="事件类型" width="200" />
          <el-table-column prop="payload" label="Payload" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                {{ row.status === 1 ? '已消费' : '待消费' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理器" width="220">
            <template #default="{ row }">
              <template v-if="row.handledBy && row.handledBy.length">
                <el-tag v-for="h in row.handledBy" :key="h" size="small" class="handle-tag">{{ h }}</el-tag>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="onEventDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination class="pager" background layout="total, prev, pager, next" :total="eventTotal"
                       :current-page="eventQuery.current" :page-size="eventQuery.size"
                       @current-change="(p) => loadEvents(p)" />
      </el-tab-pane>

      <!-- 站内通知 -->
      <el-tab-pane label="站内通知" name="notifies">
        <div class="toolbar">
          <el-input v-model="notifyQuery.receiver" placeholder="接收人" clearable style="width: 160px"
                    @keyup.enter="loadNotifies(1)" />
          <el-button type="primary" @click="loadNotifies(1)">查询</el-button>
          <el-button @click="onResetNotifies">重置</el-button>
        </div>

        <el-table :data="notifyData" v-loading="notifyLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="receiver" label="接收人" width="120" />
          <el-table-column prop="title" label="标题" width="180" show-overflow-tooltip />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.readFlag ? 'info' : 'danger'">
                {{ row.readFlag ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>

        <el-pagination class="pager" background layout="total, prev, pager, next" :total="notifyTotal"
                       :current-page="notifyQuery.current" :page-size="notifyQuery.size"
                       @current-change="(p) => loadNotifies(p)" />
      </el-tab-pane>

      <!-- 审计日志 -->
      <el-tab-pane label="审计日志" name="audits">
        <div class="toolbar">
          <el-button type="primary" @click="loadAudits(1)">查询</el-button>
        </div>

        <el-table :data="auditData" v-loading="auditLoading" border>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="action" label="操作" width="180" />
          <el-table-column prop="detail" label="详情" show-overflow-tooltip />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>

        <el-pagination class="pager" background layout="total, prev, pager, next" :total="auditTotal"
                       :current-page="auditQuery.current" :page-size="auditQuery.size"
                       @current-change="(p) => loadAudits(p)" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="事件详情" width="640px">
      <template v-if="currentEvent">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="事件 ID">{{ currentEvent.id }}</el-descriptions-item>
          <el-descriptions-item label="事件类型">{{ currentEvent.eventType }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentEvent.status === 1 ? 'success' : 'warning'">
              {{ currentEvent.status === 1 ? '已消费' : '待消费' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentEvent.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-block">
          <h4>完整 Payload</h4>
          <pre class="payload-pre">{{ currentEvent.payload }}</pre>
        </div>
        <div class="detail-block">
          <h4>处理器（handledBy）</h4>
          <template v-if="currentEvent.handledBy && currentEvent.handledBy.length">
            <el-tag v-for="h in currentEvent.handledBy" :key="h" class="handle-tag">{{ h }}</el-tag>
          </template>
          <span v-else>暂无</span>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageEvents, getEvent, pageNotifies, pageAudits } from '../api/eventCenter'

const activeTab = ref('events')

const eventLoading = ref(false)
const eventData = ref([])
const eventTotal = ref(0)
const eventQuery = reactive({ current: 1, size: 10, eventType: '', status: null })

const notifyLoading = ref(false)
const notifyData = ref([])
const notifyTotal = ref(0)
const notifyQuery = reactive({ current: 1, size: 10, receiver: '' })

const auditLoading = ref(false)
const auditData = ref([])
const auditTotal = ref(0)
const auditQuery = reactive({ current: 1, size: 10 })

const detailVisible = ref(false)
const currentEvent = ref(null)

async function loadEvents(page) {
  eventLoading.value = true
  try {
    eventQuery.current = page
    const data = await pageEvents(eventQuery)
    eventData.value = data.records
    eventTotal.value = data.total
  } finally {
    eventLoading.value = false
  }
}

function onResetEvents() {
  eventQuery.eventType = ''
  eventQuery.status = null
  loadEvents(1)
}

async function loadNotifies(page) {
  notifyLoading.value = true
  try {
    notifyQuery.current = page
    const data = await pageNotifies(notifyQuery)
    notifyData.value = data.records
    notifyTotal.value = data.total
  } finally {
    notifyLoading.value = false
  }
}

function onResetNotifies() {
  notifyQuery.receiver = ''
  loadNotifies(1)
}

async function loadAudits(page) {
  auditLoading.value = true
  try {
    auditQuery.current = page
    const data = await pageAudits(auditQuery)
    auditData.value = data.records
    auditTotal.value = data.total
  } finally {
    auditLoading.value = false
  }
}

async function onEventDetail(row) {
  currentEvent.value = await getEvent(row.id)
  detailVisible.value = true
}

onMounted(() => {
  loadEvents(1)
  loadNotifies(1)
  loadAudits(1)
})
</script>

<style scoped>
.tip { margin-bottom: 15px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.pager { margin-top: 15px; justify-content: flex-end; }
.handle-tag { margin-right: 4px; }
.detail-block { margin-top: 15px; }
.detail-block h4 { margin: 0 0 8px; }
.payload-pre {
  margin: 0; padding: 10px; background: #f5f7fa; border-radius: 4px;
  white-space: pre-wrap; word-break: break-all; font-size: 12px;
}
</style>
