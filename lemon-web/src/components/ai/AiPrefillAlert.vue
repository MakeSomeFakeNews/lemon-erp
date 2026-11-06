<template>
  <a-alert v-if="ai.source" :type="ai.warnings.length ? 'warning' : 'info'" show-icon closable class="ai-prefill-alert" @close="ai.clear()">
    <template #message>
      {{ ai.warnings.length ? $t('ai.extract.prefillWarn', { n: ai.warnings.length }) : $t('ai.extract.prefillOk') }}
    </template>
    <template v-if="ai.warnings.length" #description>
      <ul class="ai-prefill-list">
        <li v-for="(w, i) in ai.warnings" :key="i">{{ w.message }}</li>
      </ul>
    </template>
  </a-alert>
</template>

<script setup>
/**
 * 表单顶部的智能录入提示：识别结果已填入，列出需要核对的问题（名称匹配不上、数值可疑等）。
 * ai 为 useAiPrefill() 的返回值；有提示的字段由页面用 ai.cls(字段) 加上 ai-warn 样式
 */
defineProps({
  ai: { type: Object, required: true }
})
</script>

<style scoped>
.ai-prefill-alert {
  margin-bottom: 16px;
}
.ai-prefill-list {
  margin: 0;
  padding-left: 18px;
}
</style>

<style>
/* 有识别提示的字段：黄色边框（非 scoped，作用于表单中的输入组件） */
.ai-warn .ant-input,
.ai-warn .ant-input-number,
.ai-warn .ant-picker,
.ai-warn .ant-select:not(.ant-select-customize-input) .ant-select-selector {
  border-color: #faad14 !important;
}
.ai-warn-text {
  color: #d48806;
  font-size: 12px;
}
html[data-theme='dark'] .ai-warn-text {
  color: #d89614;
}
</style>
