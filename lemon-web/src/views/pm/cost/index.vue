<template>
  <div class="cost-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <ProjectSelect v-model:value="projectId" :allow-clear="false" class="project-select" @update:value="onProject" />
        <template v-if="project">
          <DictTag type="pm_project_status" :value="project.status" />
          <span class="sub-text">
            {{ $t('pm.acc.costBudget') }} {{ formatMoney(project.budgetAmount) }} · {{ $t('pm.common.actual') }} <b :class="{ danger: overBudget }">{{ formatMoney(totalActual) }}</b>
            · {{ $t('pm.cost.execRate') }} {{ totalRate }}
          </span>
        </template>
        <div class="actions">
          <a-button v-if="project" v-perm="'pm:cost:budget'" :disabled="project.status === 'CLOSED'" @click="openBudget">
            <template #icon><EditOutlined /></template>{{ $t('pm.cost.editBudget') }}
          </a-button>
          <a-button v-if="project" v-perm="'pm:cost:issue'" :disabled="project.status !== 'ACTIVE'" @click="issueOpen = true">
            <template #icon><ExportOutlined /></template>{{ $t('pm.issue.title') }}
          </a-button>
          <a-button v-if="project" v-perm="'pm:cost:add'" type="primary" :disabled="!openProject" @click="openCost">
            <template #icon><PlusOutlined /></template>{{ $t('pm.cost.addCost') }}
          </a-button>
        </div>
      </div>
    </a-card>

    <a-empty v-if="!projectId" class="page-card empty-card" :description="$t('pm.common.pleaseSelectProject')" />
    <template v-else>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('pm.cost.budgetActual')">
            <a-table row-key="costType" size="small" :columns="budgetColumns" :data-source="budgets" :pagination="false" :loading="loading">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'costType'"><DictTag type="pm_cost_type" :value="record.costType" /></template>
                <template v-else-if="column.key === 'amount'">{{ Number(record.amount) ? formatMoney(record.amount) : '-' }}</template>
                <template v-else-if="column.key === 'actual'">{{ formatMoney(record.actual) }}</template>
                <template v-else-if="column.key === 'diff'">
                  <span v-if="Number(record.amount)" :class="{ danger: diff(record) < 0 }">{{ formatMoney(diff(record)) }}</span>
                  <span v-else class="sub-text">-</span>
                </template>
                <template v-else-if="column.key === 'rate'">
                  <a-progress v-if="Number(record.amount)" :percent="Math.min(rate(record), 100)" size="small"
                              :status="rate(record) > 100 ? 'exception' : 'normal'" :format="() => `${rate(record)}%`" />
                  <span v-else-if="Number(record.actual)" class="danger">{{ $t('pm.cost.noBudget') }}</span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card :bordered="false" class="page-card" :title="$t('pm.cost.budgetActualChart')">
            <EChart :option="budgetChart" height="260px" />
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" class="page-card" :title="$t('pm.acc.monthlyCost')">
        <EChart v-if="monthly.length" :option="monthlyChart" height="260px" />
        <a-empty v-else :description="$t('pm.acc.noCost')" />
      </a-card>

      <a-card :bordered="false" class="page-card" :title="$t('pm.cost.costDetail')">
        <div class="filters">
          <a-select v-model:value="query.costType" class="w140" :placeholder="$t('pm.cost.allTypes')" allow-clear :options="toOptions(dict.pm_cost_type)" @change="search" />
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="search" />
          <span class="sub-text">{{ $t('pm.cost.laborNote') }}</span>
        </div>
        <a-table row-key="id" size="middle" :columns="costColumns" :data-source="list" :loading="tableLoading" :pagination="pagination"
                 :scroll="{ x: 900 }" @change="onTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'costType'"><DictTag type="pm_cost_type" :value="record.costType" /></template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'source'">
              <a-tag v-if="record.source === 'ISSUE'" color="cyan" :bordered="false">{{ $t('pm.cost.issue') }} {{ record.sourceNo }}</a-tag>
              <span v-else class="sub-text">{{ $t('pm.cost.manual') }}</span>
              <DictTag v-if="record.status && record.status !== 'APPROVED'" type="sys_approve_status" :value="record.status" />
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-if="record.source === 'MANUAL'" v-perm="'pm:cost:remove'">
                <a-popconfirm :title="$t('pm.cost.confirmDelete')" @confirm="removeCost(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </a-card>
    </template>

    <!-- 编制预算 -->
    <a-modal v-model:open="budgetOpen" :title="$t('pm.cost.budgetTitle')" :confirm-loading="budgetSaving" destroy-on-close @ok="saveBudget">
      <a-form layout="horizontal" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
        <a-form-item v-for="b in budgetForm" :key="b.costType" :label="typeLabel(b.costType)">
          <a-input-number v-model:value="b.amount" :min="0" :precision="2" :addon-after="$t('pm.common.yuan')" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.total')">
          <b>{{ formatMoney(budgetForm.reduce((s, b) => s + (Number(b.amount) || 0), 0)) }}</b>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 录入费用 -->
    <a-modal v-model:open="costOpen" :title="$t('pm.cost.addCost')" :confirm-loading="costSaving" destroy-on-close @ok="saveCost">
      <a-form ref="costRef" :model="costForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('pm.cost.costType')" name="costType" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('pm.cost.costType') }) }]">
              <a-select v-model:value="costForm.costType" :options="manualTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.cost.costDate')" name="costDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('common.field.date') }) }]">
              <a-date-picker v-model:value="costForm.costDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('pm.cost.amountNet')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('common.field.amount') }) }]">
              <a-input-number v-model:value="costForm.amount" :min="0.01" :precision="2" :addon-after="$t('pm.common.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.description')" name="description">
              <a-input v-model:value="costForm.description" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <IssueForm v-model:open="issueOpen" :project="project" @success="refresh" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, ExportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import IssueForm from '@/components/pm/IssueForm.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import { accountingMonthly, addCost, deleteCost, getProject, listBudgets, pageCost, projectOptions, saveBudgets } from '@/api/pm'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { findDict, toOptions } from '@/utils/dict'
