import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export default {
  calculate(data) {
    return instance.post('/amount/calculate', data).then(r => r.data)
  },

  split(params) {
    return instance.get('/amount/split', { params }).then(r => r.data)
  },

  pitfalls() {
    return instance.get('/amount/pitfalls').then(r => r.data)
  },

  explain() {
    return instance.get('/amount/explain').then(r => r.data)
  }
}
