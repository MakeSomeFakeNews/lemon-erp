<template>
  <SubPage :open="open" :title="project ? `${project.code} ${project.name}` : $t('pm.detail.title')" width="min(1100px, 100vw)" destroy-on-close
            @close="emit('update:open', false)">
    <a-spin :spinning="loading">
      <template v-if="project">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="pm_project_type" :value="project.type" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="pm_project_status" :value="project.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('pm.common.priority')"><DictTag type="pm_priority" :value="project.priority" /></a-descriptions-item>
          <a-descriptions-item :label="$t('pm.common.customer')">{{ project.customerName || $t('pm.common.internal') }}</a-descriptions-item>
          <a-descriptions-item v-if="project.salesOrderNo" :label="$t('pm.common.salesOrder')">{{ project.salesOrderNo }}</a-descriptions-item>
          <a-descriptions-item v-if="project.budgetControl === 1" :label="$t('pm.detail.budgetControl')">{{ $t('pm.detail.budgetControlValue') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pm.common.manager')">{{ project.managerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pm.detail.dept')">{{ project.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pm.common.planDates')">{{ project.planStart }} ~ {{ project.planEnd }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pm.detail.actualDates')">{{ project.actualStart || '-' }} ~ {{ project.actualEnd || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('pm.detail.contractAmount')">{{ formatMoney(project.contractAmount) }}{{ $t('pm.detail.taxRateSuffix', { rate: project.taxRate }) }}</a-descriptions-item>
          <a-descriptions-item v-if="project.description" :label="$t('common.field.description')" :span="3">{{ project.description }}</a-descriptions-item>
        </a-descriptions>

        <!-- 概况：进度、成本、收入、挣值 -->
        <a-row v-if="acc" :gutter="[16, 16]" class="block">
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">{{ $t('pm.detail.progressVsPlan') }}</div>
              <a-progress :percent="Number(acc.progress)" :success="{ percent: 0 }" size="small" />
              <a-progress :percent="Number(acc.plannedRate)" size="small" stroke-color="#d9d9d9" :format="(p) => $t('pm.detail.planPercent', { p })" />
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">{{ $t('pm.project.costBudget') }}</div>
              <div class="stat-value" :class="{ danger: overBudget }">
                {{ formatMoney(acc.actualCost) }} <span class="sub-text">/ {{ formatMoney(acc.budget) }}</span>
              </div>
              <div class="sub-text">{{ $t('pm.common.labor') }} {{ formatMoney(acc.laborCost) }} · {{ $t('pm.common.expense') }} {{ formatMoney(acc.expenseCost) }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">{{ $t('pm.detail.revenueProfit') }}</div>
              <div class="stat-value">{{ formatMoney(acc.revenue) }} <span class="sub-text">/ {{ formatMoney(acc.grossProfit) }}</span></div>
              <div class="sub-text">
                SPI <a-tag :color="indexTag(acc.spi).color" :bordered="false">{{ indexTag(acc.spi).text }}</a-tag>
                CPI <a-tag :color="indexTag(acc.cpi).color" :bordered="false">{{ indexTag(acc.cpi).text }}</a-tag>
                {{ $t('pm.common.received') }} {{ formatMoney(acc.received) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="member" :tab="$t('pm.detail.membersTab', { n: members.length })">
            <div class="tab-toolbar">
              <a-button v-perm="'pm:member:edit'" type="primary" size="small" :disabled="project.status === 'CLOSED'" @click="openMember()">
                <template #icon><PlusOutlined /></template>{{ $t('pm.detail.addMember') }}
              </a-button>
            </div>
            <a-table row-key="id" size="small" :columns="memberColumns" :data-source="members" :pagination="false" :scroll="{ x: 800 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'user'">
                  <div>{{ record.userName }}</div>
                  <div class="sub-text">{{ record.loginName }} · {{ record.deptName || '-' }}</div>
                </template>
                <template v-else-if="column.key === 'role'"><DictTag type="pm_member_role" :value="record.role" /></template>
                <template v-else-if="column.key === 'allocation'">{{ record.allocation }}%</template>
                <template v-else-if="column.key === 'costRate'">{{ formatMoney(record.costRate) }}</template>
                <template v-else-if="column.key === 'hours'">{{ $t('pm.common.nHours', { n: Number(record.approvedHours || 0) }) }}</template>
                <template v-else-if="column.key === 'dates'">{{ record.joinDate || '-' }} ~ {{ record.leaveDate || '-' }}</template>
                <template v-else-if="column.key === 'action'">
                  <span v-perm="'pm:member:edit'" class="table-actions">
                    <a-button type="link" size="small" @click="openMember(record)"><EditOutlined /></a-button>
                    <a-popconfirm v-if="record.role !== 'MANAGER'" :title="$t('pm.detail.confirmRemoveMember', { name: record.userName })" @confirm="removeMember(record)">
                      <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="plan" :tab="$t('pm.detail.planTab')">
            <GanttChart :tasks="tasks" max-height="480px" />
          </a-tab-pane>
          <a-tab-pane v-if="canTimesheet" key="timesheet" :tab="$t('pm.common.timesheet')">
            <a-table row-key="id" size="small" :columns="timesheetColumns" :data-source="timesheets.records" :loading="timesheets.loading"
                     :pagination="timesheets.pagination" :scroll="{ x: 800 }" @change="(p) => loadTimesheets(p.current)">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="pm_timesheet_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'cost'">{{ record.status === 'APPROVED' ? formatMoney(record.cost) : '-' }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="canCost" key="cost" :tab="$t('pm.common.expense')">
            <a-table row-key="id" size="small" :columns="costColumns" :data-source="costs.records" :loading="costs.loading"
                     :pagination="costs.pagination" :scroll="{ x: 700 }" @change="(p) => loadCosts(p.current)">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'costType'"><DictTag type="pm_cost_type" :value="record.costType" /></template>
                <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="billing" :tab="$t('pm.detail.billingTab')">
            <a-table row-key="id" size="small" :columns="billingColumns" :data-source="billings" :pagination="false" :scroll="{ x: 700 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'status'"><DictTag type="pm_billing_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'bill'">
                  <template v-if="record.billNo">
                    <div>{{ record.billNo }}</div>
                    <div class="sub-text">{{ record.billDate }} · {{ $t('pm.common.received') }} {{ formatMoney(record.settledAmount) }}</div>
                  </template>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('pm.detail.attachmentTab')">
            <AttachmentPanel biz-type="pm_project" :biz-id="projectId" :editable="userStore.hasPerm('pm:project:edit')" />
          </a-tab-pane>
          <a-tab-pane key="history" :tab="$t('pm.detail.historyTab')">
            <ChangeLogPanel biz-type="pm_project" :biz-id="projectId" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <!-- 成员 -->
    <a-modal v-model:open="memberOpen" :title="memberForm.id ? $t('pm.detail.editMember') : $t('pm.detail.addMember')" :confirm-loading="memberSaving" destroy-on-close
             @ok="saveMember">
      <a-form ref="memberRef" :model="memberForm" layout="vertical">
        <a-form-item :label="$t('pm.common.member')" name="userId" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('pm.common.member') }) }]">
          <IdentityUserSelect v-model:value="memberForm.userId" :label="memberForm.userName" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('pm.detail.role')" name="role" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('pm.detail.role') }) }]">
              <a-select v-model:value="memberForm.role" :options="toOptions(dict.pm_member_role)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.detail.allocation')" name="allocation" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('pm.detail.allocation') }) }]">
              <a-input-number v-model:value="memberForm.allocation" :min="1" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('pm.common.hourlyCost')" name="costRate" :extra="$t('pm.detail.costRateTip')">
              <a-input-number v-model:value="memberForm.costRate" :min="0" :precision="2" :addon-after="$t('pm.common.yuanPerHour')"
                              :placeholder="$t('pm.detail.costRatePlaceholder')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.detail.joinDate')" name="joinDate">
              <a-date-picker v-model:value="memberForm.joinDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.detail.leaveDate')" name="leaveDate">
              <a-date-picker v-model:value="memberForm.leaveDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import GanttChart from './GanttChart.vue'
