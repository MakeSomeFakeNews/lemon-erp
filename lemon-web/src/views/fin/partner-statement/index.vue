<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="filters">
        <a-radio-group v-model:value="kind" button-style="solid" @change="onKindChange">
          <a-radio-button value="AR">{{ $t('fin.partnerStatement.arKind') }}</a-radio-button>
          <a-radio-button value="AP">{{ $t('fin.partnerStatement.apKind') }}</a-radio-button>
        </a-radio-group>
        <PartnerSelect
          :key="kind"
          v-model:value="partnerId"
          :label="partnerName"
          :type="kind === BillKind.AR ? 'customer' : 'supplier'"
          :placeholder="kind === BillKind.AR ? $t('fin.partnerStatement.selectCustomer') : $t('fin.partnerStatement.selectSupplier')"
          class="partner"
        />
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
        <a-tooltip v-if="kind === BillKind.AR && canLinkAp" :title="$t('fin.linkedAp.statementTip')">
          <a-checkbox v-model:checked="withAp">{{ $t('fin.linkedAp.label') }}</a-checkbox>
        </a-tooltip>
        <a-button type="primary" :disabled="!partnerId" @click="search">{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!data" :loading="printing" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
        <a-button :disabled="!data" :loading="exporting" @click="exportAll"><template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}</a-button>
        <span class="sub-text">{{ kind === BillKind.AR ? $t('fin.partnerStatement.arTip') : $t('fin.partnerStatement.apTip') }}</span>
      </div>

      <template v-if="data">
        <a-alert v-if="data.linkedSupplierName" type="info" show-icon class="cards" :message="$t('fin.linkedAp.statementLinked', { name: data.linkedSupplierName })" />
        <a-row :gutter="12" class="cards">
          <a-col :xs="12" :md="6"><a-statistic :title="$t('fin.partnerStatement.opening')" :value="Number(data.opening)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="labels.periodIncrease" :value="Number(data.increase)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="labels.periodDecrease" :value="Number(data.decrease)" :precision="2" prefix="¥" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic :title="$t('fin.partnerStatement.closing')" :value="Number(data.closing)" :precision="2" prefix="¥" /></a-col>
        </a-row>
      </template>

      <a-table
        :row-key="(r, i) => i"
        size="small"
        bordered
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        :pagination="data ? pagination : false"
        :row-class-name="(r) => ([OPENING, CARRIED].includes(r.type) ? 'total-row' : '')"
        :locale="{ emptyText: partnerId ? $t('common.msg.noData') : kind === BillKind.AR ? $t('fin.partnerStatement.selectCustomerFirst') : $t('fin.partnerStatement.selectSupplierFirst') }"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">{{ ledgerTypeText(record, labels) }}</template>
          <template v-else-if="column.key === 'source'">{{ ledgerSourceText(record, dict) }}</template>
          <template v-else-if="column.key === 'increase'">
            <span :class="{ neg: Number(record.increase) < 0 }">{{ moneyOrBlank(record.increase) }}</span>
          </template>
          <template v-else-if="column.key === 'decrease'">
            <span :class="{ neg: Number(record.decrease) < 0 }">{{ moneyOrBlank(record.decrease) }}</span>
            <div v-if="Number(record.discount)" class="sub-text">{{ $t('fin.partnerStatement.discountOf', { amount: formatMoney(record.discount) }) }}</div>
          </template>
          <template v-else-if="column.key === 'balance'">
            <span :class="['bold', { neg: Number(record.balance) < 0 }]">{{ formatMoney(record.balance) }}</span>
          </template>
        </template>
        <template v-if="data" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell :col-span="4">{{ $t('fin.partnerStatement.periodTotal') }}</a-table-summary-cell>
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
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { partnerStatement } from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import {
  CARRIED, OPENING, exportLedger, ledgerColumns, ledgerLabels, ledgerPrintDoc, ledgerSourceText, ledgerTypeText, printField, withOpening
} from '@/components/fin/partnerLedger'
import { useDict } from '@/composables/useDict'
import { printDocs } from '@/utils/print'
import { useUserStore } from '@/store/user'

