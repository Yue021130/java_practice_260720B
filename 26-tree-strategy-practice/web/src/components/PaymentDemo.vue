<template>
  <div>
    <h3>策略模式：支付渠道</h3>
    <el-card>
      <el-form :model="form" label-width="100px">
        <el-form-item label="支付方式">
          <el-radio-group v-model="form.payType">
            <el-radio label="ALIPAY">支付宝（0.6%手续费）</el-radio>
            <el-radio label="WECHAT">微信（0.38%手续费）</el-radio>
            <el-radio label="BALANCE">余额（无手续费）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" placeholder="ORDER_20260827001" />
        </el-form-item>
        <el-form-item label="支付金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" style="width: 200px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handlePay">发起支付</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="result !== null" :title="`实付金额：${result} 元`" type="success" :closable="false" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pay } from '../api/pay.js'

const form = ref({
  payType: 'ALIPAY',
  orderNo: 'ORDER_20260827001',
  amount: 100
})
const result = ref(null)

async function handlePay() {
  try {
    result.value = await pay({
      payType: form.value.payType,
      orderNo: form.value.orderNo,
      amount: form.value.amount
    })
    ElMessage.success('支付成功')
  } catch (e) {
    ElMessage.error(e.message || '支付失败')
    result.value = null
  }
}
</script>
