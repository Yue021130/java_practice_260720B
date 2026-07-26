<script setup>
import { ref } from 'vue'
import { modules, totalScenarios } from './scenarios'
import ScenarioCard from './components/ScenarioCard.vue'

const activeKey = ref(modules[0].key)

function scrollTo(key) {
  activeKey.value = key
  const el = document.getElementById('module-' + key)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="page">
    <header class="site-header">
      <div class="title-row">
        <h1>03 · Java 集合与泛型全场景实践</h1>
        <span class="badge">{{ totalScenarios }} 个场景 / {{ modules.length }} 个模块</span>
      </div>
      <p class="subtitle">
        逐个运行集合与泛型实验场景，观察 List / Set / Map / Queue / 工具类 / 泛型的行为与面试考点。
      </p>
      <p class="backend-hint">后端地址：<code>http://localhost:8083</code>（开发环境由 Vite 代理 /api 转发）</p>
    </header>

    <div class="layout">
      <nav class="side-nav">
        <button
          v-for="m in modules"
          :key="m.key"
          class="nav-item"
          :class="{ active: activeKey === m.key }"
          @click="scrollTo(m.key)"
        >
          <span class="nav-name">{{ m.name }}</span>
          <span class="nav-count">{{ m.scenarios.length }}</span>
        </button>
      </nav>

      <main class="content">
        <section v-for="m in modules" :key="m.key" :id="'module-' + m.key" class="module-group">
          <div class="module-head">
            <h2>{{ m.name }}</h2>
            <span class="module-desc">{{ m.desc }}</span>
          </div>
          <div class="card-grid">
            <ScenarioCard v-for="s in m.scenarios" :key="s.id" :scenario="s" />
          </div>
        </section>
      </main>
    </div>
  </div>
</template>
