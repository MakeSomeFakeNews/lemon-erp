<template>
  <div class="fund-plan">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-segmented v-model:value="unit" :options="UNIT_OPTIONS" />
        <a-select v-model:value="periods" :options="periodOptions" class="periods" />
        <span class="hint">从现金和银行存款的当前余额出发，按到期日汇总未核销的应收应付、未到期票据、待付款报销和计划项</span>
        <span class="spacer" />
        <a-button :loading="loading" @click="load"><template #icon><ReloadOutlined /></template>刷新</a-button>
      </div>

      <div class="stats">
        <div class="stat">
          <div class="stat-label">当前余额（现金 + 银行）</div>
          <div class="stat-value">¥{{ formatMoney(plan?.opening) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">期内预计流入</div>
          <div class="stat-value in">¥{{ formatMoney(sum('totalIn')) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">期内预计流出</div>
          <div class="stat-value out">¥{{ formatMoney(sum('totalOut')) }}</div>
        </div>
        <div class="stat">
          <div class="stat-label">最低余额</div>
          <div :class="['stat-value', { out: minClosing < 0 }]">¥{{ formatMoney(minClosing) }}</div>
          <div v-if="shortages.length" class="stat-sub out">{{ shortages.length }} 期出现资金缺口，最早在 {{ shortages[0].label }}</div>
          <div v-else class="stat-sub">期内资金充足</div>
        </div>
      </div>

      <EChart v-if="plan" :option="chartOption" height="280px" />
    </a-card>

    <a-card :bordered="false" class="page-card" title="资金计划表">
      <template #extra><span class="hint">点击某一期查看明细</span></template>
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
            <a-tag v-if="record.shortage" color="red" :bordered="false" class="gap-tag">资金缺口</a-tag>
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

    <a-card :bordered="false" class="page-card" title="计划项">
      <template #extra>
        <a-space>
          <span class="hint">应收应付之外预计发生的收支，如房租、借款到期、投资款</span>
          <a-button v-perm="'fin:fundPlan:edit'" type="primary" size="small" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增计划项
          </a-button>
        </a-space>
      </template>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="items.list" :loading="items.loading" :pagination="itemPagination" @change="onItemChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'direction'">
            <a-tag :color="record.direction === 'IN' ? 'green' : 'red'" :bordered="false">{{ record.direction === 'IN' ? '流入' : '流出' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'action'">
            <span v-perm="'fin:fundPlan:edit'" class="table-actions">
              <a-button type="link" size="small" @click="drawer.openEdit(record)"><EditOutlined /></a-button>
              <a-popconfirm :title="`删除计划项「${record.description}」？`" @confirm="removeItem(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增计划项" edit-title="修改计划项">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="方向" name="direction">
          <a-radio-group v-model:value="form.direction" button-style="solid">
            <a-radio-button value="IN">流入</a-radio-button>
            <a-radio-button value="OUT">流出</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="预计日期" name="planDate">
          <a-date-picker v-model:value="form.planDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="金额" name="amount">
          <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item label="说明" name="description">
          <a-input v-model:value="form.description" :maxlength="128" placeholder="如：支付房租、收回投资款" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="entriesOpen" :title="`${entriesPeriod?.label || ''} 预计收支明细`" width="min(900px, 100vw)" @close="entriesOpen = false">
      <a-table row-key="key" size="small" :columns="entryColumns" :data-source="entries" :loading="entriesLoading" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'direction'">
            <a-tag :color="record.direction === 'IN' ? 'green' : 'red'" :bordered="false">{{ record.direction === 'IN' ? '流入' : '流出' }}</a-tag>
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
import { today } from '@/components/wms/wms'
import { useFormDrawer } from '@/composables/useFormDrawer'

/** 资金计划：按周 / 月预计资金流入流出与余额，标出资金缺口 */
const UNIT_OPTIONS = [
  { label: '按周', value: 'WEEK' },
  { label: '按月', value: 'MONTH' }
]
/** 与后端 FundSource 一致 */
const SOURCE_LABELS = { AR: '应收账款', AP: '应付账款', NOTE_IN: '应收票据到期', NOTE_OUT: '应付票据到期', CLAIM: '待付报销', PLAN: '计划项' }
const IN_SOURCES = ['AR', 'NOTE_IN', 'PLAN']
const OUT_SOURCES = ['AP', 'NOTE_OUT', 'CLAIM', 'PLAN']

const unit = ref('WEEK')
const periods = ref(12)
const periodOptions = computed(() =>
  (unit.value === 'WEEK' ? [4, 8, 12, 26] : [3, 6, 12]).map((n) => ({ value: n, label: `未来 ${n} ${unit.value === 'WEEK' ? '周' : '个月'}` }))
)
watch(unit, (u) => {
  periods.value = u === 'WEEK' ? 12 : 6
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
  { title: '期间', key: 'label', width: 150 },
  { title: '预计流入', children: IN_SOURCES.map((s) => ({ title: SOURCE_LABELS[s], key: `in.${s}`, align: 'right' })) },
  { title: '预计流出', children: OUT_SOURCES.map((s) => ({ title: SOURCE_LABELS[s], key: `out.${s}`, align: 'right' })) },
  { title: '净流量', key: 'net', align: 'right' },
  { title: '期末余额', key: 'closing', align: 'right' }
]

const chartOption = computed(() => {
  const ps = plan.value?.periods || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预计流入', '预计流出', '期末余额'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: ps.map((p) => p.label) },
    yAxis: [{ type: 'value', name: '收支' }, { type: 'value', name: '余额' }],
    series: [
      { name: '预计流入', type: 'bar', data: ps.map((p) => Number(p.totalIn)), itemStyle: { color: '#52c41a' } },
      { name: '预计流出', type: 'bar', data: ps.map((p) => Number(p.totalOut)), itemStyle: { color: '#ff7875' } },
      {
        name: '期末余额',
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
  { title: '到期日', dataIndex: 'dueDate', width: 110 },
  { title: '方向', key: 'direction', width: 80 },
  { title: '来源', key: 'source', width: 120 },
  { title: '单号', dataIndex: 'refNo', width: 170 },
  { title: '往来单位 / 说明', dataIndex: 'partnerName', ellipsis: true },
  { title: '金额', key: 'amount', width: 140, align: 'right' }
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
  { title: '预计日期', dataIndex: 'planDate', width: 120 },
  { title: '方向', key: 'direction', width: 90 },
  { title: '说明', dataIndex: 'description', ellipsis: true },
  { title: '金额', key: 'amount', width: 150, align: 'right' },
  { title: '操作', key: 'action', width: 100 }
]
const items = reactive({ list: [], total: 0, current: 1, size: 10, loading: false })
const itemPagination = computed(() => ({ current: items.current, pageSize: items.size, total: items.total, showTotal: (t) => `共 ${t} 项` }))
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
  direction: [{ required: true, message: '请选择方向' }],
  planDate: [{ required: true, message: '请选择日期' }],
  amount: [{ required: true, message: '请填写金额' }],
  description: [{ required: true, whitespace: true, message: '请填写说明' }]
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
  message.success('删除成功')
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
  width: 140px;
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
