<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi :label="$t('screen.sales.todayOrders')" :value="money(kpi.todayOrderAmount).value" :unit="money(kpi.todayOrderAmount).unit" :digits="money(kpi.todayOrderAmount).digits"
        :sub="$t('screen.sales.todaySub', { n: fmtNum(kpi.todayOrders), shipped: moneyText(kpi.todayShipped) })" />
      <ScreenKpi :label="$t('screen.sales.monthOrders')" :value="money(kpi.monthOrderAmount).value" :unit="money(kpi.monthOrderAmount).unit" :digits="money(kpi.monthOrderAmount).digits" />
      <ScreenKpi :label="$t('screen.sales.yearOrders')" :value="money(kpi.yearOrderAmount).value" :unit="money(kpi.yearOrderAmount).unit" :digits="money(kpi.yearOrderAmount).digits" :color="COLORS.blue" />
      <ScreenKpi :label="$t('screen.sales.monthShipped')" :value="money(kpi.monthShipped).value" :unit="money(kpi.monthShipped).unit" :digits="money(kpi.monthShipped).digits" :color="COLORS.green" />
      <ScreenKpi :label="$t('screen.sales.yearShipped')" :value="money(kpi.yearShipped).value" :unit="money(kpi.yearShipped).unit" :digits="money(kpi.yearShipped).digits" :color="COLORS.green"
        :sub="growthText" :sub-class="growth === null ? '' : growth >= 0 ? 'up' : 'down'" />
      <ScreenKpi :label="$t('screen.sales.monthReceipt')" :value="money(kpi.monthReceipt).value" :unit="money(kpi.monthReceipt).unit" :digits="money(kpi.monthReceipt).digits" :color="COLORS.purple"
        :sub="$t('screen.sales.yearN', { amount: moneyText(kpi.yearReceipt) })" />
      <ScreenKpi :label="$t('screen.sales.receivable')" :value="money(kpi.receivable).value" :unit="money(kpi.receivable).unit" :digits="money(kpi.receivable).digits" :color="COLORS.orange" />
      <ScreenKpi :label="$t('screen.sales.overdueOrders')" :value="kpi.overdueOrders" :unit="$t('screen.unit.order')" :color="Number(kpi.overdueOrders) ? COLORS.red : COLORS.green"
        :sub="$t('screen.sales.newCustomers', { n: fmtNum(kpi.newCustomers) })" />
    </div>

    <div class="panels sales-top">
      <ScreenPanel :title="$t('screen.sales.monthly')">
        <template #extra>{{ $t('screen.sales.monthlyHint', { unit: chartUnit.unit }) }}</template>
        <EChart :option="monthlyOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.sales.topCustomers')">
        <EChart :option="rankOption(data.topCustomers, COLORS.cyan)" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.sales.topProducts')">
        <EChart :option="rankOption(data.topProducts, COLORS.green)" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.sales.salesmen')">
        <EChart :option="salesmanOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.sales.orders')">
        <ScrollList :columns="orderColumns" :rows="orders" :row-height="42" :empty-text="$t('screen.sales.noOrder')">
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
      <ScreenPanel :title="$t('screen.sales.openOrders')">
        <template #extra>{{ $t('screen.sales.openSub', { n: fmtNum(openOrders), amount: moneyText(openAmount) }) }}</template>
        <EChart :option="statusOption" height="100%" dark />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, axisStyle, chartMoneyUnit, fmtMoney, fmtNum, gradient, hGradient, legend, moneyUnit, shortDay, tooltip, withDictLabel } from '@/components/screen/screen'
import { shortMoney } from '@/components/charts/charts'
import './screen.css'

/** 销售经营大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const { t } = useI18n()
const kpi = computed(() => props.data.kpi || {})

/** 指标卡金额：按 moneyUnit 缩放（中文万元、英文 K / M），缩放后两位小数 */
function money(v) {
  const n = Number(v || 0)
  const u = moneyUnit(n)
  return { value: n / u.div, unit: u.unit, digits: u.div > 1 ? 2 : 0 }
}
const moneyText = (v) => { const m = fmtMoney(v); return `${m.value} ${m.unit}` }
const growth = computed(() => {
  const last = Number(kpi.value.lastYearShipped || 0)
  return last ? ((Number(kpi.value.yearShipped || 0) - last) * 100) / last : null
})
const growthText = computed(() => (growth.value === null ? t('screen.sales.noLastYear') : t('screen.sales.yoy', { rate: `${growth.value >= 0 ? '+' : ''}${growth.value.toFixed(1)}` })))

