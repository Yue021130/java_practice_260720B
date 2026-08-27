import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5192,
    proxy: {
      '/api': {
        target: 'http://localhost:8099',
        changeOrigin: true
      }
    }
  }
})
