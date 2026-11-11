<template>
  <a-config-provider :locale="antdLocale" :theme="themeConfig">
    <ThemeVars>
      <a-app>
        <router-view />
        <PrintPreview />
      </a-app>
    </ThemeVars>
  </a-config-provider>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, watchEffect } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'
import { isZh } from '@/locales'
import { useAppStore } from '@/store/app'
import PrintPreview from '@/components/print/PrintPreview.vue'
import ThemeVars from '@/components/ThemeVars.vue'

// Ant Design 组件和日期按界面语言（切换语言会刷新页面）
const antdLocale = isZh() ? zhCN : enUS
dayjs.locale(isZh() ? 'zh-cn' : 'en')

const appStore = useAppStore()

// 界面设置：深浅色、紧凑模式、主题色（派生色由 Ant Design 算法生成，ThemeVars 同步为 CSS 变量）
const themeConfig = computed(() => ({
  algorithm: [appStore.dark ? theme.darkAlgorithm : theme.defaultAlgorithm, ...(appStore.settings.compact ? [theme.compactAlgorithm] : [])],
  token: {
    colorPrimary: appStore.settings.primaryColor,
    // 链接色由 colorInfo 派生，一并跟随主题色（链接按钮、表格中的操作链接）
    colorInfo: appStore.settings.primaryColor,
    borderRadius: 6
  }
}))

// 主题模式为「跟随系统」时随系统切换；页面关闭前保存还没来得及保存的设置
const darkMedia = window.matchMedia?.('(prefers-color-scheme: dark)')
const onSystemTheme = (e) => (appStore.systemDark = e.matches)
const onPageHide = () => appStore.flushSettingsOnUnload()
onMounted(() => {
  darkMedia?.addEventListener('change', onSystemTheme)
  window.addEventListener('pagehide', onPageHide)
})
onBeforeUnmount(() => {
  darkMedia?.removeEventListener('change', onSystemTheme)
  window.removeEventListener('pagehide', onPageHide)
})

// 同步到 html，供自定义 CSS 变量切换
watchEffect(() => {
  document.documentElement.dataset.theme = appStore.dark ? 'dark' : 'light'
})
</script>
