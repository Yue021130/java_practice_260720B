<template>
  <div class="app">
    <el-page-header title="返回" content="第27章：事务失效 + IP 归属地" />
    <div class="container">
      <TxScene />
      <el-divider />
      <IpSearch />
    </div>
  </div>
</template>

<script setup>
import TxScene from './components/TxScene.vue'
import IpSearch from './components/IpSearch.vue'
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
