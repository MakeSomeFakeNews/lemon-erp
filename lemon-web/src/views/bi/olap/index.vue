<template>
  <div class="olap-page">
    <a-card :bordered="false" class="page-card">
      <div class="config">
        <div class="field">
          <span class="field-label">数据集</span>
          <a-select v-model:value="cubeKey" :options="cubeOptions" style="width: 130px" @change="onCubeChange" />
        </div>
        <div class="field">
          <span class="field-label">行维度</span>
          <a-select v-model:value="row" :options="dimOptions" style="width: 130px" />
        </div>
        <div class="field">
          <span class="field-label">列维度</span>
          <a-select v-model:value="col" :options="colOptions" allow-clear placeholder="不分列" style="width: 130px" />
        </div>
        <div class="field">
          <span class="field-label">指标</span>
          <a-select
            v-model:value="measures"
            mode="multiple"
            :options="measureOptions"
            :max-tag-count="2"
            placeholder="全部指标"
            style="min-width: 200px; max-width: 360px"
          />
        </div>
        <div class="field">
          <span class="field-label">{{ cube?.dateFilter === 'SNAPSHOT' ? '快照日期' : '日期范围' }}</span>
          <a-select v-model:value="range" :options="rangeOptions" style="width: 120px" />
          <a-range-picker v-if="range === 'CUSTOM'" v-model:value="dates" value-format="YYYY-MM-DD" style="width: 240px" />
        </div>
        <div class="field">
          <a-button type="primary" :loading="loading" @click="query">
            <template #icon><SearchOutlined /></template>查询
          </a-button>
          <a-button v-perm="'bi:report:add'" :disabled="!result" @click="openSave(false)">
            <template #icon><SaveOutlined /></template>保存为报表
          </a-button>
          <a-button v-if="report?.mine" v-perm="'bi:report:edit'" :disabled="!result" @click="openSave(true)">更新报表</a-button>
        </div>
      </div>
      <div v-if="report || filterTags.length || drillStack.length" class="filters">
        <a-tag v-if="report" color="blue" :bordered="false">报表：{{ report.name }}</a-tag>
        <span v-if="filterTags.length" class="sub-text">筛选：</span>
        <a-tag v-for="t in filterTags" :key="t.key" closable @close.prevent="removeFilter(t)">{{ t.text }}</a-tag>
        <a-button v-if="drillStack.length" size="small" @click="drillBack">
          <template #icon><RollbackOutlined /></template>返回上一级
        </a-button>
        <a-button v-if="filterTags.length" size="small" type="link" @click="clearFilters">清除筛选</a-button>
      </div>
    </a-card>

    <a-alert
      v-if="cube && cube.dateFilter === 'SNAPSHOT' && range === 'ALL'"
      class="tip"
      type="info"
      show-icon
      message="库存分析不选日期且不按快照日期分组时，只统计最新一次快照"
    />

    <a-card :bordered="false" class="page-card" :title="chartTitle">
      <template #extra>
        <a-segmented v-model:value="chartType" :options="CHART_TYPES.filter((c) => c.value !== 'table')" size="small" />
      </template>
      <a-spin :spinning="loading">
        <a-empty v-if="!result || !result.rowTotals.length" description="暂无数据：调整条件后查询，或先在「数据仓库」中同步数据" />
        <EChart v-else-if="chartOption" :option="chartOption" height="340px" @click="onChartClick" />
      </a-spin>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <template #title>
        透视表
        <span v-if="result" class="sub-text title-sub">
          {{ result.begin || '不限' }} ~ {{ result.end || '不限' }} · {{ result.rowTotals.length }} 行
        </span>
      </template>
      <template #extra>
        <span class="sub-text">点击行维度的值钻取到</span>
        <a-select v-model:value="drillTarget" :options="drillOptions" size="small" style="width: 110px; margin-left: 8px" />
      </template>
      <PivotTable v-if="cube && result" :cube="cube" :config="lastConfig" :result="result" drillable @drill="drill" />
      <a-empty v-else />
    </a-card>

    <a-modal v-model:open="saveOpen" :title="saveUpdate ? '更新报表' : '保存为报表'" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="报表名称" required>
          <a-input v-model:value="saveForm.name" :maxlength="64" placeholder="如：近 12 个月销售趋势" />
        </a-form-item>
        <a-form-item label="默认图表">
          <a-radio-group v-model:value="saveForm.chart" :options="CHART_TYPES" />
        </a-form-item>
        <a-form-item label="共享给所有人" extra="共享后其他用户可以查看、在仪表盘中使用，只有你可以修改">
          <a-switch v-model:checked="saveForm.shared" />
        </a-form-item>
        <a-form-item label="说明">
          <a-textarea v-model:value="saveForm.remark" :rows="2" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { RollbackOutlined, SaveOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import PivotTable from '@/components/bi/PivotTable.vue'
import { addReport, getReport, olapPivot, updateReport } from '@/api/bi'
import { CHART_TYPES, RANGES, defaultDrill, dimLabelFn, loadCubes, chartMeasure, pivotChartOption } from '@/components/bi/bi'

const route = useRoute()
const cubes = ref([])
const cubeKey = ref('SALES')
const row = ref()
const col = ref()
const measures = ref([])
const range = ref('LAST_12M')
const dates = ref()
const filters = ref({})
const filterNames = ref({})
const drillStack = ref([])
const drillTarget = ref()
const chartType = ref('bar')
const result = ref(null)
const lastConfig = ref({})
const loading = ref(false)
const report = ref(null)

const cube = computed(() => cubes.value.find((c) => c.key === cubeKey.value))
const cubeOptions = computed(() => cubes.value.map((c) => ({ value: c.key, label: c.label })))
const dimOptions = computed(() => (cube.value?.dims || []).map((d) => ({ value: d.key, label: d.label })))
const colOptions = computed(() => dimOptions.value.filter((d) => d.value !== row.value))
const drillOptions = computed(() => dimOptions.value.filter((d) => d.value !== lastConfig.value.row))
const measureOptions = computed(() => (cube.value?.measures || []).map((m) => ({ value: m.key, label: m.label })))
const rangeOptions = computed(() => RANGES)
const dimName = (key) => cube.value?.dims.find((d) => d.key === key)?.label || key

const chartTitle = computed(() => {
  const m = cube.value ? chartMeasure(cube.value, lastConfig.value) : null
  return result.value && m ? `${m.label} · 按${dimName(lastConfig.value.row)}` : '图表'
})
const chartOption = computed(() =>
  cube.value && result.value ? pivotChartOption(cube.value, lastConfig.value, result.value, chartType.value) : null)

const filterTags = computed(() =>
  Object.entries(filters.value).flatMap(([dim, values]) =>
    values.map((v) => ({ key: `${dim}|${v}`, dim, value: v, text: `${dimName(dim)}：${filterNames.value[`${dim}|${v}`] || v}` }))))

function currentRequest() {
  const custom = range.value === 'CUSTOM'
  return {
    cube: cubeKey.value,
    row: row.value,
    col: col.value || null,
    measures: measures.value,
    range: custom ? null : range.value,
    begin: custom ? dates.value?.[0] || null : null,
    end: custom ? dates.value?.[1] || null : null,
    filters: filters.value,
    chart: chartType.value
  }
}

async function query() {
  if (!row.value) {
    message.warning('请选择行维度')
    return
  }
  const req = currentRequest()
  loading.value = true
  try {
    result.value = await olapPivot(req)
    lastConfig.value = req
    if (!drillTarget.value || drillTarget.value === req.row) drillTarget.value = defaultDrill(cube.value, req.row)
  } finally {
    loading.value = false
  }
}

function onCubeChange() {
  const dims = cube.value?.dims.map((d) => d.key) || []
  row.value = dims.includes('period') ? 'period' : dims[0]
  col.value = undefined
  measures.value = []
  filters.value = {}
  filterNames.value = {}
  drillStack.value = []
  drillTarget.value = undefined
  report.value = null
  result.value = null
}

/** 钻取：把点击的行维度值加入筛选，行维度切换到目标维度 */
function drill(value) {
  const from = lastConfig.value.row
  const target = drillTarget.value && drillTarget.value !== from ? drillTarget.value : defaultDrill(cube.value, from)
  if (!target) return
  drillStack.value.push({ row: row.value, col: col.value, filters: JSON.parse(JSON.stringify(filters.value)), names: { ...filterNames.value } })
  const label = dimLabelFn(cube.value, from, result.value?.labels?.rows)(value)
  filters.value = { ...filters.value, [from]: [String(value)] }
  filterNames.value = { ...filterNames.value, [`${from}|${value}`]: label }
  row.value = target
  if (col.value === target) col.value = undefined
  drillTarget.value = defaultDrill(cube.value, target)
  query()
}

function onChartClick(p) {
  if (p?.dataIndex === undefined || !result.value) return
  const r = result.value.rowTotals[p.dataIndex]
  if (r && r.r !== null && r.r !== '-') drill(r.r)
}

function drillBack() {
  const prev = drillStack.value.pop()
  if (!prev) return
  row.value = prev.row
  col.value = prev.col
  filters.value = prev.filters
  filterNames.value = prev.names
  drillTarget.value = defaultDrill(cube.value, prev.row)
  query()
}

function removeFilter(t) {
  const values = (filters.value[t.dim] || []).filter((v) => v !== t.value)
  const next = { ...filters.value }
  if (values.length) next[t.dim] = values
  else delete next[t.dim]
  filters.value = next
  query()
}

function clearFilters() {
  filters.value = {}
  filterNames.value = {}
  drillStack.value = []
  query()
}

// ---------------------------------------------------------------- 报表

const saveOpen = ref(false)
const saveUpdate = ref(false)
const saving = ref(false)
const saveForm = reactive({ name: '', chart: 'bar', shared: false, remark: '' })

function openSave(update) {
  saveUpdate.value = update
  Object.assign(saveForm, update && report.value
    ? { name: report.value.name, chart: chartType.value, shared: report.value.shared === 1, remark: report.value.remark || '' }
    : { name: '', chart: chartType.value, shared: false, remark: '' })
  saveOpen.value = true
}

async function save() {
  if (!saveForm.name.trim()) {
    message.warning('请输入报表名称')
    return
  }
  saving.value = true
  try {
    const data = {
      name: saveForm.name.trim(),
      config: { ...lastConfig.value, chart: saveForm.chart },
      shared: saveForm.shared ? 1 : 0,
      remark: saveForm.remark || null
    }
    if (saveUpdate.value) {
      await updateReport(report.value.id, data)
      report.value = { ...report.value, name: data.name, shared: data.shared, remark: data.remark }
      message.success('报表已更新')
    } else {
      const id = await addReport(data)
      report.value = { id, name: data.name, shared: data.shared, remark: data.remark, mine: true }
      message.success('已保存，可在「自定义报表」和「自定义仪表盘」中使用')
    }
    saveOpen.value = false
  } finally {
    saving.value = false
  }
}

/** 打开已保存的报表：?report=ID */
async function openReport(id) {
  const r = await getReport(id)
  const cfg = JSON.parse(r.config)
  report.value = r
  cubeKey.value = cfg.cube
  row.value = cfg.row
  col.value = cfg.col || undefined
  measures.value = cfg.measures || []
  if (cfg.range) {
    range.value = cfg.range
    dates.value = undefined
  } else {
    range.value = cfg.begin || cfg.end ? 'CUSTOM' : 'ALL'
    dates.value = cfg.begin || cfg.end ? [cfg.begin, cfg.end] : undefined
  }
  filters.value = cfg.filters || {}
  filterNames.value = {}
  chartType.value = cfg.chart && cfg.chart !== 'table' ? cfg.chart : 'bar'
  drillStack.value = []
}

onMounted(async () => {
  cubes.value = await loadCubes()
  if (route.query.report) {
    try {
      await openReport(route.query.report)
    } catch {
      onCubeChange()
    }
  } else {
    onCubeChange()
  }
  query()
})
</script>

<style scoped>
.olap-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.olap-page > .page-card + .page-card {
  margin-top: 0;
}
.config {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  align-items: center;
}
.field {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.field-label {
  color: var(--app-text-2);
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--app-border);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.title-sub {
  margin-left: 8px;
}
</style>
