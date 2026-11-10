<template>
  <div class="analysis">
    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col v-for="k in kpis" :key="k.label" :xs="12" :sm="8" :lg="6" :xl="4">
          <div class="page-card kpi">
            <div class="kpi-label">{{ k.label }}</div>
            <div class="kpi-value">{{ k.value }}</div>
            <div class="kpi-sub">{{ k.sub }}</div>
          </div>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="row">
        <a-col :xs="24" :lg="16">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.trend')">
            <EChart :option="trendOption" height="300px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.stage')">
            <EChart :option="stageOption" height="300px" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="row">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.funnel')">
            <EChart :option="funnelOption" height="260px" />
            <a-table row-key="stage" size="small" :columns="funnelColumns" :data-source="funnel" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'stage'"><DictTag type="crm_opp_stage" :value="record.stage" /></template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'conversion'">{{ record.conversion == null ? '-' : percent(record.conversion) }}</template>
              </template>
            </a-table>
            <div class="sub-text note">{{ $t('crm.analysis.funnelNote') }}</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.rfm')">
            <EChart :option="rfmOption" height="260px" />
            <div class="sub-text note">{{ $t('crm.analysis.rfmNote', { months: rfm.months ?? 12 }) }}</div>
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" class="page-card row" :title="$t('crm.analysis.card.rfmDetail')">
        <RfmSettingsBar class="rfm-settings" :settings="rfm.settings" @apply="loadRfm" />
        <a-table row-key="customerId" size="small" :columns="rfmColumns" :data-source="rfm.rows || []" :loading="rfmLoading"
          :pagination="{ pageSize: 10, showSizeChanger: false }" :scroll="{ x: 1080 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'customer'">
              <div>{{ record.name }}</div>
              <div class="sub-text">{{ record.code }}</div>
            </template>
            <template v-else-if="column.key === 'recency'">
              <span :class="record.recent ? 'good' : 'bad'">{{ $t('crm.analysis.days', { n: record.recency }) }}</span>
            </template>
            <template v-else-if="column.key === 'frequency'">{{ $t('crm.analysis.times', { n: record.frequency }) }}</template>
            <template v-else-if="column.key === 'monthlyFrequency'">
              <span :class="record.frequent ? 'good' : 'bad'">{{ record.monthlyFrequency }}</span>
            </template>
            <template v-else-if="column.key === 'monetary'">¥{{ formatMoney(record.monetary) }}</template>
            <template v-else-if="column.key === 'avgAmount'">
              <span :class="record.valuable ? 'good' : 'bad'">¥{{ formatMoney(record.avgAmount) }}</span>
            </template>
            <template v-else-if="column.key === 'segment'">
              <a-tag :color="segmentColor(record.segment)" :bordered="false">{{ segmentText(record.segment) }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-row :gutter="[16, 16]" class="row">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.source')">
            <a-table row-key="source" size="small" :columns="sourceColumns" :data-source="sources" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'source'"><DictTag type="crm_source" :value="record.source" /></template>
                <template v-else-if="column.key === 'rate'">{{ record.leads ? percent((record.converted * 100) / record.leads) : '-' }}</template>
                <template v-else-if="column.key === 'wonAmount'">¥{{ formatMoney(record.wonAmount) }}</template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('crm.analysis.card.industry')">
            <EChart :option="industryOption" height="260px" />
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" class="page-card row" :title="$t('crm.analysis.card.owner')">
        <a-table row-key="userId" size="small" :columns="ownerColumns" :data-source="owners" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'rank'">
              <span :class="['rank', { top: index < 3 }]">{{ index + 1 }}</span>
            </template>
            <template v-else-if="['pipeline', 'wonAmount'].includes(column.key)">¥{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
      </a-card>

      <a-card :bordered="false" class="page-card row" :title="$t('crm.analysis.card.service')">
        <a-table row-key="type" size="small" :columns="ticketColumns" :data-source="tickets" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'"><DictTag type="crm_ticket_type" :value="record.type" /></template>
            <template v-else-if="column.key === 'hours'">{{ record.avgMinutes == null ? '-' : $t('crm.analysis.hours', { n: (record.avgMinutes / 60).toFixed(1) }) }}</template>
            <template v-else-if="column.key === 'onTime'">{{ Number(record.resolved) ? percent((record.onTime * 100) / record.resolved) : '-' }}</template>
            <template v-else-if="column.key === 'satisfaction'">
              <a-rate v-if="record.satisfaction" :value="Math.round(Number(record.satisfaction) * 2) / 2" allow-half disabled class="rate" />
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import RfmSettingsBar from '@/components/bi/RfmSettingsBar.vue'
import { crmFunnel, crmIndustries, crmOverview, crmOwners, crmRfm, crmSources, crmTickets, crmTrend } from '@/api/crm'
import { useDict } from '@/composables/useDict'
import { formatMoney } from '@/components/scm/scm'
import { monthLabel, percent, shortMoney } from '@/components/charts/charts'

import { OpportunityStage, RfmSegment } from '@/constants/enums'
const { t } = useI18n()
const dict = useDict('crm_opp_stage', 'crm_customer_stage', 'crm_industry', 'crm_source', 'crm_ticket_type')
const label = (type, v) => dict[type].find((d) => d.value === v)?.label || v

const loading = ref(false)
const overview = ref({})
const trend = ref([])
const funnel = ref([])
const rfm = ref({})
const sources = ref([])
const owners = ref([])
const industries = ref([])
const tickets = ref([])

const rfmLoading = ref(false)
/** 按页面上的分群参数重新计算 RFM（参数为空时取本公司默认值） */
async function loadRfm(params) {
  rfmLoading.value = true
  try {
    rfm.value = await crmRfm(params)
  } finally {
    rfmLoading.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    ;[overview.value, trend.value, funnel.value, rfm.value, sources.value, owners.value, industries.value, tickets.value] = await Promise.all([
      crmOverview(),
      crmTrend(),
      crmFunnel(),
      crmRfm(),
      crmSources(),
      crmOwners(),
      crmIndustries(),
      crmTickets()
    ])
  } finally {
    loading.value = false
  }
})

const num = (v) => Number(v) || 0
const kpis = computed(() => {
  const o = overview.value
  return [
    { label: t('crm.analysis.kpi.newCustomers'), value: num(o.newCustomers), sub: t('crm.analysis.kpi.newCustomersSub', { owned: num(o.ownedCustomers), pool: num(o.poolCustomers) }) },
    { label: t('crm.analysis.kpi.newLeads'), value: num(o.newLeads), sub: t('crm.analysis.kpi.newLeadsSub', { n: num(o.convertedLeads) }) },
    { label: t('crm.analysis.kpi.pipeline'), value: `¥${shortMoney(o.pipeline)}`, sub: t('crm.analysis.kpi.pipelineSub', { n: num(o.openOpps), w: shortMoney(o.weightedPipeline) }) },
    { label: t('crm.analysis.kpi.won'), value: `¥${shortMoney(o.wonAmount)}`, sub: t('crm.analysis.kpi.wonSub', { n: num(o.wonCount) }) },
    { label: t('crm.analysis.kpi.winRate'), value: o.winRate == null ? '-' : percent(o.winRate), sub: t('crm.analysis.kpi.winRateSub', { won: num(o.wonTotal), lost: num(o.lostTotal) }) },
    {
      label: t('crm.analysis.kpi.openTickets'),
      value: num(o.openTickets),
      sub: t('crm.analysis.kpi.openTicketsSub', { overdue: num(o.overdueTickets), s: o.satisfaction == null ? '-' : Number(o.satisfaction).toFixed(1) })
    }
  ]
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: [t('crm.analysis.sales'), t('crm.analysis.wonAmount')] },
  grid: { left: 8, right: 20, top: 40, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: trend.value.map((r) => monthLabel(r.period)) },
  yAxis: { type: 'value', axisLabel: { formatter: (v) => shortMoney(v) } },
  series: [
    { name: t('crm.analysis.sales'), type: 'bar', barMaxWidth: 28, data: trend.value.map((r) => num(r.sales)) },
    { name: t('crm.analysis.wonAmount'), type: 'line', smooth: true, data: trend.value.map((r) => num(r.wonAmount)) }
  ]
}))

const stageOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      // 卡片较窄：环形缩小，给外侧标签（阶段名 + 数量）留出位置，不再被截断成「意...」
      radius: ['36%', '56%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}\n{c}', overflow: 'none' },
      labelLine: { length: 8, length2: 8 },
      data: (overview.value.customerStages || []).map((s) => ({ name: label('crm_customer_stage', s.stage), value: num(s.count) }))
    }
  ]
}))

const funnelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${t('crm.analysis.reachedTip', { n: p.value })}` },
  series: [
    {
      type: 'funnel',
      left: '10%',
      width: '80%',
      sort: 'none',
      minSize: '20%',
      label: { position: 'inside', formatter: '{b}  {c}' },
      data: funnel.value.filter((r) => r.stage !== OpportunityStage.LOST).map((r) => ({ name: label('crm_opp_stage', r.stage), value: num(r.reached) }))
    }
  ]
}))
const funnelColumns = [
  { title: t('crm.common.f.stage'), key: 'stage', width: 110 },
  { title: t('crm.analysis.col.current'), dataIndex: 'count', width: 90, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('crm.analysis.col.reached'), dataIndex: 'reached', width: 90, align: 'right' },
  { title: t('crm.analysis.col.conversion'), key: 'conversion', width: 100, align: 'right' }
]

const SEGMENT_COLORS = {
  [RfmSegment.IMPORTANT_VALUE]: 'gold',
  [RfmSegment.IMPORTANT_DEVELOP]: 'green',
  [RfmSegment.IMPORTANT_KEEP]: 'blue',
  [RfmSegment.IMPORTANT_RETAIN]: 'red',
  [RfmSegment.GENERAL_VALUE]: 'cyan',
  [RfmSegment.GENERAL_DEVELOP]: 'lime',
  [RfmSegment.GENERAL_KEEP]: 'geekblue',
  [RfmSegment.GENERAL_RETAIN]: 'default'
}
const segmentColor = (s) => SEGMENT_COLORS[s] || 'default'
const segmentText = (s) => t(`jxc.rfm.seg.${s}`)
const rfmOption = computed(() => {
  const segs = (rfm.value.segments || []).slice().reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: [t('crm.analysis.customers'), t('crm.analysis.sales')] },
    // 坐标轴名称（客户数 / 销售额）在轴的右端，containLabel 不计入名称，右侧留出名称的宽度
    grid: { left: 8, right: 64, top: 40, bottom: 20, containLabel: true },
    xAxis: [
      { type: 'value', name: t('crm.analysis.customers'), minInterval: 1 },
      { type: 'value', name: t('crm.analysis.sales'), axisLabel: { formatter: (v) => shortMoney(v) }, splitLine: { show: false } }
    ],
    yAxis: { type: 'category', data: segs.map((s) => segmentText(s.segment)) },
    series: [
      { name: t('crm.analysis.customers'), type: 'bar', barMaxWidth: 14, data: segs.map((s) => num(s.count)) },
      { name: t('crm.analysis.sales'), type: 'bar', xAxisIndex: 1, barMaxWidth: 14, data: segs.map((s) => num(s.amount)) }
    ]
  }
})
const rfmColumns = [
  { title: t('crm.common.f.customer'), key: 'customer', width: 220 },
  { title: t('crm.common.f.owner'), dataIndex: 'ownerName', width: 100 },
  { title: t('crm.analysis.col.recency'), key: 'recency', width: 120, align: 'right' },
  { title: t('crm.analysis.col.frequency'), key: 'frequency', width: 110, align: 'right' },
  { title: t('crm.analysis.col.monthlyFrequency'), key: 'monthlyFrequency', width: 110, align: 'right' },
  { title: t('crm.analysis.col.monetary'), key: 'monetary', width: 140, align: 'right' },
  { title: t('crm.analysis.col.avgAmount'), key: 'avgAmount', width: 140, align: 'right' },
  { title: t('crm.analysis.col.segment'), key: 'segment', width: 160 }
]

const sourceColumns = [
  { title: t('crm.common.f.source'), key: 'source', width: 110 },
  { title: t('crm.analysis.col.leads'), dataIndex: 'leads', width: 70, align: 'right' },
  { title: t('crm.analysis.col.converted'), dataIndex: 'converted', width: 90, align: 'right' },
  { title: t('crm.analysis.col.conversion'), key: 'rate', width: 100, align: 'right' },
  { title: t('crm.analysis.col.opps'), dataIndex: 'opps', width: 70, align: 'right' },
  { title: t('crm.analysis.wonAmount'), key: 'wonAmount', width: 130, align: 'right' }
]

const industryOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${t('crm.analysis.industryTip', { amount: formatMoney(p.value) })}<br/>${t('crm.analysis.industryCustomers', { n: p.data.customers })}` },
  legend: { type: 'scroll', bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: '62%',
      center: ['50%', '45%'],
      data: industries.value.map((r) => ({
        name: r.industry === 'UNKNOWN' ? t('crm.analysis.unset') : label('crm_industry', r.industry),
        value: num(r.sales),
        customers: num(r.customers)
      }))
    }
  ]
}))

