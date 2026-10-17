<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi :label="$t('screen.quality.inspections')" :value="kpi.inspections" :unit="$t('screen.unit.lot')" />
      <ScreenKpi :label="$t('screen.quality.passRate')" :value="kpi.passRate" :digits="1" unit="%" :color="rateColor(kpi.passRate)" />
      <ScreenKpi :label="$t('screen.quality.iqcRate')" :value="kpi.iqcRate" :digits="1" unit="%" :color="rateColor(kpi.iqcRate)" />
      <ScreenKpi :label="$t('screen.quality.processRate')" :value="kpi.processRate" :digits="1" unit="%" :color="rateColor(kpi.processRate)" />
      <ScreenKpi :label="$t('screen.quality.productionYield')" :value="kpi.productionYield" :digits="1" unit="%" :color="rateColor(kpi.productionYield)" :sub="$t('screen.quality.fromReports')" />
      <ScreenKpi :label="$t('screen.quality.scrapQty')" :value="kpi.scrapQty" :color="Number(kpi.scrapQty) ? COLORS.orange : COLORS.green" />
      <ScreenKpi :label="$t('screen.quality.openNcr')" :value="kpi.openNcr" :unit="$t('screen.unit.order')" :color="Number(kpi.openNcr) ? COLORS.red : COLORS.green" />
      <ScreenKpi :label="$t('screen.quality.monthNcr')" :value="kpi.monthNcr" :unit="$t('screen.unit.order')" :color="COLORS.purple" :sub="$t('screen.quality.doneNcr', { n: fmtNum(kpi.doneNcr) })" />
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.quality.trend')">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.quality.byType')">
        <template #extra>{{ $t('screen.quality.byTypeHint') }}</template>
        <EChart :option="typeOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.quality.pareto')">
        <template #extra>{{ $t('screen.quality.paretoHint') }}</template>
        <EChart :option="paretoOption" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.quality.suppliers')">
        <template #extra>{{ $t('screen.quality.suppliersHint') }}</template>
        <EChart :option="supplierOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.quality.recent')">
        <ScrollList :columns="inspectionColumns" :rows="inspections" :row-height="40" :empty-text="$t('screen.quality.noInspection')">
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
      <ScreenPanel :title="$t('screen.quality.openNcr')">
        <ScrollList :columns="ncrColumns" :rows="ncrs" :row-height="42" :empty-text="$t('screen.quality.noNcr')">
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
import { useI18n } from 'vue-i18n'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, axisStyle, fmtNum, gradient, hGradient, legend, shortDay, tooltip, withDictLabel } from '@/components/screen/screen'
import './screen.css'

