<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <!-- ---------------------------------------------------------------- 科目期初 -->
        <a-tab-pane key="account" tab="科目期初">
          <a-alert
            type="info"
            show-icon
            class="tip"
            message="录入系统启用时各末级科目的余额，借方合计必须等于贷方合计；计入余额表、明细账、资产负债表的期初。已有结账的期间后不能修改。"
          />
          <div class="toolbar">
            <a-input-search v-model:value="accountKeyword" placeholder="科目编码或名称" allow-clear class="search" />
            <a-checkbox v-model:checked="onlyFilled">只看有余额的科目</a-checkbox>
            <span class="spacer" />
            <span :class="['balance', { bad: !balanced }]">
              借方 {{ money(totals.debit) }} · 贷方 {{ money(totals.credit) }}
              <template v-if="!balanced">· 差额 {{ money(totals.debit - totals.credit) }}</template>
            </span>
            <a-button v-perm="'fin:opening:edit'" @click="accountImportOpen = true">
              <template #icon><ImportOutlined /></template>导入
            </a-button>
            <a-button v-perm="'fin:opening:edit'" type="primary" :loading="savingAccounts" @click="submitAccounts">保存科目期初</a-button>
          </div>
          <a-table
            row-key="accountId"
            size="small"
            bordered
            :columns="accountColumns"
            :data-source="filteredAccounts"
            :loading="loadingAccounts"
            :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (t) => `共 ${t} 个末级科目` }"
            :scroll="{ x: 760, y: '100%' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'category'"><DictTag type="fin_account_category" :value="record.category" /></template>
              <template v-else-if="column.key === 'direction'">{{ record.direction === 'DEBIT' ? '借' : '贷' }}</template>
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
        <a-tab-pane key="stock" tab="存货期初">
          <a-alert type="info" show-icon class="tip">
            <template #message>
              录入启用期间之前的结存数量和金额，作为启用期间成本核算的期初；数量默认按库存流水推算。
              存货期初金额应与「原材料」「库存商品」的科目期初一致。核算过成本后不能修改。
            </template>
          </a-alert>
          <div class="toolbar">
            <span>启用期间</span>
            <PeriodPicker :value="startPeriod" :disabled="stock.locked" @update:value="changeStart" />
            <a-tag v-if="stock.locked" color="orange" :bordered="false">已核算成本，不能修改</a-tag>
            <span class="spacer" />
            <span v-for="c in stock.checks" :key="c.accountCode" :class="['balance', { bad: Number(c.stockAmount) !== Number(c.ledgerAmount) }]">
              {{ c.accountCode }} {{ c.accountName }}：存货 {{ money(c.stockAmount) }} / 科目期初 {{ money(c.ledgerAmount) }}
            </span>
            <a-button v-perm="'fin:opening:edit'" :disabled="stock.locked || !startPeriod" @click="stockImportOpen = true">
              <template #icon><ImportOutlined /></template>导入
            </a-button>
            <a-button
              v-perm="'fin:opening:edit'"
              type="primary"
              :disabled="stock.locked || !startPeriod"
              :loading="savingStock"
              @click="submitStock"
            >
              保存存货期初
            </a-button>
          </div>
          <a-table
            row-key="key"
            size="small"
            bordered
            :columns="stockColumns"
            :data-source="stockRows"
            :loading="loadingStock"
            :pagination="{ pageSize: 50, showSizeChanger: false, showTotal: (t) => `共 ${t} 个物料` }"
            :scroll="{ x: 900, y: '100%' }"
            :locale="{ emptyText: startPeriod ? '启用期间之前没有库存' : '请先选择启用期间' }"
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
            <template #icon><PlusOutlined /></template>添加物料
          </a-button>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <ImportModal
      v-model:open="accountImportOpen"
      title="科目期初"
      :columns="accountImportColumns"
      :importer="fillAccounts"
      :sample="{ code: '1001', debit: 10000 }"
    />
    <ImportModal
      v-model:open="stockImportOpen"
      title="存货期初"
      :columns="stockImportColumns"
      :importer="fillStock"
      :sample="{ code: 'M0001', qty: 100, amount: 1000 }"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { accountOpenings, saveAccountOpenings, saveStockOpening, stockOpening } from '@/api/fin'
import { materialOptions } from '@/api/wms/material'
import { formatMoney } from '@/components/fin/fin'
import { formatQty, rowKey } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'