import {
  addMember, deleteMember, getAccounting, getProject, listBillings, listMembers, pageCost, pageTimesheet, taskTree, updateMember
} from '@/api/pm'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { indexTag } from './pm'
import { t } from '@/locales'

/** 项目详情：基本信息、进度与成本概况、成员、计划（甘特图）、工时、费用、收款 */
const props = defineProps({
  open: Boolean,
  projectId: Number
})
const emit = defineEmits(['update:open', 'changed'])

const dict = useDict('pm_member_role')
const userStore = useUserStore()
const canTimesheet = computed(() => userStore.hasPerm('pm:timesheet:list'))
const canCost = computed(() => userStore.hasPerm(['pm:cost:list', 'pm:accounting:list']))
const loading = ref(false)
const project = ref(null)
const acc = ref(null)
const tab = ref('member')
const members = ref([])
const tasks = ref([])
const billings = ref([])
const timesheets = reactive({ records: [], loading: false, pagination: { current: 1, pageSize: 10, total: 0 } })
const costs = reactive({ records: [], loading: false, pagination: { current: 1, pageSize: 10, total: 0 } })

const overBudget = computed(() => acc.value && Number(acc.value.budget) > 0 && Number(acc.value.actualCost) > Number(acc.value.budget))

const memberColumns = [
  { title: t('pm.common.member'), key: 'user', width: 180 },
  { title: t('pm.detail.roleShort'), key: 'role', width: 110 },
  { title: t('pm.detail.allocationShort'), key: 'allocation', width: 90 },
  { title: t('pm.common.hourlyCost'), key: 'costRate', width: 120, align: 'right' },
  { title: t('pm.detail.approvedHours'), key: 'hours', width: 130 },
  { title: t('pm.detail.period'), key: 'dates', width: 200 },
  { title: '', key: 'action', width: 90, fixed: 'right' }
]
const timesheetColumns = [
  { title: t('common.field.date'), dataIndex: 'workDate', width: 110 },
  { title: t('pm.common.member'), dataIndex: 'userName', width: 100 },
  { title: t('pm.common.task'), dataIndex: 'taskName', width: 160, ellipsis: true },
  { title: t('pm.common.hours'), dataIndex: 'hours', width: 70 },
  { title: t('pm.common.workContent'), dataIndex: 'content', ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('pm.common.laborCost'), key: 'cost', width: 110, align: 'right' }
]
const costColumns = [
  { title: t('common.field.date'), dataIndex: 'costDate', width: 110 },
  { title: t('common.field.type'), key: 'costType', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'description', ellipsis: true },
  { title: t('pm.common.enteredBy'), dataIndex: 'createByName', width: 110 }
]
const billingColumns = [
  { title: t('pm.common.billingItem'), dataIndex: 'name', width: 140 },
  { title: t('pm.common.milestone'), dataIndex: 'taskName', width: 140, ellipsis: true },
  { title: t('pm.common.planDate'), dataIndex: 'planDate', width: 120 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('pm.common.billReceipt'), key: 'bill', width: 220 }
]

