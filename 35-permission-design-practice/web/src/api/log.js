import request from './request'

/** 操作日志分页查询 */
export function pageOpLog(params) {
  return request.get('/log/op/page', { params })
}

/** 登录日志分页查询 */
export function pageLoginLog(params) {
  return request.get('/log/login/page', { params })
}