/** 质量管理大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const { t } = useI18n()
const kpi = computed(() => props.data.kpi || {})
const rateColor = (v) => (v === null || v === undefined ? COLORS.cyan : Number(v) >= 98 ? COLORS.green : Number(v) >= 95 ? COLORS.orange : COLORS.red)
const inspections = computed(() =>
  withDictLabel(withDictLabel(props.data.inspections, 'mes_qc_type', 'type', 'typeLabel'), 'mes_qc_result', 'result', 'resultLabel'))
const ncrs = computed(() => {
  const rows = withDictLabel(props.data.ncrs, 'qms_ncr_source', 'source', 'sourceLabel')
  return withDictLabel(withDictLabel(rows, 'mes_defect_type', 'defectType', 'defectLabel'), 'qms_ncr_status', 'status', 'statusLabel')
})
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
      { name: t('screen.quality.lots'), type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.total)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: t('screen.quality.rate'), type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, connectNulls: true, data: rows.map((r) => (r.rate === null ? null : Number(r.rate))), itemStyle: { color: COLORS.green } }
    ]
  }
})

/** 各类检验一个仪表盘 */
const typeOption = computed(() => {
  const rows = withDictLabel(props.data.byType, 'mes_qc_type', 'type', 'label').slice(0, 4)
  const n = Math.max(rows.length, 1)
  return {
    title: rows.length ? undefined : { text: t('screen.quality.noInspectionMonth'), left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    series: rows.map((r, i) => ({
      type: 'gauge', center: [`${((i + 0.5) * 100) / n}%`, '55%'], radius: n > 2 ? '62%' : '70%',
      startAngle: 220, endAngle: -40, min: 0, max: 100,
      progress: { show: true, width: 10, itemStyle: { color: rateColor(r.rate) } },
      axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(64, 158, 255, 0.15)']] } },
      axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, pointer: { show: false },
      title: { show: true, offsetCenter: [0, '72%'], color: COLORS.text, fontSize: 15 },
      detail: { valueAnimation: true, offsetCenter: [0, '0%'], fontSize: 24, fontWeight: 700, color: rateColor(r.rate), formatter: (v) => (r.rate === null ? '-' : `${v}%`) },
      data: [{ value: r.rate === null ? 0 : Number(r.rate), name: `${r.label} ${t('screen.quality.lotsN', { n: r.total })}` }]
    }))
  }
})

const paretoOption = computed(() => {
  const rows = withDictLabel(props.data.defects, 'mes_defect_type', 'value', 'label')
  const total = rows.reduce((s, r) => s + Number(r.times), 0)
  let acc = 0
  const cum = rows.map((r) => { acc += Number(r.times); return total ? Number(((acc * 100) / total).toFixed(1)) : 0 })
  return {
    tooltip: tooltip(),
    title: rows.length ? undefined : { text: t('screen.quality.noDefect'), left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
    grid: { left: 40, right: 46, top: 24, bottom: 40 },
    xAxis: { type: 'category', data: rows.map((r) => r.label), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, interval: 0, rotate: rows.length > 5 ? 30 : 0 } },
    yAxis: [
      { type: 'value', minInterval: 1, ...axisStyle },
      { type: 'value', min: 0, max: 100, ...axisStyle, splitLine: { show: false }, axisLabel: { ...axisStyle.axisLabel, formatter: '{value}%' } }
    ],
    series: [
      { name: t('screen.quality.times'), type: 'bar', barWidth: 20, data: rows.map((r) => Number(r.times)), itemStyle: { color: gradient(COLORS.orange, 0.2) } },
      { name: t('screen.quality.cumulative'), type: 'line', yAxisIndex: 1, smooth: true, symbolSize: 6, data: cum, itemStyle: { color: COLORS.red } }
    ]
  }
})

const supplierOption = computed(() => {
  const rows = props.data.suppliers || []
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>${t('screen.quality.supplierTip', { rate: r.rate, total: r.total, fail: r.fail })}` } }),
    title: rows.length ? undefined : { text: t('screen.quality.noIqc'), left: 'center', top: 'middle', textStyle: { color: COLORS.sub, fontSize: 15, fontWeight: 400 } },
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
  { key: 'date', title: t('common.field.date'), width: '64px' },
  { key: 'inspNo', title: t('screen.quality.inspNo'), width: '150px' },
  { key: 'typeLabel', title: t('common.field.type'), width: '100px' },
  { key: 'material', title: t('screen.quality.material'), width: '1fr' },
  { key: 'defectQty', title: t('screen.quality.defect'), width: '70px', align: 'right' },
  { key: 'resultLabel', title: t('screen.quality.result'), width: '110px', align: 'center' }
]
const ncrColumns = [
  { key: 'ncrNo', title: t('screen.quality.ncrNo'), width: '140px' },
  { key: 'material', title: t('screen.quality.material'), width: '1fr' },
  { key: 'defectLabel', title: t('screen.quality.defectType'), width: '100px' },
  { key: 'qty', title: t('common.field.quantity'), width: '64px', align: 'right' },
  { key: 'statusLabel', title: t('common.field.status'), width: '100px', align: 'center' }
]
</script>

<style scoped>
.bad {
  color: #ff5b5b;
}
</style>
