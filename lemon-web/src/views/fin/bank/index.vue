<template>
  <div class="bank-page">
    <a-card :bordered="false" class="page-card">
      <div class="toolbar">
        <a-space wrap>
          <AccountSelect v-model:value="accountId" :prefix="['1002']" placeholder="选择银行账户" class="account" />
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" />
          <a-button :disabled="!accountId" @click="load"><template #icon><ReloadOutlined /></template>查询</a-button>
          <a-checkbox v-model:checked="onlyOpen">只看未勾对</a-checkbox>
        </a-space>
        <a-space wrap>
          <a-button v-perm="'fin:bank:edit'" :disabled="!accountId" @click="importOpen = true">
            <template #icon><UploadOutlined /></template>导入对账单
          </a-button>
          <a-button v-perm="'fin:bank:edit'" :disabled="!accountId" :loading="matching" @click="autoMatch">
            <template #icon><ThunderboltOutlined /></template>自动勾对
          </a-button>
          <a-button v-perm="'fin:bank:edit'" type="primary" :disabled="!canMatch" @click="manualMatch">
            <template #icon><LinkOutlined /></template>手工勾对
          </a-button>
          <a-button :disabled="!accountId" @click="openReconcile">
            <template #icon><FileTextOutlined /></template>余额调节表
          </a-button>
        </a-space>
      </div>
      <div class="sub-text">
        自动勾对按金额相同、日期相差不超过 5 天匹配；手工勾对时在左右两边各选一条未勾对的记录。银行流水收入为正、支出为负，企业分录借方为正、贷方为负；导入时可以只填「金额」一列代替收入、支出两列，流水号重复的自动跳过。
      </div>
    </a-card>

    <a-row :gutter="12" class="tables">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card side-card">
          <div class="side-head">
            <span class="section-title">银行流水</span>
            <a-space>
              <span class="sub-text">
                {{ statements.length }} 条，未勾对 {{ statements.filter((s) => !s.entryId).length }} 条，
                净额 <span :class="{ neg: stmtTotal < 0 }">{{ formatMoney(stmtTotal) }}</span>
              </span>
              <span v-if="stmtKeys.length" v-perm="'fin:bank:edit'">
                <a-popconfirm :title="`删除选中的 ${stmtKeys.length} 条银行流水？`" @confirm="removeStatements">
                  <a-button size="small" danger>删除（{{ stmtKeys.length }}）</a-button>
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
            :locale="{ emptyText: accountId ? '没有银行流水，请导入对账单' : '请先选择银行账户' }"
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
                  <a-tag color="green" :bordered="false">已勾对</a-tag>
                  <div class="sub-text">{{ record.voucherNo }} {{ record.voucherDate }}</div>
                  <span v-perm="'fin:bank:edit'">
                    <a-popconfirm title="取消勾对？" @confirm="unmatch(record.id)">
                      <a class="small-link">取消勾对</a>
                    </a-popconfirm>
                  </span>
                </template>
                <a-tag v-else :bordered="false">未勾对</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card side-card">
          <div class="side-head">
            <span class="section-title">企业账面分录</span>
            <span class="sub-text">
              {{ entries.length }} 条，未勾对 {{ entries.filter((e) => !e.matched).length }} 条，
              净额 <span :class="{ neg: entryTotal < 0 }">{{ formatMoney(entryTotal) }}</span>
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
            :locale="{ emptyText: accountId ? '期间内没有该账户的分录' : '请先选择银行账户' }"
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
                <a-tag :color="record.matched ? 'green' : undefined" :bordered="false">{{ record.matched ? '已勾对' : '未勾对' }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <ImportModal
      v-model:open="importOpen"
      title="银行对账单"
      :columns="importColumns"
      :sample="importSample"
      :importer="importRows"
      @success="load"
    />

    <!-- 余额调节表 -->
    <a-modal v-model:open="reconcileOpen" title="银行存款余额调节表" width="min(900px, 100vw)" :footer="null">
      <div class="filters">
        <span>截止日期</span>
        <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" />
        <span>银行对账单余额</span>
        <a-input-number v-model:value="bankBalance" :precision="2" placeholder="不填按导入的流水累计" class="balance" />
        <a-button type="primary" :loading="reconciling" @click="loadReconcile">计算</a-button>
      </div>
      <a-spin :spinning="reconciling">
        <template v-if="rec">
          <div class="report-title">银行存款余额调节表</div>
          <div class="report-sub">
            {{ accountName }} · 截至 {{ rec.asOf }} · 单位：元
            <a-tag :color="rec.balanced ? 'green' : 'red'" :bordered="false" class="check">
              {{ rec.balanced ? '调节后余额相等' : '调节后余额不相等，请检查未勾对的记录' }}
            </a-tag>
          </div>
          <table class="rec-table">
            <thead>
              <tr>
                <th>企业账面</th>
                <th class="num">金额</th>
                <th>银行对账单</th>
                <th class="num">金额</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>银行存款日记账余额</td>
                <td class="num">{{ formatMoney(rec.bookBalance) }}</td>
                <td>银行对账单余额</td>
                <td class="num">{{ formatMoney(rec.bankBalance) }}</td>
              </tr>
              <tr>
                <td>加：银行已收、企业未收</td>
                <td class="num">{{ formatMoney(rec.bankIn) }}</td>
                <td>加：企业已收、银行未收</td>
                <td class="num">{{ formatMoney(rec.bookIn) }}</td>
              </tr>
              <tr>
                <td>减：银行已付、企业未付</td>
                <td class="num">{{ formatMoney(rec.bankOut) }}</td>
                <td>减：企业已付、银行未付</td>
                <td class="num">{{ formatMoney(rec.bookOut) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td>调节后余额</td>
                <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBook) }}</td>
                <td>调节后余额</td>
                <td :class="['num', { neg: !rec.balanced }]">{{ formatMoney(rec.adjustedBank) }}</td>
              </tr>
            </tfoot>
          </table>
          <a-row :gutter="12" class="rec-detail">
            <a-col :xs="24" :md="12">
              <div class="section-title">银行已记、企业未记（{{ rec.bankOnly.length }}）</div>
              <a-table row-key="id" size="small" :columns="bankOnlyColumns" :data-source="rec.bankOnly" :pagination="false" :scroll="{ y: 240 }">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">
                    <span :class="['num', { neg: Number(record.amount) < 0 }]">{{ formatMoney(record.amount) }}</span>
                  </template>
                </template>
              </a-table>
            </a-col>
            <a-col :xs="24" :md="12">
              <div class="section-title">企业已记、银行未记（{{ rec.bookOnly.length }}）</div>
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

