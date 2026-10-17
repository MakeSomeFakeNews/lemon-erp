<template>
  <div ref="rootRef" :class="['board-page', { fullscreen }]">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-space wrap>
          <WorkCenterSelect v-model:value="workCenterId" :placeholder="$t('mes.board.allLines')" class="wc-select" @update:value="load" />
          <a-input-search v-model:value="keyword" allow-clear :placeholder="$t('mes.board.keyword')" class="keyword" />
          <a-radio-group v-model:value="filter" button-style="solid">
            <a-radio-button value="ALL">{{ $t('mes.board.filter.ALL') }} {{ orders.length }}</a-radio-button>
            <a-radio-button value="RUNNING">{{ $t('mes.board.filter.RUNNING') }} {{ kpi.runningOrders || 0 }}</a-radio-button>
            <a-radio-button value="RISK">{{ $t('mes.board.filter.RISK') }} {{ riskCount }}</a-radio-button>
            <a-radio-button value="TO_STOCK">{{ $t('mes.board.filter.TO_STOCK') }} {{ kpi.toStockOrders || 0 }}</a-radio-button>
          </a-radio-group>
        </a-space>
        <a-space wrap>
          <span class="sub-text">{{ updatedAt ? $t('mes.board.updatedAt', { t: updatedAt }) : '' }}</span>
          <a-tooltip :title="$t('mes.board.autoRefreshTip')">
            <a-switch v-model:checked="autoRefresh" size="small" />
          </a-tooltip>
          <span class="sub-text">{{ $t('mes.board.autoRefresh') }}</span>
          <a-button :loading="loading" @click="load"><template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}</a-button>
          <a-button @click="toggleFullscreen">
            <template #icon><FullscreenExitOutlined v-if="fullscreen" /><FullscreenOutlined v-else /></template>
            {{ fullscreen ? $t('mes.board.exitFullscreen') : $t('mes.board.fullscreen') }}
          </a-button>
        </a-space>
      </div>

      <div class="kpis">
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.active') }}</div>
          <div class="kpi-value">{{ kpi.activeOrders ?? '-' }}<small>{{ $t('mes.board.unitOrder') }}</small></div>
          <div class="kpi-sub">
            <span :style="{ color: ORDER_STATE_COLORS.RUNNING }">{{ $t('mes.board.kpi.running', { n: kpi.runningOrders ?? 0 }) }}</span>
            <span>{{ $t('mes.board.kpi.waiting', { n: kpi.waitingOrders ?? 0 }) }}</span>
            <span v-if="kpi.idleOrders" :style="{ color: ORDER_STATE_COLORS.IDLE }">{{ $t('mes.board.kpi.idle', { n: kpi.idleOrders }) }}</span>
          </div>
        </div>
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.lines') }}</div>
          <div class="kpi-value">{{ kpi.runningLines ?? '-' }}<small>/ {{ kpi.lines ?? 0 }}</small></div>
          <div class="kpi-sub">{{ $t('mes.board.kpi.linesSub') }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.todayGood') }}</div>
          <div class="kpi-value">{{ formatQty(kpi.todayGood) }}</div>
          <div class="kpi-sub">
            <span>{{ $t('mes.board.kpi.scrap', { n: formatQty(kpi.todayScrap) }) }}</span>
            <span v-if="kpi.todayYield != null">{{ $t('mes.board.kpi.yield', { n: kpi.todayYield }) }}</span>
          </div>
        </div>
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.todayHours') }}</div>
          <div class="kpi-value">{{ kpi.todayHours ?? '-' }}<small>{{ $t('mes.board.unitHour') }}</small></div>
          <div class="kpi-sub">{{ $t('mes.board.kpi.completedToday', { n: kpi.completedToday ?? 0 }) }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.risk') }}</div>
          <div class="kpi-value" :style="{ color: riskCount ? RISK_COLORS.OVERDUE : RISK_COLORS.ON_TIME }">{{ riskCount }}<small>{{ $t('mes.board.unitOrder') }}</small></div>
          <div class="kpi-sub">
            <span :style="{ color: RISK_COLORS.AT_RISK }">{{ $t('mes.board.kpi.atRisk', { n: kpi.atRiskOrders ?? 0 }) }}</span>
            <span :style="{ color: RISK_COLORS.OVERDUE }">{{ $t('mes.board.kpi.overdue', { n: kpi.overdueOrders ?? 0 }) }}</span>
          </div>
        </div>
        <div class="kpi">
          <div class="kpi-label">{{ $t('mes.board.kpi.allDone') }}</div>
          <div class="kpi-value">{{ kpi.allDoneDate ? kpi.allDoneDate.slice(5) : '-' }}</div>
          <div class="kpi-sub">
            <span>{{ kpi.activeOrders ? etaText(kpi.allDoneDate) : $t('mes.board.noOrders') }}</span>
            <span>{{ $t('mes.board.kpi.remainingHours', { n: kpi.remainingHours ?? 0 }) }}</span>
          </div>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <template #title>{{ $t('mes.board.lines') }}</template>
      <template #extra>
        <span class="legend">
          <span v-for="(c, s) in LINE_STATE_COLORS" :key="s"><i class="dot" :style="{ background: c }" />{{ lineStateLabel(s) }}</span>
        </span>
      </template>
      <a-empty v-if="!lines.length" :description="$t('mes.board.noLines')" />
      <div v-else class="lines">
        <div v-for="l in lines" :key="l.id" class="line" :style="{ '--c': LINE_STATE_COLORS[l.state] }">
          <div class="line-head">
            <span class="line-name"><i :class="['dot', { pulse: l.state === 'RUNNING' }]" />{{ l.name }}</span>
            <span class="line-state">{{ lineStateLabel(l.state) }}</span>
          </div>
          <div class="line-stats">
            <div><b>{{ formatQty(l.todayGood) }}</b><span>{{ $t('mes.board.todayGood') }}</span></div>
            <div><b>{{ l.queueOrders }}</b><span>{{ $t('mes.board.queue') }}</span></div>
            <div><b>{{ l.remainingHours }}</b><span>{{ $t('mes.board.remainingH') }}</span></div>
          </div>
          <a-tooltip :title="$t('mes.board.utilTip', { h: l.todayHours, cap: l.capacity })">
            <div class="util">
              <div class="util-bar"><i :style="{ width: `${Math.min(100, Number(l.utilization || 0))}%` }" /></div>
              <span>{{ l.utilization == null ? $t('mes.board.noCapacity') : `${l.utilization}%` }}</span>
            </div>
          </a-tooltip>
          <div class="line-clear">
            {{ l.queueOrders ? $t('mes.board.clearAt', { d: l.clearDate ? l.clearDate.slice(5) : '-', rel: etaText(l.clearDate) }) : $t('mes.board.noQueue') }}
          </div>
          <div class="jobs">
            <div v-for="j in l.jobs.slice(0, 4)" :key="`${j.woNo}-${j.seq}`" :class="['job', { running: j.running }]">
              <span class="job-no">{{ j.woNo }}</span>
              <span class="job-op">{{ j.seq }} {{ j.opName }}</span>
              <span class="job-meta">{{ j.running ? $t('mes.board.todayN', { n: formatQty(j.todayGood) }) : j.eta ? j.eta.slice(5) : '-' }}</span>
            </div>
            <div v-if="l.jobs.length > 4" class="sub-text">{{ $t('mes.board.moreJobs', { n: l.jobs.length - 4 }) }}</div>
          </div>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <template #title>{{ $t('mes.board.orders') }}</template>
      <template #extra><span class="sub-text">{{ $t('mes.board.ordersTip') }}</span></template>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="filtered"
        :loading="loading && !orders.length"
        :pagination="{ pageSize: 20, showSizeChanger: true, hideOnSinglePage: true }"
        :scroll="{ x: 1200 }"
        :expanded-row-keys="expanded"
        @expand="onExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'wo'">
            <div class="strong">{{ record.woNo }}</div>
            <div class="sub-text">{{ record.product }}<template v-if="record.spec"> · {{ record.spec }}</template></div>
          </template>
          <template v-else-if="column.key === 'customer'">
            <template v-if="record.salesOrderNo">
              <div>{{ record.customerName || '-' }}</div>
              <div class="sub-text">{{ record.salesOrderNo }}</div>
            </template>
            <span v-else class="sub-text">{{ $t('mes.board.noSalesOrder') }}</span>
          </template>
          <template v-else-if="column.key === 'steps'">
            <div v-if="record.ops.length" class="steps">
              <a-tooltip v-for="op in record.ops" :key="op.id" :title="opTip(op, record.qty)">
                <i :class="['step', op.state.toLowerCase(), { today: Number(op.todayGood) > 0 }]" />
              </a-tooltip>
            </div>
            <div class="sub-text">
              <template v-if="record.currentOp">
                {{ $t('mes.board.atOp', { i: record.doneOps + 1, n: record.totalOps, op: record.currentOp }) }}
                <template v-if="record.currentWorkCenter"> · {{ record.currentWorkCenter }}</template>
              </template>
              <template v-else>{{ record.ops.length ? $t('mes.board.allOpsDone') : $t('mes.board.noOps') }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress)" size="small" :stroke-color="RISK_COLORS[record.risk] === RISK_COLORS.OVERDUE ? RISK_COLORS.OVERDUE : undefined" />
            <div class="sub-text">{{ $t('mes.board.finished', { done: formatQty(record.finishedQty), qty: formatQty(record.qty), unit: record.unit || '' }) }}</div>
          </template>
          <template v-else-if="column.key === 'remaining'">
            <div>{{ $t('mes.board.remainingOps', { n: record.totalOps - record.doneOps }) }}</div>
            <div class="sub-text">{{ $t('mes.board.hoursN', { n: record.remainingHours }) }}</div>
          </template>
          <template v-else-if="column.key === 'eta'">
            <div :style="{ color: record.risk === 'AT_RISK' || record.risk === 'OVERDUE' ? RISK_COLORS[record.risk] : undefined }" class="strong">
              {{ record.eta || '-' }}
            </div>
            <div class="sub-text">{{ etaText(record.eta) }}</div>
          </template>
          <template v-else-if="column.key === 'due'">
            <div>{{ record.dueDate || record.planEnd || '-' }}</div>
            <div class="sub-text">{{ record.dueDate ? $t('mes.board.dueBySales') : record.planEnd ? $t('mes.board.dueByPlan') : '' }}</div>
          </template>
          <template v-else-if="column.key === 'state'">
            <a-space direction="vertical" :size="2">
              <a-tag :color="ORDER_STATE_COLORS[record.state]" :bordered="false">{{ orderStateLabel(record.state) }}</a-tag>
              <a-tag :color="RISK_COLORS[record.risk]" :bordered="false">{{ riskLabel(record.risk) }}</a-tag>
            </a-space>
          </template>
        </template>
        <template #expandedRowRender="{ record }">
          <a-table row-key="id" size="small" :columns="opColumns" :data-source="record.ops" :pagination="false" class="op-table">
            <template #bodyCell="{ column, record: op }">
              <template v-if="column.key === 'op'">{{ op.seq }} {{ op.name }}</template>
              <template v-else-if="column.key === 'state'">
                <a-badge :color="OP_STATE_COLORS[op.state]" :text="opStateLabel(op.state)" />
              </template>
              <template v-else-if="column.key === 'done'">
                {{ formatQty(op.goodQty) }} / {{ formatQty(record.qty) }}
                <span v-if="Number(op.scrapQty)" class="danger"> ({{ $t('mes.board.opScrap', { n: formatQty(op.scrapQty) }) }})</span>
              </template>
              <template v-else-if="column.key === 'today'">{{ Number(op.todayGood) ? formatQty(op.todayGood) : '-' }}</template>
              <template v-else-if="column.key === 'hours'">{{ op.state === 'DONE' ? '-' : op.remainingHours }}</template>
              <template v-else-if="column.key === 'eta'">{{ op.state === 'DONE' ? '-' : op.eta || $t('mes.board.etaUnknown') }}</template>
              <template v-else>{{ op[column.dataIndex] || '-' }}</template>
            </template>
          </a-table>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { FullscreenExitOutlined, FullscreenOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { getBoard } from '@/api/mes/board'
import WorkCenterSelect from '@/components/mes/WorkCenterSelect.vue'
import { LINE_STATE_COLORS, OP_STATE_COLORS, ORDER_STATE_COLORS, RISK_COLORS, etaText, lineStateLabel, opStateLabel, opTip, orderStateLabel, riskLabel } from '@/components/mes/board'
import { formatQty } from '@/components/wms/wms'
import { t } from '@/locales'

/**
 * 生产看板：在制工单走到哪道工序、还剩几道、预计哪天完工，各产线今天的生产情况。
 * 每 30 秒自动刷新；全屏后可以放在车间的电脑上（电视免登录投屏用「数据大屏」的生产进度）
 */
const REFRESH_MS = 30000

const rootRef = ref()
const workCenterId = ref(null)
const keyword = ref('')
const filter = ref('ALL')
const autoRefresh = ref(true)
const loading = ref(false)
const data = ref({})
const updatedAt = ref('')
const expanded = ref([])
const fullscreen = ref(false)

const kpi = computed(() => data.value.kpi || {})
const lines = computed(() => data.value.lines || [])
const orders = computed(() => data.value.orders || [])
const riskCount = computed(() => (kpi.value.atRiskOrders || 0) + (kpi.value.overdueOrders || 0))

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return orders.value.filter((o) => {
    if (filter.value === 'RUNNING' && o.state !== 'RUNNING') return false
    if (filter.value === 'RISK' && o.risk !== 'AT_RISK' && o.risk !== 'OVERDUE') return false
    if (filter.value === 'TO_STOCK' && o.state !== 'TO_STOCK') return false
    if (!kw) return true
    return [o.woNo, o.product, o.materialCode, o.customerName, o.salesOrderNo].some((v) => v && String(v).toLowerCase().includes(kw))
  })
})

const columns = [
  { title: t('mes.board.col.wo'), key: 'wo', width: 200 },
  { title: t('mes.board.col.customer'), key: 'customer', width: 150 },
  { title: t('mes.board.col.steps'), key: 'steps', width: 220 },
  { title: t('mes.board.col.progress'), key: 'progress', width: 170 },
  { title: t('mes.board.col.remaining'), key: 'remaining', width: 100 },
  { title: t('mes.board.col.eta'), key: 'eta', width: 110 },
  { title: t('mes.board.col.due'), key: 'due', width: 110 },
  { title: t('mes.board.col.state'), key: 'state', width: 100 }
]
const opColumns = [
  { title: t('mes.board.col.op'), key: 'op', width: 180 },
  { title: t('mes.board.col.workCenter'), dataIndex: 'workCenterName', width: 150 },
  { title: t('mes.board.col.opState'), key: 'state', width: 100 },
  { title: t('mes.board.col.done'), key: 'done', width: 170 },
  { title: t('mes.board.col.today'), key: 'today', width: 100 },
  { title: t('mes.board.col.hours'), key: 'hours', width: 110 },
  { title: t('mes.board.col.opEta'), key: 'eta', width: 120 }
]

function onExpand(open, record) {
  expanded.value = open ? [...expanded.value, record.id] : expanded.value.filter((id) => id !== record.id)
}

let seq = 0
async function load() {
  const mine = ++seq
  loading.value = true
  try {
    const res = await getBoard({ workCenterId: workCenterId.value || undefined })
    if (mine !== seq) return
    data.value = res
    updatedAt.value = dayjs().format('HH:mm:ss')
  } finally {
    if (mine === seq) loading.value = false
  }
}

let timer
function schedule() {
  clearInterval(timer)
  if (autoRefresh.value) timer = setInterval(() => !document.hidden && load(), REFRESH_MS)
}
watch(autoRefresh, schedule)

function toggleFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen()
  else rootRef.value?.requestFullscreen?.()
}
const onFullscreenChange = () => (fullscreen.value = document.fullscreenElement === rootRef.value)

