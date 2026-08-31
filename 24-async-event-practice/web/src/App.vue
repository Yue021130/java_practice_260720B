<template>
  <div class="app">
    <header>
      <h1>异步任务与 Spring Event 实战</h1>
      <p>@Async 自定义线程池 + ApplicationEvent 解耦订单支付后续流程</p>
    </header>

    <main>
      <section class="card">
        <h2>1. 创建订单</h2>
        <label>用户 ID <input v-model.number="userId" type="number"/></label>
        <label>金额 <input v-model.number="amount" type="number" step="0.01"/></label>
        <button @click="createOrder">创建订单</button>
        <p v-if="currentOrderNo" class="info">当前订单号：{{ currentOrderNo }}</p>
      </section>

      <section class="card">
        <h2>2. 支付订单（异步事件）</h2>
        <button @click="payOrder" :disabled="!currentOrderNo">异步支付</button>
        <p class="tip">支付后发布 OrderPaidEvent，短信/邮件/积分三个监听器并行异步处理。</p>
      </section>

      <section class="card">
        <h2>3. 支付订单（同步对比）</h2>
        <button @click="paySync" :disabled="!currentOrderNo">同步支付</button>
        <p class="tip">串行执行短信、邮件、积分逻辑，耗时更长，阻塞主线程。</p>
      </section>

      <section class="card">
        <h2>4. 查询通知日志</h2>
        <button @click="queryLogs" :disabled="!currentOrderNo">查询日志</button>
        <p class="tip">异步支付后等待 1~2 秒再查询，应看到 SMS、EMAIL、POINTS 三条记录。</p>
      </section>

      <section class="card">
        <h2>5. 八股速记</h2>
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
import { ref } from 'vue'
import api from './api/order.js'

const userId = ref(1)
const amount = ref(99.99)
const currentOrderNo = ref('')
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

async function createOrder() {
  await call('createOrder', () => api.createOrder(userId.value, amount.value))
  if (result.value && result.value.orderNo) {
    currentOrderNo.value = result.value.orderNo
  }
}

function payOrder() {
  call('payOrder', () => api.payOrder(currentOrderNo.value))
}

function paySync() {
  call('paySync', () => api.payOrderSync(currentOrderNo.value))
}

function queryLogs() {
  call('queryLogs', () => api.notifyLogs(currentOrderNo.value))
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
.app { max-width: 800px; margin: 0 auto; padding: 20px; }
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
.card label { display: inline-block; margin-right: 12px; }
.card input {
  padding: 6px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-left: 6px;
  width: 140px;
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
.card button:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}
.info { color: #67c23a; font-size: 14px; margin-top: 10px; }
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
