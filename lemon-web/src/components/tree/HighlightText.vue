<template>
  <span>
    <template v-for="(part, i) in parts" :key="i">
      <mark v-if="part.hit" class="highlight">{{ part.text }}</mark>
      <template v-else>{{ part.text }}</template>
    </template>
  </span>
</template>

<script setup>
import { computed } from 'vue'

/** 高亮文本中命中的关键字（不区分大小写） */
const props = defineProps({
  text: { type: String, default: '' },
  keyword: { type: String, default: '' }
})

const parts = computed(() => {
  const kw = props.keyword.trim()
  if (!kw) return [{ text: props.text, hit: false }]
  const result = []
  const lower = props.text.toLowerCase()
  const target = kw.toLowerCase()
  let start = 0
  for (let i = lower.indexOf(target); i >= 0; i = lower.indexOf(target, start)) {
    if (i > start) result.push({ text: props.text.slice(start, i), hit: false })
    result.push({ text: props.text.slice(i, i + kw.length), hit: true })
    start = i + kw.length
  }
  if (start < props.text.length) result.push({ text: props.text.slice(start), hit: false })
  return result
})
</script>

<style scoped>
.highlight {
  padding: 0;
  color: var(--app-primary);
  background: transparent;
  font-weight: 600;
}
</style>
