import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  // H5 调试（npm run dev:h5）时 VITE_API_BASE 可设为 /api，由开发服务器代理到后端，避免跨域
  server: {
    proxy: {
      '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true }
    }
  },
  resolve: {
    alias: {
      // 与 Web 端共用：后端枚举生成的常量（constants/enums.js）、字典英文译文（locales/en-US/dict.js）
      '@web': fileURLToPath(new URL('../lemon-web/src', import.meta.url))
    }
  }
})
