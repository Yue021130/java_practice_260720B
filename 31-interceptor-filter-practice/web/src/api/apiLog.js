import request from './request'

export function pageApiLogs(params) {
  return request.get('/api-log/page', { params })
}
