<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab" class="tabs" @change="search">
        <a-tab-pane v-for="k in TABS" :key="k" :tab="$t(`jxc.goods.${k}`)" />
      </a-tabs>
      <div class="filters">
        <a-range-picker v-if="tab !== 'shortage'" v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="search" />
        <MaterialCategorySelect v-model="query.category" :placeholder="$t('jxc.common.allCategories')" class="filter-s" />
        <a-select v-model:value="query.brand" :options="toOptions(dict.wms_material_brand)" allow-clear
          :placeholder="$t('jxc.common.allBrands')" class="filter-s" />
        <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('jxc.common.allWarehouses')" class="filter-s" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('jxc.common.materialKeyword')" class="filter" @press-enter="search" />
        <a-radio-group v-if="tab === 'top'" v-model:value="query.rankBy" button-style="solid" @change="search">
          <a-radio-button :value="GoodsRankBy.AMOUNT">{{ $t('jxc.goods.rankAmount') }}</a-radio-button>
          <a-radio-button :value="GoodsRankBy.QTY">{{ $t('jxc.goods.rankQty') }}</a-radio-button>
          <a-radio-button :value="GoodsRankBy.PROFIT">{{ $t('jxc.goods.rankProfit') }}</a-radio-button>
        </a-radio-group>
        <span v-if="tab === 'slow'" class="inline">
          {{ $t('jxc.goods.slowQty') }}
          <a-input-number v-model:value="query.slowQty" :min="0" :precision="4" class="num" />
        </span>
        <span v-if="tab === 'overstock'" class="inline">
          {{ $t('jxc.goods.overstockDays') }}
          <a-input-number v-model:value="query.overstockDays" :min="1" :precision="0" class="num" /> {{ $t('jxc.goods.days') }}
        </span>
        <a-button type="primary" :loading="loading" @click="search">{{ $t('common.action.query') }}</a-button>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="head">
        <span class="sub-text">{{ $t(`jxc.goods.${tab}Tip`) }}</span>
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
        :scroll="{ x: 1200, y: '100%' }"
        @change="onChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a v-if="canDrill" @click="drill(record)">{{ record.code }} {{ record.name }}</a>
            <span v-else>{{ record.code }} {{ record.name }}</span>
            <div v-if="record.spec" class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'category'">{{ dictLabel(dict.wms_material_category, record.category) }}</template>
          <template v-else-if="column.key === 'brand'">{{ dictLabel(dict.wms_material_brand, record.brand) }}</template>
          <template v-else-if="column.key === 'lastSaleDate'">{{ record.lastSaleDate || $t('jxc.goods.never') }}</template>
          <template v-else-if="column.key === 'gapQty'"><b class="neg">{{ qty(record.gapQty) }}</b></template>
          <template v-else-if="column.key === 'margin'">{{ marginText(record.margin) }}</template>
          <template v-else-if="column.qty">{{ qty(record[column.key]) }} <span v-if="column.unit" class="sub-text">{{ record.unit }}</span></template>
          <template v-else-if="column.money">
            <span :class="{ neg: Number(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.ratio">{{ record[column.key] ?? '-' }}</template>
        </template>
      </a-table>
    </a-card>

    <JxcDetail ref="detailRef" report="sales" />
  </div>
</template>

<script setup>
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import JxcDetail from '@/components/bi/jxc/JxcDetail.vue'
import { jxcGoods } from '@/api/bi/jxc'
import { DICT_TYPES, dictLabel, marginText, rangePresets, thisMonth } from '@/components/bi/jxc/jxc'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { GoodsRankBy, JxcDimension } from '@/constants/enums'
import { t } from '@/locales'

/** 标签页 = 后端 /bi/jxc/goods/{type} */
const TABS = ['top', 'slow', 'overstock', 'shortage', 'turnover']
/** 导出时一次取的行数（分页上限） */
const EXPORT_SIZE = 500

