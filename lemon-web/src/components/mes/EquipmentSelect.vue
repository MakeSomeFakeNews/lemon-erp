<template>
  <RemoteSelect
    :value="value"
    :label="label"
    :fetch="({ keyword, current, size }) => equipmentOptions({ keyword, current, size })"
    :fetch-by-ids="equipmentsByIds"
    :to-label="(e) => `${e.code} ${e.name}`"
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :pinned="pinned"
    @update:value="(v) => emit('update:value', v)"
    @select="(e) => emit('select', e)"
  />
</template>

<script setup>
import RemoteSelect from '@/components/common/RemoteSelect.vue'
import { equipmentOptions, equipmentsByIds } from '@/api/mes/base'

/**
 * 设备下拉：未报废的设备按编码或名称远程分页搜索
 * - label：当前值的显示名（不传时按 ID 取名称，已报废的设备也能回显）
 * - pinned：没有输入关键字时排在最前面的设备（如工序所在工作中心的设备）
 */
defineProps({
  value: Number,
  label: String,
  placeholder: String,
  allowClear: { type: Boolean, default: true },
  pinned: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:value', 'select'])
</script>
