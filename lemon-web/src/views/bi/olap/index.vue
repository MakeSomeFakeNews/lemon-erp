<template>
  <div class="olap-page">
    <a-card :bordered="false" class="page-card">
      <div class="config">
        <div class="field">
          <span class="field-label">{{ $t('bi.common.cube') }}</span>
          <a-select v-model:value="cubeKey" :options="cubeOptions" style="width: 130px" @change="onCubeChange" />
        </div>
        <div class="field">
          <span class="field-label">{{ $t('bi.olap.row') }}</span>
          <a-select v-model:value="row" :options="dimOptions" style="width: 130px" />
        </div>
        <div class="field">
          <span class="field-label">{{ $t('bi.olap.col') }}</span>
          <a-select v-model:value="col" :options="colOptions" allow-clear :placeholder="$t('bi.olap.noCol')" style="width: 130px" />
        </div>
        <div class="field">
          <span class="field-label">{{ $t('bi.olap.measures') }}</span>
          <a-select
            v-model:value="measures"
            mode="multiple"
            :options="measureOptions"
            :max-tag-count="2"
            :placeholder="$t('bi.olap.allMeasures')"
            style="min-width: 200px; max-width: 360px"
          />
        </div>
        <div class="field">
          <span class="field-label">{{ cube?.dateFilter === 'SNAPSHOT' ? $t('bi.olap.snapshotDate') : $t('common.field.dateRange') }}</span>
          <a-select v-model:value="range" :options="rangeOptions" style="width: 140px" />
          <a-range-picker v-if="range === 'CUSTOM'" v-model:value="dates" value-format="YYYY-MM-DD" style="width: 240px" />
        </div>
        <div class="field">
          <a-button type="primary" :loading="loading" @click="query">
            <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
          </a-button>
          <a-button v-perm="'bi:report:add'" :disabled="!result" @click="openSave(false)">
            <template #icon><SaveOutlined /></template>{{ $t('bi.olap.saveAs') }}
          </a-button>
          <a-button v-if="report?.mine" v-perm="'bi:report:edit'" :disabled="!result" @click="openSave(true)">{{ $t('bi.olap.update') }}</a-button>
        </div>
      </div>
      <div v-if="report || filterTags.length || drillStack.length" class="filters">
        <a-tag v-if="report" color="blue" :bordered="false">{{ $t('bi.olap.reportTag', { name: report.name }) }}</a-tag>
        <span v-if="filterTags.length" class="sub-text">{{ $t('bi.olap.filters') }}</span>
        <a-tag v-for="t in filterTags" :key="t.key" closable @close.prevent="removeFilter(t)">{{ t.text }}</a-tag>
        <a-button v-if="drillStack.length" size="small" @click="drillBack">
          <template #icon><RollbackOutlined /></template>{{ $t('bi.olap.drillBack') }}
        </a-button>
        <a-button v-if="filterTags.length" size="small" type="link" @click="clearFilters">{{ $t('bi.olap.clearFilters') }}</a-button>
      </div>
    </a-card>

    <a-alert
      v-if="cube && cube.dateFilter === 'SNAPSHOT' && range === 'ALL'"
      class="tip"
      type="info"
      show-icon
      :message="$t('bi.olap.snapshotTip')"
    />

    <a-card :bordered="false" class="page-card" :title="chartTitle">
      <template #extra>
        <a-segmented v-model:value="chartType" :options="CHART_TYPES.filter((c) => c.value !== 'table')" size="small" />
      </template>
      <a-spin :spinning="loading">
        <a-empty v-if="!result || !result.rowTotals.length" :description="$t('bi.olap.noData')" />
        <EChart v-else-if="chartOption" :option="chartOption" height="340px" @click="onChartClick" />
      </a-spin>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <template #title>
        {{ $t('bi.olap.pivot') }}
        <span v-if="result" class="sub-text title-sub">
          {{ result.begin || $t('bi.olap.unlimited') }} ~ {{ result.end || $t('bi.olap.unlimited') }} · {{ $t('bi.olap.rowsN', { n: result.rowTotals.length }) }}
        </span>
      </template>
      <template #extra>
        <span class="sub-text">{{ $t('bi.olap.drillTo') }}</span>
        <a-select v-model:value="drillTarget" :options="drillOptions" size="small" style="width: 120px; margin-left: 8px" />
      </template>
      <PivotTable v-if="cube && result" :cube="cube" :config="lastConfig" :result="result" drillable @drill="drill" />
      <a-empty v-else />
    </a-card>

    <a-modal v-model:open="saveOpen" :title="saveUpdate ? $t('bi.olap.update') : $t('bi.olap.saveAs')" :confirm-loading="saving" @ok="save">
      <a-form layout="vertical">
        <a-form-item :label="$t('bi.report.reportName')" required>
          <a-input v-model:value="saveForm.name" :maxlength="64" :placeholder="$t('bi.olap.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('bi.common.defaultChart')">
          <a-radio-group v-model:value="saveForm.chart" :options="CHART_TYPES" />
        </a-form-item>
        <a-form-item :label="$t('bi.common.shareAll')" :extra="$t('bi.olap.shareExtra')">
          <a-switch v-model:checked="saveForm.shared" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
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
import { t } from '@/locales'

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
  return result.value && m ? t('bi.olap.chartTitle', { measure: m.label, dim: dimName(lastConfig.value.row) }) : t('bi.olap.chart')
})
const chartOption = computed(() =>
  cube.value && result.value ? pivotChartOption(cube.value, lastConfig.value, result.value, chartType.value) : null)

const filterTags = computed(() =>
  Object.entries(filters.value).flatMap(([dim, values]) =>
    values.map((v) => ({ key: `${dim}|${v}`, dim, value: v, text: t('bi.olap.filterTag', { dim: dimName(dim), value: filterNames.value[`${dim}|${v}`] || v }) }))))

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
    message.warning(t('bi.olap.selectRow'))
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
    message.warning(t('common.rule.required', { name: t('bi.report.reportName') }))
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
      message.success(t('bi.olap.updated'))
    } else {
      const id = await addReport(data)
      report.value = { id, name: data.name, shared: data.shared, remark: data.remark, mine: true }
      message.success(t('bi.olap.saved'))
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
