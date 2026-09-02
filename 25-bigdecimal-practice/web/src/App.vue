<template>
  <div class="app">
    <header>
      <h1>BigDecimal 高精度金额计算实战</h1>
      <p>电商订单金额、折扣、税费、分账的高精度计算</p>
    </header>

    <main>
      <section class="card">
        <h2>1. 订单金额计算</h2>
        <label>单价 <input v-model="calc.price" type="text"/></label>
        <label>数量 <input v-model.number="calc.quantity" type="number"/></label>
        <label>折扣 <input v-model="calc.discount" type="text" placeholder="0.9"/></label>
        <label>税率(%) <input v-model="calc.taxRate" type="text" placeholder="6"/></label>
        <label>精度 <input v-model.number="calc.scale" type="number"/></label>
        <button @click="calculate">计算</button>
        <p class="tip">演示 multiply、setScale、divide 的正确用法。</p>
      </section>

      <section class="card">
        <h2>2. 分账计算</h2>
        <label>总金额 <input v-model="split.total" type="text"/></label>
        <label>平台比例 <input v-model="split.platformRate" type="text" placeholder="0.1"/></label>
        <label>商家比例 <input v-model="split.merchantRate" type="text" placeholder="0.2"/></label>
        <label>精度 <input v-model.number="split.scale" type="number"/></label>
        <button @click="doSplit">分账</button>
        <p class="tip">最后一方拿剩余金额，保证 sumCheck == total。</p>
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

async function call(name, fn) {
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
  call('calculate', () => api.calculate({
    price: calc.price,
    quantity: calc.quantity,
    discount: calc.discount,
    taxRate: calc.taxRate,
    scale: calc.scale
  }))
}

function doSplit() {
  call('split', () => api.split({
    total: split.total,
    platformRate: split.platformRate,
    merchantRate: split.merchantRate,
    scale: split.scale
  }))
}

function showPitfalls() {
  call('pitfalls', () => api.pitfalls())
}

function loadExplain() {
  call('explain', () => api.explain())
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
  width: 120px;
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
.error { color: #f56c6c; }
</style>
