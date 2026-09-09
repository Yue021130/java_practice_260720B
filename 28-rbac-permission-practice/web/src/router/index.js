import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

/**
 * 静态路由：登录页、403、布局壳 + 默认工作台
 * 业务菜单（订单/用户/角色）由后端 /auth/menus 下发，登录后动态注册
 */
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
      component: () => import('../layout/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { title: '工作台' }
        }
      ]
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('../views/Forbidden.vue'),
      meta: { title: '无权限' }
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// 动态导入 views 目录下所有组件，供菜单 component 字段解析
const viewModules = import.meta.glob('../views/*.vue')

/**
 * 按后端下发的菜单树动态注册路由
 * @param {Array} menus 菜单树（type=menu 的权限节点）
 */
export function addDynamicRoutes(menus) {
  const flatten = nodes => {
    nodes.forEach(node => {
      const component = viewModules[`../views/${node.component}.vue`]
      if (node.type === 'menu' && node.path && component) {
        router.addRoute('Layout', {
          path: node.path.replace(/^\//, ''),
          name: node.code,
          component,
          meta: { title: node.name, icon: node.icon }
        })
      }
      if (node.children?.length) {
        flatten(node.children)
      }
    })
  }
  flatten(menus)
}

router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || ''} - RBAC 权限实战`
  const store = useUserStore()
  if (to.path === '/login') {
    return next()
  }
  if (!store.isLogin) {
    return next('/login')
  }
  // 刷新页面后动态路由丢失：重新拉取信息并注册
  if (!store.routesLoaded) {
    try {
      await store.fetchInfo()
      addDynamicRoutes(store.menus)
      store.routesLoaded = true
      // 重新进入目标路由，使刚注册的路由生效
      return next({ ...to, replace: true })
    } catch (e) {
      store.reset()
      return next('/login')
    }
  }
  next()
})

export default router
