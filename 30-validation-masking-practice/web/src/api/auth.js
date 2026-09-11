import request from './request'

/** 登录：返回 { token, id, username, nickname } */
export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getInfo() {
  return request.get('/auth/info')
}
