import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 封装：
 * 请求拦截器 —— 自动携带 Authorization（Sa-Token JWT）
 * 响应拦截器 —— code!==0 统一报错；401 清理登录态并跳回登录页
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      ElMessage.warning(res.msg || '登录已过期，请重新登录')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg))
  },
  error => {
    const res = error.response?.data
    ElMessage.error(res?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
