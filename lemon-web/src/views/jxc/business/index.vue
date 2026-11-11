<template>
  <div class="jxc-business">
    <div class="page-card header">
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
      <a-button :loading="loading" @click="load">
        <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
      </a-button>
      <span class="sub-text">{{ $t('jxc.business.tip') }}</span>
    </div>

    <a-row :gutter="[16, 16]">
      <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="8" :xl="4">
        <div class="page-card kpi">
          <div class="sub-text">{{ k.label }}</div>
          <div class="kpi-value" :class="{ neg: k.neg }">{{ k.value }}</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="row">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('jxc.business.salesTrend')">
          <EChart :option="salesOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('jxc.business.cashTrend')">
          <template #extra><span class="sub-text">{{ $t('jxc.business.cashTip') }}</span></template>
          <EChart v-if="data.cash" :option="cashOption" height="300px" />
          <a-empty v-else :image="simpleImage" :description="$t('jxc.business.cashNoScope')" class="empty" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('jxc.business.salesmen')">
          <EChart v-if="data.salesmen.length" :option="salesmanOption" height="240px" />
          <a-table
            :row-key="(r) => r.groupKey ?? '__none'"
            size="small"
            :columns="salesmanColumns"
            :data-source="data.salesmen"
            :pagination="data.salesmen.length > 10 ? { pageSize: 10 } : false"
            :custom-row="(r) => ({ onClick: () => drill(JxcDimension.SALESMAN, r), class: 'click-row' })"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'"><a>{{ groupLabel(JxcDimension.SALESMAN, record, dict) }}</a></template>
              <template v-else-if="column.key === 'margin'">{{ marginText(record.margin) }}</template>
              <template v-else-if="column.money">{{ money(record[column.key]) }}</template>
              <template v-else-if="column.key === 'action'">
                <span @click.stop><TableActions :actions="drillActions(() => drill(JxcDimension.SALESMAN, record), () => drill(JxcDimension.SALESMAN, record, true))" /></span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('jxc.business.customers')">
          <template #extra><span class="sub-text">{{ $t('jxc.business.customerCount', { n: data.customerCount }) }}</span></template>
          <EChart v-if="data.customers.length" :option="customerOption" height="240px" />
          <a-table
            :row-key="(r) => r.groupKey ?? '__others'"
            size="small"
            :columns="customerColumns"
            :data-source="data.customers"
            :pagination="false"
            :custom-row="(r) => ({ onClick: () => r.groupKey && drill(JxcDimension.CUSTOMER, r), class: r.groupKey ? 'click-row' : '' })"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a v-if="record.groupKey">{{ record.groupName }}</a>
                <span v-else>{{ $t('jxc.common.others') }}</span>
              </template>
              <template v-else-if="column.key === 'share'">{{ share(record.netAmount) }}</template>
              <template v-else-if="column.key === 'margin'">{{ marginText(record.margin) }}</template>
              <template v-else-if="column.money">{{ money(record[column.key]) }}</template>
              <template v-else-if="column.key === 'action'">
                <span v-if="record.groupKey" @click.stop>
                  <TableActions :actions="drillActions(() => drill(JxcDimension.CUSTOMER, record), () => drill(JxcDimension.CUSTOMER, record, true))" />
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <JxcDetail ref="detailRef" report="sales" />
    <JxcDocs ref="docsRef" report="sales" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Empty } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import JxcDetail from '@/components/bi/jxc/JxcDetail.vue'
import JxcDocs from '@/components/bi/jxc/JxcDocs.vue'
import TableActions from '@/components/common/TableActions.vue'
import { jxcBusiness } from '@/api/bi/jxc'
import { monthLabel, shortMoney } from '@/components/charts/charts'
import { DICT_TYPES, drillActions, drillColumn, groupLabel, marginText, rangePresets, thisYear } from '@/components/bi/jxc/jxc'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { money } from '@/utils/number'
import { JxcDimension } from '@/constants/enums'
import { t } from '@/locales'

const dict = useDict(...DICT_TYPES)
const userStore = useUserStore()
const canDrill = userStore.hasPerm('bi:jxc:sales')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const presets = rangePresets()
/** 走势按月展示，默认本年 */
const range = ref(thisYear())
const data = ref({ total: {}, trend: [], cash: null, salesmen: [], customers: [], customerCount: 0 })
const loading = ref(false)

const num = (v) => Number(v || 0)
const yuan = (v) => t('jxc.common.yuan', { v: shortMoney(v) })
const sumCash = (field) => (data.value.cash || []).reduce((s, r) => s + num(r[field]), 0)

const kpis = computed(() => {
  const tot = data.value.total || {}
  const list = [
    { key: 'sales', label: t('jxc.business.netSales'), value: yuan(tot.netAmount) },
    { key: 'profit', label: t('jxc.business.profit'), value: yuan(tot.profit), neg: num(tot.profit) < 0 },
    { key: 'margin', label: t('jxc.business.margin'), value: marginText(tot.margin) }
  ]
  if (data.value.cash) {
    list.push(
      { key: 'receipt', label: t('jxc.business.receipt'), value: yuan(sumCash('receipt')) },
      { key: 'payment', label: t('jxc.business.payment'), value: yuan(sumCash('payment')) },
      { key: 'net', label: t('jxc.business.netCash'), value: yuan(sumCash('net')), neg: sumCash('net') < 0 }
    )
  }
  return list
})

