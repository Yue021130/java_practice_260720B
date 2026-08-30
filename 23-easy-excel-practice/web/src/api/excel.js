import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000
})

/**
 * 通用下载：后端返回二进制流，前端生成 a 标签触发下载。
 */
function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default {
  /**
   * Easypoi 基础导入（无校验）。
   */
  importBasic(file) {
    const form = new FormData()
    form.append('file', file)
    return instance.post('/excel/easypoi/import/basic', form).then(r => r.data)
  },

  /**
   * Easypoi 带校验导入：成功落库，失败回写错误日志。
   */
  importVerify(file) {
    const form = new FormData()
    form.append('file', file)
    return instance.post('/excel/easypoi/import/verify', form).then(r => r.data)
  },

  /**
   * Easypoi Map 方式导入。
   */
  importMap(file) {
    const form = new FormData()
    form.append('file', file)
    return instance.post('/excel/easypoi/import/map', form).then(r => r.data)
  },

  /**
   * Easypoi 组内重复校验导入（ThreadLocal）。
   */
  importDuplicate(file) {
    const form = new FormData()
    form.append('file', file)
    return instance.post('/excel/easypoi/import/duplicate', form).then(r => r.data)
  },

  /**
   * 下载 Easypoi 导入模板。
   */
  downloadTemplate() {
    return instance.get('/excel/easypoi/template', {
      responseType: 'blob'
    }).then(r => {
      downloadBlob(r.data, 'sys-user-template.xlsx')
    })
  },

  /**
   * 初始化商品数据（EasyExcel 导出用）。
   */
  initProducts() {
    return instance.post('/excel/easyexcel/init').then(r => r.data)
  },

  /**
   * EasyExcel 导出商品。
   */
  exportProducts() {
    return instance.get('/excel/easyexcel/export', {
      responseType: 'blob'
    }).then(r => {
      const filename = r.headers['content-disposition']
        ? decodeURIComponent(r.headers['content-disposition'].split('filename=')[1])
        : 'products-easyexcel.xlsx'
      downloadBlob(r.data, filename)
    })
  },

  /**
   * 八股速记。
   */
  explain() {
    return instance.get('/excel/explain').then(r => r.data)
  }
}
