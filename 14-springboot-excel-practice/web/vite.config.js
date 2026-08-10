import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5187,
    proxy: {
      '/api': {
        target: 'http://localhost:8094',
        changeOrigin: true
      }
    }
  }
})
