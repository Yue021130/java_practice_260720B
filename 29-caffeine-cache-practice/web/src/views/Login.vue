<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">第29章 · 用户管理系统</h2>
      <p class="subtitle">Sa-Token JWT 无状态登录认证</p>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>
      <p class="tip">默认账号：admin / 123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh; display: flex; justify-content: center; align-items: center;
  background: linear-gradient(135deg, #304156, #409EFF);
}
.login-card { width: 380px; }
.title { text-align: center; margin: 0; }
.subtitle { text-align: center; color: #909399; font-size: 13px; }
.btn { width: 100%; }
.tip { text-align: center; color: #c0c4cc; font-size: 12px; }
</style>