async function load() {
  loading.value = true
  try {
    const id = props.projectId
    const [p, a, m, t, b] = await Promise.all([getProject(id), getAccounting(id), listMembers(id), taskTree(id), listBillings(id)])
    project.value = p
    acc.value = a
    members.value = m
    tasks.value = t
    billings.value = b
  } finally {
    loading.value = false
  }
  if (canTimesheet.value) loadTimesheets(1)
  if (canCost.value) loadCosts(1)
}

async function loadTimesheets(current) {
  timesheets.loading = true
  try {
    const res = await pageTimesheet({ projectId: props.projectId, current, size: timesheets.pagination.pageSize })
    timesheets.records = res.records
    Object.assign(timesheets.pagination, { current, total: res.total })
  } finally {
    timesheets.loading = false
  }
}

async function loadCosts(current) {
  costs.loading = true
  try {
    const res = await pageCost({ projectId: props.projectId, current, size: costs.pagination.pageSize })
    costs.records = res.records
    Object.assign(costs.pagination, { current, total: res.total })
  } finally {
    costs.loading = false
  }
}

watch(
  () => [props.open, props.projectId],
  ([open]) => {
    if (open && props.projectId) {
      tab.value = 'member'
      load()
    }
  },
  { immediate: true }
)

// ---------------------------------------------------------------- 成员

const memberOpen = ref(false)
const memberSaving = ref(false)
const memberRef = ref()
const memberForm = ref({})

function openMember(record) {
  memberForm.value = record
    ? { id: record.id, userId: record.userId, userName: record.userName, role: record.role, allocation: record.allocation,
        costRate: Number(record.costRate), joinDate: record.joinDate, leaveDate: record.leaveDate }
    : { userId: undefined, role: 'DEVELOPER', allocation: 100, costRate: undefined, joinDate: undefined, leaveDate: undefined }
  memberOpen.value = true
}

async function saveMember() {
  try {
    await memberRef.value.validate()
  } catch {
    return
  }
  memberSaving.value = true
  try {
    const { id, userName, ...data } = memberForm.value
    const payload = { ...data, costRate: data.costRate ?? null, joinDate: data.joinDate || null, leaveDate: data.leaveDate || null }
    id ? await updateMember(props.projectId, id, payload) : await addMember(props.projectId, payload)
    message.success(t('common.msg.saveSuccess'))
    memberOpen.value = false
    members.value = await listMembers(props.projectId)
    emit('changed')
  } finally {
    memberSaving.value = false
  }
}

async function removeMember(record) {
  await deleteMember(props.projectId, record.id)
  message.success(t('pm.common.removed'))
  members.value = await listMembers(props.projectId)
  emit('changed')
}
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-hover);
  height: 100%;
}
.stat-title {
  color: var(--app-text-3);
  font-size: 12px;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 18px;
  font-weight: 600;
}
.stat-value.danger {
  color: #f5222d;
}
.tab-toolbar {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: 400;
}
.full {
  width: 100%;
}
</style>
