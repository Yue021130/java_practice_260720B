import request from './request'

export function pageCustomers(params) {
  return request.get('/customer/page', { params })
}

export function getCustomer(id) {
  return request.get(`/customer/${id}`)
}

export function saveCustomer(data) {
  return request.post('/customer', data)
}

export function updateCustomer(data) {
  return request.put('/customer', data)
}

export function deleteCustomer(id) {
  return request.delete(`/customer/${id}`)
}
