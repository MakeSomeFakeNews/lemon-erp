<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" class="range" />
        <StockGoodsFilters :query="query" @search="search" />
        <span class="inline">
          {{ $t('jxc.turnover.basis') }}
          <a-select v-model:value="basisKey" :options="basisOptions" class="basis" />
        </span>
        <span class="inline">
          {{ $t('jxc.turnover.outFilter') }}
          <a-select v-model:value="query.outFilter" :options="outOptions" class="out" />
        </span>
        <a-checkbox v-model:checked="query.excludeIdle">{{ $t('jxc.turnover.excludeIdle') }}</a-checkbox>
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
          <template v-else-if="column.key === 'turnoverRate'">{{ rateText(record.turnoverRate) }}</template>
          <template v-else-if="column.key === 'turnoverDays'">{{ record.turnoverDays == null ? '—' : Number(record.turnoverDays).toFixed(1) }}</template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.money">{{ record[column.key] == null ? '—' : money(record[column.key]) }}</template>
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
import { usePriceLevels } from '@/components/wms/priceLevels'
import { jxcTurnover } from '@/api/bi/jxc'
import { dictLabel, rangePresets, thisMonth } from '@/components/bi/jxc/jxc'
import { cleanParams, goodsColumns, loadStock, useStockReport } from '@/components/bi/jxc/useStockReport'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { money, qty } from '@/utils/number'
import { TurnoverBasis, TurnoverOutFilter } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 周转分析（issue #116）：库存周转天数 = 期间天数 × (期初 + 期末) ÷ 2 ÷ 本期出库，周转率 = 本期出库 ÷ 平均库存 × 100%。
 * 计算维度可选销售数量、成本价、零售价、批发价（启用的价格列），金额 = 数量 × 对应价格；成本价需要「查看成本金额」，
 * 零售价 / 批发价需要售价查看权限（与商品速查一致，后端校验）
 */
const dict = useDict('wms_material_category', 'wms_material_brand')
const userStore = useUserStore()
const canHistory = userStore.hasPerm('wms:stock:list')
const canCost = userStore.hasPerm('wms:stock:cost')
const canSalesPrice = userStore.hasPerm(['scm:sales:list', 'scm:salesOut:list', 'scm:quotation:list', 'scm:goodsPrice:list'])
const { enabledLevels, levelName } = usePriceLevels()
const presets = rangePresets()

const range = ref(thisMonth())
const query = reactive({ warehouseId: undefined, category: undefined, brand: undefined, keyword: '', excludeIdle: true, outFilter: '' })
/** 计算维度：QTY / COST / L0（零售价）/ L1–L8（批发价列） */
const basisKey = ref(TurnoverBasis.QTY)
const basisOptions = computed(() => [
  { value: TurnoverBasis.QTY, label: t('jxc.turnover.basisQty') },
  ...(canCost ? [{ value: TurnoverBasis.COST, label: t('jxc.turnover.basisCost') }] : []),
  ...(canSalesPrice ? enabledLevels.value.map((l) => ({ value: `L${l.level}`, label: l.name })) : [])
])
const outOptions = [
  { value: '', label: t('jxc.turnover.outAll') },
  { value: TurnoverOutFilter.HAS_OUT, label: t('jxc.turnover.hasOut') },
  { value: TurnoverOutFilter.NO_OUT, label: t('jxc.turnover.noOut') }
]

function params() {
  const key = basisKey.value
  const level = key.startsWith('L') ? Number(key.slice(1)) : null
  const basis = level === null ? key : level === 0 ? TurnoverBasis.RETAIL : TurnoverBasis.WHOLESALE
  return cleanParams({ ...query, keyword: query.keyword?.trim(), begin: range.value[0], end: range.value[1], basis,
    priceLevel: level || undefined })
}

const { rows, summary, result, loading, exporting, pagination, load, search, onChange, exportRows, sorter, sortColumns } = useStockReport(jxcTurnover, params)
// 金额列随计算维度显示 / 隐藏，切换时取消排序
watch(basisKey, () => sorter.reset())

const q = (key, label, extra = {}) => ({ title: t(label), key, width: 110, align: 'right', qty: true, sortable: true, ...extra })
const m = (key, label) => ({ title: t(label), key, width: 120, align: 'right', money: true, sortable: true })
const columns = computed(() => {
  const amounts = !!result.value.amountVisible
  return sortColumns([
    ...goodsColumns(),
    { title: t('jxc.turnover.turnoverDays'), key: 'turnoverDays', width: 110, align: 'right', sortable: true, tip: t('jxc.turnover.turnoverDaysTip') },
    { title: t('jxc.turnover.turnoverRate'), key: 'turnoverRate', width: 110, align: 'right', sortable: true, tip: t('jxc.turnover.turnoverRateTip') },
    q('outQty', 'jxc.turnover.outQty', { tip: t('jxc.stock.outTip') }),
    ...(amounts ? [m('outAmount', 'jxc.turnover.outAmount')] : []),
    q('openQty', 'jxc.turnover.openQty'),
    ...(amounts ? [m('openAmount', 'jxc.turnover.openAmount')] : []),
    q('closeQty', 'jxc.turnover.closeQty'),
    ...(amounts ? [m('closeAmount', 'jxc.turnover.closeAmount')] : []),
    ...(canHistory ? [actionColumn(2)] : [])
  ])
})
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))

const priceLabel = computed(() => {
  const b = result.value.basis
  if (b === TurnoverBasis.COST) return t('jxc.turnover.basisCost')
  if (b === TurnoverBasis.RETAIL) return levelName(0)
  if (b === TurnoverBasis.WHOLESALE) return levelName(Number(basisKey.value.slice(1)))
  return ''
})
const tip = computed(() => {
  const b = result.value.basis
  const way = !b || b === TurnoverBasis.QTY ? t('jxc.turnover.tipQty') : t('jxc.turnover.tipAmount', { price: priceLabel.value })
  return `${t('jxc.turnover.tip')}${way}`
})

const rateText = (v) => (v == null ? '—' : `${Number(v).toFixed(2)}%`)

/** 合计行：第一列「合计 · N 行」，周转天数、周转率按合计计算；raw 为导出时取数字 */
function summaryCell(c, raw = false) {
  const s = summary.value
  if (c.key === 'code') return `${t('jxc.common.total')} · ${t('jxc.stock.rows', { n: s.materialCount || 0 })}`
  if (c.key === 'turnoverRate') return raw ? (s.turnoverRate == null ? '' : Number(s.turnoverRate)) : rateText(s.turnoverRate)
  if (c.key === 'turnoverDays') return s.turnoverDays == null ? (raw ? '' : '—') : raw ? Number(s.turnoverDays) : Number(s.turnoverDays).toFixed(1)
  if (c.qty) return raw ? Number(s[c.key]) : qty(s[c.key])
  if (c.money) return s[c.key] == null ? '' : raw ? Number(s[c.key]) : money(s[c.key])
  return ''
}

function doExport() {
  const cols = columns.value.filter((c) => c.key !== 'action')
  exportRows(t('jxc.turnover.exportName', { begin: range.value[0], end: range.value[1] }), cols, dict, summaryCell)
}

// 联查：经营历史（库存流水的进出与结余）、库存分布（各仓库、库位、批次）
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
.basis {
  width: 130px;
}
.out {
  width: 170px;
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
