import request from './request.js'

export function pay(data) {
  return request.post('/pay', data)
}
