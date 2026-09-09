import request from './request.js'

export function calculateDiscount(data) {
  return request.post('/discount', data)
}
