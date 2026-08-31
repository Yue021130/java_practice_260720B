import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export default {
  createOrder(userId, amount) {
    return instance.post('/order/create', null, {
      params: { userId, amount }
    }).then(r => r.data)
  },

  payOrder(orderNo) {
    return instance.post('/order/pay', null, {
      params: { orderNo }
    }).then(r => r.data)
  },

  payOrderSync(orderNo) {
    return instance.post('/order/pay-sync', null, {
      params: { orderNo }
    }).then(r => r.data)
  },

  notifyLogs(orderNo) {
    return instance.get('/order/notify-logs', {
      params: { orderNo }
    }).then(r => r.data)
  },

  explain() {
    return instance.get('/order/explain').then(r => r.data)
  }
}
