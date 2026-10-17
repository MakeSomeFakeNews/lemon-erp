<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.orderForm.createTitle', { kind: conf.label })" :edit-title="$t('scm.orderForm.editTitle', { kind: conf.label })" width="min(1200px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="conf.partnerLabel" name="partnerId">
            <PartnerSelect
              v-model:value="form.partnerId"
              :type="conf.partner"
              :label="form.partnerName"
              @select="onPartnerSelect"
            />
          </a-form-item>
          <a-alert
            v-if="kind === 'purchase' && partnerLevel === 'D'"
            type="warning"
            show-icon
            class="level-warning"
            :message="$t('scm.orderForm.levelD')"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.order.orderDate')" name="orderDate">
            <a-date-picker v-model:value="form.orderDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.order.deliveryDate')" name="deliveryDate">
            <a-date-picker v-model:value="form.deliveryDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="conf.warehouseLabel" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('scm.orderForm.warehousePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col v-if="kind === 'purchase' && (contracts.length || form.contractId)" :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('scm.order.contract')" name="contractId" :extra="$t('scm.orderForm.contractExtra')">
            <a-select
              v-model:value="form.contractId"
              allow-clear
              :options="contractOptions"
              :placeholder="$t('scm.orderForm.noContract')"
              @change="onContractChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('scm.orderForm.currency')" name="currency">
            <a-select :value="form.currency" :options="currencyOptions(currencyList)" :disabled="!!form.contractId" @change="onCurrencyChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('scm.orderForm.exchangeRate')" name="exchangeRate" :extra="$t('scm.orderForm.rateExtra', { currency: form.currency })">
            <a-input-number v-model:value="form.exchangeRate" :min="0" :precision="6" class="full" @change="onRateChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="24" :lg="16">
          <a-alert
            type="info"
            show-icon
            class="fc-tip"
            :message="$t(kind === 'purchase' ? 'scm.orderForm.foreignTipPurchase' : 'scm.orderForm.foreignTipSales', { currency: form.currency })"
          />
        </a-col>
        <a-col v-if="kind === 'sales' && (rebateBalance > 0 || form.rebateAmount)" :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('scm.orderForm.rebate')" name="rebateAmount" :extra="$t('scm.orderForm.rebateExtra', { amount: formatMoney(rebateBalance) })">
            <a-input-number v-model:value="form.rebateAmount" :min="0" :max="rebateBalance" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('scm.orderForm.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">
          {{ $t('scm.orderForm.itemsTitle', { n: form.items.length, qty: formatQty(totalQty), amount: symbol + formatMoney(totalAmount) }) }}
          <span v-if="foreign && Number(form.exchangeRate)" class="sub-text">≈ ¥{{ formatMoney(totalAmount * Number(form.exchangeRate)) }}</span>
        </span>
        <a-space>
          <a-button @click="matrixOpen = true">
            <template #icon><TableOutlined /></template>{{ $t('scm.orderForm.byVariant') }}
          </a-button>
          <a-button type="primary" ghost @click="addRow">
            <template #icon><PlusOutlined /></template>{{ $t('scm.orderForm.addMaterial') }}
          </a-button>
        </a-space>
      </div>
      <a-alert v-if="promo && (Number(promo.discountAmount) > 0 || promo.gifts.length || Number(form.rebateAmount) > 0)" type="success" show-icon class="promo-tip">
        <template #message>
          {{ $t('scm.orderForm.promoDiscount', { amount: symbol + formatMoney(promo.discountAmount) }) }}<span v-if="Number(form.rebateAmount) > 0">{{ $t('scm.orderForm.promoRebate', { amount: symbol + formatMoney(form.rebateAmount) }) }}</span>{{ $t('scm.order.comma') }}
          {{ $t('scm.orderForm.promoTotal', { amount: symbol + formatMoney(promoTotal) }) }}
          <div v-for="(n, i) in promo.notes" :key="i" class="sub-text">{{ n }}</div>
          <div class="sub-text">{{ $t('scm.orderForm.promoNote') }}</div>
        </template>
      </a-alert>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: $t('scm.orderForm.emptyItems') }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule($t('scm.orderForm.selectMaterial'))" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'unitQty']" :rules="requiredRule($t('scm.orderForm.inputQty'))" class="cell-item">
              <a-input-number
                v-model:value="record.unitQty"
                :min="0"
                :precision="record.serialEnabled === 1 && !record.unitName ? 0 : 4"
                :addon-after="record.units?.length ? undefined : record.unit || undefined"
                class="full"
                @change="kind === 'sales' && fetchSalesPrice(record)"
              >
                <template v-if="record.units?.length" #addonAfter>
                  <a-select
                    :value="record.unitName || ''"
                    :options="unitOptions(record)"
                    :dropdown-match-select-width="false"
                    class="unit-select"
                    @change="(v) => changeUnit(record, v)"
                  />
                </template>
              </a-input-number>
            </a-form-item>
            <div v-if="record.unitName && record.unitQty != null" class="sub-text">= {{ formatQty(baseQty(record)) }} {{ record.unit }}</div>
            <div v-if="record.minQty && record.unitQty && baseQty(record) < record.minQty" class="warn-text">
              {{ $t('scm.orderForm.belowMoq', { qty: formatQty(record.minQty), unit: record.unit || '' }) }}
            </div>
            <a v-if="kind === 'sales' && record.materialId" class="atp-link" @click="openAtp(record)">{{ $t('scm.orderForm.atp') }}</a>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-form-item :name="['items', index, 'unitPrice']" :rules="requiredRule($t('scm.orderForm.inputPrice'))" class="cell-item">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="4"
                :prefix="symbol"
                :disabled="contractPrices.has(record.materialId)"
                class="full"
                @change="record.priceAuto = false"
              />
            </a-form-item>
            <div v-if="contractPrices.has(record.materialId)" class="sub-text">{{ $t('scm.orderForm.contractPrice') }}</div>
            <div v-if="record.lastPrice" class="sub-text">
              {{ $t('scm.orderForm.lastPrice') }} <a @click="useLastPrice(record)">{{ currencySymbol(record.lastPrice.currency) }}{{ formatQty(Number(record.lastPrice.price) * ratioOf(record)) }}</a>
              {{ $t('scm.orderForm.paren', { text: record.lastPrice.orderDate }) }}
            </div>
            <div v-if="priceWarning(record)" class="warn-text">{{ priceWarning(record) }}</div>
            <div v-else-if="form.contractId && record.materialId" class="warn-text">{{ $t('scm.orderForm.notInContract') }}</div>
            <div v-if="record.unitName" class="sub-text">{{ $t('scm.orderForm.perUnit', { unit: record.unitName }) }}</div>
            <PriceHint
              v-if="kind === 'sales'"
              :price="record.unitPrice"
              :list-price="record.listPrice != null ? fromBase(record.listPrice * ratioOf(record)) : undefined"
              :symbol="symbol"
            />
            <div v-if="promoLine(index)" class="promo-text">
              {{ $t('scm.orderForm.promoPrice', { price: symbol + formatQty(Number(promoLine(index).price) * ratioOf(record)) }) }}
              <span v-if="promoLine(index).promotionName">{{ $t('scm.orderForm.paren', { text: promoLine(index).promotionName }) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount">{{ symbol }}{{ formatMoney(lineAmount(record.unitQty, record.unitPrice)) }}</div>
            <div class="sub-text">{{ $t('scm.orderForm.taxValue', { amount: formatMoney(lineTax(lineAmount(record.unitQty, record.unitPrice), record.taxRate)) }) }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.deliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.orderForm.sameAsOrder')" class="full" />
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
      <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.orderForm.extInfo')" :span="8" />
    </a-form>
    <VariantMatrixModal v-model:open="matrixOpen" @confirm="addVariantRows" />
    <AtpModal
      v-if="kind === 'sales'"
      v-model:open="atpOpen"
      :material-id="atpRow?.materialId"
      :material-label="atpRow?.materialLabel"
      :warehouse-id="form.warehouseId || undefined"
      :qty="atpRow ? baseQty(atpRow) || undefined : undefined"
    />
  </FormDrawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, PlusOutlined, TableOutlined } from '@ant-design/icons-vue'
import VariantMatrixModal from '@/components/wms/VariantMatrixModal.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import AtpModal from './AtpModal.vue'
import PriceHint from './PriceHint.vue'
import { addOrder, getOrder, updateOrder } from '@/api/scm/order'
import { materialUnits } from '@/api/wms/material'
import { supplierPrices } from '@/api/scm/partner'
import { previewPromotion, priceControlsOf, priceHistory, quotePrice, rebateBalance as fetchRebateBalance } from '@/api/scm/sourcing'
import { activeContracts } from '@/api/scm/srm'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, formatMoney, lineAmount, lineTax, qty4 } from './scm'
import { BASE_CURRENCY, currencyOptions, currencySymbol, fetchRate, isForeign, useCurrencies } from '@/components/fin/currency'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import { useCustomFields } from '@/composables/useCustomFields'

/**
 * 新增 / 编辑采购、销售订单（草稿）。采购订单选择供应商后，按供货价格自动带出单价、税率；
 * 销售订单选择客户和物料后按销售价目表带出单价、税率，手工改过的单价不再自动覆盖。
 * 物料有辅助单位时可按辅助单位录入：表单中 unitQty / unitPrice 为所选单位的数量、单价，
 * 提交时折算为基本单位的 qty / price（qty = unitQty × 换算率，price = unitPrice ÷ 换算率），并带上 unitName
 * 外币订单：单价、金额为订单币种的原币，按人民币维护的供货价格 / 价目价自动带出时除以汇率
 * 通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success(id)
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const emit = defineEmits(['success'])

const { t } = useI18n()
const conf = computed(() => ORDER_KINDS[props.kind])

const columns = [
  { title: t('scm.order.material'), key: 'material', width: 280 },
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('scm.order.taxPrice'), key: 'price', width: 150 },
  { title: t('scm.order.taxRate'), key: 'taxRate', width: 130 },
  { title: t('scm.order.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.order.deliveryDate'), key: 'deliveryDate', width: 140 },
  { title: t('common.field.remark'), key: 'remark', width: 150 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = computed(() => ({
  partnerId: requiredRule(t('common.rule.requiredSelect', { name: conf.value.partnerLabel })),
  orderDate: requiredRule(t('scm.orderForm.selectOrderDate')),
  exchangeRate: foreign.value ? [{ required: true, type: 'number', min: 0.000001, message: t('scm.orderForm.inputRate') }] : []
}))

const emptyForm = () => ({
  id: undefined,
  partnerId: undefined,
  partnerName: undefined,
  orderDate: today(),
  deliveryDate: undefined,
  warehouseId: undefined,
  currency: BASE_CURRENCY,
  exchangeRate: 1,
  contractId: undefined,
  rebateAmount: undefined,
  remark: '',
  items: []
})

const toRow = (item) => {
  const unitRatio = item.unitName && Number(item.unitRatio) ? Number(item.unitRatio) : 1
  return {
    ...item,
    key: item.id ? `item-${item.id}` : rowKey(),
    materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
    unitName: item.unitName || null,
    unitRatio,
    unitQty: item.qty != null ? qty4(Number(item.qty) / unitRatio) : undefined,
    unitPrice: item.price != null ? qty4(Number(item.price) * unitRatio) : undefined,
    taxRate: item.taxRate != null ? Number(item.taxRate) : 13,
    units: []
  }
}

// ---------------------------------------------------------------- 辅助单位
/** 物料的辅助单位：materialId → Promise<[{ unit, ratio }]>，表单打开期间缓存 */
const unitCache = new Map()
function loadUnits(materialId) {
  if (!unitCache.has(materialId)) {
    unitCache.set(
      materialId,
      materialUnits(materialId).catch((e) => {
        unitCache.delete(materialId)
        throw e
      })
    )
  }
  return unitCache.get(materialId)
}

/** 加载行的辅助单位；当前单位不在列表中（已删除）时保留它，避免编辑旧订单时丢失 */
async function attachUnits(row) {
  const materialId = row.materialId
  if (!materialId) return
  const list = await loadUnits(materialId).catch(() => [])
  if (row.materialId !== materialId) return
  const units = list.map((u) => ({ unit: u.unit, ratio: Number(u.ratio) }))
  if (row.unitName && !units.some((u) => u.unit === row.unitName)) units.push({ unit: row.unitName, ratio: row.unitRatio })
  row.units = units
}

const ratioOf = (row) => (row.unitName ? Number(row.unitRatio) || 1 : 1)
/** 基本单位数量 */
const baseQty = (row) => (row.unitQty == null ? 0 : qty4(Number(row.unitQty) * ratioOf(row)))
/** 基本单位单价 */
const basePrice = (row) => (row.unitPrice == null ? null : qty4(Number(row.unitPrice) / ratioOf(row)))

const unitOptions = (row) => [
  { value: '', label: row.unit || t('scm.orderForm.baseUnit') },
  ...row.units.map((u) => ({ value: u.unit, label: t('scm.orderForm.unitOption', { unit: u.unit, ratio: `${formatQty(u.ratio)}${row.unit || ''}` }) }))
]

/** 换单位：数量保持输入的数字，单价按基本单位价折算到新单位 */
function changeUnit(row, unitName) {
  const price = basePrice(row)
  const unit = row.units.find((u) => u.unit === unitName)
  row.unitName = unit ? unit.unit : null
  row.unitRatio = unit ? unit.ratio : 1
  if (price != null) row.unitPrice = qty4(price * row.unitRatio)
  if (props.kind === 'sales') fetchSalesPrice(row)
}

// 采购合同：供应商在订单日期有效的生效合同
const contracts = ref([])
async function loadContracts(supplierId, date, order) {
  contracts.value = []
  if (props.kind !== 'purchase' || !supplierId) return
  const list = await activeContracts({ supplierId, date: date || undefined })
  // 编辑时引用的合同可能已关闭或到期，仍显示
  if (order?.contractId && !list.some((c) => c.id === order.contractId)) {
    list.push({ id: order.contractId, contractNo: order.contractNo, name: t('scm.orderForm.expired'), items: [] })
  }
  contracts.value = list
}
const contractOptions = computed(() =>
  contracts.value.map((c) => ({ value: c.id, label: `${c.contractNo} ${c.name}${c.endDate ? t('scm.orderForm.until', { date: c.endDate }) : ''}` }))
)
const contractPrices = computed(() => {
  const c = contracts.value.find((x) => x.id === form.value.contractId)
  return new Map((c?.items || []).map((i) => [i.materialId, i]))
})
function onContractChange() {
  if (form.value.contractId) form.value.currency = BASE_CURRENCY
  form.value.items.forEach((row) => fillPrice(row, true))
}

// 供应商的供货价格：materialId → price
const prices = ref(new Map())
async function loadPrices(supplierId) {
  prices.value = new Map()
  if (props.kind !== 'purchase' || !supplierId) return
  const list = await supplierPrices(supplierId)
  prices.value = new Map(list.map((p) => [p.materialId, p]))
}

// 自定义字段（采购订单 scm_purchase / 销售订单 scm_sales）
const cf = useCustomFields(props.kind === 'sales' ? 'scm_sales' : 'scm_purchase')
const cfFields = cf.fields
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm,
    fetch: async (record) => {
      const order = await getOrder(props.kind, record.id)
      await Promise.all([loadPrices(order.partnerId), loadContracts(order.partnerId, order.orderDate, order), loadRebate(order.partnerId, order.id)])
      // 销售订单：赠品行由促销自动生成，单价按原价编辑（保存时重新应用促销）
      const items = order.items.filter((i) => !i.gift).map((i) => toRow(i.origPrice != null ? { ...i, price: i.origPrice } : i))
      await Promise.all(items.map(attachUnits))
      return {
        ...order,
        currency: order.currency || BASE_CURRENCY,
        exchangeRate: order.exchangeRate != null ? Number(order.exchangeRate) : 1,
        rebateAmount: Number(order.rebateAmount) || undefined,
        items
      }
    },
    beforeOpen: (record) => {
      unitCache.clear()
      if (!record) {
        prices.value = new Map()
        contracts.value = []
        rebateBalance.value = 0
      }
      promo.value = undefined
    },
    create: async (data) => {
      const id = await addOrder(props.kind, data)
      emit('success', id)
      return id
    },
    update: async (id, data) => {
      await updateOrder(props.kind, id, data)
      emit('success', id)
    },
    toPayload: (f) => ({
      partnerId: f.partnerId,
      orderDate: f.orderDate,
      deliveryDate: f.deliveryDate || null,
      warehouseId: f.warehouseId || null,
      currency: f.currency || BASE_CURRENCY,
      exchangeRate: isForeign(f.currency) ? f.exchangeRate : 1,
      contractId: f.contractId || null,
      rebateAmount: props.kind === 'sales' && f.rebateAmount ? f.rebateAmount : null,
      remark: f.remark,
      items: f.items.map((i) => ({
        materialId: i.materialId,
        qty: baseQty(i),
        price: basePrice(i),
        unitName: i.unitName || null,
        taxRate: i.taxRate ?? 0,
        deliveryDate: i.deliveryDate || null,
        remark: i.remark || null
      }))
    })
  })
)
const { form, formRef } = drawer