import { load as loadStorage, save as saveStorage } from '@/utils/storage'
import { formatMoney } from '@/components/scm/scm'
import { shortMoney, periodLabel } from '@/components/charts/charts'
import { MANUAL_COST_TYPES, today } from '@/components/pm/pm'
import { t } from '@/locales'

const STORAGE_KEY = 'pm-plan-project'
const dict = useDict('pm_cost_type', 'sys_approve_status')

const projectId = ref(loadStorage(STORAGE_KEY, undefined) || undefined)
const project = ref(null)
const budgets = ref([])
const monthly = ref([])
const loading = ref(false)

const openProject = computed(() => project.value && !['FINISHED', 'CLOSED'].includes(project.value.status))
const typeLabel = (v) => findDict(dict.pm_cost_type, v)?.label || v
// 人工成本来自已审批的工时，不能手工录入：显示为禁用并说明原因
const manualTypeOptions = computed(() =>
  toOptions(dict.pm_cost_type).map((o) => (MANUAL_COST_TYPES.includes(o.value) ? o : { ...o, label: t('pm.cost.laborFromTimesheet', { label: o.label }), disabled: true }))
)

const totalActual = computed(() => budgets.value.reduce((s, b) => s + Number(b.actual || 0), 0))
const overBudget = computed(() => Number(project.value?.budgetAmount) > 0 && totalActual.value > Number(project.value.budgetAmount))
const totalRate = computed(() => {
  const b = Number(project.value?.budgetAmount)
  return b > 0 ? `${((totalActual.value * 100) / b).toFixed(1)}%` : '-'
})

const diff = (r) => Number(r.amount) - Number(r.actual)
const rate = (r) => Math.round((Number(r.actual) * 1000) / Number(r.amount)) / 10

const budgetColumns = [
  { title: t('pm.cost.costType'), key: 'costType', width: 110 },
  { title: t('pm.common.budget'), key: 'amount', align: 'right' },
  { title: t('pm.common.actual'), key: 'actual', align: 'right' },
  { title: t('pm.cost.diff'), key: 'diff', align: 'right' },
  { title: t('pm.cost.execRate'), key: 'rate', width: 150 }
]

