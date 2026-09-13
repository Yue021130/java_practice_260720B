import request from './request'

// ==================== 链路日志 API ====================

// 分页查询请求日志（数据由 TimingFilter 记录，含鉴权失败的 401）
export function pageRequestLog(params) {
  return request.get('/request-log/page', { params })
}