const dict = useDict(...DICT_TYPES)
const canDrill = useUserStore().hasPerm('bi:jxc:sales')
const presets = rangePresets()
const tab = ref('top')
const range = ref(thisMonth())
const query = reactive({ category: undefined, brand: undefined, warehouseId: undefined, keyword: '', rankBy: GoodsRankBy.AMOUNT, slowQty: 0, overstockDays: 90 })
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const material = { title: t('jxc.field.material'), key: 'material', width: 240, fixed: 'left', ellipsis: true }
const category = { title: t('jxc.field.category'), key: 'category', width: 100 }
const brand = { title: t('jxc.field.brand'), key: 'brand', width: 100 }
const q = (key, label, extra = {}) => ({ title: t(label), key, width: 110, align: 'right', qty: true, ...extra })
const m = (key, label) => ({ title: t(label), key, width: 120, align: 'right', money: true })
const COLUMNS = {
  top: [material, category, brand, q('salesQty', 'jxc.goods.salesQty', { unit: true }), m('salesAmount', 'jxc.goods.salesAmount'),
    m('cost', 'jxc.field.cost'), m('profit', 'jxc.field.profit'), { title: t('jxc.field.margin'), key: 'margin', width: 90, align: 'right' },
    q('stockQty', 'jxc.goods.stockQty')],
  slow: [material, category, brand, q('stockQty', 'jxc.goods.stockQty', { unit: true }), m('stockAmount', 'jxc.goods.stockAmount'),
    q('salesQty', 'jxc.goods.salesQty'), m('salesAmount', 'jxc.goods.salesAmount'),
    { title: t('jxc.goods.lastSaleDate'), key: 'lastSaleDate', width: 110 }],
  overstock: [material, category, brand, q('stockQty', 'jxc.goods.stockQty', { unit: true }), m('stockAmount', 'jxc.goods.stockAmount'),
    q('salesQty', 'jxc.goods.salesQty'), { title: t('jxc.goods.coverDays'), key: 'coverDays', width: 100, align: 'right', ratio: true }],
  shortage: [material, category, brand, q('availableQty', 'jxc.goods.availableQty', { unit: true }), q('safetyStock', 'jxc.goods.safetyStock'),
    q('soOpen', 'jxc.goods.soOpen'), q('gapQty', 'jxc.goods.gapQty'), q('poOpen', 'jxc.goods.poOpen'), q('stockQty', 'jxc.goods.stockQty')],
  turnover: [material, category, brand, q('openQty', 'jxc.goods.openQty', { unit: true }), q('closeQty', 'jxc.goods.closeQty'),
    q('salesQty', 'jxc.goods.salesQty'), m('cost', 'jxc.field.cost'),
    { title: t('jxc.goods.turnoverTimes'), key: 'turnover', width: 100, align: 'right', ratio: true },
    { title: t('jxc.goods.turnoverDays'), key: 'turnoverDays', width: 100, align: 'right', ratio: true }]
}
const columns = computed(() => COLUMNS[tab.value])

function params() {
  const p = { begin: range.value[0], end: range.value[1], ...query, keyword: query.keyword?.trim() }
  return Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await jxcGoods(tab.value, { ...params(), current: pagination.current, size: pagination.pageSize })
    if (current !== seq) return
    rows.value = res.records
    pagination.total = res.total
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

const detailRef = ref()
function drill(r) {
  detailRef.value.open({ params: { begin: range.value[0], end: range.value[1], warehouseId: query.warehouseId }, dim: JxcDimension.MATERIAL,
    key: r.materialId, label: `${r.code} ${r.name}` })
}

async function exportRows() {
  exporting.value = true
  try {
    const res = await jxcGoods(tab.value, { ...params(), current: 1, size: EXPORT_SIZE })
    const cols = columns.value
    const headers = [{ title: t('common.field.code'), width: 18 }, { title: t('jxc.field.material'), width: 28 }, { title: t('jxc.field.spec') },
      ...cols.slice(1).map((c) => ({ title: c.title })), { title: t('jxc.field.unit') }]
    const cell = (c, r) => {
      if (c.key === 'category') return dictLabel(dict.wms_material_category, r.category)
      if (c.key === 'brand') return dictLabel(dict.wms_material_brand, r.brand)
      if (c.key === 'lastSaleDate') return r.lastSaleDate || ''
      const v = r[c.key]
      return v === null || v === undefined ? '' : Number(v)
    }
    const data = res.records.map((r) => [r.code, r.name, r.spec, ...cols.slice(1).map((c) => cell(c, r)), r.unit])
    await writeExcel(`${t('jxc.goods.' + tab.value)}_${range.value[0]}_${range.value[1]}`, headers, data, { withStamp: false })
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
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
.num {
  width: 110px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
