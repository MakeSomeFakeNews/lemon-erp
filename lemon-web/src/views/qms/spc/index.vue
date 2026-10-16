<template>
  <div class="page">
    <a-card :bordered="false" class="page-card block">
      <div class="filters">
        <a-select
          v-model:value="itemId"
          show-search
          option-filter-prop="label"
          :options="itemOptions"
          placeholder="选择定量检验项目"
          class="item-select"
          @change="loadSpc"
        />
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" @change="loadSpc" />
        <span class="sub-text">每张已确认检验单的实测值为一个子组；子组大小 1 用单值-移动极差图，2 ~ 10 用均值-极差图，大于 10 用均值-标准差图</span>
      </div>
      <a-empty v-if="!items.length" description="还没有录入实测值的定量检验项目：在检验方案中添加定量项目，检验时按方案录入实测值" />
      <template v-else-if="spc">
        <a-row :gutter="[16, 16]">
          <a-col :xs="12" :md="4">
            <div class="stat"><div class="sub-text">规格</div><div class="stat-value small">{{ specText(spc) }}</div></div>
          </a-col>
          <a-col :xs="12" :md="4">
            <div class="stat"><div class="sub-text">子组 / 子组大小</div><div class="stat-value">{{ spc.points.length }} / {{ spc.n || '-' }}</div></div>
          </a-col>
          <a-col :xs="12" :md="4">
            <div class="stat"><div class="sub-text">过程均值</div><div class="stat-value">{{ fmt(spc.meanCenter) }}</div></div>
          </a-col>
          <a-col :xs="12" :md="4">
            <div class="stat">
              <div class="sub-text">估计标准差 σ</div>
              <div class="stat-value">{{ fmt(spc.sigma) }}</div>
              <div class="sub-text">{{ SIGMA[spc.chart] }}</div>
            </div>
          </a-col>
          <a-col :xs="12" :md="4">
            <div class="stat"><div class="sub-text">Cp</div><div :class="['stat-value', capClass(spc.cp)]">{{ fmt(spc.cp) }}</div></div>
          </a-col>
          <a-col :xs="12" :md="4">
            <div class="stat">
              <div class="sub-text">Cpk</div>
              <div :class="['stat-value', capClass(spc.cpk)]">{{ fmt(spc.cpk) }}</div>
              <div v-if="spc.meanCenter != null" class="sub-text">{{ capText(spc.cpk) }}</div>
            </div>
          </a-col>
        </a-row>
        <a-alert v-for="s in spc.signals" :key="s" type="warning" show-icon :message="s" class="signal" />
      </template>
    </a-card>

    <a-row v-if="spc && spc.meanCenter != null" :gutter="16">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card block" :title="CHARTS[spc.chart][0]">
          <EChart :option="chartOption('mean', spc.meanCenter, spc.meanUcl, spc.meanLcl)" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card block" :title="CHARTS[spc.chart][1]">
          <EChart :option="chartOption('spread', spc.spreadCenter, spc.spreadUcl, spc.spreadLcl)" height="300px" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EChart from '@/components/charts/EChart.vue'
import { specText } from '@/components/qms/qms'
import { spcChart, spcItems } from '@/api/qms'

/** SPC：定量检验项目的控制图（I-MR / X̄-R / X̄-S）与过程能力指数 */
const CHARTS = {
  I_MR: ['单值图', '移动极差图', '移动极差'],
  XBAR_R: ['均值图', '极差图', '极差'],
  XBAR_S: ['均值图', '标准差图', '标准差']
}
const SIGMA = { I_MR: '平均移动极差 / d2', XBAR_R: '平均极差 / d2', XBAR_S: '平均标准差 / c4' }
const items = ref([])
const itemId = ref()
const range = ref()
const spc = ref()

const itemOptions = computed(() =>
  items.value.map((i) => ({
    value: i.planItemId,
    label: `${i.itemName}${i.unit ? `（${i.unit}）` : ''} - ${i.planName}${i.materialName ? ' / ' + i.materialName : ''}，${i.subgroups} 组`
  }))
)

onMounted(async () => {
  items.value = await spcItems()
  if (items.value.length) {
    itemId.value = items.value[0].planItemId
    loadSpc()
  }
})

async function loadSpc() {
  if (!itemId.value) return
  spc.value = await spcChart({ planItemId: itemId.value, beginDate: range.value?.[0], endDate: range.value?.[1] })
}

const fmt = (v) => (v == null ? '-' : Number(v).toString())
/** 过程能力：≥ 1.33 充足，1.0 ~ 1.33 尚可，< 1.0 不足 */
const capClass = (v) => (v == null ? '' : Number(v) >= 1.33 ? 'good' : Number(v) >= 1 ? 'warn' : 'danger')
const capText = (v) => (v == null ? '需要规格限' : Number(v) >= 1.67 ? '能力充分' : Number(v) >= 1.33 ? '能力充足' : Number(v) >= 1 ? '能力尚可，需关注' : '能力不足，需改进')

function axisRange(values) {
  const lo = Math.min(...values)
  const hi = Math.max(...values)
  const pad = (hi - lo) * 0.1 || Math.abs(hi) * 0.01 || 1
  const digits = Math.max(0, 2 - Math.floor(Math.log10(hi - lo || 1)))
  const round = (v) => Number(v.toFixed(digits))
  return { min: round(lo - pad), max: round(hi + pad) }
}

function chartOption(field, center, ucl, lcl) {
  const points = spc.value.points.filter((p) => p[field] != null)
  const out = (v) => v > Number(ucl) || v < Number(lcl)
  const line = (name, value, color, type = 'dashed') => ({
    name, yAxis: Number(value), lineStyle: { color, type }, label: { formatter: `${name} ${Number(value)}`, position: 'insideEndTop' }
  })
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (ps) => {
        const p = points[ps[0].dataIndex]
        const label = field === 'mean' ? (spc.value.chart === 'I_MR' ? '单值' : '均值') : CHARTS[spc.value.chart][2]
        return `${p.inspNo}<br/>${p.date}<br/>${label}：${Number(p[field])}`
      }
    },
    grid: { left: 56, right: 24, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: points.map((p) => p.date) },
    // 坐标范围包含控制限，否则超出数据范围的控制限不显示
    yAxis: { type: 'value', ...axisRange(points.map((p) => Number(p[field])).concat([Number(ucl), Number(lcl)])) },
    series: [
      {
        type: 'line',
        data: points.map((p) => {
          const v = Number(p[field])
          return out(v) ? { value: v, itemStyle: { color: '#f5222d' }, symbolSize: 9 } : v
        }),
        symbolSize: 6,
        markLine: {
          silent: true,
          symbol: 'none',
          data: [line('UCL', ucl, '#f5222d'), line('CL', center, '#52c41a', 'solid'), line('LCL', lcl, '#f5222d')]
        }
      }
    ]
  }
}
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
.item-select {
  width: min(420px, 100%);
}
.stat {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-fill, rgba(0, 0, 0, 0.02));
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.stat-value.small {
  font-size: 16px;
  line-height: 33px;
}
.stat-value.good {
  color: var(--status-success, #52c41a);
}
.stat-value.warn {
  color: var(--status-warning);
}
.stat-value.danger {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.signal {
  margin-top: 12px;
}
</style>
