import request from './request.js'

export function getMenuTree() {
  return request.get('/menu/tree')
}

export function getMenuList() {
  return request.get('/menu/list')
}

export function addMenu(data) {
  return request.post('/menu', data)
}

export function deleteMenu(id) {
  return request.delete(`/menu/${id}`)
}
