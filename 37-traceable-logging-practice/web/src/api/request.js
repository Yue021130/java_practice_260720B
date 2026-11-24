import axios from 'axios'

/**
 * Axios 封装（日志 Demo，无登录态）：
 * - baseURL 指向 /api，由 vite dev server 代理到 http://localhost:8080；
 * - 不配置全局拦截器，调用方自行处理响应体 / 响应头。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

/**
 * 携带自定义 X-Trace-Id 头发起 GET 请求（Headers 透传演示专用）。
 * 单独函数而非全局拦截器，避免影响其他按钮发起的普通请求。
 */
export function getWithTraceId(url, traceId) {
  return request.get(url, {
    headers: { 'X-Trace-Id': traceId }
  })
}

export default request
