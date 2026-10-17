<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi label="今日入库" :value="kpi.todayInQty" :sub="`${fmtNum(kpi.todayInDocs)} 单`" />
      <ScreenKpi label="今日出库" :value="kpi.todayOutQty" :color="COLORS.green" :sub="`${fmtNum(kpi.todayOutDocs)} 单`" />
      <ScreenKpi label="待拣货" :value="kpi.pendingPick" unit="单" :color="COLORS.orange" sub="发货通知待拣货" />
      <ScreenKpi label="待出库" :value="kpi.pendingShip" unit="单" :color="COLORS.orange" sub="已拣货待出库" />
      <ScreenKpi label="今日已发货" :value="kpi.shippedToday" unit="单" :color="COLORS.green" />
      <ScreenKpi label="在途调拨" :value="kpi.transit" unit="单" :color="COLORS.blue" sub="已调出待收货" />
      <ScreenKpi label="在库物料" :value="kpi.skus" unit="种" :color="COLORS.purple" :sub="`合计 ${fmtNum(kpi.stockQty)}`" />
      <ScreenKpi label="库存预警" :value="alertCount" unit="项" :color="alertCount ? COLORS.red : COLORS.green"
        :sub="`低于安全库存 ${fmtNum(kpi.lowStock)} · 临期 ${fmtNum(kpi.expiring)}`" :sub-class="alertCount ? 'down' : 'up'" />
    </div>

    <div class="panels">
      <ScreenPanel title="近 14 天出入库">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="待发货">
        <template #extra>按计划发货日期</template>
        <ScrollList :columns="deliveryColumns" :rows="data.deliveries || []" :row-height="42" empty-text="没有待发货的通知">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'shipDate'">
              <span :class="{ overdue: row.shipDate < today }">{{ shortDay(row.shipDate) }}</span>
            </template>
            <template v-else-if="column.key === 'statusLabel'">
              <span :class="['screen-tag', row.status === 'PICKED' ? 'cyan' : 'orange']">{{ row.statusLabel }}</span>
            </template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="各仓库库存">
        <EChart :option="warehouseOption" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel title="本月出入库构成">
        <EChart :option="typeOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel title="实时出入库">
        <ScrollList :columns="docColumns" :rows="data.docs || []" :row-height="40" empty-text="还没有出入库">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'time'">{{ shortTime(row.time) }}</template>
            <template v-else-if="column.key === 'kind'">
              <span :class="['screen-tag', row.kind === 'IN' ? 'cyan' : row.kind === 'OUT' ? 'green' : 'gray']">{{ KIND[row.kind] }}</span>
            </template>
            <template v-else-if="column.key === 'qty'"><span class="num-cell">{{ fmtNum(row.qty, 2) }}</span></template>
            <template v-else>{{ row[column.key] || '-' }}</template>
          </template>
        </ScrollList>
      </ScreenPanel>
      <ScreenPanel title="库存预警">
        <template #extra>低于安全库存、30 天内到期</template>
        <ScrollList :columns="alertColumns" :rows="alerts" :row-height="42" empty-text="没有库存预警">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'type'">
              <span :class="['screen-tag', row.type === 'LOW' ? 'orange' : 'red']">{{ row.type === 'LOW' ? '缺货' : row.expired ? '已过期' : '临期' }}</span>
            </template>
            <template v-else-if="column.key === 'detail'">
              <template v-if="row.type === 'LOW'">
                <div class="alert-bar">
                  <div :class="['screen-bar', 'warn']"><i :style="{ width: `${Math.min(100, (Number(row.qty) * 100) / Number(row.safety))}%` }" /></div>
                  <span class="num-cell">{{ fmtNum(row.qty) }} / {{ fmtNum(row.safety) }}</span>
                </div>
              </template>
              <span v-else>{{ row.batchNo }} · {{ shortDay(row.expiryDate) }} 到期 · {{ fmtNum(row.qty) }}</span>
            </template>
            <template v-else>{{ row[column.key] }}</template>
          </template>
        </ScrollList>
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
import { COLORS, PALETTE, axisStyle, fmtNum, gradient, hGradient, legend, shortDay, shortTime, tooltip } from '@/components/screen/screen'
import './screen.css'

