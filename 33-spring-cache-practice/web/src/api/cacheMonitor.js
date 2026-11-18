import request from './request'

/** 二级缓存统计：返回每个 cacheName 一组 CacheMetrics */
export function getCacheStats() {
  return request.get('/cache/stats')
}

/** 查询指定缓存的 key 列表：返回 { l1Keys: [], l2Keys: [] } */
export function getCacheKeys(cache) {
  return request.get('/cache/keys', { params: { cache } })
}

export function clearCache(cache) {
  return request.post('/cache/clear', null, { params: { cache } })
}
