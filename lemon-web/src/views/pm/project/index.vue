<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('pm.common.project')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('pm.common.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pm_project_status)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.type')" name="type">
          <a-select v-model:value="query.type" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.pm_project_type)" />
        </a-form-item>
        <a-form-item :label="$t('pm.project.scope')" name="mine">
          <a-checkbox v-model:checked="query.mine">{{ $t('pm.project.onlyMine') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'pm:project:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('pm.project.create') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1500, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub-text">{{ record.code }} · <DictTag type="pm_priority" :value="record.priority" /></div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="pm_project_type" :value="record.type" /></template>
          <template v-else-if="column.key === 'customer'">{{ record.customerName || $t('pm.common.internal') }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="pm_project_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress)" size="small" :status="record.status === ProjectStatus.FINISHED ? 'success' : 'normal'" />
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>{{ record.planStart }} ~ {{ record.planEnd }}</div>
            <div v-if="record.actualStart" class="sub-text">{{ $t('pm.common.actual') }} {{ record.actualStart }} ~ {{ record.actualEnd || '' }}</div>
          </template>
          <template v-else-if="column.key === 'contract'">{{ formatMoney(record.contractAmount) }}</template>
          <template v-else-if="column.key === 'cost'">
            <div :class="{ danger: overBudget(record) }">{{ formatMoney(actualCost(record)) }}</div>
            <div class="sub-text">{{ $t('pm.common.budget') }} {{ Number(record.budgetAmount) ? formatMoney(record.budgetAmount) : $t('pm.project.noBudget') }}</div>
          </template>
          <template v-else-if="column.key === 'tasks'">
            <div>{{ record.doneTaskCount }} / {{ record.taskCount }}</div>
            <div v-if="record.overdueTaskCount" class="danger sub-danger">{{ $t('pm.common.overdue') }} {{ record.overdueTaskCount }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.detail') }}
              </a-button>
              <a-button v-if="record.status !== ProjectStatus.CLOSED" v-perm="'pm:project:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-for="a in STATUS_ACTIONS[record.status]" :key="a.status" v-perm="'pm:project:edit'">
                <a-popconfirm :title="$t('pm.project.confirmStatus', { action: a.label, name: record.name, tip: a.confirm })" @confirm="changeStatus(record, a)">
                  <a-button type="link" size="small">{{ a.label }}</a-button>
                </a-popconfirm>
              </span>
              <span v-if="record.status === ProjectStatus.PLANNING" v-perm="'pm:project:remove'">
                <a-popconfirm :title="$t('pm.project.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('pm.project.create')" :edit-title="$t('pm.project.edit')" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('pm.project.code')" name="code">
              <a-input v-model:value="form.code" :maxlength="32" :placeholder="$t('pm.project.codePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="14">
            <a-form-item :label="$t('pm.project.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('pm.project.type')" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.pm_project_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('pm.common.priority')" name="priority">
              <a-select v-model:value="form.priority" :options="toOptions(dict.pm_priority)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('pm.common.manager')" name="managerId" :extra="$t('pm.project.managerTip')">
              <IdentityUserSelect v-model:value="form.managerId" :label="form.managerName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('pm.common.customer')" name="customerId" :extra="$t('pm.project.customerTip')">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('pm.common.planStart')" name="planStart">
              <a-date-picker v-model:value="form.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('pm.common.planEnd')" name="planEnd">
              <a-date-picker v-model:value="form.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item :label="$t('pm.project.contractAmountTax')" name="contractAmount">
              <a-input-number v-model:value="form.contractAmount" :min="0" :precision="2" :addon-after="$t('pm.common.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item :label="$t('pm.common.taxRate')" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.salesOrderId" :span="24">
            <a-form-item :label="$t('pm.project.salesOrder')">
              <a-input :value="form.salesOrderNo || form.salesOrderId" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="budgetControl" :extra="$t('pm.project.budgetControlTip')">
              <a-checkbox :checked="form.budgetControl === 1" @change="(e) => (form.budgetControl = e.target.checked ? 1 : 0)">{{ $t('pm.project.budgetControl') }}</a-checkbox>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('pm.project.description')" name="description">
              <a-textarea v-model:value="form.description" :rows="3" :maxlength="1000" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <ProjectDetail v-model:open="detailOpen" :project-id="detailId" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import ProjectDetail from '@/components/pm/ProjectDetail.vue'
import { addProject, changeProjectStatus, deleteProject, getProject, pageProject, updateProject } from '@/api/pm'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { STATUS_ACTIONS, actualCost, num } from '@/components/pm/pm'
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

import { ProjectStatus } from '@/constants/enums'
const dict = useDict('pm_project_status', 'pm_project_type', 'pm_priority')

const columns = [
  { title: t('pm.common.project'), key: 'name', width: 220, fixed: 'left' },
  { title: t('common.field.type'), key: 'type', width: 110, dict: 'pm_project_type' },
  { title: t('pm.common.customer'), key: 'customer', width: 180, ellipsis: true },
  { title: t('pm.common.manager'), dataIndex: 'managerName', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'pm_project_status' },
  { title: t('pm.common.progress'), key: 'progress', width: 150 },
  { title: t('pm.common.planDates'), key: 'dates', width: 210 },
  { title: t('pm.project.contract'), key: 'contract', width: 130, align: 'right' },
  { title: t('pm.project.costBudget'), key: 'cost', width: 170, align: 'right' },
  { title: t('pm.common.tasks'), key: 'tasks', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 280, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'pm-project')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, type: undefined, mine: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageProject, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
const route = useRoute()
const router = useRouter()
onMounted(() => {
  load()
  // 从销售订单、商机「创建项目」跳转过来时带入默认值
  const q = route.query
  if (q.create) {
    const num = (v) => (v == null || v === '' ? undefined : Number(v))
    drawer.openCreate({
      name: q.name || undefined,
      customerId: num(q.customerId),
      opportunityId: num(q.opportunityId),
      salesOrderId: num(q.salesOrderId),
      salesOrderNo: q.salesOrderNo || undefined,
      contractAmount: num(q.contractAmount) ?? 0
    })
    router.replace({ query: {} })
  }
})

const overBudget = (p) => num(p.budgetAmount) > 0 && actualCost(p) > num(p.budgetAmount)

const rules = {
  code: [
    { required: true, message: t('common.rule.required', { name: t('pm.project.code') }) },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('pm.project.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('pm.project.name') }) }],
  type: [{ required: true, message: t('common.rule.requiredSelect', { name: t('pm.project.type') }) }],
  planStart: [{ required: true, message: t('common.rule.requiredSelect', { name: t('pm.common.planStartDate') }) }],
  planEnd: [{ required: true, message: t('common.rule.requiredSelect', { name: t('pm.common.planEndDate') }) }]
}
const FIELDS = ['code', 'name', 'type', 'customerId', 'opportunityId', 'salesOrderId', 'managerId', 'priority', 'planStart', 'planEnd',
  'contractAmount', 'taxRate', 'description', 'budgetControl']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), type: 'DELIVERY', priority: 'MEDIUM',
    taxRate: 6, contractAmount: 0, budgetControl: 0 }),
  fetch: async (record) => {
    const p = await getProject(record.id)
    return { ...p, contractAmount: Number(p.contractAmount), taxRate: Number(p.taxRate) }
  },
  create: addProject,
  update: updateProject,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}

async function changeStatus(record, action) {
  const submitted = await changeProjectStatus(record.id, action.status)
  message.success(submitted ? t('pm.project.submittedApproval') : t('pm.project.statusDone', { action: action.label }))
  load()
}

const remove = useAction(deleteProject, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #f5222d;
}
.sub-danger {
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
