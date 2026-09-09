import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getInfo() {
  return request.get('/auth/info')
}

export function getMenus() {
  return request.get('/auth/menus')
}

export function logout() {
  return request.post('/auth/logout')
}
