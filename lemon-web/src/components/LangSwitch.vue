<template>
  <a-dropdown placement="bottomRight" :trigger="['click']">
    <a-tooltip :title="$t('layout.language')" placement="bottom">
      <a-button type="text" :class="btnClass">
        <template #icon><TranslationOutlined /></template>
        <span v-if="showLabel" class="lang-label">{{ current.label }}</span>
      </a-button>
    </a-tooltip>
    <template #overlay>
      <a-menu :selected-keys="[current.value]" @click="({ key }) => key !== current.value && setLocale(key)">
        <a-menu-item v-for="l in LOCALES" :key="l.value">{{ l.label }}</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { TranslationOutlined } from '@ant-design/icons-vue'
import { LOCALES, currentLocale, setLocale } from '@/locales'

/** 切换界面语言（保存后刷新页面） */
defineProps({
  btnClass: String,
  /** 显示当前语言名称 */
  showLabel: Boolean
})
const current = computed(() => LOCALES.find((l) => l.value === currentLocale()) || LOCALES[0])
</script>

<style scoped>
.lang-label {
  margin-left: 4px;
}
</style>
