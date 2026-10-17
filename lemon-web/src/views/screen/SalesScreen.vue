<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi label="今日订单" :value="money(kpi.todayOrderAmount).value" :unit="money(kpi.todayOrderAmount).unit" :digits="money(kpi.todayOrderAmount).digits"
        :sub="`${fmtNum(kpi.todayOrders)} 单 · 出库 ${moneyText(kpi.todayShipped)}`" />
      <ScreenKpi label="本月订单" :value="money(kpi.monthOrderAmount).value" :unit="money(kpi.monthOrderAmount).unit" :digits="money(kpi.monthOrderAmount).digits" />
      <ScreenKpi label="本年订单" :value="money(kpi.yearOrderAmount).value" :unit="money(kpi.yearOrderAmount).unit" :digits="money(kpi.yearOrderAmount).digits" :color="COLORS.blue" />
      <ScreenKpi label="本月出库" :value="money(kpi.monthShipped).value" :unit="money(kpi.monthShipped).unit" :digits="money(kpi.monthShipped).digits" :color="COLORS.green" />
      <ScreenKpi label="本年出库" :value="money(kpi.yearShipped).value" :unit="money(kpi.yearShipped).unit" :digits="money(kpi.yearShipped).digits" :color="COLORS.green"
        :sub="growthText" :sub-class="growth === null ? '' : growth >= 0 ? 'up' : 'down'" />
      <ScreenKpi label="本月回款" :value="money(kpi.monthReceipt).value" :unit="money(kpi.monthReceipt).unit" :digits="money(kpi.monthReceipt).digits" :color="COLORS.purple"
        :sub="`本年 ${moneyText(kpi.yearReceipt)}`" />
      <ScreenKpi label="应收余额" :value="money(kpi.receivable).value" :unit="money(kpi.receivable).unit" :digits="money(kpi.receivable).digits" :color="COLORS.orange" />
      <ScreenKpi label="逾期未交订单" :value="kpi.overdueOrders" unit="单" :color="Number(kpi.overdueOrders) ? COLORS.red : COLORS.green"
        :sub="`本月新客户 ${fmtNum(kpi.newCustomers)}`" />
    </div>

    <div class="panels sales-top">
      <ScreenPanel title="月度销售趋势">
        <template #extra>订单、出库与上年同期出库（万元）</template>
        <EChart :option="monthlyOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="本年客户排行">
        <EChart :option="rankOption(data.topCustomers, COLORS.cyan)" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="本年产品排行">
        <EChart :option="rankOption(data.topProducts, COLORS.green)" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel title="本月业务员排行">
        <EChart :option="salesmanOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="实时订单">
        <ScrollList :columns="orderColumns" :rows="data.orders || []" :row-height="42" empty-text="还没有销售订单">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'amount'"><span class="num-cell amount">{{ fmtNum(row.amount, 2) }}</span></template>
            <template v-else-if="column.key === 'orderDate'">{{ shortDay(row.orderDate) }}</template>
            <template v-else-if="column.key === 'statusLabel'">
              <span :class="['screen-tag', STATUS_COLOR[row.status] || 'gray']">{{ row.statusLabel }}</span>
            </template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="执行中订单">
        <template #extra>{{ fmtNum(openOrders) }} 单 · {{ moneyText(openAmount) }}</template>
        <EChart :option="statusOption" height="100%" dark />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, axisStyle, fmtMoney, fmtNum, gradient, hGradient, legend, shortDay, tooltip } from '@/components/screen/screen'
import './screen.css'

/** 销售经营大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const kpi = computed(() => props.data.kpi || {})

/** 指标卡金额：一万以上按万元显示两位小数 */
function money(v) {
  const n = Number(v || 0)
  return Math.abs(n) >= 10000 ? { value: n / 10000, unit: '万元', digits: 2 } : { value: n, unit: '元', digits: 0 }
}
const moneyText = (v) => { const m = fmtMoney(v); return `${m.value} ${m.unit}` }
const growth = computed(() => {
  const last = Number(kpi.value.lastYearShipped || 0)
  return last ? ((Number(kpi.value.yearShipped || 0) - last) * 100) / last : null
})
const growthText = computed(() => (growth.value === null ? '上年同期无出库' : `同比 ${growth.value >= 0 ? '+' : ''}${growth.value.toFixed(1)}%`))

