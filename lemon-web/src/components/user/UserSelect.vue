<template>
  <RemoteSelect
    :value="value"
    :label="label"
    :fetch="({ keyword, current, size }) => fetcher({ current, size, username: keyword })"
    :to-label="toLabel"
    :placeholder="placeholder"
    :not-found="$t('components.user.noMatch')"
    @update:value="(v) => emit('update:value', v)"
  />
</template>

<script setup>
import { pageUser } from '@/api/system/user'
import { t } from '@/locales'
import RemoteSelect from '@/components/common/RemoteSelect.vue'

/**
 * 用户单选下拉：按用户名或昵称远程分页搜索（每页 20 条，滚动加载更多），选项显示昵称、部门和用户名
 * - v-model:value：用户 ID
 * - label：当前值的显示名（编辑时回显，避免为了显示名称额外请求）
 * - fetcher(params)：返回 { records, total }，默认用户管理的分页接口
 */
defineProps({
  value: Number,
  label: String,
  placeholder: { type: String, default: () => t('components.user.searchPlaceholder') },
  fetcher: { type: Function, default: pageUser }
})
const emit = defineEmits(['update:value'])

/** 显示「昵称 · 部门（用户名）」，同名的人可按部门、用户名区分 */
const toLabel = (u) => {
  if (u.deptName) return `${u.nickname || u.username} · ${u.deptName}（${u.username}）`
  return u.nickname ? `${u.nickname}（${u.username}）` : u.username
}
</script>
