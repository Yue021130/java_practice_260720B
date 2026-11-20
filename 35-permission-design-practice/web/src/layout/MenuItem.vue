<template>
  <template v-for="menu in menus" :key="menu.id">
    <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path || String(menu.id)">
      <template #title>
        <el-icon><component :is="iconOf(menu.icon)" /></el-icon>
        <span>{{ menu.name }}</span>
      </template>
      <menu-item :menus="menu.children" />
    </el-sub-menu>
    <el-menu-item v-else-if="menu.path" :index="menu.path">
      <el-icon><component :is="iconOf(menu.icon)" /></el-icon>
      <template #title>{{ menu.name }}</template>
    </el-menu-item>
  </template>
</template>

<script setup>
import { Menu, Setting, User, Avatar, OfficeBuilding, Tickets, List, Document } from '@element-plus/icons-vue'

defineOptions({ name: 'MenuItem' })
defineProps({
  menus: { type: Array, default: () => [] }
})

const iconMap = {
  menu: Menu, setting: Setting, user: User, avatar: Avatar,
  dept: OfficeBuilding, order: Tickets, log: List, document: Document
}

function iconOf(name) {
  return iconMap[name] || Menu
}
</script>
