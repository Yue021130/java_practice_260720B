import request from './request'

/** 缓存统计 */
export function cacheStats() {
  return request.get('/cache/stats')
}

/** 缓存对比实验：同一用户连查两次，第一次走库第二次走缓存 */
export function cacheCompare(id) {
  return request.get(`/cache/compare/${id}`)
}

/** 清空全部缓存 */
export function cacheClear() {
  return request.get('/cache/clear')
}
