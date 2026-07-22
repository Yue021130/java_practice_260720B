import axios from 'axios'

const api = axios.create({
  timeout: 120000
})

/**
 * 运行一个实验场景
 * @param {string} endpoint 后端接口路径，如 /api/locks/transfer-deadlock
 * @param {object} params 查询参数，全部走 query string
 * @returns {Promise<object>} 后端统一返回的 data 部分
 */
export async function runScenario(endpoint, params = {}) {
  const res = await api.post(endpoint, null, { params })
  return res.data
}

export default api
