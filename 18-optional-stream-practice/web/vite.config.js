import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5191,
    proxy: {
      '/api': {
        target: 'http://localhost:8098',
        changeOrigin: true
      }
    }
  }
})
