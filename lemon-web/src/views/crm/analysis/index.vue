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
          <a-card :bordered="false" class="page-card" title="近 12 个月销售与赢单">
            <EChart :option="trendOption" height="300px" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="page-card" title="客户阶段分布">
            <EChart :option="stageOption" height="300px" />
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="row">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" title="销售漏斗">
            <EChart :option="funnelOption" height="260px" />
            <a-table row-key="stage" size="small" :columns="funnelColumns" :data-source="funnel" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'stage'"><DictTag type="crm_opp_stage" :value="record.stage" /></template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'conversion'">{{ record.conversion == null ? '-' : percent(record.conversion) }}</template>
              </template>
            </a-table>
            <div class="sub-text note">到达：曾经推进到该阶段的商机数；转化率 = 到达下一阶段 / 到达本阶段</div>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" title="客户价值分层（RFM，近一年）">
            <EChart :option="rfmOption" height="260px" />
            <div class="sub-text note">
              与平均值比较：最近购买距今 {{ rfm.avgRecency ?? '-' }} 天、购买 {{ rfm.avgFrequency ?? '-' }} 次、
              金额 ¥{{ formatMoney(rfm.avgMonetary) }}；R 近、F 多为「价值 / 保持」，M 高为「重要」
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" class="page-card row" title="客户 RFM 明细">
        <a-table row-key="customerId" size="small" :columns="rfmColumns" :data-source="rfm.rows || []" :pagination="{ pageSize: 10, showSizeChanger: false }" :scroll="{ x: 860 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'customer'">
              <div>{{ record.name }}</div>
              <div class="sub-text">{{ record.code }}</div>
            </template>
            <template v-else-if="column.key === 'recency'">
              <span :class="record.recent ? 'good' : 'bad'">{{ record.recency }} 天</span>
            </template>
            <template v-else-if="column.key === 'frequency'">
              <span :class="record.frequent ? 'good' : 'bad'">{{ record.frequency }} 次</span>
            </template>
            <template v-else-if="column.key === 'monetary'">
              <span :class="record.valuable ? 'good' : 'bad'">¥{{ formatMoney(record.monetary) }}</span>
            </template>
            <template v-else-if="column.key === 'segment'">
              <a-tag :color="segmentColor(record.segment)" :bordered="false">{{ record.segment }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-row :gutter="[16, 16]" class="row">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" title="来源分析">
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
          <a-card :bordered="false" class="page-card" title="行业分布（近一年销售额）">
            <EChart :option="industryOption" height="260px" />
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" class="page-card row" title="销售人员排名">
        <a-table row-key="userId" size="small" :columns="ownerColumns" :data-source="owners" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'rank'">
              <span :class="['rank', { top: index < 3 }]">{{ index + 1 }}</span>
            </template>
            <template v-else-if="['pipeline', 'wonAmount'].includes(column.key)">¥{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
      </a-card>

      <a-card :bordered="false" class="page-card row" title="服务质量">
        <a-table row-key="type" size="small" :columns="ticketColumns" :data-source="tickets" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'"><DictTag type="crm_ticket_type" :value="record.type" /></template>
            <template v-else-if="column.key === 'hours'">{{ record.avgMinutes == null ? '-' : `${(record.avgMinutes / 60).toFixed(1)} 小时` }}</template>
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
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { crmFunnel, crmIndustries, crmOverview, crmOwners, crmRfm, crmSources, crmTickets, crmTrend } from '@/api/crm'
import { useDict } from '@/composables/useDict'
import { formatMoney } from '@/components/scm/scm'
import { monthLabel, percent, shortMoney } from '@/components/charts/charts'

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
    { label: '本月新增客户', value: num(o.newCustomers), sub: `负责中 ${num(o.ownedCustomers)} · 公海 ${num(o.poolCustomers)}` },
    { label: '本月新增线索', value: num(o.newLeads), sub: `本月转化 ${num(o.convertedLeads)}` },
    { label: '进行中商机', value: `¥${shortMoney(o.pipeline)}`, sub: `${num(o.openOpps)} 个 · 加权 ¥${shortMoney(o.weightedPipeline)}` },
    { label: '本月赢单', value: `¥${shortMoney(o.wonAmount)}`, sub: `${num(o.wonCount)} 个` },
    { label: '赢单率', value: o.winRate == null ? '-' : percent(o.winRate), sub: `赢 ${num(o.wonTotal)} · 输 ${num(o.lostTotal)}` },
    {
      label: '未关闭工单',
      value: num(o.openTickets),
      sub: `超时 ${num(o.overdueTickets)} · 满意度 ${o.satisfaction == null ? '-' : Number(o.satisfaction).toFixed(1)}`
    }
  ]
})

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['销售额', '赢单金额'] },
  grid: { left: 60, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: trend.value.map((r) => monthLabel(r.period)) },
  yAxis: { type: 'value', axisLabel: { formatter: (v) => shortMoney(v) } },
  series: [
    { name: '销售额', type: 'bar', barMaxWidth: 28, data: trend.value.map((r) => num(r.sales)) },
    { name: '赢单金额', type: 'line', smooth: true, data: trend.value.map((r) => num(r.wonAmount)) }
  ]
}))

const stageOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}\n{c}' },
      data: (overview.value.customerStages || []).map((s) => ({ name: label('crm_customer_stage', s.stage), value: num(s.count) }))
    }
  ]
}))

const funnelOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>到达 ${p.value} 个` },
  series: [
    {
      type: 'funnel',
      left: '10%',
      width: '80%',
      sort: 'none',
      minSize: '20%',
      label: { position: 'inside', formatter: '{b}  {c}' },
      data: funnel.value.filter((r) => r.stage !== 'LOST').map((r) => ({ name: label('crm_opp_stage', r.stage), value: num(r.reached) }))
    }
  ]
}))
const funnelColumns = [
  { title: '阶段', key: 'stage', width: 110 },
  { title: '当前商机', dataIndex: 'count', width: 90, align: 'right' },
  { title: '金额', key: 'amount', width: 140, align: 'right' },
  { title: '到达', dataIndex: 'reached', width: 80, align: 'right' },
  { title: '转化率', key: 'conversion', width: 90, align: 'right' }
]

const SEGMENT_COLORS = {
  重要价值客户: 'gold',
  重要发展客户: 'green',
  重要保持客户: 'blue',
  重要挽留客户: 'red',
  一般价值客户: 'cyan',
  一般发展客户: 'lime',
  一般保持客户: 'geekblue',
  一般挽留客户: 'default'
}
const segmentColor = (s) => SEGMENT_COLORS[s] || 'default'
const rfmOption = computed(() => {
  const segs = (rfm.value.segments || []).slice().reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['客户数', '销售额'] },
    grid: { left: 100, right: 40, top: 40, bottom: 20 },
    xAxis: [
      { type: 'value', name: '客户数', minInterval: 1 },
      { type: 'value', name: '销售额', axisLabel: { formatter: (v) => shortMoney(v) }, splitLine: { show: false } }
    ],
    yAxis: { type: 'category', data: segs.map((s) => s.segment) },
    series: [
      { name: '客户数', type: 'bar', barMaxWidth: 14, data: segs.map((s) => num(s.count)) },
      { name: '销售额', type: 'bar', xAxisIndex: 1, barMaxWidth: 14, data: segs.map((s) => num(s.amount)) }
    ]
  }
})
const rfmColumns = [
  { title: '客户', key: 'customer', width: 220 },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '最近购买（R）', key: 'recency', width: 120, align: 'right' },
  { title: '购买次数（F）', key: 'frequency', width: 120, align: 'right' },
  { title: '购买金额（M）', key: 'monetary', width: 150, align: 'right' },
  { title: '客户分层', key: 'segment', width: 140 }
]

const sourceColumns = [
  { title: '来源', key: 'source', width: 110 },
  { title: '线索', dataIndex: 'leads', width: 70, align: 'right' },
  { title: '已转化', dataIndex: 'converted', width: 80, align: 'right' },
  { title: '转化率', key: 'rate', width: 80, align: 'right' },
  { title: '商机', dataIndex: 'opps', width: 70, align: 'right' },
  { title: '赢单金额', key: 'wonAmount', width: 130, align: 'right' }
]

const industryOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>销售额 ¥${formatMoney(p.value)}<br/>客户 ${p.data.customers} 个` },
  legend: { type: 'scroll', bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: '62%',
      center: ['50%', '45%'],
      data: industries.value.map((r) => ({
        name: r.industry === 'UNKNOWN' ? '未设置' : label('crm_industry', r.industry),
        value: num(r.sales),
        customers: num(r.customers)
      }))
    }
  ]
}))

const ownerColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '销售', dataIndex: 'name', width: 120 },
  { title: '负责客户', dataIndex: 'customers', width: 90, align: 'right' },
  { title: '在途商机', dataIndex: 'openOpps', width: 90, align: 'right' },
  { title: '在途金额', key: 'pipeline', width: 140, align: 'right' },
  { title: '本年赢单', dataIndex: 'wonCount', width: 90, align: 'right' },
  { title: '本年赢单金额', key: 'wonAmount', width: 150, align: 'right' },
  { title: '本月跟进', dataIndex: 'followups', width: 90, align: 'right' }
]

const ticketColumns = [
  { title: '工单类型', key: 'type', width: 100 },
  { title: '工单数', dataIndex: 'count', width: 80, align: 'right' },
  { title: '未关闭', dataIndex: 'open', width: 80, align: 'right' },
  { title: '平均解决时长', key: 'hours', width: 120, align: 'right' },
  { title: '按时解决率', key: 'onTime', width: 110, align: 'right' },
  { title: '平均满意度', key: 'satisfaction', width: 160 }
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
