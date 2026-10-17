<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi label="今日报工合格" :value="kpi.todayGood" :sub="`报废 ${fmtNum(kpi.todayScrap)}`" />
      <ScreenKpi label="今日良品率" :value="kpi.todayYield" :digits="1" unit="%" :color="rateColor(kpi.todayYield)"
        :sub="`本月 ${fmtRate(kpi.monthYield)}%`" />
      <ScreenKpi label="本月报工合格" :value="kpi.monthGood" :color="COLORS.green" />
      <ScreenKpi v-if="kpi.todayFinishedIn !== null && kpi.todayFinishedIn !== undefined" label="今日完工入库" :value="kpi.todayFinishedIn" :color="COLORS.green" />
      <ScreenKpi v-else label="本月良品率" :value="kpi.monthYield" :digits="1" unit="%" :color="rateColor(kpi.monthYield)" />
      <ScreenKpi label="今日报工工时" :value="kpi.todayHours" :digits="1" unit="小时" :color="COLORS.blue" />
      <ScreenKpi label="在制工单" :value="kpi.activeOrders" unit="单" :color="COLORS.cyan" :sub="`今日完工 ${fmtNum(kpi.completedOrders)} 单`" />
      <ScreenKpi label="逾期工单" :value="kpi.overdueOrders" unit="单" :color="Number(kpi.overdueOrders) ? COLORS.red : COLORS.green"
        :sub="Number(kpi.overdueOrders) ? '超过计划完工日期' : '全部按期'" :sub-class="Number(kpi.overdueOrders) ? 'down' : 'up'" />
      <ScreenKpi label="设备运行率" :value="kpi.runningRate" :digits="1" unit="%" :color="COLORS.purple"
        :sub="`运行 ${fmtNum(kpi.runningEquipments)} / ${fmtNum(kpi.equipments)} 台`" />
    </div>

    <div class="panels">
      <ScreenPanel title="近 14 天产量趋势">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="在制工单进度">
        <template #extra>按计划完工日期</template>
        <ScrollList :columns="woColumns" :rows="data.workOrders || []" :row-height="42" empty-text="没有在制工单">
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
            <template v-else-if="column.key === 'currentOp'">{{ row.currentOp || '待入库' }}</template>
            <template v-else>{{ row[column.key] }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="设备状态">
        <template #extra>
          <span v-for="s in statusSummary" :key="s.status" class="legend-item"><i :style="{ background: s.color }" />{{ s.label }} {{ s.count }}</span>
        </template>
        <div class="equipments">
          <div v-for="e in equipments" :key="e.code" class="equipment" :style="{ '--c': statusColor(e.status) }">
            <div class="eq-name"><i class="eq-dot" />{{ e.name }}</div>
            <div class="eq-meta">
              <span>{{ e.statusLabel }}</span>
              <span class="num-cell">今日 {{ fmtNum(e.good) }}</span>
            </div>
          </div>
          <div v-if="!equipments.length" class="empty">没有设备</div>
        </div>
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel title="工作中心今日负荷">
        <template #extra>报工工时 ÷ 日产能</template>
        <EChart :option="loadOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="实时报工">
        <ScrollList :columns="reportColumns" :rows="data.reports || []" :row-height="40" empty-text="还没有报工">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'time'">{{ shortTime(row.time) }}</template>
            <template v-else-if="column.key === 'good'"><span class="num-cell good">{{ fmtNum(row.good) }}</span></template>
            <template v-else-if="column.key === 'scrap'"><span :class="['num-cell', { bad: Number(row.scrap) }]">{{ fmtNum(row.scrap) }}</span></template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="本月停机原因">
        <template #extra>合计 {{ fmtNum(stopTotal, 1) }} 小时</template>
        <EChart :option="stopOption" height="100%" dark />
      </ScreenPanel>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, PALETTE, axisStyle, fmtNum, fmtRate, gradient, hGradient, legend, shortDay, shortTime, tooltip } from '@/components/screen/screen'
import './screen.css'

/** 生产车间大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
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
      { name: '合格', type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.good)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: '报废', type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.scrap)), itemStyle: { color: gradient(COLORS.orange, 0.2) } },
      {
        name: '良品率', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, itemStyle: { color: COLORS.green },
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
  { key: 'woNo', title: '工单号', width: '140px' },
  { key: 'product', title: '产品', width: '1.3fr' },
  { key: 'currentOp', title: '当前工序', width: '1fr' },
  { key: 'qty', title: '完成/计划', width: '96px', align: 'right' },
  { key: 'progress', title: '进度', width: '124px' },
  { key: 'planEnd', title: '计划完工', width: '76px', align: 'center' }
]
const progress = (r) => (Number(r.qty) ? Math.min(100, Math.round((Number(r.doneQty || 0) * 100) / Number(r.qty))) : 0)
const isOverdue = (r) => !!r.planEnd && r.planEnd < today

// ---------------------------------------------------------------- 设备
const STATUS = {
  RUNNING: { label: '运行', color: COLORS.green },
  IDLE: { label: '闲置', color: COLORS.orange },
  REPAIR: { label: '维修', color: COLORS.red }
}
const statusColor = (s) => STATUS[s]?.color || COLORS.sub
const equipments = computed(() => props.data.equipments || [])
const statusSummary = computed(() =>
  Object.entries(STATUS).map(([status, s]) => ({ status, ...s, count: equipments.value.filter((e) => e.status === status).length }))
)

// ---------------------------------------------------------------- 工作中心负荷
const loadOption = computed(() => {
  const rows = (props.data.workCenters || []).slice(0, 10)
  const load = rows.map((r) => (Number(r.capacity) ? Number(((Number(r.hours) * 100) / Number(r.capacity)).toFixed(1)) : 0))
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>负荷 ${load[p[0].dataIndex]}%<br/>工时 ${fmtNum(r.hours, 1)} / ${fmtNum(r.capacity, 1)}<br/>合格 ${fmtNum(r.good)}　报废 ${fmtNum(r.scrap)}` } }),
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
  { key: 'time', title: '时间', width: '104px' },
  { key: 'woNo', title: '工单', width: '140px' },
  { key: 'op', title: '工序', width: '1fr' },
  { key: 'operator', title: '操作员', width: '90px' },
  { key: 'equipment', title: '设备', width: '1fr' },
  { key: 'good', title: '合格', width: '80px', align: 'right' },
  { key: 'scrap', title: '报废', width: '70px', align: 'right' }
]

// ---------------------------------------------------------------- 停机原因
const stopTotal = computed(() => (props.data.stopReasons || []).reduce((s, r) => s + Number(r.hours), 0))
const stopOption = computed(() => {
  const rows = props.data.stopReasons || []
  return {
    color: PALETTE,
    tooltip: tooltip({ trigger: 'item', formatter: '{b}<br/>{c} 小时（{d}%）' }),
    legend: legend({ orient: 'vertical', top: 'middle', right: 10, formatter: (n) => { const r = rows.find((x) => x.label === n); return `${n}  ${fmtNum(r?.hours, 1)} h` } }),
    title: rows.length ? undefined : { text: '本月没有停机记录', left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
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
