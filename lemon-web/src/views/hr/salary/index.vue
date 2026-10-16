<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="员工" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="工号或姓名" allow-clear />
        </a-form-item>
        <a-form-item label="部门" name="deptId">
          <DeptSelect v-model:value="query.deptId" placeholder="全部" />
        </a-form-item>
        <a-form-item label="档案" name="missing">
          <a-select v-model:value="query.missing" placeholder="全部" allow-clear :options="[{ value: true, label: '未建立薪资档案' }]" />
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
        :scroll="{ x: 1300, y: '100%' }"
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
              <span v-else class="sub-text">默认比例</span>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'housingRate'">
            <span v-if="record.id">{{ Number(record.housingRate) }}%</span>
          </template>
          <template v-else-if="column.key === 'monthly'">
            <template v-if="record.id">
              <div>{{ formatMoney(monthlyCost(record)) }}</div>
              <div class="sub-text">时薪成本 {{ formatMoney(monthlyCost(record) / 174) }}</div>
            </template>
            <a-tag v-else color="orange" :bordered="false">未建立</a-tag>
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
              <template #icon><EditOutlined /></template>{{ record.id ? '调整' : '建立档案' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="open" :title="`薪资档案 - ${current?.employeeName || ''}`" width="min(640px, 100vw)" destroy-on-close>
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="基本工资" name="baseSalary" :rules="[{ required: true, message: '请填写基本工资' }]" extra="修改基本工资会记录一条调薪异动">
              <a-input-number v-model:value="form.baseSalary" :min="0" :precision="2" addon-after="元/月" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="岗位津贴" name="postAllowance">
              <a-input-number v-model:value="form.postAllowance" :min="0" :precision="2" addon-after="元/月" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="其他补贴" name="otherAllowance" extra="餐补、交通等，不参与缺勤扣款计算">
              <a-input-number v-model:value="form.otherAllowance" :min="0" :precision="2" addon-after="元/月" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="绩效奖金基数" name="perfBonusBase" extra="实发 = 基数 × 绩效系数（S 1.5 / A 1.2 / B 1.0 / C 0.8 / D 0）">
              <a-input-number v-model:value="form.perfBonusBase" :min="0" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="社保方案" name="schemeId" extra="按方案的缴费比例和基数上下限计算社保；不选按默认比例（个人 10.5%、单位 26%）">
              <a-select v-model:value="form.schemeId" allow-clear placeholder="默认比例" :options="schemeOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="社保基数" name="socialBase" extra="不填按基本工资 + 岗位津贴">
              <a-input-number v-model:value="form.socialBase" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="公积金基数" name="housingBase">
              <a-input-number v-model:value="form.housingBase" :min="0" :precision="2" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="公积金比例" name="housingRate">
              <a-input-number v-model:value="form.housingRate" :min="5" :max="12" :precision="0" addon-after="%" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="工资卡开户行" name="bankName"><a-input v-model:value="form.bankName" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="工资卡账号" name="bankAccount"><a-input v-model:value="form.bankAccount" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="生效日期" name="effectiveDate" extra="晚于今天的调整到期后计入工资；工资表按月末生效的薪资计算">
              <a-date-picker v-model:value="form.effectiveDate" value-format="YYYY-MM-DD" class="full" placeholder="默认今天（新建档案为入职日期）" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="备注" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
        <a-alert type="info" show-icon :message="estimate" />
        <template v-if="history.length">
          <div class="history-title">薪资历史</div>
          <a-table row-key="id" size="small" :columns="historyColumns" :data-source="history" :pagination="false">
            <template #bodyCell="{ column, record: h }">
              <template v-if="column.key === 'amount'">{{ formatMoney(h[column.dataIndex]) }}</template>
            </template>
          </a-table>
        </template>
      </a-form>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="open = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="save">保存</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import { listSocialScheme, pageSalary, saveSalary, salaryHistory } from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'

useDict('hr_emp_status')
const showSearch = ref(true)
const columns = [
  { title: '员工', key: 'employee', width: 240, fixed: 'left' },
  { title: '基本工资', key: 'baseSalary', width: 110, align: 'right' },
  { title: '岗位津贴', key: 'postAllowance', width: 100, align: 'right' },
  { title: '其他补贴', key: 'otherAllowance', width: 100, align: 'right' },
  { title: '绩效基数', key: 'perfBonusBase', width: 100, align: 'right' },
  { title: '社保方案', key: 'scheme', width: 140, exportValue: (r) => (r.id ? (r.schemeId ? schemeName(r) : '默认比例') : '') },
  { title: '社保基数', key: 'socialBase', width: 110, align: 'right' },
  { title: '公积金基数', key: 'housingBase', width: 110, align: 'right' },
  { title: '公积金比例', key: 'housingRate', width: 90, align: 'right' },
  { title: '月度人工成本', key: 'monthly', width: 150, align: 'right' },
  { title: '工资卡', key: 'bank', width: 170 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
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
})

// 社保方案：未选择方案时按默认比例（个人 10.5%、单位 26%）
const DEFAULT_RATES = { personal: 10.5, company: 26 }
const schemes = ref([])
async function loadSchemes() {
  schemes.value = await listSocialScheme().catch(() => [])
}
const schemeOptions = computed(() =>
  schemes.value
    .filter((s) => s.status === 1 || s.id === form.schemeId)
    .map((s) => ({ value: s.id, label: `${s.name}${s.city ? `（${s.city}）` : ''} · 个人 ${+schemeRates(s).personal.toFixed(2)}% / 单位 ${+schemeRates(s).company.toFixed(2)}%` }))
)
const schemeName = (r) => r.schemeName || schemes.value.find((s) => s.id === r.schemeId)?.name || `方案 #${r.schemeId}`
function schemeRates(s) {
  if (!s) return DEFAULT_RATES
  const n = (v) => Number(v) || 0
  return {
    personal: n(s.pensionPersonal) + n(s.medicalPersonal) + n(s.unemploymentPersonal),
    company: n(s.pensionCompany) + n(s.medicalCompany) + n(s.unemploymentCompany) + n(s.injuryCompany)
  }
}
const ratesOf = (schemeId) => schemeRates(schemeId ? schemes.value.find((s) => s.id === schemeId) : null)

/** 月度人工成本 = 基本工资 + 津贴 + 单位社保（按社保方案，默认 26%）+ 单位公积金 */
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
  { title: '生效日期', dataIndex: 'effectiveDate', width: 110 },
  { title: '基本工资', key: 'amount', dataIndex: 'baseSalary', width: 110, align: 'right' },
  { title: '岗位津贴', key: 'amount', dataIndex: 'postAllowance', width: 100, align: 'right' },
  { title: '社保基数', key: 'amount', dataIndex: 'socialBase', width: 100, align: 'right' },
  { title: '说明', dataIndex: 'remark', ellipsis: true }
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
  return `个人社保公积金约 ${formatMoney(personal)} 元/月，单位人工成本约 ${formatMoney(cost)} 元/月（项目工时按 ${formatMoney(cost / 174)} 元/小时计算）`
})
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await saveSalary(current.value.employeeId, Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v === '' || v === undefined ? null : v])))
    message.success('已保存')
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
