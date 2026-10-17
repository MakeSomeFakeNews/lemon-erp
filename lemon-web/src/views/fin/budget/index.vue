<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <span>{{ $t('fin.budget.year') }}</span>
          <a-date-picker v-model:value="yearValue" picker="year" value-format="YYYY" :allow-clear="false" @change="load" />
          <DeptSelect v-model:value="deptId" :placeholder="$t('fin.budget.allDepts')" class="dept" />
          <a-radio-group v-model:value="tab" button-style="solid">
            <a-radio-button value="execution">{{ $t('fin.budget.execution') }}</a-radio-button>
            <a-radio-button value="budget">{{ $t('fin.budget.planning') }}</a-radio-button>
          </a-radio-group>
        </a-space>
        <a-button v-perm="'fin:budget:edit'" type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>{{ $t('fin.budget.add') }}
        </a-button>
      </div>

      <a-table
        v-if="tab === 'execution'"
        row-key="budgetId"
        size="middle"
        :columns="execColumns"
        :data-source="execution"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100 }"
        :locale="{ emptyText: $t('fin.budget.empty') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'account'">
            <div>{{ record.accountCode }} {{ record.accountName }}</div>
            <div class="sub-text">{{ record.deptName }}</div>
          </template>
          <template v-else-if="column.key === 'totalBudget'">¥{{ formatMoney(record.totalBudget) }}</template>
          <template v-else-if="column.key === 'totalActual'">
            <span :class="{ over: overBudget(record) }">¥{{ formatMoney(record.totalActual) }}</span>
          </template>
          <template v-else-if="column.key === 'rate'">
            <a-progress
              v-if="record.rate != null"
              :percent="Math.min(Number(record.rate), 100)"
              :status="Number(record.rate) > 100 ? 'exception' : Number(record.rate) > 90 ? 'active' : 'normal'"
              :format="() => `${record.rate}%`"
              size="small"
            />
          </template>
          <template v-else-if="column.key === 'months'">
            <div class="months">
              <div v-for="(b, i) in record.budgets" :key="i" class="month" :class="{ over: Number(record.actuals[i]) > Number(b) && Number(b) > 0 }">
                <div class="m">{{ $t('fin.budget.month', { m: i + 1 }) }}</div>
                <div class="a">{{ short(record.actuals[i]) }}</div>
                <div class="b">/{{ short(b) }}</div>
              </div>
            </div>
          </template>
        </template>
      </a-table>

      <a-table
        v-else
        row-key="id"
        size="middle"
        :columns="budgetColumns"
        :data-source="budgets"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'account'">
            <div>{{ record.accountCode }} {{ record.accountName }}</div>
            <div class="sub-text">{{ record.deptId === 0 ? $t('fin.budget.company') : record.deptName }}</div>
          </template>
          <template v-else-if="column.key === 'total'">¥{{ formatMoney(record.total) }}</template>
          <template v-else-if="column.key === 'controlMode'"><DictTag type="fin_budget_control" :value="record.controlMode || 'NONE'" /></template>
          <template v-else-if="column.key === 'months'">
            <span class="sub-text">{{ MONTH_KEYS.map((k) => short(record[k])).join(' · ') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:budget:edit'" class="table-actions">
              <a-button type="link" size="small" @click="openForm(record)">{{ $t('common.action.edit') }}</a-button>
              <a-popconfirm :title="$t('fin.budget.confirmDelete')" @confirm="remove(record.id)">
                <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="form.id ? $t('fin.budget.edit') : $t('fin.budget.add')" width="720px" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item :label="$t('fin.budget.year')" required>
              <a-input-number v-model:value="form.budgetYear" :min="2000" :max="2999" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('common.field.dept')" :extra="$t('fin.budget.deptExtra')">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="10">
            <a-form-item :label="$t('fin.budget.account')" name="accountId" :rules="[{ required: true, message: $t('fin.budget.accountRequired') }]" :extra="$t('fin.budget.accountExtra')">
              <AccountSelect v-model:value="form.accountId" all :prefix="['5', '6']" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="months-head">
          <span>{{ $t('fin.budget.monthsTotal', { amount: formatMoney(form.months?.reduce((s, v) => s + (Number(v) || 0), 0) || 0) }) }}</span>
          <a-space>
            <a-input-number v-model:value="spread" :min="0" :precision="2" :placeholder="$t('fin.budget.annualAmount')" size="small" />
            <a-button size="small" @click="spreadEvenly">{{ $t('fin.budget.spread') }}</a-button>
          </a-space>
        </div>
        <a-row :gutter="[12, 8]">
          <a-col v-for="(m, i) in form.months" :key="i" :span="6">
            <a-input-number v-model:value="form.months[i]" :min="0" :precision="2" :addon-before="$t('fin.budget.month', { m: i + 1 })" class="full" />
          </a-col>
        </a-row>
        <a-row :gutter="16" class="remark">
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('fin.budget.control')" :extra="$t('fin.budget.controlExtra')">
              <a-select v-model:value="form.controlMode" :options="toOptions(dict.fin_budget_control)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="14">
            <a-form-item :label="$t('common.field.remark')">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { currentLocale } from '@/locales'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { addBudget, budgetExecution, deleteBudget, listBudget, updateBudget } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/fin/fin'

const { t } = useI18n()
const dict = useDict('fin_budget_control')

const MONTH_KEYS = ['m01', 'm02', 'm03', 'm04', 'm05', 'm06', 'm07', 'm08', 'm09', 'm10', 'm11', 'm12']
const yearValue = ref(String(dayjs().year()))
const year = computed(() => Number(yearValue.value))
const deptId = ref()
const tab = ref('execution')
const loading = ref(false)
const execution = ref([])
const budgets = ref([])

const short = (n) => {
  const v = Number(n) || 0
  if (v === 0) return '0'
  // 万元以上用紧凑写法（中文 1.5万，英文 15K）
  return Math.abs(v) >= 10000
    ? new Intl.NumberFormat(currentLocale(), { notation: 'compact', maximumFractionDigits: 1 }).format(v)
    : v.toLocaleString(currentLocale(), { maximumFractionDigits: 0 })
}
const overBudget = (r) => r.rate != null && Number(r.rate) > 100

const execColumns = [
  { title: t('fin.budget.accountDept'), key: 'account', width: 220, fixed: 'left' },
  { title: t('fin.budget.annualBudget'), key: 'totalBudget', width: 130, align: 'right' },
  { title: t('fin.budget.actual'), key: 'totalActual', width: 130, align: 'right' },
  { title: t('fin.budget.rate'), key: 'rate', width: 160 },
  { title: t('fin.budget.monthsActual'), key: 'months', width: 420 }
]
const budgetColumns = [
  { title: t('fin.budget.accountDept'), key: 'account', width: 240 },
  { title: t('fin.budget.annualBudget'), key: 'total', width: 140, align: 'right' },
  { title: t('fin.budget.control'), key: 'controlMode', width: 140 },
  { title: t('fin.budget.months'), key: 'months' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]

async function load() {
  loading.value = true
  try {
    const params = { year: year.value, deptId: deptId.value ?? undefined }
    ;[execution.value, budgets.value] = await Promise.all([budgetExecution(params), listBudget(params)])
  } finally {
    loading.value = false
  }
}
watch(deptId, load)
onMounted(load)

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const spread = ref()
function openForm(record) {
  form.value = record
    ? { id: record.id, budgetYear: record.budgetYear, deptId: record.deptId || undefined, accountId: record.accountId,
        months: MONTH_KEYS.map((k) => Number(record[k])), controlMode: record.controlMode || 'NONE', remark: record.remark }
    : { budgetYear: year.value, deptId: deptId.value, accountId: undefined, months: Array(12).fill(0), controlMode: 'NONE', remark: '' }
  spread.value = undefined
  formOpen.value = true
}
function spreadEvenly() {
  if (!spread.value) return
  const each = Math.floor((spread.value / 12) * 100) / 100
  form.value.months = Array(12).fill(each)
  form.value.months[11] = Math.round((spread.value - each * 11) * 100) / 100
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { id, ...data } = form.value
    data.deptId = data.deptId || null
    id ? await updateBudget(id, data) : await addBudget(data)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function remove(id) {
  await deleteBudget(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.dept {
  width: 200px;
}
.months {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
}
.month {
  min-width: 52px;
  text-align: center;
  padding: 2px 4px;
  border-radius: 4px;
  background: var(--app-bg-2, #fafafa);
  font-size: 12px;
}
.month .m {
  color: var(--app-text-3);
}
.month .b {
  color: var(--app-text-3);
}
.month.over,
.over {
  color: #cf1322;
}
.months-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.remark {
  margin-top: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
