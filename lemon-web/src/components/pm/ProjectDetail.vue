<template>
  <SubPage :open="open" :title="project ? `${project.code} ${project.name}` : '项目详情'" width="min(1100px, 100vw)" destroy-on-close
            @close="emit('update:open', false)">
    <a-spin :spinning="loading">
      <template v-if="project">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item label="类型"><DictTag type="pm_project_type" :value="project.type" /></a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="pm_project_status" :value="project.status" /></a-descriptions-item>
          <a-descriptions-item label="优先级"><DictTag type="pm_priority" :value="project.priority" /></a-descriptions-item>
          <a-descriptions-item label="客户">{{ project.customerName || '内部项目' }}</a-descriptions-item>
          <a-descriptions-item v-if="project.salesOrderNo" label="销售订单">{{ project.salesOrderNo }}</a-descriptions-item>
          <a-descriptions-item v-if="project.budgetControl === 1" label="预算控制">超预算不能录入费用、领料</a-descriptions-item>
          <a-descriptions-item label="项目经理">{{ project.managerName }}</a-descriptions-item>
          <a-descriptions-item label="负责部门">{{ project.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="计划日期">{{ project.planStart }} ~ {{ project.planEnd }}</a-descriptions-item>
          <a-descriptions-item label="实际日期">{{ project.actualStart || '-' }} ~ {{ project.actualEnd || '-' }}</a-descriptions-item>
          <a-descriptions-item label="合同金额">{{ formatMoney(project.contractAmount) }}（税率 {{ project.taxRate }}%）</a-descriptions-item>
          <a-descriptions-item v-if="project.description" label="说明" :span="3">{{ project.description }}</a-descriptions-item>
        </a-descriptions>

        <!-- 概况：进度、成本、收入、挣值 -->
        <a-row v-if="acc" :gutter="[16, 16]" class="block">
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">完工进度 / 计划进度</div>
              <a-progress :percent="Number(acc.progress)" :success="{ percent: 0 }" size="small" />
              <a-progress :percent="Number(acc.plannedRate)" size="small" stroke-color="#d9d9d9" :format="(p) => `计划 ${p}%`" />
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">实际成本 / 预算</div>
              <div class="stat-value" :class="{ danger: overBudget }">
                {{ formatMoney(acc.actualCost) }} <span class="sub-text">/ {{ formatMoney(acc.budget) }}</span>
              </div>
              <div class="sub-text">人工 {{ formatMoney(acc.laborCost) }} · 费用 {{ formatMoney(acc.expenseCost) }}</div>
            </div>
          </a-col>
          <a-col :xs="24" :md="8">
            <div class="stat">
              <div class="stat-title">已确认收入 / 毛利</div>
              <div class="stat-value">{{ formatMoney(acc.revenue) }} <span class="sub-text">/ {{ formatMoney(acc.grossProfit) }}</span></div>
              <div class="sub-text">
                SPI <a-tag :color="indexTag(acc.spi).color" :bordered="false">{{ indexTag(acc.spi).text }}</a-tag>
                CPI <a-tag :color="indexTag(acc.cpi).color" :bordered="false">{{ indexTag(acc.cpi).text }}</a-tag>
                已收款 {{ formatMoney(acc.received) }}
              </div>
            </div>
          </a-col>
        </a-row>

        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="member" :tab="`成员（${members.length}）`">
            <div class="tab-toolbar">
              <a-button v-perm="'pm:member:edit'" type="primary" size="small" :disabled="project.status === 'CLOSED'" @click="openMember()">
                <template #icon><PlusOutlined /></template>添加成员
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
                <template v-else-if="column.key === 'hours'">{{ Number(record.approvedHours || 0) }} 小时</template>
                <template v-else-if="column.key === 'dates'">{{ record.joinDate || '-' }} ~ {{ record.leaveDate || '-' }}</template>
                <template v-else-if="column.key === 'action'">
                  <span v-perm="'pm:member:edit'" class="table-actions">
                    <a-button type="link" size="small" @click="openMember(record)"><EditOutlined /></a-button>
                    <a-popconfirm v-if="record.role !== 'MANAGER'" :title="`确定移除成员「${record.userName}」？`" @confirm="removeMember(record)">
                      <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="plan" tab="计划">
            <GanttChart :tasks="tasks" max-height="480px" />
          </a-tab-pane>
          <a-tab-pane v-if="canTimesheet" key="timesheet" tab="工时">
            <a-table row-key="id" size="small" :columns="timesheetColumns" :data-source="timesheets.records" :loading="timesheets.loading"
                     :pagination="timesheets.pagination" :scroll="{ x: 800 }" @change="(p) => loadTimesheets(p.current)">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="pm_timesheet_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'cost'">{{ record.status === 'APPROVED' ? formatMoney(record.cost) : '-' }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="canCost" key="cost" tab="费用">
            <a-table row-key="id" size="small" :columns="costColumns" :data-source="costs.records" :loading="costs.loading"
                     :pagination="costs.pagination" :scroll="{ x: 700 }" @change="(p) => loadCosts(p.current)">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'costType'"><DictTag type="pm_cost_type" :value="record.costType" /></template>
                <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="billing" tab="收款">
            <a-table row-key="id" size="small" :columns="billingColumns" :data-source="billings" :pagination="false" :scroll="{ x: 700 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'status'"><DictTag type="pm_billing_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'bill'">
                  <template v-if="record.billNo">
                    <div>{{ record.billNo }}</div>
                    <div class="sub-text">{{ record.billDate }} · 已收 {{ formatMoney(record.settledAmount) }}</div>
                  </template>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="attachment" tab="附件">
            <AttachmentPanel biz-type="pm_project" :biz-id="projectId" :editable="userStore.hasPerm('pm:project:edit')" />
          </a-tab-pane>
          <a-tab-pane key="history" tab="变更记录">
            <ChangeLogPanel biz-type="pm_project" :biz-id="projectId" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <!-- 成员 -->
    <a-modal v-model:open="memberOpen" :title="memberForm.id ? '编辑成员' : '添加成员'" :confirm-loading="memberSaving" destroy-on-close
             @ok="saveMember">
      <a-form ref="memberRef" :model="memberForm" layout="vertical">
        <a-form-item label="成员" name="userId" :rules="[{ required: true, message: '请选择成员' }]">
          <IdentityUserSelect v-model:value="memberForm.userId" :label="memberForm.userName" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="项目角色" name="role" :rules="[{ required: true, message: '请选择项目角色' }]">
              <a-select v-model:value="memberForm.role" :options="toOptions(dict.pm_member_role)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="投入比例" name="allocation" :rules="[{ required: true, message: '请填写投入比例' }]">
              <a-input-number v-model:value="memberForm.allocation" :min="1" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="小时成本" name="costRate" extra="审批工时时按此计算人工成本">
              <a-input-number v-model:value="memberForm.costRate" :min="0" :precision="2" addon-after="元/小时"
                              placeholder="不填按薪资档案自动计算" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="加入日期" name="joinDate">
              <a-date-picker v-model:value="memberForm.joinDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="退出日期" name="leaveDate">
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
  { title: '成员', key: 'user', width: 180 },
  { title: '角色', key: 'role', width: 100 },
  { title: '投入', key: 'allocation', width: 80 },
  { title: '小时成本', key: 'costRate', width: 110, align: 'right' },
  { title: '已审批工时', key: 'hours', width: 110 },
  { title: '参与期间', key: 'dates', width: 200 },
  { title: '', key: 'action', width: 90, fixed: 'right' }
]
const timesheetColumns = [
  { title: '日期', dataIndex: 'workDate', width: 110 },
  { title: '成员', dataIndex: 'userName', width: 100 },
  { title: '任务', dataIndex: 'taskName', width: 160, ellipsis: true },
  { title: '工时', dataIndex: 'hours', width: 70 },
  { title: '工作内容', dataIndex: 'content', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '人工成本', key: 'cost', width: 110, align: 'right' }
]
const costColumns = [
  { title: '日期', dataIndex: 'costDate', width: 110 },
  { title: '类型', key: 'costType', width: 90 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '说明', dataIndex: 'description', ellipsis: true },
  { title: '录入人', dataIndex: 'createByName', width: 100 }
]
const billingColumns = [
  { title: '款项', dataIndex: 'name', width: 140 },
  { title: '里程碑', dataIndex: 'taskName', width: 140, ellipsis: true },
  { title: '计划日期', dataIndex: 'planDate', width: 110 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '应收单 / 收款', key: 'bill', width: 220 }
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
    message.success('保存成功')
    memberOpen.value = false
    members.value = await listMembers(props.projectId)
    emit('changed')
  } finally {
    memberSaving.value = false
  }
}

async function removeMember(record) {
  await deleteMember(props.projectId, record.id)
  message.success('已移除')
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
