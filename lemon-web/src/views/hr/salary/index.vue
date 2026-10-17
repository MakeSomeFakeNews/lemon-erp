<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.salary.employee')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.salary.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.dept')" name="deptId">
          <DeptSelect v-model:value="query.deptId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('hr.salary.profile')" name="missing">
          <a-select v-model:value="query.missing" :placeholder="$t('common.placeholder.all')" allow-clear :options="[{ value: true, label: $t('hr.salary.missing') }]" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load" />
      <a-table
        row-key="employeeId"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1600, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'employee'">
            <div>{{ record.employeeName }} <DictTag type="hr_emp_status" :value="record.empStatus" /></div>
            <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }} · {{ record.positionName || '-' }}</div>
          </template>
          <template v-else-if="['baseSalary', 'postAllowance', 'otherAllowance', 'perfBonusBase', 'socialBase', 'housingBase'].includes(column.key)">
            <span v-if="record.id">{{ formatMoney(record[column.key]) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'scheme'">
            <template v-if="record.id">
              <span v-if="record.schemeId">{{ schemeName(record) }}</span>
              <span v-else class="sub-text">{{ $t('hr.salary.defaultRates') }}</span>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'housingRate'">
            <span v-if="record.id">{{ Number(record.housingRate) }}%</span>
          </template>
          <template v-else-if="column.key === 'monthly'">
            <template v-if="record.id">
              <div>{{ formatMoney(monthlyCost(record)) }}</div>
              <div class="sub-text">{{ $t('hr.salary.hourlyCost', { amount: formatMoney(hourlyCost(monthlyCost(record))) }) }}</div>
            </template>
            <a-tag v-else color="orange" :bordered="false">{{ $t('hr.salary.notCreated') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'bank'">
            <template v-if="record.bankAccount">
              <div>{{ record.bankName }}</div>
              <div class="sub-text">{{ maskAccount(record.bankAccount) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button v-perm="'hr:salary:edit'" type="link" size="small" @click="openEdit(record)">
              <template #icon><EditOutlined /></template>{{ record.id ? $t('hr.salary.adjust') : $t('hr.salary.create') }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="open" :title="$t('hr.salary.drawerTitle', { name: current?.employeeName || '' })" width="min(640px, 100vw)" destroy-on-close>
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.baseSalary')" name="baseSalary" :rules="[{ required: true, message: $t('hr.salary.rule.baseSalary') }]" :extra="$t('hr.salary.baseSalaryExtra')">
              <a-input-number v-model:value="form.baseSalary" :min="0" :precision="2" :addon-after="$t('hr.salary.perMonth')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.postAllowance')" name="postAllowance">
              <a-input-number v-model:value="form.postAllowance" :min="0" :precision="2" :addon-after="$t('hr.salary.perMonth')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.otherAllowance')" name="otherAllowance" :extra="$t('hr.salary.otherAllowanceExtra')">
              <a-input-number v-model:value="form.otherAllowance" :min="0" :precision="2" :addon-after="$t('hr.salary.perMonth')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.perfBonusBase')" name="perfBonusBase" :extra="$t('hr.salary.perfBonusBaseExtra')">
              <a-input-number v-model:value="form.perfBonusBase" :min="0" :precision="2" :addon-after="$t('hr.salary.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('hr.salary.scheme')" name="schemeId" :extra="$t('hr.salary.schemeExtra', defaultRatesText)">
              <a-select v-model:value="form.schemeId" allow-clear :placeholder="$t('hr.salary.defaultRates')" :options="schemeOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.salary.socialBase')" name="socialBase" :extra="$t('hr.salary.socialBaseExtra')">
              <a-input-number v-model:value="form.socialBase" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.salary.housingBase')" name="housingBase">
              <a-input-number v-model:value="form.housingBase" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.salary.housingRate')" name="housingRate">
              <a-input-number v-model:value="form.housingRate" :min="5" :max="12" :precision="0" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.bankName')" name="bankName"><a-input v-model:value="form.bankName" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.bankAccount')" name="bankAccount"><a-input v-model:value="form.bankAccount" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.salary.effectiveDate')" name="effectiveDate" :extra="$t('hr.salary.effectiveDateExtra')">
              <a-date-picker v-model:value="form.effectiveDate" value-format="YYYY-MM-DD" class="full" :placeholder="$t('hr.salary.effectiveDatePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.remark')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
        <a-alert type="info" show-icon :message="estimate" />
        <template v-if="history.length">
          <div class="history-title">{{ $t('hr.salary.history') }}</div>
          <a-table row-key="id" size="small" :columns="historyColumns" :data-source="history" :pagination="false">
            <template #bodyCell="{ column, record: h }">
              <template v-if="column.key === 'amount'">{{ formatMoney(h[column.dataIndex]) }}</template>
            </template>
          </a-table>
        </template>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="open = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import { listSocialScheme, pageSalary, saveSalary, salaryHistory, salaryRules } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

const { t } = useI18n()

useDict('hr_emp_status')
const showSearch = ref(true)
const columns = [
  { title: t('hr.salary.employee'), key: 'employee', width: 240, fixed: 'left' },
  { title: t('hr.salary.baseSalary'), key: 'baseSalary', width: 120, align: 'right' },
  { title: t('hr.salary.postAllowance'), key: 'postAllowance', width: 150, align: 'right' },
  { title: t('hr.salary.otherAllowance'), key: 'otherAllowance', width: 130, align: 'right' },
  { title: t('hr.salary.col.perfBonusBase'), key: 'perfBonusBase', width: 130, align: 'right' },
  { title: t('hr.salary.scheme'), key: 'scheme', width: 180, exportValue: (r) => (r.id ? (r.schemeId ? schemeName(r) : t('hr.salary.defaultRates')) : '') },
  { title: t('hr.salary.socialBase'), key: 'socialBase', width: 170, align: 'right' },
  { title: t('hr.salary.housingBase'), key: 'housingBase', width: 150, align: 'right' },
  { title: t('hr.salary.housingRate'), key: 'housingRate', width: 140, align: 'right' },
  { title: t('hr.salary.col.monthly'), key: 'monthly', width: 160, align: 'right' },
  { title: t('hr.salary.col.bank'), key: 'bank', width: 170 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-salary')
const emptyQuery = () => ({ keyword: undefined, deptId: undefined, missing: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageSalary, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(() => {
  load()
  loadSchemes()
  loadRules()
})

// 当前租户的计薪规则（业务参数）：未选择社保方案时的默认比例、月计薪小时（月计薪天数 × 每日工时）
const payRules = ref({})
async function loadRules() {
  payRules.value = await salaryRules().catch(() => ({}))
}
const defaultRates = computed(() => ({ personal: Number(payRules.value.socialPersonal) || 0, company: Number(payRules.value.socialCompany) || 0 }))
const pct = (v) => (v === undefined || v === null ? '-' : Number(v))
const defaultRatesText = computed(() => ({ personal: pct(payRules.value.socialPersonal), company: pct(payRules.value.socialCompany) }))
/** 小时人工成本 = 月度人工成本 / 月计薪小时（规则未取到时不显示） */
const hourlyCost = (monthly) => (Number(payRules.value.monthPayHours) ? monthly / Number(payRules.value.monthPayHours) : null)
const schemes = ref([])
async function loadSchemes() {
  schemes.value = await listSocialScheme().catch(() => [])
}
const schemeOptions = computed(() =>
  schemes.value
    .filter((s) => s.status === 1 || s.id === form.schemeId)
    .map((s) => ({
      value: s.id,
      label: t('hr.salary.schemeOption', {
        name: s.city ? t('hr.salary.nameCity', { name: s.name, city: s.city }) : s.name,
        personal: +schemeRates(s).personal.toFixed(2),
        company: +schemeRates(s).company.toFixed(2)
      })
    }))
)
const schemeName = (r) => r.schemeName || schemes.value.find((s) => s.id === r.schemeId)?.name || t('hr.salary.schemeNo', { id: r.schemeId })
function schemeRates(s) {
  if (!s) return defaultRates.value
  const n = (v) => Number(v) || 0
  return {
    personal: n(s.pensionPersonal) + n(s.medicalPersonal) + n(s.unemploymentPersonal),
    company: n(s.pensionCompany) + n(s.medicalCompany) + n(s.unemploymentCompany) + n(s.injuryCompany)
  }
}
const ratesOf = (schemeId) => schemeRates(schemeId ? schemes.value.find((s) => s.id === schemeId) : null)

/** 月度人工成本 = 基本工资 + 津贴 + 单位社保（按社保方案，未选择时按业务参数的默认单位比例）+ 单位公积金 */
function monthlyCost(r) {
  const n = (v) => Number(v) || 0
  return n(r.baseSalary) + n(r.postAllowance) + n(r.otherAllowance) + (n(r.socialBase) * ratesOf(r.schemeId).company) / 100 +
    (n(r.housingBase) * n(r.housingRate)) / 100
}
const maskAccount = (a) => (a.length > 8 ? `${a.slice(0, 4)} **** ${a.slice(-4)}` : a)

const open = ref(false)
const saving = ref(false)
const current = ref()
const formRef = ref()
const form = reactive({})
const history = ref([])
const historyColumns = [
  { title: t('hr.salary.effectiveDate'), dataIndex: 'effectiveDate', width: 120 },
  { title: t('hr.salary.baseSalary'), key: 'amount', dataIndex: 'baseSalary', width: 110, align: 'right' },
  { title: t('hr.salary.postAllowance'), key: 'amount', dataIndex: 'postAllowance', width: 130, align: 'right' },
  { title: t('hr.salary.socialBase'), key: 'amount', dataIndex: 'socialBase', width: 130, align: 'right' },
  { title: t('hr.salary.col.description'), dataIndex: 'remark', ellipsis: true }
]
function openEdit(record) {
  current.value = record
  history.value = []
  if (record.id) salaryHistory(record.employeeId).then((list) => (history.value = list))
  Object.assign(form, {
    baseSalary: record.baseSalary ?? undefined,
    postAllowance: record.postAllowance ?? 0,
    otherAllowance: record.otherAllowance ?? 0,
    perfBonusBase: record.perfBonusBase ?? 0,
    socialBase: record.socialBase ?? undefined,
    housingBase: record.housingBase ?? undefined,
    housingRate: record.housingRate ?? 7,
    schemeId: record.schemeId ?? undefined,
    bankName: record.bankName,
    bankAccount: record.bankAccount,
    remark: record.remark,
    effectiveDate: undefined
  })
  open.value = true
}
const estimate = computed(() => {
  const base = (Number(form.baseSalary) || 0) + (Number(form.postAllowance) || 0)
  const social = form.socialBase ?? base
  const housing = form.housingBase ?? base
  const personal = (social * ratesOf(form.schemeId).personal) / 100 + (housing * (form.housingRate || 7)) / 100
  const cost = monthlyCost({ ...form, socialBase: social, housingBase: housing })
  return t('hr.salary.estimate', { personal: formatMoney(personal), cost: formatMoney(cost), hourly: formatMoney(hourlyCost(cost)) })
})
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await saveSalary(current.value.employeeId, Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v === '' || v === undefined ? null : v])))
    message.success(t('common.msg.saved'))
    open.value = false
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.history-title {
  font-weight: 500;
  margin: 16px 0 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
