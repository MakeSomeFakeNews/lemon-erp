<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <!-- ---------------------------------------------------------------- 启用向导 -->
        <a-tab-pane key="guide" :tab="$t('fin.opening.guideTab')">
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.opening.guideTip')"
          />
          <a-spin :spinning="loadingSummary">
            <div v-if="summary" class="guide">
              <div class="guide-step">
                <div class="guide-no done">1</div>
                <div class="guide-body">
                  <div class="guide-title">{{ $t('fin.opening.step1') }}</div>
                  <div class="sub-text">{{ $t('fin.opening.step1Tip') }}</div>
                </div>
              </div>
              <div class="guide-step">
                <div :class="['guide-no', { done: summary.accounts.count && accountsBalanced }]">2</div>
                <div class="guide-body">
                  <div class="guide-title">{{ $t('fin.opening.accountTab') }}</div>
                  <div class="sub-text">
                    {{ $t('fin.opening.step2Tip', { n: summary.accounts.count, debit: money(summary.accounts.debit), credit: money(summary.accounts.credit) }) }}
                    <span v-if="summary.accounts.count && !accountsBalanced" class="bad">{{ $t('fin.opening.unbalanced') }}</span>
                  </div>
                </div>
                <a-button @click="tab = 'account'">{{ $t('fin.opening.goEnter') }}</a-button>
              </div>
              <div class="guide-step">
                <div :class="['guide-no', { done: summary.stockDocs.length }]">3</div>
                <div class="guide-body">
                  <div class="guide-title">{{ $t('fin.opening.step3') }}</div>
                  <div class="sub-text">
                    {{ $t('fin.opening.step3Tip') }}
                    {{ $t('fin.opening.step3Done', { n: summary.stockDocs.length, qty: formatQty(stockDocQty) }) }}
                  </div>
                  <div v-if="summary.stockDocs.length" class="guide-docs">
                    <a-tag v-for="d in summary.stockDocs" :key="d.id" :bordered="false" :color="d.reversed ? 'default' : 'gold'">
                      {{ d.docNo }} {{ d.warehouseName }} {{ formatQty(d.totalQty) }}{{ d.reversed ? $t('fin.opening.reversed') : '' }}
                    </a-tag>
                  </div>
                </div>
                <a-space v-perm="'fin:opening:edit'">
                  <span class="sub-text">{{ $t('fin.opening.startPeriod') }}</span>
                  <PeriodPicker v-model:value="guideStart" :disabled="!!startPeriod && stock.locked" />
                  <a-button :disabled="!guideStart" @click="stockInImportOpen = true">
                    <template #icon><ImportOutlined /></template>{{ $t('fin.opening.importStock') }}
                  </a-button>
                </a-space>
              </div>
              <div class="guide-step">
                <div :class="['guide-no', { done: summary.stock.saved }]">4</div>
                <div class="guide-body">
                  <div class="guide-title">{{ $t('fin.opening.stockTab') }}</div>
                  <div class="sub-text">
                    {{ $t('fin.opening.step4Tip') }}
                    {{ summary.stock.saved ? $t('fin.opening.savedAt', { period: periodText(summary.stock.openingPeriod) }) : $t('fin.opening.notSaved') }}
                  </div>
                </div>
                <a-button @click="tab = 'stock'">{{ $t('fin.opening.goEnter') }}</a-button>
              </div>
              <div class="guide-step">
                <div :class="['guide-no', { done: summary.bills.AR.count || summary.bills.AP.count }]">5</div>
                <div class="guide-body">
                  <div class="guide-title">{{ $t('fin.opening.step5') }}</div>
                  <div class="sub-text">{{ $t('fin.opening.step5Tip') }}</div>
                  <div v-for="k in ['AR', 'AP']" :key="k" class="guide-check">
                    {{ $t(k === 'AR' ? 'fin.opening.arCheck' : 'fin.opening.apCheck', { n: summary.bills[k].count, amount: money(summary.bills[k].amount) }) }}
                    {{ $t('fin.opening.ledgerCheck', { code: summary.bills[k].accountCode, amount: money(summary.bills[k].ledger) }) }}
                    <a-tag v-if="Math.abs(summary.bills[k].amount - summary.bills[k].ledger) < 0.005" color="green" :bordered="false">{{ $t('fin.opening.match') }}</a-tag>
                    <a-tag v-else color="orange" :bordered="false">{{ $t('fin.opening.diff', { amount: money(summary.bills[k].amount - summary.bills[k].ledger) }) }}</a-tag>
                  </div>
                </div>
                <a-space v-perm="'fin:opening:edit'" direction="vertical">
                  <a-button @click="openBillImport('AR')"><template #icon><ImportOutlined /></template>{{ $t('fin.opening.importAr') }}</a-button>
                  <a-button @click="openBillImport('AP')"><template #icon><ImportOutlined /></template>{{ $t('fin.opening.importAp') }}</a-button>
                </a-space>
              </div>
            </div>
          </a-spin>
        </a-tab-pane>

        <!-- ---------------------------------------------------------------- 科目期初 -->
        <a-tab-pane key="account" :tab="$t('fin.opening.accountTab')">
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="$t('fin.opening.accountTip')"
          />
          <div class="toolbar">
            <a-input-search v-model:value="accountKeyword" :placeholder="$t('fin.opening.accountKeyword')" allow-clear class="search" />
            <a-checkbox v-model:checked="onlyFilled">{{ $t('fin.opening.onlyFilled') }}</a-checkbox>
            <span class="spacer" />
            <span :class="['balance', { bad: !balanced }]">
              {{ $t('fin.opening.debit') }} {{ money(totals.debit) }} · {{ $t('fin.opening.credit') }} {{ money(totals.credit) }}
              <template v-if="!balanced">· {{ $t('fin.opening.difference') }} {{ money(totals.debit - totals.credit) }}</template>
            </span>
            <a-button v-perm="'fin:opening:edit'" @click="accountImportOpen = true">
              <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
            </a-button>
            <a-button v-perm="'fin:opening:edit'" type="primary" :loading="savingAccounts" @click="submitAccounts">{{ $t('fin.opening.saveAccounts') }}</a-button>
          </div>
          <a-table
            row-key="accountId"
            size="small"
            bordered
            :columns="accountColumns"
            :data-source="filteredAccounts"
            :loading="loadingAccounts"
            :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (total) => $t('fin.opening.totalAccounts', { total }) }"
            :scroll="{ x: 760, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'category'"><DictTag type="fin_account_category" :value="record.category" /></template>
              <template v-else-if="column.key === 'direction'">{{ record.direction === 'DEBIT' ? $t('fin.opening.dr') : $t('fin.opening.cr') }}</template>
              <template v-else-if="column.key === 'debit' || column.key === 'credit'">
                <a-input-number
                  v-model:value="record[column.key]"
                  :min="0"
                  :precision="2"
                  :disabled="!canEdit"
                  size="small"
                  class="full"
                  :placeholder="record.direction === (column.key === 'debit' ? 'DEBIT' : 'CREDIT') ? '' : '-'"
                />
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- ---------------------------------------------------------------- 存货期初 -->
        <a-tab-pane key="stock" :tab="$t('fin.opening.stockTab')">
          <a-alert type="info" show-icon class="tip">
            <template #message>
              {{ $t('fin.opening.stockTip') }}
            </template>
          </a-alert>
          <div class="toolbar">
            <span>{{ $t('fin.opening.startPeriod') }}</span>
            <PeriodPicker :value="startPeriod" :disabled="stock.locked" @update:value="changeStart" />
            <a-tag v-if="stock.locked" color="orange" :bordered="false">{{ $t('fin.opening.locked') }}</a-tag>
            <span class="spacer" />
            <span v-for="c in stock.checks" :key="c.accountCode" :class="['balance', { bad: Number(c.stockAmount) !== Number(c.ledgerAmount) }]">
              {{ $t('fin.opening.stockCheck', { code: c.accountCode, name: c.accountName, stock: money(c.stockAmount), ledger: money(c.ledgerAmount) }) }}
            </span>
            <a-button v-perm="'fin:opening:edit'" :disabled="stock.locked || !startPeriod" @click="stockImportOpen = true">
              <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
            </a-button>
            <a-button
              v-perm="'fin:opening:edit'"
              type="primary"
              :disabled="stock.locked || !startPeriod"
              :loading="savingStock"
              @click="submitStock"
            >
              {{ $t('fin.opening.saveStock') }}
            </a-button>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="stockColumns"
            :data-source="stockRows"
            :loading="loadingStock"
            :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (total) => $t('fin.opening.totalMaterials', { total }) }"
            :scroll="{ x: stockScrollX, y: '100%' }"
            :locale="{ emptyText: startPeriod ? $t('fin.opening.noStock') : $t('fin.opening.selectStartFirst') }"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'material'">
                <MaterialSelect
                  v-if="!record.materialCode"
                  :value="record.materialId"
                  @update:value="(v) => (record.materialId = v)"
                  @select="(m) => Object.assign(record, { materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit })"
                />
                <template v-else>
                  <div>{{ record.materialCode }} {{ record.materialName }}</div>
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
              </template>
              <template v-else-if="column.key === 'bookQty'">{{ record.bookQty == null ? '-' : formatQty(record.bookQty) }} {{ record.unit }}</template>
              <template v-else-if="column.key === 'qty'">
                <a-input-number v-model:value="record.qty" :min="0" :precision="4" :disabled="stock.locked" size="small" class="full" />
              </template>
              <template v-else-if="column.key === 'price'">
                <a-input-number
                  :value="record.qty ? Math.round((record.amount / record.qty) * 10000) / 10000 : undefined"
                  :min="0"
                  :precision="4"
                  :disabled="stock.locked || !record.qty"
                  size="small"
                  class="full"
                  @change="(v) => (record.amount = Math.round((v || 0) * record.qty * 100) / 100)"
                />
              </template>
              <template v-else-if="column.key === 'amount'">
                <a-input-number v-model:value="record.amount" :min="0" :precision="2" :disabled="stock.locked" size="small" class="full" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="record.bookQty == null && !stock.locked" type="link" danger size="small" @click="stockRows.splice(index, 1)">
                  <DeleteOutlined />
                </a-button>
              </template>
            </template>
          </a-table>
          <a-button v-if="!stock.locked && startPeriod" type="dashed" block class="add-row" @click="addStockRow">
            <template #icon><PlusOutlined /></template>{{ $t('fin.opening.addMaterial') }}
          </a-button>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <ImportModal
      v-model:open="accountImportOpen"
      :title="$t('fin.opening.accountTab')"
      :columns="accountImportColumns"
      :importer="fillAccounts"
      :sample="{ code: '1001', debit: 10000 }"
    />
    <ImportModal
      v-model:open="stockInImportOpen"
      :title="$t('fin.opening.step3')"
      :columns="stockInImportColumns"
      :importer="(data) => importOpeningStock(guideStart, data)"
      :sample="{ warehouse: $t('fin.opening.sampleWarehouse'), materialCode: 'M0001', qty: 100 }"
      @success="loadSummary(); loadStock(guideStart)"
    />
    <ImportModal
      v-model:open="billImportOpen"
      :title="billImportKind === 'AR' ? $t('fin.opening.openingAr') : $t('fin.opening.openingAp')"
      :columns="billImportColumns"
      :importer="(data) => importOpeningBills(billImportKind, data)"
      :sample="{ partner: billImportKind === 'AR' ? 'C0001' : 'S0001', billDate: '2026-01-31', amount: 10000 }"
      @success="loadSummary"
    />
    <ImportModal
      v-model:open="stockImportOpen"
      :title="$t('fin.opening.stockTab')"
      :columns="stockImportColumns"
      :importer="fillStock"
      :sample="{ code: 'M0001', qty: 100, amount: 1000 }"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { accountOpenings, importOpeningBills, importOpeningStock, openingSummary, saveAccountOpenings, saveStockOpening, stockOpening } from '@/api/fin'
