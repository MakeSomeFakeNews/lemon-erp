<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.quotation.createTitle')" :edit-title="$t('scm.quotation.editTitle')" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('scm.quotation.customer')" name="customerId">
            <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" @select="onCustomerSelect" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.quotation.quoteDate')" name="quoteDate">
            <a-date-picker v-model:value="form.quoteDate" value-format="YYYY-MM-DD" class="full" @change="refreshPrices" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.quotation.validTo')" name="validTo">
            <a-date-picker v-model:value="form.validTo" value-format="YYYY-MM-DD" :placeholder="$t('scm.quotation.optional')" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('scm.quotation.opportunity')" name="opportunityId">
            <a-tag v-if="form.opportunityId" closable class="opp-tag" @close="form.opportunityId = undefined">
              {{ $t('scm.quotation.opportunityNo', { id: form.opportunityId }) }}
            </a-tag>
            <span v-else class="sub-text opp-tag">{{ $t('scm.quotation.noOpportunity') }}</span>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.taxRate.invoiceKind')" name="invoiceKind" :tooltip="$t('scm.taxRate.invoiceKindExtra')">
            <InvoiceKindSelect v-model:value="form.invoiceKind" @change="onInvoiceKindChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="18">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('scm.quotation.remarkPh')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">
          {{ $t('scm.quotation.itemsTitle', { n: form.items.length, amount: formatMoney(totalAmount) }) }}
        </span>
        <a-button type="primary" ghost @click="addRow">
          <template #icon><PlusOutlined /></template>{{ $t('scm.quotation.addMaterial') }}
        </a-button>
      </div>

      <LineItemsTable class="items-table" :items="form.items" :columns="columns" :empty-text="$t('scm.quotation.emptyItems')">
        <template #header="{ column }">
          <TaxRateTitle v-if="column.key === 'taxRate'" :title="column.title" @apply="applyAllTaxRate" />
        </template>
        <template #cell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule($t('scm.quotation.selectMaterial'))" class="cell-item">
              <MaterialSelect virtual v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="requiredRule($t('scm.quotation.inputQty'))" class="cell-item">
              <a-input-number
                v-model:value="record.qty"
                :min="0"
                :precision="4"
                :addon-after="record.unit || undefined"
                class="full"
                @change="fetchPrice(record)"
              />
            </a-form-item>
            <a v-if="record.materialId" class="atp-link" @click="openAtp(record)">{{ $t('scm.quotation.atp') }}</a>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-form-item :name="['items', index, 'price']" :rules="requiredRule($t('scm.quotation.inputPrice'))" class="cell-item">
              <a-input-number
                v-model:value="record.price"
                :min="0"
                :precision="4"
                prefix="¥"
                class="full"
                @change="record.priceAuto = false"
              />
            </a-form-item>
            <div v-if="record.priceSource && record.priceAuto" class="sub-text">{{ $t(`pricing.source.${record.priceSource}`) }}</div>
            <PriceHint :price="record.price" :list-price="record.listPrice" />
            <a v-if="record.materialId" class="atp-link" @click="openReference(record)">
              <HistoryOutlined /> {{ $t('pricing.reference.open') }}
            </a>
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount">¥{{ formatMoney(lineAmount(record.qty, record.price)) }}</div>
            <div class="sub-text">{{ $t('scm.quotation.taxAmountVal', { amount: formatMoney(lineTax(lineAmount(record.qty, record.price), record.taxRate)) }) }}</div>
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
        </template>
      </LineItemsTable>
      <WholeDiscountBar v-if="form.items.length" v-model:value="form.wholeDiscount" :total="round2(totalAmount)" />
    </a-form>
    <AtpModal
      v-model:open="atpOpen"
      :material-id="atpRow?.materialId"
      :material-label="atpRow?.materialLabel"
      :qty="atpRow?.qty || undefined"
    />
    <PriceReference ref="referenceRef" pickable @pick="onReferencePick" />
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { HistoryOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import AtpModal from './AtpModal.vue'
import PriceHint from './PriceHint.vue'
import PriceReference from './PriceReference.vue'
import WholeDiscountBar from './WholeDiscountBar.vue'
import InvoiceKindSelect from './InvoiceKindSelect.vue'
import TaxRateTitle from './TaxRateTitle.vue'
import { applyTaxRate, useTaxRates } from './taxRate'
import { round2 } from '@/utils/number'
import { addQuotation, getQuotation, updateQuotation } from '@/api/scm/sourcing'
import { defaultPrices } from '@/api/scm/pricing'
import { OrderKind, PriceSource } from '@/constants/enums'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, useLineItems } from '@/composables/useLineItems'
import { today } from '@/components/wms/wms'
import { formatMoney, lineAmount, lineTax } from './scm'

const { t } = useI18n()

/**
 * 新增 / 编辑销售报价（草稿）。选择客户和物料后按开单默认价（客户执行价 → 价目表 → 价格等级价 → 零售价 → 最近售价）
 * 带出单价、税率，手工改过的单价不再自动覆盖；单价下方可打开「价格参考」
 * 通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success(id)
 */
const emit = defineEmits(['success'])

