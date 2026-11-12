<template>
  <a-select
    ref="selectRef"
    :value="selectValue"
    :mode="multiple ? 'multiple' : undefined"
    show-search
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :filter-option="false"
    :dropdown-match-select-width="dropdownWidth"
    :options="options"
    :loading="remote.loading.value"
    :disabled="disabled"
    :not-found-content="remote.loading.value ? $t('common.msg.loading') : notFound || $t('common.select.noMatch')"
    @search="onSearch"
    @focus="remote.focus"
    @popup-scroll="remote.popupScroll"
    @change="onChange"
  >
    <template #dropdownRender="{ menuNode }">
      <slot name="header" />
      <component :is="menuNode" />
      <div v-if="remote.list.value.length && (remote.hasMore.value || remote.loading.value)" class="remote-select-hint">
        {{ remote.loading.value ? $t('common.msg.loading') : $t('common.select.moreHint') }}
      </div>
    </template>
  </a-select>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRemoteOptions } from '@/composables/useRemoteOptions'

/**
 * 远程搜索下拉（客户、供应商、员工、设备、项目等可能上千条的资料）：打开时取第一页，输入时防抖搜索，
 * 滚动到底部加载下一页；已选值用 label 回显，没有 label 时按 ID 取名称（fetchByIds）。
 * - fetch({ keyword, current, size })：返回数组或 { records, total }
 * - toLabel(record)：选项文字
 * - v-model:value：ID（multiple 时为 ID 数组）；@select(record)：单选选中后返回记录（清空时不触发）
 * - pinned：没有输入关键字时排在最前面的记录（如工序所在工作中心的设备）
 * - sourceKey：fetch 的数据来源标识（如客户 / 供应商），变化时丢弃已加载的选项、下次打开按新来源重新加载；
 *   fetch 依赖的条件会变时必须传（模板里的箭头函数每次渲染都是新函数，不能据此判断来源变化）。
 *   已选值不会自动清空，由页面在切换条件时清空（编辑回显时条件和值会同时变化）
 */
const props = defineProps({
  value: [Number, Array],
  label: String,
  fetch: { type: Function, required: true },
  fetchByIds: Function,
  toLabel: { type: Function, required: true },
  multiple: Boolean,
  placeholder: String,
  notFound: String,
  allowClear: { type: Boolean, default: true },
  disabled: Boolean,
  dropdownWidth: { type: [Number, Boolean], default: 300 },
  pageSize: { type: Number, default: 20 },
  pinned: { type: Array, default: () => [] },
  sourceKey: { type: null, default: undefined }
})
const emit = defineEmits(['update:value', 'select'])

const selectRef = ref()
const remote = useRemoteOptions((p) => props.fetch(p), { pageSize: props.pageSize, fetchByIds: (ids) => props.fetchByIds?.(ids) })

const selectedIds = computed(() => {
  if (props.multiple) return Array.isArray(props.value) ? props.value : []
  return props.value != null ? [props.value] : []
})
const selectValue = computed(() => (props.multiple ? selectedIds.value : (props.value ?? undefined)))

const options = computed(() => {
  const rows = remote.keyword.value ? remote.list.value : [...props.pinned, ...remote.list.value]
  const seen = new Set()
  const opts = rows.filter((r) => !seen.has(r.id) && seen.add(r.id)).map((r) => ({ value: r.id, label: props.toLabel(r) }))
  const shown = new Set(opts.map((o) => o.value))
  // 已选值不在当前结果中：用记住的记录、label 或 ID 回显
  const extra = selectedIds.value.filter((id) => !shown.has(id)).map((id) => {
    const r = remote.known.value.get(id)
    return { value: id, label: r ? props.toLabel(r) : (!props.multiple && props.label) || String(id) }
  })
  return [...extra, ...opts]
})

watch(
  () => props.pinned,
  (rows) => remote.remember(...rows),
  { immediate: true }
)

// 没有 label 的已选值按 ID 取名称（编辑时回显，不需要把全部数据加载到前端）
function resolveSelected(ids) {
  const missing = ids.filter((id) => !(props.label && !props.multiple) && !remote.find(id))
  if (missing.length) remote.resolve(missing)
}
watch(selectedIds, resolveSelected, { immediate: true })

// 数据来源变化：重新加载选项，仍有已选值时按新来源回显
watch(
  () => props.sourceKey,
  () => {
    remote.reset()
    remote.remember(...props.pinned)
    resolveSelected(selectedIds.value)
  }
)

function onSearch(keyword) {
  remote.search(keyword)
}

function onChange(v) {
  if (props.multiple) {
    const ids = v || []
    remote.remember(...ids.map((id) => remote.find(id)))
    emit('update:value', ids)
    return
  }
  const record = v == null ? undefined : remote.find(v)
  emit('update:value', v ?? null)
  if (record) {
    remote.remember(record)
    emit('select', record)
  }
}

defineExpose({
  /** 按关键字重新加载第一页 */
  reload: (keyword) => remote.load(keyword),
  blur: () => selectRef.value?.blur(),
  focus: () => selectRef.value?.focus()
})
</script>

<style scoped>
.remote-select-hint {
  padding: 4px 12px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
