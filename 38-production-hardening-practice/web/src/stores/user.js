import { defineStore } from 'pinia'
import request from '../api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  getters: {
    isLogin: state => !!state.token
  },
  actions: {
    async login(form) {
      const data = await request.post('/auth/login', form)
      this.token = data.token
      this.userInfo = {
        username: data.username,
        nickname: data.nickname,
        role: data.role
      }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    async fetchMe() {
      const data = await request.get('/auth/me')
      this.userInfo = {
        username: data.username,
        nickname: data.nickname,
        role: data.role
      }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (e) {
        // 即使后端登出失败，本地登录态也要清理
      }
      this.reset()
    },
    reset() {
      this.token = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
