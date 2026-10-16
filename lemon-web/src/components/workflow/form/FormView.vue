<template>
  <a-descriptions v-if="items.length" :column="1" bordered size="small" class="form-view">
    <a-descriptions-item v-for="item in items" :key="item.key" :label="item.label">
      <span :class="{ multiline: item.multiline }">{{ display(item.value) }}</span>
    </a-descriptions-item>
  </a-descriptions>
  <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="无表单数据" />
</template>

<script setup>
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'

/**
 * 只读展示表单数据；没有表单定义时（业务单据发起的流程）直接展示变量
 */
const props = defineProps({
  fields: { type: Array, default: () => [] },
  data: { type: Object, default: () => ({}) }
})

const items = computed(() => {
  if (props.fields.length) {
    return props.fields.map((f) => ({
      key: f.field,
      label: f.label,
      value: props.data?.[f.field],
      multiline: f.type === 'TEXTAREA'
    }))
  }
  return Object.entries(props.data || {}).map(([k, v]) => ({ key: k, label: k, value: v }))
})

const display = (v) => (v === null || v === undefined || v === '' ? '-' : typeof v === 'object' ? JSON.stringify(v) : v)
</script>

<style scoped>
.form-view :deep(.ant-descriptions-item-label) {
  width: 140px;
}
.multiline {
  white-space: pre-wrap;
}
</style>
