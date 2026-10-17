<template>
  <div class="capacity-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <!-- 负荷分析 -->
        <a-tab-pane key="load" :tab="$t('mes.capacity.loadTab')">
          <div class="toolbar">
            <a-space wrap>
              <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadData" />
              <WorkCenterSelect v-model:value="workCenterId" :placeholder="$t('mes.capacity.allWc')" class="wc-select" />
              <a-button @click="loadData"><template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}</a-button>
            </a-space>
            <a-button v-perm="'mes:capacity:edit'" type="primary" :loading="scheduling" @click="scheduleAllWo">
              <template #icon><ScheduleOutlined /></template>{{ $t('mes.capacity.scheduleAll') }}
            </a-button>
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('mes.capacity.tip')"
          />
          <div class="legend">
            <span><i class="cell-dot lv-idle" />{{ $t('mes.capacity.idle') }}</span>
            <span><i class="cell-dot lv-ok" />≤ 85%</span>
            <span><i class="cell-dot lv-high" />85% ~ 100%</span>
            <span><i class="cell-dot lv-over" />{{ $t('mes.capacity.over') }}</span>
            <span><i class="cell-dot lv-off" />{{ $t('mes.capacity.off') }}</span>
          </div>
          <a-table
            row-key="workCenterId"
            size="small"
            bordered
            :columns="loadColumns"
            :data-source="rows"
            :loading="loading"
            :pagination="false"
            :row-expandable="(r) => !!r.ops?.length"
            :scroll="{ x: 380 + days.length * 64 }"
          >
            <template #headerCell="{ column }">
              <template v-if="column.date">
                <div class="day-head" :class="{ weekend: column.weekend }">
                  <div>{{ column.date.slice(5) }}</div>
                  <div class="sub-text">{{ column.week }}</div>
                </div>
              </template>
            </template>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'wc'">
                <div>{{ record.name }}</div>
                <div class="sub-text">{{ $t('mes.capacity.wcOps', { code: record.code, n: record.ops?.length || 0 }) }}</div>
              </template>
              <template v-else-if="column.key === 'util'">
                <div :class="['util', levelOf(record.load, record.capacity)]">{{ utilText(record.load, record.capacity) }}</div>
                <div class="sub-text">{{ $t('mes.capacity.loadOfCap', { load: formatQty(round(record.load)), cap: formatQty(round(record.capacity)) }) }}</div>
              </template>
              <template v-else-if="column.date">
                <a-tooltip v-if="dayOf(record, column.date)" :title="$t('mes.capacity.dayTip', { date: column.date, load: formatQty(round(dayOf(record, column.date).load)), cap: formatQty(round(dayOf(record, column.date).capacity)) })">
                  <div :class="['day-cell', levelOf(dayOf(record, column.date).load, dayOf(record, column.date).capacity)]">
                    <template v-if="Number(dayOf(record, column.date).capacity) || Number(dayOf(record, column.date).load)">
                      <div class="day-load">{{ formatQty(round(dayOf(record, column.date).load)) }}</div>
                      <div class="day-cap">/ {{ formatQty(round(dayOf(record, column.date).capacity)) }}</div>
                    </template>
                    <span v-else class="day-cap">{{ $t('mes.capacity.offShort') }}</span>
                  </div>
                </a-tooltip>
              </template>
            </template>
            <template #expandedRowRender="{ record }">
              <a-table row-key="opId" size="small" :columns="opColumns" :data-source="record.ops" :pagination="false">
                <template #bodyCell="{ column, record: op }">
                  <template v-if="column.key === 'woNo'">
                    <a @click="toWo(op.woId)">{{ op.woNo }}</a>
                  </template>
                  <template v-else-if="column.key === 'woStatus'"><DictTag type="mes_wo_status" :value="op.woStatus" /></template>
                  <template v-else-if="column.key === 'op'">{{ op.seq }} {{ op.opName }}</template>
                  <template v-else-if="column.key === 'plan'">
                    <template v-if="op.planStart">{{ op.planStart }} ~ {{ op.planEnd || '-' }}</template>
                    <span v-else class="sub-text">{{ $t('mes.wo.unscheduled') }}</span>
                  </template>
                  <template v-else-if="column.key === 'hours'">{{ formatQty(round(op.hours)) }}</template>
                </template>
              </a-table>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 工厂日历 -->
        <a-tab-pane key="calendar" :tab="$t('mes.capacity.calendarTab')">
          <div class="toolbar">
            <a-space wrap>
              <a-select v-model:value="calWc" :options="calWcOptions" show-search option-filter-prop="label" class="wc-select" @change="loadCalendar" />
              <span class="sub-text">{{ $t('mes.capacity.calendarTip') }}</span>
            </a-space>
          </div>
          <a-row :gutter="16">
            <a-col :xs="24" :lg="16">
              <a-calendar v-model:value="calMonth" @panel-change="onPanelChange" @select="onSelectDate">
                <template #dateCellRender="{ current }">
                  <div v-if="entryOf(current)" :class="['cal-entry', { inherited: entryOf(current).workCenterId !== calWc, off: !Number(entryOf(current).hours) }]">
                    <div>{{ Number(entryOf(current).hours) ? $t('mes.bom.hoursN', { v: formatQty(entryOf(current).hours) }) : $t('mes.capacity.off') }}</div>
                    <div v-if="entryOf(current).workCenterId !== calWc" class="sub-text">{{ $t('mes.capacity.factory') }}</div>
                    <div v-if="entryOf(current).remark" class="cal-remark">{{ entryOf(current).remark }}</div>
                  </div>
                </template>
              </a-calendar>
            </a-col>
            <a-col :xs="24" :lg="8">
              <div class="section-title">{{ $t('mes.capacity.monthSettings', { wc: calWcLabel }) }}</div>
              <a-table row-key="id" size="small" :columns="calColumns" :data-source="ownEntries" :loading="calLoading" :pagination="false">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'hours'">
                    <span v-if="Number(record.hours)">{{ $t('mes.bom.hoursN', { v: formatQty(record.hours) }) }}</span>
                    <a-tag v-else :bordered="false">{{ $t('mes.capacity.off') }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <span v-perm="'mes:capacity:edit'">
                      <a-popconfirm :title="$t('mes.capacity.deleteConfirm')" @confirm="removeEntry(record.id)">
                        <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                      </a-popconfirm>
                    </span>
                  </template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="entryOpen" :title="`${entryForm.calDate} · ${calWcLabel}`" :confirm-loading="saving" @ok="submitEntry">
      <a-form layout="vertical">
        <a-form-item :label="$t('mes.capacity.hoursPerResource')" required :extra="$t('mes.capacity.hoursExtra')">
          <a-input-number v-model:value="entryForm.hours" :min="0" :max="24" :precision="2" :addon-after="$t('mes.wo.hoursUnit')" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="entryForm.remark" :maxlength="100" :placeholder="$t('mes.capacity.remarkPh')" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button v-if="entryForm.id" danger :loading="saving" class="footer-left" @click="removeEntry(entryForm.id, true)">{{ $t('mes.capacity.deleteSetting') }}</a-button>
        <a-button @click="entryOpen = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="submitEntry">{{ $t('common.action.save') }}</a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, ReloadOutlined, ScheduleOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import { workCenterOptions } from '@/api/mes/base'
import { capacityLoad, deleteCalendar, listCalendar, saveCalendar, scheduleAll } from '@/api/mes/capacity'
import { formatQty } from '@/components/wms/wms'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const tab = ref('load')

const WEEK = t('mes.capacity.week').split(',')
const MAX_DAYS = 92
const round = (n) => Math.round(Number(n || 0) * 100) / 100

/** 利用率等级：休息（无产能无负荷）、空闲、正常、偏高（> 85%）、超负荷（> 100%） */
function levelOf(load, capacity) {
  const l = Number(load || 0)
  const c = Number(capacity || 0)
  if (!c) return l > 0 ? 'lv-over' : 'lv-off'
  if (!l) return 'lv-idle'
  const r = l / c
  return r > 1 ? 'lv-over' : r > 0.85 ? 'lv-high' : 'lv-ok'
}
function utilText(load, capacity) {
  const c = Number(capacity || 0)
  if (!c) return Number(load) > 0 ? t('mes.capacity.noCapacity') : '-'
  return `${Math.round((Number(load || 0) / c) * 100)}%`
}

// ---------------------------------------------------------------- 负荷分析
const range = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(13, 'day').format('YYYY-MM-DD')])
const workCenterId = ref()
const rows = ref([])
const loading = ref(false)

