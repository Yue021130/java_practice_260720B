<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await store.login(form)
    await store.fetchInfo()
    store.routesLoaded = true
    const { addDynamicRoutes } = await import('../router')
    addDynamicRoutes(store.menus)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">RBAC 权限实战</h2>
      <p class="subtitle">第28章：Sa-Token JWT + 动态路由 + 数据权限</p>
      <el-form @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码"
                    show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="tips">
        <p>演示账号（密码均为 123456）：</p>
        <p>admin — 全部数据 + 全部权限</p>
        <p>manager — 本部门数据 + 订单/用户查询</p>
        <p>zhangsan — 仅本人数据 + 订单查询/下单</p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #304156 0%, #409eff 100%);
}
.login-card {
  width: 380px;
}
.title {
  text-align: center;
  margin: 0;
}
.subtitle {
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.btn {
  width: 100%;
}
.tips {
  margin-top: 16px;
  padding: 12px;
  background: #f4f4f5;
  border-radius: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}
.tips p {
  margin: 0;
}
</style>
