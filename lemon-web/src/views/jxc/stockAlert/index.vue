<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="query.tab" class="tabs" @change="search">
        <a-tab-pane :key="StockAlertTab.WAREHOUSE" :tab="$t('jxc.stockAlert.warehouse')" />
        <a-tab-pane :key="StockAlertTab.HIGH" :tab="$t('jxc.stockAlert.high')" />
        <a-tab-pane :key="StockAlertTab.LOW" :tab="$t('jxc.stockAlert.low')" />
      </a-tabs>
      <div class="filters">
        <span class="inline">
          <a-tooltip :title="$t('jxc.overstock.dateTip')">{{ $t('jxc.overstock.date') }}</a-tooltip>
          <a-date-picker v-model:value="query.date" value-format="YYYY-MM-DD" :allow-clear="false" :disabled-date="(v) => v.isAfter(dayjs(), 'day')" class="date" />
        </span>
        <StockGoodsFilters :query="query" @search="search" />
        <span class="inline">
          {{ $t('jxc.stockAlert.stockFilter') }}
          <a-select v-model:value="query.stockFilter" :options="stockOptions" class="sel" />
        </span>
        <span v-if="query.tab === StockAlertTab.WAREHOUSE" class="inline">
          {{ $t('jxc.stockAlert.alertFilter') }}
          <a-select v-model:value="query.alertFilter" :options="alertOptions" class="sel" />
        </span>
        <a-button type="primary" :loading="loading" @click="search">{{ $t('common.action.query') }}</a-button>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="head">
        <span class="sub-text">{{ tip }}</span>
        <a-button :disabled="!rows.length" :loading="exporting" @click="doExport">
          <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
        </a-button>
      </div>
      <a-table
        :row-key="(r) => `${r.warehouseId || 0}-${r.materialId}`"
        size="middle"
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onChange"
      >
        <template #headerCell="{ column }">
          <template v-if="column.tip">
            {{ column.title }}
            <a-tooltip :title="column.tip"><QuestionCircleOutlined class="help" /></a-tooltip>
          </template>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <a v-if="canHistory" @click="openHistory(record)">{{ record.code }}</a>
            <span v-else>{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'category'">{{ dictLabel(dict.wms_material_category, record.category) }}</template>
          <template v-else-if="column.key === 'brand'">{{ dictLabel(dict.wms_material_brand, record.brand) }}</template>
          <template v-else-if="column.key === 'warehouseName'">{{ record.warehouseName || $t('jxc.stockAlert.allWarehouses') }}</template>
          <template v-else-if="column.date">{{ record[column.key] || $t('jxc.stock.never') }}</template>
          <template v-else-if="column.key === 'stockQty'">
            <span :class="alertClass(record)">{{ qty(record.stockQty) }}</span>
          </template>
          <template v-else-if="column.key === 'gapQty'"><b :class="query.tab === StockAlertTab.HIGH ? 'high' : 'low'">{{ qty(record.gapQty) }}</b></template>
          <template v-else-if="column.key === 'ratio'">{{ record.ratio == null ? '—' : Number(record.ratio).toFixed(2) }}</template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.price">{{ record[column.key] == null ? '-' : price(record[column.key]) }}</template>
          <template v-else-if="column.money">{{ money(record[column.key]) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { key: 'history', type: 'history', label: $t('jxc.stock.history'), show: canHistory, onClick: () => openHistory(record) },
                { key: 'distribution', label: $t('jxc.stock.distribution'), icon: ClusterOutlined, show: canHistory, onClick: () => stockRef.open(record) }
              ]"
            />
          </template>
        </template>
        <template v-if="rows.length && summary" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :align="c.align">{{ summaryCell(c, false, i) }}</a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>

    <StockHistory v-if="canHistory" ref="historyRef" />
    <StockDistributionModal v-if="canHistory" ref="stockRef" :loader="loadStock" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ClusterOutlined, DownloadOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import StockDistributionModal from '@/components/wms/StockDistributionModal.vue'
import TableActions from '@/components/common/TableActions.vue'
import StockGoodsFilters from '@/components/bi/jxc/StockGoodsFilters.vue'
import { usePriceLevels } from '@/components/wms/priceLevels'
import { jxcStockAlert } from '@/api/bi/jxc'
import { dictLabel } from '@/components/bi/jxc/jxc'
import { cleanParams, loadStock, useStockReport } from '@/components/bi/jxc/useStockReport'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { money, price, qty } from '@/utils/number'
import { StockAlert, StockAlertFilter, StockAlertTab, StockQtyFilter } from '@/constants/enums'
import { t } from '@/locales'
import { SUMMARY_KEYS, alertColumns } from './columns'

/**
 * 库存预警（issue #116）：截至某日的库存与上下限，三个标签页——仓库（每个仓库 + 商品的明细、月销量、订单数量、
 * 金额与价格、上下限、货位）、高于上限（积压基数、系数、数量、资金占用）、低于下限（补货基数、系数、数量、补货资金）。
 * 预警口径与库存查询一致；金额、价格按权限由后端过滤（costVisible、purchaseVisible、salesVisible）
 */
const dict = useDict('wms_material_category', 'wms_material_brand')
const canHistory = useUserStore().hasPerm('wms:stock:list')
const { enabledLevels } = usePriceLevels()

const query = reactive({ tab: StockAlertTab.WAREHOUSE, date: dayjs().format('YYYY-MM-DD'), warehouseId: undefined, category: undefined,
  brand: undefined, keyword: '', stockFilter: '', alertFilter: '' })
const stockOptions = [
  { value: '', label: t('jxc.stockAlert.stockAll') },
  { value: StockQtyFilter.POSITIVE, label: t('jxc.stockAlert.stockPositive') },
  { value: StockQtyFilter.ZERO, label: t('jxc.stockAlert.stockZero') },
  { value: StockQtyFilter.NEGATIVE, label: t('jxc.stockAlert.stockNegative') },
  { value: StockQtyFilter.NONZERO, label: t('jxc.stockAlert.stockNonzero') }
]
const alertOptions = [
  { value: '', label: t('jxc.stockAlert.alertAll') },
  { value: StockAlertFilter.LOW, label: t('jxc.stockAlert.alertLow') },
  { value: StockAlertFilter.HIGH, label: t('jxc.stockAlert.alertHigh') },
  { value: StockAlertFilter.ANY, label: t('jxc.stockAlert.alertAny') }
]

function params() {
  return cleanParams({ ...query, keyword: query.keyword?.trim(), alertFilter: query.tab === StockAlertTab.WAREHOUSE ? query.alertFilter : undefined })
}

const { rows, summary, result, loading, exporting, pagination, load, search, onChange, exportRows } = useStockReport(jxcStockAlert, params)

/** 列按上一次查询的标签页和权限（切换标签页后表格与数据一起更新） */
const shownTab = computed(() => result.value.tab || StockAlertTab.WAREHOUSE)
const columns = computed(() => alertColumns(shownTab.value, result.value, enabledLevels.value, canHistory))
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))

