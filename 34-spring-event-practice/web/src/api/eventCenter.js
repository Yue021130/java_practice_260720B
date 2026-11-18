import request from './request'

/** 事件记录分页查询 */
export function pageEvents(params) {
  return request.get('/event/page', { params })
}

export function getEvent(id) {
  return request.get(`/event/${id}`)
}

/** 站内通知分页查询 */
export function pageNotifies(params) {
  return request.get('/event/notify/page', { params })
}

/** 审计日志分页查询 */
export function pageAudits(params) {
  return request.get('/event/audit/page', { params })
}
