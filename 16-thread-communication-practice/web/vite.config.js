import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5189,
    proxy: {
      '/api': {
        target: 'http://localhost:8096',
        changeOrigin: true
      }
    }
  }
})
