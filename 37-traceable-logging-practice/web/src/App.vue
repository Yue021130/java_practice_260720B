<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">第37章 日志可追溯</div>
      <el-menu :default-active="active" background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409EFF" @select="onSelect">
        <el-menu-item index="trace">链路演示</el-menu-item>
        <el-menu-item index="batch">批次进度</el-menu-item>
        <el-menu-item index="awk">日志检索</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">MDC TraceId 全链路日志追踪实战</span>
      </el-header>
      <el-main>
        <component :is="currentView" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import TraceDemo from './views/TraceDemo.vue'
import BatchTask from './views/BatchTask.vue'
import AwkGuide from './views/AwkGuide.vue'

const active = ref('trace')
const views = { trace: TraceDemo, batch: BatchTask, awk: AwkGuide }
const currentView = computed(() => views[active.value])

function onSelect(index) {
  active.value = index
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background-color: #304156; }
.logo { color: #fff; text-align: center; line-height: 60px; font-weight: bold; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #e6e6e6; background: #fff;
}
.title { font-size: 15px; }
</style>
