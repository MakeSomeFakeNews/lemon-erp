<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi label="本月检验" :value="kpi.inspections" unit="批" />
      <ScreenKpi label="综合合格率" :value="kpi.passRate" :digits="1" unit="%" :color="rateColor(kpi.passRate)" />
      <ScreenKpi label="来料合格率" :value="kpi.iqcRate" :digits="1" unit="%" :color="rateColor(kpi.iqcRate)" />
      <ScreenKpi label="过程 / 成品合格率" :value="kpi.processRate" :digits="1" unit="%" :color="rateColor(kpi.processRate)" />
      <ScreenKpi label="生产良品率" :value="kpi.productionYield" :digits="1" unit="%" :color="rateColor(kpi.productionYield)" sub="本月报工" />
      <ScreenKpi label="本月报废" :value="kpi.scrapQty" :color="Number(kpi.scrapQty) ? COLORS.orange : COLORS.green" />
      <ScreenKpi label="待处置不合格品" :value="kpi.openNcr" unit="单" :color="Number(kpi.openNcr) ? COLORS.red : COLORS.green" />
      <ScreenKpi label="本月不合格品" :value="kpi.monthNcr" unit="单" :color="COLORS.purple" :sub="`已处置 ${fmtNum(kpi.doneNcr)} 单`" />
    </div>

    <div class="panels">
      <ScreenPanel title="近 14 天检验合格率">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="各类检验合格率">
        <template #extra>本月已确认的检验</template>
        <EChart :option="typeOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="不良类型柏拉图">
        <template #extra>本月不合格 / 让步接收</template>
        <EChart :option="paretoOption" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel title="供应商来料质量">
        <template #extra>本月，按合格率从低到高</template>
        <EChart :option="supplierOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="最近检验">
        <ScrollList :columns="inspectionColumns" :rows="data.inspections || []" :row-height="40" empty-text="还没有检验记录">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'date'">{{ shortDay(row.date) }}</template>
            <template v-else-if="column.key === 'resultLabel'">
              <span :class="['screen-tag', RESULT_COLOR[row.result] || 'gray']">{{ row.resultLabel }}</span>
            </template>
            <template v-else-if="column.key === 'defectQty'">
              <span :class="['num-cell', { bad: Number(row.defectQty) }]">{{ fmtNum(row.defectQty) }}</span>
            </template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="待处置不合格品">
        <ScrollList :columns="ncrColumns" :rows="data.ncrs || []" :row-height="42" empty-text="没有待处置的不合格品">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'qty'"><span class="num-cell bad">{{ fmtNum(row.qty) }}</span></template>
            <template v-else-if="column.key === 'statusLabel'"><span class="screen-tag orange">{{ row.statusLabel }}</span></template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
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
import { COLORS, axisStyle, fmtNum, gradient, hGradient, legend, shortDay, tooltip } from '@/components/screen/screen'
import './screen.css'

