import { defineStore } from 'pinia'
import { login as apiLogin, getInfo, getMenus, logout as apiLogout } from '../api/auth'

/**
 * 用户状态：token、用户信息、角色、权限点、动态菜单
 * token 持久化到 localStorage，刷新页面不丢失登录态
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: null,
    username: '',
    nickname: '',
    roles: [],
    permissions: [],
    menus: [],
    // 动态路由是否已注册（刷新后需重新注册）
    routesLoaded: false
  }),

  getters: {
    isLogin: state => !!state.token,
    /** 数据权限范围描述（后端 data_scope 的角色编码映射） */
    dataScopeText: state => {
      if (state.roles.includes('ADMIN')) return '全部数据'
      if (state.roles.includes('MANAGER')) return '本部门数据'
      return '仅本人数据'
    }
  },

  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.token = data.token
      localStorage.setItem('token', data.token)
    },

    /** 拉取用户信息 + 菜单树（动态路由数据源） */
    async fetchInfo() {
      const [info, menus] = await Promise.all([getInfo(), getMenus()])
      this.userId = info.userId
      this.username = info.username
      this.nickname = info.nickname
      this.roles = info.roles || []
      this.permissions = info.permissions || []
      this.menus = menus || []
    },

    has(permission) {
      return this.permissions.includes(permission)
    },

    async logout() {
      try {
        await apiLogout()
      } finally {
        this.reset()
      }
    },

    reset() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.roles = []
      this.permissions = []
      this.menus = []
      this.routesLoaded = false
      localStorage.removeItem('token')
    }
  }
})
