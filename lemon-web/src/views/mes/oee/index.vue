<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="oee" tab="设备 OEE">
          <div class="toolbar">
            <a-space wrap>
              <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="load" />
              <WorkCenterSelect v-model:value="workCenterId" placeholder="全部工作中心" class="filter" />
            </a-space>
            <span class="sub-text">OEE = 可用率 × 性能率 × 良品率；计划时间按工作日历，扣除保养、无生产计划等计划停机</span>
          </div>
          <a-row :gutter="16" class="stats">
            <a-col :xs="12" :md="6"><a-statistic title="综合 OEE" :value="pct(overall.oee)" /></a-col>
            <a-col :xs="12" :md="6"><a-statistic title="可用率" :value="pct(overall.availability)" /></a-col>
            <a-col :xs="12" :md="6"><a-statistic title="性能率" :value="pct(overall.performance)" /></a-col>
            <a-col :xs="12" :md="6"><a-statistic title="良品率" :value="pct(overall.quality)" /></a-col>
          </a-row>
          <a-table row-key="equipmentId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1200 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'equipment'">
                <a @click="openDetail(record)">{{ record.equipmentCode }} {{ record.equipmentName }}</a>
                <div class="sub-text">{{ record.workCenterName || '未关联工作中心' }}</div>
              </template>
              <template v-else-if="column.key === 'time'">
                {{ hours(record.runHours) }} / {{ hours(record.plannedHours) }}
                <div class="sub-text">日历 {{ hours(record.calendarHours) }}</div>
              </template>
              <template v-else-if="column.key === 'stop'">
                <span :class="{ danger: Number(record.downtimeHours) > 0 }">{{ hours(record.downtimeHours) }}</span>
                <div class="sub-text">计划停机 {{ hours(record.plannedStopHours) }}</div>
              </template>
              <template v-else-if="column.key === 'output'">
                {{ formatQty(record.goodQty) }}
                <div v-if="Number(record.scrapQty)" class="sub-text">报废 {{ formatQty(record.scrapQty) }}</div>
              </template>
              <template v-else-if="column.rate">
                <a-progress v-if="record[column.key] != null" :percent="Math.round(record[column.key] * 1000) / 10" size="small"
                  :status="column.key === 'oee' ? undefined : 'normal'" :stroke-color="rateColor(record[column.key])" />
                <span v-else class="sub-text">-</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="stops" tab="停机记录">
          <div class="toolbar">
            <a-space wrap>
              <a-select v-model:value="stopQuery.equipmentId" allow-clear show-search option-filter-prop="label" placeholder="全部设备"
                :options="equipmentChoices" class="filter-wide" @change="stops.search" />
              <a-select v-model:value="stopQuery.reason" allow-clear placeholder="停机原因" :options="toOptions(dict.mes_stop_reason)" class="filter"
                @change="stops.search" />
              <a-range-picker v-model:value="stopRange" value-format="YYYY-MM-DD" @change="stops.search" />
            </a-space>
            <a-button v-perm="'mes:equipment:stop'" type="primary" @click="openStop()">
              <template #icon><PlusOutlined /></template>登记停机
            </a-button>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="stopColumns"
            :data-source="stops.list.value"
            :loading="stops.loading.value"
            :pagination="stops.pagination"
            :scroll="{ x: 1000 }"
            @change="stops.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'equipment'">{{ record.equipmentCode }} {{ record.equipmentName }}</template>
              <template v-else-if="column.key === 'time'">
                {{ record.startTime }}
                <div class="sub-text">至 {{ record.endTime }}</div>
              </template>
              <template v-else-if="column.key === 'hours'">{{ hours(record.hours) }}</template>
              <template v-else-if="column.key === 'reason'">
                <DictTag type="mes_stop_reason" :value="record.reason" />
                <div v-if="record.maintenanceNo" class="sub-text">维护单 {{ record.maintenanceNo }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-if="!record.maintenanceId" v-perm="'mes:equipment:stop'" class="table-actions">
                  <a-button type="link" size="small" @click="openStop(record)">编辑</a-button>
                  <a-popconfirm title="删除该停机记录？" @confirm="removeStop(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </span>
                <span v-else class="sub-text">自动记录</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <SubPage :open="detailOpen" :title="detail ? `设备 OEE - ${detail.total.equipmentCode} ${detail.total.equipmentName}` : ''"
      width="min(1100px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-row :gutter="16" class="stats">
          <a-col :xs="12" :md="6"><a-statistic title="OEE" :value="pct(detail.total.oee)" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="可用率" :value="pct(detail.total.availability)" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="性能率" :value="pct(detail.total.performance)" /></a-col>
          <a-col :xs="12" :md="6"><a-statistic title="良品率" :value="pct(detail.total.quality)" /></a-col>
        </a-row>
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="期间">{{ range[0] }} ~ {{ range[1] }}</a-descriptions-item>
          <a-descriptions-item label="计划时间">日历 {{ hours(detail.total.calendarHours) }} - 计划停机 {{ hours(detail.total.plannedStopHours) }}
            = {{ hours(detail.total.plannedHours) }}</a-descriptions-item>
          <a-descriptions-item label="运行时间">{{ hours(detail.total.runHours) }}（非计划停机 {{ hours(detail.total.downtimeHours) }}）</a-descriptions-item>
          <a-descriptions-item label="产量">合格 {{ formatQty(detail.total.goodQty) }}，报废 {{ formatQty(detail.total.scrapQty) }}</a-descriptions-item>
          <a-descriptions-item label="理论工时">{{ hours(detail.total.idealHours) }}</a-descriptions-item>
          <a-descriptions-item label="停机原因">
            <span v-if="!downtimeList.length" class="sub-text">无</span>
            <a-space v-else wrap :size="4">
              <span v-for="d in downtimeList" :key="d.reason"><DictTag type="mes_stop_reason" :value="d.reason" /> {{ hours(d.hours) }}</span>
            </a-space>
          </a-descriptions-item>
        </a-descriptions>
        <div class="section-title">每日趋势</div>
        <EChart :option="trendOption" height="300px" />
      </template>
    </SubPage>

    <a-modal v-model:open="stopOpen" :title="stopForm.id ? '编辑停机记录' : '登记停机'" :confirm-loading="saving" @ok="submitStop">
      <a-form layout="vertical">
        <a-form-item label="设备" required>
          <a-select v-model:value="stopForm.equipmentId" show-search option-filter-prop="label" :options="equipmentChoices" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始时间" required>
              <a-date-picker v-model:value="stopForm.startTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束时间" required>
              <a-date-picker v-model:value="stopForm.endTime" show-time value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="停机原因" required extra="计划保养、无生产计划为计划停机，从计划时间中扣除；其余计入可用率损失">
          <a-select v-model:value="stopForm.reason" :options="toOptions(dict.mes_stop_reason)" />
        </a-form-item>
        <a-form-item label="说明">
          <a-input v-model:value="stopForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import EChart from '@/components/charts/EChart.vue'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import { addStop, deleteStop, equipmentOptions, oeeDetail, oeeSummary, pageStop, updateStop } from '@/api/mes/base'
import { formatQty, today } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 设备 OEE：按工作日历、停机记录和报工（设备、产量、标准工时）计算可用率、性能率、良品率 */
const dict = useDict('mes_stop_reason')
const tab = ref('oee')

const pct = (v) => (v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`)
const hours = (v) => `${Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })} h`
const rateColor = (v) => (v >= 0.85 ? '#52c41a' : v >= 0.6 ? '#1677ff' : '#faad14')
const ratio = (a, b) => (b > 0 ? a / b : null)

const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), today()])
const workCenterId = ref()
const rows = ref([])
const loading = ref(false)
const columns = [
  { title: '设备', key: 'equipment', width: 220, fixed: 'left' },
  { title: '运行 / 计划时间', key: 'time', width: 150 },
  { title: '非计划停机', key: 'stop', width: 130 },
  { title: '合格产量', key: 'output', width: 110, align: 'right' },
  { title: '可用率', key: 'availability', rate: true, width: 140 },
  { title: '性能率', key: 'performance', rate: true, width: 140 },
  { title: '良品率', key: 'quality', rate: true, width: 140 },
  { title: 'OEE', key: 'oee', rate: true, width: 150 }
]
/** 全部设备合计：按时间和数量汇总后再计算比率 */
const overall = computed(() => {
  const s = (k) => rows.value.reduce((t, r) => t + Number(r[k] || 0), 0)
  const a = ratio(s('runHours'), s('plannedHours'))
  const p = ratio(s('idealHours'), s('runHours'))
  const q = ratio(s('goodQty'), s('goodQty') + s('scrapQty'))
  return { availability: a, performance: p, quality: q, oee: a != null && p != null && q != null ? a * p * q : null }
})
async function load() {
  loading.value = true
  try {
    rows.value = await oeeSummary({ beginDate: range.value[0], endDate: range.value[1], workCenterId: workCenterId.value ?? undefined })
  } finally {
    loading.value = false
  }
}
watch(workCenterId, load)

const detailOpen = ref(false)
const detail = ref()
const downtimeList = computed(() => Object.entries(detail.value?.downtime || {}).map(([reason, h]) => ({ reason, hours: h })))
async function openDetail(record) {
  detail.value = await oeeDetail(record.equipmentId, { beginDate: range.value[0], endDate: range.value[1] })
  detailOpen.value = true
}
const trendOption = computed(() => {
  const days = detail.value?.days || []
  const series = [['OEE', 'oee'], ['可用率', 'availability'], ['性能率', 'performance'], ['良品率', 'quality']]
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : `${v}%`) },
    legend: { top: 0 },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: days.map((d) => d.date.slice(5)) },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: series.map(([name, key]) => ({
      name, type: key === 'oee' ? 'bar' : 'line', smooth: true, connectNulls: false,
      data: days.map((d) => (d[key] == null ? null : Math.round(d[key] * 1000) / 10))
    }))
  }
})

// ---------------------------------------------------------------- 停机记录
const equipments = ref([])
const equipmentChoices = computed(() => equipments.value.map((e) => ({ value: e.id, label: `${e.code} ${e.name}` })))
const stopRange = ref()
const stopQuery = reactive({ equipmentId: undefined, reason: undefined })
const stops = useTable(pageStop, stopQuery, {
  params: (q) => ({ ...q, beginDate: stopRange.value?.[0], endDate: stopRange.value?.[1] })
})
const stopColumns = [
  { title: '设备', key: 'equipment', width: 200 },
  { title: '时间', key: 'time', width: 180 },
  { title: '时长', key: 'hours', width: 90, align: 'right' },
  { title: '原因', key: 'reason', width: 160 },
  { title: '说明', dataIndex: 'remark', ellipsis: true },
  { title: '登记人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]
watch(tab, (t) => t === 'stops' && stops.load())

const stopOpen = ref(false)
const saving = ref(false)
const stopForm = reactive({})
function openStop(record) {
  const now = dayjs().second(0)
  Object.assign(stopForm, record
    ? { id: record.id, equipmentId: record.equipmentId, startTime: record.startTime.replace(' ', 'T'), endTime: record.endTime.replace(' ', 'T'),
        reason: record.reason, remark: record.remark || '' }
    : { id: undefined, equipmentId: stopQuery.equipmentId, startTime: now.subtract(1, 'hour').format('YYYY-MM-DDTHH:mm:ss'),
        endTime: now.format('YYYY-MM-DDTHH:mm:ss'), reason: 'BREAKDOWN', remark: '' })
  stopOpen.value = true
}
async function submitStop() {
  if (!stopForm.equipmentId || !stopForm.startTime || !stopForm.endTime || !stopForm.reason) return message.warning('请填写设备、时间和原因')
  saving.value = true
  try {
    const { id, ...data } = stopForm
    const payload = { ...data, remark: data.remark || null }
    if (id) await updateStop(id, payload)
    else await addStop(payload)
    message.success('已保存')
    stopOpen.value = false
    stops.load()
    load()
  } finally {
    saving.value = false
  }
}
async function removeStop(id) {
  await deleteStop(id)
  message.success('已删除')
  stops.load()
  load()
}

onMounted(async () => {
  load()
  equipments.value = await equipmentOptions()
})
</script>

<style scoped>
.filter {
  width: 160px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.stats {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
