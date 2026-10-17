<template>
  <div class="progress-grid">
    <div class="kpis">
      <ScreenKpi :label="$t('screen.progress.active')" :value="kpi.activeOrders" :unit="$t('screen.unit.order')" :color="COLORS.cyan"
        :sub="$t('screen.progress.waitingN', { n: fmtNum(kpi.waitingOrders) })" />
      <ScreenKpi :label="$t('screen.progress.running')" :value="kpi.runningOrders" :unit="$t('screen.unit.order')" :color="COLORS.blue"
        :sub="kpi.idleOrders ? $t('screen.progress.idleN', { n: fmtNum(kpi.idleOrders) }) : $t('screen.progress.noIdle')" :sub-class="kpi.idleOrders ? 'warn' : 'up'" />
      <ScreenKpi :label="$t('screen.progress.lines')" :value="`${kpi.runningLines ?? 0} / ${kpi.lines ?? 0}`" :color="COLORS.green" :sub="$t('screen.progress.linesSub')" />
      <ScreenKpi :label="$t('screen.progress.todayGood')" :value="kpi.todayGood" :color="COLORS.green" :sub="$t('screen.factory.scrapN', { n: fmtNum(kpi.todayScrap) })" />
      <ScreenKpi :label="$t('screen.progress.yield')" :value="kpi.todayYield" :digits="1" unit="%" :color="yieldColor" :sub="$t('screen.progress.hoursN', { n: fmtNum(kpi.todayHours, 1) })" />
      <ScreenKpi :label="$t('screen.progress.completed')" :value="kpi.completedToday" :unit="$t('screen.unit.order')" :color="COLORS.purple"
        :sub="$t('screen.progress.toStockN', { n: fmtNum(kpi.toStockOrders) })" />
      <ScreenKpi :label="$t('screen.progress.risk')" :value="riskCount" :unit="$t('screen.unit.order')" :color="riskCount ? COLORS.red : COLORS.green"
        :sub="$t('screen.progress.riskSub', { late: fmtNum(kpi.atRiskOrders), overdue: fmtNum(kpi.overdueOrders) })" :sub-class="riskCount ? 'down' : 'up'" />
      <ScreenKpi :label="$t('screen.progress.allDone')" :value="kpi.allDoneDate ? shortDay(kpi.allDoneDate) : '-'" :color="COLORS.orange"
        :sub="kpi.activeOrders ? `${etaText(kpi.allDoneDate)} · ${$t('screen.progress.remainingH', { n: fmtNum(kpi.remainingHours, 1) })}` : $t('mes.board.noOrders')" />
    </div>

    <ScreenPanel :title="$t('screen.progress.orders')" class="orders">
      <template #extra>{{ $t('screen.progress.ordersHint') }}</template>
      <ScrollList :columns="orderColumns" :rows="data.orders || []" :row-height="56" :interval="4000" :empty-text="$t('screen.progress.noOrders')">
        <template #cell="{ column, row }">
          <template v-if="column.key === 'wo'">
            <div class="two-line">
              <span class="num-cell">{{ row.woNo }}</span>
              <small>{{ row.product }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'customer'">
            <div class="two-line">
              <span>{{ row.customerName || '-' }}</span>
              <small class="num-cell">{{ row.salesOrderNo || '' }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'steps'">
            <div class="two-line">
              <span class="steps">
                <i v-for="op in row.ops" :key="op.id" :class="['step', op.state.toLowerCase(), { today: Number(op.todayGood) > 0 }]" />
              </span>
              <small>{{ row.currentOp ? `${row.doneOps + 1}/${row.totalOps} ${row.currentOp}` : $t('screen.factory.toStock') }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <div class="progress-cell">
              <div :class="['screen-bar', barClass(row)]"><i :style="{ width: `${Number(row.progress)}%` }" /></div>
              <span class="num-cell">{{ Math.round(Number(row.progress)) }}%</span>
            </div>
          </template>
          <template v-else-if="column.key === 'eta'">
            <div class="two-line center">
              <span :class="['num-cell', riskClass(row.risk)]">{{ row.eta ? shortDay(row.eta) : '-' }}</span>
              <small>{{ etaText(row.eta) }}</small>
            </div>
          </template>
          <template v-else-if="column.key === 'due'">
            <span class="num-cell">{{ shortDay(row.dueDate || row.planEnd) || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'risk'">
            <span :class="['screen-tag', RISK_TAG[row.risk]]">{{ riskLabel(row.risk) }}</span>
          </template>
        </template>
      </ScrollList>
    </ScreenPanel>

    <ScreenPanel :title="$t('screen.progress.lineStatus')" class="lines-panel">
      <template #extra>
        <span v-for="(c, s) in LINE_COLORS" :key="s" class="legend-item"><i :style="{ background: c }" />{{ lineStateLabel(s) }}</span>
      </template>
      <div class="lines">
        <div v-for="l in lines" :key="l.id" class="line" :style="{ '--c': LINE_COLORS[l.state] }">
          <div class="line-head">
            <span class="line-name"><i class="line-dot" />{{ l.name }}</span>
            <span class="line-state">{{ lineStateLabel(l.state) }}</span>
          </div>
          <div class="line-row">
            <span>{{ $t('screen.progress.lineGood') }} <b class="num-cell">{{ fmtNum(l.todayGood) }}</b></span>
            <span>{{ $t('screen.progress.lineQueue') }} <b class="num-cell">{{ l.queueOrders }}</b></span>
          </div>
          <div class="line-util">
            <div :class="['screen-bar', Number(l.utilization) > 100 ? 'danger' : '']"><i :style="{ width: `${Math.min(100, Number(l.utilization || 0))}%` }" /></div>
            <span class="num-cell">{{ l.utilization == null ? '-' : `${Math.round(Number(l.utilization))}%` }}</span>
          </div>
          <div class="line-job">
            <template v-if="currentJob(l)">{{ currentJob(l).woNo }} · {{ currentJob(l).opName }}</template>
            <template v-else>{{ l.queueOrders ? $t('screen.progress.notStarted') : $t('screen.progress.noJob') }}</template>
          </div>
          <div class="line-clear">{{ l.queueOrders ? $t('screen.progress.clearAt', { d: l.clearDate ? shortDay(l.clearDate) : '-' }) : '' }}</div>
        </div>
        <div v-if="!lines.length" class="empty">{{ $t('mes.board.noLines') }}</div>
      </div>
    </ScreenPanel>

    <ScreenPanel :title="$t('screen.progress.hourly')" class="hourly">
      <EChart :option="hourlyOption" height="100%" dark />
    </ScreenPanel>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, axisStyle, fmtNum, gradient, legend, shortDay, tooltip } from '@/components/screen/screen'
import { etaText, lineStateLabel, riskLabel } from '@/components/mes/board'
import { DeliveryRisk } from '@/constants/enums'
import './screen.css'

/** 生产进度大屏：与「生产看板」相同的数据，在制工单的工序进度与预计完工、各产线状态、今日每小时产出 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const { t } = useI18n()
const kpi = computed(() => props.data.kpi || {})
const lines = computed(() => props.data.lines || [])
const riskCount = computed(() => (kpi.value.atRiskOrders || 0) + (kpi.value.overdueOrders || 0))
const yieldColor = computed(() => {
  const v = kpi.value.todayYield
  return v === null || v === undefined ? COLORS.cyan : Number(v) >= 98 ? COLORS.green : Number(v) >= 95 ? COLORS.orange : COLORS.red
})

const LINE_COLORS = { RUNNING: COLORS.green, IDLE: COLORS.orange, FREE: COLORS.blue, REST: COLORS.sub }
const RISK_TAG = { ON_TIME: 'green', AT_RISK: 'orange', OVERDUE: 'red', UNKNOWN: 'gray' }
const riskClass = (r) => (r === DeliveryRisk.OVERDUE ? 'bad' : r === DeliveryRisk.AT_RISK ? 'warn' : '')
const barClass = (row) => (row.risk === DeliveryRisk.OVERDUE ? 'danger' : row.risk === DeliveryRisk.AT_RISK ? 'warn' : '')
const currentJob = (l) => l.jobs.find((j) => j.running)

const orderColumns = [
  { key: 'wo', title: t('screen.progress.colWo'), width: '1.3fr' },
  { key: 'customer', title: t('screen.progress.colCustomer'), width: '1fr' },
  { key: 'steps', title: t('screen.progress.colSteps'), width: '1.3fr' },
  { key: 'progress', title: t('screen.progress.colProgress'), width: '150px' },
  { key: 'eta', title: t('screen.progress.colEta'), width: '96px', align: 'center' },
  { key: 'due', title: t('screen.progress.colDue'), width: '70px', align: 'center' },
  { key: 'risk', title: t('screen.progress.colRisk'), width: '96px', align: 'center' }
]

// ---------------------------------------------------------------- 每小时产出
const hourlyOption = computed(() => {
  const rows = props.data.hourly || []
  const hours = rows.map((r) => r.hour)
  const from = Math.min(8, ...hours)
  const to = Math.max(18, ...hours)
  const axis = []
  for (let h = from; h <= to; h++) axis.push(h)
  const value = (h, k) => Number(rows.find((r) => r.hour === h)?.[k] || 0)
  return {
    tooltip: tooltip(),
    legend: legend(),
    grid: { left: 50, right: 16, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: axis.map((h) => `${String(h).padStart(2, '0')}:00`), ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: [
      { name: t('screen.factory.good'), type: 'bar', stack: 'q', barWidth: 16, data: axis.map((h) => value(h, 'good')), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: t('screen.factory.scrap'), type: 'bar', stack: 'q', barWidth: 16, data: axis.map((h) => value(h, 'scrap')), itemStyle: { color: gradient(COLORS.orange, 0.2) } }
    ]
  }
})
</script>

<style scoped>
.progress-grid {
  display: grid;
  grid-template-rows: 118px 1fr 1fr;
  grid-template-columns: 1.65fr 1fr;
  gap: 16px;
  height: 100%;
}
.progress-grid .kpis {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 14px;
}
.orders {
  grid-row: 2 / 4;
}
.num-cell {
  font-family: 'DIN Alternate', 'Bahnschrift', 'Roboto Mono', 'SF Mono', Consolas, monospace;
}
.two-line {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  min-width: 0;
}
.two-line > * {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.two-line.center {
  align-items: center;
}
.two-line small {
  font-size: 13px;
  color: rgba(160, 190, 230, 0.75);
}
.steps {
  display: flex;
  gap: 3px;
  padding: 3px 0;
}
.step {
  flex: 1;
  max-width: 30px;
  height: 10px;
  border-radius: 2px;
  background: rgba(64, 158, 255, 0.18);
}
.step.done {
  background: #2fe6a5;
}
.step.doing {
  background: #00e4ff;
}
.step.doing.today {
  box-shadow: 0 0 8px #00e4ff;
  animation: blink 1.6s infinite;
}
.progress-cell {
  display: grid;
  grid-template-columns: 1fr 48px;
  align-items: center;
  gap: 8px;
}
.warn {
  color: #ffb03a;
}
.bad {
  color: #ff5b5b;
  font-weight: 600;
}
.legend-item {
  margin-left: 12px;
}
.legend-item i {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 4px;
  border-radius: 50%;
}
.lines {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 156px;
  gap: 10px;
  height: 100%;
  overflow: hidden;
  align-content: start;
}
.line {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  background: color-mix(in srgb, var(--c) 9%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 45%, transparent);
  font-size: 13px;
  color: rgba(160, 190, 230, 0.9);
  min-width: 0;
}
.line-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.line-name {
  font-size: 15px;
  color: rgba(225, 238, 255, 0.95);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.line-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 6px;
  border-radius: 50%;
  background: var(--c);
  box-shadow: 0 0 8px var(--c);
}
.line-state {
  color: var(--c);
  flex: none;
}
.line-row {
  display: flex;
  justify-content: space-between;
}
.line-row b {
  color: rgba(225, 238, 255, 0.95);
  font-size: 16px;
}
.line-util {
  display: grid;
  grid-template-columns: 1fr 44px;
  align-items: center;
  gap: 6px;
}
.line-job,
.line-clear {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.line-clear {
  color: rgba(160, 190, 230, 0.65);
}
.empty {
  grid-column: 1 / -1;
  align-self: center;
  text-align: center;
  color: rgba(160, 190, 230, 0.6);
}
@keyframes blink {
  50% { opacity: 0.4; }
}
</style>
