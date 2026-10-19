import { createSSRApp } from 'vue'
import App from './App.vue'
import { t } from '@/locales'

export function createApp() {
  const app = createSSRApp(App)
  // 模板中可直接用 $t；setup 中用 useI18n().t
  app.config.globalProperties.$t = t
  return { app }
}
