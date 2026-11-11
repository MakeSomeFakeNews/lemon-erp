<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder || $t(type === 'supplier' ? 'scm.partnerSelect.searchSupplier' : 'scm.partnerSelect.searchCustomer')"
    :filter-option="false"
    :dropdown-match-select-width="360"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    @search="onSearch"
    @focus="onFocus"
    @change="onChange"
  >
    <template v-if="carrier && !marked" #dropdownRender="{ menuNode }">
      <div class="carrier-hint">{{ $t('scm.freight.carrierUnmarked') }}</div>
      <component :is="menuNode" />
    </template>
  </a-select>
</template>

<script setup>
import { computed, ref } from 'vue'
import { carrierOptions, partnerOptions } from '@/api/scm/partner'

/**
 * 供应商 / 客户下拉：按编码或名称远程搜索启用的往来单位。
 * - type：supplier | customer
 * - v-model:value：ID；label：当前值的显示名（编辑时回显）
 * - @select(partner)：选中后返回 { id, code, name, contact, phone, address }
 * - carrier：承运商（采购运费、发运单），只列出标记为物流公司的供应商；还没有标记时列出全部供应商并提示去标记
 */
const props = defineProps({
  type: { type: String, required: true },
  value: Number,
  label: String,
  placeholder: String,
  allowClear: Boolean,
  disabled: Boolean,
  carrier: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const list = ref([])
/** 承运商：是否已有标记为物流公司的供应商 */
const marked = ref(true)
const loading = ref(false)
let seq = 0
let timer

const options = computed(() => {
  const opts = list.value.map((p) => ({ value: p.id, label: `${p.code} ${p.name}` }))
  if (props.value != null && !opts.some((o) => o.value === props.value)) {
    opts.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return opts
})

async function load(keyword) {
  const current = ++seq
  loading.value = true
  try {
    if (props.carrier) {
      const res = await carrierOptions(keyword || undefined)
      if (current === seq) {
        list.value = res.options
        marked.value = res.marked
      }
    } else {
      const res = await partnerOptions(props.type, keyword || undefined)
      if (current === seq) list.value = res
    }
  } finally {
    if (current === seq) loading.value = false
  }
}

function onSearch(keyword) {
  clearTimeout(timer)
  timer = setTimeout(() => load(keyword), 300)
}

function onFocus() {
  if (!list.value.length) load()
}

function onChange(id) {
  emit('update:value', id ?? null)
  const partner = list.value.find((p) => p.id === id)
  if (partner) emit('select', partner)
}
</script>

<style scoped>
.carrier-hint {
  padding: 4px 12px 8px;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: normal;
}
</style>