const chartUnit = chartMoneyUnit()
const wan = (v) => (v === null || v === undefined ? null : Number((Number(v) / chartUnit.div).toFixed(2)))
const monthlyOption = computed(() => {
  const rows = props.data.monthly || []
  return {
    tooltip: tooltip({ valueFormatter: (v) => (v === null || v === undefined ? '-' : `${v} ${chartUnit.unit}`) }),
    legend: legend(),
    grid: { left: 56, right: 16, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: rows.map((r) => t(`screen.month.${Number(r.month)}`)), ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: [
      { name: t('screen.sales.order'), type: 'bar', barWidth: 14, data: rows.map((r) => wan(r.orders)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: t('screen.sales.shipped'), type: 'bar', barWidth: 14, data: rows.map((r) => wan(r.shipped)), itemStyle: { color: gradient(COLORS.green, 0.2) } },
      { name: t('screen.sales.lastYear'), type: 'line', smooth: true, symbolSize: 6, data: rows.map((r) => wan(r.lastYear)), itemStyle: { color: COLORS.orange }, lineStyle: { type: 'dashed' } }
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
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>${moneyText(r.amount)}　${t('screen.common.docsN', { n: r.orders })}` } }),
    grid: { left: 16, right: 16, top: 30, bottom: 30, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => r.name), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, color: COLORS.text } },
    yAxis: { type: 'value', ...axisStyle, axisLabel: { ...axisStyle.axisLabel, formatter: (v) => shortMoney(v) } },
    series: [{
      type: 'bar', barWidth: 22, data: rows.map((r) => Number(r.amount)),
      itemStyle: { color: gradient(COLORS.purple, 0.2), borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', color: COLORS.text, fontSize: 13, formatter: (p) => fmtMoney(p.value).value }
    }]
  }
})

const orderColumns = [
  { key: 'orderDate', title: t('common.field.date'), width: '70px' },
  { key: 'orderNo', title: t('screen.sales.orderNo'), width: '170px' },
  { key: 'customer', title: t('screen.sales.customer'), width: '1fr' },
  { key: 'amount', title: t('common.field.amount'), width: '130px', align: 'right' },
  { key: 'statusLabel', title: t('common.field.status'), width: '110px', align: 'center' }
]
const STATUS_COLOR = { DRAFT: 'gray', APPROVING: 'orange', APPROVED: 'cyan', PARTIAL: 'cyan', COMPLETED: 'green', CLOSED: 'gray' }

const orders = computed(() => withDictLabel(props.data.orders, 'scm_order_status', 'status', 'statusLabel'))
const openOrders = computed(() => (props.data.orderStatus || []).reduce((s, r) => s + Number(r.orders), 0))
const openAmount = computed(() => (props.data.orderStatus || []).reduce((s, r) => s + Number(r.amount || 0), 0))
const statusOption = computed(() => {
  const rows = withDictLabel(props.data.orderStatus, 'scm_order_status', 'status', 'label')
  return {
    color: [COLORS.sub, COLORS.orange, COLORS.cyan, COLORS.green],
    tooltip: tooltip({ trigger: 'item', formatter: (p) => `${p.name}<br/>${t('screen.common.docsN', { n: p.value })} (${p.percent}%)` }),
    legend: legend({ orient: 'vertical', top: 'middle', right: 16, formatter: (n) => { const r = rows.find((x) => x.label === n); return `${n}  ${t('screen.common.docsN', { n: r?.orders ?? 0 })}` } }),
    series: [{
      type: 'pie', radius: ['46%', '70%'], center: ['34%', '52%'],
      itemStyle: { borderColor: '#071a38', borderWidth: 3 },
      label: { show: true, position: 'center', formatter: () => `${openOrders.value}\n${t('screen.unit.order')}`, color: COLORS.text, fontSize: 24, fontWeight: 700, lineHeight: 30 },
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
