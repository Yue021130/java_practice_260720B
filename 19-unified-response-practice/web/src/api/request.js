import axios from 'axios'

/**
 * 统一封装 axios：响应拦截器根据后端 Result 结构统一处理。
 *
 * <p>约定：</p>
 * <ul>
 *     <li>code === 0：业务成功，直接返回 data</li>
 *     <li>code === 401：登录过期，跳转登录页</li>
 *     <li>其他 code：统一弹出错误提示，并 reject</li>
 * </ul>
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 1. 非 Result 结构（比如文件下载的 blob）直接放行
    if (response.config.responseType === 'blob') {
      return response
    }

    // 2. 兼容非 Result 结构的兜底：如果取不到 code，直接返回原响应
    if (res == null || typeof res.code !== 'number') {
      return res
    }

    // 3. 业务成功：直接返回 data，业务代码里不用再 res.data.data
    if (res.code === 0) {
      return res.data
    }

    // 4. 登录过期，跳转登录页
    if (res.code === 401) {
      alert('登录已过期，请重新登录')
      window.location.href = '/login'
      return Promise.reject(new Error(res.msg || '登录已过期'))
    }

    // 5. 其他失败：统一弹出提示
    alert(`请求失败 [${res.code}]: ${res.msg || '系统繁忙'}`)
    return Promise.reject(new Error(res.msg || '系统繁忙'))
  },
  (error) => {
    alert('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
