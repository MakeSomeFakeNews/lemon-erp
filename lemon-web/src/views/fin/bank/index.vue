<template>
  <div class="bank-page">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-space wrap>
          <AccountSelect v-model:value="accountId" :roles="['BANK']" :placeholder="$t('fin.bank.selectAccount')" class="account" />
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
          <a-button :disabled="!accountId" @click="openReconcile">
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
                {{ $t('fin.bank.countText', { total: statements.length, open: statements.filter((s) => !s.entryId).length }) }}
                {{ $t('fin.bank.net') }} <span :class="{ neg: stmtTotal < 0 }">{{ formatMoney(stmtTotal) }}</span>
              </span>
              <span v-if="stmtKeys.length" v-perm="'fin:bank:edit'">
                <a-popconfirm :title="$t('fin.bank.confirmDelete', { n: stmtKeys.length })" @confirm="removeStatements">
                  <a-button size="small" danger>{{ $t('fin.bank.deleteN', { n: stmtKeys.length }) }}</a-button>
                </a-popconfirm>
              </span>
            </a-space>
          </div>
          <a-table
            row-key="id"
            size="small"
            :columns="stmtColumns"
            :data-source="shownStatements"
            :loading="loading"
            :pagination="false"
            :row-selection="{ selectedRowKeys: stmtKeys, onChange: (k) => (stmtKeys = k) }"
            :row-class-name="(r) => (r.entryId ? 'matched-row' : '')"
            :scroll="{ x: 640, y: 520 }"
            :locale="{ emptyText: accountId ? $t('fin.bank.emptyStatements') : $t('fin.bank.selectAccountFirst') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">
                <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'summary'">
                {{ record.summary || '-' }}
                <div v-if="record.refNo" class="sub-text">{{ record.refNo }}</div>
              </template>
              <template v-else-if="column.key === 'match'">
                <template v-if="record.entryId">
                  <a-tag color="green" :bordered="false">{{ $t('fin.bank.matched') }}</a-tag>
                  <div class="sub-text">{{ record.voucherNo }} {{ record.voucherDate }}</div>
                  <span v-perm="'fin:bank:edit'">
                    <a-popconfirm :title="$t('fin.bank.confirmUnmatch')" @confirm="unmatch(record.id)">
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
              {{ $t('fin.bank.countText', { total: entries.length, open: entries.filter((e) => !e.matched).length }) }}
              {{ $t('fin.bank.net') }} <span :class="{ neg: entryTotal < 0 }">{{ formatMoney(entryTotal) }}</span>
            </span>
          </div>
          <a-table
            row-key="entryId"
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
                <a @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
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

    <!-- 余额调节表 -->
    <a-modal v-model:open="reconcileOpen" :title="$t('fin.bank.reconcileTitle')" width="min(900px, 100vw)" :footer="null">
      <div class="filters">
        <span>{{ $t('fin.bank.asOf') }}</span>
        <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" />
        <span>{{ $t('fin.bank.bankBalance') }}</span>
        <a-input-number v-model:value="bankBalance" :precision="2" :placeholder="$t('fin.bank.bankBalancePh')" class="balance" />
        <a-button type="primary" :loading="reconciling" @click="loadReconcile">{{ $t('fin.bank.calculate') }}</a-button>
      </div>
      <a-spin :spinning="reconciling">
        <template v-if="rec">
          <div class="report-title">{{ $t('fin.bank.reconcileTitle') }}</div>
          <div class="report-sub">
            {{ $t('fin.bank.reportSub', { account: accountName, date: rec.asOf }) }}
            <a-tag :color="rec.balanced ? 'green' : 'red'" :bordered="false" class="check">
              {{ rec.balanced ? $t('fin.bank.balanced') : $t('fin.bank.unbalanced') }}
            </a-tag>
          </div>
          <table class="rec-table">
            <thead>
              <tr>
                <th>{{ $t('fin.bank.book') }}</th>
                <th class="num">{{ $t('common.field.amount') }}</th>
                <th>{{ $t('fin.bank.statementTitle') }}</th>
                <th class="num">{{ $t('common.field.amount') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>{{ $t('fin.bank.bookBalance') }}</td>
                <td class="num">{{ formatMoney(rec.bookBalance) }}</td>
                <td>{{ $t('fin.bank.bankBalance') }}</td>
                <td class="num">{{ formatMoney(rec.bankBalance) }}</td>
              </tr>
              <tr>
                <td>{{ $t('fin.bank.bankIn') }}</td>
                <td class="num">{{ formatMoney(rec.bankIn) }}</td>
                <td>{{ $t('fin.bank.bookIn') }}</td>
                <td class="num">{{ formatMoney(rec.bookIn) }}</td>
              </tr>
              <tr>
                <td>{{ $t('fin.bank.bankOut') }}</td>
                <td class="num">{{ formatMoney(rec.bankOut) }}</td>
                <td>{{ $t('fin.bank.bookOut') }}</td>
                <td class="num">{{ formatMoney(rec.bookOut) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>{{ $t('fin.bank.adjusted') }}</td>
                <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBook) }}</td>
                <td>{{ $t('fin.bank.adjusted') }}</td>
                <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBank) }}</td>
              </tr>
            </tfoot>
          </table>
          <a-row :gutter="12" class="rec-detail">
            <a-col :xs="24" :md="12">
              <div class="section-title">{{ $t('fin.bank.bankOnly', { n: rec.bankOnly.length }) }}</div>
              <a-table row-key="id" size="small" :columns="bankOnlyColumns" :data-source="rec.bankOnly" :pagination="false" :scroll="{ y: 240 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">
                    <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
                  </template>
                </template>
              </a-table>
            </a-col>
            <a-col :xs="24" :md="12">
              <div class="section-title">{{ $t('fin.bank.bookOnly', { n: rec.bookOnly.length }) }}</div>
              <a-table row-key="entryId" size="small" :columns="bookOnlyColumns" :data-source="rec.bookOnly" :pagination="false" :scroll="{ y: 240 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">
                    <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
                  </template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </template>
      </a-spin>
    </a-modal>

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
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import {
  accountOptions,
  autoMatchBank,
  bankEntries,
  bankReconcile,
  bankStatements,
  deleteBankStatements,
  importBankStatements,
  matchBank,
  unmatchBank
} from '@/api/fin'
import { today } from '@/components/wms/wms'
import { cached, formatMoney } from '@/components/fin/fin'
import { round2 } from '@/utils/number'
import { useAction } from '@/composables/useAction'