// ---------------------------------------------------------------- 币种、汇率
const currencyList = useCurrencies()
const foreign = computed(() => isForeign(form.value.currency))
const symbol = computed(() => currencySymbol(form.value.currency))

/** 人民币价格折算为订单币种（外币时除以汇率，保留 4 位小数；未填汇率时不折算） */
const fromBase = (price) => {
  const rate = Number(form.value.exchangeRate)
  return foreign.value && rate > 0 ? qty4(Number(price) / rate) : Number(price)
}

/** 自动带出的单价按新的币种 / 汇率重新折算 */
function refillAutoPrices() {
  form.value.items.forEach((row) => {
    if (props.kind === 'sales') fetchSalesPrice(row)
    else if (row.priceAuto) fillPrice(row, true)
  })
}

let rateSeq = 0
/** 按订单日期带出记账汇率（可修改） */
async function refreshRate() {
  if (!foreign.value) return
  const seq = ++rateSeq
  const rate = await fetchRate(form.value.currency, form.value.orderDate)
  if (seq !== rateSeq || rate == null) return
  form.value.exchangeRate = rate
  refillAutoPrices()
}

async function onCurrencyChange(code) {
  form.value.currency = code
  if (!isForeign(code)) {
    form.value.exchangeRate = 1
    refillAutoPrices()
    return
  }
  form.value.exchangeRate = undefined
  await refreshRate()
  if (form.value.exchangeRate == null) message.warning(t('scm.orderForm.noRate', { code }))
}