const columns = [
  { title: t('scm.quotation.material'), key: 'material', width: 280 },
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('scm.quotation.taxPrice'), key: 'price', width: 160 },
  { title: t('scm.quotation.taxRate'), key: 'taxRate', width: 130 },
  { title: t('scm.quotation.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('common.field.remark'), key: 'remark', width: 150 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  customerId: requiredRule(t('scm.quotation.selectCustomer')),
  quoteDate: requiredRule(t('scm.quotation.selectQuoteDate'))
}

const emptyForm = () => ({
  id: undefined,
  customerId: undefined,
  customerName: undefined,
  opportunityId: undefined,
  quoteDate: today(),
  validTo: undefined,
  remark: '',
  wholeDiscount: undefined,
  invoiceKind: undefined,
  items: []
})

const num = (v) => (v != null ? Number(v) : undefined)
const toRow = (item) => ({
  ...item,
  key: lineKey(item),
  materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
  qty: num(item.qty),
  price: num(item.price),
  taxRate: item.taxRate != null ? Number(item.taxRate) : undefined,
  listPrice: num(item.listPrice),
  priceAuto: false
})

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const q = await getQuotation(record.id)
    return { ...q, wholeDiscount: Number(q.wholeDiscount) || undefined, invoiceKind: q.invoiceKind || undefined, items: q.items.map(toRow) }
  },
  create: async (data) => emit('success', await addQuotation(data)),
  update: async (id, data) => {
    await updateQuotation(id, data)
    emit('success', id)
  },
  toPayload: (f) => ({
    customerId: f.customerId,
    opportunityId: f.opportunityId || null,
    quoteDate: f.quoteDate,
    validTo: f.validTo || null,
    remark: f.remark || null,
    wholeDiscount: f.wholeDiscount || null,
    invoiceKind: f.invoiceKind || null,
    items: f.items.map((i) => ({
      materialId: i.materialId,
      qty: i.qty,
      price: i.price,
      taxRate: i.taxRate ?? 0,
      remark: i.remark || null
    }))
  })
})
const { form, formRef } = drawer

// 税率：新明细为发票类型的税率，没有时为业务参数「默认税率」；选择发票类型、列头「统一设置税率」批量调整（issue #82）
const tax = useTaxRates()
const lines = useLineItems(() => form.value.items, {
  newRow: () => ({ materialId: undefined, qty: undefined, price: undefined, taxRate: tax.fallback(form.value.invoiceKind), priceAuto: true })
})
function onInvoiceKindChange(kind, rate) {
  if (rate == null) return
  message.success(t('scm.taxRate.invoiceKindApplied', { n: applyTaxRate(form.value.items, rate), rate }))
}
function applyAllTaxRate(rate) {
  message.success(t('scm.taxRate.applied', { n: applyTaxRate(form.value.items, rate), rate }))
}
const totalAmount = lines.total((i) => lineAmount(i.qty, i.price), (n) => n)
const addRow = () => lines.addRow()

const fetchPrice = (row) => fetchPrices([row])

/** 按开单默认价取价（多行一次请求）：来源为价目表时更新价目价；单价为空或是自动带出的，同时更新单价、税率 */
async function fetchPrices(rows) {
  const list = rows.filter((row) => row.materialId)
  if (!list.length || !form.value.customerId) return
  const seqs = list.map((row) => (row.priceSeq = (row.priceSeq || 0) + 1))
  const res = await defaultPrices({
    kind: OrderKind.SALES,
    partnerId: form.value.customerId,
    materialIds: list.map((row) => row.materialId),
    qtys: list.map((row) => row.qty || 1),
    date: form.value.quoteDate || undefined
  }).catch(() => [])
  list.forEach((row, i) => {
    if (seqs[i] !== row.priceSeq) return
    // 默认价按行返回（与请求的物料一一对应）
    const p = res?.[i]?.materialId === row.materialId ? res[i] : undefined
    const found = p && p.price != null
    row.listPrice = found && p.source === PriceSource.PRICE_LIST ? Number(p.price) : undefined
    if (found && (row.priceAuto || row.price == null)) {
      row.price = Number(p.price)
      // 选了发票类型时税率按发票类型，否则按价格来源的税率（没有时保持原税率）
      const rate = tax.sourceRate(form.value.invoiceKind, p.taxRate)
      if (rate != null) row.taxRate = rate
      row.priceAuto = true
      row.priceSource = p.source
    }
  })
}

function refreshPrices() {
  fetchPrices(form.value.items)
}

// 价格参考：点选的历史价格填入单价
const referenceRef = ref()
let referenceRow
function openReference(row) {
  referenceRow = row
  referenceRef.value.open({ materialId: row.materialId, materialName: row.materialLabel, partnerId: form.value.customerId, kind: OrderKind.SALES })
}
function onReferencePick(price) {
  if (!referenceRow) return
  referenceRow.price = price
  referenceRow.priceAuto = false
}

function onCustomerSelect(partner) {
  form.value.customerName = `${partner.code} ${partner.name}`
  refreshPrices()
}

function applyMaterial(row, material) {
  Object.assign(row, {
    materialLabel: material.label,
    materialCode: material.code,
    materialName: material.name,
    unit: material.unit,
    listPrice: undefined,
    priceSource: undefined,
    priceAuto: true
  })
  fetchPrice(row)
}

// 可承诺量
const atpOpen = ref(false)
const atpRow = ref()
function openAtp(row) {
  atpRow.value = row
  atpOpen.value = true
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.amount {
  line-height: 32px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.atp-link {
  font-size: 12px;
}
.opp-tag {
  line-height: 32px;
}
</style>
