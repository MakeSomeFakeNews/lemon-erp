<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template #extra>
      <a-button size="small" :loading="exporting" @click="exportRows">
        <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
      </a-button>
    </template>
    <div class="sub-text range">{{ params.begin }} ~ {{ params.end }} · {{ $t('jxc.docs.tip') }}</div>
    <a-table
      row-key="docId"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      :custom-row="(r) => ({ onClick: () => openLines(r), class: 'click-row' })"
      @change="onChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'docNo'">
          <a>{{ record.docNo }}</a>
          <DictTag type="scm_trade_kind" :value="record.kind" class="kind" />
        </template>
        <template v-else-if="column.key === 'qty'">
          <span :class="{ neg: Number(record.qty) < 0 }">{{ qty(record.qty) }}</span>
        </template>
        <template v-else-if="column.money">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
        </template>
        <template v-else-if="column.key === 'margin'">
          <span :class="{ neg: Number(record.margin) < 0 }">{{ marginText(record.margin) }}</span>
        </template>
        <template v-else-if="column.key === 'cost'">
          {{ money(record.cost) }}
          <a-tooltip v-if="Number(record.missingCost) > 0" :title="$t('jxc.common.missingCostTip', { n: record.missingCost })">
            <a-tag color="orange" :bordered="false" class="hint">{{ $t('jxc.common.hasRefCost') }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <span @click.stop>
            <TableActions :actions="[
              { key: 'lines', label: $t('jxc.common.drillLines'), icon: UnorderedListOutlined, onClick: () => openLines(record) },
              { type: 'view', label: $t('jxc.common.viewDoc'), onClick: () => openDoc(record) }
            ]" />
          </span>
        </template>
      </template>
      <template v-if="rows.length" #summary>
        <a-table-summary>
          <a-table-summary-row class="total-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="c.key === 'bizDate'">{{ $t('jxc.common.total') }}</template>
              <template v-else-if="c.key === 'qty'">{{ qty(summary.netQty) }}</template>
              <template v-else-if="c.money || c.key === 'cost'">{{ money(summary[totalKey(c.key)]) }}</template>
              <template v-else-if="c.key === 'margin'">{{ marginText(summary.margin) }}</template>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
    <JxcDetail ref="detailRef" :report="report" />
    <TradeDocDetail v-model:open="docOpen" :kind="docKind" :doc-id="docId" />
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, UnorderedListOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import TradeDocDetail from '@/components/scm/TradeDocDetail.vue'
import JxcDetail from './JxcDetail.vue'
import { jxcDocs, jxcDocsExport } from '@/api/bi/jxc'
import { useDict } from '@/composables/useDict'
import { money, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { TRADE_PATHS, drillColumn, marginText } from './jxc'
import { t } from '@/locales'

/**
 * 进销存报表的单据列表子页：汇总项下的出入库单 / 退货单，每张一行（金额为该单中属于汇总项的明细合计，列出货款、整单优惠、运费、
 * 折让、坏账、销售金额、成本、毛利，退货为负数，合计与汇总行一致）；
 * 点击单据打开该单（在汇总项范围内）的明细子页，操作列可打开单据详情。
 * 用法：ref.open({ params, dim, key, label })，参数同 JxcDetail
 */
const props = defineProps({
  report: { type: String, required: true }
})

const dict = useDict('scm_trade_kind')
const sales = computed(() => props.report === 'sales')
const open = ref(false)
const label = ref('')
const params = ref({})
const rows = ref([])
const summary = ref({})
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const title = computed(() => (label.value ? t('jxc.docs.title', { name: label.value }) : t('jxc.docs.titleAll')))

/** 合计行取汇总的字段（与单据行的字段名不同的） */
const TOTAL_KEYS = { amount: 'netAmount', amountAfterDiscount: 'netAfterDiscount' }
const totalKey = (k) => TOTAL_KEYS[k] || k

/**
 * 金额列（issue #93 口径）：货款 − 整单优惠 = 净额；销售金额 = 净额 − 本公司运费 − 收款折让 − 坏账核销，毛利 = 销售金额 − 成本；
 * 进货：扣折让后金额 = 净额 − 付款折让
 */
const moneyColumns = computed(() => {
  const m = (key, titleKey, width = 120) => ({ title: t(titleKey), key, width, align: 'right', money: true })
  const head = [m('goodsAmount', 'jxc.field.goodsAmount'), m('discount', 'jxc.field.discount', 110), m('amount', 'jxc.field.netAmount'),
    m('taxedAmount', 'jxc.field.taxedAmount')]
  return sales.value
    ? [...head, m('freight', 'jxc.field.freight', 110), m('settleDiscount', 'jxc.field.receiptDiscount', 110), m('badDebt', 'jxc.field.badDebt', 110),
        m('salesAmount', 'jxc.field.salesAmount'), { title: t('jxc.field.cost'), key: 'cost', width: 160, align: 'right' },
        m('profit', 'jxc.field.profit'), { title: t('jxc.field.margin'), key: 'margin', width: 90, align: 'right' }]
    : [...head, m('settleDiscount', 'jxc.field.purchaseDiscount', 110), m('amountAfterDiscount', 'jxc.field.amountAfterDiscount', 140)]
})

const columns = computed(() => [
  { title: t('jxc.field.bizDate'), dataIndex: 'bizDate', key: 'bizDate', width: 105, fixed: 'left' },
  { title: t('jxc.field.docNo'), key: 'docNo', width: 250, fixed: 'left' },
  { title: t(sales.value ? 'jxc.field.customer' : 'jxc.field.supplier'), dataIndex: 'partnerName', key: 'partnerName', width: 170, ellipsis: true },
  ...(sales.value ? [{ title: t('jxc.field.salesman'), dataIndex: 'salesmanName', key: 'salesmanName', width: 100, ellipsis: true }] : []),
  { title: t('jxc.field.warehouse'), dataIndex: 'warehouseName', key: 'warehouseName', width: 110, ellipsis: true },
  { title: t('jxc.field.lineCount'), dataIndex: 'lineCount', key: 'lineCount', width: 70, align: 'right' },
  { title: t('jxc.field.qty'), key: 'qty', width: 100, align: 'right' },
  ...moneyColumns.value,
  { title: t('jxc.field.remark'), dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  drillColumn()
])
const scrollX = computed(() => columns.value.reduce((s, c) => s + c.width, 0))

/** 去掉空值，避免把空字符串当作筛选条件 */
const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''))

