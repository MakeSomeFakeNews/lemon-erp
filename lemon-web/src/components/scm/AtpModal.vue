<template>
  <a-modal :open="open" title="可承诺量（ATP）" width="min(860px, 100vw)" :footer="null" @cancel="emit('update:open', false)">
    <a-spin :spinning="loading">
      <div class="atp-head">
        <span>{{ materialLabel || '-' }}</span>
        <span class="sub-text">
          {{ warehouseId ? '指定仓库' : '全部仓库' }}<template v-if="qty">，需求 {{ formatQty(qty) }}</template>
        </span>
      </div>
      <template v-if="data">
        <a-row :gutter="16" class="atp-stats">
          <a-col :xs="12" :sm="6"><a-statistic title="可用库存" :value="fmt(data.available)" /></a-col>
          <a-col :xs="12" :sm="6"><a-statistic title="已预留" :value="fmt(data.reserved)" /></a-col>
          <a-col :xs="12" :sm="6"><a-statistic title="空闲库存" :value="fmt(data.free)" /></a-col>
          <a-col :xs="12" :sm="6">
            <a-statistic title="可承诺量" :value="fmt(data.atp)" :value-style="{ color: '#1677ff' }" />
          </a-col>
        </a-row>
        <a-alert
          v-if="qty"
          :type="data.promiseDate ? 'success' : 'warning'"
          show-icon
          class="atp-alert"
          :message="data.promiseDate ? `最早可承诺日期：${data.promiseDate}` : '按现有库存和在途供应无法满足该数量'"
        />
        <a-table
          row-key="key"
          size="small"
          :columns="columns"
          :data-source="lines"
          :pagination="false"
          :scroll="{ x: 640, y: 320 }"
          :locale="{ emptyText: '没有在途供应和待发需求' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="TYPES[record.type]?.color" :bordered="false">{{ TYPES[record.type]?.label || record.type }}</a-tag>
            </template>
            <template v-else-if="column.key === 'qty'">
              <span :class="Number(record.qty) < 0 ? 'neg' : 'pos'">{{ Number(record.qty) > 0 ? '+' : '' }}{{ formatQty(record.qty) }}</span>
            </template>
            <template v-else-if="column.key === 'balance'">
              <span :class="{ neg: Number(record.balance) < 0 }">{{ formatQty(record.balance) }}</span>
            </template>
          </template>
        </a-table>
        <div class="sub-text atp-note">结余 = 空闲库存按日期累加供应（+）和需求（−）；可承诺量取今天及之后各日结余的最小值，承诺后不影响已有订单。结余为负说明届时供应不足。</div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { atp } from '@/api/scm/planning'
import { formatQty } from '@/components/wms/wms'

/** 可承诺量：物料的库存、预留、在途供应和待发需求时间线，以及最早可承诺日期 */
const props = defineProps({
  open: Boolean,
  materialId: Number,
  materialLabel: String,
  warehouseId: Number,
  qty: Number
})
const emit = defineEmits(['update:open'])

const TYPES = {
  PO: { label: '采购在途', color: 'blue' },
  WO: { label: '生产在制', color: 'cyan' },
  SO: { label: '销售待发', color: 'orange' },
  WM: { label: '工单待领料', color: 'purple' }
}

const columns = [
  { title: '日期', dataIndex: 'date', width: 110 },
  { title: '类型', key: 'type', width: 110 },
  { title: '单据号', dataIndex: 'docNo', width: 180 },
  { title: '数量', key: 'qty', width: 110, align: 'right' },
  { title: '结余', key: 'balance', width: 110, align: 'right' }
]

const data = ref()
const loading = ref(false)
const lines = computed(() => (data.value?.lines || []).map((l, i) => ({ ...l, key: i })))
const fmt = (n) => formatQty(n ?? 0)

watch(
  () => props.open,
  async (open) => {
    data.value = undefined
    if (!open || !props.materialId) return
    loading.value = true
    try {
      data.value = await atp({ materialId: props.materialId, warehouseId: props.warehouseId || undefined, qty: props.qty || undefined })
    } finally {
      loading.value = false
    }
  }
)
</script>

<style scoped>
.atp-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-weight: 500;
}
.atp-stats {
  margin-bottom: 12px;
}
.atp-alert {
  margin-bottom: 12px;
}
.atp-note {
  margin-top: 8px;
}
.pos {
  color: #52c41a;
}
.neg {
  color: #ff4d4f;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
</style>
