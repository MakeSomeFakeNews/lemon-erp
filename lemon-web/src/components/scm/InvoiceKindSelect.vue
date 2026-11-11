<template>
  <a-select
    :value="value || undefined"
    allow-clear
    :options="options"
    :disabled="disabled"
    :placeholder="$t('scm.taxRate.invoiceKindPlaceholder')"
    @change="onChange"
  >
    <template #option="{ label, rate }">
      <div class="kind-option">
        <span>{{ label }}</span>
        <span v-if="rate != null" class="rate">{{ rate }}%</span>
      </div>
    </template>
  </a-select>
</template>

<script setup>
import { computed } from 'vue'
import { kindRate, useTaxRates } from './taxRate'

/**
 * 发票类型（业务字典 scm_invoice_kind，扩展值为税率）：选择后触发 change(kind, rate)，由表单把明细税率统一调整为 rate；
 * 清空时 rate 为 undefined（明细税率不变）。当前值已停用时仍显示
 */
const props = defineProps({
  value: { type: String, default: undefined },
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'change'])

const tax = useTaxRates()
const options = computed(() => {
  const list = tax.kinds.value.map((d) => ({ value: d.value, label: d.label, rate: kindRate(d) }))
  if (props.value && !list.some((o) => o.value === props.value)) list.push({ value: props.value, label: props.value })
  return list
})

function onChange(v) {
  emit('update:value', v || null)
  emit('change', v || null, v ? tax.rateOf(v) : undefined)
}
</script>

<style scoped>
.kind-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.rate {
  color: var(--app-text-3);
}
</style>
