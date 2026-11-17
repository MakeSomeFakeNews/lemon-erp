<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-segmented v-model:value="query.mode" :options="modeOptions" @change="changeMode" />
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" class="range" />
        <StockGoodsFilters :query="query" @search="search" />
        <span class="inline">
          {{ $t('jxc.salesRate.base') }}
          <a-select v-model:value="query.base" :options="baseOptions" class="base" />
        </span>
        <span class="inline">
          {{ query.mode === SalesRateMode.HOT ? $t('jxc.salesRate.ratioHot') : $t('jxc.salesRate.ratioSlow') }}
          <a-input-number v-model:value="query.ratio" :min="0" :precision="2" addon-after="%" class="num" />
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
          <template v-else-if="column.key === 'ratio'">{{ rateText(record.ratio) }}</template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ClusterOutlined, DownloadOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import StockDistributionModal from '@/components/wms/StockDistributionModal.vue'
import TableActions from '@/components/common/TableActions.vue'
import StockGoodsFilters from '@/components/bi/jxc/StockGoodsFilters.vue'
import { actionColumn } from '@/components/common/tableActions'
import { jxcSalesRate } from '@/api/bi/jxc'
import { dictLabel, rangePresets, thisMonth } from '@/components/bi/jxc/jxc'
import { cleanParams, goodsColumns, loadStock, useStockReport } from '@/components/bi/jxc/useStockReport'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'
import { SalesRateBase, SalesRateMode } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 滞销 / 畅销分析（issue #116）：销售比例 = 本期净销量 ÷ 销售基数（本期进货量，或 期初库存 + 本期进货量）× 100%，
 * 滞销列出不高于阈值的（默认 80%）、畅销列出不低于阈值的（默认 10%），切换时阈值恢复各自的默认值；基数不大于 0 的不参与
 */
const DEFAULT_RATIO = { [SalesRateMode.SLOW]: 80, [SalesRateMode.HOT]: 10 }

const dict = useDict('wms_material_category', 'wms_material_brand')
const canHistory = useUserStore().hasPerm('wms:stock:list')
const presets = rangePresets()

const range = ref(thisMonth())
const query = reactive({ warehouseId: undefined, category: undefined, brand: undefined, keyword: '', mode: SalesRateMode.SLOW,
  base: SalesRateBase.PURCHASE, ratio: DEFAULT_RATIO[SalesRateMode.SLOW] })
const modeOptions = [
  { value: SalesRateMode.SLOW, label: t('jxc.salesRate.slow') },
  { value: SalesRateMode.HOT, label: t('jxc.salesRate.hot') }
]
const baseOptions = [
  { value: SalesRateBase.PURCHASE, label: t('jxc.salesRate.basePurchase') },
  { value: SalesRateBase.OPENING_PURCHASE, label: t('jxc.salesRate.baseOpening') }
]

function params() {
  return cleanParams({ ...query, keyword: query.keyword?.trim(), begin: range.value[0], end: range.value[1] })
}

const { rows, summary, result, loading, exporting, pagination, load, search, onChange, exportRows, sorter, sortColumns } = useStockReport(jxcSalesRate, params)
// 期初库存、本期进货两列只在基数为「期初库存 + 本期进货量」时显示，切换基数时取消排序
watch(() => query.base, () => sorter.reset())

function changeMode(mode) {
  query.ratio = DEFAULT_RATIO[mode]
  search()
}

const q = (key, label, extra = {}) => ({ title: t(label), key, width: 110, align: 'right', qty: true, sortable: true, ...extra })
const columns = computed(() => sortColumns([
  ...goodsColumns(),
  q('salesQty', 'jxc.salesRate.salesQty', { tip: t('jxc.stock.outTip') }),
  ...(result.value.base === SalesRateBase.OPENING_PURCHASE ? [q('openQty', 'jxc.salesRate.openQty'), q('purchaseQty', 'jxc.salesRate.purchaseQty')] : []),
  q('baseQty', 'jxc.salesRate.baseQty', { tip: t('jxc.salesRate.baseQtyTip') }),
  { title: t('jxc.salesRate.ratio'), key: 'ratio', width: 110, align: 'right', sortable: true },
  ...(canHistory ? [actionColumn(2)] : [])
]))
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))

const tip = computed(() => {
  const r = result.value
  if (!r.mode) return ''
  const head = r.mode === SalesRateMode.HOT ? t('jxc.salesRate.tipHot', { ratio: r.ratio }) : t('jxc.salesRate.tipSlow', { ratio: r.ratio })
  return `${head}${t('jxc.salesRate.tipCommon')}`
})

const rateText = (v) => (v == null ? '—' : `${Number(v).toFixed(2)}%`)

/** 合计行：第一列「合计 · N 行」，销售比例按合计计算；raw 为导出时取数字 */
function summaryCell(c, raw = false) {
  const s = summary.value
  if (c.key === 'code') return `${t('jxc.common.total')} · ${t('jxc.stock.rows', { n: s.materialCount || 0 })}`
  if (c.key === 'ratio') return raw ? (s.ratio == null ? '' : Number(s.ratio)) : rateText(s.ratio)
  if (c.qty) return s[c.key] == null ? '' : raw ? Number(s[c.key]) : qty(s[c.key])
  return ''
}

function doExport() {
  const cols = columns.value.filter((c) => c.key !== 'action')
  const mode = query.mode === SalesRateMode.HOT ? t('jxc.salesRate.hot') : t('jxc.salesRate.slow')
  exportRows(t('jxc.salesRate.exportName', { mode, begin: range.value[0], end: range.value[1] }), cols, dict, summaryCell)
}

// 联查：经营历史、库存分布
const historyRef = ref()
const stockRef = ref()
function openHistory(r) {
  historyRef.value.open({ materialId: r.materialId, materialCode: r.code, materialName: r.name, warehouseId: query.warehouseId })
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
.range {
  width: 240px;
}
.base {
  width: 190px;
}
.num {
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 768px) {
  .range,
  .inline {
    width: 100%;
  }
  .head {
    flex-wrap: wrap;
  }
}
</style>
