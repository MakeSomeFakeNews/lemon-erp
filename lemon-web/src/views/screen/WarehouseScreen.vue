<template>
  <div class="screen-grid">
    <div class="kpis">
      <ScreenKpi :label="$t('screen.warehouse.todayIn')" :value="kpi.todayInQty" :sub="$t('screen.common.docsN', { n: fmtNum(kpi.todayInDocs) })" />
      <ScreenKpi :label="$t('screen.warehouse.todayOut')" :value="kpi.todayOutQty" :color="COLORS.green" :sub="$t('screen.common.docsN', { n: fmtNum(kpi.todayOutDocs) })" />
      <ScreenKpi :label="$t('screen.warehouse.pendingPick')" :value="kpi.pendingPick" :unit="$t('screen.unit.order')" :color="COLORS.orange" :sub="$t('screen.warehouse.pendingPickSub')" />
      <ScreenKpi :label="$t('screen.warehouse.pendingShip')" :value="kpi.pendingShip" :unit="$t('screen.unit.order')" :color="COLORS.orange" :sub="$t('screen.warehouse.pendingShipSub')" />
      <ScreenKpi :label="$t('screen.warehouse.shippedToday')" :value="kpi.shippedToday" :unit="$t('screen.unit.order')" :color="COLORS.green" />
      <ScreenKpi :label="$t('screen.warehouse.transit')" :value="kpi.transit" :unit="$t('screen.unit.order')" :color="COLORS.blue" :sub="$t('screen.warehouse.transitSub')" />
      <ScreenKpi :label="$t('screen.warehouse.skus')" :value="kpi.skus" :unit="$t('screen.unit.kind')" :color="COLORS.purple" :sub="$t('screen.common.totalN', { n: fmtNum(kpi.stockQty) })" />
      <ScreenKpi :label="$t('screen.warehouse.alerts')" :value="alertCount" :unit="$t('screen.unit.item')" :color="alertCount ? COLORS.red : COLORS.green"
        :sub="$t('screen.warehouse.alertSub', { low: fmtNum(kpi.lowStock), expiring: fmtNum(kpi.expiring) })" :sub-class="alertCount ? 'down' : 'up'" />
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.warehouse.trend')">
        <EChart :option="trendOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.warehouse.deliveries')">
        <template #extra>{{ $t('screen.warehouse.byShipDate') }}</template>
        <ScrollList :columns="deliveryColumns" :rows="deliveries" :row-height="42" :empty-text="$t('screen.warehouse.noDelivery')">
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
      <ScreenPanel :title="$t('screen.warehouse.byWarehouse')">
        <EChart :option="warehouseOption" height="100%" dark />
      </ScreenPanel>
    </div>

    <div class="panels">
      <ScreenPanel :title="$t('screen.warehouse.byType')">
        <EChart :option="typeOption" height="100%" dark />
      </ScreenPanel>
      <ScreenPanel :title="$t('screen.warehouse.docs')">
        <ScrollList :columns="docColumns" :rows="docs" :row-height="40" :empty-text="$t('screen.warehouse.noDoc')">
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
      <ScreenPanel :title="$t('screen.warehouse.alerts')">
        <template #extra>{{ $t('screen.warehouse.alertHint') }}</template>
        <ScrollList :columns="alertColumns" :rows="alerts" :row-height="42" :empty-text="$t('screen.warehouse.noAlert')">
          <template #cell="{ column, row }">
            <template v-if="column.key === 'type'">
              <span :class="['screen-tag', row.type === 'LOW' ? 'orange' : 'red']">{{ row.type === 'LOW' ? $t('screen.warehouse.short') : row.expired ? $t('screen.warehouse.expired') : $t('screen.warehouse.expiring') }}</span>
            </template>
            <template v-else-if="column.key === 'detail'">
              <template v-if="row.type === 'LOW'">
                <div class="alert-bar">
                  <div :class="['screen-bar', 'warn']"><i :style="{ width: `${Math.min(100, (Number(row.qty) * 100) / Number(row.safety))}%` }" /></div>
                  <span class="num-cell">{{ fmtNum(row.qty) }} / {{ fmtNum(row.safety) }}</span>
                </div>
              </template>
              <span v-else>{{ $t('screen.warehouse.expiryDetail', { batch: row.batchNo, date: shortDay(row.expiryDate), qty: fmtNum(row.qty) }) }}</span>
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
import { useI18n } from 'vue-i18n'
import EChart from '@/components/charts/EChart.vue'
import ScreenKpi from '@/components/screen/ScreenKpi.vue'
import ScreenPanel from '@/components/screen/ScreenPanel.vue'
import ScrollList from '@/components/screen/ScrollList.vue'
import { COLORS, PALETTE, axisStyle, fmtNum, gradient, hGradient, legend, shortDay, shortTime, tooltip, withDictLabel } from '@/components/screen/screen'
import './screen.css'