/** 仓储物流大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const kpi = computed(() => props.data.kpi || {})
const today = dayjs().format('YYYY-MM-DD')
const KIND = { IN: '入库', OUT: '出库', TRANSFER: '调拨' }
const alertCount = computed(() => Number(kpi.value.lowStock || 0) + Number(kpi.value.expiring || 0))

const trendOption = computed(() => {
  const rows = props.data.trend || []
  return {
    tooltip: tooltip(),
    legend: legend(),
    grid: { left: 56, right: 16, top: 36, bottom: 26 },
    xAxis: { type: 'category', data: rows.map((r) => shortDay(r.day)), ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: [
      { name: '入库', type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.inQty)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: '出库', type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.outQty)), itemStyle: { color: gradient(COLORS.green, 0.2) } }
    ]
  }
})

const deliveryColumns = [
  { key: 'deliveryNo', title: '发货通知', width: '160px' },
  { key: 'customer', title: '客户', width: '1.4fr' },
  { key: 'warehouse', title: '仓库', width: '1fr' },
  { key: 'shipDate', title: '发货日期', width: '90px', align: 'center' },
  { key: 'statusLabel', title: '状态', width: '90px', align: 'center' }
]

const warehouseOption = computed(() => {
  const rows = (props.data.warehouses || []).slice(0, 10)
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>库存 ${fmtNum(r.qty, 2)}<br/>物料 ${fmtNum(r.skus)} 种` } }),
    grid: { left: 110, right: 90, top: 8, bottom: 8 },
    xAxis: { type: 'value', show: false },
    yAxis: { type: 'category', inverse: true, data: rows.map((r) => r.name), ...axisStyle, axisLabel: { ...axisStyle.axisLabel, fontSize: 14, color: COLORS.text }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{
      type: 'bar', barWidth: 14, data: rows.map((r) => Number(r.qty)),
      showBackground: true, backgroundStyle: { color: 'rgba(64, 158, 255, 0.12)', borderRadius: 7 },
      itemStyle: { borderRadius: 7, color: hGradient('#0a7cff', COLORS.cyan) },
      label: { show: true, position: 'right', color: COLORS.text, fontSize: 14, formatter: (p) => fmtNum(p.value) }
    }]
  }
})

/** 本月出入库按类型：入库在左，出库在右 */
const typeOption = computed(() => {
  const rows = props.data.byType || []
  const pie = (kind, center, name) => ({
    name, type: 'pie', radius: ['36%', '56%'], center,
    // 小于约 5% 的扇区不显示标签，避免重叠（悬停可看）
    minShowLabelAngle: 18,
    itemStyle: { borderColor: '#071a38', borderWidth: 2 },
    label: { color: COLORS.text, fontSize: 13, formatter: '{b}\n{d}%' },
    labelLine: { length: 8, length2: 8 },
    data: rows.filter((r) => r.kind === kind).map((r) => ({ name: r.label, value: Number(r.qty) }))
  })
  return {
    color: PALETTE,
    tooltip: tooltip({ trigger: 'item', formatter: (p) => `${p.seriesName}<br/>${p.name} ${fmtNum(p.value, 2)}（${p.percent}%）` }),
    title: [
      { text: '入库', left: '25%', top: '47%', textAlign: 'center', textStyle: { color: COLORS.cyan, fontSize: 16 } },
      { text: '出库', left: '75%', top: '47%', textAlign: 'center', textStyle: { color: COLORS.green, fontSize: 16 } }
    ],
    series: [pie('IN', ['25%', '52%'], '入库'), pie('OUT', ['75%', '52%'], '出库')]
  }
})

const docColumns = [
  { key: 'time', title: '时间', width: '110px' },
  { key: 'docNo', title: '单号', width: '160px' },
  { key: 'kind', title: '', width: '64px', align: 'center' },
  { key: 'typeLabel', title: '类型', width: '1fr' },
  { key: 'warehouse', title: '仓库', width: '1fr' },
  { key: 'qty', title: '数量', width: '100px', align: 'right' }
]

const alertColumns = [
  { key: 'type', title: '', width: '72px', align: 'center' },
  { key: 'name', title: '物料', width: '1fr' },
  { key: 'detail', title: '库存 / 安全库存、批次', width: '1.3fr' }
]
const alerts = computed(() => [
  ...(props.data.lowStock || []).map((r) => ({ ...r, type: 'LOW' })),
  ...(props.data.expiring || []).map((r) => ({ ...r, type: 'EXPIRY', expired: r.expiryDate < today }))
])
</script>

<style scoped>
.overdue {
  color: #ff5b5b;
  font-weight: 600;
}
.alert-bar {
  display: grid;
  grid-template-columns: 1fr 110px;
  align-items: center;
  gap: 8px;
  text-align: right;
}
</style>
