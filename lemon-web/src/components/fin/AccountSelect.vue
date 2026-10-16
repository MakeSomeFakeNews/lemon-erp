<template>
  <a-select
    :value="value ?? undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    option-filter-prop="label"
    :dropdown-match-select-width="false"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { accountOptions, allAccounts } from '@/api/fin'
import { cached } from './fin'

/**
 * 科目选择：默认只能选启用的末级科目（凭证、结算账户、折旧科目）；all 为 true 时可选上级科目（账簿、预算）。
 * prefix：只列出编码以其中之一开头的科目，如 ['1001', '1002'] 限定为现金、银行
 */
const props = defineProps({
  value: Number,
  all: Boolean,
  prefix: Array,
  placeholder: { type: String, default: '搜索科目编码或名称' },
  allowClear: Boolean,
  disabled: Boolean
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() =>
  list.value
    .filter((a) => !props.prefix || props.prefix.some((p) => a.code.startsWith(p)))
    .map((a) => ({ value: a.id, label: `${a.code} ${props.all ? '　'.repeat((a.code.length - 4) / 2) : ''}${a.name}` }))
)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await (props.all ? cached('accounts-all', allAccounts) : cached('accounts-leaf', accountOptions))
  } finally {
    loading.value = false
  }
})
</script>