import { materialOptions } from '@/api/wms/material'
import { formatMoney, periodText } from '@/components/fin/fin'
import { formatQty, rowKey } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'

useDict('fin_account_category')
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('fin:opening:edit'))
const money = (n) => formatMoney(Number(n) || 0)
const tab = ref('guide')

// ---------------------------------------------------------------- 启用向导
const summary = ref(null)
const loadingSummary = ref(false)
const guideStart = ref()
const accountsBalanced = computed(() => Math.abs(Number(summary.value.accounts.debit) - Number(summary.value.accounts.credit)) < 0.005)
const stockDocQty = computed(() => (summary.value?.stockDocs || []).filter((d) => !d.reversed).reduce((s, d) => s + Number(d.totalQty), 0))

async function loadSummary() {
  loadingSummary.value = true
  try {
    summary.value = await openingSummary()
  } finally {
    loadingSummary.value = false
  }
}

const stockInImportOpen = ref(false)
const stockInImportColumns = [
  { title: t('fin.opening.warehouse'), key: 'warehouse', required: true },
  { title: t('fin.opening.location'), key: 'location' },
  { title: t('fin.opening.materialCode'), key: 'materialCode', required: true },
  { title: t('fin.opening.materialName'), key: 'materialName' },
  { title: t('fin.opening.batchNo'), key: 'batchNo' },
  { title: t('common.field.quantity'), key: 'qty', type: 'number', required: true },
  { title: t('fin.opening.serialNos'), key: 'serialNos' },
  { title: t('fin.opening.productionDate'), key: 'productionDate', type: 'date' },
  { title: t('fin.opening.expiryDate'), key: 'expiryDate', type: 'date' }
]

