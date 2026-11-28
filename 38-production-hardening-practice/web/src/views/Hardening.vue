<script setup>
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import request from '../api/request'

/* ---------- b. Actuator 端点探测 ---------- */
const actuatorResult = ref('')
const actuatorLoading = ref('')

async function probeActuator(path) {
  actuatorLoading.value = path
  actuatorResult.value = ''
  try {
    const { data } = await axios.get(`/api${path}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
    const text = typeof data === 'string' ? data : JSON.stringify(data, null, 2)
    actuatorResult.value = text.slice(0, 500) + (text.length > 500 ? '\n...（已截断，仅展示前 500 字符）' : '')
  } catch (e) {
    actuatorResult.value = `请求失败：HTTP ${e.response?.status ?? '-'} ${JSON.stringify(e.response?.data ?? '')}`
  } finally {
    actuatorLoading.value = ''
  }
}

/* ---------- c. 限流演练 ---------- */
const rateRunning = ref(false)
const rateStats = reactive({ total: 0, ok: 0, limited: 0, other: 0 })

async function fireMe() {
  if (rateRunning.value) return
  rateRunning.value = true
  Object.assign(rateStats, { total: 15, ok: 0, limited: 0, other: 0 })
  const token = localStorage.getItem('token')
  // 快速循环并发 15 次，绕过拦截器自行统计状态码
  await Promise.all(
    Array.from({ length: 15 }, () =>
      axios
        .get('/api/auth/me', { headers: { Authorization: `Bearer ${token}` } })
        .then(() => {
          rateStats.ok++
        })
        .catch(e => {
          if (e.response?.status === 429) {
            rateStats.limited++
          } else {
            rateStats.other++
          }
        })
    )
  )
  rateRunning.value = false
}

/* ---------- d. 审计日志 ---------- */
const auditList = ref([])
const auditTotal = ref(0)
const auditPage = reactive({ current: 1, size: 10 })
const auditLoading = ref(false)

async function fetchAudit() {
  auditLoading.value = true
  try {
    const data = await request.get('/audit/page', {
      params: { current: auditPage.current, size: auditPage.size }
    })
    auditList.value = data.records || []
    auditTotal.value = data.total || 0
  } finally {
    auditLoading.value = false
  }
}

function handleAuditPageChange(current) {
  auditPage.current = current
  fetchAudit()
}

onMounted(fetchAudit)
</script>

<template>
  <div class="hardening-grid">
    <!-- a. 密钥外置 -->
    <el-card shadow="never" class="grid-item">
      <template #header><b>密钥外置</b></template>
      <p class="desc-text">
        密码、JWT 密钥等敏感配置不写入代码仓库，统一通过环境变量注入。
        application.yml 中使用 <code>${DB_PASSWORD:}</code>、<code>${JWT_SECRET:}</code>
        占位，缺省时启动直接失败，避免明文密钥入库。
      </p>
      <div class="pre-block"># Linux / macOS 启动示例
DB_PASSWORD=xxx JWT_SECRET=yyy mvn spring-boot:run

# Windows PowerShell 启动示例
$env:DB_PASSWORD="xxx"; $env:JWT_SECRET="yyy"; mvn spring-boot:run</div>
    </el-card>

    <!-- b. Actuator 端点探测 -->
    <el-card shadow="never" class="grid-item">
      <template #header><b>Actuator 端点探测</b></template>
      <div class="btn-group">
        <el-button size="small" :loading="actuatorLoading === '/actuator/health'"
                   @click="probeActuator('/actuator/health')">GET /actuator/health</el-button>
        <el-button size="small" :loading="actuatorLoading === '/actuator/metrics'"
                   @click="probeActuator('/actuator/metrics')">GET /actuator/metrics</el-button>
        <el-button size="small" :loading="actuatorLoading === '/actuator/prometheus'"
                   @click="probeActuator('/actuator/prometheus')">GET /actuator/prometheus</el-button>
      </div>
      <pre v-if="actuatorResult" class="pre-block">{{ actuatorResult }}</pre>
      <p class="desc-text">
        注意：/actuator/prometheus 需要 management 配置中已开启（management.endpoints.web.exposure.include）。
        生产环境只暴露 health 等必要端点，metrics/prometheus 视监控需求开启并加访问控制。
      </p>
      <p class="desc-text">运行时调整业务包日志级别（无需重启）：</p>
      <div class="pre-block"># 切换 DEBUG
curl -X POST http://localhost:8080/actuator/loggers/com.example.prod \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'

# 恢复 INFO
curl -X POST http://localhost:8080/actuator/loggers/com.example.prod \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"INFO"}'</div>
    </el-card>

    <!-- c. 限流演练 -->
    <el-card shadow="never" class="grid-item">
      <template #header><b>限流演练</b></template>
      <p class="desc-text">
        后端基于 Bucket4j 实现 IP 维度限流（每 IP 每分钟 10 个令牌桶）。
        点击下方按钮将对 <code>/api/auth/me</code> 连发 15 次请求（携带当前 token），
        预期前若干次返回 200，超出配额后触发 429。
      </p>
      <el-button type="warning" :loading="rateRunning" @click="fireMe">
        连发 15 次 /api/auth/me
      </el-button>
      <div v-if="rateStats.total" class="rate-stats">
        <el-tag type="info">总请求 {{ rateStats.total }}</el-tag>
        <el-tag type="success">200 成功 {{ rateStats.ok }}</el-tag>
        <el-tag type="danger">429 限流 {{ rateStats.limited }}</el-tag>
        <el-tag v-if="rateStats.other" type="warning">其他 {{ rateStats.other }}</el-tag>
      </div>
      <p v-if="rateStats.total && !rateStats.limited" class="desc-text">
        本次未被限流：令牌桶按分钟窗口补充，可稍等或加大并发再试。
      </p>
      <p v-if="rateStats.limited" class="desc-text">
        已触发限流（429）——前端全局拦截器会弹出「触发限流（429），请稍后再试」。
      </p>
    </el-card>

    <!-- d. 审计日志 -->
    <el-card shadow="never" class="grid-item">
      <template #header><b>审计日志</b></template>
      <p class="desc-text">
        登录、登出、订单写操作均会落入 audit_event 表，并可通过 /api/audit/page 分页查询。
      </p>
      <el-table :data="auditList" v-loading="auditLoading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="principal" label="操作人" width="120" />
        <el-table-column prop="type" label="类型" width="140" />
        <el-table-column prop="data" label="数据" min-width="200" show-overflow-tooltip />
        <el-table-column prop="timestamp" label="时间" min-width="170" />
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="auditTotal"
                       v-model:current-page="auditPage.current"
                       :page-size="auditPage.size"
                       @current-change="handleAuditPageChange" />
      </div>
    </el-card>

    <!-- e. 文档开关 -->
    <el-card shadow="never" class="grid-item">
      <template #header><b>文档开关</b></template>
      <p class="desc-text">
        springdoc-openapi 按 profile 控制：prod 环境下
        <code>api-docs.enabled=false</code>、<code>swagger-ui.enabled=false</code>，
        接口文档不对生产环境暴露；dev 环境下可正常访问 /swagger-ui.html 在线调试。
        切换 profile：<code>mvn spring-boot:run -Dspring-boot.run.profiles=prod</code>。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.hardening-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(460px, 1fr));
  gap: 16px;
}
.grid-item :deep(.el-card__body) {
  font-size: 14px;
}
.btn-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.rate-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}
code {
  background: #f4f4f5;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
}
</style>
