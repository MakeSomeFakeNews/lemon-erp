<template>
  <div class="page">
    <a-card :bordered="false" class="page-card block">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadAll" />
        <span class="sub-text">已确认的检验单；批次合格率中让步接收计为合格</span>
      </div>
      <a-row :gutter="[16, 16]">
        <a-col v-for="t in typeStats" :key="t.type" :xs="12" :md="6">
          <div class="stat">
            <div class="stat-label"><DictTag type="mes_qc_type" :value="t.type" /></div>
            <div :class="['stat-value', { danger: t.rate < 95 }]">{{ percent(t.rate) }}</div>
            <div class="sub-text">{{ t.total }} 批，不合格 {{ t.fail }} 批</div>
          </div>
        </a-col>
        <a-col v-if="!typeStats.length" :span="24"><a-empty description="期间内没有已确认的检验单" /></a-col>
      </a-row>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" class="page-card block" title="批次合格率趋势">
          <EChart :option="trendOption" height="320px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" class="page-card block" title="不良类型柏拉图">
          <EChart v-if="pareto.length" :option="paretoOption" height="320px" />
          <a-empty v-else description="期间内没有不合格检验" class="empty" />
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="page-card" title="供应商来料质量排名">
      <a-table row-key="groupKey" size="small" :columns="supplierColumns" :data-source="suppliers" :loading="loading" :pagination="false" :scroll="{ x: 800 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'rank'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'supplier'">{{ record.groupName }} <span class="sub-text">{{ record.groupKey }}</span></template>
          <template v-else-if="column.key === 'counts'">
            {{ record.totalCount }} 批：合格 {{ record.passCount }} · 不合格 {{ record.failCount }} · 让步 {{ record.concessionCount }}
          </template>
          <template v-else-if="column.key === 'passRate'">
            <a-progress :percent="Number(record.passRate || 0)" size="small" :status="record.passRate < 95 ? 'exception' : 'success'" />
          </template>
          <template v-else-if="column.key === 'defectRate'">
            <span :class="{ danger: record.defectRate > 1 }">{{ record.defectRate != null ? record.defectRate + '%' : '-' }}</span>
            <div class="sub-text">{{ formatQty(record.defectQty) }} / {{ formatQty(record.sampleQty) }}</div>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { percent } from '@/components/charts/charts'
import { formatQty } from '@/components/wms/wms'
import { qualityPareto, qualityTrend, supplierQuality } from '@/api/qms'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

/** 质量分析：合格率趋势、不良柏拉图、供应商排名 */
const dict = useDict('mes_qc_type', 'mes_defect_type')
const range = ref([dayjs().subtract(11, 'month').startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const trend = ref([])
const pareto = ref([])
const suppliers = ref([])
const loading = ref(false)

async function loadAll() {
  const params = { beginDate: range.value[0], endDate: range.value[1] }
  loading.value = true
  try {
    const [t, p, s] = await Promise.all([qualityTrend(params), qualityPareto(params), supplierQuality(params)])
    trend.value = t
    pareto.value = p
    // 合格率低的排在前面，便于关注
    suppliers.value = [...s].sort((a, b) => Number(a.passRate ?? 100) - Number(b.passRate ?? 100) || b.totalCount - a.totalCount)
  } finally {
    loading.value = false
  }
}
onMounted(loadAll)

const TYPES = ['IQC', 'IPQC', 'FQC', 'FAI']
const rate = (pass, total) => (total ? Math.round((pass / total) * 1000) / 10 : null)
const typeStats = computed(() =>
  TYPES.map((type) => {
    const rows = trend.value.filter((r) => r.type === type)
    const total = rows.reduce((s, r) => s + r.total, 0)
    const pass = rows.reduce((s, r) => s + r.pass, 0)
    return { type, total, fail: total - pass, rate: rate(pass, total) }
  }).filter((t) => t.total > 0)
)

const dictLabel = (list, v) => findDict(list, v)?.label
const typeLabel = (t) => dictLabel(dict.mes_qc_type, t) || t
const trendOption = computed(() => {
  const months = [...new Set(trend.value.map((r) => r.month))].sort()
  const types = TYPES.filter((t) => trend.value.some((r) => r.type === t))
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : `${v}%`) },
    legend: { top: 0 },
    grid: { left: 48, right: 16, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: types.map((t) => ({
      name: typeLabel(t),
      type: 'line',
      connectNulls: true,
      data: months.map((m) => {
        const r = trend.value.find((x) => x.month === m && x.type === t)
        return r ? rate(r.pass, r.total) : null
      })
    }))
  }
})

const paretoOption = computed(() => {
  const total = pareto.value.reduce((s, r) => s + Number(r.qty || 0), 0)
  let acc = 0
  const cumulative = pareto.value.map((r) => {
    acc += Number(r.qty || 0)
    return total ? Math.round((acc / total) * 1000) / 10 : 0
  })
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 48, right: 48, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: pareto.value.map((r) => dictLabel(dict.mes_defect_type, r.defectType) || (r.defectType === 'OTHER' ? '其他' : r.defectType)) },
    yAxis: [
      { type: 'value', name: '不良数' },
      { type: 'value', name: '累计', max: 100, axisLabel: { formatter: '{value}%' }, splitLine: { show: false } }
    ],
    series: [
      { name: '不良数', type: 'bar', data: pareto.value.map((r) => Number(r.qty || 0)), barMaxWidth: 40 },
      { name: '累计占比', type: 'line', yAxisIndex: 1, data: cumulative, tooltip: { valueFormatter: (v) => `${v}%` },
        markLine: { silent: true, symbol: 'none', data: [{ yAxis: 80, label: { formatter: '80%', position: 'insideStartTop' } }] } }
    ]
  }
})

const supplierColumns = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '供应商', key: 'supplier', width: 240 },
  { title: '来料批次', key: 'counts', width: 300 },
  { title: '批次合格率', key: 'passRate', width: 200 },
  { title: '抽样不良率', key: 'defectRate', width: 140 }
]
</script>

<style scoped>
.page {
  padding-bottom: 16px;
}
.block {
  margin-bottom: 16px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-fill, rgba(0, 0, 0, 0.02));
}
.stat-label {
  margin-bottom: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--status-critical);
}
.empty {
  padding: 80px 0;
}
</style>
