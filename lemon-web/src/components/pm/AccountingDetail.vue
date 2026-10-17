<template>
  <SubPage :open="open" :title="acc ? $t('pm.acc.titleWith', { code: acc.code, name: acc.name }) : $t('pm.acc.title')" width="min(1000px, 100vw)" destroy-on-close
            @close="emit('update:open', false)">
    <a-spin :spinning="loading">
      <template v-if="acc">
        <a-row :gutter="[16, 16]" class="block">
          <a-col v-for="s in stats" :key="s.label" :xs="12" :md="6">
            <div class="stat">
              <div class="stat-title">{{ s.label }}</div>
              <div class="stat-value" :class="s.cls">{{ s.value }}</div>
              <div v-if="s.sub" class="sub-text">{{ s.sub }}</div>
            </div>
          </a-col>
        </a-row>

        <a-row :gutter="16" class="block">
          <a-col :xs="24" :md="10">
            <div class="chart-title">{{ $t('pm.acc.evm') }}</div>
            <EChart :option="evmChart" height="240px" />
            <div class="sub-text">
              {{ $t('pm.acc.evmTip') }}
              SPI <a-tag :color="indexTag(acc.spi).color" :bordered="false">{{ indexTag(acc.spi).text }}</a-tag>
              CPI <a-tag :color="indexTag(acc.cpi).color" :bordered="false">{{ indexTag(acc.cpi).text }}</a-tag>
              {{ $t('pm.acc.eac') }} {{ acc.eac == null ? '-' : formatMoney(acc.eac) }}
            </div>
          </a-col>
          <a-col :xs="24" :md="14">
            <div class="chart-title">{{ $t('pm.acc.monthlyCost') }}</div>
            <EChart v-if="monthly.length" :option="monthlyChart" height="240px" />
            <a-empty v-else :description="$t('pm.acc.noCost')" />
          </a-col>
        </a-row>

        <div class="section-head">
          <span class="chart-title">{{ $t('pm.acc.billingPlan') }}</span>
          <a-button v-perm="'pm:billing:edit'" type="primary" size="small" :disabled="acc.status === 'CLOSED'" @click="openBilling()">
            <template #icon><PlusOutlined /></template>{{ $t('pm.acc.addBilling') }}
          </a-button>
        </div>
        <a-table row-key="id" size="small" :columns="billingColumns" :data-source="billings" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div>{{ record.name }}</div>
              <div v-if="record.taskName" class="sub-text"><FlagFilled class="milestone" />{{ record.taskName }}</div>
            </template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'status'"><DictTag type="pm_billing_status" :value="record.status" /></template>
            <template v-else-if="column.key === 'bill'">
              <template v-if="record.billNo">
                <div>{{ record.billNo }} <span class="sub-text">{{ record.billDate }}</span></div>
                <a-progress :percent="received(record)" size="small" :format="() => `${$t('pm.common.received')} ${formatMoney(record.settledAmount)}`" class="recv" />
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <template v-if="record.status === 'PLANNED'">
                  <a-button v-perm="'pm:billing:bill'" type="link" size="small" @click="openBill(record)">{{ $t('pm.acc.bill') }}</a-button>
                  <a-button v-perm="'pm:billing:edit'" type="link" size="small" @click="openBilling(record)"><EditOutlined /></a-button>
                  <span v-perm="'pm:billing:edit'">
                    <a-popconfirm :title="$t('pm.acc.confirmDeleteBilling', { name: record.name })" @confirm="removeBilling(record)">
                      <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </span>
                </template>
                <span v-else v-perm="'pm:billing:bill'">
                  <a-popconfirm :title="$t('pm.acc.confirmUnbill')" @confirm="unbill(record)">
                    <a-button type="link" size="small">{{ $t('pm.acc.unbill') }}</a-button>
                  </a-popconfirm>
                </span>
              </span>
            </template>
          </template>
        </a-table>
        <div class="sub-text billing-tip">{{ $t('pm.acc.billingTip') }}</div>

        <div class="section-head">
          <span class="chart-title">{{ $t('pm.acc.monthClose') }}</span>
        </div>
        <a-table row-key="id" size="small" :columns="revenueColumns" :data-source="revenues" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="['laborCost', 'expenseCost', 'revenue', 'cost'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
            <template v-else-if="column.key === 'completionRate'">{{ record.completionRate == null ? '-' : `${num(record.completionRate)}%` }}</template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <!-- 收款计划 -->
    <a-modal v-model:open="billingOpen" :title="billingForm.id ? $t('pm.acc.editBilling') : $t('pm.acc.addBilling')" :confirm-loading="saving" destroy-on-close @ok="saveBilling">
      <a-form ref="billingRef" :model="billingForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('pm.acc.billingName')" name="name" :rules="[{ required: true, whitespace: true, message: $t('common.rule.required', { name: $t('pm.acc.billingName') }) }]">
              <a-input v-model:value="billingForm.name" :maxlength="64" :placeholder="$t('pm.acc.billingNamePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.acc.linkedMilestone')" name="taskId">
              <a-select v-model:value="billingForm.taskId" allow-clear :options="milestoneOptions" :placeholder="$t('pm.common.optional')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.acc.planBillDate')" name="planDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('common.field.date') }) }]">
              <a-date-picker v-model:value="billingForm.planDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('pm.acc.amountTax')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('common.field.amount') }) }]">
              <a-input-number v-model:value="billingForm.amount" :min="0.01" :precision="2" :addon-after="$t('pm.common.yuan')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="billingForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 开票 -->
    <a-modal v-model:open="billOpen" :title="$t('pm.acc.bill')" :confirm-loading="saving" destroy-on-close @ok="confirmBill">
      <p>{{ $t('pm.acc.billConfirm', { customer: acc?.customerName, amount: formatMoney(billTarget?.amount) }) }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('pm.acc.billDate')" required>
          <a-date-picker v-model:value="billDate" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, FlagFilled, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import {
  accountingMonthly, accountingRevenue, addBilling, billBilling, deleteBilling, getAccounting, listBillings, taskTree, unbillBilling, updateBilling
} from '@/api/pm'
import { formatMoney } from '@/components/scm/scm'
import { periodLabel, shortMoney } from '@/components/charts/charts'
import { flattenTasks, indexTag, num, today } from './pm'
import { t } from '@/locales'

/** 项目核算详情：收入、成本、毛利、挣值、按月成本、收款计划与开票 */
const props = defineProps({
  open: Boolean,
  projectId: Number
})
const emit = defineEmits(['update:open', 'changed'])

const loading = ref(false)
const acc = ref(null)
const monthly = ref([])
const billings = ref([])
const milestoneOptions = ref([])

const stats = computed(() => {
  const a = acc.value
  return [
    { label: t('pm.detail.progressVsPlan'), value: `${num(a.progress)}% / ${num(a.plannedRate)}%`, cls: num(a.progress) < num(a.plannedRate) ? 'warn' : '' },
    { label: t('pm.acc.revenue'), value: formatMoney(a.revenue), sub: `${t('pm.acc.contractNet')} ${formatMoney(a.contractNet)}` },
    { label: t('pm.acc.actualCost'), value: formatMoney(a.actualCost), sub: `${t('pm.common.labor')} ${formatMoney(a.laborCost)} · ${t('pm.common.expense')} ${formatMoney(a.expenseCost)}`,
      cls: num(a.budget) > 0 && num(a.actualCost) > num(a.budget) ? 'danger' : '' },
    { label: t('pm.acc.profitMargin'), value: formatMoney(a.grossProfit), sub: a.grossMargin == null ? '-' : `${t('pm.acc.margin')} ${a.grossMargin}%`,
      cls: num(a.grossProfit) < 0 ? 'danger' : '' },
    { label: t('pm.acc.costBudget'), value: formatMoney(a.budget) },
    { label: t('pm.project.contractAmountTax'), value: formatMoney(a.contractAmount) },
    { label: t('pm.acc.billed'), value: formatMoney(a.billed) },
    { label: t('pm.common.received'), value: formatMoney(a.received), sub: num(a.billed) ? `${t('pm.acc.arBalance')} ${formatMoney(num(a.billed) - num(a.received))}` : '' }
  ]
})

const revenues = ref([])
const revenueColumns = [
  { title: t('pm.acc.period'), dataIndex: 'period', width: 90 },
  { title: t('pm.acc.laborPosted'), key: 'laborCost', width: 120, align: 'right' },
  { title: t('pm.acc.expensePosted'), key: 'expenseCost', width: 130, align: 'right' },
  { title: t('pm.acc.completion'), key: 'completionRate', width: 110, align: 'right' },
  { title: t('pm.acc.recognizedRevenue'), key: 'revenue', width: 130, align: 'right' },
  { title: t('pm.acc.carriedCost'), key: 'cost', width: 120, align: 'right' },
  { title: t('pm.acc.voucher'), dataIndex: 'voucherNo', width: 100 }
]

const billingColumns = [
  { title: t('pm.common.billingItem'), key: 'name', width: 180 },
  { title: t('pm.common.planDate'), dataIndex: 'planDate', width: 120 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('pm.acc.billProgress'), key: 'bill', width: 260 },
  { title: t('common.action.operation'), key: 'action', width: 170, fixed: 'right' }
]

const evmChart = computed(() => {
  const a = acc.value
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => formatMoney(v) },
    grid: { left: 60, right: 16, top: 16, bottom: 30 },
    xAxis: { type: 'category', data: [t('pm.acc.bac'), t('pm.acc.pv'), t('pm.acc.ev'), t('pm.acc.ac')] },
    yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 36,
        data: [
          { value: num(a.budget), itemStyle: { color: '#8c8c8c' } },
          { value: num(a.pv), itemStyle: { color: '#1677ff' } },
          { value: num(a.ev), itemStyle: { color: '#52c41a' } },
          { value: num(a.actualCost), itemStyle: { color: num(a.actualCost) > num(a.ev) ? '#f5222d' : '#faad14' } }
        ],
        label: { show: true, position: 'top', formatter: (p) => shortMoney(p.value) }
      }
    ]
  }
})

