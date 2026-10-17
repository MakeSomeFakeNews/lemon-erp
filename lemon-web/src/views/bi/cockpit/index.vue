<template>
  <div class="cockpit">
    <div class="page-card header">
      <div>
        <div class="title">{{ $t('bi.cockpit.title') }}</div>
        <div class="sub-text">
          {{ data.lastSync ? $t('bi.cockpit.subSynced', { time: dayjs(data.lastSync).format('YYYY-MM-DD HH:mm') }) : $t('bi.cockpit.sub') }}
        </div>
      </div>
      <a-space>
        <a-button v-perm="'bi:etl:run'" :loading="syncing" @click="sync">
          <template #icon><SyncOutlined /></template>{{ $t('bi.etl.syncNow') }}
        </a-button>
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
        </a-button>
      </a-space>
    </div>

    <a-alert v-if="!loading && !data.lastSync" type="warning" show-icon class="alert" :message="$t('bi.cockpit.notSynced')">
      <template #description>
        {{ $t('bi.cockpit.notSyncedDesc') }}
        <a-button v-perm="'bi:etl:run'" size="small" type="primary" :loading="syncing" class="inline-btn" @click="sync">{{ $t('bi.etl.syncNow') }}</a-button>
      </template>
    </a-alert>

    <a-row :gutter="[16, 16]">
      <a-col v-for="k in kpis" :key="k.key" :xs="12" :md="8" :xl="6" :xxl="4">
        <div class="page-card kpi" :class="{ link: k.to }" @click="k.to && router.push(k.to)">
          <div class="kpi-label">{{ k.label }}</div>
          <a-skeleton v-if="loading" active :paragraph="false" :title="{ width: 80 }" />
          <template v-else>
            <div class="kpi-value">{{ k.value }}</div>
            <div class="kpi-extra">
              <template v-for="t in k.tags" :key="t.label">
                <span class="tag-label">{{ t.label }}</span>
                <span :class="['tag-value', t.color]">{{ t.text }}</span>
              </template>
            </div>
          </template>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="16">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.salesTrend')">
          <EChart :option="salesOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.alerts')">
          <template #extra><router-link to="/bi/decision">{{ $t('bi.cockpit.decision') }}</router-link></template>
          <a-empty v-if="!triggered.length" :image="simpleImage" :description="$t('bi.cockpit.noAlert')" />
          <div v-else class="alerts">
            <div v-for="a in triggered" :key="a.ruleId" class="alert-item">
              <DictTag type="bi_alert_level" :value="a.level" />
              <div class="alert-body">
                <div>{{ a.name }}</div>
                <div class="sub-text">{{ $t('bi.cockpit.alertCurrent', { value: fmtValue(a.value, a.unit), unit: a.unit, remark: a.remark || a.metricLabel }) }}</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.profit')">
          <EChart :option="profitOption" height="280px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.purchase')">
          <EChart :option="purchaseOption" height="280px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.topCustomers')">
          <EChart :option="rankOption(data.topCustomers, 'amount')" height="260px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.topProducts')">
          <EChart :option="rankOption(data.topMaterials, 'amount')" height="260px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('bi.cockpit.inventory')">
          <template #extra>
            <span v-if="data.inventory?.[0]?.snapDate" class="sub-text">{{ $t('bi.cockpit.snapshot', { date: data.inventory[0].snapDate }) }}</span>
          </template>
          <EChart :option="inventoryOption" height="260px" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { alerts as loadAlerts, cockpit, runEtl } from '@/api/bi'
import { growthTag, monthLabel, percent, shortMoney } from '@/components/charts/charts'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'

const router = useRouter()
const dict = useDict('wms_material_category', 'bi_alert_level')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const data = ref({ kpi: {}, trend: [], topCustomers: [], topMaterials: [], inventory: [] })
const alertList = ref([])
const loading = ref(false)
const syncing = ref(false)

const num = (v) => Number(v || 0)
const money = (v) => `¥ ${shortMoney(v)}`
const fmtValue = (v, unit) => (v === null || v === undefined ? '-' : unit === t('bi.common.yuanUnit') ? shortMoney(v) : Number(v).toLocaleString('zh-CN'))
const triggered = computed(() => alertList.value.filter((a) => a.triggered))

