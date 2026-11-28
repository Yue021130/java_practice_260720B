import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('../layout/Index.vue'),
      redirect: '/order',
      children: [
        {
          path: 'order',
          name: 'Order',
          component: () => import('../views/Orders.vue'),
          meta: { title: '订单管理' }
        },
        {
          path: 'hardening',
          name: 'Hardening',
          component: () => import('../views/Hardening.vue'),
          meta: { title: '加固看板' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} - 生产级加固实战`
  const store = useUserStore()
  if (to.path === '/login') {
    return next()
  }
  if (!store.isLogin) {
    return next('/login')
  }
  next()
})

export default router
