<script setup>
import { useUserStore } from '../stores/user'

const store = useUserStore()
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>当前用户</template>
          <p><strong>{{ store.nickname }}</strong>（{{ store.username }}）</p>
          <p>角色：{{ store.roles.join('、') || '无' }}</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>数据权限范围</template>
          <el-tag type="success" size="large">{{ store.dataScopeText }}</el-tag>
          <p class="tip">订单列表将按此范围自动过滤（MyBatis-Plus 拦截器注入 WHERE）</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>权限点数量</template>
          <p class="count">{{ store.permissions.length }}</p>
          <p class="tip">{{ store.permissions.join('、') || '无任何权限点' }}</p>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>本章演示点</template>
      <el-steps direction="vertical" :active="4">
        <el-step title="Sa-Token JWT 无状态登录" description="POST /api/auth/login 签发 JWT，Axios 拦截器自动携带" />
        <el-step title="RBAC 功能权限" description="@SaCheckPermission 权限点校验，无权限返回 403，前端 v-permission 隐藏按钮" />
        <el-step title="动态路由" description="后端 /auth/menus 按角色下发菜单，前端登录后动态 addRoute" />
        <el-step title="数据权限" description="DataPermissionInterceptor 统一注入 dept_id / user_id 条件，告别硬编码 WHERE" />
      </el-steps>
    </el-card>
  </div>
</template>

<style scoped>
.tip {
  color: #909399;
  font-size: 12px;
}
.count {
  font-size: 28px;
  font-weight: 600;
  color: #409eff;
  margin: 0;
}
</style>
