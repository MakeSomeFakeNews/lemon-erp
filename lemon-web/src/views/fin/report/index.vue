<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-radio-group v-model:value="type" button-style="solid" @change="load">
          <a-radio-button value="balance">{{ TITLES.balance }}</a-radio-button>
          <a-radio-button value="income">{{ TITLES.income }}</a-radio-button>
          <a-radio-button value="cash">{{ TITLES.cash }}</a-radio-button>
        </a-radio-group>
        <template v-if="type === 'cash'">
          <PeriodPicker v-model:value="beginPeriod" :placeholder="$t('fin.report.beginPeriod')" @update:value="load" />
          <span>{{ $t('fin.report.to') }}</span>
        </template>
        <PeriodPicker v-model:value="period" @update:value="load" />
        <a-checkbox v-if="type !== 'cash'" v-model:checked="hideZero">{{ $t('fin.report.hideZero') }}</a-checkbox>
        <div class="toolbar-right">
          <a-button :disabled="!ready" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
          <a-button :disabled="!ready" @click="exportExcel"><template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}</a-button>
        </div>
      </div>

      <div ref="scrollEl" class="report-scroll">
        <a-spin :spinning="loading">
          <div v-if="metrics.length" class="metrics">
            <a-tooltip v-for="m in metrics" :key="m.name" :title="m.hint">
              <div class="metric">
                <div class="metric-name">{{ m.name }}</div>
                <div class="metric-value">{{ metricText(m.value, m.unit) }}</div>
                <div class="metric-sub">{{ type === 'balance' ? $t('fin.report.yearBegin') : $t('fin.report.ytd') }} {{ metricText(m.value2, m.unit) }}</div>
              </div>
            </a-tooltip>
          </div>

          <div class="paper">
            <div class="paper-title">{{ TITLES[type] }}</div>
            <div class="paper-meta">
              <span>{{ $t('fin.report.preparedByUnit', { name: companyName }) }}</span>
              <span>{{ subTitle }}</span>
              <span>{{ formNo ? `${formNo} · ` : '' }}{{ $t('fin.report.unitYuan') }}</span>
            </div>
            <a-alert v-if="type === 'balance' && report && !report.balanced" type="error" show-icon class="warn"
              :message="$t('fin.report.unbalanced')" />
            <a-alert v-if="type === 'cash' && cash && !cash.balanced" type="warning" show-icon class="warn"
              :message="$t('fin.report.cashMismatch', { amount: formatMoney(cash.closingBalance) })" />

            <a-table
              :row-key="(r, i) => i"
              size="small"
              bordered
              :columns="tableColumns"
              :data-source="tableRows"
              :pagination="false"
              :scroll="{ x: type === 'balance' ? 1100 : 760 }"
              :sticky="{ getContainer: () => scrollEl }"
              class="report-table"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.name">
                  <span v-if="record[column.side]" :class="['item', `lv${record[column.side].level}`, { bold: record[column.side].bold }]">
                    {{ record[column.side].name }}
                  </span>
                </template>
                <template v-else-if="column.line">
                  <span class="line-no">{{ record[column.side]?.lineNo ?? '' }}</span>
                </template>
                <template v-else-if="column.index != null">
                  <template v-if="record[column.side] && !record[column.side].title">
                    <a
                      v-if="drillable(record[column.side])"
                      :class="['num', 'drill', { neg: valueOf(record[column.side], column.index) < 0, bold: record[column.side].bold }]"
                      @click="openDrill(record[column.side])"
                    >{{ amountText(record[column.side], column.index) }}</a>
                    <span v-else :class="['num', { neg: valueOf(record[column.side], column.index) < 0, bold: record[column.side].bold }]">
                      {{ amountText(record[column.side], column.index) }}
                    </span>
                  </template>
                </template>
              </template>
            </a-table>
            <div class="paper-foot">
              <span>{{ $t('fin.report.headOfUnitColon') }}</span><span>{{ $t('fin.report.chiefAccountantColon') }}</span><span>{{ $t('fin.report.preparerColon', { name: userStore.displayName }) }}</span>
            </div>
            <div class="sub-text tip">
              {{ $t('fin.report.postedOnly', { tip: TIPS[type] }) }}{{ type !== 'cash' ? $t('fin.report.drillTip') : '' }}
            </div>
          </div>

          <div v-if="type === 'income' && trend.length > 1" class="trend">
            <div class="trend-title">{{ $t('fin.report.trendTitle', { year: String(period).slice(0, 4) }) }}</div>
            <EChart :option="trendOption" height="280px" />
          </div>
        </a-spin>
      </div>
    </a-card>
    <ReportDrill v-model:open="drillOpen" :row="drillRow" :columns="report?.columns || []" :begin="drillBegin" :end="period" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import ReportDrill from '@/components/fin/ReportDrill.vue'
