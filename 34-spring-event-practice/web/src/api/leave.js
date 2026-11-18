import request from './request'

/** 请假单分页查询 */
export function pageLeave(params) {
  return request.get('/leave/page', { params })
}

export function getLeave(id) {
  return request.get(`/leave/${id}`)
}

/** 提交请假单（发布 LeaveSubmittedEvent） */
export function submitLeave(data) {
  return request.post('/leave/submit', data)
}

/** 审批通过（事务提交后发布 LeaveApprovedEvent） */
export function approveLeave(data) {
  return request.post('/leave/approve', data)
}

/** 审批拒绝（事务提交后发布 LeaveApprovedEvent） */
export function rejectLeave(data) {
  return request.post('/leave/reject', data)
}
