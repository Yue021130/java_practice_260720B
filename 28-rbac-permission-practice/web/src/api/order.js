import request from './request'

export function pageOrders(params) {
  return request.get('/order/page', { params })
}

export function createOrder(data) {
  return request.post('/order', data)
}

export function refundOrder(id) {
  return request.post(`/order/${id}/refund`)
}

export function removeOrder(id) {
  return request.delete(`/order/${id}`)
}
