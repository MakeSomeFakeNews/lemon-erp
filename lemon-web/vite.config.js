import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 后端 context-path 为 /api，直接转发
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 附带 X-Forwarded-For，后端据此记录真实客户端 IP（生产环境由 Nginx 等代理负责）
        xfwd: true
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        // 只把明确指定的模块放进手动 chunk，避免 antd 把全量图标库（MenuIcon/IconPicker 异步加载）连带打进首屏
        onlyExplicitManualChunks: true,
        // 第三方库拆分为独立 chunk，便于浏览器长期缓存
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          // 图标基础组件及其依赖的颜色库单独成 chunk：放进 antd 会与图标 chunk 循环引用，生产环境加载时 blue 未初始化
          if (/node_modules\/(@ant-design\/colors|@ctrl\/tinycolor)\//.test(id) ||
            /@ant-design\/icons-vue\/es\/(components|utils|dynamicCSS|insert-css)/.test(id)) return 'icons-core'
          if (id.includes('@ant-design/icons-vue') || id.includes('@ant-design/icons-svg')) return
          if (id.includes('ant-design-vue') || id.includes('@ant-design') || id.includes('dayjs')) return 'antd'
          if (/node_modules\/(vue|@vue|vue-router|pinia)\//.test(id)) return 'vue'
          // 图表库只在商务智能等页面使用，单独成 chunk，不进入首屏
          if (id.includes('node_modules/echarts') || id.includes('node_modules/zrender')) return 'echarts'
          return 'vendor'
        }
      }
    }
  }
})