function onRateChange() {
  refillAutoPrices()
}

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + baseQty(i), 0))
const totalAmount = computed(() => form.value.items.reduce((sum, i) => sum + lineAmount(i.unitQty, i.unitPrice), 0))

// 按规格录入：款式的规格矩阵中填写的各 SKU 数量逐行加入明细（单价按物料自动带出）
const matrixOpen = ref(false)
function addVariantRows(lines) {
  lines.forEach(({ material, qty }) => {
    addRow()
    const row = form.value.items[form.value.items.length - 1]
    row.materialId = material.id
    applyMaterial(row, material)
    row.unitQty = qty
  })
}

function addRow() {
  form.value.items.push({
    key: rowKey(),
    materialId: undefined,
    unitName: null,
    unitRatio: 1,
    unitQty: undefined,
    unitPrice: undefined,
    units: [],
    taxRate: 13,
    priceAuto: true
  })
}

const partnerLevel = ref()
async function onPartnerSelect(partner) {
  partnerLevel.value = partner.level
  form.value.partnerName = `${partner.code} ${partner.name}`
  if (props.kind === 'sales') {
    // 换了客户：按客户等级重新取价，更新自动带出的单价；返利余额按客户
    form.value.items.forEach((row) => fetchSalesPrice(row))
    form.value.rebateAmount = undefined
    loadRebate(partner.id, form.value.id)
    return
  }
  form.value.contractId = undefined
  await Promise.all([loadPrices(partner.id), loadContracts(partner.id, form.value.orderDate)])
  // 换了供应商：按新供应商的价格更新已录入的明细
  form.value.items.forEach((row) => fillPrice(row, true))
}