const days = computed(() => {
  const [from, to] = range.value || []
  if (!from || !to) return []
  const list = []
  for (let d = dayjs(from); !d.isAfter(dayjs(to)) && list.length < MAX_DAYS + 1; d = d.add(1, 'day')) list.push(d)
  return list
})
const loadColumns = computed(() => [
  { title: t('mes.wo.workCenter'), key: 'wc', width: 200, fixed: 'left' },
  { title: t('mes.capacity.util'), key: 'util', width: 150, fixed: 'left' },
  ...days.value.map((d) => ({
    key: d.format('YYYY-MM-DD'),
    date: d.format('YYYY-MM-DD'),
    week: WEEK[d.day()],
    weekend: d.day() === 0 || d.day() === 6,
    width: 64,
    align: 'center'
  }))
])
const opColumns = [
  { title: t('mes.wo.wo'), key: 'woNo', width: 180 },
  { title: t('mes.capacity.woStatus'), key: 'woStatus', width: 120 },
  { title: t('mes.wo.op'), key: 'op', width: 160 },
  { title: t('mes.wo.planDate'), key: 'plan', width: 200 },
  { title: t('mes.capacity.remainingHours'), key: 'hours', width: 130, align: 'right' }
]

/** 每行按日期建索引，单元格直接取 */
const dayIndex = computed(() => new Map(rows.value.map((r) => [r.workCenterId, new Map((r.days || []).map((d) => [d.date, d]))])))
const dayOf = (record, date) => dayIndex.value.get(record.workCenterId)?.get(date)