/** 质量管理大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const kpi = computed(() => props.data.kpi || {})
const rateColor = (v) => (v === null || v === undefined ? COLORS.cyan : Number(v) >= 98 ? COLORS.green : Number(v) >= 95 ? COLORS.orange : COLORS.red)
const RESULT_COLOR = { PASS: 'green', FAIL: 'red', CONCESSION: 'orange' }

const trendOption = computed(() => {
  const rows = props.data.trend || []
  return {
    tooltip: tooltip(),
    legend: legend(),
    grid: { left: 46, right: 50, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: rows.map((r) => shortDay(r.day)), ...axisStyle },
    yAxis: [
      { type: 'value', minInterval: 1, ...axisStyle },
      { type: 'value', min: 0, max: 100, ...axisStyle, splitLine: { show: false }, axisLabel: { ...axisStyle.axisLabel, formatter: '{value}%' } }
    ],
    series: [
      { name: '检验批次', type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.total)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: '合格率', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, connectNulls: true, data: rows.map((r) => (r.rate === null ? null : Number(r.rate))), itemStyle: { color: COLORS.green } }
    ]
  }
})

/** 各类检验一个仪表盘 */
const typeOption = computed(() => {
  const rows = (props.data.byType || []).slice(0, 4)
  const n = Math.max(rows.length, 1)
  return {
    title: rows.length ? undefined : { text: '本月还没有检验', left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    series: rows.map((r, i) => ({
      type: 'gauge', center: [`${((i + 0.5) * 100) / n}%`, '55%'], radius: n > 2 ? '62%' : '70%',
      startAngle: 220, endAngle: -40, min: 0, max: 100,
      progress: { show: true, width: 10, itemStyle: { color: rateColor(r.rate) } },
      axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(64, 158, 255, 0.15)']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, pointer: { show: false },
      title: { show: true, offsetCenter: [0, '72%'], color: COLORS.text, fontSize: 15 },
      detail: { valueAnimation: true, offsetCenter: [0, '0%'], fontSize: 24, fontWeight: 700, color: rateColor(r.rate), formatter: (v) => (r.rate === null ? '-' : `${v}%`) },
      data: [{ value: r.rate === null ? 0 : Number(r.rate), name: `${r.label} ${r.total} 批` }]
    }))
  }
})

const paretoOption = computed(() => {
  const rows = props.data.defects || []
  const total = rows.reduce((s, r) => s + Number(r.times), 0)
  let acc = 0
  const cum = rows.map((r) => { acc += Number(r.times); return total ? Number(((acc * 100) / total).toFixed(1)) : 0 })
  return {
    tooltip: tooltip(),
    title: rows.length ? undefined : { text: '本月没有不良记录', left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    grid: { left: 40, right: 46, top: 24, bottom: 40 },
    xAxis: { type: 'category', data: rows.map((r) => r.label), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 0, rotate: rows.length > 5 ? 30 : 0 } },
    yAxis: [
      { type: 'value', minInterval: 1, ...axisStyle },
      { type: 'value', min: 0, max: 100, ...axisStyle, splitLine: { show: false }, axisLabel: { ...axisStyle.axisLabel, formatter: '{value}%' } }
    ],
    series: [
      { name: '次数', type: 'bar', barWidth: 20, data: rows.map((r) => Number(r.times)), itemStyle: { color: gradient(COLORS.orange, 0.2) } },
      { name: '累计占比', type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, data: cum, itemStyle: { color: COLORS.red } }
    ]
  }
})

const supplierOption = computed(() => {
  const rows = props.data.suppliers || []
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>合格率 ${r.rate}%<br/>检验 ${r.total} 批，不合格 ${r.fail} 批` } }),
    title: rows.length ? undefined : { text: '本月没有来料检验', left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    grid: { left: 130, right: 70, top: 4, bottom: 4 },
    xAxis: { type: 'value', min: 0, max: 100, show: false },
    yAxis: { type: 'category', inverse: true, data: rows.map((r) => r.name), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, fontSize: 14, color: COLORS.text, width: 120, overflow: 'truncate' }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.rate)),
      showBackground: true, backgroundStyle: { color: 'rgba(64, 158, 255, 0.12)', borderRadius: 6 },
      itemStyle: { borderRadius: 6, color: (p) => (p.value >= 98 ? hGradient('#0a9f6b', COLORS.green) : p.value >= 95 ? hGradient('#ff8a00', COLORS.orange) : hGradient('#d4380d', COLORS.red)) },
      label: { show: true, position: 'right', color: COLORS.text, fontSize: 14, formatter: '{c}%' }
    }]
  }
})

const inspectionColumns = [
  { key: 'date', title: '日期', width: '64px' },
  { key: 'inspNo', title: '检验单', width: '150px' },
  { key: 'typeLabel', title: '类型', width: '90px' },
  { key: 'material', title: '物料', width: '1fr' },
  { key: 'defectQty', title: '不良', width: '60px', align: 'right' },
  { key: 'resultLabel', title: '结论', width: '96px', align: 'center' }
]
const ncrColumns = [
  { key: 'ncrNo', title: '处理单', width: '140px' },
  { key: 'material', title: '物料', width: '1fr' },
  { key: 'defectLabel', title: '不良类型', width: '90px' },
  { key: 'qty', title: '数量', width: '64px', align: 'right' },
  { key: 'statusLabel', title: '状态', width: '84px', align: 'center' }
]
</script>

<style scoped>
.bad {
  color: #ff5b5b;
}
</style>
