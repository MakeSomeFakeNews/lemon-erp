<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template #extra>
      <a-button size="small" :loading="exporting" @click="exportRows">
        <template #icon><DownloadOutlined /></template>{{ $t('mesStat.common.exportDetail') }}
      </a-button>
    </template>
    <div class="sub-text range">{{ params.begin }} ~ {{ params.end }}</div>
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      @change="onChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'product'">
          <div>{{ record.productCode }} {{ record.productName }}</div>
        </template>
        <template v-else-if="column.key === 'op'">{{ record.opSeq }} {{ record.opName }}</template>
        <template v-else-if="column.qty">{{ qty(record[column.key]) }}</template>
        <template v-else-if="column.key === 'pieceRate'">{{ record.pieceRate == null ? '-' : price(record.pieceRate) }}</template>
        <template v-else-if="column.key === 'pieceWage'">{{ record.pieceWage == null ? '-' : money(record.pieceWage) }}</template>
      </template>
    </a-table>
  </SubPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import { mesStatReports } from '@/api/mes/stat'
import { money, price, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { t } from '@/locales'

/**
 * 报工明细子页：人员计件、工序产量的汇总行穿透（或全部明细），按报工日期倒序；导出最多 EXPORT_LIMIT 行。
 * 用法：ref.open({ params, label })，params 为汇总页的查询条件（含日期范围）加穿透条件
 */
const EXPORT_LIMIT = 5000
const PAGE = 500

const open = ref(false)
const label = ref('')
const params = ref({})
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })

const title = computed(() => t('mesStat.detail.title', { name: label.value }))

const f = (key) => t(`mesStat.field.${key}`)
const columns = [
  { title: f('reportDate'), dataIndex: 'reportDate', width: 105, fixed: 'left' },
  { title: f('woNo'), dataIndex: 'woNo', width: 150 },
  { title: f('product'), key: 'product', width: 200, ellipsis: true },
  { title: f('op'), key: 'op', width: 130, ellipsis: true },
  { title: f('workCenter'), dataIndex: 'workCenterName', width: 120, ellipsis: true },
  { title: f('operator'), dataIndex: 'operatorName', width: 100, ellipsis: true },
  { title: f('goodQty'), key: 'goodQty', width: 100, align: 'right', qty: true },
  { title: f('scrapQty'), key: 'scrapQty', width: 90, align: 'right', qty: true },
  { title: f('hours'), key: 'hours', width: 90, align: 'right', qty: true },
  { title: f('pieceRate'), key: 'pieceRate', width: 100, align: 'right' },
  { title: f('pieceWage'), key: 'pieceWage', width: 100, align: 'right' },
  { title: f('equipment'), dataIndex: 'equipmentName', width: 120, ellipsis: true },
  { title: f('remark'), dataIndex: 'remark', width: 160, ellipsis: true }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

/** 去掉空值，避免把空字符串当作筛选条件 */
const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== ''))

async function load() {
  loading.value = true
  try {
    const res = await mesStatReports({ ...params.value, current: pagination.current, size: pagination.pageSize })
    rows.value = res.records
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

/** @param {{ params: object, label?: string }} opts */
function openDetail({ params: p, label: l }) {
  params.value = clean(p)
  label.value = l || ''
  pagination.current = 1
  rows.value = []
  open.value = true
  load()
}

async function exportRows() {
  exporting.value = true
  try {
    const all = []
    let total = 0
    for (let page = 1; all.length < EXPORT_LIMIT; page++) {
      const res = await mesStatReports({ ...params.value, current: page, size: PAGE })
      total = res.total
      all.push(...res.records)
      if (res.records.length < PAGE) break
    }
    const list = all.slice(0, EXPORT_LIMIT)
    const headers = [
      f('reportDate'), f('woNo'), t('common.field.code'), f('product'), f('opSeq'), f('op'), f('workCenter'), f('operator'),
      f('goodQty'), f('scrapQty'), f('hours'), f('pieceRate'), f('pieceWage'), f('equipment'), f('remark')
    ].map((title) => ({ title }))
    const num = (v) => (v === null || v === undefined ? '' : Number(v))
    const data = list.map((r) => [
      r.reportDate, r.woNo, r.productCode, r.productName, r.opSeq, r.opName, r.workCenterName, r.operatorName,
      num(r.goodQty), num(r.scrapQty), num(r.hours), num(r.pieceRate), num(r.pieceWage), r.equipmentName, r.remark
    ])
    await writeExcel(t('mesStat.detail.fileName') + (label.value ? `_${label.value}` : ''), headers, data)
    if (total > list.length) message.warning(t('mesStat.common.exportTruncated', { n: list.length }))
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
