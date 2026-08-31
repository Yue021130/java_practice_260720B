import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置：开发服务器端口 5197，代理到后端 8104。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5197,
    proxy: {
      '/api': {
        target: 'http://localhost:8104',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
})
