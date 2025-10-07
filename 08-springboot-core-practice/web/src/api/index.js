import axios from 'axios'

const api = axios.create({
  timeout: 120000
})

export async function runScenario(endpoint, params = {}) {
  const res = await api.post(endpoint, null, { params })
  return res.data
}

export default api
