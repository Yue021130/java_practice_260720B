import request from './request.js'

export function searchIp(ip) {
  return request.get('/ip/search', { params: { ip } })
}