const accountId = ref()
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), today()])
const onlyOpen = ref(false)
const loading = ref(false)
const statements = ref([])
const entries = ref([])
const stmtKeys = ref([])
const entryKeys = ref([])
const round2 = (n) => Math.round(n * 100) / 100

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
  { title: '交易日期', dataIndex: 'txnDate', width: 105 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '摘要 / 流水号', key: 'summary', ellipsis: true },
  { title: '勾对', key: 'match', width: 150 }
]
const entryColumns = [
  { title: '凭证', key: 'voucher', width: 120 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '摘要', dataIndex: 'summary', ellipsis: true },
  { title: '勾对', key: 'match', width: 90 }
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
    message.warning(`金额不一致：银行流水 ${formatMoney(s.amount)}，账面分录 ${formatMoney(e.amount)}`)
    return
  }
  await matchBank(s.id, e.entryId)
  message.success('已勾对')
  load()
}

async function unmatch(id) {
  await unmatchBank(id)
  message.success('已取消勾对')
  load()
}

const matching = ref(false)
async function autoMatch() {
  matching.value = true
  try {
    const count = await autoMatchBank(params())
    message.success(count ? `自动勾对 ${count} 条` : '没有可以自动勾对的记录')
    load()
  } finally {
    matching.value = false
  }
}

async function removeStatements() {
  await deleteBankStatements(stmtKeys.value)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 导入
const importOpen = ref(false)
const importColumns = [
  { title: '交易日期', key: 'txnDate', required: true, type: 'date', width: 14 },
  { title: '收入', key: 'income', type: 'number', width: 14 },
  { title: '支出', key: 'expense', type: 'number', width: 14 },
  { title: '金额', key: 'amount', type: 'number', width: 14 },
  { title: '摘要', key: 'summary', width: 30 },
  { title: '流水号', key: 'refNo', width: 24 }
]
const importSample = { txnDate: '2026-10-08', income: '10000.00', expense: '', amount: '', summary: '收到货款', refNo: 'B202610080001' }

async function importRows({ rows }) {
  const errors = []
  const data = []
  rows.forEach(({ rowNum, data: d }) => {
    const amount = d.amount !== undefined ? d.amount : round2((d.income || 0) - (d.expense || 0))
    if (!amount) {
      errors.push({ rowNum, message: '请填写收入、支出或金额' })
      return
    }
    data.push({ txnDate: d.txnDate, amount, summary: d.summary || null, refNo: d.refNo || null })
  })
  if (errors.length) return { total: rows.length, imported: 0, errors }
  const imported = await importBankStatements({ accountId: accountId.value, rows: data })
  if (imported < rows.length) message.info(`${rows.length - imported} 条流水号重复，已跳过`)
  return { total: rows.length, imported, errors: [] }
}

// ---------------------------------------------------------------- 余额调节表
const reconcileOpen = ref(false)
const reconciling = ref(false)
const asOf = ref(today())
const bankBalance = ref()
const rec = ref()
const bankOnlyColumns = [
  { title: '日期', dataIndex: 'txnDate', width: 100 },
  { title: '金额', key: 'amount', width: 110, align: 'right' },
  { title: '摘要', dataIndex: 'summary', ellipsis: true }
]
const bookOnlyColumns = [
  { title: '凭证', dataIndex: 'voucherNo', width: 100 },
  { title: '金额', key: 'amount', width: 110, align: 'right' },
  { title: '摘要', dataIndex: 'summary', ellipsis: true }
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
