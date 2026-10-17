<template>
  <div class="workspace">
    <a-card :bordered="false" class="page-card workspace-side payroll-side">
      <div class="workspace-side-head">
        <a-select v-model:value="year" :options="years" class="year" @change="loadList" />
        <a-button v-perm="'hr:payroll:add'" type="primary" @click="openGenerate">
          <template #icon><PlusOutlined /></template>{{ $t('hr.payroll.generate') }}
        </a-button>
      </div>
      <a-spin :spinning="listLoading">
        <a-empty v-if="!payrolls.length" :description="$t('hr.payroll.emptyYear')" />
        <div
          v-for="p in payrolls"
          :key="p.id"
          class="payroll-item"
          :class="{ active: p.id === currentId }"
          @click="select(p.id)"
        >
          <div class="payroll-item-head">
            <b>{{ periodText(p.period) }}</b>
            <DictTag type="hr_payroll_status" :value="p.status" />
          </div>
          <div class="sub-text">{{ $t('hr.payroll.itemSub', { n: p.headcount, amount: formatMoney(p.netTotal) }) }}</div>
        </div>
      </a-spin>
    </a-card>

    <div class="table-page workspace-main">
      <a-card v-if="!payroll" :bordered="false" class="page-card">
        <a-empty :description="$t('hr.payroll.emptySelect')" />
      </a-card>
      <template v-else>
        <a-card :bordered="false" class="page-card">
          <div class="title-row">
            <div>
              <span class="title">{{ $t('hr.payroll.title', { period: periodText(payroll.period) }) }}</span>
              <DictTag type="hr_payroll_status" :value="payroll.status" />
              <span class="sub-text">{{ payroll.payrollNo }} · {{ $t('hr.payroll.createdBy', { name: payroll.createByName || '-' }) }}</span>
            </div>
            <a-space wrap>
              <a-button :disabled="!payroll.items?.length" @click="printSlips">
                <template #icon><PrinterOutlined /></template>{{ $t('hr.payroll.printSlips') }}
              </a-button>
              <template v-if="payroll.status === PayrollStatus.DRAFT">
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:add')" :title="$t('hr.payroll.confirmRegenerate')" @confirm="regenerate">
                  <a-button>{{ $t('hr.payroll.regenerate') }}</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:confirm')" :title="$t('hr.payroll.confirmConfirm')" @confirm="confirm">
                  <a-button type="primary">{{ $t('hr.payroll.confirm') }}</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:remove')" :title="$t('hr.payroll.confirmDelete')" @confirm="remove">
                  <a-button danger>{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </template>
              <template v-else-if="payroll.status === PayrollStatus.CONFIRMED">
                <a-button v-perm="'hr:payroll:pay'" type="primary" @click="openPay">{{ $t('hr.payroll.pay') }}</a-button>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:confirm')" :title="$t('hr.payroll.confirmUnconfirm')" @confirm="unconfirm">
                  <a-button>{{ $t('hr.payroll.unconfirm') }}</a-button>
                </a-popconfirm>
              </template>
              <a-button
                v-if="payroll.status !== PayrollStatus.DRAFT && !payroll.accrueVoucherId"
                v-perm="'hr:payroll:confirm'"
                @click="regenerateAccrual"
              >
                {{ $t('hr.payroll.genAccrual') }}
              </a-button>
            </a-space>
          </div>
          <a-row :gutter="12" class="totals">
            <a-col v-for="t in totals" :key="t.label" :xs="12" :sm="8" :lg="4">
              <div class="total-label">{{ t.label }}</div>
              <div class="total-value" :class="t.cls">{{ t.value }}</div>
            </a-col>
          </a-row>
          <div class="vouchers sub-text">
            {{ $t('hr.payroll.accrueVoucher', { no: payroll.accrueVoucherNo || '-' }) }}
            <span v-if="payroll.payVoucherNo">{{ $t('hr.payroll.payVoucher', { no: payroll.payVoucherNo, date: payroll.payDate }) }}</span>
            <span v-if="payroll.confirmByName">{{ $t('hr.payroll.confirmedBy', { name: payroll.confirmByName, time: payroll.confirmTime }) }}</span>
          </div>
        </a-card>

        <a-card :bordered="false" class="page-card table-card">
          <div class="table-head">
            <a-input-search v-model:value="keyword" :placeholder="$t('hr.payroll.keywordPh')" allow-clear class="w220" @search="loadDetail" />
            <span class="sub-text">{{ $t('hr.payroll.dailyTip') }}</span>
            <span class="sub-text">{{ $t('hr.payroll.taxTip') }}</span>
          </div>
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="itemColumns"
            :data-source="payroll.items"
            :loading="detailLoading"
            :pagination="false"
            :scroll="{ x: 3000, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'employee'">
                <div>{{ record.employeeName }}</div>
                <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
              </template>
              <template v-else-if="column.key === 'days'">
                {{ Number(record.shouldDays) }} / {{ Number(record.monthDays) }}
              </template>
              <template v-else-if="column.key === 'perf'">
                <template v-if="record.perfGrade">
                  <DictTag type="hr_perf_grade" :value="record.perfGrade" /> {{ formatMoney(record.perfBonus) }}
                </template>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'deduct'">
                <a-tooltip v-if="Number(record.attendanceDeduct)">
                  <template #title>
                    {{
                      $t('hr.payroll.deductTip', {
                        absent: Number(record.absentDays),
                        late: record.lateCount,
                        personal: Number(record.personalHours),
                        sick: Number(record.sickHours)
                      })
                    }}
                  </template>
                  <span class="danger">-{{ formatMoney(record.attendanceDeduct) }}</span>
                </a-tooltip>
                <span v-else class="sub-text">0.00</span>
              </template>
              <template v-else-if="column.key === 'overtime'">
                <span v-if="Number(record.overtimePay)">{{ formatMoney(record.overtimePay) }}<div class="sub-text">{{ $t('hr.payroll.hoursN', { n: Number(record.overtimeHours) }) }}</div></span>
                <span v-else class="sub-text">0.00</span>
              </template>
              <template v-else-if="MONEY_KEYS.includes(column.key)">
                <span :class="{ strong: ['grossPay', 'netPay'].includes(column.key) }">{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="payroll.status === PayrollStatus.DRAFT" v-perm="'hr:payroll:edit'" type="link" size="small" @click="openAdjust(record)">{{ $t('hr.payroll.adjust') }}</a-button>
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" :col-span="2"><b>{{ $t('hr.payroll.total') }}</b></a-table-summary-cell>
                  <a-table-summary-cell v-for="(c, i) in itemColumns.slice(2)" :key="c.key" :index="i + 2" align="right">
                    <b v-if="SUM_KEYS.includes(c.key)">{{ formatMoney(sum(c.key)) }}</b>
                  </a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
        </a-card>
      </template>
    </div>

    <a-modal v-model:open="generateOpen" :title="$t('hr.payroll.genTitle')" :confirm-loading="saving" @ok="generate">
      <a-alert type="info" show-icon class="mb" :message="$t('hr.payroll.genTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('hr.payroll.period')"><PeriodPicker v-model:value="genPeriod" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="adjustOpen" :title="$t('hr.payroll.adjustTitle', { name: adjusting?.employeeName || '' })" :confirm-loading="saving" destroy-on-close @ok="saveAdjust">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('hr.payroll.otherBonus')"><a-input-number v-model:value="adjustForm.otherBonus" :min="0" :precision="2" class="full" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('hr.payroll.otherDeduct')"><a-input-number v-model:value="adjustForm.otherDeduct" :min="0" :precision="2" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="adjustForm.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <span class="sub-text">{{ $t('hr.payroll.adjustTip') }}</span>
    </a-modal>

    <a-modal v-model:open="payOpen" :title="$t('hr.payroll.payTitle')" :confirm-loading="saving" destroy-on-close @ok="pay">
      <a-alert type="info" show-icon class="mb" :message="$t('hr.payroll.payTip', { amount: formatMoney(payroll?.netTotal) })" />
      <a-form ref="payRef" :model="payForm" layout="vertical">
        <a-form-item :label="$t('hr.payroll.payDate')" name="payDate" :rules="[{ required: true, message: $t('hr.payroll.rule.payDate') }]">
          <a-date-picker v-model:value="payForm.payDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('hr.payroll.account')" name="accountId" :rules="[{ required: true, message: $t('hr.payroll.rule.account') }]">
          <AccountSelect v-model:value="payForm.accountId" :roles="['CASH', 'BANK']" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { periodText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import { printDocs } from '@/utils/print'
import {
  accruePayroll,
  adjustPayrollItem,
  confirmPayroll,
  deletePayroll,
  generatePayroll,
  getPayroll,
  pagePayroll,
  payPayroll,
  unconfirmPayroll
} from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { useAction } from '@/composables/useAction'

import { PayrollStatus } from '@/constants/enums'
const { t } = useI18n()

const userStore = useUserStore()
useDict('hr_payroll_status', 'hr_perf_grade')
const MONEY_KEYS = ['baseSalary', 'allowance', 'pieceWage', 'commission', 'otherBonus', 'grossPay', 'socialPersonal', 'housingPersonal', 'specialDeduct', 'incomeTax',
  'otherDeduct', 'netPay', 'socialCompany', 'housingCompany', 'cumTaxable', 'cumTax']
const SUM_KEYS = [...MONEY_KEYS.filter((k) => !['cumTaxable', 'cumTax'].includes(k)), 'attendanceDeduct', 'overtime', 'perf']

const thisYear = dayjs().year()
const years = [thisYear, thisYear - 1, thisYear - 2, thisYear - 3, thisYear - 4, thisYear - 5, thisYear - 6, thisYear - 7]
  .map((y) => ({ value: y, label: t('hr.payroll.yearOption', { year: y }) }))
const year = ref(thisYear)
const payrolls = ref([])
const listLoading = ref(false)
const currentId = ref()
const payroll = ref()
const detailLoading = ref(false)
const keyword = ref('')
const saving = ref(false)

async function loadList() {
  listLoading.value = true
  try {
    payrolls.value = (await pagePayroll({ year: year.value, current: 1, size: 12 })).records
    if (!payrolls.value.some((p) => p.id === currentId.value)) {
      currentId.value = payrolls.value[0]?.id
      await loadDetail()
    }
  } finally {
    listLoading.value = false
  }
}
async function loadDetail() {
  if (!currentId.value) {
    payroll.value = null
    return
  }
  detailLoading.value = true
  try {
    payroll.value = await getPayroll(currentId.value, keyword.value || undefined)
  } finally {
    detailLoading.value = false
  }
}
function select(id) {
  currentId.value = id
  loadDetail()
}
async function refresh() {
  await loadList()
  await loadDetail()
}
onMounted(loadList)

const totals = computed(() => {
  const p = payroll.value
  return [
    { label: t('hr.payroll.tot.headcount'), value: p.headcount },
    { label: t('hr.payroll.tot.gross'), value: formatMoney(p.grossTotal) },
    { label: t('hr.payroll.tot.personal'), value: formatMoney(p.personalTotal) },
    { label: t('hr.payroll.tot.tax'), value: formatMoney(p.taxTotal) },
    { label: t('hr.payroll.tot.net'), value: formatMoney(p.netTotal), cls: 'net' },
    { label: t('hr.payroll.tot.company'), value: formatMoney(p.companyTotal) }
  ]
})

const itemColumns = [
  { title: t('hr.payroll.col.employee'), key: 'employee', width: 170, fixed: 'left' },
  { title: t('hr.payroll.col.days'), key: 'days', width: 150, align: 'right' },
  { title: t('hr.payroll.col.baseSalary'), key: 'baseSalary', width: 110, align: 'right' },
  { title: t('hr.payroll.col.allowance'), key: 'allowance', width: 110, align: 'right' },
  { title: t('hr.payroll.col.overtime'), key: 'overtime', width: 120, align: 'right' },
  { title: t('hr.payroll.col.pieceWage'), key: 'pieceWage', width: 110, align: 'right' },
  { title: t('hr.payroll.col.commission'), key: 'commission', width: 140, align: 'right' },
  { title: t('hr.payroll.col.perf'), key: 'perf', width: 160, align: 'right' },
  { title: t('hr.payroll.otherBonus'), key: 'otherBonus', width: 120, align: 'right' },
  { title: t('hr.payroll.col.deduct'), key: 'deduct', width: 170, align: 'right' },
  { title: t('hr.payroll.col.grossPay'), key: 'grossPay', width: 120, align: 'right' },
  { title: t('hr.payroll.col.socialPersonal'), key: 'socialPersonal', width: 150, align: 'right' },
  { title: t('hr.payroll.col.housingPersonal'), key: 'housingPersonal', width: 170, align: 'right' },
  { title: t('hr.payroll.col.specialDeduct'), key: 'specialDeduct', width: 200, align: 'right' },
  { title: t('hr.payroll.col.incomeTax'), key: 'incomeTax', width: 100, align: 'right' },
  { title: t('hr.payroll.otherDeduct'), key: 'otherDeduct', width: 140, align: 'right' },
  { title: t('hr.payroll.col.netPay'), key: 'netPay', width: 120, align: 'right' },
  { title: t('hr.payroll.col.socialCompany'), key: 'socialCompany', width: 150, align: 'right' },
  { title: t('hr.payroll.col.housingCompany'), key: 'housingCompany', width: 170, align: 'right' },
  { title: t('hr.payroll.col.cumTaxable'), key: 'cumTaxable', width: 160, align: 'right' },
  { title: t('hr.payroll.col.cumTax'), key: 'cumTax', width: 150, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true },
  { title: '', key: 'action', width: 70, fixed: 'right' }
]
/** 工资条：当前列表（可先按工号或姓名筛选）每人一条；打印字段名是模板匹配用的原始名称，保持中文 */
function printSlips() {
  const p = payroll.value
  const m = (v) => formatMoney(Number(v || 0))
  printDocs(
    p.items.map((i) => ({
      slip: true,
      title: `${periodText(p.period)} 工资条`, // i18n-ignore
      fields: [
        ['姓名', i.employeeName], // i18n-ignore
        ['工号', i.empNo], // i18n-ignore
        ['部门', i.deptName], // i18n-ignore
        ['出勤 / 工作日', `${Number(i.shouldDays)} / ${Number(i.monthDays)}`], // i18n-ignore
        ['基本工资', m(i.baseSalary)], // i18n-ignore
        ['津贴补贴', m(i.allowance)], // i18n-ignore
        ['加班费', m(i.overtimePay)], // i18n-ignore
        ['计件工资', m(i.pieceWage)], // i18n-ignore
        ['销售提成', m(i.commission)], // i18n-ignore
        ['绩效奖金', m(i.perfBonus)], // i18n-ignore
        ['其他奖金', m(i.otherBonus)], // i18n-ignore
        ['考勤扣款', m(i.attendanceDeduct)], // i18n-ignore
        ['应发工资', m(i.grossPay)], // i18n-ignore
        ['个人社保', m(i.socialPersonal)], // i18n-ignore
        ['个人公积金', m(i.housingPersonal)], // i18n-ignore
        ['专项附加扣除', m(i.specialDeduct)], // i18n-ignore
        ['个税', m(i.incomeTax)], // i18n-ignore
        ['其他扣款', m(i.otherDeduct)], // i18n-ignore
        ['实发工资', m(i.netPay)], // i18n-ignore
        ...(i.remark ? [['备注', i.remark, true]] : []) // i18n-ignore
      ]
    })),
    `${periodText(p.period)} 工资条`, // i18n-ignore
    'hr.payslip'
  )
}

function sum(key) {
  const field = { deduct: 'attendanceDeduct', overtime: 'overtimePay', perf: 'perfBonus' }[key] || key
  return (payroll.value?.items || []).reduce((s, i) => s + Number(i[field] || 0), 0)
}

// 生成
const generateOpen = ref(false)
const genPeriod = ref()
function openGenerate() {
  genPeriod.value = Number(dayjs().subtract(dayjs().date() < 15 ? 1 : 0, 'month').format('YYYYMM'))
  generateOpen.value = true
}
async function generate() {
  saving.value = true
  try {
    currentId.value = await generatePayroll(genPeriod.value)
    message.success(t('hr.payroll.generated'))
    generateOpen.value = false
    year.value = Math.floor(genPeriod.value / 100)
    await refresh()
  } finally {
    saving.value = false
  }
}
const regenerate = useAction(() => generatePayroll(payroll.value.period), { successKey: 'hr.payroll.regenerated', reload: () => refresh() })
async function confirm() {
  const submitted = await confirmPayroll(payroll.value.id)
  message.success(submitted ? t('hr.payroll.submitted') : t('hr.payroll.confirmed'))
  refresh()
}
const unconfirm = useAction(() => unconfirmPayroll(payroll.value.id), { successKey: 'hr.payroll.unconfirmed', reload: () => refresh() })
const regenerateAccrual = useAction(() => accruePayroll(payroll.value.id), { successKey: 'hr.payroll.accrued', reload: () => refresh() })
async function remove() {
  await deletePayroll(payroll.value.id)
  message.success(t('common.msg.deleted'))
  currentId.value = undefined
  refresh()
}

// 调整
const adjustOpen = ref(false)
const adjusting = ref()
const adjustForm = reactive({ otherBonus: 0, otherDeduct: 0, remark: '' })
function openAdjust(record) {
  adjusting.value = record
  Object.assign(adjustForm, { otherBonus: Number(record.otherBonus), otherDeduct: Number(record.otherDeduct), remark: record.remark || '' })
  adjustOpen.value = true
}
async function saveAdjust() {
  saving.value = true
  try {
    await adjustPayrollItem(adjusting.value.id, { ...adjustForm, remark: adjustForm.remark || null })
    message.success(t('hr.payroll.adjusted'))
    adjustOpen.value = false
    refresh()
  } finally {
    saving.value = false
  }
}

// 发放
const payOpen = ref(false)
const payRef = ref()
const payForm = reactive({ payDate: undefined, accountId: undefined })
function openPay() {
  payForm.payDate = dayjs().format('YYYY-MM-DD')
  payOpen.value = true
}
async function pay() {
  await payRef.value.validate()
  saving.value = true
  try {
    await payPayroll(payroll.value.id, { ...payForm })
    message.success(t('hr.payroll.paid'))
    payOpen.value = false
    refresh()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.payroll-side :deep(.ant-card-body) {
  overflow: auto;
}
.year {
  flex: 1;
}
.payroll-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
}
.payroll-item:hover {
  background: var(--app-hover);
}
.payroll-item.active {
  background: var(--app-primary-bg);
}
.payroll-item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.title {
  font-size: 16px;
  font-weight: 600;
  margin-right: 8px;
}
.totals {
  margin-top: 16px;
}
.total-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.total-value {
  font-size: 18px;
  font-weight: 600;
}
.total-value.net {
  color: #1677ff;
}
.vouchers {
  margin-top: 8px;
}
.table-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.w220 {
  width: 220px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.strong {
  font-weight: 600;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 12px;
}
</style>
