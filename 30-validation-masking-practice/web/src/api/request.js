import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

/**
 * Axios 封装：
 * - 请求拦截器：自动携带 Authorization 头（Token 来自 Pinia，持久化在 localStorage）；
 * - 响应拦截器：解包后端统一返回 Result，code !== 0 时提示错误并 reject，
 *   401（未登录/过期）时清空登录态并跳回登录页。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 0) {
      return res.data
    }
    if (res.code === 401) {
      handleUnauthorized(res.msg)
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      handleUnauthorized('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized(msg) {
  const userStore = useUserStore()
  userStore.reset()
  ElMessage.warning(msg)
  router.push('/login')
}

export default request
