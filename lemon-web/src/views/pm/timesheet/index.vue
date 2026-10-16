<template>
  <div class="timesheet-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <!-- 我的工时 -->
        <a-tab-pane key="mine" tab="我的工时">
          <div class="filters">
            <a-button @click="shiftWeek(-1)"><LeftOutlined /></a-button>
            <a-date-picker v-model:value="weekDay" picker="week" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadMine" />
            <a-button @click="shiftWeek(1)"><RightOutlined /></a-button>
            <span class="sub-text">{{ week[0] }} ~ {{ week[1] }} · 合计 <b>{{ weekTotal }}</b> 小时</span>
            <a-button type="primary" class="push-right" @click="openEntry()">
              <template #icon><PlusOutlined /></template>填报工时
            </a-button>
          </div>
          <div class="days">
            <div v-for="d in weekDays" :key="d.date" class="day" :class="{ today: d.date === todayStr }">
              <div class="day-name">{{ d.label }}</div>
              <div class="day-hours" :class="{ over: d.hours > 8 }">{{ d.hours }}h</div>
            </div>
          </div>
          <a-table row-key="id" size="middle" :columns="mineColumns" :data-source="mine" :loading="mineLoading" :pagination="false"
                   :scroll="{ x: 900 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'project'">
                <div>{{ record.projectName }}</div>
                <div class="sub-text">{{ record.projectCode }}{{ record.taskName ? ` · ${record.taskName}` : '' }}</div>
              </template>
              <template v-else-if="column.key === 'hours'">{{ Number(record.hours) }}</template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="pm_timesheet_status" :value="record.status" />
                <div v-if="record.status === 'REJECTED'" class="sub-text danger">{{ record.rejectReason }}</div>
              </template>
              <template v-else-if="column.key === 'action'">
                <span v-if="record.status !== 'APPROVED'" class="table-actions">
                  <a-button type="link" size="small" @click="openEntry(record)"><EditOutlined /></a-button>
                  <a-popconfirm title="确定删除这条工时？" @confirm="removeEntry(record)">
                    <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                  </a-popconfirm>
                </span>
              </template>
            </template>
          </a-table>

          <div class="section-title">我的任务</div>
          <a-table row-key="id" size="small" :columns="taskColumns" :data-source="tasks" :loading="taskLoading" :pagination="false"
                   :scroll="{ x: 800 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'task'">
                <div>{{ record.name }}</div>
                <div class="sub-text">{{ record.projectName }}</div>
              </template>
              <template v-else-if="column.key === 'dates'">
                {{ record.planStart }} ~ {{ record.planEnd }}
                <a-tag v-if="record.overdue" color="red" :bordered="false">逾期</a-tag>
              </template>
              <template v-else-if="column.key === 'progress'">
                <a-progress :percent="record.progress" size="small" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popover trigger="click" title="更新进度" :open="progressFor === record.id" @open-change="(v) => togglePopover(v, record)">
                  <template #content>
                    <div class="progress-pop">
                      <a-slider v-model:value="progressValue" :step="5" :marks="{ 0: '0', 50: '50', 100: '100' }" />
                      <div class="pop-actions">
                        <a-button size="small" @click="progressFor = null">取消</a-button>
                        <a-button size="small" type="primary" :loading="progressSaving" @click="saveProgress(record)">保存</a-button>
                      </div>
                    </div>
                  </template>
                  <a-button type="link" size="small">更新进度</a-button>
                </a-popover>
                <a-button type="link" size="small" @click="openEntry(null, record)">填工时</a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 工时审批 -->
        <a-tab-pane v-if="userStore.hasPerm('pm:timesheet:list')" key="review" tab="工时审批">
          <div class="filters">
            <ProjectSelect v-model:value="query.projectId" class="w220" placeholder="全部项目" />
            <IdentityUserSelect v-model:value="query.userId" class="w180" placeholder="全部成员" />
            <a-select v-model:value="query.status" class="w120" placeholder="全部状态" allow-clear :options="toOptions(dict.pm_timesheet_status)" />
            <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
            <a-button type="primary" @click="search"><template #icon><SearchOutlined /></template>查询</a-button>
          </div>
          <div class="filters">
            <span v-perm="'pm:timesheet:approve'">
              <a-space>
                <a-popconfirm :title="`确定通过选中的 ${selectedKeys.length} 条工时？`" :disabled="!selectedKeys.length" @confirm="approve">
                  <a-button type="primary" :disabled="!selectedKeys.length"><template #icon><CheckOutlined /></template>批量通过</a-button>
                </a-popconfirm>
                <a-button danger :disabled="!selectedKeys.length" @click="rejectOpen = true"><template #icon><CloseOutlined /></template>批量驳回</a-button>
              </a-space>
            </span>
            <span v-if="selectedKeys.length" class="sub-text">已选 {{ selectedKeys.length }} 项，{{ selectedHours }} 小时</span>
          </div>
          <a-table row-key="id" size="middle" :columns="reviewColumns" :data-source="list" :loading="loading" :pagination="pagination"
                   :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'SUBMITTED' }) }"
                   :scroll="{ x: 1200 }" @change="onTableChange">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'project'">
                <div>{{ record.projectName }}</div>
                <div class="sub-text">{{ record.projectCode }}{{ record.taskName ? ` · ${record.taskName}` : '' }}</div>
              </template>
              <template v-else-if="column.key === 'hours'">{{ Number(record.hours) }}</template>
              <template v-else-if="column.key === 'status'">
                <DictTag type="pm_timesheet_status" :value="record.status" />
                <div v-if="record.status === 'REJECTED'" class="sub-text danger">{{ record.rejectReason }}</div>
              </template>
              <template v-else-if="column.key === 'cost'">
                <template v-if="record.status === 'APPROVED'">
                  {{ formatMoney(record.cost) }}
                  <div class="sub-text">{{ formatMoney(record.costRate) }} 元/小时</div>
                </template>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'approve'">
                <template v-if="record.approveByName">
                  <div>{{ record.approveByName }}</div>
                  <div class="sub-text">{{ record.approveTime }}</div>
                </template>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 填报 -->
    <a-modal v-model:open="entryOpen" :title="entry.id ? '修改工时' : '填报工时'" :confirm-loading="entrySaving" destroy-on-close @ok="saveEntry">
      <a-form ref="entryRef" :model="entry" layout="vertical">
        <a-form-item label="项目" name="projectId" :rules="[{ required: true, message: '请选择项目' }]" extra="只能选择你参与的、未完成的项目">
          <ProjectSelect v-model:value="entry.projectId" mine :allow-clear="false" @update:value="onEntryProject" />
        </a-form-item>
        <a-form-item label="任务" name="taskId">
          <a-select v-model:value="entry.taskId" allow-clear show-search option-filter-prop="label" :options="taskOptions" placeholder="选填" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="日期" name="workDate" :rules="[{ required: true, message: '请选择日期' }]">
              <a-date-picker v-model:value="entry.workDate" value-format="YYYY-MM-DD" :disabled-date="(d) => d.isAfter(dayjs(), 'day')"
                             class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="工时" name="hours" :rules="[{ required: true, message: '请填写工时' }]">
              <a-input-number v-model:value="entry.hours" :min="0.5" :max="24" :step="0.5" :precision="1" addon-after="小时" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="工作内容" name="content">
          <a-textarea v-model:value="entry.content" :rows="3" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 驳回 -->
    <a-modal v-model:open="rejectOpen" title="驳回工时" :confirm-loading="rejecting" destroy-on-close @ok="reject">
      <a-textarea v-model:value="rejectReason" :rows="3" :maxlength="255" placeholder="请填写驳回原因" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  CheckOutlined, CloseOutlined, DeleteOutlined, EditOutlined, LeftOutlined, PlusOutlined, RightOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import {
  addTimesheet, deleteTimesheet, myTasks, myTimesheet, pageTimesheet, reviewTimesheet, taskTree, updateTaskProgress, updateTimesheet
} from '@/api/pm'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { leafTasks, today, weekRange } from '@/components/pm/pm'

