import request from './request'

export function pageUsers(params) {
  return request.get('/user/page', { params })
}

export function getUser(id) {
  return request.get(`/user/${id}`)
}

export function saveUser(data) {
  return request.post('/user', data)
}

export function updateUser(data) {
  return request.put('/user', data)
}

export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}
