<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert v-if="unvoucheredText" type="warning" show-icon class="tip" :message="unvoucheredText" />
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-space>
            <a-range-picker v-model:value="range" :allow-clear="false" value-format="YYYY-MM-DD" />
            <a-checkbox v-model:checked="posted">只看已记账</a-checkbox>
          </a-space>
        </template>

        <a-tab-pane key="journal" tab="日记账">
          <div class="filters">
            <AccountSelect v-model:value="accountId" all :prefix="CASH_PREFIX" allow-clear placeholder="全部现金、银行账户" class="account" />
            <a-button type="primary" @click="loadJournal">查询</a-button>
            <span class="sub-text">数据来自凭证，默认包含未审核、未记账的凭证；收入为借方、支出为贷方</span>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="journalColumns"
            :data-source="journal"
            :loading="journalLoading"
            :pagination="false"
            :row-class-name="(r) => (r.type !== 'ENTRY' ? 'total-row' : '')"
            :scroll="{ x: 1200, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'voucherNo'">
                <template v-if="record.voucherId">
                  <a @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                  <DictTag v-if="record.voucherStatus !== 'POSTED'" type="fin_voucher_status" :value="record.voucherStatus" class="ml" />
                </template>
              </template>
              <template v-else-if="column.key === 'account'">
                <span v-if="record.accountCode">{{ record.accountCode }} {{ record.accountName }}</span>
              </template>
              <template v-else-if="['debit', 'credit'].includes(column.key)">
                <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'balance'">
                <span :class="['num', { neg: Number(record.balance) < 0 }]">{{ formatMoney(record.balance) }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="daily" tab="资金日报">
          <div class="filters">
            <a-button type="primary" @click="loadDaily">查询</a-button>
            <span class="sub-text">各现金、银行账户在所选日期范围内的期初、收入、支出和余额</span>
          </div>
          <a-table
            row-key="accountId"
            size="small"
            bordered
            :columns="dailyColumns"
            :data-source="daily.rows"
            :loading="dailyLoading"
            :pagination="false"
            :scroll="{ x: 1000, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'account'">
                <a @click="openJournal(record.accountId)">{{ record.code }}</a> {{ record.name }}
              </template>
              <template v-else-if="['open', 'debit', 'credit', 'close'].includes(column.key)">
                <span :class="['num', { neg: Number(record[column.key]) < 0 }]">{{ formatMoney(record[column.key]) }}</span>
              </template>
            </template>
            <template v-if="daily.rows.length" #summary>
              <a-table-summary fixed>
                <a-table-summary-row class="total-row">
                  <a-table-summary-cell>合计</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(daily.total.open) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(daily.total.debit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ daily.total.inCount }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(daily.total.credit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ daily.total.outCount }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(daily.total.close) }}</a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { cashDaily, cashJournal, cashUnvouchered } from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'

/** 出纳管理：现金 / 银行日记账（逐笔收支、对方科目、每日小计）与资金日报（各账户期初、收入、支出、余额） */
useDict('fin_voucher_status')
const CASH_PREFIX = ['1001', '1002']
const tab = ref('daily')
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const posted = ref(false)
const params = () => ({ begin: range.value[0], end: range.value[1], posted: posted.value })

// ---------------------------------------------------------------- 日记账
const journalColumns = [
  { title: '日期', dataIndex: 'entryDate', width: 110 },
  { title: '凭证号', key: 'voucherNo', width: 150 },
  { title: '摘要', dataIndex: 'summary', width: 240, ellipsis: true },
  { title: '账户', key: 'account', width: 180 },
  { title: '对方科目', dataIndex: 'counterAccounts', width: 180, ellipsis: true },
  { title: '收入', key: 'debit', width: 130, align: 'right' },
  { title: '支出', key: 'credit', width: 130, align: 'right' },
  { title: '余额', key: 'balance', width: 140, align: 'right' }
]
const accountId = ref()
const journal = ref([])
const journalLoading = ref(false)
async function loadJournal() {
  journalLoading.value = true
  try {
    const rows = await cashJournal({ ...params(), accountId: accountId.value })
    journal.value = rows.map((r, i) => ({ ...r, key: i }))
  } finally {
    journalLoading.value = false
  }
}
function openJournal(id) {
  accountId.value = id
  tab.value = 'journal'
  loadJournal()
}

// ---------------------------------------------------------------- 资金日报
const dailyColumns = [
  { title: '账户', key: 'account', width: 240 },
  { title: '期初余额', key: 'open', width: 140, align: 'right' },
  { title: '收入', key: 'debit', width: 140, align: 'right' },
  { title: '收入笔数', dataIndex: 'inCount', width: 90, align: 'right' },
  { title: '支出', key: 'credit', width: 140, align: 'right' },
  { title: '支出笔数', dataIndex: 'outCount', width: 90, align: 'right' },
  { title: '期末余额', key: 'close', width: 140, align: 'right' }
]
const daily = ref({ rows: [], total: {} })
const dailyLoading = ref(false)
async function loadDaily() {
  dailyLoading.value = true
  try {
    daily.value = await cashDaily(params())
  } finally {
    dailyLoading.value = false
  }
}

watch([range, posted], () => (tab.value === 'daily' ? loadDaily() : loadJournal()))
watch(tab, (t) => (t === 'daily' ? loadDaily() : loadJournal()))

// ---------------------------------------------------------------- 未生成凭证的收付款
const unvouchered = ref({})
const unvoucheredText = computed(() => {
  const parts = [
    ['RECEIPT', '收款'],
    ['PAYMENT', '付款']
  ]
    .filter(([k]) => unvouchered.value[k]?.count)
    .map(([k, label]) => `${label}单 ${unvouchered.value[k].count} 张（${formatMoney(unvouchered.value[k].amount)}）`)
  return parts.length ? `还有${parts.join('、')}没有生成凭证，不在日记账中；请在收款单、付款单中生成凭证` : ''
})

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}

onMounted(async () => {
  loadDaily()
  unvouchered.value = await cashUnvouchered()
})
</script>

<style scoped>
.tip {
  margin-bottom: 8px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.account {
  width: 280px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.neg {
  color: var(--status-critical);
}
.ml {
  margin-left: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.total-row) td {
  font-weight: 500;
  background: var(--app-bg-2, #fafafa);
}
</style>
