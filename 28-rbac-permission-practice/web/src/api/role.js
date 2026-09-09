import request from './request'

export function listRoles() {
  return request.get('/role/list')
}

export function listRolePermissions(id) {
  return request.get(`/role/${id}/permissions`)
}

export function saveRole(data) {
  return request.post('/role', data)
}

export function assignRolePermissions(id, permissionIds) {
  return request.put(`/role/${id}/permissions`, { permissionIds })
}

export function removeRole(id) {
  return request.delete(`/role/${id}`)
}