const budgetChart = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: [t('pm.common.budget'), t('pm.common.actual')], top: 0 },
  grid: { left: 60, right: 16, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: budgets.value.map((b) => typeLabel(b.costType)) },
  yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
  series: [
    { name: t('pm.common.budget'), type: 'bar', barMaxWidth: 24, data: budgets.value.map((b) => Number(b.amount)) },
    {
      name: t('pm.common.actual'),
      type: 'bar',
      barMaxWidth: 24,
      data: budgets.value.map((b) => ({
        value: Number(b.actual),
        itemStyle: Number(b.amount) > 0 && Number(b.actual) > Number(b.amount) ? { color: '#f5222d' } : undefined
      }))
    }
  ]
}))

const monthlyChart = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: [t('pm.common.labor'), t('pm.common.expense')], top: 0 },
  grid: { left: 60, right: 16, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: monthly.value.map((m) => periodLabel(m.period)) },
  yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
  series: [
    { name: t('pm.common.labor'), type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => Number(m.labor)) },
    { name: t('pm.common.expense'), type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => Number(m.expense)) }
  ]
}))

// 费用列表
const query = reactive({ projectId, costType: undefined, range: undefined })
const { list, loading: tableLoading, pagination, load: loadCosts, search, onTableChange } = useTable(pageCost, query, {
  params: ({ range, ...q }) => ({ ...q, beginDate: range?.[0], endDate: range?.[1] })
})

const costColumns = [
  { title: t('common.field.date'), dataIndex: 'costDate', width: 110 },
  { title: t('common.field.type'), key: 'costType', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'description', ellipsis: true },
  { title: t('pm.cost.source'), key: 'source', width: 200 },
  { title: t('pm.common.enteredBy'), dataIndex: 'createByName', width: 110 },
  { title: '', key: 'action', width: 60, fixed: 'right' }
]

async function refresh() {
  if (!projectId.value) return
  loading.value = true
  try {
    const [p, b, m] = await Promise.all([getProject(projectId.value), listBudgets(projectId.value), accountingMonthly(projectId.value)])
    project.value = p
    budgets.value = b
    monthly.value = m
  } finally {
    loading.value = false
  }
  search()
}

function onProject(id) {
  saveStorage(STORAGE_KEY, id)
  refresh()
}
onMounted(async () => {
  // 没有记住的项目时默认选中第一个进行中的项目
  if (!projectId.value) {
    const list = await projectOptions().catch(() => [])
    projectId.value = (list.find((p) => p.status === 'ACTIVE') || list[0])?.id
  }
  refresh()
})

// 预算
const budgetOpen = ref(false)
const budgetSaving = ref(false)
const budgetForm = ref([])

function openBudget() {
  budgetForm.value = budgets.value.map((b) => ({ costType: b.costType, amount: Number(b.amount) || undefined }))
  budgetOpen.value = true
}

async function saveBudget() {
  budgetSaving.value = true
  try {
    await saveBudgets(projectId.value, budgetForm.value.map((b) => ({ costType: b.costType, amount: b.amount || 0 })))
    message.success(t('pm.cost.budgetSaved'))
    budgetOpen.value = false
    refresh()
  } finally {
    budgetSaving.value = false
  }
}

// 费用
const costOpen = ref(false)
const costSaving = ref(false)
const costRef = ref()
const costForm = ref({})

function openCost() {
  costForm.value = { costType: 'TRAVEL', costDate: today(), amount: undefined, description: undefined }
  costOpen.value = true
}

async function saveCost() {
  try {
    await costRef.value.validate()
  } catch {
    return
  }
  costSaving.value = true
  try {
    await addCost({ ...costForm.value, projectId: projectId.value, description: costForm.value.description || null })
    message.success(t('pm.cost.costSaved'))
    costOpen.value = false
    refresh()
  } finally {
    costSaving.value = false
  }
}

async function removeCost(id) {
  await deleteCost(id)
  message.success(t('common.msg.deleteSuccess'))
  refresh()
}

const issueOpen = ref(false)
</script>

<style scoped>
.cost-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.cost-page > .page-card + .page-card {
  margin-top: 0;
}
.cost-page :deep(.ant-row) .page-card {
  height: 100%;
}
.empty-card {
  padding: 48px 0;
  background: var(--app-card);
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.project-select {
  width: 300px;
}
.actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.w140 {
  width: 140px;
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
</style>
