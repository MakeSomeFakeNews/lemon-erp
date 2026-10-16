<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <a-app>
      <router-view />
    </a-app>
  </a-config-provider>
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { useAppStore } from '@/store/app'

dayjs.locale('zh-cn')

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
