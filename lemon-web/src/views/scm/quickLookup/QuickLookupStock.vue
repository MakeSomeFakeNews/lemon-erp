<template>
  <a-modal v-model:open="visible" :title="$t('pricing.lookup.stockTitle', { name: material?.name || '' })" width="min(760px, 100vw)" :footer="null">
    <a-table
      :row-key="(r, i) => i"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 640, y: 400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'qty' || column.key === 'frozenQty'">{{ qty(record[column.key]) }}</template>
      </template>
    </a-table>
    <div class="sub-text">{{ $t('pricing.lookup.total', { n: `${qty(total)} ${material?.unit || ''}` }) }}</div>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { quickLookupStock } from '@/api/scm/quickLookup'
import { qty } from '@/utils/number'

/** 库存分布：各仓库、库位、批次（有效期、冻结）的数量。用法：ref.open(row) */
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const material = ref()
const rows = ref([])
const total = computed(() => rows.value.reduce((s, r) => s + (Number(r.qty) || 0), 0))

const columns = computed(() => [
  { title: t('pricing.lookup.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 140 },
  { title: t('pricing.lookup.location'), dataIndex: 'locationCode', key: 'locationCode', width: 110 },
  { title: t('pricing.lookup.batch'), dataIndex: 'batchNo', key: 'batchNo', width: 130 },
  { title: t('pricing.lookup.expiry'), dataIndex: 'expiryDate', key: 'expiryDate', width: 110 },
  { title: t('pricing.lookup.qty'), key: 'qty', width: 80, align: 'right' },
  { title: t('pricing.lookup.frozen'), key: 'frozenQty', width: 70, align: 'right' }
])

async function open(m) {
  material.value = m
  rows.value = []
  visible.value = true
  loading.value = true
  try {
    rows.value = (await quickLookupStock(m.materialId)) || []
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  margin-top: 8px;
}
</style>
