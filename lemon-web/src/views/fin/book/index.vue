<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="balance" :tab="$t('fin.book.trialBalance')">
          <div class="filters">
            <span>{{ $t('fin.book.period') }}</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>{{ $t('fin.book.to') }}</span>
            <PeriodPicker v-model:value="range.end" />
            <a-checkbox v-model:checked="showZero">{{ $t('fin.book.showZero') }}</a-checkbox>
            <a-button type="primary" @click="loadBalance">{{ $t('common.action.query') }}</a-button>
            <a-tag v-if="tb" :color="tb.balanced ? 'green' : 'red'" :bordered="false">{{ tb.balanced ? $t('fin.book.balanced') : $t('fin.book.unbalanced') }}</a-tag>
          </div>
          <a-table
            row-key="accountId"
            size="small"
            bordered
            :columns="balanceColumns"
            :data-source="tb?.rows || []"
            :loading="balanceLoading"
            :pagination="false"
            :scroll="{ x: 1000, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'account'">
                <span :style="{ paddingLeft: `${(record.level - 1) * 16}px`, fontWeight: record.level === 1 ? 500 : 400 }">
                  <a @click="openLedger(record)">{{ record.code }}</a> {{ record.name }}
                </span>
              </template>
              <template v-else-if="['openBalance', 'debit', 'credit', 'closeBalance'].includes(column.key)">
                <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'direction'">{{ record.direction === 'DEBIT' ? $t('fin.book.dr') : $t('fin.book.cr') }}</template>
            </template>
            <template v-if="tb" #summary>
              <a-table-summary fixed>
                <a-table-summary-row class="total-row">
                  <a-table-summary-cell>{{ $t('fin.book.totalLevel1') }}</a-table-summary-cell>
                  <a-table-summary-cell />
                  <a-table-summary-cell align="right">{{ $t('fin.book.dr') }} {{ formatMoney(tb.openDebit) }}<br />{{ $t('fin.book.cr') }} {{ formatMoney(tb.openCredit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(tb.debit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(tb.credit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ $t('fin.book.dr') }} {{ formatMoney(tb.closeDebit) }}<br />{{ $t('fin.book.cr') }} {{ formatMoney(tb.closeCredit) }}</a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="ledger" :tab="$t('fin.book.ledger')">
          <div class="filters">
            <AccountSelect v-model:value="ledgerAccount" all :placeholder="$t('fin.book.selectAccount')" class="account" />
            <span>{{ $t('fin.book.period') }}</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>{{ $t('fin.book.to') }}</span>
            <PeriodPicker v-model:value="range.end" />
            <a-button type="primary" :disabled="!ledgerAccount" @click="loadLedger">{{ $t('common.action.query') }}</a-button>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="ledgerColumns"
            :data-source="ledgerRows"
            :loading="ledgerLoading"
            :pagination="false"
            :row-class-name="(r) => (r.type !== 'ENTRY' ? 'total-row' : '')"
            :scroll="{ x: 1000, y: '100%' }"
            :locale="{ emptyText: $t('fin.book.selectAccountFirst') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'voucherNo'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
              </template>
              <template v-else-if="column.key === 'account'">
                <span v-if="record.accountCode">{{ record.accountCode }} {{ record.accountName }}</span>
              </template>
              <template v-else-if="['debit', 'credit'].includes(column.key)">
                <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'balance'">
                <span class="num">{{ formatMoney(Math.abs(record.balance)) }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="aux" :tab="$t('fin.book.aux')">
          <div class="filters">
            <a-radio-group v-model:value="aux.dimension" button-style="solid" @change="loadAux">
              <a-radio-button v-for="a in AUX_ITEMS" :key="a.value" :value="a.value">{{ a.label }}</a-radio-button>
            </a-radio-group>
            <AccountSelect v-model:value="aux.accountId" :placeholder="$t('fin.book.allAccounts')" allow-clear class="account" />
            <span>{{ $t('fin.book.period') }}</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>{{ $t('fin.book.to') }}</span>
            <PeriodPicker v-model:value="range.end" />
            <a-button type="primary" @click="loadAux">{{ $t('common.action.query') }}</a-button>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="auxColumns"
            :data-source="aux.rows"
            :loading="aux.loading"
            :pagination="false"
            :scroll="{ x: 900, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'auxName'">{{ [record.auxCode, record.auxName].filter(Boolean).join(' ') }}</template>
              <template v-else-if="column.key === 'account'">{{ record.accountCode }} {{ record.accountName }}</template>
              <template v-else-if="['debit', 'credit'].includes(column.key)">
                <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'balance'">
                <span class="num">{{ Number(record.balance) >= 0 ? $t('fin.book.dr') : $t('fin.book.cr') }} {{ formatMoney(Math.abs(Number(record.balance))) }}</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { auxBalance, ledger, trialBalance } from '@/api/fin'
import { AUX_ITEMS, currentPeriod, formatMoney, moneyOrBlank } from '@/components/fin/fin'

const { t } = useI18n()
const tab = ref('balance')
const range = reactive({ begin: Math.floor(currentPeriod() / 100) * 100 + 1, end: currentPeriod() })
const showZero = ref(false)

// ---------------------------------------------------------------- 科目余额表
const balanceColumns = [
  { title: t('fin.book.account'), key: 'account', width: 300 },
  { title: t('fin.book.direction'), key: 'direction', width: 80 },
  { title: t('fin.book.openBalance'), key: 'openBalance', width: 150, align: 'right' },
  { title: t('fin.book.periodDebit'), key: 'debit', width: 150, align: 'right' },
  { title: t('fin.book.periodCredit'), key: 'credit', width: 150, align: 'right' },
  { title: t('fin.book.closeBalance'), key: 'closeBalance', width: 150, align: 'right' }
]
const tb = ref()
const balanceLoading = ref(false)
async function loadBalance() {
  balanceLoading.value = true
  try {
    tb.value = await trialBalance({ begin: range.begin, end: range.end, showZero: showZero.value })
  } finally {
    balanceLoading.value = false
  }
}
onMounted(loadBalance)

// ---------------------------------------------------------------- 明细账
const ledgerColumns = [
  { title: t('common.field.date'), dataIndex: 'entryDate', width: 110 },
  { title: t('fin.book.voucherNo'), key: 'voucherNo', width: 120 },
  { title: t('fin.book.summary'), dataIndex: 'summary', width: 230, ellipsis: true },
  { title: t('fin.book.account'), key: 'account', width: 180 },
  { title: t('fin.book.debit'), key: 'debit', width: 140, align: 'right' },
  { title: t('fin.book.credit'), key: 'credit', width: 140, align: 'right' },
  { title: t('fin.book.direction'), dataIndex: 'side', width: 80, align: 'center' },
  { title: t('fin.book.balance'), key: 'balance', width: 150, align: 'right' }
]
const ledgerAccount = ref()
const ledgerRows = ref([])
const ledgerLoading = ref(false)
async function loadLedger() {
  ledgerLoading.value = true
  try {
    const rows = await ledger({ accountId: ledgerAccount.value, begin: range.begin, end: range.end })
    ledgerRows.value = rows.map((r, i) => ({ ...r, key: i }))
  } finally {
    ledgerLoading.value = false
  }
}

/** 从余额表点科目编码打开明细账 */
function openLedger(row) {
  ledgerAccount.value = row.accountId
  tab.value = 'ledger'
  loadLedger()
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}

// ---------------------------------------------------------------- 辅助核算
const auxColumns = computed(() => [
  { title: AUX_ITEMS.find((a) => a.value === aux.dimension)?.label, key: 'auxName', width: 240 },
  { title: t('fin.book.account'), key: 'account', width: 220 },
  { title: t('fin.book.periodDebit'), key: 'debit', width: 150, align: 'right' },
  { title: t('fin.book.periodCredit'), key: 'credit', width: 150, align: 'right' },
  { title: t('fin.book.closeBalance'), key: 'balance', width: 170, align: 'right' }
])
const aux = reactive({ dimension: 'PROJECT', accountId: undefined, rows: [], loading: false })
async function loadAux() {
  aux.loading = true
  try {
    const rows = await auxBalance({ dimension: aux.dimension, begin: range.begin, end: range.end, accountId: aux.accountId })
    aux.rows = rows.map((r, i) => ({ ...r, key: i }))
  } finally {
    aux.loading = false
  }
}
</script>

<style scoped>
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
:deep(.total-row) td {
  font-weight: 500;
  background: var(--app-bg-2, #fafafa);
}
</style>
