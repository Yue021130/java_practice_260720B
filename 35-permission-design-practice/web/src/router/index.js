import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

/** 菜单 path → 页面组件映射（后端只下发 path，组件由前端按约定映射） */
const componentMap = {
  '/system/user': () => import('../views/system/User.vue'),
  '/system/role': () => import('../views/system/Role.vue'),
  '/system/dept': () => import('../views/system/Dept.vue'),
  '/system/perm': () => import('../views/system/Perm.vue'),
  '/order': () => import('../views/biz/Order.vue')
}

/** 固定路由：不在后端 menus 里，登录后即可访问（挂在 layout 下） */
const fixedRoutes = [
  {
    path: '/log/op',
    name: 'OpLog',
    meta: { title: '操作日志' },
    component: () => import('../views/log/OpLog.vue')
  },
  {
    path: '/log/login',
    name: 'LoginLog',
    meta: { title: '登录日志' },
    component: () => import('../views/log/LoginLog.vue')
  }
]

/** 静态路由只有登录页和 layout 壳，业务页面全部登录后动态注册 */
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'layout',
    component: () => import('../layout/Index.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 动态路由是否已注册（模块级标记，刷新页面后自然重置，配合守卫重新注册） */
let dynamicRegistered = false

function addMenuRoute(menu) {
  const component = componentMap[menu.path]
  if (component) {
    router.addRoute('layout', {
      path: menu.path,
      name: menu.path,
      component,
      meta: { title: menu.name }
    })
  }
  if (menu.children && menu.children.length) {
    menu.children.forEach(addMenuRoute)
  }
}

/** 登录成功后根据 menus 树注册动态路由（含固定日志路由），最后兜底 404 */
export function registerDynamicRoutes(menus) {
  ;(menus || []).forEach(addMenuRoute)
  fixedRoutes.forEach((r) => router.addRoute('layout', r))
  router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/' })
  dynamicRegistered = true
}

export function isDynamicRegistered() {
  return dynamicRegistered
}

/** 第一个可访问的菜单路径，作为登录后/访问根路径的落地页 */
export function firstMenuPath(menus) {
  const find = (list) => {
    for (const m of list || []) {
      if (componentMap[m.path]) return m.path
      const child = find(m.children)
      if (child) return child
    }
    return ''
  }
  return find(menus) || '/log/op'
}

router.beforeEach((to) => {
  const userStore = useUserStore()
  // 白名单：登录页
  if (to.path === '/login') {
    return userStore.token ? '/' : true
  }
  // 未登录访问业务页面 → 跳登录页
  if (!userStore.token) {
    return '/login'
  }
  // 已登录但动态路由未注册（如刷新页面）：先注册再走一遍目标路由，避免 404
  if (!dynamicRegistered) {
    registerDynamicRoutes(userStore.menus)
    return { ...to, replace: true }
  }
  // 访问根路径时落到第一个菜单
  if (to.path === '/') {
    return firstMenuPath(userStore.menus)
  }
})

export default router
