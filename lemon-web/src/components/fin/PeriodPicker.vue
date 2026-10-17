<template>
  <a-date-picker
    picker="month"
    :value="periodValue(value)"
    value-format="YYYY-MM"
    :allow-clear="false"
    :placeholder="placeholder"
    @change="(v) => emit('update:value', v ? toPeriod(v) : undefined)"
  />
</template>

<script setup>
import { t } from '@/locales'
import { periodValue, toPeriod } from './fin'

/** 会计期间选择：v-model 为 yyyyMM 数字 */
defineProps({
  value: Number,
  placeholder: { type: String, default: () => t('fin.periodPicker.placeholder') }
})
const emit = defineEmits(['update:value'])
</script>
