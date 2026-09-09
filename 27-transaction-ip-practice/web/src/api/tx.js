import request from './request.js'

export function getTxScenes() {
  return request.get('/tx/scenes')
}
