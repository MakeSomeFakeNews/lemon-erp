<template>
  <a-select
    :value="value || undefined"
    show-search
    allow-clear
    :placeholder="warehouseId ? placeholder : $t('wms.common.selectWarehouseFirst')"
    :options="options"
    :loading="loading"
    :disabled="disabled || !warehouseId"
    option-filter-prop="label"
    :dropdown-match-select-width="280"
    @change="(v) => emit('update:value', v ?? null)"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { locationOptions } from '@/api/wms/location'
import { t } from '@/locales'

/**
 * 库位下拉：仓库下启用的库位，不选为「未指定」。
 * 同一仓库的库位在页面内缓存，明细表格中每行一个下拉也只请求一次
 */
const props = defineProps({
  value: Number,
  warehouseId: Number,
  placeholder: { type: String, default: () => t('wms.common.unspecified') },
  disabled: Boolean
})
const emit = defineEmits(['update:value'])

const list = ref([])
const loading = ref(false)
const options = computed(() => list.value.map((l) => ({ value: l.id, label: l.name ? `${l.code} ${l.name}` : l.code })))

watch(
  () => props.warehouseId,
  async (id) => {
    list.value = []
    if (!id) return
    loading.value = true
    try {
      list.value = await loadLocations(id)
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)
</script>

<script>
const cache = new Map()
const TTL = 60 * 1000

function loadLocations(warehouseId) {
  const hit = cache.get(warehouseId)
  if (hit && Date.now() - hit.time < TTL) return hit.promise
  const promise = locationOptions(warehouseId).catch((e) => {
    cache.delete(warehouseId)
    throw e
  })
  cache.set(warehouseId, { promise, time: Date.now() })
  return promise
}
</script>
