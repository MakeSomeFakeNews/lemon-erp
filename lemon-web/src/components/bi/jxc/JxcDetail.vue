<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template #extra>
      <a-button size="small" :loading="exporting" @click="exportRows">
        <template #icon><DownloadOutlined /></template>{{ $t('jxc.common.exportDetail') }}
      </a-button>
    </template>
    <div class="sub-text range">{{ params.begin }} ~ {{ params.end }}</div>
    <a-table
      row-key="key"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: sales ? 2060 : 1300 }"
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
        <template v-else-if="['amount', 'taxedAmount', 'profit', 'badDebt', 'freight', 'profitAfterBadDebt', 'profitAfterFreight'].includes(column.key)">
          <span :class="{ neg: Number(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
        </template>
        <template v-else-if="column.key === 'cost'">
          {{ money(record.cost) }}
          <a-tooltip v-if="!Number(record.costed)" :title="$t('jxc.common.refCostTip')">
            <a-tag color="orange" :bordered="false" class="hint">{{ $t('jxc.common.refCost') }}</a-tag>
          </a-tooltip>
        </template>
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
import { money, price, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { DICT_TYPES, TRADE_PATHS, dictLabel } from './jxc'
import { t } from '@/locales'

/**
 * 进销存报表的明细子页：按汇总的筛选条件 + 穿透的维度取值列出出入库单明细（退货为负数），点击单据号打开单据详情。
 * 用法：ref.open({ params, dim, key, label })，params 为汇总页的查询条件（含日期范围）
 */
const props = defineProps({
  report: { type: String, required: true }
})

const dict = useDict(...DICT_TYPES, 'scm_trade_kind')
const sales = computed(() => props.report === 'sales')
const open = ref(false)
const label = ref('')
const params = ref({})
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const title = computed(() => (label.value ? t('jxc.detail.title', { name: label.value }) : t('jxc.detail.titleAll')))

const columns = computed(() => [
  { title: t('jxc.field.bizDate'), dataIndex: 'bizDate', width: 105, fixed: 'left' },
  { title: t('jxc.field.docNo'), key: 'docNo', width: 210, fixed: 'left' },
  { title: t(sales.value ? 'jxc.field.customer' : 'jxc.field.supplier'), dataIndex: 'partnerName', width: 170, ellipsis: true },
  ...(sales.value ? [{ title: t('jxc.field.salesman'), dataIndex: 'salesmanName', width: 100, ellipsis: true }] : []),
  { title: t('jxc.field.material'), key: 'material', width: 220, ellipsis: true },
  { title: t('jxc.field.category'), key: 'category', width: 100 },
  { title: t('jxc.field.brand'), key: 'brand', width: 100 },
  { title: t('jxc.field.warehouse'), dataIndex: 'warehouseName', width: 110, ellipsis: true },
  { title: t('jxc.field.qty'), key: 'qty', width: 110, align: 'right' },
  { title: t('jxc.field.price'), key: 'price', width: 100, align: 'right' },
  { title: t('jxc.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('jxc.field.taxedAmount'), key: 'taxedAmount', width: 120, align: 'right' },
  ...(sales.value
    ? [
        { title: t('jxc.field.cost'), key: 'cost', width: 160, align: 'right' },
        { title: t('jxc.field.profit'), key: 'profit', width: 110, align: 'right' },
        { title: t('jxc.field.badDebt'), key: 'badDebt', width: 110, align: 'right' },
        { title: t('jxc.field.profitAfterBadDebt'), key: 'profitAfterBadDebt', width: 130, align: 'right' },
        { title: t('jxc.field.freight'), key: 'freight', width: 110, align: 'right' },
        { title: t('jxc.field.profitAfterFreight'), key: 'profitAfterFreight', width: 150, align: 'right' }
      ]
    : []),
  { title: t('jxc.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true }
])

/** 去掉空值，避免把空字符串当作筛选条件 */
const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''))

function query() {
  return clean({ ...params.value, current: pagination.current, size: pagination.pageSize })
}

async function load() {
  loading.value = true
  try {
    const res = await jxcDetail(props.report, query())
    rows.value = res.records.map((r, i) => ({ ...r, key: `${r.docId}-${i}-${pagination.current}` }))
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
function openDetail({ params: p, dim, key, label: l }) {
  params.value = { ...clean(p), dim: dim || undefined, key: dim ? (key ?? '') : undefined }
  if (!dim) delete params.value.key
  label.value = l || ''
  pagination.current = 1
  rows.value = []
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

async function exportRows() {
  exporting.value = true
  try {
    const p = { ...params.value }
    delete p.current
    delete p.size
    const res = await jxcExport(props.report, clean(p))
    const kindLabel = (k) => findDict(dict.scm_trade_kind, k)?.label || k
    const headers = [
      t('jxc.field.bizDate'), t('jxc.field.docNo'), t('jxc.field.kind'), t(sales.value ? 'jxc.field.customer' : 'jxc.field.supplier'),
      ...(sales.value ? [t('jxc.field.salesman')] : []),
      t('common.field.code'), t('jxc.field.material'), t('jxc.field.spec'), t('jxc.field.category'), t('jxc.field.brand'),
      t('jxc.field.warehouse'), t('jxc.field.qty'), t('jxc.field.unit'), t('jxc.field.price'), t('jxc.field.amount'), t('jxc.field.taxedAmount'),
      ...(sales.value ? [t('jxc.field.cost'), t('jxc.field.profit'), t('jxc.field.badDebt'), t('jxc.field.profitAfterBadDebt'),
        t('jxc.field.freight'), t('jxc.field.profitAfterFreight')] : []),
      t('jxc.field.remark')
    ].map((title) => ({ title }))
    const data = res.rows.map((r) => [
      r.bizDate, r.docNo, kindLabel(r.kind), r.partnerName,
      ...(sales.value ? [r.salesmanName] : []),
      r.materialCode, r.materialName, r.spec, dictLabel(dict.wms_material_category, r.category), dictLabel(dict.wms_material_brand, r.brand),
      r.warehouseName, Number(r.qty), r.unit, Number(r.price), Number(r.amount), Number(r.taxedAmount),
      ...(sales.value ? [Number(r.cost), Number(r.profit), Number(r.badDebt), Number(r.profitAfterBadDebt), Number(r.freight),
        Number(r.profitAfterFreight)] : []),
      r.remark
    ])
    const name = t(sales.value ? 'jxc.detail.salesName' : 'jxc.detail.purchaseName') + (label.value ? `_${label.value}` : '')
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
</style>
