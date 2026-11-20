import request from './request'

/** 角色分页查询 */
export function pageRole(params) {
  return request.get('/role/page', { params })
}

export function getRole(id) {
  return request.get(`/role/${id}`)
}

export function createRole(data) {
  return request.post('/role', data)
}

export function updateRole(data) {
  return request.put('/role', data)
}

export function deleteRole(id) {
  return request.delete(`/role/${id}`)
}