/** 销售订单按价目表取价：更新价目价；单价为空或是自动带出的，同时更新单价、税率 */
async function fetchSalesPrice(row) {
  if (props.kind !== 'sales' || !row.materialId || !form.value.partnerId) return
  const seq = (row.priceSeq = (row.priceSeq || 0) + 1)
  const res = await quotePrice({
    materialId: row.materialId,
    customerId: form.value.partnerId,
    qty: baseQty(row) || undefined,
    date: form.value.orderDate || undefined
  })
  if (seq !== row.priceSeq) return
  // 价目价按基本单位
  row.listPrice = res ? Number(res.price) : undefined
  if (res && (row.priceAuto || row.unitPrice == null)) {
    row.unitPrice = qty4(fromBase(Number(res.price) * ratioOf(row)))
    if (res.taxRate != null) row.taxRate = Number(res.taxRate)
    row.priceAuto = true
  }
}

function fillPrice(row, overwrite) {
  // 引用合同时按合同价
  const cp = contractPrices.value.get(row.materialId)
  if (cp) {
    row.unitPrice = qty4(Number(cp.price) * ratioOf(row))
    row.taxRate = Number(cp.taxRate)
    row.priceAuto = true
    return
  }
  const p = prices.value.get(row.materialId)
  if (!p) return
  // 供货价格按基本单位
  if (overwrite || row.unitPrice == null) {
    row.unitPrice = qty4(fromBase(Number(p.price) * ratioOf(row)))
    row.priceAuto = true
  }
  if (overwrite || row.taxRate == null) row.taxRate = Number(p.taxRate)
  row.minQty = p.minQty ? Number(p.minQty) : undefined
}