const monthlyChart = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: [t('pm.common.labor'), t('pm.common.expense')], top: 0 },
  grid: { left: 60, right: 16, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: monthly.value.map((m) => periodLabel(m.period)) },
  yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
  series: [
    { name: t('pm.common.labor'), type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => num(m.labor)) },
    { name: t('pm.common.expense'), type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => num(m.expense)) }
  ]
}))

const received = (b) => (num(b.amount) ? Math.min(Math.round((num(b.settledAmount) * 100) / num(b.amount)), 100) : 0)

async function load() {
  loading.value = true
  try {
    const [a, m, b, tree, r] = await Promise.all([
      getAccounting(props.projectId), accountingMonthly(props.projectId), listBillings(props.projectId), taskTree(props.projectId),
      accountingRevenue(props.projectId)
    ])
    revenues.value = r
    acc.value = a
    monthly.value = m
    billings.value = b
    milestoneOptions.value = flattenTasks(tree)
      .filter((t) => t.milestone === 1)
      .map((task) => ({ value: task.id, label: `${task.name} (${task.planEnd})` }))
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.projectId],
  ([open]) => open && props.projectId && load(),
  { immediate: true }
)

async function reload() {
  await load()
  emit('changed')
}

// 收款计划
const billingOpen = ref(false)
const billingRef = ref()
const billingForm = ref({})
const saving = ref(false)

