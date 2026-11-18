import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/Index.vue'),
    redirect: '/product',
    children: [
      { path: 'product', component: () => import('../views/Product.vue') },
      { path: 'cache-monitor', component: () => import('../views/CacheMonitor.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 全局路由守卫：未登录访问业务页面时跳回登录页 */
router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }
  if (to.path === '/login' && userStore.token) {
    return '/'
  }
})

export default router