async function loadData() {
  const [from, to] = range.value || []
  if (!from || !to) return
  if (dayjs(to).diff(dayjs(from), 'day') + 1 > MAX_DAYS) {
    message.warning(t('mes.capacity.maxDays', { n: MAX_DAYS }))
    return
  }
  loading.value = true
  try {
    rows.value = await capacityLoad({ from, to, workCenterId: workCenterId.value || undefined })
  } finally {
    loading.value = false
  }
}

const scheduling = ref(false)
function scheduleAllWo() {
  Modal.confirm({
    title: t('mes.capacity.scheduleAllConfirm'),
    content: t('mes.capacity.scheduleAllContent'),
    async onOk() {
      scheduling.value = true
      try {
        const count = await scheduleAll()
        message.success(t('mes.capacity.scheduledN', { n: count }))
        loadData()
      } finally {
        scheduling.value = false
      }
    }
  })
}

watch(workCenterId, loadData)

const toWo = (id) => router.push({ path: '/mes/wo', query: { id } })

// ---------------------------------------------------------------- 工厂日历
const centers = ref([])
const calWc = ref(0)
const calWcOptions = computed(() => [{ value: 0, label: t('mes.capacity.factory') }, ...centers.value.map((w) => ({ value: w.id, label: `${w.code} ${w.name}` }))])
const calWcLabel = computed(() => calWcOptions.value.find((o) => o.value === calWc.value)?.label || t('mes.capacity.factory'))
const calMonth = ref(dayjs())
const entries = ref([])
const calLoading = ref(false)

async function loadCalendar() {
  // 日历面板会显示上月末、下月初的日期，多取一周
  const from = calMonth.value.startOf('month').subtract(7, 'day').format('YYYY-MM-DD')
  const to = calMonth.value.endOf('month').add(14, 'day').format('YYYY-MM-DD')
  calLoading.value = true
  try {
    // 选工作中心时同时取全厂设置，用于显示继承的全厂工时
    entries.value = await listCalendar({ from, to })
  } finally {
    calLoading.value = false
  }
}

/** 某天生效的设置：本工作中心的优先，其次全厂 */
const entryMap = computed(() => {
  const map = new Map()
  entries.value.filter((e) => Number(e.workCenterId) === 0).forEach((e) => map.set(e.calDate, e))
  if (calWc.value) entries.value.filter((e) => e.workCenterId === calWc.value).forEach((e) => map.set(e.calDate, e))
  return map
})
const entryOf = (d) => entryMap.value.get(d.format('YYYY-MM-DD'))
/** 当前范围（全厂或所选工作中心）本月的设置 */
const ownEntries = computed(() => {
  const month = calMonth.value.format('YYYY-MM')
  return entries.value.filter((e) => Number(e.workCenterId) === Number(calWc.value) && e.calDate.startsWith(month))
})
const calColumns = [
  { title: t('common.field.date'), dataIndex: 'calDate', width: 110 },
  { title: t('mes.wo.hours'), key: 'hours', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: '', key: 'action', width: 50 }
]