useDict('fin_account_category')
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('fin:opening:edit'))
const money = (n) => formatMoney(Number(n) || 0)
const tab = ref('account')

// ---------------------------------------------------------------- 科目期初
const accountColumns = [
  { title: '科目编码', dataIndex: 'code', width: 110 },
  { title: '科目名称', dataIndex: 'name', width: 200 },
  { title: '类别', key: 'category', width: 90 },
  { title: '方向', key: 'direction', width: 60, align: 'center' },
  { title: '期初借方', key: 'debit', width: 150 },
  { title: '期初贷方', key: 'credit', width: 150 }
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
  { title: '科目编码', key: 'code', required: true },
  { title: '科目名称', key: 'name' },
  { title: '期初借方', key: 'debit', type: 'number' },
  { title: '期初贷方', key: 'credit', type: 'number' }
]
async function fillAccounts({ rows }) {
  const byCode = new Map(accounts.value.map((a) => [a.code, a]))
  const errors = []
  rows.forEach(({ rowNum, data }) => {
    const a = byCode.get(data.code)
    if (!a) errors.push({ rowNum, message: `科目「${data.code}」不存在或不是末级科目` })
    else if (data.debit < 0 || data.credit < 0) errors.push({ rowNum, message: '金额不能小于0' })
  })
  if (!errors.length) {
    rows.forEach(({ data }) => Object.assign(byCode.get(data.code), { debit: data.debit || null, credit: data.credit || null }))
    message.info('已填入表格，请核对后点击「保存科目期初」')
  }
  return { total: rows.length, imported: errors.length ? 0 : rows.length, errors }
}

async function submitAccounts() {
  if (!balanced.value) {
    message.warning('借方合计与贷方合计不相等')
    return
  }
  savingAccounts.value = true
  try {
    await saveAccountOpenings(
      accounts.value.filter((a) => Number(a.debit) || Number(a.credit)).map((a) => ({ accountId: a.accountId, debit: a.debit || 0, credit: a.credit || 0 }))
    )
    message.success('科目期初已保存')
    loadAccounts()
    loadStock()
  } finally {
    savingAccounts.value = false
  }
}

// ---------------------------------------------------------------- 存货期初
const stockColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '账面结存', key: 'bookQty', width: 120, align: 'right' },
  { title: '期初数量', key: 'qty', width: 140 },
  { title: '单价', key: 'price', width: 140 },
  { title: '期初金额', key: 'amount', width: 160 },
  { title: '', key: 'action', width: 50 }
]
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
  { title: '物料编码', key: 'code', required: true },
  { title: '物料名称', key: 'name' },
  { title: '期初数量', key: 'qty', type: 'number', required: true },
  { title: '期初金额', key: 'amount', type: 'number', required: true }
]
async function fillStock({ rows }) {
  const byCode = new Map(stockRows.value.filter((r) => r.materialCode).map((r) => [r.materialCode, r]))
  const errors = []
  const found = new Map()
  for (const { rowNum, data } of rows) {
    if (data.qty < 0 || data.amount < 0) {
      errors.push({ rowNum, message: '数量和金额不能小于0' })
      continue
    }
    if (!data.qty && data.amount) {
      errors.push({ rowNum, message: '数量为 0 的物料不能有期初金额' })
      continue
    }
    if (byCode.has(data.code)) continue
    const m = (await materialOptions(data.code)).find((x) => x.code === data.code)
    if (m) found.set(data.code, m)
    else errors.push({ rowNum, message: `物料「${data.code}」不存在或已停用` })
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
    message.info('已填入表格，请核对后点击「保存存货期初」')
  }
  return { total: rows.length, imported: errors.length ? 0 : rows.length, errors }
}

async function submitStock() {
  const items = stockRows.value.filter((r) => r.materialId)
  if (items.some((r) => !r.qty && Number(r.amount))) {
    message.warning('数量为 0 的物料不能有期初金额')
    return
  }
  savingStock.value = true
  try {
    await saveStockOpening({
      startPeriod: startPeriod.value,
      items: items.map((r) => ({ materialId: r.materialId, qty: r.qty || 0, amount: r.amount || 0 }))
    })
    message.success('存货期初已保存')
    loadStock()
  } finally {
    savingStock.value = false
  }
}

watch(tab, (t) => {
  if (t === 'stock' && !stock.value.rows.length && startPeriod.value === undefined) loadStock()
})

onMounted(loadAccounts)
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
</style>
