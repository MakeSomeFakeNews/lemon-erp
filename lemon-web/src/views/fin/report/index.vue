<template>
  <div class="report-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-radio-group v-model:value="type" button-style="solid" @change="load">
          <a-radio-button value="balance">资产负债表</a-radio-button>
          <a-radio-button value="income">利润表</a-radio-button>
          <a-radio-button value="cash">现金流量表</a-radio-button>
        </a-radio-group>
        <template v-if="type === 'cash'">
          <PeriodPicker v-model:value="beginPeriod" placeholder="开始期间" @update:value="load" />
          <span>至</span>
        </template>
        <PeriodPicker v-model:value="period" @update:value="load" />
        <span class="sub-text">只统计已记账凭证；{{ TIPS[type] }}</span>
      </div>
      <div class="report">
        <div class="report-title">{{ TITLES[type] }}</div>
        <div class="report-sub">
          {{ subTitle }} · 单位：元
          <a-tag v-if="type === 'balance' && balanced !== null" :color="balanced ? 'green' : 'red'" :bordered="false" class="check">
            {{ balanced ? '资产 = 负债 + 所有者权益' : '报表不平衡，请检查科目设置' }}
          </a-tag>
        </div>
        <a-alert
          v-if="type === 'cash' && cash && !cash.balanced"
          type="warning"
          show-icon
          class="cash-warn"
          :message="`期末余额与账面不一致（账面现金及银行存款余额 ¥${formatMoney(cash.closingBalance)}），可能有未记账的现金凭证`"
        />
        <a-table row-key="name" size="middle" bordered :columns="columns" :data-source="lines" :loading="loading" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <span :style="{ paddingLeft: `${record.level * 24}px`, fontWeight: record.bold ? 600 : 400 }">{{ record.name }}</span>
            </template>
            <template v-else-if="['amount', 'amount2'].includes(column.key)">
              <span v-if="record[column.key] !== null && record[column.key] !== undefined" :class="['num', { neg: record[column.key] < 0, bold: record.bold }]">
                {{ formatMoney(record[column.key]) }}
              </span>
            </template>
          </template>
        </a-table>
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { balanceSheet, cashFlow, incomeStatement } from '@/api/fin'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'

const type = ref('balance')
const period = ref(currentPeriod())
const lines = ref([])
const loading = ref(false)

const TITLES = { balance: '资产负债表', income: '利润表', cash: '现金流量表' }
const TIPS = {
  balance: '未分配利润含尚未结转的本年损益',
  income: '不含期末结转损益凭证',
  cash: '直接法，按现金、银行存款的对方科目分类，现金科目之间的划转不计入'
}
const beginPeriod = ref(Number(`${String(currentPeriod()).slice(0, 4)}01`))
const cash = ref()

const subTitle = computed(() => {
  if (type.value === 'balance') return `${periodText(period.value)}末`
  if (type.value === 'cash') return beginPeriod.value === period.value ? periodText(period.value) : `${periodText(beginPeriod.value)} 至 ${periodText(period.value)}`
  return periodText(period.value)
})

const columns = computed(() =>
  type.value === 'cash'
    ? [
        { title: '项目', key: 'name' },
        { title: '金额', key: 'amount', width: 220, align: 'right' }
      ]
    : [
        { title: '项目', key: 'name' },
        { title: type.value === 'balance' ? '期末余额' : '本期金额', key: 'amount', width: 200, align: 'right' },
        { title: type.value === 'balance' ? '年初余额' : '本年累计', key: 'amount2', width: 200, align: 'right' }
      ]
)

const balanced = computed(() => {
  if (type.value !== 'balance' || !lines.value.length) return null
  const get = (n) => Number(lines.value.find((l) => l.name === n)?.amount || 0)
  return Math.abs(get('资产总计') - get('负债和所有者权益总计')) < 0.005
})

async function load() {
  loading.value = true
  try {
    if (type.value === 'cash') {
      if (beginPeriod.value > period.value) beginPeriod.value = period.value
      cash.value = await cashFlow(beginPeriod.value, period.value)
      lines.value = cash.value.lines
    } else {
      lines.value = type.value === 'balance' ? await balanceSheet(period.value) : await incomeStatement(period.value)
    }
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.report {
  max-width: 900px;
  margin: 0 auto;
}
.report-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
}
.report-sub {
  text-align: center;
  color: var(--app-text-3);
  margin: 6px 0 12px;
}
.cash-warn {
  margin-bottom: 12px;
}
.check {
  margin-left: 8px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.neg {
  color: #cf1322;
}
.bold {
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
