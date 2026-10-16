<template>
  <SubPage :open="open" :title="acc ? `项目核算 - ${acc.code} ${acc.name}` : '项目核算'" width="min(1000px, 100vw)" destroy-on-close
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
            <div class="chart-title">挣值分析</div>
            <EChart :option="evmChart" height="240px" />
            <div class="sub-text">
              PV 计划价值 = 预算 × 计划进度；EV 挣值 = 预算 × 完工进度；AC 实际成本。
              SPI <a-tag :color="indexTag(acc.spi).color" :bordered="false">{{ indexTag(acc.spi).text }}</a-tag>
              CPI <a-tag :color="indexTag(acc.cpi).color" :bordered="false">{{ indexTag(acc.cpi).text }}</a-tag>
              完工估算 EAC {{ acc.eac == null ? '-' : formatMoney(acc.eac) }}
            </div>
          </a-col>
          <a-col :xs="24" :md="14">
            <div class="chart-title">按月成本</div>
            <EChart v-if="monthly.length" :option="monthlyChart" height="240px" />
            <a-empty v-else description="还没有成本" />
          </a-col>
        </a-row>

        <div class="section-head">
          <span class="chart-title">收款计划</span>
          <a-button v-perm="'pm:billing:edit'" type="primary" size="small" :disabled="acc.status === 'CLOSED'" @click="openBilling()">
            <template #icon><PlusOutlined /></template>新增款项
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
                <a-progress :percent="received(record)" size="small" :format="() => `已收 ${formatMoney(record.settledAmount)}`" class="recv" />
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <span class="table-actions">
                <template v-if="record.status === 'PLANNED'">
                  <a-button v-perm="'pm:billing:bill'" type="link" size="small" @click="openBill(record)">开票</a-button>
                  <a-button v-perm="'pm:billing:edit'" type="link" size="small" @click="openBilling(record)"><EditOutlined /></a-button>
                  <span v-perm="'pm:billing:edit'">
                    <a-popconfirm :title="`确定删除款项「${record.name}」？`" @confirm="removeBilling(record)">
                      <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                    </a-popconfirm>
                  </span>
                </template>
                <span v-else v-perm="'pm:billing:bill'">
                  <a-popconfirm title="取消开票将删除应收单（已收款或已生成凭证的不能取消），确定？" @confirm="unbill(record)">
                    <a-button type="link" size="small">取消开票</a-button>
                  </a-popconfirm>
                </span>
              </span>
            </template>
          </template>
        </a-table>
        <div class="sub-text billing-tip">开票后在「财务管理 / 应收管理」中生成凭证（计入合同负债），在「收款单」中收款核销。</div>

        <div class="section-head">
          <span class="chart-title">项目月结（入总账）</span>
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
    <a-modal v-model:open="billingOpen" :title="billingForm.id ? '编辑款项' : '新增款项'" :confirm-loading="saving" destroy-on-close @ok="saveBilling">
      <a-form ref="billingRef" :model="billingForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="款项名称" name="name" :rules="[{ required: true, whitespace: true, message: '请填写款项名称' }]">
              <a-input v-model:value="billingForm.name" :maxlength="64" placeholder="如 预付款、验收款" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联里程碑" name="taskId">
              <a-select v-model:value="billingForm.taskId" allow-clear :options="milestoneOptions" placeholder="选填" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划开票日期" name="planDate" :rules="[{ required: true, message: '请选择日期' }]">
              <a-date-picker v-model:value="billingForm.planDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="金额（含税）" name="amount" :rules="[{ required: true, message: '请填写金额' }]">
              <a-input-number v-model:value="billingForm.amount" :min="0.01" :precision="2" addon-after="元" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="billingForm.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 开票 -->
    <a-modal v-model:open="billOpen" title="开票" :confirm-loading="saving" destroy-on-close @ok="confirmBill">
      <p>为客户「{{ acc?.customerName }}」生成应收单，金额 {{ formatMoney(billTarget?.amount) }}（含税）。</p>
      <a-form layout="vertical">
        <a-form-item label="开票日期" required>
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
    { label: '完工进度 / 计划进度', value: `${num(a.progress)}% / ${num(a.plannedRate)}%`, cls: num(a.progress) < num(a.plannedRate) ? 'warn' : '' },
    { label: '已确认收入', value: formatMoney(a.revenue), sub: `不含税合同额 ${formatMoney(a.contractNet)}` },
    { label: '实际成本', value: formatMoney(a.actualCost), sub: `人工 ${formatMoney(a.laborCost)} · 费用 ${formatMoney(a.expenseCost)}`,
      cls: num(a.budget) > 0 && num(a.actualCost) > num(a.budget) ? 'danger' : '' },
    { label: '毛利 / 毛利率', value: formatMoney(a.grossProfit), sub: a.grossMargin == null ? '-' : `毛利率 ${a.grossMargin}%`,
      cls: num(a.grossProfit) < 0 ? 'danger' : '' },
    { label: '成本预算', value: formatMoney(a.budget) },
    { label: '合同金额（含税）', value: formatMoney(a.contractAmount) },
    { label: '已开票', value: formatMoney(a.billed) },
    { label: '已收款', value: formatMoney(a.received), sub: num(a.billed) ? `应收余额 ${formatMoney(num(a.billed) - num(a.received))}` : '' }
  ]
})

const revenues = ref([])
const revenueColumns = [
  { title: '期间', dataIndex: 'period', width: 90 },
  { title: '人工入账', key: 'laborCost', width: 120, align: 'right' },
  { title: '费用入账', key: 'expenseCost', width: 120, align: 'right' },
  { title: '完工进度', key: 'completionRate', width: 90, align: 'right' },
  { title: '确认收入', key: 'revenue', width: 120, align: 'right' },
  { title: '结转成本', key: 'cost', width: 120, align: 'right' },
  { title: '凭证', dataIndex: 'voucherNo', width: 100 }
]

const billingColumns = [
  { title: '款项', key: 'name', width: 180 },
  { title: '计划日期', dataIndex: 'planDate', width: 110 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '应收单 / 收款进度', key: 'bill', width: 260 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]

const evmChart = computed(() => {
  const a = acc.value
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => formatMoney(v) },
    grid: { left: 60, right: 16, top: 16, bottom: 30 },
    xAxis: { type: 'category', data: ['预算 BAC', '计划价值 PV', '挣值 EV', '实际成本 AC'] },
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
  legend: { data: ['人工', '费用'], top: 0 },
  grid: { left: 60, right: 16, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: monthly.value.map((m) => periodLabel(m.period)) },
  yAxis: { type: 'value', axisLabel: { formatter: shortMoney } },
  series: [
    { name: '人工', type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => num(m.labor)) },
    { name: '费用', type: 'bar', stack: 'cost', barMaxWidth: 32, data: monthly.value.map((m) => num(m.expense)) }
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
      .map((t) => ({ value: t.id, label: `${t.name}（${t.planEnd}）` }))
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
    message.success('保存成功')
    billingOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function removeBilling(record) {
  await deleteBilling(record.id)
  message.success('删除成功')
  reload()
}

// 开票
const billOpen = ref(false)
const billTarget = ref(null)
const billDate = ref(today())

function openBill(record) {
  if (!acc.value.customerName) {
    message.warning('项目没有客户，不能开票，请先在项目中设置客户')
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
    message.success('已开票，生成应收单')
    billOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

async function unbill(record) {
  await unbillBilling(record.id)
  message.success('已取消开票')
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
