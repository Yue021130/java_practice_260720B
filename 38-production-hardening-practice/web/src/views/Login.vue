<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form)
    ElMessage.success('登录成功')
    router.push('/order')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">第38章：生产级加固实战</h2>
      <p class="subtitle">JWT 认证 · 方法级权限 · 限流 · 审计</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="tip">
        演示账号：admin（ADMIN）/ user01（USER），密码均为 123456
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
  background: linear-gradient(135deg, #304156 0%, #1f2d3d 100%);
}
.login-card {
  width: 400px;
  border-radius: 8px;
}
.title {
  text-align: center;
  font-size: 20px;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-bottom: 24px;
}
.btn {
  width: 100%;
}
.tip {
  margin-top: 16px;
  padding: 10px 12px;
  background: #f4f4f5;
  border-radius: 4px;
  color: #909399;
  font-size: 12px;
  text-align: center;
}
</style>
