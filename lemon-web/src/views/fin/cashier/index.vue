<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert v-if="unvoucheredText" type="warning" show-icon class="tip" :message="unvoucheredText" />
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-space>
            <a-range-picker v-model:value="range" :allow-clear="false" value-format="YYYY-MM-DD" />
            <a-checkbox v-if="tab !== 'check'" v-model:checked="posted">{{ $t('fin.cashier.postedOnly') }}</a-checkbox>
          </a-space>
        </template>

        <a-tab-pane key="journal" :tab="$t('fin.cashier.journal')">
          <div class="filters">
            <CashAccountSelect v-model:value="accountId" all allow-clear :placeholder="$t('fin.cashier.allAccounts')" class="account" />
            <a-button type="primary" @click="loadJournal">{{ $t('common.action.query') }}</a-button>
            <span class="sub-text">{{ $t('fin.cashier.journalTip') }}</span>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="journalColumns"
            :data-source="journal"
            :loading="journalLoading"
            :pagination="false"
            :row-class-name="(r) => (r.type !== LedgerRowType.ENTRY ? 'total-row' : '')"
            :scroll="{ x: 1350, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'voucherNo'">
                <template v-if="record.voucherId">
                  <a @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                  <DictTag v-if="record.voucherStatus !== VoucherStatus.POSTED" type="fin_voucher_status" :value="record.voucherStatus" class="ml" />
                </template>
              </template>
              <template v-else-if="column.key === 'source'">
                <template v-if="record.sourceNo">
                  <DictTag type="fin_voucher_source" :value="record.sourceType" />
                  <div class="sub-text">{{ record.sourceNo }}</div>
                </template>
              </template>
              <template v-else-if="column.key === 'account'">
                <template v-if="record.accountCode">
                  {{ record.accountName }}
                  <div class="sub-text">{{ record.accountCode }}</div>
                </template>
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

        <a-tab-pane key="daily" :tab="$t('fin.cashier.daily')">
          <div class="filters">
            <a-button type="primary" @click="loadDaily">{{ $t('common.action.query') }}</a-button>
            <span class="sub-text">{{ $t('fin.cashier.dailyTip') }}</span>
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
                <a @click="openJournal(record.accountId)">{{ record.name }}</a>
                <span class="sub-text ml">{{ record.code }}</span>
              </template>
              <template v-else-if="['open', 'debit', 'credit', 'close'].includes(column.key)">
                <span :class="['num', { neg: Number(record[column.key]) < 0 }]">{{ formatMoney(record[column.key]) }}</span>
              </template>
            </template>
            <template v-if="daily.rows.length" #summary>
              <a-table-summary fixed>
                <a-table-summary-row class="total-row">
                  <a-table-summary-cell>{{ $t('common.field.total') }}</a-table-summary-cell>
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

        <a-tab-pane key="check" :tab="$t('fin.cashier.check')">
          <div class="filters">
            <a-button type="primary" @click="loadCheck">{{ $t('common.action.query') }}</a-button>
            <span class="sub-text">{{ $t('fin.cashier.checkTip') }}</span>
          </div>
          <a-table
            row-key="cashAccountId"
            size="small"
            bordered
            :columns="checkColumns"
            :data-source="checkRows"
            :loading="checkLoading"
            :pagination="false"
            :scroll="{ x: 1600, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'account'">
                <a @click="openJournal(record.accountId)">{{ record.name }}</a>
                <div class="sub-text">{{ record.accountCode }} {{ record.accountName }}</div>
              </template>
              <template v-else-if="column.key === 'matched'">
                <a-tag :color="record.matched ? 'green' : 'red'" :bordered="false">{{ record.matched ? $t('fin.cashier.matched') : $t('fin.cashier.unmatched') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'unvouchered'">
                <span v-if="record.unvoucheredCount">{{ $t('fin.cashier.unvoucheredCell', { n: record.unvoucheredCount, amount: formatMoney(record.unvoucheredAmount) }) }}</span>
              </template>
              <template v-else-if="column.money">
                <span :class="['num', { neg: Number(record[column.key]) < 0 }]">{{ formatMoney(record[column.key]) }}</span>
              </template>
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
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import CashAccountSelect from '@/components/fin/CashAccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { cashCheck, cashDaily, cashJournal, cashUnvouchered } from '@/api/fin'
import { formatMoney, moneyOrBlank } from '@/components/fin/fin'
import { useDict } from '@/composables/useDict'

import { LedgerRowType, VoucherStatus } from '@/constants/enums'
/** 出纳管理：资金账户的日记账（逐笔收支、对方科目、每日小计）、资金日报（各账户期初、收入、支出、余额）和核对总账（日记账与总账科目余额比对） */
const { t } = useI18n()
useDict('fin_voucher_status', 'fin_voucher_source')
const tab = ref('daily')
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const posted = ref(false)
const params = () => ({ begin: range.value[0], end: range.value[1], posted: posted.value })

// ---------------------------------------------------------------- 日记账
const journalColumns = [
  { title: t('common.field.date'), dataIndex: 'entryDate', width: 110 },
  { title: t('fin.cashier.voucherNo'), key: 'voucherNo', width: 150 },
  { title: t('fin.cashier.summary'), dataIndex: 'summary', width: 240, ellipsis: true },
  { title: t('fin.cashier.source'), key: 'source', width: 150 },
  { title: t('fin.cashier.account'), key: 'account', width: 180 },
  { title: t('fin.cashier.counterAccounts'), dataIndex: 'counterAccounts', width: 180, ellipsis: true },
  { title: t('fin.cashier.income'), key: 'debit', width: 130, align: 'right' },
  { title: t('fin.cashier.expense'), key: 'credit', width: 130, align: 'right' },
  { title: t('fin.cashier.balance'), key: 'balance', width: 140, align: 'right' }
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
  { title: t('fin.cashier.account'), key: 'account', width: 240 },
  { title: t('fin.cashier.openBalance'), key: 'open', width: 140, align: 'right' },
  { title: t('fin.cashier.income'), key: 'debit', width: 140, align: 'right' },
  { title: t('fin.cashier.inCount'), dataIndex: 'inCount', width: 110, align: 'right' },
  { title: t('fin.cashier.expense'), key: 'credit', width: 140, align: 'right' },
  { title: t('fin.cashier.outCount'), dataIndex: 'outCount', width: 110, align: 'right' },
  { title: t('fin.cashier.closeBalance'), key: 'close', width: 140, align: 'right' }
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

// ---------------------------------------------------------------- 核对总账
const moneyColumn = (key, title) => ({ title, key, width: 120, align: 'right', money: true })
const checkColumns = [
  { title: t('fin.cashier.account'), key: 'account', width: 200, fixed: 'left' },
  {
    title: t('fin.cashier.journalSide'),
    children: [
      moneyColumn('journalOpen', t('fin.cashier.openBalance')),
      moneyColumn('journalDebit', t('fin.cashier.income')),
      moneyColumn('journalCredit', t('fin.cashier.expense')),
      moneyColumn('journalClose', t('fin.cashier.closeBalance'))
    ]
  },
  {
    title: t('fin.cashier.ledgerSide'),
    children: [
      moneyColumn('ledgerOpen', t('fin.cashier.openBalance')),
      moneyColumn('ledgerDebit', t('fin.cashier.debit')),
      moneyColumn('ledgerCredit', t('fin.cashier.credit')),
      moneyColumn('ledgerClose', t('fin.cashier.closeBalance'))
    ]
  },
  moneyColumn('difference', t('fin.cashier.difference')),
  moneyColumn('unposted', t('fin.cashier.unposted')),
  moneyColumn('openingDifference', t('fin.cashier.openingDifference')),
  { title: t('fin.cashier.unvoucheredCol'), key: 'unvouchered', width: 150 },
  { title: t('fin.cashier.checkResult'), key: 'matched', width: 90, fixed: 'right' }
]
const checkRows = ref([])
const checkLoading = ref(false)
async function loadCheck() {
  checkLoading.value = true
  try {
    checkRows.value = await cashCheck({ begin: range.value[0], end: range.value[1] })
  } finally {
    checkLoading.value = false
  }
}

const loaders = { daily: () => loadDaily(), journal: () => loadJournal(), check: () => loadCheck() }
watch([range, posted], () => loaders[tab.value]())
watch(tab, (v) => loaders[v]())

// ---------------------------------------------------------------- 未生成凭证的收付款
const unvouchered = ref({})
const unvoucheredText = computed(() => {
  const parts = [
    ['RECEIPT', t('fin.cashier.receipt')],
    ['PAYMENT', t('fin.cashier.payment')]
  ]
    .filter(([k]) => unvouchered.value[k]?.count)
    .map(([k, label]) => t('fin.cashier.unvoucheredPart', { type: label, n: unvouchered.value[k].count, amount: formatMoney(unvouchered.value[k].amount) }))
  return parts.length ? t('fin.cashier.unvouchered', { parts: parts.join(t('fin.cashier.sep')) }) : ''
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
