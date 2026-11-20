<template>
  <el-container class="layout">
    <el-aside width="230px" class="aside">
      <div class="logo">企业级权限中心</div>
      <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409EFF">
        <menu-item :menus="userStore.menus" />
        <el-sub-menu index="/log">
          <template #title>
            <el-icon><document /></el-icon>
            <span>日志中心</span>
          </template>
          <el-menu-item index="/log/op">操作日志</el-menu-item>
          <el-menu-item index="/log/login">登录日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">企业级权限中心</span>
        <el-dropdown @command="onCommand">
          <span class="user">
            {{ userStore.userInfo.nickname || userStore.userInfo.username }}
            <el-icon><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Document } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import MenuItem from './MenuItem.vue'

const userStore = useUserStore()
const router = useRouter()

async function onCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background-color: #304156; }
.logo { color: #fff; text-align: center; line-height: 60px; font-weight: bold; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #e6e6e6;
}
.title { font-size: 15px; font-weight: bold; }
.user { cursor: pointer; display: flex; align-items: center; gap: 4px; }
</style>