// ---------------------------------------------------------------- 历史成交价、价格控制
/** 往来单位该物料最近一次成交价（基本单位、订单币种）；没有价目 / 供货价时默认带出 */
async function loadLastPrice(row) {
  row.lastPrice = undefined
  if (!row.materialId || !form.value.partnerId) return
  const materialId = row.materialId
  const list = await priceHistory({ kind: props.kind, partnerId: form.value.partnerId, materialId, limit: 1 }).catch(() => [])
  if (row.materialId !== materialId || !list.length) return
  row.lastPrice = list[0]
  const sameCurrency = (list[0].currency || BASE_CURRENCY) === (form.value.currency || BASE_CURRENCY)
  if (row.unitPrice == null && sameCurrency) {
    row.unitPrice = qty4(Number(list[0].price) * ratioOf(row))
    if (list[0].taxRate != null) row.taxRate = Number(list[0].taxRate)
  }
}
function useLastPrice(row) {
  row.unitPrice = qty4(Number(row.lastPrice.price) * ratioOf(row))
  row.priceAuto = false
}

const priceControls = ref(new Map())
let controlTimer
watch(
  () => form.value.items?.map((i) => i.materialId).filter(Boolean).join(','),
  (ids) => {
    clearTimeout(controlTimer)
    if (!ids) return
    controlTimer = setTimeout(async () => {
      const list = await priceControlsOf(ids.split(',').map(Number)).catch(() => [])
      priceControls.value = new Map(list.map((c) => [c.materialId, c]))
    }, 300)
  }
)
/** 单价（含税、基本单位、折算本位币）超出价格控制时的提示 */
function priceWarning(row) {
  const c = priceControls.value.get(row.materialId)
  if (!c || row.unitPrice == null) return ''
  const rate = foreign.value ? Number(form.value.exchangeRate) || 1 : 1
  const price = (Number(row.unitPrice) / ratioOf(row)) * rate
  const block = c.controlMode === 'BLOCK' ? t('scm.orderForm.cannotSave') : ''
  if (props.kind === 'sales' && c.minSalePrice != null && price < Number(c.minSalePrice) - 0.00005) {
    return t('scm.orderForm.belowMinSale', { price: Number(c.minSalePrice) }) + block
  }
  if (props.kind === 'purchase' && c.maxPurchasePrice != null && price > Number(c.maxPurchasePrice) + 0.00005) {
    return t('scm.orderForm.aboveMaxPurchase', { price: Number(c.maxPurchasePrice) }) + block
  }
  return ''
}

