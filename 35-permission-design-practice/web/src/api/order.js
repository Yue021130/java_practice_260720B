import request from './request'

/** 订单分页查询 */
export function pageOrder(params) {
  return request.get('/order/page', { params })
}

/** 订单详情（带数据权限校验，越权访问他人订单返回 403） */
export function getOrder(id) {
  return request.get(`/order/${id}`)
}

/** 不设防的订单详情（越权对比演示用：只校验登录，不校验数据归属） */
export function getOrderInsecure(id) {
  return request.get(`/order/insecure/${id}`)
}
