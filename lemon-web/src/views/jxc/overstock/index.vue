<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('jxc.common.allWarehouses')" class="filter-s" />
        <MaterialCategorySelect v-model="query.category" :placeholder="$t('jxc.common.allCategories')" class="filter-s" />
        <a-select v-model:value="query.brand" :options="toOptions(dict.wms_material_brand)" allow-clear show-search option-filter-prop="label"
          :placeholder="$t('jxc.common.allBrands')" class="filter-s" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('jxc.overstock.keyword')" class="filter" @press-enter="search" />
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
        <a-button :disabled="!rows.length" :loading="exporting" @click="exportRows">
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
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import StockDistributionModal from '@/components/wms/StockDistributionModal.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { jxcOverstock } from '@/api/bi/jxc'
import { pageStock } from '@/api/wms/stock'
import { dictLabel } from '@/components/bi/jxc/jxc'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { OverstockBase } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 积压分析（issue #111）：截至某日库存数量超过积压基数（库存上限 / 上个月销量 / 最近 N 天销量）的商品，
 * 积压数量 = 库存数量 − 积压基数，只列出大于 0 的；资金占用需要「查看成本金额」权限（后端不返回）。
 * 有库存查询权限时可联查经营历史、库存分布
 */
const PAGE_SIZE = 20
/** 导出：每次取的行数（分页上限）与最多导出的行数 */
const EXPORT_PAGE = 500
const EXPORT_MAX = 5000

const dict = useDict('wms_material_category', 'wms_material_brand')
const canHistory = useUserStore().hasPerm('wms:stock:list')
const query = reactive({ warehouseId: undefined, category: undefined, brand: undefined, keyword: '', date: dayjs().format('YYYY-MM-DD'),
  base: OverstockBase.RECENT_DAYS, days: 60 })
const baseOptions = [
  { value: OverstockBase.MAX_STOCK, label: t('jxc.overstock.baseMax') },
  { value: OverstockBase.LAST_MONTH, label: t('jxc.overstock.baseLastMonth') },
  { value: OverstockBase.RECENT_DAYS, label: t('jxc.overstock.baseRecent') }
]
const rows = ref([])
const summary = ref(null)
/** 上一次查询的结果信息：销量期间、是否返回资金占用 */
const result = ref({ costVisible: false })
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: PAGE_SIZE, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const q = (key, label, extra = {}) => ({ title: t(label), key, width: 100, align: 'right', qty: true, ...extra })
const d = (key, label, tip) => ({ title: t(label), key, width: 108, date: true, tip: t(tip) })
const columns = computed(() => [
  d('createDate', 'jxc.overstock.createDate', 'jxc.overstock.createDateTip'),
  d('lastPurchaseDate', 'jxc.overstock.lastPurchase', 'jxc.overstock.lastPurchaseTip'),
  d('lastSaleDate', 'jxc.overstock.lastSale', 'jxc.overstock.lastSaleTip'),
  { title: t('common.field.code'), key: 'code', width: 130 },
  { title: t('jxc.field.material'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: t('jxc.field.category'), key: 'category', width: 110 },
  { title: t('jxc.field.brand'), key: 'brand', width: 100 },
  { title: t('jxc.field.spec'), dataIndex: 'spec', key: 'spec', width: 130, ellipsis: true },
  { title: t('jxc.field.unit'), dataIndex: 'unit', key: 'unit', width: 64 },
  q('stockQty', 'jxc.overstock.stockQty', { tip: t('jxc.overstock.stockQtyTip') }),
  { title: t('jxc.overstock.stockDays'), dataIndex: 'stockDays', key: 'stockDays', width: 100, align: 'right', tip: t('jxc.overstock.stockDaysTip') },
  q('baseQty', 'jxc.overstock.baseQty'),
  { title: t('jxc.overstock.ratio'), key: 'ratio', width: 104, align: 'right', tip: t('jxc.overstock.ratioTip') },
  q('overQty', 'jxc.overstock.overQty', { tip: t('jxc.overstock.overQtyTip') }),
  ...(result.value.costVisible ? [{ title: t('jxc.overstock.amount'), key: 'amount', width: 120, align: 'right', tip: t('jxc.overstock.amountTip') }] : []),
  ...(canHistory ? [actionColumn(2)] : [])
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))

const tip = computed(() => {
  const r = result.value
  const base = r.salesBegin ? t('jxc.overstock.tipSales', { begin: r.salesBegin, end: r.salesEnd }) : t('jxc.overstock.tipMax')
  return `${base}${t('jxc.overstock.tipCommon')}`
})

function params() {
  const p = { ...query, keyword: query.keyword?.trim(), days: query.base === OverstockBase.RECENT_DAYS ? query.days : undefined }
  return Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await jxcOverstock({ ...params(), current: pagination.current, size: pagination.pageSize })
    if (current !== seq) return
    rows.value = res.records
    pagination.total = res.total
    summary.value = res.summary
    result.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

function search() {
  pagination.current = 1
  load()
}

function onChange(p) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}

/** 合计行：第一列显示「合计 · N 个商品」，数量与资金占用为全部积压商品的合计 */
function summaryCell(c) {
  const s = summary.value
  if (c.key === 'createDate') return `${t('jxc.common.total')} · ${t('jxc.overstock.count', { n: s.materialCount || 0 })}`
  if (['stockQty', 'baseQty', 'overQty'].includes(c.key)) return qty(s[c.key])
  if (c.key === 'amount') return money(s.amount)
  return ''
}

// 联查：经营历史（库存流水的进出与结余）、库存分布（各仓库、库位、批次）
const historyRef = ref()
const stockRef = ref()
function openHistory(r) {
  historyRef.value.open({ materialId: r.materialId, materialCode: r.code, materialName: r.name, warehouseId: query.warehouseId })
}
const loadStock = async (materialId) => (await pageStock({ materialId, current: 1, size: 500 })).records

async function exportRows() {
  exporting.value = true
  try {
    const all = []
    for (let page = 1; all.length < Math.min(pagination.total, EXPORT_MAX); page++) {
      const res = await jxcOverstock({ ...params(), current: page, size: EXPORT_PAGE })
      all.push(...res.records)
      if (res.records.length < EXPORT_PAGE) break
    }
    const cols = columns.value.filter((c) => c.key !== 'action')
    const cell = (c, r) => {
      if (c.key === 'category') return dictLabel(dict.wms_material_category, r.category)
      if (c.key === 'brand') return dictLabel(dict.wms_material_brand, r.brand)
      const v = r[c.key]
      if (c.date || ['code', 'name', 'spec', 'unit'].includes(c.key)) return v || ''
      return v === null || v === undefined ? '' : Number(v)
    }
    const data = all.map((r) => cols.map((c) => cell(c, r)))
    data.push(cols.map((c) => {
      const v = summaryCell(c)
      return ['stockQty', 'baseQty', 'overQty', 'amount'].includes(c.key) ? Number(summary.value[c.key]) : v
    }))
    await writeExcel(t('jxc.overstock.exportName', { date: query.date }), cols.map((c) => ({ title: c.title, width: Math.max(10, Math.round((c.width || 100) / 8)) })), data,
      { withStamp: false })
  } finally {
    exporting.value = false
  }
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
.filter {
  width: 220px;
}
.filter-s {
  width: 150px;
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
  .filter,
  .filter-s {
    width: 100%;
  }
  .head {
    flex-wrap: wrap;
  }
}
</style>
