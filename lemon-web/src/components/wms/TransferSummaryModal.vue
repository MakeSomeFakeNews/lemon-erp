<template>
  <!-- 调拨汇总：已调出的调拨单（含红字冲销）按 调出仓库 → 调入仓库 汇总，用于分店 / 仓库之间的考核（调拨价是内部结算价，不影响存货成本） -->
  <a-modal v-model:open="visible" :title="$t('wms.transfer.summary')" width="min(1000px, 100vw)" :footer="null" destroy-on-close>
    <a-space wrap class="toolbar">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" />
      <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('common.placeholder.all')" allow-clear style="width: 180px" />
      <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.search') }}</a-button>
    </a-space>
    <div class="sub-text tip">{{ $t('wms.transfer.summaryTip') }}</div>
    <div class="table-wrap">
      <a-table row-key="key" size="small" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 900 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'route'">
            {{ record.warehouseName }} <ArrowRightOutlined class="arrow" /> {{ record.toWarehouseName }}
          </template>
          <template v-else-if="column.key === 'totalQty'">{{ formatQty(record.totalQty) }}</template>
          <template v-else-if="column.money">
            <span :class="negativeClass(record[column.key])">{{ money(record[column.key]) }}</span>
          </template>
        </template>
        <template #summary>
          <a-table-summary-row v-if="rows.length">
            <a-table-summary-cell :index="0">{{ $t('wms.transfer.total') }}</a-table-summary-cell>
            <a-table-summary-cell :index="1" align="right">{{ sumBy(rows, 'docCount', (n) => n) }}</a-table-summary-cell>
            <a-table-summary-cell :index="2" align="right">{{ formatQty(sumBy(rows, 'totalQty', (n) => n)) }}</a-table-summary-cell>
            <a-table-summary-cell v-for="(k, i) in MONEY_KEYS" :key="k" :index="3 + i" align="right">{{ money(sumBy(rows, k)) }}</a-table-summary-cell>
          </a-table-summary-row>
        </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { transferSummary } from '@/api/wms/doc'
import { formatQty, negativeClass } from './wms'
import { money, sumBy } from '@/utils/number'

const { t } = useI18n()
const MONEY_KEYS = ['costAmount', 'transferAmount', 'diffAmount', 'fee']

const visible = ref(false)
const range = ref()
const warehouseId = ref()
const rows = ref([])
const loading = ref(false)

const columns = computed(() => [
  { title: t('wms.doc.fromTo'), key: 'route', width: 240 },
  { title: t('wms.transfer.docCount'), dataIndex: 'docCount', width: 80, align: 'right' },
  { title: t('wms.common.totalQty'), key: 'totalQty', width: 110, align: 'right' },
  { title: t('wms.transfer.costAmount'), key: 'costAmount', money: true, width: 130, align: 'right' },
  { title: t('wms.transfer.transferAmount'), key: 'transferAmount', money: true, width: 130, align: 'right' },
  { title: t('wms.transfer.diffAmount'), key: 'diffAmount', money: true, width: 120, align: 'right' },
  { title: t('wms.transfer.fee'), key: 'fee', money: true, width: 110, align: 'right' }
])

async function load() {
  loading.value = true
  try {
    const list = await transferSummary({ warehouseId: warehouseId.value, beginDate: range.value?.[0], endDate: range.value?.[1] })
    rows.value = list.map((r) => ({ ...r, key: `${r.warehouseId}-${r.toWarehouseId}` }))
  } finally {
    loading.value = false
  }
}

/** 打开时沿用列表的业务日期范围 */
function open(initialRange) {
  range.value = initialRange
  visible.value = true
  load()
}

defineExpose({ open })
</script>

<style scoped>
.toolbar {
  margin-bottom: 8px;
}
.tip {
  margin-bottom: 12px;
}
.table-wrap {
  overflow-x: auto;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.arrow {
  color: var(--app-text-3);
  font-size: 12px;
  margin: 0 2px;
}
.qty-negative {
  color: var(--status-critical);
}
</style>
