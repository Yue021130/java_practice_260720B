import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置：开发服务器端口 5195，代理到后端 8102。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5195,
    proxy: {
      '/api': {
        target: 'http://localhost:8102',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