const wan = (v) => (v === null || v === undefined ? null : Number((Number(v) / 10000).toFixed(2)))
const monthlyOption = computed(() => {
  const rows = props.data.monthly || []
  return {
    tooltip: tooltip({ valueFormatter: (v) => (v === null || v === undefined ? '-' : `${v} 万元`) }),
    legend: legend(),
    grid: { left: 56, right: 16, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: rows.map((r) => `${r.month}月`), ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: [
      { name: '订单', type: 'bar', barWidth: 14, data: rows.map((r) => wan(r.orders)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: '出库', type: 'bar', barWidth: 14, data: rows.map((r) => wan(r.shipped)), itemStyle: { color: gradient(COLORS.green, 0.2) } },
      { name: '上年出库', type: 'line', smooth: true, symbolSize: 6, data: rows.map((r) => wan(r.lastYear)), itemStyle: { color: COLORS.orange }, lineStyle: { type: 'dashed' } }
    ]
  }
})

function rankOption(list, color) {
  const rows = (list || []).slice(0, 10)
  return {
    tooltip: tooltip({ formatter: (p) => `${rows[p[0].dataIndex].name}<br/>${moneyText(rows[p[0].dataIndex].amount)}` }),
    grid: { left: 130, right: 80, top: 4, bottom: 4 },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category', inverse: true, data: rows.map((r, i) => `${i + 1}. ${r.name}`), ...axisStyle,
      axisLabel: { ...axisStyle.axisLabel, fontSize: 14, color: COLORS.text, width: 120, overflow: 'truncate' }, axisLine: { show: false }, axisTick: { show: false }
    },
    series: [{
      type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.amount)),
      itemStyle: { borderRadius: 6, color: hGradient(color + '33', color) },
      label: { show: true, position: 'right', color: COLORS.text, fontSize: 13, formatter: (p) => moneyText(p.value) }
    }]
  }
}

const salesmanOption = computed(() => {
  const rows = props.data.salesmen || []
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>${moneyText(r.amount)}　${r.orders} 单` } }),
    grid: { left: 50, right: 16, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: rows.map((r) => r.name), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, color: COLORS.text } },
    yAxis: { type: 'value', ...axisStyle, axisLabel: { ...axisStyle.axisLabel, formatter: (v) => (v >= 10000 ? `${v / 10000}万` : v) } },
    series: [{
      type: 'bar', barWidth: 22, data: rows.map((r) => Number(r.amount)),
      itemStyle: { color: gradient(COLORS.purple, 0.2), borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: COLORS.text, fontSize: 13, formatter: (p) => fmtMoney(p.value).value }
    }]
  }
})

const orderColumns = [
  { key: 'orderDate', title: '日期', width: '70px' },
  { key: 'orderNo', title: '订单号', width: '170px' },
  { key: 'customer', title: '客户', width: '1fr' },
  { key: 'amount', title: '金额', width: '130px', align: 'right' },
  { key: 'statusLabel', title: '状态', width: '96px', align: 'center' }
]
const STATUS_COLOR = { DRAFT: 'gray', APPROVING: 'orange', APPROVED: 'cyan', PARTIAL: 'cyan', COMPLETED: 'green', CLOSED: 'gray' }

const openOrders = computed(() => (props.data.orderStatus || []).reduce((s, r) => s + Number(r.orders), 0))
const openAmount = computed(() => (props.data.orderStatus || []).reduce((s, r) => s + Number(r.amount || 0), 0))
const statusOption = computed(() => {
  const rows = props.data.orderStatus || []
  return {
    color: [COLORS.sub, COLORS.orange, COLORS.cyan, COLORS.green],
    tooltip: tooltip({ trigger: 'item', formatter: (p) => `${p.name}<br/>${p.value} 单（${p.percent}%）` }),
    legend: legend({ orient: 'vertical', top: 'middle', right: 16, formatter: (n) => { const r = rows.find((x) => x.label === n); return `${n}  ${r?.orders ?? 0} 单` } }),
    series: [{
      type: 'pie', radius: ['46%', '70%'], center: ['34%', '52%'],
      itemStyle: { borderColor: '#071a38', borderWidth: 3 },
      label: { show: true, position: 'center', formatter: () => `${openOrders.value}\n单`, color: COLORS.text, fontSize: 24, fontWeight: 700, lineHeight: 30 },
      data: rows.map((r) => ({ name: r.label, value: Number(r.orders) }))
    }]
  }
})
</script>

<style scoped>
.sales-top {
  grid-template-columns: 1fr 440px 440px !important;
}
.amount {
  color: #ffb03a;
}
</style>