const grid = { left: 16, right: 16, top: 36, bottom: 8, containLabel: true }
const valueAxis = { type: 'value', axisLabel: { formatter: shortMoney } }

const salesOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => yuan(v) },
  legend: { top: 0 },
  grid,
  xAxis: { type: 'category', data: data.value.trend.map((r) => monthLabel(r.groupKey)) },
  yAxis: valueAxis,
  series: [
    { name: t('jxc.business.netSales'), type: 'bar', barMaxWidth: 28, data: data.value.trend.map((r) => num(r.netAmount)) },
    { name: t('jxc.business.profit'), type: 'line', data: data.value.trend.map((r) => num(r.profit)) }
  ]
}))

const cashOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => yuan(v) },
  legend: { top: 0 },
  grid,
  xAxis: { type: 'category', data: (data.value.cash || []).map((r) => monthLabel(r.period)) },
  yAxis: valueAxis,
  series: [
    { name: t('jxc.business.receipt'), type: 'bar', barMaxWidth: 20, itemStyle: { color: '#52c41a' }, data: (data.value.cash || []).map((r) => num(r.receipt)) },
    { name: t('jxc.business.payment'), type: 'bar', barMaxWidth: 20, itemStyle: { color: '#faad14' }, data: (data.value.cash || []).map((r) => num(r.payment)) },
    { name: t('jxc.business.netCash'), type: 'line', data: (data.value.cash || []).map((r) => num(r.net)) }
  ]
}))

const salesmanOption = computed(() => {
  const rows = data.value.salesmen.slice(0, 10).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => yuan(v) },
    legend: { top: 0 },
    grid: { left: 8, right: 24, top: 28, bottom: 8, containLabel: true },
    xAxis: valueAxis,
    yAxis: { type: 'category', data: rows.map((r) => groupLabel(JxcDimension.SALESMAN, r, dict)) },
    series: [
      { name: t('jxc.business.netSales'), type: 'bar', barMaxWidth: 16, data: rows.map((r) => num(r.netAmount)) },
      { name: t('jxc.business.profit'), type: 'bar', barMaxWidth: 16, data: rows.map((r) => num(r.profit)) }
    ]
  }
})

const customerOption = computed(() => ({
  tooltip: { trigger: 'item', valueFormatter: (v) => yuan(v) },
  legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
  series: [{
    type: 'pie', radius: ['40%', '70%'], center: ['35%', '50%'],
    data: data.value.customers.filter((r) => num(r.netAmount) > 0)
      .map((r) => ({ name: r.groupKey ? r.groupName : t('jxc.common.others'), value: num(r.netAmount) }))
  }]
}))

const salesmanColumns = [
  { title: t('jxc.field.salesman'), key: 'name', ellipsis: true },
  { title: t('jxc.field.netAmount'), key: 'netAmount', align: 'right', money: true, width: 120 },
  { title: t('jxc.field.profit'), key: 'profit', align: 'right', money: true, width: 110 },
  { title: t('jxc.field.margin'), key: 'margin', align: 'right', width: 80 },
  { title: t('jxc.field.docCount'), dataIndex: 'docCount', align: 'right', width: 70 },
  ...(canDrill ? [drillColumn({ fixed: false })] : [])
]
const customerColumns = [
  { title: t('jxc.field.customer'), key: 'name', ellipsis: true },
  { title: t('jxc.field.netAmount'), key: 'netAmount', align: 'right', money: true, width: 120 },
  { title: t('jxc.business.share'), key: 'share', align: 'right', width: 70 },
  { title: t('jxc.field.profit'), key: 'profit', align: 'right', money: true, width: 110 },
  { title: t('jxc.field.margin'), key: 'margin', align: 'right', width: 80 },
  ...(canDrill ? [drillColumn({ fixed: false })] : [])
]

function share(v) {
  const all = num(data.value.total?.netAmount)
  return all ? `${((num(v) / all) * 100).toFixed(1)}%` : '-'
}

async function load() {
  loading.value = true
  try {
    data.value = await jxcBusiness({ begin: range.value[0], end: range.value[1] })
  } finally {
    loading.value = false
  }
}

/** 业务员、客户与商品没有直接关系：点击行到单据列表（再到单据明细），lines 为 true 时到商品明细 */
const detailRef = ref()
const docsRef = ref()
function drill(dim, row, lines = false) {
  if (!canDrill) return
  const opts = { params: { begin: range.value[0], end: range.value[1] }, dim, key: row.groupKey, label: groupLabel(dim, row, dict) }
  if (lines) detailRef.value.open(opts)
  else docsRef.value.open(opts)
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.kpi {
  padding: 14px 16px;
}
.kpi-value {
  font-size: 20px;
  font-weight: 600;
  margin-top: 4px;
}
.row {
  margin-top: 16px;
}
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.empty {
  padding: 80px 0;
}
:deep(.click-row) {
  cursor: pointer;
}
</style>
