import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd, { Select, TreeSelect } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import { i18n } from './locales'
import router from './router'
import perm from './directives/perm'
import { isHandledError } from './utils/request'
import { installEllipsisTitle } from './utils/ellipsisTitle'
import './style.css'

// 下拉框默认按内容宽度展开（不窄于输入框），避免放在表格窄列中时选项被省略；过长的选项在 style.css 中换行
for (const comp of [Select, TreeSelect]) {
  comp.props.dropdownMatchSelectWidth = { type: [Boolean, Number], default: false }
}

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
installEllipsisTitle()
app.mount('#app')