/** 仓储物流大屏 */
const props = defineProps({ data: { type: Object, default: () => ({}) } })
const { t } = useI18n()
const kpi = computed(() => props.data.kpi || {})
const today = dayjs().format('YYYY-MM-DD')
const KIND = { IN: t('screen.warehouse.in'), OUT: t('screen.warehouse.out'), TRANSFER: t('screen.warehouse.transfer') }
/** 出入库类型字典：调拨的名称后端已翻译 */
const ioType = (r) => (r.kind === 'IN' ? 'wms_in_type' : r.kind === 'OUT' ? 'wms_out_type' : null)
const deliveries = computed(() => withDictLabel(props.data.deliveries, 'scm_delivery_status', 'status', 'statusLabel'))
const docs = computed(() => (props.data.docs || []).map((r) => (ioType(r) ? withDictLabel([r], ioType, 'bizType', 'typeLabel')[0] : r)))
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
      { name: KIND.IN, type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.inQty)), itemStyle: { color: gradient(COLORS.cyan, 0.2) } },
      { name: KIND.OUT, type: 'bar', barWidth: 12, data: rows.map((r) => Number(r.outQty)), itemStyle: { color: gradient(COLORS.green, 0.2) } }
    ]
  }
})

const deliveryColumns = [
  { key: 'deliveryNo', title: t('screen.warehouse.deliveryNo'), width: '160px' },
  { key: 'customer', title: t('screen.warehouse.customer'), width: '1.4fr' },
  { key: 'warehouse', title: t('screen.warehouse.warehouse'), width: '1fr' },
  { key: 'shipDate', title: t('screen.warehouse.shipDate'), width: '96px', align: 'center' },
  { key: 'statusLabel', title: t('common.field.status'), width: '90px', align: 'center' }
]

const warehouseOption = computed(() => {
  const rows = (props.data.warehouses || []).slice(0, 10)
  return {
    tooltip: tooltip({ formatter: (p) => { const r = rows[p[0].dataIndex]; return `${r.name}<br/>${t('screen.warehouse.warehouseTip', { qty: fmtNum(r.qty, 2), skus: fmtNum(r.skus) })}` } }),
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
  const rows = withDictLabel(props.data.byType, ioType, 'value', 'label')
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
      { text: KIND.IN, left: '25%', top: '47%', textAlign: 'center', textStyle: { color: COLORS.cyan, fontSize: 16 } },
      { text: KIND.OUT, left: '75%', top: '47%', textAlign: 'center', textStyle: { color: COLORS.green, fontSize: 16 } }
    ],
    series: [pie('IN', ['25%', '52%'], KIND.IN), pie('OUT', ['75%', '52%'], KIND.OUT)]
  }
})

const docColumns = [
  { key: 'time', title: t('screen.warehouse.time'), width: '110px' },
  { key: 'docNo', title: t('screen.warehouse.docNo'), width: '160px' },
  { key: 'kind', title: '', width: '80px', align: 'center' },
  { key: 'typeLabel', title: t('common.field.type'), width: '1fr' },
  { key: 'warehouse', title: t('screen.warehouse.warehouse'), width: '1fr' },
  { key: 'qty', title: t('common.field.quantity'), width: '100px', align: 'right' }
]

const alertColumns = [
  { key: 'type', title: '', width: '80px', align: 'center' },
  { key: 'name', title: t('screen.warehouse.material'), width: '1fr' },
  { key: 'detail', title: t('screen.warehouse.alertDetail'), width: '1.3fr' }
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
