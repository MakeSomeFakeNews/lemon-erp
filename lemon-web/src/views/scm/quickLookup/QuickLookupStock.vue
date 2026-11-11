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
    <div class="stock-footer">
      <span class="sub-text">{{ $t('pricing.lookup.total', { n: `${qty(total)} ${material?.unit || ''}` }) }}</span>
      <a v-if="canHistory" @click="openHistory"><HistoryOutlined /> {{ $t('pricing.lookup.stockHistory') }}</a>
    </div>
  </a-modal>
  <StockHistory v-if="canHistory" ref="historyRef" />
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { HistoryOutlined } from '@ant-design/icons-vue'
import { quickLookupStock } from '@/api/scm/quickLookup'
import { qty } from '@/utils/number'
import { useUserStore } from '@/store/user'
import StockHistory from '@/components/wms/StockHistory.vue'

/**
 * 库存分布：各仓库、库位、批次（有效期、冻结）的数量。用法：ref.open(row)
 * 有库存查询权限时可联查经营历史（进销存，与库存查询、物料资料中的「经营历史」相同，与小程序一致）
 */
const { t } = useI18n()
const canHistory = useUserStore().hasPerm('wms:stock:list')
const historyRef = ref()
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

/** 关闭弹窗，以子页打开该物料的经营历史（返回后回到商品速查） */
function openHistory() {
  const m = material.value
  visible.value = false
  historyRef.value.open({ materialId: m.materialId, materialCode: m.code, materialName: m.name })
}

defineExpose({ open })
</script>

<style scoped>
.stock-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
