import request from './request'

export function permissionTree() {
  return request.get('/permission/tree')
}