const billImportOpen = ref(false)
const billImportKind = ref('AR')
const billImportColumns = [
  { title: t('fin.opening.partnerCol'), key: 'partner', required: true },
  { title: t('fin.opening.billDate'), key: 'billDate', type: 'date', required: true },
  { title: t('fin.opening.dueDate'), key: 'dueDate', type: 'date' },
  { title: t('common.field.amount'), key: 'amount', type: 'number', required: true },
  { title: t('common.field.remark'), key: 'remark' }
]
function openBillImport(kind) {
  billImportKind.value = kind
  billImportOpen.value = true
}

// ---------------------------------------------------------------- 科目期初
const accountColumns = [
  { title: t('fin.opening.accountCode'), dataIndex: 'code', width: 120 },
  { title: t('fin.opening.accountName'), dataIndex: 'name', width: 200 },
  { title: t('fin.opening.category'), key: 'category', width: 100 },
  { title: t('fin.opening.direction'), key: 'direction', width: 80, align: 'center' },
  { title: t('fin.opening.openingDebit'), key: 'debit', width: 150 },
  { title: t('fin.opening.openingCredit'), key: 'credit', width: 150 }
]
const accounts = ref([])
const loadingAccounts = ref(false)
const savingAccounts = ref(false)
const accountKeyword = ref('')
const onlyFilled = ref(false)
const filteredAccounts = computed(() => {
  const kw = accountKeyword.value.trim()
  return accounts.value.filter(
    (a) => (!kw || a.code.includes(kw) || a.name.includes(kw)) && (!onlyFilled.value || Number(a.debit) || Number(a.credit))
  )
})
const totals = computed(() => ({
  debit: accounts.value.reduce((s, a) => s + (Number(a.debit) || 0), 0),
  credit: accounts.value.reduce((s, a) => s + (Number(a.credit) || 0), 0)
}))
const balanced = computed(() => Math.abs(totals.value.debit - totals.value.credit) < 0.005)

