import request from './request'

export function pageUsers(params) {
  return request.get('/user/page', { params })
}

export function saveUser(data) {
  return data.id ? request.put('/user', data) : request.post('/user', data)
}

export function removeUser(id) {
  return request.delete(`/user/${id}`)
}
