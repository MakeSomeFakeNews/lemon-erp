<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <StockGoodsFilters :query="query" @search="search" />
        <span class="inline">
          <a-tooltip :title="$t('jxc.overstock.dateTip')">{{ $t('jxc.overstock.date') }}</a-tooltip>
          <a-date-picker v-model:value="query.date" value-format="YYYY-MM-DD" :allow-clear="false" :disabled-date="(d) => d.isAfter(dayjs(), 'day')" class="date" />
        </span>
        <span class="inline">
          {{ $t('jxc.overstock.base') }}
          <a-select v-model:value="query.base" :options="baseOptions" class="base" />
          <template v-if="query.base === OverstockBase.RECENT_DAYS">
            <a-input-number v-model:value="query.days" :min="1" :max="366" :precision="0" class="num" /> {{ $t('jxc.overstock.days') }}
          </template>
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
        row-key="materialId"
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
          <template v-else-if="column.date">{{ record[column.key] || $t('jxc.overstock.never') }}</template>
          <template v-else-if="column.key === 'ratio'">{{ record.ratio == null ? '—' : Number(record.ratio).toFixed(2) }}</template>
          <template v-else-if="column.key === 'overQty'"><b class="neg">{{ qty(record.overQty) }}</b></template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'amount'">{{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { key: 'history', type: 'history', label: $t('jxc.overstock.history'), show: canHistory, onClick: () => openHistory(record) },
                { key: 'distribution', label: $t('jxc.overstock.distribution'), icon: ClusterOutlined, show: canHistory, onClick: () => stockRef.open(record) }
              ]"
            />
          </template>
        </template>
        <template v-if="rows.length && summary" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell v-for="c in columns" :key="c.key" :align="c.align">{{ summaryCell(c) }}</a-table-summary-cell>
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
import { actionColumn } from '@/components/common/tableActions'
import { jxcOverstock } from '@/api/bi/jxc'
import { dictLabel } from '@/components/bi/jxc/jxc'
import { cleanParams, goodsColumns, loadStock, useStockReport } from '@/components/bi/jxc/useStockReport'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { money, qty } from '@/utils/number'
import { OverstockBase } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 积压分析（issue #111）：截至某日库存数量超过积压基数（库存上限 / 上个月销量 / 最近 N 天销量）的商品，
 * 积压数量 = 库存数量 − 积压基数，只列出大于 0 的；资金占用需要「查看成本金额」权限（后端不返回）。
 * 有库存查询权限时可联查经营历史、库存分布；列头可排序（服务端排序，导出按同样的顺序）
 */
const dict = useDict('wms_material_category', 'wms_material_brand')
const canHistory = useUserStore().hasPerm('wms:stock:list')
const query = reactive({ warehouseId: undefined, category: undefined, brand: undefined, keyword: '', date: dayjs().format('YYYY-MM-DD'),
  base: OverstockBase.RECENT_DAYS, days: 60 })
const baseOptions = [
  { value: OverstockBase.MAX_STOCK, label: t('jxc.overstock.baseMax') },
  { value: OverstockBase.LAST_MONTH, label: t('jxc.overstock.baseLastMonth') },
  { value: OverstockBase.RECENT_DAYS, label: t('jxc.overstock.baseRecent') }
]

function params() {
  return cleanParams({ ...query, keyword: query.keyword?.trim(), days: query.base === OverstockBase.RECENT_DAYS ? query.days : undefined })
}

/** result：上一次查询的结果信息（销量期间、是否返回资金占用）；列头排序为服务端排序（资金占用只在有成本权限时可排序） */
const { rows, summary, result, loading, exporting, pagination, load, search, onChange, exportRows, sortColumns } = useStockReport(jxcOverstock, params)

const q = (key, label, extra = {}) => ({ title: t(label), key, width: 100, align: 'right', qty: true, sortable: true, ...extra })
const d = (key, label, tip) => ({ title: t(label), key, width: 108, date: true, sortable: true, tip: t(tip) })
const columns = computed(() => sortColumns([
  d('createDate', 'jxc.overstock.createDate', 'jxc.overstock.createDateTip'),
  d('lastPurchaseDate', 'jxc.overstock.lastPurchase', 'jxc.overstock.lastPurchaseTip'),
  d('lastSaleDate', 'jxc.overstock.lastSale', 'jxc.overstock.lastSaleTip'),
  ...goodsColumns(t('common.field.code')),
  q('stockQty', 'jxc.overstock.stockQty', { tip: t('jxc.overstock.stockQtyTip') }),
  { title: t('jxc.overstock.stockDays'), dataIndex: 'stockDays', key: 'stockDays', width: 100, align: 'right', sortable: true, tip: t('jxc.overstock.stockDaysTip') },
  q('baseQty', 'jxc.overstock.baseQty'),
  { title: t('jxc.overstock.ratio'), key: 'ratio', width: 104, align: 'right', sortable: true, tip: t('jxc.overstock.ratioTip') },
  q('overQty', 'jxc.overstock.overQty', { tip: t('jxc.overstock.overQtyTip') }),
  ...(result.value.costVisible ? [{ title: t('jxc.overstock.amount'), key: 'amount', width: 120, align: 'right', sortable: true, tip: t('jxc.overstock.amountTip') }] : []),
  ...(canHistory ? [actionColumn(2)] : [])
]))
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))

const tip = computed(() => {
  const r = result.value
  const base = r.salesBegin ? t('jxc.overstock.tipSales', { begin: r.salesBegin, end: r.salesEnd }) : t('jxc.overstock.tipMax')
  return `${base}${t('jxc.overstock.tipCommon')}`
})

/** 合计行：第一列显示「合计 · N 个商品」，数量与资金占用为全部积压商品的合计；raw 为导出时取数字 */
function summaryCell(c, raw = false) {
  const s = summary.value
  if (c.key === 'createDate') return `${t('jxc.common.total')} · ${t('jxc.overstock.count', { n: s.materialCount || 0 })}`
  if (['stockQty', 'baseQty', 'overQty'].includes(c.key)) return raw ? Number(s[c.key]) : qty(s[c.key])
  if (c.key === 'amount') return raw ? Number(s.amount) : money(s.amount)
  return ''
}

// 联查：经营历史（库存流水的进出与结余）、库存分布（各仓库、库位、批次）
const historyRef = ref()
const stockRef = ref()
function openHistory(r) {
  historyRef.value.open({ materialId: r.materialId, materialCode: r.code, materialName: r.name, warehouseId: query.warehouseId })
}

function doExport() {
  const cols = columns.value.filter((c) => c.key !== 'action')
  exportRows(t('jxc.overstock.exportName', { date: query.date }), cols, dict, summaryCell)
}

onMounted(load)
</script>

<style scoped>
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
.base {
  width: 140px;
}
.num {
  width: 80px;
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
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 768px) {
  .head {
    flex-wrap: wrap;
  }
}
</style>
