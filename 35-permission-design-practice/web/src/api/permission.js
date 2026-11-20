import request from './request'

/** 权限点树（菜单 + 按钮） */
export function getPermissionTree() {
  return request.get('/permission/tree')
}

export function createPermission(data) {
  return request.post('/permission', data)
}

export function updatePermission(data) {
  return request.put('/permission', data)
}

export function deletePermission(id) {
  return request.delete(`/permission/${id}`)
}

/** 刷新权限缓存 */
export function refreshPermissionCache() {
  return request.post('/permission/refresh-cache')
}