function onPanelChange(value) {
  calMonth.value = value
}
// 切换月份（翻页或点击相邻月份的日期）时重新加载
watch(() => calMonth.value.format('YYYY-MM'), loadCalendar)

const entryOpen = ref(false)
const saving = ref(false)
const entryForm = reactive({ id: undefined, calDate: undefined, hours: 8, remark: '' })
function onSelectDate(date, info) {
  // 切换年 / 月时也会触发 select，只处理点击日期
  if (info?.source && info.source !== 'date') return
  if (!userStore.hasPerm('mes:capacity:edit')) return
  const calDate = date.format('YYYY-MM-DD')
  const own = entries.value.find((e) => Number(e.workCenterId) === Number(calWc.value) && e.calDate === calDate)
  Object.assign(entryForm, {
    id: own?.id,
    calDate,
    hours: own ? Number(own.hours) : date.day() === 0 || date.day() === 6 ? 0 : 8,
    remark: own?.remark || ''
  })
  entryOpen.value = true
}

async function submitEntry() {
  if (entryForm.hours == null) {
    message.warning(t('mes.capacity.hoursRequired'))
    return
  }
  saving.value = true
  try {
    await saveCalendar({ workCenterId: calWc.value || 0, calDate: entryForm.calDate, hours: entryForm.hours, remark: entryForm.remark || null })
    message.success(t('common.msg.saved'))
    entryOpen.value = false
    loadCalendar()
  } finally {
    saving.value = false
  }
}

async function removeEntry(id, fromModal) {
  saving.value = true
  try {
    await deleteCalendar(id)
    message.success(t('common.msg.deleted'))
    if (fromModal) entryOpen.value = false
    loadCalendar()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  loadData()
  loadCalendar()
  centers.value = await workCenterOptions().catch(() => [])
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.wc-select {
  width: 220px;
}
.tip {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.section-title {
  font-weight: 500;
  margin: 12px 0 8px;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--app-text-3);
}
.legend span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.cell-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
}
.day-head {
  line-height: 1.3;
  text-align: center;
}
.day-head.weekend {
  color: #fa8c16;
}
.day-cell {
  border-radius: 4px;
  padding: 2px 0;
  line-height: 1.3;
  cursor: default;
}
.day-load {
  font-weight: 500;
}
.day-cap {
  font-size: 11px;
  opacity: 0.75;
}
.util {
  font-weight: 600;
}
.util.lv-ok {
  color: #52c41a;
}
.util.lv-high {
  color: #fa8c16;
}
.util.lv-over {
  color: #ff4d4f;
}
.util.lv-idle,
.util.lv-off {
  color: var(--app-text-3);
}
.day-cell.lv-idle,
.cell-dot.lv-idle {
  background: rgba(82, 196, 26, 0.06);
  border: 1px dashed rgba(82, 196, 26, 0.35);
}
.day-cell.lv-ok,
.cell-dot.lv-ok {
  background: rgba(82, 196, 26, 0.2);
}
.day-cell.lv-high,
.cell-dot.lv-high {
  background: rgba(250, 140, 22, 0.28);
}
.day-cell.lv-over,
.cell-dot.lv-over {
  background: rgba(255, 77, 79, 0.32);
}
.day-cell.lv-off,
.cell-dot.lv-off {
  background: rgba(128, 128, 128, 0.12);
  color: var(--app-text-3);
}
.cal-entry {
  font-size: 12px;
  padding: 2px 4px;
  border-radius: 4px;
  background: rgba(22, 119, 255, 0.12);
  line-height: 1.4;
}
.cal-entry.off {
  background: rgba(128, 128, 128, 0.15);
}
.cal-entry.inherited {
  opacity: 0.6;
}
.cal-remark {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.footer-left {
  float: left;
}
</style>
