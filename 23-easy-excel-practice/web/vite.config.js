import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置：开发服务器端口 5196，代理到后端 8103。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5196,
    proxy: {
      '/api': {
        target: 'http://localhost:8103',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