const dict = useDict('pm_timesheet_status')
const userStore = useUserStore()
const tab = ref('mine')
const todayStr = today()

// ---------------------------------------------------------------- 我的工时

const weekDay = ref(todayStr)
const week = computed(() => weekRange(weekDay.value))
const mine = ref([])
const mineLoading = ref(false)

const mineColumns = [
  { title: '日期', dataIndex: 'workDate', width: 110 },
  { title: '项目 / 任务', key: 'project', width: 260 },
  { title: '工时', key: 'hours', width: 80 },
  { title: '工作内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', key: 'status', width: 150 },
  { title: '', key: 'action', width: 100, fixed: 'right' }
]

const weekTotal = computed(() => mine.value.filter((t) => t.status !== 'REJECTED').reduce((s, t) => s + Number(t.hours), 0))
const weekDays = computed(() => {
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return names.map((n, i) => {
    const date = dayjs(week.value[0]).add(i, 'day').format('YYYY-MM-DD')
    const hours = mine.value.filter((t) => t.workDate === date && t.status !== 'REJECTED').reduce((s, t) => s + Number(t.hours), 0)
    return { date, label: `${n} ${date.slice(5)}`, hours }
  })
})

async function loadMine() {
  mineLoading.value = true
  try {
    mine.value = await myTimesheet(week.value[0], week.value[1])
  } finally {
    mineLoading.value = false
  }
}

function shiftWeek(n) {
  weekDay.value = dayjs(weekDay.value).add(n, 'week').format('YYYY-MM-DD')
  loadMine()
}

// 我的任务
const tasks = ref([])
const taskLoading = ref(false)
const taskColumns = [
  { title: '任务', key: 'task', width: 240 },
  { title: '计划日期', key: 'dates', width: 230 },
  { title: '计划工时', dataIndex: 'planHours', width: 90 },
  { title: '进度', key: 'progress', width: 160 },
  { title: '', key: 'action', width: 170, fixed: 'right' }
]