import { BillKind } from '@/constants/enums'
/**
 * 往来对账单：客户 / 供应商在期间内的应收应付、收付款、预收预付和逐笔余额，可打印给对方核对。
 * 明细分页（issue #134）：每页的余额基于该页之前的全部明细（后端计算），第一页首行为期初余额，之后的页为承前页余额；
 * 合计、期末基于全部明细；打印、导出取全部明细
 */
const { t } = useI18n()
const dict = useDict('fin_bill_source', 'fin_settle_method')
// 从往来汇总进入（?kind=AR&partnerId=&partnerName=）时直接查询该单位
const route = useRoute()
const kind = ref(route.query.kind === BillKind.AP ? BillKind.AP : BillKind.AR)
const partnerId = ref(Number(route.query.partnerId) || undefined)
const partnerName = ref(route.query.partnerName ? String(route.query.partnerName) : undefined)
// 挂接应付（issue #95，仅客户、需要应付的查看权限）：关联供应商的应付按相反方向列入，余额 = 应收 − 应付
const canLinkAp = useUserStore().hasPerm('fin:ap:list')
const withAp = ref(route.query.withAp === '1' && canLinkAp)
if (route.query.partnerId) useRouter().replace({ query: {} })
const range = ref([dayjs().startOf('year').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref()
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 50,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['20', '50', '100', '200', '500'],
  showTotal: (total) => t('common.table.total', { total })
})

const labels = computed(() => ledgerLabels(kind.value))
const columns = computed(() => ledgerColumns(labels.value))
// 每页首行：第一页为期初余额，之后为承前页余额
const rows = computed(() => {
  if (!data.value) return []
  const first = pagination.current === 1
  return [{ type: first ? OPENING : CARRIED, bizDate: first ? range.value[0] : '', balance: data.value.pageOpening }, ...data.value.rows]
})

const params = () => ({
  kind: kind.value,
  partnerId: partnerId.value,
  begin: range.value[0],
  end: range.value[1],
  withAp: kind.value === BillKind.AR && withAp.value
})

let seq = 0
async function load() {
  if (!partnerId.value) return
  const current = ++seq
  loading.value = true
  try {
    const res = await partnerStatement({ ...params(), current: pagination.current, size: pagination.pageSize })
    if (current !== seq) return
    data.value = res
    pagination.total = res.total
  } finally {
    if (current === seq) loading.value = false
  }
}
function search() {
  pagination.current = 1
  load()
}
function onTableChange(p) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  load()
}
function onKindChange() {
  partnerId.value = undefined
  data.value = undefined
}
watch([partnerId, range, withAp], search)
if (partnerId.value) load()

/** 打印、导出：全部明细（不分页） */
async function loadAll() {
  return partnerStatement(params())
}
const printing = ref(false)
async function print() {
  printing.value = true
  try {
    const d = await loadAll()
    const l = labels.value
    const fields = [
      [l.partner, d.partnerName],
      ...(d.linkedSupplierName ? [[t('fin.linkedAp.linkedSupplier'), d.linkedSupplierName]] : []),
      printField(t('fin.partnerStatement.statementPeriod'), t('fin.partnerStatement.range', { begin: range.value[0], end: range.value[1] }), 'statementPeriod'),
      printField(t('fin.partnerStatement.opening'), formatMoney(d.opening), 'beginBalance'),
      printField(t('fin.partnerStatement.closing'), formatMoney(d.closing), 'endBalance')
    ]
    await printDocs(ledgerPrintDoc({ title: l.title, fields, rows: withOpening(d.rows, range.value[0], d.opening), totals: d, labels: l, dict }),
      `${l.title} ${d.partnerName}`, 'fin.partnerStatement')
  } finally {
    printing.value = false
  }
}
const exporting = ref(false)
async function exportAll() {
  exporting.value = true
  try {
    const d = await loadAll()
    const l = labels.value
    const period = t('fin.partnerStatement.range', { begin: range.value[0], end: range.value[1] })
    await exportLedger(`${l.title}_${d.partnerName}`, [[l.partner, d.partnerName], [t('fin.partnerStatement.statementPeriod'), period]],
      withOpening(d.rows, range.value[0], d.opening), d, l, dict)
  } finally {
    exporting.value = false
  }
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
