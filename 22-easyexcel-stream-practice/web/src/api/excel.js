import axios from 'axios'

const api = axios.create({
  baseURL: '/api/excel',
  timeout: 120000
})

function handleError(error) {
  if (error.response && error.response.data) {
    const data = error.response.data
    throw new Error(data.message || `请求失败: ${error.response.status}`)
  }
  throw new Error(error.message || '网络异常')
}

function wrap(promise) {
  return promise.then(res => res.data).catch(handleError)
}

function download(url, filename) {
  return api.get(url, { responseType: 'blob' }).then(res => {
    const blob = new Blob([res.data], { type: res.headers['content-type'] })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    link.click()
    URL.revokeObjectURL(link.href)
  }).catch(handleError)
}

export default {
  generate: (count) => wrap(api.post('/generate', null, { params: { count } })),
  exportInMemory: () => download('/export/in-memory', 'orders-in-memory.xlsx'),
  exportStream: () => download('/export/stream', 'orders-stream.xlsx'),
  submitAsync: (totalRows) => wrap(api.post('/export/async', null, { params: { totalRows } })),
  getAsyncStatus: (taskId) => wrap(api.get(`/export/async/${taskId}/status`)),
  downloadAsync: (taskId) => download(`/export/async/${taskId}/download`, 'orders-async.xlsx'),
  explain: () => wrap(api.get('/explain'))
}