import { balanceSheet, cashFlow, incomeStatement, incomeTrend } from '@/api/fin'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'
import { useUserStore } from '@/store/user'
import { writeExcel } from '@/utils/excel'
import { defaultCompanyName, printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'

/**
 * 财务报表：资产负债表（左右两栏）、利润表（本月、本年累计、上年同期）、现金流量表（直接法）。
 * 格式参照《小企业会计准则》，带行次和财务指标；点击金额追溯到构成科目、明细账和凭证
 */
const userStore = useUserStore()
const { t } = useI18n()
const companyName = defaultCompanyName()
const type = ref('balance')
const period = ref(currentPeriod())
const beginPeriod = ref(Number(`${String(currentPeriod()).slice(0, 4)}01`))
const hideZero = ref(false)
const loading = ref(false)
const report = ref()
const cash = ref()
const trend = ref([])
const scrollEl = ref()

const TITLES = { balance: t('fin.report.balanceSheet'), income: t('fin.report.incomeStatement'), cash: t('fin.report.cashFlow') }
const TIPS = {
  balance: t('fin.report.tipBalance'),
  income: t('fin.report.tipIncome'),
  cash: t('fin.report.tipCash')
}

const ready = computed(() => (type.value === 'cash' ? !!cash.value : !!report.value))
const formNo = computed(() => (type.value === 'cash' ? t('fin.report.cashFormNo') : report.value?.formNo))
const metrics = computed(() => (type.value === 'cash' ? [] : report.value?.metrics || []))
const subTitle = computed(() => {
  if (type.value === 'balance') return t('fin.report.periodEnd', { period: periodText(period.value) })
  if (type.value === 'cash') return beginPeriod.value === period.value ? periodText(period.value) : t('fin.report.periodRange', { begin: periodText(beginPeriod.value), end: periodText(period.value) })
  return periodText(period.value)
})

// ---------------------------------------------------------------- 表格

/** 现金流量表转为与其他报表相同的行结构，按顺序编行次 */
const cashRows = computed(() => {
  let no = 1
  return (cash.value?.lines || []).map((l) => {
    const title = l.amount === null || l.amount === undefined
    return { name: l.name, level: l.level, bold: l.bold, title, lineNo: title ? null : no++, values: title ? null : [l.amount] }
  })
})
const isZeroRow = (r) => !r.title && r.level > 0 && !r.bold && (r.values || []).every((v) => !Number(v))
const visible = (rows) => (hideZero.value ? rows.filter((r) => !isZeroRow(r)) : rows)

const tableRows = computed(() => {
  if (type.value === 'cash') return cashRows.value.map((r) => ({ l: r }))
  if (!report.value) return []
  const left = visible(report.value.rows)
  if (type.value !== 'balance') return left.map((r) => ({ l: r }))
  const right = visible(report.value.right)
  return Array.from({ length: Math.max(left.length, right.length) }, (_, i) => ({ l: left[i], r: right[i] }))
})

const amountColumns = (side, names) => names.map((c, i) => ({ title: c, key: `${side}${i}`, side, index: i, width: 150, align: 'right' }))
const tableColumns = computed(() => {
  if (type.value === 'cash') {
    return [{ title: t('fin.report.item'), key: 'lname', side: 'l', name: true }, { title: t('fin.report.lineNo'), key: 'lline', side: 'l', line: true, width: 70, align: 'center' },
      ...amountColumns('l', [t('common.field.amount')])]
  }
  const names = report.value?.columns || []
  const half = (side, title) => [
    { title, key: `${side}name`, side, name: true },
    { title: t('fin.report.lineNo'), key: `${side}line`, side, line: true, width: 64, align: 'center' },
    ...amountColumns(side, names)
  ]
  return type.value === 'balance' ? [...half('l', t('fin.report.assets')), ...half('r', t('fin.report.liabilitiesEquity'))] : half('l', t('fin.report.item'))
})

const valueOf = (row, i) => Number(row.values?.[i] || 0)
/** 合计行显示 0.00，项目行为零时留空 */
function amountText(row, i) {
  const v = row.values?.[i]
  if (!Number(v) && !row.bold) return ''
  return formatMoney(v ?? 0)
}
const metricText = (v, unit) => (v === null || v === undefined ? '-' : `${Number(v).toFixed(2)}${unit === '%' ? '%' : t('fin.report.timesSuffix')}`)

// ---------------------------------------------------------------- 追溯

const drillOpen = ref(false)
const drillRow = ref()
/** 明细账从本年 1 月查到报表期间 */
const drillBegin = computed(() => Number(`${String(period.value).slice(0, 4)}01`))
const drillable = (row) => type.value !== 'cash' && !!row.formula
function openDrill(row) {
  drillRow.value = row
  drillOpen.value = true
}

// ---------------------------------------------------------------- 利润趋势

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis', valueFormatter: (v) => `¥${formatMoney(v)}` },
  legend: { top: 0 },
  grid: { left: 60, right: 20, top: 36, bottom: 28 },
  xAxis: { type: 'category', data: trend.value.map((x) => t('fin.report.monthLabel', { m: Number(String(x.period).slice(4)) })) },
  yAxis: { type: 'value' },
  series: [
    { name: t('fin.report.revenue'), type: 'bar', data: trend.value.map((t) => Number(t.revenue)), barMaxWidth: 28 },
    { name: t('fin.report.cost'), type: 'bar', data: trend.value.map((t) => Number(t.cost)), barMaxWidth: 28 },
    { name: t('fin.report.operatingProfit'), type: 'line', data: trend.value.map((t) => Number(t.operating)), smooth: true },
    { name: t('fin.report.netProfit'), type: 'line', data: trend.value.map((t) => Number(t.net)), smooth: true }
  ]
}))

