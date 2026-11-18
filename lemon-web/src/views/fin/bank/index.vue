<template>
  <div class="bank-page">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-space wrap>
          <CashAccountSelect v-model:value="accountId" all bank :placeholder="$t('fin.bank.selectAccount')" class="account" />
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
          <a-button :disabled="!accountId" @click="load"><template #icon><ReloadOutlined /></template>{{ $t('common.action.query') }}</a-button>
          <a-checkbox v-model:checked="onlyOpen">{{ $t('fin.bank.onlyOpen') }}</a-checkbox>
        </a-space>
        <a-space wrap>
          <a-button v-perm="'fin:bank:edit'" :disabled="!accountId" @click="importOpen = true">
            <template #icon><UploadOutlined /></template>{{ $t('fin.bank.importStatement') }}
          </a-button>
          <a-button v-perm="'fin:bank:edit'" :disabled="!accountId" :loading="matching" @click="autoMatch">
            <template #icon><ThunderboltOutlined /></template>{{ $t('fin.bank.autoMatch') }}
          </a-button>
          <a-button v-perm="'fin:bank:edit'" type="primary" :disabled="!canMatch" @click="manualMatch">
            <template #icon><LinkOutlined /></template>{{ $t('fin.bank.manualMatch') }}
          </a-button>
          <a-button :disabled="!accountId" @click="reconcileRef.open(range?.[1])">
            <template #icon><FileTextOutlined /></template>{{ $t('fin.bank.reconcile') }}
          </a-button>
        </a-space>
      </div>
      <div class="sub-text">
        {{ $t('fin.bank.hint') }}
      </div>
    </a-card>

    <a-row :gutter="12" class="tables">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card side-card">
          <div class="side-head">
            <span class="section-title">{{ $t('fin.bank.statements') }}</span>
            <a-space>
              <span class="sub-text">
                {{ $t('fin.bank.countText', { total: stmtRows.length, open: stmtRows.filter((s) => !s.matched).length }) }}
                {{ $t('fin.bank.net') }} <span :class="{ neg: stmtTotal < 0 }">{{ formatMoney(stmtTotal) }}</span>
              </span>
              <span v-if="deletable.length" v-perm="'fin:bank:edit'">
                <a-popconfirm :title="$t('fin.bank.confirmDelete', { n: deletable.length })" @confirm="removeStatements">
                  <a-button size="small" danger>{{ $t('fin.bank.deleteN', { n: deletable.length }) }}</a-button>
                </a-popconfirm>
              </span>
            </a-space>
          </div>
          <a-table
            row-key="key"
            size="small"
            :columns="stmtColumns"
            :data-source="shownStatements"
            :loading="loading"
            :pagination="false"
            :row-selection="{ selectedRowKeys: stmtKeys, onChange: (k) => (stmtKeys = k) }"
            :row-class-name="(r) => (r.matched ? 'matched-row' : '')"
            :scroll="{ x: 640, y: 520 }"
            :locale="{ emptyText: accountId ? $t('fin.bank.emptyStatements') : $t('fin.bank.selectAccountFirst') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">
                <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'summary'">
                <a-tag v-if="record.opening" :bordered="false" color="orange">{{ $t('fin.bank.opening') }}</a-tag>{{ record.summary || '-' }}
                <div v-if="record.refNo" class="sub-text">{{ record.refNo }}</div>
              </template>
              <template v-else-if="column.key === 'match'">
                <template v-if="record.matched">
                  <a-tag color="green" :bordered="false">{{ $t('fin.bank.matched') }}</a-tag>
                  <div class="sub-text">{{ record.openingItemId ? $t('fin.bank.matchedOpening') : `${record.voucherNo || ''} ${record.voucherDate || ''}` }}</div>
                  <span v-perm="'fin:bank:edit'">
                    <a-popconfirm :title="$t('fin.bank.confirmUnmatch')" @confirm="unmatchStatement(record)">
                      <a class="small-link">{{ $t('fin.bank.unmatch') }}</a>
                    </a-popconfirm>
                  </span>
                </template>
                <a-tag v-else :bordered="false">{{ $t('fin.bank.unmatched') }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card side-card">
          <div class="side-head">
            <span class="section-title">{{ $t('fin.bank.bookEntries') }}</span>
            <span class="sub-text">
              {{ $t('fin.bank.countText', { total: entryRows.length, open: entryRows.filter((e) => !e.matched).length }) }}
              {{ $t('fin.bank.net') }} <span :class="{ neg: entryTotal < 0 }">{{ formatMoney(entryTotal) }}</span>
            </span>
          </div>
          <a-table
            row-key="key"
            size="small"
            :columns="entryColumns"
            :data-source="shownEntries"
            :loading="loading"
            :pagination="false"
            :row-selection="{ type: 'radio', selectedRowKeys: entryKeys, onChange: (k) => (entryKeys = k), getCheckboxProps: (r) => ({ disabled: !!r.matched }) }"
            :row-class-name="(r) => (r.matched ? 'matched-row' : '')"
            :scroll="{ x: 600, y: 520 }"
            :locale="{ emptyText: accountId ? $t('fin.bank.emptyEntries') : $t('fin.bank.selectAccountFirst') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'voucher'">
                <a-tag v-if="record.opening" :bordered="false" color="orange">{{ $t('fin.bank.opening') }}</a-tag>
                <a v-else @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <div class="sub-text">{{ record.voucherDate }}</div>
              </template>
              <template v-else-if="column.key === 'amount'">
                <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'match'">
                <a-tag :color="record.matched ? 'green' : undefined" :bordered="false">{{ record.matched ? $t('fin.bank.matched') : $t('fin.bank.unmatched') }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <ImportModal
      v-model:open="importOpen"
      :title="$t('fin.bank.statementTitle')"
      :columns="importColumns"
      :sample="importSample"
      :importer="importRows"
      @success="load"
    />
    <ReconcileModal ref="reconcileRef" :account-id="accountId" :account-name="accountName" />
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { FileTextOutlined, LinkOutlined, ReloadOutlined, ThunderboltOutlined, UploadOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import CashAccountSelect from '@/components/fin/CashAccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import ReconcileModal from './ReconcileModal.vue'
import {
  autoMatchBank,
  bankEntries,
  bankOpeningItems,
  bankStatements,
  cashAccountOptions,
  deleteBankStatements,
  importBankStatements,
  matchBank,
  matchBankOpening,
  unmatchBank,
  unmatchBankOpening
} from '@/api/fin'
import { today } from '@/components/wms/wms'
import { cached, formatMoney } from '@/components/fin/fin'
import { round2 } from '@/utils/number'
import { useAction } from '@/composables/useAction'
import { BankOpeningSide } from '@/constants/enums'

/**
 * 银行对账：银行类资金账户的对账单流水（左）与凭证分录（右）勾对，生成余额调节表。
 * 账户有期初未达账时一并列出（标「期初」）：银行已记企业未记的列在流水一侧，与凭证分录勾对；企业已记银行未记的列在分录一侧，与流水勾对
 */
const { t } = useI18n()
const accountId = ref()
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), today()])
const onlyOpen = ref(false)
const loading = ref(false)
const statements = ref([])
const entries = ref([])
const openingItems = ref([])
const stmtKeys = ref([])
const entryKeys = ref([])
const reconcileRef = ref()

