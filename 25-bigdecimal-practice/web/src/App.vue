<template>
  <div class="app">
    <header>
      <h1>BigDecimal 高精度金额计算实战</h1>
      <p>电商订单金额、折扣、税费、分账的高精度计算</p>
    </header>

    <main>
      <section class="card">
        <h2>1. 订单金额计算</h2>
        <label>单价 <input v-model="calc.price" type="text" placeholder="必填，如 99.99"/></label>
        <label>数量 <input v-model.number="calc.quantity" type="number" placeholder="必填，至少 1"/></label>
        <label>折扣 <input v-model="calc.discount" type="text" placeholder="可选，如 0.9"/></label>
        <label>税率(%) <input v-model="calc.taxRate" type="text" placeholder="可选，如 6 表示 6%"/></label>
        <label>精度 <input v-model.number="calc.scale" type="number" placeholder="0~10"/></label>
        <button @click="calculate">计算</button>
        <p class="tip">演示 multiply、setScale、divide 的正确用法。</p>
      </section>

      <section class="card">
        <h2>2. 分账计算</h2>
        <label>总金额 <input v-model="split.total" type="text" placeholder="必填，如 100"/></label>
        <label>平台比例 <input v-model="split.platformRate" type="text" placeholder="必填，如 0.1"/></label>
        <label>商家比例 <input v-model="split.merchantRate" type="text" placeholder="必填，如 0.2"/></label>
        <label>精度 <input v-model.number="split.scale" type="number" placeholder="0~10"/></label>
        <button @click="doSplit">分账</button>
        <p class="tip">平台比例 + 商家比例 不能超过 1。最后一方拿剩余金额，保证 sumCheck == total。</p>
      </section>

      <section class="card">
        <h2>3. 常见坑演示</h2>
        <button @click="showPitfalls">查看</button>
        <p class="tip">new BigDecimal(0.1) / new BigDecimal("0.1") / equals vs compareTo 对比。</p>
      </section>

      <section class="card">
        <h2>4. 八股速记</h2>
        <button @click="loadExplain">加载核心考点</button>
      </section>

      <div class="result">
        <h3>结果</h3>
        <pre v-if="result">{{ JSON.stringify(result, null, 2) }}</pre>
        <p v-if="error" class="error">{{ error }}</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import api from './api/amount.js'

const calc = reactive({
  price: '99.99',
  quantity: 3,
  discount: '0.9',
  taxRate: '6',
  scale: 2
})

const split = reactive({
  total: '100',
  platformRate: '0.1',
  merchantRate: '0.2',
  scale: 2
})

const result = ref(null)
const error = ref('')

function reset() {
  result.value = null
  error.value = ''
}

/** 验证订单计算表单，返回错误信息数组 */
function validateCalc() {
  const errs = []
  const price = parseFloat(calc.price)
  if (!calc.price || isNaN(price) || price <= 0) {
    errs.push('单价必须大于 0')
  }
  if (!calc.quantity || calc.quantity < 1 || !Number.isInteger(calc.quantity)) {
    errs.push('数量至少为 1（整数）')
  }
  if (calc.discount !== '' && calc.discount !== null && calc.discount !== undefined) {
    const d = parseFloat(calc.discount)
    if (isNaN(d) || d < 0) errs.push('折扣不能为负数')
  }
  if (calc.taxRate !== '' && calc.taxRate !== null && calc.taxRate !== undefined) {
    const t = parseFloat(calc.taxRate)
    if (isNaN(t) || t < 0) errs.push('税率不能为负数')
  }
  if (calc.scale === null || calc.scale === undefined || calc.scale < 0 || calc.scale > 10) {
    errs.push('精度范围 0~10')
  }
  return errs
}

/** 验证分账表单，返回错误信息数组 */
function validateSplit() {
  const errs = []
  if (!split.total || isNaN(parseFloat(split.total))) {
    errs.push('总金额不能为空')
  }
  const p = parseFloat(split.platformRate)
  const m = parseFloat(split.merchantRate)
  if (split.platformRate === '' || isNaN(p) || p < 0) {
    errs.push('平台比例不能为空且不能为负数')
  }
  if (split.merchantRate === '' || isNaN(m) || m < 0) {
    errs.push('商家比例不能为空且不能为负数')
  }
  if (!isNaN(p) && !isNaN(m) && p + m > 1) {
    errs.push('平台比例 + 商家比例 不能超过 1')
  }
  if (split.scale === null || split.scale === undefined || split.scale < 0 || split.scale > 10) {
    errs.push('精度范围 0~10')
  }
  return errs
}

async function call(fn) {
  reset()
  try {
    const res = await fn()
    if (res && res.code !== undefined && res.code !== 200) {
      throw new Error(res.message || '业务失败')
    }
    result.value = res && res.data !== undefined ? res.data : res
  } catch (e) {
    error.value = e.message
  }
}

function calculate() {
  const errs = validateCalc()
  if (errs.length) {
    error.value = errs.join('；')
    result.value = null
    return
  }
  call(() => api.calculate({
    price: calc.price,
    quantity: calc.quantity,
    discount: calc.discount || undefined,
    taxRate: calc.taxRate || undefined,
    scale: calc.scale
  }))
}

function doSplit() {
  const errs = validateSplit()
  if (errs.length) {
    error.value = errs.join('；')
    result.value = null
    return
  }
  call(() => api.split({
    total: split.total,
    platformRate: split.platformRate,
    merchantRate: split.merchantRate,
    scale: split.scale
  }))
}

function showPitfalls() {
  call(() => api.pitfalls())
}

function loadExplain() {
  call(() => api.explain())
}
</script>

<style>
* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background: #f5f7fa;
  color: #333;
}
.app { max-width: 900px; margin: 0 auto; padding: 20px; }
header { text-align: center; margin-bottom: 24px; }
header h1 { margin: 0; font-size: 28px; }
header p { color: #666; margin-top: 6px; }

.card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.card h2 {
  font-size: 18px;
  margin-bottom: 12px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}
.card label { display: inline-block; margin-right: 12px; margin-bottom: 8px; }
.card input {
  padding: 6px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-left: 6px;
  width: 160px;
}
.card input:focus {
  outline: none;
  border-color: #409eff;
}
.card button {
  padding: 6px 16px;
  border: none;
  background: #409eff;
  color: #fff;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 8px;
}
.card button:hover {
  background: #66b1ff;
}
.tip { color: #999; font-size: 13px; margin-top: 10px; }

.result {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.result pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}
.error { color: #f56c6c; font-weight: 500; }
</style>
