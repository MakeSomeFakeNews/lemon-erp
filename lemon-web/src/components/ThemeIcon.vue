<template>
  <span role="img" class="anticon theme-icon" :aria-label="dark ? $t('components.theme.light') : $t('components.theme.dark')">
    <!-- 暗色模式下显示太阳（点击切到亮色），亮色模式下显示月亮 -->
    <svg v-if="dark" viewBox="0 0 24 24" width="1em" height="1em" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
      <circle cx="12" cy="12" r="4.5" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
    <svg v-else viewBox="0 0 24 24" width="1em" height="1em" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round">
      <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
    </svg>
  </span>
</template>

<script setup>
defineProps({ dark: Boolean })
</script>
