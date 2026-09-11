<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">第30章 校验脱敏</div>
      <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409EFF">
        <el-menu-item index="/customer">
          <span>客户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">Jakarta Validation 参数校验 + Jackson 数据脱敏</span>
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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const router = useRouter()

onMounted(() => {
  // 刷新页面后重新拉取最新用户信息（昵称等可能被后台修改）
  userStore.fetchInfo().catch(() => {})
})

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
.title { font-size: 15px; }
.user { cursor: pointer; display: flex; align-items: center; gap: 4px; }
</style>
