<template>
  <div class="workspace">
    <a-card :bordered="false" class="page-card workspace-side payroll-side">
      <div class="workspace-side-head">
        <a-select v-model:value="year" :options="years" class="year" @change="loadList" />
        <a-button v-perm="'hr:payroll:add'" type="primary" @click="openGenerate">
          <template #icon><PlusOutlined /></template>生成
        </a-button>
      </div>
      <a-spin :spinning="listLoading">
        <a-empty v-if="!payrolls.length" description="本年还没有工资表" />
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
          <div class="sub-text">{{ p.headcount }} 人 · 实发 {{ formatMoney(p.netTotal) }}</div>
        </div>
      </a-spin>
    </a-card>

    <div class="table-page workspace-main">
      <a-card v-if="!payroll" :bordered="false" class="page-card">
        <a-empty description="选择左侧的工资表，或生成本月工资表" />
      </a-card>
      <template v-else>
        <a-card :bordered="false" class="page-card">
          <div class="title-row">
            <div>
              <span class="title">{{ periodText(payroll.period) }} 工资表</span>
              <DictTag type="hr_payroll_status" :value="payroll.status" />
              <span class="sub-text">{{ payroll.payrollNo }} · 制表 {{ payroll.createByName || '-' }}</span>
            </div>
            <a-space wrap>
              <a-button :disabled="!payroll.items?.length" @click="printSlips">
                <template #icon><PrinterOutlined /></template>打印工资条
              </a-button>
              <template v-if="payroll.status === 'DRAFT'">
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:add')" title="按最新的考勤、绩效、薪资档案重新计算？手工调整会丢失" @confirm="regenerate">
                  <a-button>重新计算</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:confirm')" title="确认后员工可以查看工资条，并生成计提凭证，确定确认？" @confirm="confirm">
                  <a-button type="primary">确认</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:remove')" title="删除该工资表？" @confirm="remove">
                  <a-button danger>删除</a-button>
                </a-popconfirm>
              </template>
              <template v-else-if="payroll.status === 'CONFIRMED'">
                <a-button v-perm="'hr:payroll:pay'" type="primary" @click="openPay">发放</a-button>
                <a-popconfirm v-if="userStore.hasPerm('hr:payroll:confirm')" title="取消确认会删除未审核的计提凭证，确定取消？" @confirm="unconfirm">
                  <a-button>取消确认</a-button>
                </a-popconfirm>
              </template>
              <a-button
                v-if="payroll.status !== 'DRAFT' && !payroll.accrueVoucherId"
                v-perm="'hr:payroll:confirm'"
                @click="regenerateAccrual"
              >
                生成计提凭证
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
            计提凭证：{{ payroll.accrueVoucherNo || '-' }}
            <span v-if="payroll.payVoucherNo">· 发放凭证：{{ payroll.payVoucherNo }}（{{ payroll.payDate }}）</span>
            <span v-if="payroll.confirmByName">· 确认 {{ payroll.confirmByName }} {{ payroll.confirmTime }}</span>
          </div>
        </a-card>

        <a-card :bordered="false" class="page-card table-card">
          <div class="table-head">
            <a-input-search v-model:value="keyword" placeholder="工号或姓名" allow-clear class="w220" @search="loadDetail" />
            <span class="sub-text">日工资 = (基本工资 + 岗位津贴) / 21.75；考勤扣款含旷工、事假、病假 40%、迟到早退每次 50 元</span>
            <span class="sub-text">
              个税按累计预扣法：本年累计应纳税所得额 = 累计（应发 − 个人社保公积金 − 专项附加扣除 − 5000），本月个税 = 累计应纳税额 − 已预扣
            </span>
          </div>
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="itemColumns"
            :data-source="payroll.items"
            :loading="detailLoading"
            :pagination="false"
            :scroll="{ x: 2400, y: '100%' }"
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
                    旷工 {{ Number(record.absentDays) }} 天 · 迟到早退 {{ record.lateCount }} 次 · 事假 {{ Number(record.personalHours) }} 时 · 病假
                    {{ Number(record.sickHours) }} 时
                  </template>
                  <span class="danger">-{{ formatMoney(record.attendanceDeduct) }}</span>
                </a-tooltip>
                <span v-else class="sub-text">0.00</span>
              </template>
              <template v-else-if="column.key === 'overtime'">
                <span v-if="Number(record.overtimePay)">{{ formatMoney(record.overtimePay) }}<div class="sub-text">{{ Number(record.overtimeHours) }} 小时</div></span>
                <span v-else class="sub-text">0.00</span>
              </template>
              <template v-else-if="MONEY_KEYS.includes(column.key)">
                <span :class="{ strong: ['grossPay', 'netPay'].includes(column.key) }">{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="payroll.status === 'DRAFT'" v-perm="'hr:payroll:edit'" type="link" size="small" @click="openAdjust(record)">调整</a-button>
              </template>
            </template>
            <template #summary>
              <a-table-summary fixed>
                <a-table-summary-row>
                  <a-table-summary-cell :index="0" :col-span="2"><b>合计</b></a-table-summary-cell>
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

    <a-modal v-model:open="generateOpen" title="生成工资表" :confirm-loading="saving" @ok="generate">
      <a-alert type="info" show-icon class="mb" message="按薪资档案、当月考勤汇总（旷工、请假、迟到早退、加班）和发放月份为该月的已归档绩效计算；已有草稿时重新计算。" />
      <a-form layout="vertical">
        <a-form-item label="工资月份"><PeriodPicker v-model:value="genPeriod" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="adjustOpen" :title="`调整：${adjusting?.employeeName || ''}`" :confirm-loading="saving" destroy-on-close @ok="saveAdjust">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="其他奖金"><a-input-number v-model:value="adjustForm.otherBonus" :min="0" :precision="2" class="full" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="其他扣款"><a-input-number v-model:value="adjustForm.otherDeduct" :min="0" :precision="2" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注"><a-input v-model:value="adjustForm.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <span class="sub-text">保存后重新计算个税和实发工资</span>
    </a-modal>

    <a-modal v-model:open="payOpen" title="发放工资" :confirm-loading="saving" destroy-on-close @ok="pay">
      <a-alert type="info" show-icon class="mb" :message="`实发 ${formatMoney(payroll?.netTotal)} 元，生成发放凭证：借 应付职工薪酬，贷 银行存款、应交个人所得税、其他应付款`" />
      <a-form ref="payRef" :model="payForm" layout="vertical">
        <a-form-item label="发放日期" name="payDate" :rules="[{ required: true, message: '请选择发放日期' }]">
          <a-date-picker v-model:value="payForm.payDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="付款账户" name="accountId" :rules="[{ required: true, message: '请选择付款账户' }]">
          <AccountSelect v-model:value="payForm.accountId" :prefix="['1001', '1002']" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
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

const userStore = useUserStore()
useDict('hr_payroll_status', 'hr_perf_grade')
const MONEY_KEYS = ['baseSalary', 'allowance', 'pieceWage', 'otherBonus', 'grossPay', 'socialPersonal', 'housingPersonal', 'specialDeduct', 'incomeTax',
  'otherDeduct', 'netPay', 'socialCompany', 'housingCompany', 'cumTaxable', 'cumTax']
const SUM_KEYS = [...MONEY_KEYS.filter((k) => !['cumTaxable', 'cumTax'].includes(k)), 'attendanceDeduct', 'overtime', 'perf']

const thisYear = dayjs().year()
const years = [thisYear, thisYear - 1, thisYear - 2, thisYear - 3, thisYear - 4, thisYear - 5, thisYear - 6, thisYear - 7]
  .map((y) => ({ value: y, label: `${y} 年` }))
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
    { label: '人数', value: p.headcount },
    { label: '应发合计', value: formatMoney(p.grossTotal) },
    { label: '个人社保公积金', value: formatMoney(p.personalTotal) },
    { label: '个人所得税', value: formatMoney(p.taxTotal) },
    { label: '实发合计', value: formatMoney(p.netTotal), cls: 'net' },
    { label: '单位社保公积金', value: formatMoney(p.companyTotal) }
  ]
})