const params = () => ({ accountId: accountId.value, begin: range.value?.[0], end: range.value?.[1] })
/** 流水一侧：对账单流水 + 银行已记企业未记的期初未达账 */
const stmtRows = computed(() => [
  ...openingItems.value
    .filter((i) => i.side === BankOpeningSide.BANK)
    .map((i) => ({ ...i, key: `o${i.id}`, itemId: i.id, opening: true, matched: !!i.entryId })),
  ...statements.value.map((s) => ({ ...s, key: `s${s.id}`, matched: !!(s.entryId || s.openingItemId) }))
])
/** 分录一侧：凭证分录 + 企业已记银行未记的期初未达账 */
const entryRows = computed(() => [
  ...openingItems.value
    .filter((i) => i.side === BankOpeningSide.BOOK)
    .map((i) => ({ key: `o${i.id}`, itemId: i.id, opening: true, voucherDate: i.txnDate, summary: i.summary, amount: i.amount, matched: i.statementId ? 1 : 0 })),
  ...entries.value.map((e) => ({ ...e, key: `e${e.entryId}` }))
])
const shownStatements = computed(() => (onlyOpen.value ? stmtRows.value.filter((s) => !s.matched) : stmtRows.value))
const shownEntries = computed(() => (onlyOpen.value ? entryRows.value.filter((e) => !e.matched) : entryRows.value))
const stmtTotal = computed(() => round2(stmtRows.value.reduce((s, r) => s + Number(r.amount), 0)))
const entryTotal = computed(() => round2(entryRows.value.reduce((s, r) => s + Number(r.amount), 0)))
/** 可删除的是导入的流水（期初未达账在「资金账户」中维护） */
const deletable = computed(() => stmtRows.value.filter((s) => !s.opening && stmtKeys.value.includes(s.key)).map((s) => s.id))

const accountName = ref('')
async function refreshAccountName() {
  const list = await cached('cash-accounts', cashAccountOptions)
  accountName.value = list.find((x) => x.accountId === accountId.value)?.name || ''
}

