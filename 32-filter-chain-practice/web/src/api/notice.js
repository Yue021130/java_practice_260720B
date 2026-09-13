import request from './request'

// ==================== 公告管理 API ====================

// 分页查询公告
export function pageNotice(params) {
  return request.get('/notice/page', { params })
}

// 公告详情
export function getNotice(id) {
  return request.get(`/notice/${id}`)
}

// 新增公告
export function saveNotice(data) {
  return request.post('/notice', data)
}

// 修改公告
export function updateNotice(data) {
  return request.put('/notice', data)
}

// 删除公告
export function deleteNotice(id) {
  return request.delete(`/notice/${id}`)
}