async function loadTasks() {
  taskLoading.value = true
  try {
    tasks.value = await myTasks()
  } finally {
    taskLoading.value = false
  }
}

const progressFor = ref(null)
const progressValue = ref(0)
const progressSaving = ref(false)

function togglePopover(open, record) {
  progressFor.value = open ? record.id : null
  if (open) progressValue.value = record.progress
}

async function saveProgress(record) {
  progressSaving.value = true
  try {
    await updateTaskProgress(record.id, progressValue.value)
    message.success('进度已更新')
    progressFor.value = null
    loadTasks()
  } finally {
    progressSaving.value = false
  }
}

// 填报
const entryOpen = ref(false)
const entrySaving = ref(false)
const entryRef = ref()
const entry = ref({})
const taskOptions = ref([])

async function onEntryProject(projectId) {
  taskOptions.value = []
  if (!projectId) return
  const tree = await taskTree(projectId)
  taskOptions.value = leafTasks(tree).map((t) => ({ value: t.id, label: t.name }))
  if (entry.value.taskId && !taskOptions.value.some((o) => o.value === entry.value.taskId)) entry.value.taskId = undefined
}

function openEntry(record, task) {
  entry.value = record
    ? { id: record.id, projectId: record.projectId, taskId: record.taskId || undefined, workDate: record.workDate, hours: Number(record.hours),
        content: record.content }
    : { id: undefined, projectId: task?.projectId, taskId: task?.id, workDate: todayStr, hours: 8, content: undefined }
  taskOptions.value = []
  if (entry.value.projectId) onEntryProject(entry.value.projectId)
  entryOpen.value = true
}

async function saveEntry() {
  try {
    await entryRef.value.validate()
  } catch {
    return
  }
  entrySaving.value = true
  try {
    const { id, ...data } = entry.value
    const payload = { ...data, taskId: data.taskId || null, content: data.content || null }
    const res = id ? await updateTimesheet(id, payload) : await addTimesheet(payload)
    if (res?.warning) message.warning(`已保存，但${res.warning}`, 5)
    else message.success('保存成功')
    entryOpen.value = false
    loadMine()
  } finally {
    entrySaving.value = false
  }
}

async function removeEntry(record) {
  await deleteTimesheet(record.id)
  message.success('删除成功')
  loadMine()
}

// ---------------------------------------------------------------- 工时审批

const query = reactive({ projectId: undefined, userId: undefined, status: 'SUBMITTED', range: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(pageTimesheet, query, {
  params: ({ range, ...q }) => ({ ...q, beginDate: range?.[0], endDate: range?.[1] })
})

const reviewColumns = [
  { title: '日期', dataIndex: 'workDate', width: 110 },
  { title: '成员', dataIndex: 'userName', width: 100 },
  { title: '项目 / 任务', key: 'project', width: 260 },
  { title: '工时', key: 'hours', width: 70 },
  { title: '工作内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', key: 'status', width: 150 },
  { title: '人工成本', key: 'cost', width: 130, align: 'right' },
  { title: '审批', key: 'approve', width: 160 }
]

const selectedHours = computed(() =>
  list.value.filter((t) => selectedKeys.value.includes(t.id)).reduce((s, t) => s + Number(t.hours), 0)
)

async function approve() {
  const count = await reviewTimesheet({ ids: selectedKeys.value, approved: true })
  message.success(`已通过 ${count} 条`)
  selectedKeys.value = []
  load()
}

const rejectOpen = ref(false)
const rejecting = ref(false)
const rejectReason = ref('')

async function reject() {
  if (!rejectReason.value.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  try {
    const count = await reviewTimesheet({ ids: selectedKeys.value, approved: false, reason: rejectReason.value })
    message.success(`已驳回 ${count} 条`)
    rejectOpen.value = false
    rejectReason.value = ''
    selectedKeys.value = []
    load()
  } finally {
    rejecting.value = false
  }
}

onMounted(() => {
  loadMine()
  loadTasks()
  if (userStore.hasPerm('pm:timesheet:list')) load()
})
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.push-right {
  margin-left: auto;
}
.w220 {
  width: 220px;
}
.w180 {
  width: 180px;
}
.w120 {
  width: 120px;
}
.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.day {
  padding: 8px;
  border-radius: 8px;
  background: var(--app-hover);
  text-align: center;
}
.day.today {
  outline: 1px solid var(--app-primary);
}
.day-name {
  font-size: 12px;
  color: var(--app-text-3);
}
.day-hours {
  font-size: 16px;
  font-weight: 600;
}
.day-hours.over {
  color: #fa8c16;
}
.section-title {
  margin: 24px 0 12px;
  font-weight: 600;
}
.progress-pop {
  width: 240px;
}
.pop-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.full {
  width: 100%;
}
@media (max-width: 767px) {
  .days {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