async function load() {
  if (!accountId.value) return
  loading.value = true
  try {
    ;[statements.value, entries.value, openingItems.value] = await Promise.all([
      bankStatements(params()),
      bankEntries(params()),
      bankOpeningItems(accountId.value)
    ])
    stmtKeys.value = []
    entryKeys.value = []
  } finally {
    loading.value = false
  }
}
watch(accountId, () => {
  statements.value = []
  entries.value = []
  openingItems.value = []
  refreshAccountName()
  load()
})
watch(range, load)

const stmtColumns = [
  { title: t('fin.bank.txnDate'), dataIndex: 'txnDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.bank.summaryRef'), key: 'summary', ellipsis: true },
  { title: t('fin.bank.match'), key: 'match', width: 150 }
]
const entryColumns = [
  { title: t('fin.bank.voucher'), key: 'voucher', width: 120 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.bank.summary'), dataIndex: 'summary', ellipsis: true },
  { title: t('fin.bank.match'), key: 'match', width: 110 }
]

// ---------------------------------------------------------------- 勾对
const selectedStatement = computed(() => {
  const rows = stmtRows.value.filter((s) => stmtKeys.value.includes(s.key))
  return rows.length === 1 && !rows[0].matched ? rows[0] : null
})
const selectedEntry = computed(() => entryRows.value.find((e) => entryKeys.value.includes(e.key) && !e.matched))
const canMatch = computed(() => !!selectedStatement.value && !!selectedEntry.value)

async function manualMatch() {
  const s = selectedStatement.value
  const e = selectedEntry.value
  if (s.opening && e.opening) {
    message.warning(t('fin.bank.openingBothSides'))
    return
  }
  if (Math.abs(Number(s.amount) - Number(e.amount)) >= 0.005) {
    message.warning(t('fin.bank.amountMismatch', { bank: formatMoney(s.amount), book: formatMoney(e.amount) }))
    return
  }
  if (s.opening) await matchBankOpening(s.itemId, e.entryId)
  else if (e.opening) await matchBankOpening(e.itemId, s.id)
  else await matchBank(s.id, e.entryId)
  message.success(t('fin.bank.matched'))
  load()
}

/** 取消勾对：期初未达账或被期初未达账勾对的流水取消未达账的勾对，否则取消流水的勾对 */
const unmatchStatement = useAction(
  (r) => (r.opening ? unmatchBankOpening(r.itemId) : r.openingItemId ? unmatchBankOpening(r.openingItemId) : unmatchBank(r.id)),
  { successKey: 'fin.bank.unmatchedMsg', reload: () => load() }
)

const matching = ref(false)
async function autoMatch() {
  matching.value = true
  try {
    const count = await autoMatchBank(params())
    message.success(count ? t('fin.bank.autoMatched', { count }) : t('fin.bank.nothingToMatch'))
    load()
  } finally {
    matching.value = false
  }
}

const removeStatements = useAction(() => deleteBankStatements(deletable.value), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 导入
const importOpen = ref(false)
const importColumns = [
  { title: t('fin.bank.txnDate'), key: 'txnDate', required: true, type: 'date', width: 14 },
  { title: t('fin.bank.income'), key: 'income', type: 'number', width: 14 },
  { title: t('fin.bank.expense'), key: 'expense', type: 'number', width: 14 },
  { title: t('common.field.amount'), key: 'amount', type: 'number', width: 14 },
  { title: t('fin.bank.summary'), key: 'summary', width: 30 },
  { title: t('fin.bank.refNo'), key: 'refNo', width: 24 }
]
const importSample = { txnDate: '2026-10-08', income: '10000.00', expense: '', amount: '', summary: t('fin.bank.sampleSummary'), refNo: 'B202610080001' }

async function importRows({ rows }) {
  const errors = []
  const data = []
  rows.forEach(({ rowNum, data: d }) => {
    const amount = d.amount !== undefined ? d.amount : round2((d.income || 0) - (d.expense || 0))
    if (!amount) {
      errors.push({ rowNum, message: t('fin.bank.needAmount') })
      return
    }
    data.push({ txnDate: d.txnDate, amount, summary: d.summary || null, refNo: d.refNo || null })
  })
  if (errors.length) return { total: rows.length, imported: 0, errors }
  const imported = await importBankStatements({ accountId: accountId.value, rows: data })
  if (imported < rows.length) message.info(t('fin.bank.skipped', { n: rows.length - imported }))
  return { total: rows.length, imported, errors: [] }
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}
.account {
  width: 260px;
}
.tables {
  margin-top: 12px;
}
.side-card {
  margin-bottom: 12px;
}
.side-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.small-link {
  font-size: 12px;
}
.num {
  font-variant-numeric: tabular-nums;
  text-align: right;
}
.neg {
  color: #cf1322;
}
:deep(.matched-row) td {
  color: var(--app-text-3);
}
</style>
