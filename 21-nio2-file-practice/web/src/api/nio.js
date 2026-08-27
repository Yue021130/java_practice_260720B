import axios from 'axios'

/**
 * axios 实例：统一前缀 /api/nio。
 * 开发时由 vite.config.js 代理到后端 8101；生产环境需 Nginx/Caddy 反向代理。
 */
const api = axios.create({
  baseURL: '/api/nio',
  timeout: 30000
})

/**
 * 统一错误处理：把后端统一响应体的 message 抛给调用方。
 */
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

export default {
  // Path 四兄弟
  resolve: (base, other) => wrap(api.get('/path/resolve', { params: { base, other } })),
  resolveSibling: (path, sibling) => wrap(api.get('/path/resolveSibling', { params: { path, sibling } })),
  relativize: (from, to) => wrap(api.get('/path/relativize', { params: { from, to } })),
  normalize: (path) => wrap(api.get('/path/normalize', { params: { path } })),
  toAndFromFile: (path) => wrap(api.get('/path/to-and-from-file', { params: { path } })),

  // 创建与删除
  create: (body) => wrap(api.post('/file/create', body)),
  delete: (body) => wrap(api.post('/file/delete', body)),

  // 读写
  write: (body) => wrap(api.post('/file/write', body)),
  read: (path) => wrap(api.get('/file/read', { params: { path } })),
  lines: (path) => wrap(api.get('/file/lines', { params: { path } })),
  lineStats: (path) => wrap(api.get('/file/lineStats', { params: { path } })),
  writeLines: (body) => wrap(api.post('/file/writeLines', body)),
  readLinesBuffered: (path) => wrap(api.get('/file/readLinesBuffered', { params: { path } })),
  writeProperties: (body) => wrap(api.post('/file/writeProperties', body)),
  readProperties: (path) => wrap(api.get('/file/readProperties', { params: { path } })),

  // 复制与移动
  copy: (body) => wrap(api.post('/file/copy', body)),
  copyDirectory: (body) => wrap(api.post('/file/copyDirectory', body)),
  move: (body) => wrap(api.post('/file/move', body)),
  upload: (file, dst) => {
    const form = new FormData()
    form.append('file', file)
    form.append('dst', dst)
    return wrap(api.post('/file/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }))
  },

  // 遍历
  list: (dir) => wrap(api.get('/file/list', { params: { dir } })),
  walk: (dir, maxDepth) => wrap(api.get('/file/walk', { params: { dir, maxDepth } })),
  stats: (dir) => wrap(api.get('/file/stats', { params: { dir } })),

  // 属性
  properties: (path) => wrap(api.get('/file/properties', { params: { path } })),

  // 八股速记
  explain: () => wrap(api.get('/explain'))
}