const tip = computed(() => {
  const r = result.value
  const head = { [StockAlertTab.WAREHOUSE]: 'tipWarehouse', [StockAlertTab.HIGH]: 'tipHigh', [StockAlertTab.LOW]: 'tipLow' }[shownTab.value]
  return `${t(`jxc.stockAlert.${head}`)}${r.monthBegin ? t('jxc.stockAlert.tipMonth', { begin: r.monthBegin, date: r.date }) : ''}`
})

const alertClass = (r) => {
  if (shownTab.value !== StockAlertTab.WAREHOUSE) return ''
  return r.alert === StockAlert.LOW ? 'low' : r.alert === StockAlert.HIGH ? 'high' : ''
}

/** 合计行：第一列「合计 · N 行」，数量、金额为全部行的合计；raw 为导出时取数字 */
function summaryCell(c, raw = false, index = columns.value.indexOf(c)) {
  const s = summary.value
  if (index === 0) return `${t('jxc.common.total')} · ${t('jxc.stock.rows', { n: s.materialCount || 0 })}`
  if (!SUMMARY_KEYS.includes(c.key) || s[c.key] == null) return ''
  if (raw) return Number(s[c.key])
  return c.money ? money(s[c.key]) : qty(s[c.key])
}

function doExport() {
  const cols = columns.value.filter((c) => c.key !== 'action')
  const tabName = t(`jxc.stockAlert.${{ [StockAlertTab.WAREHOUSE]: 'warehouse', [StockAlertTab.HIGH]: 'high', [StockAlertTab.LOW]: 'low' }[shownTab.value]}`)
  exportRows(t('jxc.stockAlert.exportName', { tab: tabName, date: query.date }), cols, dict, (c, raw) => summaryCell(c, raw, cols.indexOf(c)),
    (c, r) => (c.key === 'warehouseName' ? r.warehouseName || t('jxc.stockAlert.allWarehouses') : undefined))
}

// 联查：经营历史（按行的仓库）、库存分布
const historyRef = ref()
const stockRef = ref()
function openHistory(r) {
  historyRef.value.open({ materialId: r.materialId, materialCode: r.code, materialName: r.name, warehouseId: r.warehouseId || query.warehouseId })
}

onMounted(load)
</script>

<style scoped>
.tabs {
  margin-top: -8px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.date {
  width: 140px;
}
.sel {
  width: 130px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
.low {
  color: var(--status-critical, #cf1322);
}
.high {
  color: var(--status-warning, #d46b08);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 768px) {
  .inline {
    width: 100%;
  }
  .head {
    flex-wrap: wrap;
  }
}
</style>