function openBilling(record) {
  billingForm.value = record
    ? { id: record.id, name: record.name, taskId: record.taskId || undefined, planDate: record.planDate, amount: num(record.amount), remark: record.remark }
    : { id: undefined, name: '', taskId: undefined, planDate: today(), amount: undefined, remark: undefined }
  billingOpen.value = true
}

async function saveBilling() {
  try {
    await billingRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const { id, ...data } = billingForm.value
    const payload = { ...data, projectId: props.projectId, taskId: data.taskId || null, remark: data.remark || null }
    id ? await updateBilling(id, payload) : await addBilling(payload)
    message.success(t('common.msg.saveSuccess'))
    billingOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function removeBilling(record) {
  await deleteBilling(record.id)
  message.success(t('common.msg.deleteSuccess'))
  reload()
}

// 开票
const billOpen = ref(false)
const billTarget = ref(null)
const billDate = ref(today())

function openBill(record) {
  if (!acc.value.customerName) {
    message.warning(t('pm.acc.noCustomer'))
    return
  }
  billTarget.value = record
  billDate.value = today()
  billOpen.value = true
}

async function confirmBill() {
  saving.value = true
  try {
    await billBilling(billTarget.value.id, billDate.value)
    message.success(t('pm.acc.billed2'))
    billOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function unbill(record) {
  await unbillBilling(record.id)
  message.success(t('pm.acc.unbilled'))
  reload()
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
.stat-value.warn {
  color: #fa8c16;
}
.chart-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.milestone {
  color: #faad14;
  margin-right: 4px;
}
.recv {
  width: 220px;
  margin: 0;
}
.billing-tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
