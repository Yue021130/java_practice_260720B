<template>
  <div class="app">
    <el-page-header title="返回" content="第26章：树形结构 + 策略模式" />
    <div class="container">
      <MenuTree />
      <el-divider />
      <PaymentDemo />
      <el-divider />
      <DiscountDemo />
    </div>
  </div>
</template>

<script setup>
import MenuTree from './components/MenuTree.vue'
import PaymentDemo from './components/PaymentDemo.vue'
import DiscountDemo from './components/DiscountDemo.vue'
</script>

<style>
.app {
  padding: 20px;
}
.container {
  margin-top: 20px;
}
h3 {
  margin-bottom: 16px;
}
</style>
