<template>
  <div>
    <h3>@Transactional 失效场景演示</h3>
    <el-button type="primary" @click="loadScenes">运行全部场景</el-button>
    <el-table :data="scenes" style="margin-top: 16px;" v-loading="loading">
      <el-table-column prop="scene" label="场景" width="160" />
      <el-table-column prop="description" label="说明" />
      <el-table-column prop="rollbackSuccess" label="是否回滚" width="120">
        <template #default="{ row }">
          <el-tag :type="row.rollbackSuccess ? 'success' : 'danger'">
            {{ row.rollbackSuccess ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="结果" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTxScenes } from '../api/tx.js'

const scenes = ref([])
const loading = ref(false)

async function loadScenes() {
  loading.value = true
  try {
    scenes.value = await getTxScenes()
    ElMessage.success('场景运行完成')
  } catch (e) {
    ElMessage.error(e.message || '运行失败')
  } finally {
    loading.value = false
  }
}
</script>
