<template>
  <div class="cockpit">
    <div class="page-card header">
      <div>
        <div class="title">经营驾驶舱</div>
        <div class="sub-text">
          趋势、排行来自数据仓库<template v-if="data.lastSync">（数据更新于 {{ dayjs(data.lastSync).format('YYYY-MM-DD HH:mm') }}）</template>；应收应付、在途订单、生产、项目、客户服务为实时数据
        </div>
      </div>
      <a-space>
        <a-button v-perm="'bi:etl:run'" :loading="syncing" @click="sync">
          <template #icon><SyncOutlined /></template>立即同步
        </a-button>
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>刷新
        </a-button>
      </a-space>
    </div>

    <a-alert v-if="!loading && !data.lastSync" type="warning" show-icon class="alert" message="数据仓库尚未同步">
      <template #description>
        销售、采购、利润趋势读取数据仓库，请先同步一次（之后每天凌晨自动同步）。
        <a-button v-perm="'bi:etl:run'" size="small" type="primary" :loading="syncing" class="inline-btn" @click="sync">立即同步</a-button>
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
        <a-card :bordered="false" class="page-card" title="近 12 个月销售额与毛利">
          <EChart :option="salesOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" title="预警">
          <template #extra><router-link to="/bi/decision">决策支持</router-link></template>
          <a-empty v-if="!triggered.length" :image="simpleImage" description="当前没有触发的预警" />
          <div v-else class="alerts">
            <div v-for="a in triggered" :key="a.ruleId" class="alert-item">
              <DictTag type="bi_alert_level" :value="a.level" />
              <div class="alert-body">
                <div>{{ a.name }}</div>
                <div class="sub-text">当前 {{ fmtValue(a.value, a.unit) }}{{ a.unit }}，{{ a.remark || a.metricLabel }}</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" title="收入、费用与净利润（已记账凭证）">
          <EChart :option="profitOption" height="280px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" title="采购额">
          <EChart :option="purchaseOption" height="280px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12" :xl="8">
        <a-card :bordered="false" class="page-card" title="本年客户 Top 5">
          <EChart :option="rankOption(data.topCustomers, 'amount')" height="260px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12" :xl="8">
        <a-card :bordered="false" class="page-card" title="本年产品 Top 5">
          <EChart :option="rankOption(data.topMaterials, 'amount')" height="260px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" title="库存金额（按物料分类）">
          <template #extra>
            <span v-if="data.inventory?.[0]?.snapDate" class="sub-text">快照 {{ data.inventory[0].snapDate }}</span>
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

const router = useRouter()
const dict = useDict('wms_material_category', 'bi_alert_level')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const data = ref({ kpi: {}, trend: [], topCustomers: [], topMaterials: [], inventory: [] })
const alertList = ref([])
const loading = ref(false)
const syncing = ref(false)

const num = (v) => Number(v || 0)
const money = (v) => `¥ ${shortMoney(v)}`
const fmtValue = (v, unit) => (v === null || v === undefined ? '-' : unit === '元' ? shortMoney(v) : Number(v).toLocaleString('zh-CN'))
const triggered = computed(() => alertList.value.filter((a) => a.triggered))

const kpis = computed(() => {
  const k = data.value.kpi || {}
  const mom = growthTag(k.monthSalesMom)
  const yoy = growthTag(k.monthSalesYoy)
  const yyoy = growthTag(k.yearSalesYoy)
  return [
    { key: 'monthSales', label: '本月销售额', value: money(k.monthSales), to: '/bi/olap',
      tags: [{ label: '环比', text: mom.text, color: mom.color }, { label: '同比', text: yoy.text, color: yoy.color }] },
    { key: 'monthProfit', label: '本月毛利', value: money(k.monthProfit), tags: [{ label: '毛利率', text: percent(k.monthMargin) }] },
    { key: 'yearSales', label: '本年销售额', value: money(k.yearSales),
      tags: [{ label: '同比', text: yyoy.text, color: yyoy.color }, { label: '毛利率', text: percent(k.yearMargin) }] },
    { key: 'ar', label: '应收余额', value: money(k.arBalance), to: '/fin/ar',
      tags: [{ label: '已逾期', text: money(k.arOverdue), color: num(k.arOverdue) > 0 ? 'red' : '' }] },
    { key: 'ap', label: '应付余额', value: money(k.apBalance), to: '/fin/ap', tags: [{ label: '本月采购', text: money(k.monthPurchase) }] },
    { key: 'openSales', label: '在途销售订单', value: money(k.openSalesAmount), to: '/scm/sales', tags: [{ label: '未出库金额', text: '' }] },
    { key: 'wo', label: '在制工单', value: `${num(k.openWorkOrders)} 张`, to: '/mes/wo',
      tags: [{ label: '延期', text: `${num(k.lateWorkOrders)} 张`, color: num(k.lateWorkOrders) > 0 ? 'red' : '' }] },
    { key: 'projects', label: '进行中项目', value: `${num(k.activeProjects)} 个`, to: '/pm/project',
      tags: [{ label: '平均进度', text: percent(k.projectProgress) }] },
    { key: 'headcount', label: '在职人数', value: `${num(k.headcount)} 人`, to: '/hr/employee', tags: [] },
    { key: 'pipeline', label: '商机储备', value: money(k.pipeline), to: '/crm/opportunity', tags: [{ label: '进行中商机金额', text: '' }] },
    { key: 'tickets', label: '服务工单', value: `${num(k.openTickets)} 张`, to: '/crm/ticket',
      tags: [{ label: '超时', text: `${num(k.overdueTickets)} 张`, color: num(k.overdueTickets) > 0 ? 'red' : '' }] }
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
    { name: '销售额', type: 'bar', barMaxWidth: 28, data: data.value.trend.map((t) => num(t.sales)) },
    { name: '毛利', type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.profit)) }
  ]
}))

const profitOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => money(v) },
  legend: { top: 0 },
  grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: months.value },
  yAxis: axis(),
  series: [
    { name: '收入', type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.revenue)) },
    { name: '费用', type: 'line', smooth: true, data: data.value.trend.map((t) => num(t.expense)) },
    { name: '净利润', type: 'bar', barMaxWidth: 24, data: data.value.trend.map((t) => num(t.netProfit)) }
  ]
}))

const purchaseOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => money(v) },
  grid: { left: 16, right: 16, top: 16, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: months.value },
  yAxis: axis(),
  series: [{ name: '采购额', type: 'bar', barMaxWidth: 28, itemStyle: { color: '#13c2c2' }, data: data.value.trend.map((t) => num(t.purchase)) }]
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
      name: findDict(dict.wms_material_category, r.category)?.label || r.category || '未分类',
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
    message.success(`同步完成：${log.rowCounts || ''}`)
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