const ownerColumns = [
  { title: t('crm.analysis.col.rank'), key: 'rank', width: 70 },
  { title: t('crm.analysis.col.sales'), dataIndex: 'name', width: 120 },
  { title: t('crm.analysis.col.customers'), dataIndex: 'customers', width: 100, align: 'right' },
  { title: t('crm.analysis.col.openOpps'), dataIndex: 'openOpps', width: 100, align: 'right' },
  { title: t('crm.analysis.col.pipeline'), key: 'pipeline', width: 140, align: 'right' },
  { title: t('crm.analysis.col.wonCount'), dataIndex: 'wonCount', width: 100, align: 'right' },
  { title: t('crm.analysis.col.wonAmount'), key: 'wonAmount', width: 160, align: 'right' },
  { title: t('crm.analysis.col.followups'), dataIndex: 'followups', width: 140, align: 'right' }
]

const ticketColumns = [
  { title: t('crm.analysis.col.ticketType'), key: 'type', width: 110 },
  { title: t('crm.analysis.col.ticketCount'), dataIndex: 'count', width: 80, align: 'right' },
  { title: t('crm.analysis.col.open'), dataIndex: 'open', width: 80, align: 'right' },
  { title: t('crm.analysis.col.avgHours'), key: 'hours', width: 130, align: 'right' },
  { title: t('crm.analysis.col.onTime'), key: 'onTime', width: 120, align: 'right' },
  { title: t('crm.analysis.col.satisfaction'), key: 'satisfaction', width: 160 }
]
</script>

<style scoped>
.analysis {
  padding-bottom: 16px;
}
.row {
  margin-top: 16px;
}
.kpi {
  padding: 16px 20px;
  height: 100%;
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 13px;
}
.kpi-value {
  margin: 6px 0 4px;
  font-size: 22px;
  font-weight: 600;
  white-space: nowrap;
}
.kpi-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.note {
  margin-top: 8px;
}
.rfm-settings {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.good {
  color: #52c41a;
}
.bad {
  color: var(--app-text-3);
}
.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: var(--app-hover);
  font-size: 12px;
}
.rank.top {
  background: var(--app-primary);
  color: #fff;
}
.rate {
  font-size: 13px;
}
</style>