const itemColumns = [
  { title: '员工', key: 'employee', width: 170, fixed: 'left' },
  { title: '出勤 / 工作日', key: 'days', width: 110, align: 'right' },
  { title: '基本工资', key: 'baseSalary', width: 110, align: 'right' },
  { title: '津贴补贴', key: 'allowance', width: 100, align: 'right' },
  { title: '加班费', key: 'overtime', width: 110, align: 'right' },
  { title: '计件工资', key: 'pieceWage', width: 100, align: 'right' },
  { title: '绩效奖金', key: 'perf', width: 140, align: 'right' },
  { title: '其他奖金', key: 'otherBonus', width: 100, align: 'right' },
  { title: '考勤扣款', key: 'deduct', width: 110, align: 'right' },
  { title: '应发工资', key: 'grossPay', width: 120, align: 'right' },
  { title: '个人社保', key: 'socialPersonal', width: 100, align: 'right' },
  { title: '个人公积金', key: 'housingPersonal', width: 100, align: 'right' },
  { title: '专项附加扣除', key: 'specialDeduct', width: 110, align: 'right' },
  { title: '个税', key: 'incomeTax', width: 100, align: 'right' },
  { title: '其他扣款', key: 'otherDeduct', width: 100, align: 'right' },
  { title: '实发工资', key: 'netPay', width: 120, align: 'right' },
  { title: '单位社保', key: 'socialCompany', width: 100, align: 'right' },
  { title: '单位公积金', key: 'housingCompany', width: 100, align: 'right' },
  { title: '累计应纳税所得额', key: 'cumTaxable', width: 140, align: 'right' },
  { title: '累计已扣个税', key: 'cumTax', width: 120, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true },
  { title: '', key: 'action', width: 70, fixed: 'right' }
]
/** 工资条：当前列表（可先按工号或姓名筛选）每人一条 */
function printSlips() {
  const p = payroll.value
  const m = (v) => formatMoney(Number(v || 0))
  printDocs(
    p.items.map((i) => ({
      slip: true,
      title: `${periodText(p.period)} 工资条`,
      fields: [
        ['姓名', i.employeeName],
        ['工号', i.empNo],
        ['部门', i.deptName],
        ['出勤 / 工作日', `${Number(i.shouldDays)} / ${Number(i.monthDays)}`],
        ['基本工资', m(i.baseSalary)],
        ['津贴补贴', m(i.allowance)],
        ['加班费', m(i.overtimePay)],
        ['计件工资', m(i.pieceWage)],
        ['绩效奖金', m(i.perfBonus)],
        ['其他奖金', m(i.otherBonus)],
        ['考勤扣款', m(i.attendanceDeduct)],
        ['应发工资', m(i.grossPay)],
        ['个人社保', m(i.socialPersonal)],
        ['个人公积金', m(i.housingPersonal)],
        ['专项附加扣除', m(i.specialDeduct)],
        ['个税', m(i.incomeTax)],
        ['其他扣款', m(i.otherDeduct)],
        ['实发工资', m(i.netPay)],
        ...(i.remark ? [['备注', i.remark, true]] : [])
      ]
    })),
    `${periodText(p.period)} 工资条`,
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
    message.success('工资表已生成')
    generateOpen.value = false
    year.value = Math.floor(genPeriod.value / 100)
    await refresh()
  } finally {
    saving.value = false
  }
}
async function regenerate() {
  await generatePayroll(payroll.value.period)
  message.success('已重新计算')
  refresh()
}
async function confirm() {
  const submitted = await confirmPayroll(payroll.value.id)
  message.success(submitted ? '已提交审批，审批通过后确认并生成计提凭证' : '已确认，计提凭证已生成')
  refresh()
}
async function unconfirm() {
  await unconfirmPayroll(payroll.value.id)
  message.success('已取消确认')
  refresh()
}
async function regenerateAccrual() {
  await accruePayroll(payroll.value.id)
  message.success('计提凭证已生成')
  refresh()
}
async function remove() {
  await deletePayroll(payroll.value.id)
  message.success('已删除')
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
    message.success('已调整')
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
    message.success('已发放，发放凭证已生成')
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
