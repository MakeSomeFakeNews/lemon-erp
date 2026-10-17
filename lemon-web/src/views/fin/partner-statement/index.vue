<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="filters">
        <a-radio-group v-model:value="kind" button-style="solid" @change="onKindChange">
          <a-radio-button value="AR">客户（应收）</a-radio-button>
          <a-radio-button value="AP">供应商（应付）</a-radio-button>
        </a-radio-group>
        <PartnerSelect
          :key="kind"
          v-model:value="partnerId"
          :type="kind === 'AR' ? 'customer' : 'supplier'"
          :placeholder="kind === 'AR' ? '选择客户' : '选择供应商'"
          class="partner"
        />
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-button type="primary" :disabled="!partnerId" @click="load">查询</a-button>
        <a-button :disabled="!data" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
        <span class="sub-text">{{ labels.bill }}增加余额，{{ labels.settle }}和{{ labels.advance }}减少余额；{{ labels.advance }}在收付时即冲减</span>
      </div>

      <template v-if="data">
        <a-row :gutter="12" class="cards">
          <a-col :xs="12" :md="6"><a-statistic title="期初余额" :value="Number(data.opening)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="`本期${labels.bill}`" :value="Number(data.increase)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="`本期${labels.settle} / ${labels.advance}`" :value="Number(data.decrease)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="期末余额" :value="Number(data.closing)" :precision="2" prefix="¥" /></a-col>
        </a-row>
      </template>

      <a-table
        :row-key="(r, i) => i"
        size="small"
        bordered
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="false"
        :row-class-name="(r) => (r.type === 'OPENING' ? 'total-row' : '')"
        :locale="{ emptyText: partnerId ? '没有数据' : `请选择${kind === 'AR' ? '客户' : '供应商'}后查询` }"
        :scroll="{ x: 1100, y: '100%' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">{{ typeText(record) }}</template>
          <template v-else-if="column.key === 'source'">{{ sourceText(record) }}</template>
          <template v-else-if="['increase', 'decrease'].includes(column.key)">
            <span :class="{ neg: Number(record[column.key]) < 0 }">{{ moneyOrBlank(record[column.key]) }}</span>
          </template>
          <template v-else-if="column.key === 'balance'">
            <span :class="['bold', { neg: Number(record.balance) < 0 }]">{{ formatMoney(record.balance) }}</span>
          </template>
        </template>
        <template v-if="data" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell :col-span="4">本期合计</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.increase) }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.decrease) }}</a-table-summary-cell>
              <a-table-summary-cell align="right">{{ formatMoney(data.closing) }}</a-table-summary-cell>
              <a-table-summary-cell />
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { PrinterOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { partnerStatement } from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { printDocs } from '@/utils/print'

/** 往来对账单：客户 / 供应商在期间内的应收应付、收付款、预收预付和逐笔余额，可打印给对方核对 */
const dict = useDict('fin_bill_source', 'fin_settle_method')
const kind = ref('AR')
const partnerId = ref()
const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref()
const loading = ref(false)

const labels = computed(() =>
  kind.value === 'AR'
    ? { partner: '客户', bill: '应收', settle: '收款', advance: '预收款' }
    : { partner: '供应商', bill: '应付', settle: '付款', advance: '预付款' }
)
const columns = computed(() => [
  { title: '日期', dataIndex: 'bizDate', width: 110 },
  { title: '类型', key: 'type', width: 100 },
  { title: '单号', dataIndex: 'docNo', width: 160 },
  { title: '来源 / 结算方式', key: 'source', width: 200, ellipsis: true },
  { title: `${labels.value.bill}金额`, key: 'increase', width: 130, align: 'right' },
  { title: `${labels.value.settle}金额`, key: 'decrease', width: 130, align: 'right' },
  { title: '余额', key: 'balance', width: 140, align: 'right' },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
])
const rows = computed(() =>
  data.value ? [{ type: 'OPENING', bizDate: range.value[0], balance: data.value.opening }, ...data.value.rows] : []
)

function typeText(r) {
  const l = labels.value
  return { OPENING: '期初余额', BILL: `${l.bill}单`, SETTLEMENT: l.settle, ADVANCE: l.advance }[r.type]
}
function sourceText(r) {
  if (r.type === 'OPENING') return ''
  if (r.type === 'BILL') {
    const src = findDict(dict.fin_bill_source, r.sourceType)?.label || r.sourceType || ''
    return [src, r.sourceNo].filter(Boolean).join(' ')
  }
  return findDict(dict.fin_settle_method, r.sourceType)?.label || r.sourceType || ''
}

let seq = 0
async function load() {
  if (!partnerId.value) return
  const current = ++seq
  loading.value = true
  try {
    const res = await partnerStatement({ kind: kind.value, partnerId: partnerId.value, begin: range.value[0], end: range.value[1] })
    if (current === seq) data.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}
function onKindChange() {
  partnerId.value = undefined
  data.value = undefined
}
watch([partnerId, range], load)

function print() {
  const d = data.value
  const l = labels.value
  printDocs({
    title: `${l.partner}对账单`,
    fields: [
      [l.partner, d.partnerName],
      ['对账期间', `${range.value[0]} 至 ${range.value[1]}`],
      ['期初余额', formatMoney(d.opening)],
      ['期末余额', formatMoney(d.closing)]
    ],
    columns: [
      { title: '日期', key: 'bizDate' },
      { title: '类型', key: 'type', value: typeText },
      { title: '单号', key: 'docNo' },
      { title: '来源 / 结算方式', key: 'source', value: sourceText },
      { title: '增加金额', key: 'increase', value: (r) => moneyOrBlank(r.increase), align: 'right' },
      { title: '减少金额', key: 'decrease', value: (r) => moneyOrBlank(r.decrease), align: 'right' },
      { title: '余额', key: 'balance', value: (r) => formatMoney(r.balance), align: 'right' },
      { title: '备注', key: 'remark' }
    ],
    rows: rows.value,
    totals: { bizDate: '本期合计', increase: formatMoney(d.increase), decrease: formatMoney(d.decrease), balance: formatMoney(d.closing) },
    signs: ['制表', '财务审核', `${l.partner}确认（盖章）`],
    note: `请贵方核对以上往来明细，如无误请签字盖章后回传；如有差异请注明。`
  }, `${l.partner}对账单 ${d.partnerName}`, 'fin.partnerStatement')
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.partner {
  width: 240px;
}
.cards {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: var(--status-critical);
}
.bold {
  font-weight: 500;
}
:deep(.total-row) td {
  font-weight: 500;
  background: var(--app-bg-2, #fafafa);
}
</style>
