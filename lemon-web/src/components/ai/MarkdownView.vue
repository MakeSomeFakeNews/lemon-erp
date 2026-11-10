<template>
  <div class="markdown-view" v-html="html"></div>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

/** 显示 Markdown（模型的回答、通知公告正文等；marked 解析后经 DOMPurify 清理，见 utils/markdown.js） */
const props = defineProps({ content: { type: String, default: '' } })
const html = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.markdown-view {
  line-height: 1.8;
  word-break: break-word;
}
.markdown-view :deep(h1),
.markdown-view :deep(h2) {
  margin: 20px 0 10px;
  padding-bottom: 6px;
  font-size: 18px;
  border-bottom: 1px solid var(--app-border);
}
.markdown-view :deep(h3) {
  margin: 16px 0 8px;
  font-size: 15px;
}
.markdown-view :deep(h2:first-child),
.markdown-view :deep(h3:first-child) {
  margin-top: 0;
}
.markdown-view :deep(p) {
  margin: 0 0 8px;
}
.markdown-view :deep(ul),
.markdown-view :deep(ol) {
  padding-left: 22px;
  margin: 0 0 10px;
}
.markdown-view :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  display: block;
  overflow-x: auto;
}
.markdown-view :deep(th),
.markdown-view :deep(td) {
  border: 1px solid var(--app-border);
  padding: 4px 10px;
}
.markdown-view :deep(th) {
  background: var(--app-hover);
}
.markdown-view :deep(code) {
  padding: 1px 4px;
  background: var(--app-code-bg);
  border-radius: 4px;
  font-size: 13px;
}
.markdown-view :deep(blockquote) {
  margin: 0 0 8px;
  padding-left: 10px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-2);
}
</style>
