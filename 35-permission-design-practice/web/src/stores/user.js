import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout } from '../api/auth'

/**
 * 用户状态：token、用户信息、菜单树、按钮权限点。
 * 全部持久化到 localStorage，刷新页面后无需重新登录即可恢复动态路由。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    menus: JSON.parse(localStorage.getItem('menus') || '[]'),
    perms: JSON.parse(localStorage.getItem('perms') || '[]')
  }),
  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.token = data.token
      this.userInfo = { id: data.id, username: data.username, nickname: data.nickname }
      this.menus = data.menus || []
      this.perms = data.perms || []
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('menus', JSON.stringify(this.menus))
      localStorage.setItem('perms', JSON.stringify(this.perms))
    },
    /** 判断是否拥有某个按钮权限点；perms 含 *:*:* 视为超管全部放行 */
    hasPerm(code) {
      if (!code) return true
      return this.perms.includes('*:*:*') || this.perms.includes(code)
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
      this.userInfo = {}
      this.menus = []
      this.perms = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('menus')
      localStorage.removeItem('perms')
    }
  }
})
