<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, Monitor, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const activeMenu = computed(() => route.path)
const nickname = computed(() => store.userInfo.nickname || store.userInfo.username || '-')
const role = computed(() => store.userInfo.role || '-')

async function handleLogout() {
  await store.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">生产级加固实战</div>
      <el-menu :default-active="activeMenu" router background-color="#304156"
               text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/order">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/hardening">
          <el-icon><Monitor /></el-icon>
          <span>加固看板</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title || '' }}</span>
        <el-dropdown @command="handleLogout">
          <span class="user">
            {{ nickname }}（{{ role }}）
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
  font-size: 17px;
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
  overflow-y: auto;
}
</style>
