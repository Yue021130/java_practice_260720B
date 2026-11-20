import request from './request'

/** 部门树（后端返回平铺列表，前端自行构建树） */
export function getDeptTree() {
  return request.get('/dept/tree')
}

export function createDept(data) {
  return request.post('/dept', data)
}

export function updateDept(data) {
  return request.put('/dept', data)
}

export function deleteDept(id) {
  return request.delete(`/dept/${id}`)
}
