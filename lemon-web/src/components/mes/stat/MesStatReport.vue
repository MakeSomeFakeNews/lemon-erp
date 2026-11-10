<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
        <IdentityUserSelect v-if="has('operator')" v-model:value="query.operatorId" :placeholder="$t('mesStat.common.allOperators')" class="filter-s" />
        <WorkCenterSelect v-if="has('workCenter')" v-model:value="query.workCenterId" :placeholder="$t('mesStat.common.allWorkCenters')" class="filter-s" />
        <a-select v-if="has('status')" v-model:value="query.status" :options="statusOptions" allow-clear
          :placeholder="$t('mesStat.common.allStatus')" class="filter-s" />
        <a-select v-if="has('category')" v-model:value="query.category" :options="toOptions(dict.wms_material_category)" allow-clear
          :placeholder="$t('mesStat.common.allCategories')" class="filter-s" />
        <a-input v-if="has('keyword')" v-model:value="query.keyword" allow-clear :placeholder="$t(`mesStat.${report}.keyword`)" class="filter" @press-enter="load" />
        <a-button type="primary" :loading="loading" @click="load">{{ $t('common.action.query') }}</a-button>
      </div>
      <div class="dims">
        <span class="sub-text">{{ $t('mesStat.common.groupBy') }}</span>
        <a-radio-group v-model:value="groupBy" button-style="solid" size="small" @change="load">
          <a-radio-button v-for="d in config.dims" :key="d" :value="d">{{ dimLabel(d) }}</a-radio-button>
        </a-radio-group>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="head">
        <div class="kpis">
          <div v-for="k in kpis" :key="k.key" class="kpi">
            <div class="sub-text">{{ $t(`mesStat.field.${k.key}`) }}</div>
            <b :class="{ danger: k.danger && Number(total[k.key]) > 0 }">{{ kpiText(k) }}</b>
          </div>
          <template v-if="config.noTotal">
            <div class="kpi">
              <div class="sub-text">{{ $t('mesStat.usage.overCount') }}</div>
              <b class="danger">{{ overCount }}</b>
            </div>
            <div class="kpi">
              <div class="sub-text">{{ $t('mesStat.usage.underCount') }}</div>
              <b>{{ underCount }}</b>
            </div>
          </template>
        </div>
        <a-space>
          <a-button v-if="config.drill" @click="openDetail()">{{ $t('mesStat.common.allDetail') }}</a-button>
          <a-button :disabled="!data.rows.length" @click="exportSummary">
            <template #icon><DownloadOutlined /></template>{{ $t('mesStat.common.export') }}
          </a-button>
        </a-space>
      </div>
      <div class="sub-text caliber">{{ $t(config.tip) }}</div>
      <a-alert v-if="data.truncated" type="warning" show-icon class="alert" :message="$t('mesStat.common.truncated', { n: data.rows.length })" />
      <a-table
        :row-key="(r) => r.groupKey ?? '__none'"
        size="middle"
        :columns="columns"
        :data-source="data.rows"
        :loading="loading"
        :pagination="data.rows.length > 50 ? { pageSize: 50, showSizeChanger: false, showTotal: (n) => $t('common.table.total', { total: n }) } : false"
        :scroll="{ x: scrollX, y: '100%' }"
        :custom-row="config.drill ? (r) => ({ onClick: () => openDetail(r), class: 'click-row' }) : undefined"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <DictTag type="mes_wo_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'dueState'">
            <a-tag v-if="record.dueState" :color="DUE_COLORS[record.dueState]" :bordered="false">{{ $t(`mesStat.due.${record.dueState}`) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'varianceQty' || column.key === 'varianceRate'">
            <span :class="{ danger: Number(record[column.key]) > 0, good: Number(record[column.key]) < 0 }">{{ cellText(column, record, dict) }}</span>
          </template>
          <template v-else-if="config.drill && column.key === columns[0].key">
            <a>{{ cellText(column, record, dict) }}</a>
          </template>
          <template v-else>{{ cellText(column, record, dict) }}</template>
        </template>
        <template v-if="data.rows.length && !config.noTotal" #summary>
          <a-table-summary fixed>
            <a-table-summary-row class="total-row">
              <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
                <template v-if="i === 0">{{ $t('mesStat.common.total') }}</template>
                <template v-else-if="c.type !== 'text' && !NO_TOTAL.includes(c.key)">{{ cellText(c, total, dict) }}</template>
              </a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>

    <MesStatReportDetail v-if="config.drill" ref="detailRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import MesStatReportDetail from './MesStatReportDetail.vue'
import { mesStat } from '@/api/mes/stat'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money, qty } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { rangePresets, thisMonth } from '@/components/bi/jxc/jxc'
import { DICT_TYPES, DUE_COLORS, REPORTS, cellText, dimLabel, exportCell, rateText } from './stat'
import { t } from '@/locales'
import { WorkOrderStatus } from '@/constants/enums'

/**
 * 生产统计报表页（生产制造 → 生产统计）：按维度汇总，人员计件、工序产量可点击汇总行查看报工明细；导出当前汇总结果。
 * - report：piece | output | wo | product | usage，配置见 stat.js 的 REPORTS
 */
const props = defineProps({
  report: { type: String, required: true }
})

/** 汇总行中不合计的列（工单状态、延期天数等单行指标） */
const NO_TOTAL = ['delayDays', 'unitCost']

const config = computed(() => REPORTS[props.report])
const dict = useDict(...DICT_TYPES)
const userStore = useUserStore()
const costVisible = userStore.hasPerm('wms:stock:cost')
const presets = rangePresets()
const range = ref(thisMonth())
const groupBy = ref(config.value.dims[0])
const query = reactive({ operatorId: undefined, workCenterId: undefined, status: undefined, category: undefined, keyword: '' })
const data = ref({ rows: [], total: {}, truncated: false })
const total = computed(() => data.value.total || {})
const loading = ref(false)

const has = (f) => config.value.filters.includes(f)
/** 工单状态：报表只统计已下达的工单，不列出草稿 */
const statusOptions = computed(() => toOptions(dict.mes_wo_status).filter((o) => o.value !== WorkOrderStatus.DRAFT))
const columns = computed(() => config.value.columns(groupBy.value, costVisible))
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + (c.width || 100), 0))
const kpis = computed(() => config.value.kpis.filter((k) => !k.cost || costVisible))
const overCount = computed(() => data.value.rows.filter((r) => Number(r.varianceQty) > 0).length)
const underCount = computed(() => data.value.rows.filter((r) => Number(r.varianceQty) < 0).length)

