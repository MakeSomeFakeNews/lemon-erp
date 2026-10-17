<template>
  <a-select
    :value="value"
    show-search
    allow-clear
    :placeholder="placeholder"
    :filter-option="false"
    :options="options"
    :loading="loading"
    :not-found-content="loading ? $t('common.msg.loading') : $t('components.user.noMatch')"
    @search="onSearch"
    @focus="onFocus"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, ref } from 'vue'
import { pageUser } from '@/api/system/user'
import { t } from '@/locales'

/**
 * 用户单选下拉：按用户名或昵称远程搜索
 * - v-model:value：用户 ID
 * - label：当前值的显示名（编辑时回显，避免为了显示名称额外请求）
 * - fetcher(params)：返回 { records, total }，默认用户管理的分页接口
 */
const props = defineProps({
  value: Number,
  label: String,
  placeholder: { type: String, default: () => t('components.user.searchPlaceholder') },
  fetcher: { type: Function, default: pageUser }
})
const emit = defineEmits(['update:value'])

const users = ref([])
const loading = ref(false)
let seq = 0
let timer

const toOption = (u) => ({ value: u.id, label: u.nickname ? `${u.nickname}（${u.username}）` : u.username })

// 当前值不在搜索结果中时，仍用 label 回显
const options = computed(() => {
  const list = users.value.map(toOption)
  if (props.value != null && !list.some((o) => o.value === props.value)) {
    list.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return list
})

async function load(username) {
  const current = ++seq
  loading.value = true
  try {
    const res = await props.fetcher({ current: 1, size: 20, username: username || undefined })
    if (current === seq) users.value = res.records
  } finally {
    if (current === seq) loading.value = false
  }
}

function onSearch(keyword) {
  clearTimeout(timer)
  timer = setTimeout(() => load(keyword), 300)
}

function onFocus() {
  if (!users.value.length) load()
}
</script>
