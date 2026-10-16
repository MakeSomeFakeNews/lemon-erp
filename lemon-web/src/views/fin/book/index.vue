<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="balance" tab="科目余额表">
          <div class="filters">
            <span>期间</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>至</span>
            <PeriodPicker v-model:value="range.end" />
            <a-checkbox v-model:checked="showZero">显示无发生额的科目</a-checkbox>
            <a-button type="primary" @click="loadBalance">查询</a-button>
            <a-tag v-if="tb" :color="tb.balanced ? 'green' : 'red'" :bordered="false">{{ tb.balanced ? '试算平衡' : '试算不平衡' }}</a-tag>
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
              <template v-else-if="column.key === 'direction'">{{ record.direction === 'DEBIT' ? '借' : '贷' }}</template>
            </template>
            <template v-if="tb" #summary>
              <a-table-summary fixed>
                <a-table-summary-row class="total-row">
                  <a-table-summary-cell>合计（一级科目）</a-table-summary-cell>
                  <a-table-summary-cell />
                  <a-table-summary-cell align="right">借 {{ formatMoney(tb.openDebit) }}<br />贷 {{ formatMoney(tb.openCredit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(tb.debit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">{{ formatMoney(tb.credit) }}</a-table-summary-cell>
                  <a-table-summary-cell align="right">借 {{ formatMoney(tb.closeDebit) }}<br />贷 {{ formatMoney(tb.closeCredit) }}</a-table-summary-cell>
                </a-table-summary-row>
              </a-table-summary>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="ledger" tab="明细账">
          <div class="filters">
            <AccountSelect v-model:value="ledgerAccount" all placeholder="选择科目（含下级）" class="account" />
            <span>期间</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>至</span>
            <PeriodPicker v-model:value="range.end" />
            <a-button type="primary" :disabled="!ledgerAccount" @click="loadLedger">查询</a-button>
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
            :locale="{ emptyText: '选择科目后查询' }"
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
        <a-tab-pane key="aux" tab="辅助核算">
          <div class="filters">
            <a-radio-group v-model:value="aux.dimension" button-style="solid" @change="loadAux">
              <a-radio-button value="project">项目</a-radio-button>
              <a-radio-button value="customer">客户</a-radio-button>
              <a-radio-button value="supplier">供应商</a-radio-button>
            </a-radio-group>
            <AccountSelect v-model:value="aux.accountId" placeholder="全部科目" allow-clear class="account" />
            <span>期间</span>
            <PeriodPicker v-model:value="range.begin" />
            <span>至</span>
            <PeriodPicker v-model:value="range.end" />
            <a-button type="primary" @click="loadAux">查询</a-button>
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
              <template v-if="column.key === 'auxName'">{{ record.auxCode }} {{ record.auxName }}</template>
              <template v-else-if="column.key === 'account'">{{ record.accountCode }} {{ record.accountName }}</template>
              <template v-else-if="['debit', 'credit'].includes(column.key)">
                <span class="num">{{ moneyOrBlank(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'balance'">
                <span class="num">{{ Number(record.balance) >= 0 ? '借' : '贷' }} {{ formatMoney(Math.abs(Number(record.balance))) }}</span>
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
import { onMounted, reactive, ref } from 'vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { auxBalance, ledger, trialBalance } from '@/api/fin'
import { currentPeriod, formatMoney, moneyOrBlank } from '@/components/fin/fin'

const tab = ref('balance')
const range = reactive({ begin: Math.floor(currentPeriod() / 100) * 100 + 1, end: currentPeriod() })
const showZero = ref(false)

// ---------------------------------------------------------------- 科目余额表
const balanceColumns = [
  { title: '科目', key: 'account', width: 300 },
  { title: '方向', key: 'direction', width: 60 },
  { title: '期初余额', key: 'openBalance', width: 150, align: 'right' },
  { title: '本期借方', key: 'debit', width: 150, align: 'right' },
  { title: '本期贷方', key: 'credit', width: 150, align: 'right' },
  { title: '期末余额', key: 'closeBalance', width: 150, align: 'right' }
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
  { title: '日期', dataIndex: 'entryDate', width: 110 },
  { title: '凭证号', key: 'voucherNo', width: 100 },
  { title: '摘要', dataIndex: 'summary', width: 230, ellipsis: true },
  { title: '科目', key: 'account', width: 180 },
  { title: '借方', key: 'debit', width: 140, align: 'right' },
  { title: '贷方', key: 'credit', width: 140, align: 'right' },
  { title: '方向', dataIndex: 'side', width: 60, align: 'center' },
  { title: '余额', key: 'balance', width: 150, align: 'right' }
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
const auxColumns = [
  { title: '项目 / 往来单位', key: 'auxName', width: 240 },
  { title: '科目', key: 'account', width: 220 },
  { title: '本期借方', key: 'debit', width: 150, align: 'right' },
  { title: '本期贷方', key: 'credit', width: 150, align: 'right' },
  { title: '期末余额', key: 'balance', width: 170, align: 'right' }
]
const aux = reactive({ dimension: 'project', accountId: undefined, rows: [], loading: false })
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
