<template>
  <div>
    <h3>策略模式：折扣计算</h3>
    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="折扣策略">
          <el-radio-group v-model="form.strategyType">
            <el-radio label="NORMAL">普通会员（无折扣）</el-radio>
            <el-radio label="VIP">VIP（8.8折）</el-radio>
            <el-radio label="SEASONAL">季节性促销（满200减50再9折）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原始金额">
          <el-input-number v-model="form.originalAmount" :min="0" :precision="2" :step="10" style="width: 200px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleCalculate">计算折扣</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="result !== null" :title="`折扣后金额：${result} 元`" type="success" :closable="false" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { calculateDiscount } from '../api/discount.js'

const form = ref({
  strategyType: 'VIP',
  originalAmount: 100
})
const result = ref(null)

async function handleCalculate() {
  try {
    result.value = await calculateDiscount({
      strategyType: form.value.strategyType,
      originalAmount: form.value.originalAmount
    })
    ElMessage.success('计算成功')
  } catch (e) {
    ElMessage.error(e.message || '计算失败')
    result.value = null
  }
}
</script>
