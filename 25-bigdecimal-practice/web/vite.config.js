import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置：开发服务器端口 5198，代理到后端 8105。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5198,
    proxy: {
      '/api': {
        target: 'http://localhost:8105',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