const { t } = useI18n()
const accountId = ref()
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), today()])
const onlyOpen = ref(false)
const loading = ref(false)
const statements = ref([])
const entries = ref([])
const stmtKeys = ref([])
const entryKeys = ref([])

const params = () => ({ accountId: accountId.value, begin: range.value?.[0], end: range.value?.[1] })
const shownStatements = computed(() => (onlyOpen.value ? statements.value.filter((s) => !s.entryId) : statements.value))
const shownEntries = computed(() => (onlyOpen.value ? entries.value.filter((e) => !e.matched) : entries.value))
const stmtTotal = computed(() => round2(statements.value.reduce((s, r) => s + Number(r.amount), 0)))
const entryTotal = computed(() => round2(entries.value.reduce((s, r) => s + Number(r.amount), 0)))

const accountName = ref('')
async function refreshAccountName() {
  const list = await cached('accounts-leaf', accountOptions)
  const a = list.find((x) => x.id === accountId.value)
  accountName.value = a ? `${a.code} ${a.name}` : ''
}

async function load() {
  if (!accountId.value) return
  loading.value = true
  try {
    ;[statements.value, entries.value] = await Promise.all([bankStatements(params()), bankEntries(params())])
    stmtKeys.value = []
    entryKeys.value = []
  } finally {
    loading.value = false
  }
}
watch(accountId, () => {
  statements.value = []
  entries.value = []
  rec.value = undefined
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
  const rows = statements.value.filter((s) => stmtKeys.value.includes(s.id))
  return rows.length === 1 && !rows[0].entryId ? rows[0] : null
})
const selectedEntry = computed(() => entries.value.find((e) => entryKeys.value.includes(e.entryId) && !e.matched))
const canMatch = computed(() => !!selectedStatement.value && !!selectedEntry.value)

async function manualMatch() {
  const s = selectedStatement.value
  const e = selectedEntry.value
  if (Math.abs(Number(s.amount) - Number(e.amount)) >= 0.005) {
    message.warning(t('fin.bank.amountMismatch', { bank: formatMoney(s.amount), book: formatMoney(e.amount) }))
    return
  }
  await matchBank(s.id, e.entryId)
  message.success(t('fin.bank.matched'))
  load()
}

const unmatch = useAction(unmatchBank, { successKey: 'fin.bank.unmatchedMsg', reload: () => load() })

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

const removeStatements = useAction(() => deleteBankStatements(stmtKeys.value), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

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

// ---------------------------------------------------------------- 余额调节表
const reconcileOpen = ref(false)
const reconciling = ref(false)
const asOf = ref(today())
const bankBalance = ref()
const rec = ref()
const bankOnlyColumns = [
  { title: t('common.field.date'), dataIndex: 'txnDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.bank.summary'), dataIndex: 'summary', ellipsis: true }
]
const bookOnlyColumns = [
  { title: t('fin.bank.voucher'), dataIndex: 'voucherNo', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.bank.summary'), dataIndex: 'summary', ellipsis: true }
]
function openReconcile() {
  asOf.value = range.value?.[1] || today()
  reconcileOpen.value = true
  loadReconcile()
}
async function loadReconcile() {
  reconciling.value = true
  try {
    rec.value = await bankReconcile({ accountId: accountId.value, asOf: asOf.value, bankBalance: bankBalance.value ?? undefined })
  } finally {
    reconciling.value = false
  }
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
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.balance {
  width: 200px;
}
.report-title {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}
.report-sub {
  text-align: center;
  color: var(--app-text-3);
  margin: 6px 0 12px;
}
.check {
  margin-left: 8px;
}
.rec-table {
  width: 100%;
  border-collapse: collapse;
}
.rec-table th,
.rec-table td {
  border: 1px solid var(--app-border, #d9d9d9);
  padding: 8px 10px;
}
.rec-table th {
  background: var(--app-bg-2, #fafafa);
  font-weight: 500;
  text-align: left;
}
.rec-table th.num {
  text-align: right;
}
.rec-table tfoot td {
  font-weight: 600;
}
.rec-detail {
  margin-top: 16px;
}
.rec-detail .section-title {
  margin: 8px 0;
}
</style>
