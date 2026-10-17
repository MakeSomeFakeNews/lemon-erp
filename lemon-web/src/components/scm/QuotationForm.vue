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
        <a-col :span="24">
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

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: $t('scm.quotation.emptyItems') }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule($t('scm.quotation.selectMaterial'))" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
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
            <PriceHint :price="record.price" :list-price="record.listPrice" />
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
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>
    <AtpModal
      v-model:open="atpOpen"
      :material-id="atpRow?.materialId"
      :material-label="atpRow?.materialLabel"
      :qty="atpRow?.qty || undefined"
    />
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { computed, ref } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import AtpModal from './AtpModal.vue'
import PriceHint from './PriceHint.vue'
import { addQuotation, getQuotation, quotePrice, updateQuotation } from '@/api/scm/sourcing'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { rowKey, today } from '@/components/wms/wms'
import { formatMoney, lineAmount, lineTax } from './scm'

const { t } = useI18n()

/**
 * 新增 / 编辑销售报价（草稿）。选择客户和物料后按销售价目表带出单价、税率，手工改过的单价不再自动覆盖
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
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

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
  items: []
})

const num = (v) => (v != null ? Number(v) : undefined)
const toRow = (item) => ({
  ...item,
  key: item.id ? `item-${item.id}` : rowKey(),
  materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
  qty: num(item.qty),
  price: num(item.price),
  taxRate: item.taxRate != null ? Number(item.taxRate) : 13,
  listPrice: num(item.listPrice),
  priceAuto: false
})

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const q = await getQuotation(record.id)
    return { ...q, items: q.items.map(toRow) }
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

const totalAmount = computed(() => form.value.items.reduce((sum, i) => sum + lineAmount(i.qty, i.price), 0))

function addRow() {
  form.value.items.push({ key: rowKey(), materialId: undefined, qty: undefined, price: undefined, taxRate: 13, priceAuto: true })
}

/** 按价目表取价：更新价目价；单价为空或是自动带出的，同时更新单价、税率 */
async function fetchPrice(row) {
  if (!row.materialId || !form.value.customerId) return
  const seq = (row.priceSeq = (row.priceSeq || 0) + 1)
  const res = await quotePrice({
    materialId: row.materialId,
    customerId: form.value.customerId,
    qty: row.qty || undefined,
    date: form.value.quoteDate || undefined
  })
  if (seq !== row.priceSeq) return
  row.listPrice = res ? Number(res.price) : undefined
  if (res && (row.priceAuto || row.price == null)) {
    row.price = Number(res.price)
    if (res.taxRate != null) row.taxRate = Number(res.taxRate)
    row.priceAuto = true
  }
}

function refreshPrices() {
  form.value.items.forEach((row) => fetchPrice(row))
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
