<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.contract.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_contract_status)" />
        </a-form-item>
        <a-form-item :label="$t('hr.contract.contractType')" name="contractType">
          <a-select v-model:value="query.contractType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_contract_type)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'hr:contract:edit'" type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>{{ $t('hr.contract.newContract') }}
        </a-button>
        <a-radio-group :value="query.expireDays === 30 ? 'expiring' : 'all'" button-style="solid" @change="onQuickFilter">
          <a-radio-button value="all">{{ $t('common.status.all') }}</a-radio-button>
          <a-radio-button value="expiring">{{ $t('hr.contract.expiring') }}</a-radio-button>
        </a-radio-group>
        <a-alert
          type="info"
          show-icon
          :message="$t('hr.contract.tip')"
        />
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
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }} <DictTag type="hr_emp_status" :value="record.empStatus" /></div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'contractType'">
            <DictTag type="hr_contract_type" :value="record.contractType" />
          </template>
          <template v-else-if="column.key === 'term'">
            {{ record.startDate }} ~ {{ record.endDate || $t('hr.contract.openEnded') }}
            <div v-if="record.probationMonths" class="sub-text">{{ $t('hr.contract.probationN', { n: record.probationMonths }) }}</div>
          </template>
          <template v-else-if="column.key === 'daysLeft'">
            <span v-if="record.daysLeft == null || record.status !== LaborContractStatus.ACTIVE" class="sub-text">-</span>
            <span v-else :class="daysClass(record.daysLeft)">{{ daysText(record.daysLeft) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_contract_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'terminate'">
            <template v-if="record.terminateDate">
              <div>{{ record.terminateDate }}</div>
              <div class="sub-text">{{ record.terminateReason || '-' }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-if="record.status === LaborContractStatus.ACTIVE || record.status === LaborContractStatus.EXPIRED" v-perm="'hr:contract:edit'" class="table-actions">
              <a-button v-if="record.status === LaborContractStatus.ACTIVE" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <a-button type="link" size="small" @click="openRenew(record)">
                <template #icon><SyncOutlined /></template>{{ $t('hr.contract.renew') }}
              </a-button>
              <a-button v-if="record.status === LaborContractStatus.ACTIVE" type="link" danger size="small" @click="openTerminate(record)">{{ $t('hr.contract.terminate') }}</a-button>
              <a-popconfirm v-if="record.status === LaborContractStatus.ACTIVE" :title="$t('hr.contract.confirmDelete', { no: record.contractNo })" @confirm="remove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="renewFrom ? $t('hr.contract.renewTitle') : $t('hr.contract.createTitle')" :edit-title="$t('hr.contract.editTitle')" width="min(680px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-alert
          v-if="renewFrom"
          type="info"
          show-icon
          class="mb"
          :message="$t('hr.contract.renewTip', { no: renewFrom.contractNo, start: renewFrom.startDate, end: renewFrom.endDate || $t('hr.contract.openEnded') })"
        />
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.contract.employee')" name="employeeId">
              <EmployeeSelect v-model:value="form.employeeId" :label="form.employeeLabel" :disabled="isEdit || !!renewFrom" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.contract.contractNo')" name="contractNo" :extra="$t('hr.contract.autoNo')">
              <a-input v-model:value="form.contractNo" :maxlength="32" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.contract.contractType')" name="contractType">
              <a-select v-model:value="form.contractType" :options="toOptions(dict.hr_contract_type)" @change="onTypeChange" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.contract.signDate')" name="signDate">
              <a-date-picker v-model:value="form.signDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.beginDate')" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" @change="onStartChange" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.endDate')" name="endDate" :extra="form.contractType === 'OPEN' ? $t('hr.contract.openNoEnd') : undefined">
              <a-date-picker
                v-model:value="form.endDate"
                value-format="YYYY-MM-DD"
                class="full"
                :disabled="form.contractType === 'OPEN'"
                :disabled-date="(d) => form.startDate && d.isBefore(dayjs(form.startDate))"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.contract.probation')" name="probationMonths" :extra="$t('hr.contract.probationExtra')">
              <a-input-number v-model:value="form.probationMonths" :min="0" :max="6" :precision="0" :addon-after="$t('hr.contract.months')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="2" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <a-modal v-model:open="terminateOpen" :title="$t('hr.contract.terminateTitle', { no: terminating?.contractNo || '' })" :confirm-loading="saving" destroy-on-close @ok="submitTerminate">
      <a-form ref="terminateRef" :model="terminateForm" layout="vertical">
        <a-form-item :label="$t('hr.contract.terminateDate')" name="date" :rules="[{ required: true, message: $t('hr.contract.terminateDateRequired') }]">
          <a-date-picker v-model:value="terminateForm.date" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('hr.contract.terminateReason')" name="reason" :rules="[{ required: true, whitespace: true, message: $t('hr.contract.terminateReasonRequired') }]">
          <a-textarea v-model:value="terminateForm.reason" :rows="3" :maxlength="255" :placeholder="$t('hr.contract.terminateReasonPh')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import EmployeeSelect from '@/components/hr/EmployeeSelect.vue'
import { addContract, deleteContract, getContract, pageContract, renewContract, terminateContract, updateContract } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

import { LaborContractStatus } from '@/constants/enums'
const dict = useDict('hr_contract_type', 'hr_contract_status', 'hr_emp_status')
const route = useRoute()

const columns = [
  { title: t('hr.contract.contractNo'), dataIndex: 'contractNo', width: 160, fixed: 'left' },
  { title: t('hr.contract.employee'), key: 'employee', width: 210, exportValue: (r) => `${r.empNo} ${r.employeeName}` },
  { title: t('hr.contract.contractType'), key: 'contractType', dataIndex: 'contractType', width: 150, dict: 'hr_contract_type' },
  { title: t('hr.contract.signDate'), dataIndex: 'signDate', width: 110 },
  { title: t('hr.contract.term'), key: 'term', width: 230, exportValue: (r) => `${r.startDate} ~ ${r.endDate || t('hr.contract.openEnded')}` },
  { title: t('hr.contract.daysLeft'), key: 'daysLeft', dataIndex: 'daysLeft', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'hr_contract_status' },
  { title: t('hr.contract.terminate'), key: 'terminate', width: 180, exportValue: (r) => (r.terminateDate ? `${r.terminateDate} ${r.terminateReason || ''}` : '') },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 260, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-contract')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, contractType: undefined, expireDays: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageContract, query)
onMounted(() => {
  // 到期提醒消息可带 ?expireDays=30 跳转过来
  if (route.query.expireDays) query.expireDays = Number(route.query.expireDays)
  load()
})

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
function onQuickFilter(e) {
  query.expireDays = e.target.value === 'expiring' ? 30 : undefined
  if (query.expireDays) query.status = undefined
  search()
}

const daysText = (d) => (d < 0 ? t('hr.contract.overdueDays', { n: -d }) : d === 0 ? t('hr.contract.dueToday') : t('hr.contract.daysN', { n: d }))
const daysClass = (d) => (d < 0 ? 'danger' : d <= 30 ? 'warn' : '')

// 新签 / 编辑 / 续签
const FIELDS = ['employeeId', 'contractNo', 'contractType', 'signDate', 'startDate', 'endDate', 'probationMonths', 'remark']
const renewFrom = ref(null)
const rules = {
  employeeId: [{ required: true, message: t('hr.contract.employeeRequired') }],
  contractType: [{ required: true, message: t('hr.contract.contractTypeRequired') }],
  signDate: [{ required: true, message: t('hr.contract.signDateRequired') }],
  startDate: [{ required: true, message: t('hr.contract.startDateRequired') }],
  endDate: [
    {
      validator: (_, v) => (drawer.form.value.contractType !== 'OPEN' && !v ? Promise.reject(new Error(t('hr.contract.endDateRequired'))) : Promise.resolve())
    }
  ]
}
const emptyForm = () => ({
  id: undefined,
  employeeId: undefined,
  employeeLabel: undefined,
  contractNo: undefined,
  contractType: 'FIXED',
  signDate: dayjs().format('YYYY-MM-DD'),
  startDate: undefined,
  endDate: undefined,
  probationMonths: 0,
  remark: undefined
})
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getContract(record.id)
    return { ...d, employeeLabel: `${d.empNo} ${d.employeeName}` }
  },
  create: (payload) => (renewFrom.value ? renewContract(renewFrom.value.id, payload) : addContract(payload)),
  update: updateContract,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form, isEdit } = drawer

function openCreate() {
  renewFrom.value = null
  drawer.openCreate({ startDate: dayjs().format('YYYY-MM-DD'), endDate: dayjs().add(3, 'year').subtract(1, 'day').format('YYYY-MM-DD'), probationMonths: 3 })
}

function openRenew(record) {
  renewFrom.value = record
  const start = record.endDate ? dayjs(record.endDate).add(1, 'day') : dayjs()
  // 续签默认沿用原合同期限
  const years = record.endDate ? Math.max(dayjs(record.endDate).add(1, 'day').diff(dayjs(record.startDate), 'year'), 1) : 3
  drawer.openCreate({
    employeeId: record.employeeId,
    employeeLabel: `${record.empNo} ${record.employeeName}`,
    contractType: record.contractType,
    startDate: start.format('YYYY-MM-DD'),
    endDate: record.contractType === 'OPEN' ? undefined : start.add(years, 'year').subtract(1, 'day').format('YYYY-MM-DD'),
    probationMonths: 0
  })
}

function onTypeChange(type) {
  if (type === 'OPEN') form.value.endDate = undefined
}
/** 固定期限合同：选开始日期后默认 3 年 */
function onStartChange(start) {
  if (start && !form.value.endDate && form.value.contractType !== 'OPEN') {
    form.value.endDate = dayjs(start).add(3, 'year').subtract(1, 'day').format('YYYY-MM-DD')
  }
}

const remove = useAction(deleteContract, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// 终止
const saving = ref(false)
const terminateOpen = ref(false)
const terminating = ref()
const terminateRef = ref()
const terminateForm = reactive({ date: undefined, reason: '' })
function openTerminate(record) {
  terminating.value = record
  Object.assign(terminateForm, { date: dayjs().format('YYYY-MM-DD'), reason: '' })
  terminateOpen.value = true
}
async function submitTerminate() {
  await terminateRef.value.validate()
  saving.value = true
  try {
    await terminateContract(terminating.value.id, terminateForm.date, terminateForm.reason)
    message.success(t('hr.contract.terminated'))
    terminateOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
.danger {
  color: #ff4d4f;
  font-weight: 500;
}
.warn {
  color: #fa8c16;
  font-weight: 500;
}
</style>
