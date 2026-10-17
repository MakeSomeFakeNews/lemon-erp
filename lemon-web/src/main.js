import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import { i18n } from './locales'
import router from './router'
import perm from './directives/perm'
import { isHandledError } from './utils/request'
import './style.css'

const app = createApp(App)
app.use(i18n)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.directive('perm', perm)

// 接口错误已由请求拦截器提示，这里只记录其他未预期的错误
app.config.errorHandler = (err, instance, info) => {
  if (!isHandledError(err)) console.error(err, info)
}
window.addEventListener('unhandledrejection', (e) => {
  if (isHandledError(e.reason)) e.preventDefault()
})
app.mount('#app')