const kpis = computed(() => {
  const k = data.value.kpi || {}
  const mom = growthTag(k.monthSalesMom)
  const yoy = growthTag(k.monthSalesYoy)
  const yyoy = growthTag(k.yearSalesYoy)
  return [
    { key: 'monthSales', label: t('bi.cockpit.monthSales'), value: money(k.monthSales), to: '/bi/olap',
      tags: [{ label: t('bi.cockpit.mom'), text: mom.text, color: mom.color }, { label: t('bi.cockpit.yoy'), text: yoy.text, color: yoy.color }] },
    { key: 'monthProfit', label: t('bi.cockpit.monthProfit'), value: money(k.monthProfit), tags: [{ label: t('bi.cockpit.margin'), text: percent(k.monthMargin) }] },
    { key: 'yearSales', label: t('bi.cockpit.yearSales'), value: money(k.yearSales),
      tags: [{ label: t('bi.cockpit.yoy'), text: yyoy.text, color: yyoy.color }, { label: t('bi.cockpit.margin'), text: percent(k.yearMargin) }] },
    { key: 'ar', label: t('bi.cockpit.arBalance'), value: money(k.arBalance), to: '/fin/ar',
      tags: [{ label: t('bi.cockpit.overdue'), text: money(k.arOverdue), color: num(k.arOverdue) > 0 ? 'red' : '' }] },
    { key: 'ap', label: t('bi.cockpit.apBalance'), value: money(k.apBalance), to: '/fin/ap', tags: [{ label: t('bi.cockpit.monthPurchase'), text: money(k.monthPurchase) }] },
    { key: 'openSales', label: t('bi.cockpit.openSales'), value: money(k.openSalesAmount), to: '/scm/sales', tags: [{ label: t('bi.cockpit.unshippedAmount'), text: '' }] },
    { key: 'wo', label: t('bi.cockpit.openWo'), value: t('bi.cockpit.countN', { n: num(k.openWorkOrders) }), to: '/mes/wo',
      tags: [{ label: t('bi.cockpit.late'), text: t('bi.cockpit.countN', { n: num(k.lateWorkOrders) }), color: num(k.lateWorkOrders) > 0 ? 'red' : '' }] },
    { key: 'projects', label: t('bi.cockpit.activeProjects'), value: t('bi.cockpit.countN', { n: num(k.activeProjects) }), to: '/pm/project',
      tags: [{ label: t('bi.cockpit.avgProgress'), text: percent(k.projectProgress) }] },
    { key: 'headcount', label: t('bi.cockpit.headcount'), value: t('bi.cockpit.peopleN', { n: num(k.headcount) }), to: '/hr/employee', tags: [] },
    { key: 'pipeline', label: t('bi.cockpit.pipeline'), value: money(k.pipeline), to: '/crm/opportunity', tags: [{ label: t('bi.cockpit.pipelineAmount'), text: '' }] },
    { key: 'tickets', label: t('bi.cockpit.tickets'), value: t('bi.cockpit.countN', { n: num(k.openTickets) }), to: '/crm/ticket',
      tags: [{ label: t('bi.cockpit.overtime'), text: t('bi.cockpit.countN', { n: num(k.overdueTickets) }), color: num(k.overdueTickets) > 0 ? 'red' : '' }] }
  ]
})

const months = computed(() => data.value.trend.map((t) => monthLabel(t.period)))
const axis = (formatter = shortMoney) => ({ type: 'value', axisLabel: { formatter } })

const salesOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => money(v) },
  legend: { top: 0 },
  grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: months.value },
  yAxis: axis(),
  series: [
    { name: t('bi.cockpit.sales'), type: 'bar', barMaxWidth: 28, data: data.value.trend.map((t) => num(t.sales)) },
    { name: t('bi.cockpit.profitName'), type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.profit)) }
  ]
}))

const profitOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => money(v) },
  legend: { top: 0 },
  grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: months.value },
  yAxis: axis(),
  series: [
    { name: t('bi.cockpit.revenue'), type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.revenue)) },
    { name: t('bi.cockpit.expense'), type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.expense)) },
    { name: t('bi.cockpit.netProfit'), type: 'bar', barMaxWidth: 24, data: data.value.trend.map((t) => num(t.netProfit)) }
  ]
}))

const purchaseOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => money(v) },
  grid: { left: 16, right: 16, top: 16, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: months.value },
  yAxis: axis(),
  series: [{ name: t('bi.cockpit.purchase'), type: 'bar', barMaxWidth: 28, itemStyle: { color: '#13c2c2' }, data: data.value.trend.map((t) => num(t.purchase)) }]
}))

function rankOption(list = [], key) {
  const rows = [...list].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => money(v) },
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
    yAxis: { type: 'category', data: rows.map((r) => (r.name?.length > 10 ? `${r.name.slice(0, 10)}…` : r.name)) },
    series: [{ type: 'bar', barMaxWidth: 18, data: rows.map((r) => num(r[key])) }]
  }
}

const inventoryOption = computed(() => ({
  tooltip: { trigger: 'item', valueFormatter: (v) => money(v) },
  legend: { type: 'scroll', bottom: 0 },
  series: [{
    type: 'pie', radius: ['40%', '66%'], center: ['50%', '44%'],
    data: (data.value.inventory || []).map((r) => ({
      name: findDict(dict.wms_material_category, r.category)?.label || r.category || t('bi.cockpit.uncategorized'),
      value: num(r.amount)
    }))
  }]
}))

async function load() {
  loading.value = true
  try {
    const [c, a] = await Promise.all([cockpit(), loadAlerts().catch(() => [])])
    data.value = c
    alertList.value = a
  } finally {
    loading.value = false
  }
}

async function sync() {
  syncing.value = true
  try {
    const log = await runEtl()
    message.success(t('bi.etl.done', { detail: log.rowCounts || '' }))
    await load()
  } finally {
    syncing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.cockpit {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.cockpit .page-card + .page-card {
  margin-top: 0;
}
.header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.inline-btn {
  margin-left: 8px;
}
.kpi {
  padding: 16px 18px;
  height: 100%;
  min-height: 112px;
}
.kpi.link {
  cursor: pointer;
}
.kpi.link:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 13px;
}
.kpi-value {
  font-size: 22px;
  font-weight: 600;
  margin: 6px 0 4px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.kpi-extra {
  font-size: 12px;
  color: var(--app-text-3);
  display: flex;
  flex-wrap: wrap;
  gap: 2px 6px;
}
.tag-value {
  color: var(--app-text-2);
  margin-right: 6px;
}
.tag-value.green {
  color: #52c41a;
}
.tag-value.red {
  color: #f5222d;
}
.alerts {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow: auto;
}
.alert-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.alert-body {
  flex: 1;
  min-width: 0;
}
</style>
