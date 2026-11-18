import request from './request'

/** 商品分页查询 */
export function pageProduct(params) {
  return request.get('/product/page', { params })
}

export function getProduct(id) {
  return request.get(`/product/${id}`)
}

/** 热点商品查询：演示缓存击穿防护（逻辑过期 + 互斥锁） */
export function getHotProduct(id) {
  return request.get(`/product/hot/${id}`)
}

export function saveProduct(data) {
  return request.post('/product', data)
}

export function updateProduct(data) {
  return request.put('/product', data)
}

export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}
