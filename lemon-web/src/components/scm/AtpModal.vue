<template>
  <a-modal :open="open" :title="$t('scm.atp.title')" width="min(860px, 100vw)" :footer="null" @cancel="emit('update:open', false)">
    <a-spin :spinning="loading">
      <div class="atp-head">
        <span>{{ materialLabel || '-' }}</span>
        <span class="sub-text">
          {{ warehouseId ? $t('scm.atp.oneWarehouse') : $t('scm.atp.allWarehouses') }}<template v-if="qty">{{ $t('scm.atp.demand', { qty: formatQty(qty) }) }}</template>
        </span>
      </div>
      <template v-if="data">
        <a-row :gutter="16" class="atp-stats">
          <a-col :xs="12" :sm="6"><a-statistic :title="$t('scm.atp.available')" :value="fmt(data.available)" /></a-col>
          <a-col :xs="12" :sm="6"><a-statistic :title="$t('scm.atp.reserved')" :value="fmt(data.reserved)" /></a-col>
          <a-col :xs="12" :sm="6"><a-statistic :title="$t('scm.atp.free')" :value="fmt(data.free)" /></a-col>
          <a-col :xs="12" :sm="6">
            <a-statistic :title="$t('scm.atp.atp')" :value="fmt(data.atp)" :value-style="{ color: '#1677ff' }" />
          </a-col>
        </a-row>
        <a-alert
          v-if="qty"
          :type="data.promiseDate ? 'success' : 'warning'"
          show-icon
          class="atp-alert"
          :message="data.promiseDate ? $t('scm.atp.promiseDate', { date: data.promiseDate }) : $t('scm.atp.cannotMeet')"
        />
        <a-table
          row-key="key"
          size="small"
          :columns="columns"
          :data-source="lines"
          :pagination="false"
          :scroll="{ x: 640, y: 320 }"
          :locale="{ emptyText: $t('scm.atp.empty') }"
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
        <div class="sub-text atp-note">{{ $t('scm.atp.note') }}</div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()
const TYPES = {
  PO: { label: t('scm.atp.typePO'), color: 'blue' },
  WO: { label: t('scm.atp.typeWO'), color: 'cyan' },
  SO: { label: t('scm.atp.typeSO'), color: 'orange' },
  WM: { label: t('scm.atp.typeWM'), color: 'purple' }
}

const columns = [
  { title: t('common.field.date'), dataIndex: 'date', width: 110 },
  { title: t('common.field.type'), key: 'type', width: 140 },
  { title: t('scm.order.docNo'), dataIndex: 'docNo', width: 180 },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('scm.atp.balance'), key: 'balance', width: 110, align: 'right' }
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