function applyMaterial(row, material) {
  Object.assign(row, {
    materialLabel: material.label,
    materialCode: material.code,
    materialName: material.name,
    unit: material.unit,
    serialEnabled: material.serialEnabled,
    // 扫辅助单位条码时直接选该单位
    unitName: material.scanUnit || null,
    unitRatio: material.scanUnit ? Number(material.scanRatio) || 1 : 1,
    units: material.scanUnit ? [{ unit: material.scanUnit, ratio: Number(material.scanRatio) || 1 }] : []
  })
  attachUnits(row)
  if (props.kind === 'sales') {
    row.listPrice = undefined
    row.priceAuto = true
    fetchSalesPrice(row).then(() => loadLastPrice(row))
    return
  }
  fillPrice(row, true)
  loadLastPrice(row)
}

// ---------------------------------------------------------------- 促销预览、返利抵扣
const rebateBalance = ref(0)
async function loadRebate(customerId, orderId) {
  if (props.kind !== 'sales' || !customerId) return
  rebateBalance.value = Number(await fetchRebateBalance(customerId, orderId).catch(() => 0)) || 0
}
const promo = ref()
let promoTimer
let promoSeq = 0
/** 客户、日期、明细、返利抵扣变化后（防抖）预览促销 */
watch(
  () => props.kind === 'sales' && drawer.open.value
    ? JSON.stringify([form.value.partnerId, form.value.orderDate, form.value.rebateAmount,
      form.value.items.map((i) => [i.materialId, baseQty(i), basePrice(i)])])
    : null,
  (key) => {
    clearTimeout(promoTimer)
    if (!key) return
    promoTimer = setTimeout(async () => {
      const f = form.value
      const lines = f.items.filter((i) => i.materialId && baseQty(i) > 0 && basePrice(i) != null)
      if (!f.partnerId || !f.orderDate || !lines.length || lines.length !== f.items.length) {
        promo.value = undefined
        return
      }
      const seq = ++promoSeq
      const res = await previewPromotion({
        customerId: f.partnerId, orderDate: f.orderDate, rebateAmount: f.rebateAmount || null,
        items: lines.map((i) => ({ materialId: i.materialId, qty: baseQty(i), price: basePrice(i) }))
      }).catch(() => undefined)
      if (seq === promoSeq) promo.value = res
    }, 500)
  }
)
/** 预览中第 index 行的促销价（有优惠时） */
const promoLine = (index) => {
  const line = promo.value?.lines?.[index]
  return line && line.origPrice != null ? line : null
}
const promoTotal = computed(() => (promo.value?.lines || []).reduce((sum, l, i) => sum + Number(l.price) * baseQty(form.value.items[i] || {}), 0))

// 可承诺量
const atpOpen = ref(false)
const atpRow = ref()
function openAtp(row) {
  atpRow.value = row
  atpOpen.value = true
}

/** 传入的明细（基本单位）转为表单行 */
async function openCreate(defaults = {}) {
  if (!defaults.items?.length) return drawer.openCreate(defaults)
  await drawer.openCreate({ ...defaults, items: defaults.items.map(toRow) })
  form.value.items.forEach(attachUnits)
}

defineExpose({ openCreate, openEdit: drawer.openEdit })
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
.promo-tip {
  margin-bottom: 12px;
}
.promo-text {
  color: var(--ant-color-success, #52c41a);
  font-size: 12px;
}
.amount {
  line-height: 32px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn-text {
  color: #faad14;
  font-size: 12px;
}
.unit-select {
  min-width: 64px;
}
.atp-link {
  font-size: 12px;
}
.fc-tip {
  margin-bottom: 16px;
}
.level-warning {
  margin-top: -8px;
  margin-bottom: 12px;
}
</style>
