<template>
  <a-select
    :value="value"
    :placeholder="warehouseId ? $t('scm.stockSlot.selectSlot') : $t('scm.stockSlot.selectWarehouseFirst')"
    :options="options"
    :loading="loading"
    :disabled="disabled || !warehouseId"
    :not-found-content="loading ? $t('common.msg.loading') : $t('scm.stockSlot.noStock')"
    @dropdown-visible-change="(open) => open && load()"
    @change="onChange"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { pageStock } from '@/api/wms/stock'
import { cached } from '@/utils/cache'
import { availableQty, expiryStatus, formatQty, locationText } from '@/components/wms/wms'

/**
 * 库存位置选择：列出仓库中某物料各库位、批次的现有库存，出库、退货时选择从哪里出；
 * 批次按先到期先出排序，临期、过期的批次在选项中标注。
 * - value：`${locationId}|${batchNo}`，未选时为 undefined
 * - @select({ locationId, locationCode, batchNo, qty, label })
 */
const props = defineProps({
  value: String,
  /** 当前值的显示名（表格重新渲染时组件可能重建，用于回显） */
  label: String,
  warehouseId: Number,
  materialId: Number,
  disabled: Boolean,
  /** 采购退货：冻结的待检、不合格库存也可以退 */
  includeFrozen: Boolean
})
const emit = defineEmits(['update:value', 'select'])
const { t } = useI18n()

const slots = ref([])
const loading = ref(false)
let loadedKey = ''

const slotKey = (s) => `${s.locationId || 0}|${s.batchNo || ''}`
const usable = (s) => (props.includeFrozen ? Number(s.qty) : availableQty(s))
/** 先到期先出（FEFO）：未过期的按有效期从近到远，没有有效期的随后，已过期的放最后并标注 */
const sorted = computed(() => {
  const rank = (s) => (expiryStatus(s.expiryDate)?.color === 'red' ? 2 : s.expiryDate ? 0 : 1)
  return [...slots.value].sort((a, b) => rank(a) - rank(b) || String(a.expiryDate || '').localeCompare(String(b.expiryDate || '')))
})

const options = computed(() => {
  const opts = sorted.value.map((s) => {
    const expiry = expiryStatus(s.expiryDate)
    return {
      value: slotKey(s),
      label:
        `${locationText(s.locationCode)}${s.batchNo ? ' · ' + s.batchNo : ''} · ${t('scm.stockSlot.available', { qty: formatQty(usable(s)) })}` +
        `${Number(s.frozenQty) ? t('scm.stockSlot.frozen', { qty: formatQty(s.frozenQty) }) : ''}${expiry ? ` · ${expiry.text}` : ''}`
    }
  })
  if (props.value && !opts.some((o) => o.value === props.value)) {
    const [loc, batch] = props.value.split('|')
    opts.unshift({ value: props.value, label: props.label || `${loc === '0' ? t('scm.stockSlot.unspecified') : t('scm.stockSlot.location', { loc })}${batch ? ' · ' + batch : ''}` })
  }
  return opts
})

// 分别比较两个来源，只有仓库或物料真的变化时才清空（返回数组的 getter 每次都是新数组，会误触发）
watch(
  [() => props.warehouseId, () => props.materialId],
  () => {
    slots.value = []
    loadedKey = ''
  }
)

async function load() {
  if (!props.warehouseId || !props.materialId) return
  const key = `${props.warehouseId}|${props.materialId}`
  if (key === loadedKey) return
  loading.value = true
  try {
    const params = { warehouseId: props.warehouseId, materialId: props.materialId, current: 1, size: 200 }
    const res = await cached(`stock-slots:${key}`, () => pageStock(params))
    slots.value = res.records
    loadedKey = key
  } finally {
    loading.value = false
  }
}

function onChange(v) {
  emit('update:value', v)
  const slot = slots.value.find((s) => slotKey(s) === v)
  if (slot) {
    const label = options.value.find((o) => o.value === v)?.label
    emit('select', { locationId: slot.locationId || null, locationCode: slot.locationCode, batchNo: slot.batchNo || '', qty: usable(slot), label })
  }
}

defineExpose({ load })
</script>