function kpiText(k) {
  const v = total.value[k.key]
  if (k.type === 'rate') return rateText(v)
  if (k.type === 'money') return `¥${money(v)}`
  if (k.type === 'qty') return qty(v)
  return v ?? 0
}

/** 查询条件（不含汇总维度），去掉空值 */
function params() {
  const p = { begin: range.value[0], end: range.value[1], ...query, keyword: query.keyword?.trim() }
  return Object.fromEntries(Object.entries(p).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await mesStat(props.report, { ...params(), groupBy: groupBy.value })
    if (current === seq) data.value = { ...res, total: res.total || {} }
  } finally {
    if (current === seq) loading.value = false
  }
}

const detailRef = ref()
function openDetail(row) {
  const base = params()
  if (!row) {
    detailRef.value.open({ params: base, label: t('mesStat.common.allDetail') })
    return
  }
  const first = columns.value[0]
  const label = [first, columns.value[1]].filter((c) => c && c.type === 'text' && c.value).map((c) => c.value(row, dict)).filter(Boolean).join(' / ')
  detailRef.value.open({ params: { ...base, ...config.value.drillParams(groupBy.value, row) }, label: label || cellText(first, row, dict) })
}

function exportSummary() {
  const cols = columns.value
  const rows = data.value.rows.map((r) => cols.map((c) => exportCell(c, r, dict)))
  if (!config.value.noTotal) {
    rows.push(cols.map((c, i) => (i === 0 ? t('mesStat.common.total') : c.type === 'text' || NO_TOTAL.includes(c.key) ? '' : exportCell(c, total.value, dict))))
  }
  const name = `${t(config.value.title)}_${dimLabel(groupBy.value)}_${range.value[0]}_${range.value[1]}`
  writeExcel(name, cols.map((c) => ({ title: c.title, width: Math.max(10, Math.round((c.width || 100) / 8)) })), rows, { withStamp: false })
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 220px;
}
.filter-s {
  width: 170px;
}
.dims {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.kpis {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 28px;
}
.kpi b {
  font-size: 18px;
}
.caliber {
  margin-bottom: 12px;
  line-height: 1.6;
}
.alert {
  margin-bottom: 12px;
}
.danger {
  color: var(--status-critical, #cf1322);
}
.good {
  color: var(--status-success, #389e0d);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.click-row) {
  cursor: pointer;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
