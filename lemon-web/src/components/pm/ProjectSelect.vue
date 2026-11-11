<template>
  <RemoteSelect
    :value="value"
    :label="label"
    :fetch="({ keyword, current, size }) => projectOptions(mine, keyword, { current, size })"
    :fetch-by-ids="projectsByIds"
    :to-label="(p) => `${p.code} ${p.name}`"
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :dropdown-width="360"
    @update:value="(v) => emit('update:value', v)"
    @select="(p) => emit('select', p)"
  />
</template>

<script setup>
import { projectOptions, projectsByIds } from '@/api/pm'
import { t } from '@/locales'
import RemoteSelect from '@/components/common/RemoteSelect.vue'

/**
 * 项目下拉：未完成的项目按编码或名称远程分页搜索；mine 只列出我参与的项目（填报工时）
 * - label：当前值的显示名（不传时按 ID 取名称，已结束的项目也能回显）
 * - @select(project)：选中后返回项目
 */
defineProps({
  value: Number,
  label: String,
  mine: Boolean,
  allowClear: { type: Boolean, default: true },
  placeholder: { type: String, default: () => t('pm.common.selectProject') }
})
const emit = defineEmits(['update:value', 'select'])
</script>
