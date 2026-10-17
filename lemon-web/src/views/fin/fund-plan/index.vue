<template>
  <div class="fund-plan">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-segmented v-model:value="unit" :options="UNIT_OPTIONS" />
        <a-select v-model:value="periods" :options="periodOptions" class="periods" />
        <span class="hint">{{ $t('fin.fundPlan.hint') }}</span>
        <span class="spacer" />
        <a-button :loading="loading" @click="load"><template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}</a-button>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">{{ $t('fin.fundPlan.currentBalance') }}</div>
          <div class="stat-value">¥{{ formatMoney(plan?.opening) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('fin.fundPlan.periodIn') }}</div>
          <div class="stat-value in">¥{{ formatMoney(sum('totalIn')) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('fin.fundPlan.periodOut') }}</div>
          <div class="stat-value out">¥{{ formatMoney(sum('totalOut')) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">{{ $t('fin.fundPlan.minBalance') }}</div>
          <div :class="['stat-value', { out: minClosing < 0 }]">¥{{ formatMoney(minClosing) }}</div>
          <div v-if="shortages.length" class="stat-sub out">{{ $t('fin.fundPlan.shortageSummary', { n: shortages.length, first: shortages[0].label }) }}</div>
          <div v-else class="stat-sub">{{ $t('fin.fundPlan.sufficient') }}</div>
        </div>
      </div>

      <EChart v-if="plan" :option="chartOption" height="280px" />
    </a-card>

    <a-card :bordered="false" class="page-card" :title="$t('fin.fundPlan.planTable')">
      <template #extra><span class="hint">{{ $t('fin.fundPlan.clickHint') }}</span></template>
      <a-table
        row-key="label"
        size="middle"
        bordered
        :columns="tableColumns"
        :data-source="plan?.periods || []"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
        :row-class-name="(r) => (r.shortage ? 'shortage-row' : '')"
        :custom-row="(r) => ({ onClick: () => openEntries(r), style: 'cursor: pointer' })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'label'">
            <b>{{ record.label }}</b>
            <a-tag v-if="record.shortage" color="red" :bordered="false" class="gap-tag">{{ $t('fin.fundPlan.shortage') }}</a-tag>
          </template>
          <template v-else-if="column.key.startsWith('in.')">{{ moneyOrBlank(record.inflow[column.key.slice(3)]) }}</template>
          <template v-else-if="column.key.startsWith('out.')">{{ moneyOrBlank(record.outflow[column.key.slice(4)]) }}</template>
          <template v-else-if="column.key === 'net'">
            <span :class="net(record) > 0 ? 'in' : 'out'">{{ moneyOrBlank(net(record)) }}</span>
          </template>
          <template v-else-if="column.key === 'closing'">
            <b :class="{ out: record.closing < 0 }">{{ formatMoney(record.closing) }}</b>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" class="page-card" :title="$t('fin.fundPlan.planItems')">
      <template #extra>
        <a-space>
          <span class="hint">{{ $t('fin.fundPlan.itemHint') }}</span>
          <a-button v-perm="'fin:fundPlan:edit'" type="primary" size="small" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('fin.fundPlan.addItem') }}
          </a-button>
        </a-space>
      </template>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items.list" :loading="items.loading" :pagination="itemPagination" @change="onItemChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'direction'">
            <a-tag :color="record.direction === FundDirection.IN ? 'green' : 'red'" :bordered="false">{{ record.direction === FundDirection.IN ? $t('fin.fundPlan.in') : $t('fin.fundPlan.out') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:fundPlan:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)"><EditOutlined /></a-button>
              <a-popconfirm :title="$t('fin.fundPlan.confirmDeleteItem', { name: record.description })" @confirm="removeItem(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('fin.fundPlan.addItem')" :edit-title="$t('fin.fundPlan.editItem')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('fin.fundPlan.direction')" name="direction">
          <a-radio-group v-model:value="form.direction" button-style="solid">
            <a-radio-button value="IN">{{ $t('fin.fundPlan.in') }}</a-radio-button>
            <a-radio-button value="OUT">{{ $t('fin.fundPlan.out') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('fin.fundPlan.planDate')" name="planDate">
          <a-date-picker v-model:value="form.planDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.amount')" name="amount">
          <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')" name="description">
          <a-input v-model:value="form.description" :maxlength="128" :placeholder="$t('fin.fundPlan.descPlaceholder')" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="entriesOpen" :title="$t('fin.fundPlan.entriesTitle', { period: entriesPeriod?.label || '' })" width="min(900px, 100vw)" @close="entriesOpen = false">
      <a-table row-key="key" size="small" :columns="entryColumns" :data-source="entries" :loading="entriesLoading" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'direction'">
            <a-tag :color="record.direction === FundDirection.IN ? 'green' : 'red'" :bordered="false">{{ record.direction === FundDirection.IN ? $t('fin.fundPlan.in') : $t('fin.fundPlan.out') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'source'">{{ SOURCE_LABELS[record.source] }}</template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
        </template>
      </a-table>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import {
  addFundPlanItem,
  deleteFundPlanItem,
  fundPlan,
  fundPlanEntries,
  pageFundPlanItem,
  updateFundPlanItem
} from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { t } from '@/locales'
import { today } from '@/components/wms/wms'
import { useFormDrawer } from '@/composables/useFormDrawer'

import { FundDirection, FundSource, PlanUnit } from '@/constants/enums'
/** 资金计划：按周 / 月预计资金流入流出与余额，标出资金缺口 */
const UNIT_OPTIONS = [
  { label: t('fin.fundPlan.byWeek'), value: 'WEEK' },
  { label: t('fin.fundPlan.byMonth'), value: 'MONTH' }
]
/** 与后端 FundSource 一致 */
const SOURCE_LABELS = {
  AR: t('fin.fundPlan.source.AR'),
  AP: t('fin.fundPlan.source.AP'),
  NOTE_IN: t('fin.fundPlan.source.NOTE_IN'),
  NOTE_OUT: t('fin.fundPlan.source.NOTE_OUT'),
  CLAIM: t('fin.fundPlan.source.CLAIM'),
  PLAN: t('fin.fundPlan.source.PLAN')
}
const IN_SOURCES = [FundSource.AR, FundSource.NOTE_IN, FundSource.PLAN]
const OUT_SOURCES = [FundSource.AP, FundSource.NOTE_OUT, FundSource.CLAIM, FundSource.PLAN]

const unit = ref('WEEK')
const periods = ref(12)
const periodOptions = computed(() =>
  (unit.value === PlanUnit.WEEK ? [4, 8, 12, 26] : [3, 6, 12]).map((n) => ({ value: n, label: unit.value === PlanUnit.WEEK ? t('fin.fundPlan.nextWeeks', { n }) : t('fin.fundPlan.nextMonths', { n }) }))
)
watch(unit, (u) => {
  periods.value = u === PlanUnit.WEEK ? 12 : 6
})

const plan = ref()
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    plan.value = await fundPlan({ unit: unit.value, periods: periods.value })
  } finally {
    loading.value = false
  }
}
watch([unit, periods], load)

const sum = (key) => (plan.value?.periods || []).reduce((s, p) => s + Number(p[key] || 0), 0)
const net = (p) => Number(p.totalIn) - Number(p.totalOut)
const minClosing = computed(() => Math.min(Number(plan.value?.opening || 0), ...(plan.value?.periods || []).map((p) => Number(p.closing))))
const shortages = computed(() => (plan.value?.periods || []).filter((p) => p.shortage))

// 金额列不设宽度，按内容自适应铺满；窄屏时横向滚动
const tableColumns = [
  { title: t('fin.fundPlan.period'), key: 'label', width: 150 },
  { title: t('fin.fundPlan.expectedIn'), children: IN_SOURCES.map((s) => ({ title: SOURCE_LABELS[s], key: `in.${s}`, align: 'right' })) },
  { title: t('fin.fundPlan.expectedOut'), children: OUT_SOURCES.map((s) => ({ title: SOURCE_LABELS[s], key: `out.${s}`, align: 'right' })) },
  { title: t('fin.fundPlan.net'), key: 'net', align: 'right' },
  { title: t('fin.fundPlan.closing'), key: 'closing', align: 'right' }
]

const chartOption = computed(() => {
  const ps = plan.value?.periods || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: [t('fin.fundPlan.expectedIn'), t('fin.fundPlan.expectedOut'), t('fin.fundPlan.closing')] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: ps.map((p) => p.label) },
    yAxis: [{ type: 'value', name: t('fin.fundPlan.inOut') }, { type: 'value', name: t('fin.fundPlan.balance') }],
    series: [
      { name: t('fin.fundPlan.expectedIn'), type: 'bar', data: ps.map((p) => Number(p.totalIn)), itemStyle: { color: '#52c41a' } },
      { name: t('fin.fundPlan.expectedOut'), type: 'bar', data: ps.map((p) => Number(p.totalOut)), itemStyle: { color: '#ff7875' } },
      {
        name: t('fin.fundPlan.closing'),
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: ps.map((p) => Number(p.closing)),
        markLine: { silent: true, symbol: 'none', data: [{ yAxis: 0 }], lineStyle: { color: '#d03b3b', type: 'dashed' } }
      }
    ]
  }
})

// ---------------------------------------------------------------- 明细
const entryColumns = [
  { title: t('fin.fundPlan.dueDate'), dataIndex: 'dueDate', width: 110 },
  { title: t('fin.fundPlan.direction'), key: 'direction', width: 90 },
  { title: t('fin.fundPlan.source.title'), key: 'source', width: 160 },
  { title: t('fin.fundPlan.refNo'), dataIndex: 'refNo', width: 170 },
  { title: t('fin.fundPlan.partnerOrDesc'), dataIndex: 'partnerName', ellipsis: true },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' }
]
const entriesOpen = ref(false)
const entriesPeriod = ref()
const entries = ref([])
const entriesLoading = ref(false)
async function openEntries(p) {
  entriesPeriod.value = p
  entriesOpen.value = true
  entriesLoading.value = true
  try {
    const list = await fundPlanEntries({ from: p.start || undefined, to: p.end })
    entries.value = list.map((e, i) => ({ ...e, key: i }))
  } finally {
    entriesLoading.value = false
  }
}

// ---------------------------------------------------------------- 计划项
const itemColumns = [
  { title: t('fin.fundPlan.planDate'), dataIndex: 'planDate', width: 130 },
  { title: t('fin.fundPlan.direction'), key: 'direction', width: 90 },
  { title: t('common.field.description'), dataIndex: 'description', ellipsis: true },
  { title: t('common.field.amount'), key: 'amount', width: 150, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 100 }
]
const items = reactive({ list: [], total: 0, current: 1, size: 10, loading: false })
const itemPagination = computed(() => ({ current: items.current, pageSize: items.size, total: items.total, showTotal: (total) => t('common.table.total', { total }) }))
async function loadItems() {
  items.loading = true
  try {
    const res = await pageFundPlanItem({ current: items.current, size: items.size, begin: today() })
    items.list = res.records
    items.total = res.total
  } finally {
    items.loading = false
  }
}
function onItemChange(p) {
  items.current = p.current
  items.size = p.pageSize
  loadItems()
}
const rules = {
  direction: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.fundPlan.direction') }) }],
  planDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.date') }) }],
  amount: [{ required: true, message: t('common.rule.required', { name: t('common.field.amount') }) }],
  description: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('common.field.description') }) }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, direction: 'OUT', planDate: today(), amount: undefined, description: '' }),
  create: addFundPlanItem,
  update: updateFundPlanItem,
  toPayload: ({ direction, planDate, amount, description }) => ({ direction, planDate, amount, description }),
  onSuccess: () => {
    loadItems()
    load()
  }
})
const { formRef, form } = drawer
async function removeItem(id) {
  await deleteFundPlanItem(id)
  message.success(t('common.msg.deleteSuccess'))
  loadItems()
  load()
}

onMounted(() => {
  load()
  loadItems()
})
</script>

<style scoped>
.fund-plan {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.fund-plan .page-card + .page-card {
  margin-top: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.periods {
  width: 170px;
}
.spacer {
  flex: 1;
}
.hint {
  color: var(--app-text-3);
  font-size: 12px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 8px;
}
.stat {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--app-hover);
}
.stat-label {
  font-size: 13px;
  color: var(--app-text-3);
}
.stat-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text);
}
.stat-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--app-text-3);
}
.in {
  color: #389e0d;
}
.out {
  color: var(--status-critical) !important;
}
.gap-tag {
  margin-left: 6px;
}
.full {
  width: 100%;
}
.fund-plan :deep(.shortage-row > td) {
  background: rgba(208, 59, 59, 0.06);
}
</style>
