import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 封装：
 * 请求拦截器 —— 自动携带 Authorization: Bearer <token>（JWT 存 localStorage）
 * 响应拦截器 —— 401 清 token 跳登录；403 方法级权限拦截；429 限流；code!=200 统一报错
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

function redirectLogin() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

service.interceptors.response.use(
  response => {
    const res = response.data
    // 统一包装结构：code 存在且不为 200 视为业务错误
    if (res && typeof res === 'object' && 'code' in res && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data
    const msg = (body && (body.message || body.msg)) || error.message || '网络异常'

    if (status === 401) {
      if (location.pathname.startsWith('/login')) {
        // 登录接口本身的 401：仅提示，不做跳转
        ElMessage.error(msg || '用户名或密码错误')
      } else {
        ElMessage.warning(msg || '登录已过期，请重新登录')
        redirectLogin()
      }
    } else if (status === 403) {
      ElMessage.error('无权限：方法级权限拦截')
    } else if (status === 429) {
      ElMessage.error('触发限流（429），请稍后再试')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default service