onMounted(() => {
  load()
  schedule()
  document.addEventListener('fullscreenchange', onFullscreenChange)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped>
.board-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.board-page > .page-card + .page-card {
  margin-top: 0;
}
.board-page.fullscreen {
  background: var(--app-bg);
  padding: 16px;
  overflow: auto;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
}
.wc-select {
  width: 200px;
}
.keyword {
  width: 220px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.strong {
  font-weight: 500;
}
.danger {
  color: #ff4d4f;
}
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 12px;
}
.kpi {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-hover);
}
.kpi-label {
  color: var(--app-text-2);
  font-size: 13px;
}
.kpi-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.4;
  font-variant-numeric: tabular-nums;
}
.kpi-value small {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
  color: var(--app-text-3);
}
.kpi-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  font-size: 12px;
  color: var(--app-text-3);
}
.legend {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--app-text-3);
}
.legend span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c);
}
.dot.pulse {
  animation: pulse 1.6s ease-out infinite;
}
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--c) 60%, transparent);
  }
  100% {
    box-shadow: 0 0 0 8px transparent;
  }
}
.lines {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.line {
  border: 1px solid var(--app-border);
  border-top: 3px solid var(--c);
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.line-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.line-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
.line-state {
  font-size: 12px;
  color: var(--c);
}
.line-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  text-align: center;
}
.line-stats b {
  display: block;
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}
.line-stats span {
  font-size: 12px;
  color: var(--app-text-3);
}
.util {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-2);
}
.util-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--app-hover);
  overflow: hidden;
}
.util-bar i {
  display: block;
  height: 100%;
  background: var(--c);
  border-radius: 3px;
}
.line-clear {
  font-size: 12px;
  color: var(--app-text-2);
}
.jobs {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-top: 1px dashed var(--app-border);
  padding-top: 8px;
  min-height: 24px;
}
.job {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-2);
}
.job.running .job-no::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 4px;
  border-radius: 50%;
  background: #52c41a;
  vertical-align: middle;
}
.job-op {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.job-meta {
  color: var(--app-text-3);
}
.steps {
  display: flex;
  gap: 3px;
  margin-bottom: 4px;
}
.step {
  flex: 1;
  max-width: 36px;
  height: 10px;
  border-radius: 2px;
  background: rgba(128, 128, 128, 0.25);
  cursor: default;
}
.step.done {
  background: #52c41a;
}
.step.doing {
  background: #1677ff;
}
.step.doing.today {
  animation: blink 1.6s ease-in-out infinite;
}
@keyframes blink {
  50% {
    opacity: 0.45;
  }
}
.op-table {
  margin: 4px 0;
}
</style>
