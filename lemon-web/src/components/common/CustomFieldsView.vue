<template>
  <a-descriptions v-if="fields.length" :column="{ xs: 1, sm: 2, lg: 3 }" size="small" :title="title">
    <a-descriptions-item v-for="f in fields" :key="f.fieldKey" :label="f.label">{{ displayValue(f, values[f.fieldKey]) || '-' }}</a-descriptions-item>
  </a-descriptions>
</template>

<script setup>
import { watch } from 'vue'
import { displayValue, useCustomFields } from '@/composables/useCustomFields'

/** 详情中只读显示自定义字段 */
const props = defineProps({ bizType: { type: String, required: true }, bizId: [Number, String], title: { type: String, default: '扩展信息' } })
const { fields, values, load } = useCustomFields(props.bizType)
watch(
  () => props.bizId,
  (id) => load(id),
  { immediate: true }
)
defineExpose({ reload: () => load(props.bizId) })
</script>