async function loadAccounts() {
  loadingAccounts.value = true
  try {
    accounts.value = (await accountOpenings()).map((a) => ({ ...a, debit: Number(a.debit) || null, credit: Number(a.credit) || null }))
  } finally {
    loadingAccounts.value = false
  }
}

// 导入：按科目编码填入表格，核对后再保存
const accountImportOpen = ref(false)
const accountImportColumns = [
  { title: t('fin.opening.accountCode'), key: 'code', required: true },
  { title: t('fin.opening.accountName'), key: 'name' },
  { title: t('fin.opening.openingDebit'), key: 'debit', type: 'number' },
  { title: t('fin.opening.openingCredit'), key: 'credit', type: 'number' }
]
async function fillAccounts({ rows }) {
  const byCode = new Map(accounts.value.map((a) => [a.code, a]))
  const errors = []
  rows.forEach(({ rowNum, data }) => {
    const a = byCode.get(data.code)
    if (!a) errors.push({ rowNum, message: t('fin.opening.accountNotFound', { code: data.code }) })
    else if (data.debit < 0 || data.credit < 0) errors.push({ rowNum, message: t('fin.opening.amountNegative') })
  })
  if (!errors.length) {
    rows.forEach(({ data }) => Object.assign(byCode.get(data.code), { debit: data.debit || null, credit: data.credit || null }))
    message.info(t('fin.opening.filledAccounts'))
  }
  return { total: rows.length, imported: errors.length ? 0 : rows.length, errors }
}

async function submitAccounts() {
  if (!balanced.value) {
    message.warning(t('fin.opening.notBalanced'))
    return
  }
  savingAccounts.value = true
  try {
    await saveAccountOpenings(
      accounts.value.filter((a) => Number(a.debit) || Number(a.credit)).map((a) => ({ accountId: a.accountId, debit: a.debit || 0, credit: a.credit || 0 }))
    )
    message.success(t('fin.opening.accountsSaved'))
    loadAccounts()
    loadStock()
  } finally {
    savingAccounts.value = false
  }
}

// ---------------------------------------------------------------- 存货期初
const stockColumns = [
  { title: t('fin.opening.material'), key: 'material', width: 290 },
  { title: t('fin.opening.bookQty'), key: 'bookQty', width: 130, align: 'right' },
  { title: t('fin.opening.openingQty'), key: 'qty', width: 140 },
  { title: t('common.field.price'), key: 'price', width: 140 },
  { title: t('fin.opening.openingAmount'), key: 'amount', width: 160 },
  { title: '', key: 'action', width: 50 }
]
const stockScrollX = stockColumns.reduce((sum, c) => sum + c.width, 0)
const stock = ref({ locked: false, checks: [], rows: [] })
const stockRows = ref([])
const startPeriod = ref()
const loadingStock = ref(false)
const savingStock = ref(false)

