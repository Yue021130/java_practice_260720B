<template>
  <div>
    <h3>IP 归属地查询（ip2region）</h3>
    <el-form inline>
      <el-form-item label="IP 地址">
        <el-input v-model="ip" placeholder="例如 114.114.114.114" style="width: 220px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-descriptions v-if="result" :column="2" border>
      <el-descriptions-item label="IP">{{ result.ip }}</el-descriptions-item>
      <el-descriptions-item label="国家">{{ result.country || '-' }}</el-descriptions-item>
      <el-descriptions-item label="省份">{{ result.province || '-' }}</el-descriptions-item>
      <el-descriptions-item label="城市">{{ result.city || '-' }}</el-descriptions-item>
      <el-descriptions-item label="运营商">{{ result.isp || '-' }}</el-descriptions-item>
      <el-descriptions-item label="完整地址">{{ result.fullAddress || '-' }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchIp } from '../api/ip.js'

const ip = ref('114.114.114.114')
const result = ref(null)

async function handleSearch() {
  try {
    result.value = await searchIp(ip.value)
  } catch (e) {
    ElMessage.error(e.message || '查询失败')
  }
}
</script>