async function load() {
  loading.value = true
  try {
    const res = await jxcDocs(props.report, clean({ ...params.value, current: pagination.current, size: pagination.pageSize }))
    rows.value = res.records
    summary.value = res.summary || {}
    pagination.total = res.total
  } finally {
    loading.value = false
  }
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
function openDocs({ params: p, dim, key, label: l }) {
  params.value = { ...clean(p), dim: dim || undefined, key: dim ? (key ?? '') : undefined }
  if (!dim) delete params.value.key
  label.value = l || ''
  pagination.current = 1
  rows.value = []
  summary.value = {}
  open.value = true
  load()
}

/** 单据 → 该单的明细：沿用汇总项的条件（按类别等汇总时只看匹配的商品），再加单据 */
const detailRef = ref()
function openLines(r) {
  const { dim, key, ...rest } = params.value
  detailRef.value.open({
    params: { ...rest, docId: r.docId },
    dim,
    key,
    label: label.value ? t('jxc.docs.lineLabel', { name: label.value, docNo: r.docNo }) : r.docNo
  })
}

const docOpen = ref(false)
const docId = ref()
/** 详情组件常驻（打开时由 open 的变化触发加载），种类随所点单据切换 */
const docKind = ref(sales.value ? 'salesOut' : 'purchaseIn')
function openDoc(r) {
  docKind.value = TRADE_PATHS[r.kind]
  docId.value = r.docId
  docOpen.value = true
}

async function exportRows() {
  exporting.value = true
  try {
    const res = await jxcDocsExport(props.report, clean(params.value))
    const kindLabel = (k) => findDict(dict.scm_trade_kind, k)?.label || k
    const s = sales.value
    const cols = moneyColumns.value
    const headers = [
      t('jxc.field.bizDate'), t('jxc.field.docNo'), t('jxc.field.kind'), t(s ? 'jxc.field.customer' : 'jxc.field.supplier'),
      ...(s ? [t('jxc.field.salesman')] : []),
      t('jxc.field.warehouse'), t('jxc.field.lineCount'), t('jxc.field.qty'),
      ...cols.map((c) => c.title),
      t('jxc.field.remark')
    ].map((title) => ({ title }))
    const n = (v) => (v === null || v === undefined ? '' : Number(v))
    const data = res.rows.map((r) => [
      r.bizDate, r.docNo, kindLabel(r.kind), r.partnerName,
      ...(s ? [r.salesmanName] : []),
      r.warehouseName, n(r.lineCount), n(r.qty),
      ...cols.map((c) => n(r[c.key])),
      r.remark
    ])
    // 合计行（与页面一致，取全部单据的合计）
    const sum = res.summary || {}
    data.push([t('jxc.common.total'), '', '', '', ...(s ? [''] : []), '', '', n(sum.netQty), ...cols.map((c) => n(sum[totalKey(c.key)])), ''])
    const name = t(s ? 'jxc.docs.salesName' : 'jxc.docs.purchaseName') + (label.value ? `_${label.value}` : '')
    await writeExcel(name, headers, data)
    if (res.truncated) message.warning(t('jxc.common.exportTruncated', { n: res.rows.length }))
  } finally {
    exporting.value = false
  }
}

defineExpose({ open: openDocs })
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
:deep(.click-row) {
  cursor: pointer;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
