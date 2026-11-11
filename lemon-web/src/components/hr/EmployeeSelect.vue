<template>
  <RemoteSelect
    :value="value"
    :label="label"
    :multiple="multiple"
    :fetch="({ keyword, current, size }) => employeeOptions(keyword, { current, size })"
    :fetch-by-ids="employeesByIds"
    :to-label="(e) => `${e.empNo} ${e.name}${e.deptName ? ` · ${e.deptName}` : ''}`"
    :placeholder="placeholder"
    :not-found="$t('hr.employeeSelect.notFound')"
    @update:value="(v) => emit('update:value', v)"
  />
</template>

<script setup>
import { t } from '@/locales'
import RemoteSelect from '@/components/common/RemoteSelect.vue'
import { employeeOptions, employeesByIds } from '@/api/hr'

/**
 * 员工下拉：按工号或姓名远程分页搜索在职员工
 * - v-model:value：员工 ID（multiple 时为 ID 数组）
 * - label：单选时当前值的显示名（不传时按 ID 取名称；多选按 ID 取）
 */
defineProps({
  value: [Number, Array],
  label: String,
  multiple: Boolean,
  placeholder: { type: String, default: () => t('hr.employeeSelect.placeholder') }
})
const emit = defineEmits(['update:value'])
</script>
