<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template #extra>
      <a-space>
        <a-tooltip :title="$t('jxc.adjust.tip')">
          <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="reload">
            <a-radio-button :value="AdjustMode.BOTTOM">{{ $t('jxc.adjust.BOTTOM') }}</a-radio-button>
            <a-radio-button :value="AdjustMode.ALLOCATE">{{ $t('jxc.adjust.ALLOCATE') }}</a-radio-button>
          </a-radio-group>
        </a-tooltip>
        <a-button size="small" :loading="exporting" @click="exportRows">
          <template #icon><DownloadOutlined /></template>{{ $t('jxc.common.exportDetail') }}
        </a-button>
      </a-space>
    </template>
    <div class="sub-text range">{{ params.begin }} ~ {{ params.end }} · {{ modeTip }}</div>
    <a-table
      row-key="key"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      @change="onChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'docNo'">
          <a @click="openDoc(record)">{{ record.docNo }}</a>
          <DictTag type="scm_trade_kind" :value="record.kind" class="kind" />
        </template>
        <template v-else-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div v-if="record.spec" class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'category'">{{ dictLabel(dict.wms_material_category, record.category) }}</template>
        <template v-else-if="column.key === 'brand'">{{ dictLabel(dict.wms_material_brand, record.brand) }}</template>
        <template v-else-if="column.key === 'qty'">
          <span :class="{ neg: Number(record.qty) < 0 }">{{ qty(record.qty) }}</span> {{ record.unit }}
        </template>
        <template v-else-if="column.key === 'price'">{{ price(record.price) }}</template>
        <template v-else-if="column.key === 'cost'">
          {{ money(record.cost) }}
          <a-tooltip v-if="!Number(record.costed)" :title="$t('jxc.common.refCostTip')">
            <a-tag color="orange" :bordered="false" class="hint">{{ $t('jxc.common.refCost') }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.money">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
        </template>
      </template>
      <template v-if="rows.length" #summary>
        <a-table-summary>
          <!-- 调整行（汇总到底部）：全部明细的合计，金额为负、成本 0、毛利为负，退货方向相反 -->
          <a-table-summary-row v-for="a in adjustments" :key="a.item" class="adjust-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="c.key === 'material'">{{ adjustLabel(a.item) }}</template>
              <template v-else-if="c.key === mainKey">
                <span :class="{ neg: Number(a.amount) < 0 }">{{ money(a.amount) }}</span>
              </template>
              <template v-else-if="c.key === 'taxedAmount' && a.taxedAmount != null">
                <span :class="{ neg: Number(a.taxedAmount) < 0 }">{{ money(a.taxedAmount) }}</span>
              </template>
              <template v-else-if="c.key === 'cost' && a.cost != null">{{ money(a.cost) }}</template>
              <template v-else-if="c.key === 'profit' && a.profit != null">
                <span :class="{ neg: Number(a.profit) < 0 }">{{ money(a.profit) }}</span>
              </template>
            </a-table-summary-cell>
          </a-table-summary-row>
          <a-table-summary-row class="total-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="c.key === 'bizDate'">{{ $t('jxc.common.total') }}</template>
              <template v-else-if="c.key === 'qty'">{{ qty(summary.netQty) }}</template>
              <template v-else-if="c.money || c.key === 'cost'">
                <span :class="{ neg: Number(summary[totalKey(c.key)]) < 0 }">{{ money(summary[totalKey(c.key)]) }}</span>
              </template>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
    <TradeDocDetail v-model:open="docOpen" :kind="docKind" :doc-id="docId" />
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import TradeDocDetail from '@/components/scm/TradeDocDetail.vue'
import { jxcDetail, jxcExport } from '@/api/bi/jxc'
import { useDict } from '@/composables/useDict'
import { useAdjustMode } from '@/composables/useAdjustMode'
import { money, price, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { AdjustItem, AdjustMode } from '@/constants/enums'
import { DICT_TYPES, TRADE_PATHS, dictLabel } from './jxc'
import { t } from '@/locales'

/**
 * 进销存报表的明细子页：按汇总的筛选条件 + 穿透的维度取值列出出入库单明细（退货为负数），点击单据号打开单据详情。
 * 调整项（整单优惠、本公司运费、收付款折让、坏账）两种显示方式（issue #93）：汇总到底部（行上为优惠前的货款，底部按类别列出调整行）
 * 或分摊到各行；合计都等于汇总行。用法：ref.open({ params, dim, key, label })，params 为汇总页的查询条件（含日期范围）
 */
const props = defineProps({
  report: { type: String, required: true }
})

const dict = useDict(...DICT_TYPES, 'scm_trade_kind')
const sales = computed(() => props.report === 'sales')
const mode = useAdjustMode()
const bottom = computed(() => mode.value === AdjustMode.BOTTOM)
const open = ref(false)
const label = ref('')
const params = ref({})
const rows = ref([])
const adjustments = ref([])
const summary = ref({})
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const title = computed(() => (label.value ? t('jxc.detail.title', { name: label.value }) : t('jxc.detail.titleAll')))
const modeTip = computed(() => t(`jxc.adjust.${sales.value ? 'sales' : 'purchase'}${bottom.value ? 'BottomTip' : 'AllocateTip'}`))

/** 汇总到底部时承载调整行金额的列：销售为销售金额，进货为扣折让后金额 */
const mainKey = computed(() => (sales.value ? 'salesAmount' : 'amountAfterDiscount'))
/** 合计行取汇总的字段（与明细的字段名不同的） */
const TOTAL_KEYS = { amount: 'netAmount', amountAfterDiscount: 'netAfterDiscount' }
const totalKey = (k) => TOTAL_KEYS[k] || k
const adjustLabel = (item) => (item === AdjustItem.SETTLE_DISCOUNT && !sales.value ? t('jxc.adjust.PAYMENT_DISCOUNT') : t(`jxc.adjust.${item}`))

/** 金额列（按报表和显示方式），合计行、导出共用 */
const moneyColumns = computed(() => {
  const m = (key, titleKey, width = 120) => ({ title: t(titleKey), key, width, align: 'right', money: true })
  if (bottom.value) {
    return sales.value
      ? [m('taxedAmount', 'jxc.field.grossTaxedAmount', 130), m('salesAmount', 'jxc.field.salesAmount'),
          { title: t('jxc.field.cost'), key: 'cost', width: 160, align: 'right' }, m('profit', 'jxc.field.profit')]
      : [m('taxedAmount', 'jxc.field.grossTaxedAmount', 130), m('amountAfterDiscount', 'jxc.field.amount')]
  }
  return sales.value
    ? [m('goodsAmount', 'jxc.field.goodsAmount'), m('discount', 'jxc.field.discount', 110), m('amount', 'jxc.field.netAmount'),
        m('taxedAmount', 'jxc.field.taxedAmount'), m('freight', 'jxc.field.freight', 110), m('settleDiscount', 'jxc.field.receiptDiscount', 110),
        m('badDebt', 'jxc.field.badDebt', 110), m('salesAmount', 'jxc.field.salesAmount'),
        { title: t('jxc.field.cost'), key: 'cost', width: 160, align: 'right' }, m('profit', 'jxc.field.profit')]
    : [m('goodsAmount', 'jxc.field.goodsAmount'), m('discount', 'jxc.field.discount', 110), m('amount', 'jxc.field.netAmount'),
        m('taxedAmount', 'jxc.field.taxedAmount'), m('settleDiscount', 'jxc.field.purchaseDiscount', 110),
        m('amountAfterDiscount', 'jxc.field.amountAfterDiscount', 140)]
})

const columns = computed(() => [
  { title: t('jxc.field.bizDate'), dataIndex: 'bizDate', key: 'bizDate', width: 105, fixed: 'left' },
  { title: t('jxc.field.docNo'), key: 'docNo', width: 210, fixed: 'left' },
  { title: t(sales.value ? 'jxc.field.customer' : 'jxc.field.supplier'), dataIndex: 'partnerName', key: 'partnerName', width: 170, ellipsis: true },
  ...(sales.value ? [{ title: t('jxc.field.salesman'), dataIndex: 'salesmanName', key: 'salesmanName', width: 100, ellipsis: true }] : []),
  { title: t('jxc.field.material'), key: 'material', width: 220, ellipsis: true },
  { title: t('jxc.field.category'), key: 'category', width: 100 },
  { title: t('jxc.field.brand'), key: 'brand', width: 100 },
  { title: t('jxc.field.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 110, ellipsis: true },
  { title: t('jxc.field.qty'), key: 'qty', width: 110, align: 'right' },
  { title: t(bottom.value ? 'jxc.field.grossPrice' : 'jxc.field.price'), key: 'price', width: 110, align: 'right' },
  ...moneyColumns.value,
  { title: t('jxc.field.remark'), dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true }
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + c.width, 0))

/** 去掉空值，避免把空字符串当作筛选条件 */
const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''))

function query() {
  return clean({ ...params.value, adjustMode: mode.value, current: pagination.current, size: pagination.pageSize })
}

async function load() {
  loading.value = true
  try {
    const res = await jxcDetail(props.report, query())
    rows.value = res.records.map((r, i) => ({ ...r, key: `${r.docId}-${i}-${pagination.current}` }))
    adjustments.value = res.adjustments || []
    summary.value = res.summary || {}
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.current = 1
  load()
}

function onChange(p) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}

/**
 * @param {{ params: object, dim?: string, key?: string|number|null, label?: string }} opts
 * key 为空（未设置）时也要传 dim
 */
function openDetail({ params: p, dim, key, label: l }) {
  params.value = { ...clean(p), dim: dim || undefined, key: dim ? (key ?? '') : undefined }
  if (!dim) delete params.value.key
  label.value = l || ''
  pagination.current = 1
  rows.value = []
  adjustments.value = []
  summary.value = {}
  open.value = true
  load()
}

const docOpen = ref(false)
const docId = ref()
/** 详情组件常驻（打开时由 open 的变化触发加载），种类随所点单据切换 */
const docKind = ref(props.report === 'sales' ? 'salesOut' : 'purchaseIn')
function openDoc(r) {
  docKind.value = TRADE_PATHS[r.kind]
  docId.value = r.docId
  docOpen.value = true
}

/** 导出与页面一致：明细行、调整行（汇总到底部时）、合计行 */
async function exportRows() {
  exporting.value = true
  try {
    const p = { ...params.value, adjustMode: mode.value }
    const res = await jxcExport(props.report, clean(p))
    const kindLabel = (k) => findDict(dict.scm_trade_kind, k)?.label || k
    const s = sales.value
    const cols = moneyColumns.value
    const headers = [
      t('jxc.field.bizDate'), t('jxc.field.docNo'), t('jxc.field.kind'), t(s ? 'jxc.field.customer' : 'jxc.field.supplier'),
      ...(s ? [t('jxc.field.salesman')] : []),
      t('common.field.code'), t('jxc.field.material'), t('jxc.field.spec'), t('jxc.field.category'), t('jxc.field.brand'),
      t('jxc.field.warehouse'), t('jxc.field.qty'), t('jxc.field.unit'), t(bottom.value ? 'jxc.field.grossPrice' : 'jxc.field.price'),
      ...cols.map((c) => c.title),
      t('jxc.field.remark')
    ].map((title) => ({ title }))
    const num = (v) => (v === null || v === undefined ? '' : Number(v))
    const lead = s ? 11 : 10
    const data = res.rows.map((r) => [
      r.bizDate, r.docNo, kindLabel(r.kind), r.partnerName,
      ...(s ? [r.salesmanName] : []),
      r.materialCode, r.materialName, r.spec, dictLabel(dict.wms_material_category, r.category),
      dictLabel(dict.wms_material_brand, r.brand), r.warehouseName, num(r.qty), r.unit, num(r.price),
      ...cols.map((c) => num(r[c.key])),
      r.remark
    ])
    // 调整行：商品列为类别名称，只有金额（整单优惠另有价税合计）、成本、毛利
    for (const a of res.adjustments || []) {
      const values = { [mainKey.value]: a.amount, taxedAmount: a.taxedAmount, cost: a.cost, profit: a.profit }
      const head = Array(lead).fill('')
      head[s ? 6 : 5] = adjustLabel(a.item)
      data.push([...head, '', '', '', ...cols.map((c) => num(values[c.key])), ''])
    }
    const sum = res.summary || {}
    const totalHead = Array(lead).fill('')
    totalHead[0] = t('jxc.common.total')
    data.push([...totalHead, num(sum.netQty), '', '', ...cols.map((c) => num(sum[totalKey(c.key)])), ''])
    const name = t(s ? 'jxc.detail.salesName' : 'jxc.detail.purchaseName') + (label.value ? `_${label.value}` : '')
    await writeExcel(name, headers, data)
    if (res.truncated) message.warning(t('jxc.common.exportTruncated', { n: res.rows.length }))
  } finally {
    exporting.value = false
  }
}

defineExpose({ open: openDetail })
</script>

<style scoped>
.range {
  margin-bottom: 8px;
  line-height: 1.6;
}
.kind {
  margin-left: 6px;
}
.hint {
  margin-left: 6px;
  margin-right: 0;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.adjust-row) {
  color: var(--app-text-2);
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
