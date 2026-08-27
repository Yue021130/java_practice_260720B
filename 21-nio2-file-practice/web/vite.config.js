import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置：开发服务器端口 5194，代理到后端 8101 避免跨域。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5194,
    proxy: {
      '/api': {
        target: 'http://localhost:8101',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
