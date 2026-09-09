<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Odometer, Tickets, User, Lock, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const iconMap = { Odometer, Tickets, User, Lock }
const activeMenu = computed(() => route.path)

function resolveIcon(name) {
  return iconMap[name] || Odometer
}

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  await store.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">RBAC 权限实战</div>
      <el-menu :default-active="activeMenu" router background-color="#304156"
               text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item v-for="menu in store.menus" :key="menu.id" :index="menu.path">
          <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
          <span>{{ menu.name }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title || '工作台' }}</span>
        <el-dropdown @command="handleLogout">
          <span class="user">
            {{ store.nickname }}（{{ store.roles.join(' / ') || '无角色' }}）
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  line-height: 60px;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}
.main {
  background: #f0f2f5;
}
</style>
