<template>
  <a-select
    :value="value ?? undefined"
    :mode="multiple ? 'multiple' : undefined"
    show-search
    allow-clear
    :placeholder="placeholder"
    :filter-option="false"
    :dropdown-match-select-width="300"
    :options="options"
    :loading="loading"
    :not-found-content="loading ? $t('common.msg.loading') : $t('hr.employeeSelect.notFound')"
    @search="onSearch"
    @focus="onFocus"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { t } from '@/locales'
import { employeeOptions } from '@/api/hr'

/**
 * 员工下拉：按工号或姓名远程搜索在职员工
 * - v-model:value：员工 ID（multiple 时为 ID 数组）
 * - label：单选时当前值的显示名
 */
const props = defineProps({
  value: [Number, Array],
  label: String,
  multiple: Boolean,
  placeholder: { type: String, default: () => t('hr.employeeSelect.placeholder') }
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
let seq = 0
let timer

const options = computed(() => {
  const opts = list.value.map((e) => ({ value: e.id, label: `${e.empNo} ${e.name}${e.deptName ? ` · ${e.deptName}` : ''}` }))
  if (!props.multiple && props.value != null && !opts.some((o) => o.value === props.value)) {
    opts.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return opts
})

async function load(keyword) {
  const current = ++seq
  loading.value = true
  try {
    const res = await employeeOptions(keyword || undefined)
    if (current === seq) list.value = res
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
</script>
