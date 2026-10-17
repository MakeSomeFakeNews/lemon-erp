<template>
  <a-config-provider :locale="antdLocale" :theme="themeConfig">
    <a-app>
      <router-view />
      <PrintPreview />
    </a-app>
  </a-config-provider>
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'
import { isZh } from '@/locales'
import { useAppStore } from '@/store/app'
import PrintPreview from '@/components/print/PrintPreview.vue'

// Ant Design 组件和日期按界面语言（切换语言会刷新页面）
const antdLocale = isZh() ? zhCN : enUS
dayjs.locale(isZh() ? 'zh-cn' : 'en')

const appStore = useAppStore()

const themeConfig = computed(() => ({
  algorithm: appStore.dark ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6
  }
}))

// 同步到 html，供自定义 CSS 变量切换
watchEffect(() => {
  document.documentElement.dataset.theme = appStore.dark ? 'dark' : 'light'
})
</script>
