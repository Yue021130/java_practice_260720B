import request from './request'

export function pageResources(params) {
  return request.get('/resource/page', { params })
}

export function getResource(id) {
  return request.get(`/resource/${id}`)
}

export function saveResource(data) {
  return request.post('/resource', data)
}

export function updateResource(data) {
  return request.put('/resource', data)
}

export function deleteResource(id) {
  return request.delete(`/resource/${id}`)
}

export function testRateLimit() {
  return request.get('/resource/test-rate-limit')
}
