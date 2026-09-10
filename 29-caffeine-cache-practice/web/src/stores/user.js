import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getInfo } from '../api/auth'

/**
 * 用户状态：token 与用户信息，token 持久化到 localStorage 保证刷新不丢。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  actions: {
    async login(form) {
      const data = await apiLogin(form)
      this.token = data.token
      this.userInfo = { id: data.id, username: data.username, nickname: data.nickname }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    async fetchInfo() {
      const data = await getInfo()
      this.userInfo = data
      localStorage.setItem('userInfo', JSON.stringify(data))
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
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
