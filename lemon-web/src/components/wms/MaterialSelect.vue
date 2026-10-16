<template>
  <a-select
    ref="selectRef"
    :value="value ?? undefined"
    show-search
    :placeholder="placeholder"
    :filter-option="false"
    :options="options"
    :loading="loading"
    :disabled="disabled"
    :not-found-content="loading ? '加载中…' : '没有匹配的物料'"
    @search="onSearch"
    @focus="onFocus"
    @blur="searchText = ''"
    @change="onChange"
  />
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { materialByBarcode, materialOptions } from '@/api/wms/material'

/**
 * 物料下拉：按编码或名称远程搜索启用的物料，支持扫码。
 * - v-model:value：物料 ID
 * - label：当前值的显示名（编辑时回显）
 * - @select(material)：选中后返回物料信息（单位、批次 / 序列号管理、条码、默认仓库库位等）
 *
 * 扫码：输入（或扫码枪录入）条码后回车，按物料条码或辅助单位条码查找并选中；
 * 搜索结果中有条码完全一致的物料时也会直接选中。
 * 扫码选中时 select 事件额外带 scanUnit / scanRatio（辅助单位条码对应的单位和换算率，基本单位条码时为空）
 */
const props = defineProps({
  value: Number,
  label: String,
  placeholder: { type: String, default: '搜索物料编码、名称或扫码' },
  disabled: Boolean
})
const emit = defineEmits(['update:value', 'select'])

const selectRef = ref()
const materials = ref([])
const loading = ref(false)
const searchText = ref('')
let seq = 0
let timer
let pending = false

const toLabel = (m) => `${m.code} ${m.name}${m.spec ? `（${m.spec}）` : ''}`

const options = computed(() => {
  const list = materials.value.map((m) => ({ value: m.id, label: toLabel(m) }))
  if (props.value != null && !list.some((o) => o.value === props.value)) {
    list.unshift({ value: props.value, label: props.label || String(props.value) })
  }
  return list
})

async function load(keyword) {
  const current = ++seq
  loading.value = true
  try {
    const res = await materialOptions(keyword || undefined)
    if (current !== seq) return
    materials.value = res
    // 搜索文本与某个物料的条码完全一致：视为扫码
    const code = keyword?.trim()
    const hit = code && res.find((m) => m.barcode && m.barcode === code)
    if (hit && searchText.value.trim() === code) pick(hit)
  } finally {
    if (current === seq) loading.value = false
  }
}

function onSearch(keyword) {
  searchText.value = keyword
  clearTimeout(timer)
  pending = true
  timer = setTimeout(() => {
    pending = false
    load(keyword)
  }, 300)
}

function onFocus() {
  if (!materials.value.length) load()
}

function onChange(id) {
  searchText.value = ''
  emit('update:value', id ?? null)
  const material = materials.value.find((m) => m.id === id)
  if (material) emit('select', { ...material, label: toLabel(material) })
}

/** 选中扫码得到的物料，并收起下拉 */
function pick(material, scanUnit = null, scanRatio = null) {
  clearTimeout(timer)
  pending = false
  seq++
  loading.value = false
  searchText.value = ''
  if (!materials.value.some((m) => m.id === material.id)) materials.value = [material, ...materials.value]
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
  clearTimeout(timer)
  pending = false
  await load(code)
  if (!materials.value.length) message.warning(`没有找到编码、名称或条码为「${code}」的物料`)
}

/**
 * 回车：搜索结果还没刷新（扫码枪录入很快）或没有结果时按条码查找，拦截下拉自己的回车选中；
 * 结果已刷新且有条码一致的物料时选中它；其它情况保持原来的行为（选中高亮项）
 */
function onKeydown(e) {
  if (e.key !== 'Enter' || e.isComposing) return
  const code = searchText.value.trim()
  if (!code) return
  const exact = !pending && !loading.value && materials.value.find((m) => m.barcode && m.barcode === code)
  if (exact) {
    e.preventDefault()
    e.stopPropagation()
    pick(exact)
    return
  }
  if (pending || loading.value || !materials.value.length) {
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
  clearTimeout(timer)
  if (rootEl instanceof Element) rootEl.removeEventListener('keydown', onKeydown, true)
})
</script>
