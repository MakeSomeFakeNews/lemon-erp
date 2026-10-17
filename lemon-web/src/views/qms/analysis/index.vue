<template>
  <div class="page">
    <a-card :bordered="false" class="page-card block">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadAll" />
        <span class="sub-text">{{ $t('qms.analysis.tip') }}</span>
      </div>
      <a-row :gutter="[16, 16]">
        <a-col v-for="s in typeStats" :key="s.type" :xs="12" :md="6">
          <div class="stat">
            <div class="stat-label"><DictTag type="mes_qc_type" :value="s.type" /></div>
            <div :class="['stat-value', { danger: s.rate < 95 }]">{{ percent(s.rate) }}</div>
            <div class="sub-text">{{ $t('qms.analysis.batches', { total: s.total, fail: s.fail }) }}</div>
          </div>
        </a-col>
        <a-col v-if="!typeStats.length" :span="24"><a-empty :description="$t('qms.analysis.noInspection')" /></a-col>
      </a-row>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" class="page-card block" :title="$t('qms.analysis.trend')">
          <EChart :option="trendOption" height="320px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" class="page-card block" :title="$t('qms.analysis.pareto')">
          <EChart v-if="pareto.length" :option="paretoOption" height="320px" />
          <a-empty v-else :description="$t('qms.analysis.noFail')" class="empty" />
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false" class="page-card" :title="$t('qms.analysis.supplierRank')">
      <a-table row-key="groupKey" size="small" :columns="supplierColumns" :data-source="suppliers" :loading="loading" :pagination="false" :scroll="{ x: 800 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'rank'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'supplier'">{{ record.groupName }} <span class="sub-text">{{ record.groupKey }}</span></template>
          <template v-else-if="column.key === 'counts'">
            {{ $t('qms.analysis.counts', { total: record.totalCount, pass: record.passCount, fail: record.failCount, concession: record.concessionCount }) }}
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
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { percent } from '@/components/charts/charts'
import { formatQty } from '@/components/wms/wms'
import { qualityPareto, qualityTrend, supplierQuality } from '@/api/qms'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'

import { QcType } from '@/constants/enums'
/** 质量分析：合格率趋势、不良柏拉图、供应商排名 */
const dict = useDict('mes_qc_type', 'mes_defect_type')
const { t } = useI18n()
const range = ref([dayjs().subtract(11, 'month').startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const trend = ref([])
const pareto = ref([])
const suppliers = ref([])
const loading = ref(false)

async function loadAll() {
  const params = { beginDate: range.value[0], endDate: range.value[1] }
  loading.value = true
  try {
    const [tr, p, s] = await Promise.all([qualityTrend(params), qualityPareto(params), supplierQuality(params)])
    trend.value = tr
    pareto.value = p
    // 合格率低的排在前面，便于关注
    suppliers.value = [...s].sort((a, b) => Number(a.passRate ?? 100) - Number(b.passRate ?? 100) || b.totalCount - a.totalCount)
  } finally {
    loading.value = false
  }
}
onMounted(loadAll)

const TYPES = [QcType.IQC, QcType.IPQC, QcType.FQC, QcType.FAI]
const rate = (pass, total) => (total ? Math.round((pass / total) * 1000) / 10 : null)
const typeStats = computed(() =>
  TYPES.map((type) => {
    const rows = trend.value.filter((r) => r.type === type)
    const total = rows.reduce((s, r) => s + r.total, 0)
    const pass = rows.reduce((s, r) => s + r.pass, 0)
    return { type, total, fail: total - pass, rate: rate(pass, total) }
  }).filter((s) => s.total > 0)
)

const dictLabel = (list, v) => findDict(list, v)?.label
const typeLabel = (v) => dictLabel(dict.mes_qc_type, v) || v
const trendOption = computed(() => {
  const months = [...new Set(trend.value.map((r) => r.month))].sort()
  const types = TYPES.filter((ty) => trend.value.some((r) => r.type === ty))
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : `${v}%`) },
    legend: { top: 0 },
    grid: { left: 48, right: 16, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: types.map((ty) => ({
      name: typeLabel(ty),
      type: 'line',
      connectNulls: true,
      data: months.map((m) => {
        const r = trend.value.find((x) => x.month === m && x.type === ty)
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
    xAxis: { type: 'category', data: pareto.value.map((r) => dictLabel(dict.mes_defect_type, r.defectType) || (r.defectType === 'OTHER' ? t('qms.analysis.other') : r.defectType)) },
    yAxis: [
      { type: 'value', name: t('qms.analysis.defectCount') },
      { type: 'value', name: t('qms.analysis.cumulative'), max: 100, axisLabel: { formatter: '{value}%' }, splitLine: { show: false } }
    ],
    series: [
      { name: t('qms.analysis.defectCount'), type: 'bar', data: pareto.value.map((r) => Number(r.qty || 0)), barMaxWidth: 40 },
      { name: t('qms.analysis.cumulativePct'), type: 'line', yAxisIndex: 1, data: cumulative, tooltip: { valueFormatter: (v) => `${v}%` },
        markLine: { silent: true, symbol: 'none', data: [{ yAxis: 80, label: { formatter: '80%', position: 'insideStartTop' } }] } }
    ]
  }
})

const supplierColumns = [
  { title: t('qms.analysis.rank'), key: 'rank', width: 70 },
  { title: t('mes.mrp.supplier'), key: 'supplier', width: 240 },
  { title: t('qms.analysis.incomingBatches'), key: 'counts', width: 360 },
  { title: t('qms.analysis.passRate'), key: 'passRate', width: 200 },
  { title: t('qms.analysis.defectRate'), key: 'defectRate', width: 150 }
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