// ---------------------------------------------------------------- 加载

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    if (type.value === 'cash') {
      if (beginPeriod.value > period.value) beginPeriod.value = period.value
      const res = await cashFlow(beginPeriod.value, period.value)
      if (current === seq) cash.value = res
    } else if (type.value === 'balance') {
      const res = await balanceSheet(period.value)
      if (current === seq) report.value = res
    } else {
      const [res, t] = await Promise.all([incomeStatement(period.value), incomeTrend(period.value)])
      if (current === seq) {
        report.value = res
        trend.value = t
      }
    }
  } finally {
    if (current === seq) loading.value = false
  }
}
onMounted(load)

// ---------------------------------------------------------------- 打印、导出

/** 打印 / 导出用的列：{ title, id（打印模板匹配用，右栏加「（右）」区分同名列）, value(row), align, width(mm) } */
function flatColumns() {
  const balance = type.value === 'balance'
  // A4 纵向可用约 186mm：资产负债表两栏各 93mm，其他报表一栏
  const widths = balance ? { name: 33, line: 8, amount: 26 } : { name: type.value === 'cash' ? 126 : 84, line: 12, amount: type.value === 'cash' ? 48 : 30 }
  return tableColumns.value.map((c) => {
    const id = c.side === 'r' ? t('fin.report.rightColumnId', { title: c.title }) : c.title
    if (c.name) return { title: c.title, id, width: widths.name, value: (r) => (r[c.side] ? `${'　'.repeat(r[c.side].level)}${r[c.side].name}` : '') }
    if (c.line) return { title: c.title, id, width: widths.line, align: 'center', value: (r) => r[c.side]?.lineNo ?? '' }
    return { title: c.title, id, width: widths.amount, align: 'right', value: (r) => (r[c.side] && !r[c.side].title ? amountText(r[c.side], c.index) : '') }
  })
}
const printKey = { balance: 'fin.balanceSheet', income: 'fin.incomeStatement', cash: 'fin.cashFlow' }

function print() {
  printDocs({
    title: TITLES[type.value],
    fields: [
      [t('fin.report.preparedBy'), companyName],
      [t('fin.report.period'), subTitle.value],
      [t('fin.report.unit'), formNo.value ? t('fin.report.yuanWithForm', { form: formNo.value }) : t('fin.report.yuan')]
    ],
    columns: flatColumns(),
    rows: tableRows.value,
    signs: [t('fin.report.headOfUnit'), t('fin.report.chiefAccountant'), [t('fin.report.preparer'), userStore.displayName]]
  }, `${TITLES[type.value]} ${subTitle.value}`, printKey[type.value])
}

function exportExcel() {
  const cols = flatColumns()
  const rows = tableRows.value.map((r) => cols.map((c) => {
    const v = c.value(r)
    return c.align === 'right' && v !== '' ? Number(String(v).replace(/,/g, '')) : v
  }))
  writeExcel(`${TITLES[type.value]}_${subTitle.value}`, cols.map((c) => ({ title: c.title, width: c.align ? (c.align === 'center' ? 8 : 18) : 36 })), rows)
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.report-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  max-width: 1200px;
  margin: 0 auto 16px;
}
.metric {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-bg-2, #fafafa);
  cursor: default;
}
.metric-name {
  color: var(--app-text-3);
  font-size: 12px;
}
.metric-value {
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  margin: 2px 0;
}
.metric-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.paper {
  max-width: 1200px;
  margin: 0 auto;
}
.paper-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
}
.paper-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 4px 16px;
  color: var(--app-text-2);
  margin: 10px 0 8px;
}
.warn {
  margin-bottom: 8px;
}
.item.lv1 {
  padding-left: 1em;
}
.item.lv2 {
  padding-left: 3em;
}
.line-no {
  color: var(--app-text-3);
  font-variant-numeric: tabular-nums;
}
.num {
  font-variant-numeric: tabular-nums;
}
.drill {
  color: inherit;
}
.drill:hover {
  color: var(--app-primary);
  text-decoration: underline;
}
.neg,
.drill.neg {
  color: var(--status-critical);
}
.bold {
  font-weight: 600;
}
.paper-foot {
  display: flex;
  justify-content: space-between;
  color: var(--app-text-2);
  margin-top: 12px;
}
.paper-foot span {
  min-width: 160px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.trend {
  max-width: 1200px;
  margin: 24px auto 0;
}
.trend-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.report-table :deep(.ant-table-thead > tr > th) {
  text-align: center;
}
</style>
