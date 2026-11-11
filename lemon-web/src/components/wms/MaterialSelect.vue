<template>
  <a-select
    ref="selectRef"
    :value="value ?? undefined"
    show-search
    :placeholder="placeholder"
    :filter-option="false"
    :dropdown-match-select-width="400"
    :options="options"
    :loading="remote.loading.value"
    :disabled="disabled"
    :not-found-content="remote.loading.value ? $t('common.msg.loading') : $t('wms.select.noMaterial')"
    @search="onSearch"
    @focus="remote.focus"
    @blur="searchText = ''"
    @popup-scroll="remote.popupScroll"
    @change="onChange"
  >
    <template #dropdownRender="{ menuNode }">
      <component :is="menuNode" />
      <div v-if="remote.list.value.length && (remote.hasMore.value || remote.loading.value)" class="remote-select-hint">
        {{ remote.loading.value ? $t('common.msg.loading') : $t('common.select.moreHint') }}
      </div>
    </template>
  </a-select>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { materialByBarcode, materialOptions, materialsByIds } from '@/api/wms/material'
import { useRemoteOptions } from '@/composables/useRemoteOptions'
import { t } from '@/locales'

/**
 * 物料下拉：启用的物料远程分页搜索（编码、名称、规格、条码、拼音首字母、去掉分隔符的缩写，如「126034」找到「12/R60 34TH」），
 * 打开时取前 20 条，滚动加载更多；支持扫码。
 * - v-model:value：物料 ID
 * - label：当前值的显示名（编辑时回显，不传时按 ID 取名称）
 * - @select(material)：选中后返回物料信息（单位、批次 / 序列号管理、条码、默认仓库库位等）
 *
 * 扫码：输入（或扫码枪录入）条码后回车，按物料条码或辅助单位条码查找并选中；
 * 搜索结果中有条码完全一致的物料时也会直接选中。
 * 扫码选中时 select 事件额外带 scanUnit / scanRatio（辅助单位条码对应的单位和换算率，基本单位条码时为空）
 */
const props = defineProps({
  value: Number,
  label: String,
  placeholder: { type: String, default: () => t('wms.select.materialPlaceholder') },
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const selectRef = ref()
const searchText = ref('')
let pending = false

const remote = useRemoteOptions(
  async ({ keyword, current, size }) => {
    const res = await materialOptions(keyword, { current, size })
    // 搜索文本与某个物料的条码完全一致：视为扫码
    const code = keyword?.trim()
    const hit = current === 1 && code && res.find((m) => m.barcode && m.barcode === code)
    if (hit && searchText.value.trim() === code) setTimeout(() => pick(hit))
    return res
  },
  { fetchByIds: materialsByIds }
)

const toLabel = (m) => `${m.code} ${m.name}${m.spec ? `（${m.spec}）` : ''}`

const options = computed(() => {
  const list = remote.list.value.map((m) => ({ value: m.id, label: toLabel(m) }))
  if (props.value != null && !list.some((o) => o.value === props.value)) {
    const known = remote.known.value.get(props.value)
    list.unshift({ value: props.value, label: known ? toLabel(known) : props.label || String(props.value) })
  }
  return list
})

// 没有 label 的已选值按 ID 取名称
watch(
  () => props.value,
  (id) => {
    if (id != null && !props.label && !remote.find(id)) remote.resolve([id])
  },
  { immediate: true }
)

function onSearch(keyword) {
  searchText.value = keyword
  pending = true
  remote.search(keyword)
}

// 防抖结束（开始请求）后不再视为「结果未刷新」
watch(remote.loading, (v) => {
  if (v) pending = false
})

function onChange(id) {
  searchText.value = ''
  emit('update:value', id ?? null)
  const material = id == null ? null : remote.find(id)
  if (material) {
    remote.remember(material)
    emit('select', { ...material, label: toLabel(material) })
  }
}

/** 选中扫码得到的物料，并收起下拉 */
function pick(material, scanUnit = null, scanRatio = null) {
  remote.cancel()
  pending = false
  searchText.value = ''
  remote.remember(material)
  emit('update:value', material.id)
  emit('select', { ...material, label: toLabel(material), scanUnit, scanRatio })
  selectRef.value?.blur()
}

async function scan(code) {
  const res = await materialByBarcode(code)
  if (res?.material) {
    pick(res.material, res.unit || null, res.ratio != null ? Number(res.ratio) : null)
    return
  }
  // 不是条码：按关键字搜索
  remote.cancel()
  pending = false
  const rows = await remote.load(code)
  if (rows && !rows.length) message.warning(t('wms.select.materialNotFound', { code }))
}

/**
 * 回车：搜索结果还没刷新（扫码枪录入很快）或没有结果时按条码查找，拦截下拉自己的回车选中；
 * 结果已刷新且有条码一致的物料时选中它；其它情况保持原来的行为（选中高亮项）
 */
function onKeydown(e) {
  if (e.key !== 'Enter' || e.isComposing) return
  const code = searchText.value.trim()
  if (!code) return
  const busy = pending || remote.loading.value
  const exact = !busy && remote.list.value.find((m) => m.barcode && m.barcode === code)
  if (exact) {
    e.preventDefault()
    e.stopPropagation()
    pick(exact)
    return
  }
  if (busy || !remote.list.value.length) {
    e.preventDefault()
    e.stopPropagation()
    scan(code)
  }
}

let rootEl
onMounted(() => {
  rootEl = selectRef.value?.$el
  if (rootEl instanceof Element) rootEl.addEventListener('keydown', onKeydown, true)
})
onBeforeUnmount(() => {
  if (rootEl instanceof Element) rootEl.removeEventListener('keydown', onKeydown, true)
})
</script>

<style scoped>
.remote-select-hint {
  padding: 4px 12px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