async function loadStock(period) {
  loadingStock.value = true
  try {
    stock.value = await stockOpening(period)
    startPeriod.value = stock.value.startPeriod ?? period
    guideStart.value ??= startPeriod.value
    stockRows.value = stock.value.rows.map((r) => ({ ...r, key: rowKey(), qty: Number(r.qty), amount: Number(r.amount) }))
  } finally {
    loadingStock.value = false
  }
}

function changeStart(p) {
  startPeriod.value = p
  loadStock(p)
}

function addStockRow() {
  stockRows.value.push({ key: rowKey(), materialId: undefined, bookQty: null, qty: null, amount: null })
}

const stockImportOpen = ref(false)
const stockImportColumns = [
  { title: t('fin.opening.materialCode'), key: 'code', required: true },
  { title: t('fin.opening.materialName'), key: 'name' },
  { title: t('fin.opening.openingQty'), key: 'qty', type: 'number', required: true },
  { title: t('fin.opening.openingAmount'), key: 'amount', type: 'number', required: true }
]
async function fillStock({ rows }) {
  const byCode = new Map(stockRows.value.filter((r) => r.materialCode).map((r) => [r.materialCode, r]))
  const errors = []
  const found = new Map()
  for (const { rowNum, data } of rows) {
    if (data.qty < 0 || data.amount < 0) {
      errors.push({ rowNum, message: t('fin.opening.qtyAmountNegative') })
      continue
    }
    if (!data.qty && data.amount) {
      errors.push({ rowNum, message: t('fin.opening.zeroQtyAmount') })
      continue
    }
    if (byCode.has(data.code)) continue
    const m = (await materialOptions(data.code)).find((x) => x.code === data.code)
    if (m) found.set(data.code, m)
    else errors.push({ rowNum, message: t('fin.opening.materialNotFound', { code: data.code }) })
  }
  if (!errors.length) {
    rows.forEach(({ data }) => {
      let r = byCode.get(data.code)
      if (!r) {
        const m = found.get(data.code)
        r = { key: rowKey(), materialId: m.id, materialCode: m.code, materialName: m.name, spec: m.spec, unit: m.unit, bookQty: null }
        stockRows.value.push(r)
        byCode.set(data.code, r)
      }
      Object.assign(r, { qty: data.qty, amount: data.amount })
    })
    message.info(t('fin.opening.filledStock'))
  }
  return { total: rows.length, imported: errors.length ? 0 : rows.length, errors }
}

async function submitStock() {
  const items = stockRows.value.filter((r) => r.materialId)
  if (items.some((r) => !r.qty && Number(r.amount))) {
    message.warning(t('fin.opening.zeroQtyAmount'))
    return
  }
  savingStock.value = true
  try {
    await saveStockOpening({
      startPeriod: startPeriod.value,
      items: items.map((r) => ({ materialId: r.materialId, qty: r.qty || 0, amount: r.amount || 0 }))
    })
    message.success(t('fin.opening.stockSaved'))
    loadStock()
  } finally {
    savingStock.value = false
  }
}

watch(tab, (t) => {
  if (t === 'guide') loadSummary()
})

onMounted(() => {
  loadSummary()
  loadAccounts()
  loadStock()
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 12px;
}
.search {
  width: 220px;
}
.spacer {
  flex: 1;
}
.balance {
  color: var(--app-text-2);
  font-size: 13px;
}
.balance.bad {
  color: var(--status-critical);
}
.full {
  width: 100%;
}
.add-row {
  flex: none;
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.guide {
  max-width: 980px;
}
.guide-step {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--app-border);
}
.guide-no {
  flex: none;
  width: 28px;
  height: 28px;
  line-height: 26px;
  text-align: center;
  border-radius: 50%;
  border: 1px solid var(--app-border);
  color: var(--app-text-3);
}
.guide-no.done {
  background: var(--app-primary);
  border-color: transparent;
  color: #fff;
}
.guide-body {
  flex: 1;
  min-width: 0;
}
.guide-title {
  font-weight: 500;
  margin-bottom: 4px;
}
.guide-docs {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 0;
}
.guide-check {
  margin-top: 4px;
  font-size: 13px;
  color: var(--app-text-2);
}
.bad {
  color: var(--status-critical);
}
</style>
