<script setup>
import { watchEffect } from 'vue'
import { theme } from 'ant-design-vue'

/**
 * 把 Ant Design 按主题色、深浅色派生出的 token 写成 html 上的 CSS 变量，供自定义样式使用（放在 a-config-provider 内）：
 * - --app-primary / --app-primary-bg / --app-primary-hover / --app-primary-border：项目自定义样式的强调色
 * - --ant-color-*：页面中 var(--ant-color-primary, #1677ff) 等写法使用（Ant Design Vue 4 本身不输出 CSS 变量）
 * 需要半透明的主色时用 color-mix(in srgb, var(--app-primary) 20%, transparent)，不要写死 rgba(22, 119, 255, …)
 */
const { token } = theme.useToken()

const VARS = {
  '--app-primary': 'colorPrimary',
  '--app-primary-bg': 'colorPrimaryBg',
  '--app-primary-hover': 'colorPrimaryHover',
  '--app-primary-border': 'colorPrimaryBorder',
  '--ant-color-primary': 'colorPrimary',
  '--ant-color-primary-hover': 'colorPrimaryHover',
  '--ant-color-primary-active': 'colorPrimaryActive',
  '--ant-color-primary-bg': 'colorPrimaryBg',
  '--ant-color-primary-bg-hover': 'colorPrimaryBgHover',
  '--ant-color-primary-border': 'colorPrimaryBorder',
  '--ant-color-primary-text': 'colorPrimaryText',
  '--ant-color-success': 'colorSuccess',
  '--ant-color-warning': 'colorWarning',
  '--ant-color-error': 'colorError',
  '--ant-color-info': 'colorInfo',
  '--ant-color-text-tertiary': 'colorTextTertiary'
}

watchEffect(() => {
  const style = document.documentElement.style
  for (const [name, key] of Object.entries(VARS)) style.setProperty(name, token.value[key])
})
</script>

<template>
  <slot />
</template>
