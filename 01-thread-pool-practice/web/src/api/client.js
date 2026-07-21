import axios from 'axios'

// axios 统一封装：baseURL 指向 /api，由 vite 代理转发到后端 8081
const client = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/** 获取所有线程池实时指标 */
export function getMetrics() {
  return client.get('/pool/metrics').then((res) => res.data)
}

/** 向指定池提交 count 个耗时 taskDurationMs 毫秒的模拟任务 */
export function submitTasks(poolName, count, taskDurationMs) {
  return client
    .post(`/pool/${poolName}/submit`, null, { params: { count, taskDurationMs } })
    .then((res) => res.data)
}

/** 动态调整指定池的核心/最大线程数 */
export function resizePool(poolName, corePoolSize, maxPoolSize) {
  return client
    .post(`/pool/${poolName}/resize`, null, { params: { corePoolSize, maxPoolSize } })
    .then((res) => res.data)
}
