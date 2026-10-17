<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi :label="$t('screen.factory.todayGood')" :value="kpi.todayGood" :sub="$t('screen.factory.scrapN', { n: fmtNum(kpi.todayScrap) })" />
      <ScreenKpi :label="$t('screen.factory.todayYield')" :value="kpi.todayYield" :digits="1" unit="%" :color="rateColor(kpi.todayYield)"
        :sub="$t('screen.factory.monthRate', { rate: fmtRate(kpi.monthYield) })" />
      <ScreenKpi :label="$t('screen.factory.monthGood')" :value="kpi.monthGood" :color="COLORS.green" />
      <ScreenKpi v-if="kpi.todayFinishedIn !== null && kpi.todayFinishedIn !== undefined" :label="$t('screen.factory.todayFinishedIn')" :value="kpi.todayFinishedIn" :color="COLORS.green" />
      <ScreenKpi v-else :label="$t('screen.factory.monthYield')" :value="kpi.monthYield" :digits="1" unit="%" :color="rateColor(kpi.monthYield)" />
      <ScreenKpi :label="$t('screen.factory.todayHours')" :value="kpi.todayHours" :digits="1" :unit="$t('screen.unit.hour')" :color="COLORS.blue" />
      <ScreenKpi :label="$t('screen.factory.activeOrders')" :value="kpi.activeOrders" :unit="$t('screen.unit.order')" :color="COLORS.cyan" :sub="$t('screen.factory.completedToday', { n: fmtNum(kpi.completedOrders) })" />
      <ScreenKpi :label="$t('screen.factory.overdueOrders')" :value="kpi.overdueOrders" :unit="$t('screen.unit.order')" :color="Number(kpi.overdueOrders) ? COLORS.red : COLORS.green"
        :sub="Number(kpi.overdueOrders) ? $t('screen.factory.pastDue') : $t('screen.factory.allOnTime')" :sub-class="Number(kpi.overdueOrders) ? 'down' : 'up'" />
      <ScreenKpi :label="$t('screen.factory.runningRate')" :value="kpi.runningRate" :digits="1" unit="%" :color="COLORS.purple"
        :sub="$t('screen.factory.runningN', { n: fmtNum(kpi.runningEquipments), total: fmtNum(kpi.equipments) })" />
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.factory.trend')">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.factory.woProgress')">
        <template #extra>{{ $t('screen.factory.byPlanEnd') }}</template>
        <ScrollList :columns="woColumns" :rows="data.workOrders || []" :row-height="42" :empty-text="$t('screen.factory.noWo')">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'progress'">
              <div class="progress-cell">
                <div :class="['screen-bar', { danger: isOverdue(row) }]"><i :style="{ width: `${progress(row)}%` }" /></div>
                <span class="num-cell">{{ progress(row) }}%</span>
              </div>
            </template>
            <template v-else-if="column.key === 'planEnd'">
              <span :class="{ overdue: isOverdue(row) }">{{ shortDay(row.planEnd) || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'qty'">
              <span class="num-cell">{{ fmtNum(row.doneQty) }} / {{ fmtNum(row.qty) }}</span>
            </template>
            <template v-else-if="column.key === 'currentOp'">{{ row.currentOp || $t('screen.factory.toStock') }}</template>
            <template v-else>{{ row[column.key] }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.factory.equipment')">
        <template #extra>
          <span v-for="s in statusSummary" :key="s.status" class="legend-item"><i :style="{ background: s.color }" />{{ s.label }} {{ s.count }}</span>
        </template>
        <div class="equipments">
          <div v-for="e in equipments" :key="e.code" class="equipment" :style="{ '--c': statusColor(e.status) }">
            <div class="eq-name"><i class="eq-dot" />{{ e.name }}</div>
            <div class="eq-meta">
              <span>{{ e.statusLabel }}</span>
              <span class="num-cell">{{ $t('screen.factory.todayN', { n: fmtNum(e.good) }) }}</span>
            </div>
          </div>
          <div v-if="!equipments.length" class="empty">{{ $t('screen.factory.noEquipment') }}</div>
        </div>
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.factory.load')">
        <template #extra>{{ $t('screen.factory.loadHint') }}</template>
        <EChart :option="loadOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.factory.reports')">
        <ScrollList :columns="reportColumns" :rows="data.reports || []" :row-height="40" :empty-text="$t('screen.factory.noReport')">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'time'">{{ shortTime(row.time) }}</template>
            <template v-else-if="column.key === 'good'"><span class="num-cell good">{{ fmtNum(row.good) }}</span></template>
            <template v-else-if="column.key === 'scrap'"><span :class="['num-cell', { bad: Number(row.scrap) }]">{{ fmtNum(row.scrap) }}</span></template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.factory.stops')">
        <template #extra>{{ $t('screen.factory.stopTotal', { n: fmtNum(stopTotal, 1) }) }}</template>
        <EChart :option="stopOption" height="100%" dark />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, PALETTE, axisStyle, fmtNum, fmtRate, gradient, hGradient, legend, shortDay, shortTime, tooltip, withDictLabel } from '@/components/screen/screen'
import './screen.css'

/** 生产车间大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const { t } = useI18n()
const kpi = computed(() => props.data.kpi || {})
const today = dayjs().format('YYYY-MM-DD')

const rateColor = (v) => (v === null || v === undefined ? COLORS.cyan : Number(v) >= 98 ? COLORS.green : Number(v) >= 95 ? COLORS.orange : COLORS.red)

// ---------------------------------------------------------------- 产量趋势
const trendOption = computed(() => {
  const rows = props.data.trend || []
  return {
    tooltip: tooltip(),
    legend: legend(),
    grid: { left: 50, right: 46, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: rows.map((r) => shortDay(r.day)), ...axisStyle },
    yAxis: [
      { type: 'value', ...axisStyle },
      { type: 'value', min: (v) => Math.max(0, Math.floor(v.min - 2)), max: 100, ...axisStyle, splitLine: { show: false }, axisLabel: { ...axisStyle.axisLabel, formatter: '{value}%' } }
    ],
    series: [
      { name: t('screen.factory.good'), type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.good)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: t('screen.factory.scrap'), type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.scrap)), itemStyle: { color: gradient(COLORS.orange, 0.2) } },
      {
        name: t('screen.factory.yield'), type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, itemStyle: { color: COLORS.green },
        data: rows.map((r) => {
          const all = Number(r.good) + Number(r.scrap)
          return all ? Number(((Number(r.good) * 100) / all).toFixed(1)) : null
        })
      }
    ]
  }
})

// ---------------------------------------------------------------- 在制工单
const woColumns = [
  { key: 'woNo', title: t('screen.factory.woNo'), width: '140px' },
  { key: 'product', title: t('screen.factory.product'), width: '1.3fr' },
  { key: 'currentOp', title: t('screen.factory.currentOp'), width: '1fr' },
  { key: 'qty', title: t('screen.factory.donePlan'), width: '110px', align: 'right' },
  { key: 'progress', title: t('screen.factory.progress'), width: '124px' },
  { key: 'planEnd', title: t('screen.factory.planEnd'), width: '80px', align: 'center' }
]
const progress = (r) => (Number(r.qty) ? Math.min(100, Math.round((Number(r.doneQty || 0) * 100) / Number(r.qty))) : 0)
const isOverdue = (r) => !!r.planEnd && r.planEnd < today

// ---------------------------------------------------------------- 设备
const STATUS = {
  RUNNING: { label: t('screen.factory.running'), color: COLORS.green },
  IDLE: { label: t('screen.factory.idle'), color: COLORS.orange },
  REPAIR: { label: t('screen.factory.repair'), color: COLORS.red }
}
const statusColor = (s) => STATUS[s]?.color || COLORS.sub
const equipments = computed(() => withDictLabel(props.data.equipments, 'mes_equipment_status', 'status', 'statusLabel'))
const statusSummary = computed(() =>
  Object.entries(STATUS).map(([status, s]) => ({ status, ...s, count: equipments.value.filter((e) => e.status === status).length }))
)

// ---------------------------------------------------------------- 工作中心负荷
const loadOption = computed(() => {
  const rows = (props.data.workCenters || []).slice(0, 10)
  const load = rows.map((r) => (Number(r.capacity) ? Number(((Number(r.hours) * 100) / Number(r.capacity)).toFixed(1)) : 0))
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>${t('screen.factory.loadTip', { load: load[p[0].dataIndex], hours: fmtNum(r.hours, 1), capacity: fmtNum(r.capacity, 1), good: fmtNum(r.good), scrap: fmtNum(r.scrap) })}` } }),
    grid: { left: 110, right: 60, top: 8, bottom: 8 },
    xAxis: { type: 'value', max: (v) => Math.max(100, Math.ceil(v.max)), show: false },
    yAxis: { type: 'category', inverse: true, data: rows.map((r) => r.name), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, fontSize: 14, color: COLORS.text }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', barWidth: 14, data: load,
      showBackground: true, backgroundStyle: { color: 'rgba(64, 158, 255, 0.12)', borderRadius: 7 },
      itemStyle: { borderRadius: 7, color: (p) => (p.value > 100 ? hGradient('#d4380d', COLORS.red) : p.value >= 80 ? hGradient('#ff8a00', COLORS.orange) : hGradient('#0a7cff', COLORS.cyan)) },
      label: { show: true, position: 'right', color: COLORS.text, formatter: '{c}%', fontSize: 14 }
    }]
  }
})

// ---------------------------------------------------------------- 实时报工
const reportColumns = [
  { key: 'time', title: t('screen.factory.time'), width: '104px' },
  { key: 'woNo', title: t('screen.factory.wo'), width: '140px' },
  { key: 'op', title: t('screen.factory.op'), width: '1fr' },
  { key: 'operator', title: t('screen.factory.operator'), width: '90px' },
  { key: 'equipment', title: t('screen.factory.equipmentCol'), width: '1fr' },
  { key: 'good', title: t('screen.factory.good'), width: '80px', align: 'right' },
  { key: 'scrap', title: t('screen.factory.scrap'), width: '70px', align: 'right' }
]

// ---------------------------------------------------------------- 停机原因
const stopTotal = computed(() => (props.data.stopReasons || []).reduce((s, r) => s + Number(r.hours), 0))
const stopOption = computed(() => {
  const rows = withDictLabel(props.data.stopReasons, 'mes_stop_reason', 'value', 'label')
  return {
    color: PALETTE,
    tooltip: tooltip({ trigger: 'item', formatter: (p) => `${p.name}<br/>${t('screen.factory.stopTip', { hours: p.value, percent: p.percent })}` }),
    legend: legend({ orient: 'vertical', top: 'middle', right: 10, formatter: (n) => { const r = rows.find((x) => x.label === n); return `${n}  ${fmtNum(r?.hours, 1)} h` } }),
    title: rows.length ? undefined : { text: t('screen.factory.noStop'), left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['34%', '52%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#071a38', borderWidth: 3 },
      label: { show: false },
      data: rows.map((r) => ({ name: r.label, value: Number(r.hours) }))
    }]
  }
})
</script>

<style scoped>
.progress-cell {
  display: grid;
  grid-template-columns: 1fr 48px;
  align-items: center;
  gap: 8px;
}
.overdue {
  color: #ff5b5b;
  font-weight: 600;
}
.good {
  color: #2fe6a5;
}
.bad {
  color: #ff5b5b;
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
.equipments {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 64px;
  gap: 10px;
  height: 100%;
  overflow: hidden;
  align-content: start;
}
.equipment {
  padding: 8px 10px;
  background: color-mix(in srgb, var(--c) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 45%, transparent);
}
.eq-name {
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.eq-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 6px;
  border-radius: 50%;
  background: var(--c);
  box-shadow: 0 0 8px var(--c);
  animation: blink 1.6s infinite;
}
.eq-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 13px;
  color: rgba(160, 190, 230, 0.85);
}
.empty {
  grid-column: 1 / -1;
  align-self: center;
  text-align: center;
  color: rgba(160, 190, 230, 0.6);
}
@keyframes blink {
  50% { opacity: 0.35; }
}
</style>
