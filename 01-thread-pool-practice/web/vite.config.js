import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 开发服务器端口 5174，/api 请求代理到后端 8081，避免跨域
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
