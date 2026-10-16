<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="项目" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="编号或名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.pm_project_status)" />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="query.type" placeholder="全部" allow-clear :options="toOptions(dict.pm_project_type)" />
        </a-form-item>
        <a-form-item label="范围" name="mine">
          <a-checkbox v-model:checked="query.mine">只看我负责的</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'pm:project:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新建项目
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
          <template v-else-if="column.key === 'customer'">{{ record.customerName || '内部项目' }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="pm_project_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Number(record.progress)" size="small" :status="record.status === 'FINISHED' ? 'success' : 'normal'" />
          </template>
          <template v-else-if="column.key === 'dates'">
            <div>{{ record.planStart }} ~ {{ record.planEnd }}</div>
            <div v-if="record.actualStart" class="sub-text">实际 {{ record.actualStart }} ~ {{ record.actualEnd || '' }}</div>
          </template>
          <template v-else-if="column.key === 'contract'">{{ formatMoney(record.contractAmount) }}</template>
          <template v-else-if="column.key === 'cost'">
            <div :class="{ danger: overBudget(record) }">{{ formatMoney(actualCost(record)) }}</div>
            <div class="sub-text">预算 {{ Number(record.budgetAmount) ? formatMoney(record.budgetAmount) : '未编制' }}</div>
          </template>
          <template v-else-if="column.key === 'tasks'">
            <div>{{ record.doneTaskCount }} / {{ record.taskCount }}</div>
            <div v-if="record.overdueTaskCount" class="danger sub-danger">逾期 {{ record.overdueTaskCount }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>详情
              </a-button>
              <a-button v-if="record.status !== 'CLOSED'" v-perm="'pm:project:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <span v-for="a in STATUS_ACTIONS[record.status]" :key="a.status" v-perm="'pm:project:edit'">
                <a-popconfirm :title="`${a.label}项目「${record.name}」？${a.confirm}`" @confirm="changeStatus(record, a)">
                  <a-button type="link" size="small">{{ a.label }}</a-button>
                </a-popconfirm>
              </span>
              <span v-if="record.status === 'PLANNING'" v-perm="'pm:project:remove'">
                <a-popconfirm :title="`确定删除项目「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新建项目" edit-title="编辑项目" width="min(720px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="10">
            <a-form-item label="项目编号" name="code">
              <a-input v-model:value="form.code" :maxlength="32" placeholder="如 PRJ-2026-001" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="14">
            <a-form-item label="项目名称" name="name">
              <a-input v-model:value="form.name" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="项目类型" name="type">
              <a-select v-model:value="form.type" :options="toOptions(dict.pm_project_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="优先级" name="priority">
              <a-select v-model:value="form.priority" :options="toOptions(dict.pm_priority)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="项目经理" name="managerId" extra="不填为当前用户，自动加入项目成员">
              <IdentityUserSelect v-model:value="form.managerId" :label="form.managerName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="客户" name="customerId" extra="对外项目填写，开票时生成该客户的应收单">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="计划开始" name="planStart">
              <a-date-picker v-model:value="form.planStart" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="计划完成" name="planEnd">
              <a-date-picker v-model:value="form.planEnd" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item label="合同金额（含税）" name="contractAmount">
              <a-input-number v-model:value="form.contractAmount" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="12">
            <a-form-item label="税率" name="taxRate">
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.salesOrderId" :span="24">
            <a-form-item label="关联销售订单">
              <a-input :value="form.salesOrderNo || form.salesOrderId" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item name="budgetControl" extra="开启后，费用、领料超过成本预算（有分类预算时按分类）时不能录入">
              <a-checkbox :checked="form.budgetControl === 1" @change="(e) => (form.budgetControl = e.target.checked ? 1 : 0)">超预算控制</a-checkbox>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="项目说明" name="description">
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

const dict = useDict('pm_project_status', 'pm_project_type', 'pm_priority')

const columns = [
  { title: '项目', key: 'name', width: 220, fixed: 'left' },
  { title: '类型', key: 'type', width: 100, dict: 'pm_project_type' },
  { title: '客户', key: 'customer', width: 180, ellipsis: true },
  { title: '项目经理', dataIndex: 'managerName', width: 100 },
  { title: '状态', key: 'status', width: 90, dict: 'pm_project_status' },
  { title: '进度', key: 'progress', width: 150 },
  { title: '计划日期', key: 'dates', width: 210 },
  { title: '合同额', key: 'contract', width: 130, align: 'right' },
  { title: '实际成本 / 预算', key: 'cost', width: 150, align: 'right' },
  { title: '任务', key: 'tasks', width: 90 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' }
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
    { required: true, message: '请输入项目编号' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '只能包含字母、数字、下划线和短横线' }
  ],
  name: [{ required: true, whitespace: true, message: '请输入项目名称' }],
  type: [{ required: true, message: '请选择项目类型' }],
  planStart: [{ required: true, message: '请选择计划开始日期' }],
  planEnd: [{ required: true, message: '请选择计划完成日期' }]
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
  message.success(submitted ? '已提交立项审批，审批通过后项目进入进行中' : `已${action.label}`)
  load()
}

async function remove(id) {
  await deleteProject(id)
  message.success('删除成功')
  load()
}
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
