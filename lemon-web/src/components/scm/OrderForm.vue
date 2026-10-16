<template>
  <FormDrawer page :drawer="drawer" :create-title="`新增${conf.label}`" :edit-title="`编辑${conf.label}`" width="min(1200px, 100vw)">
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
            message="该供应商评估等级为 D（待改进），请谨慎下单或更换供应商"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item label="订单日期" name="orderDate">
            <a-date-picker v-model:value="form.orderDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item label="交货日期" name="deliveryDate">
            <a-date-picker v-model:value="form.deliveryDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="conf.warehouseLabel" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" placeholder="选填，入库 / 出库时默认" />
          </a-form-item>
        </a-col>
        <a-col v-if="kind === 'purchase' && (contracts.length || form.contractId)" :xs="24" :sm="12" :lg="8">
          <a-form-item label="采购合同" name="contractId" extra="引用合同时按合同价下单，校验合同期限和数量、金额上限">
            <a-select
              v-model:value="form.contractId"
              allow-clear
              :options="contractOptions"
              placeholder="不引用合同"
              @change="onContractChange"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item label="币种" name="currency">
            <a-select :value="form.currency" :options="currencyOptions(currencyList)" :disabled="!!form.contractId" @change="onCurrencyChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="12" :sm="6" :lg="4">
          <a-form-item label="汇率" name="exchangeRate" :extra="`1 ${form.currency} = ? 人民币`">
            <a-input-number v-model:value="form.exchangeRate" :min="0" :precision="6" class="full" @change="onRateChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="24" :lg="16">
          <a-alert
            type="info"
            show-icon
            class="fc-tip"
            :message="`单价、金额均为 ${form.currency} 原币；${kind === 'purchase' ? '供货价格' : '价目表价格'}按人民币维护，自动带出时已按汇率折算为 ${form.currency}（保留 4 位小数）`"
          />
        </a-col>
        <a-col v-if="kind === 'sales' && (rebateBalance > 0 || form.rebateAmount)" :xs="24" :sm="12" :lg="8">
          <a-form-item label="返利抵扣" name="rebateAmount" :extra="`可用返利余额 ¥${formatMoney(rebateBalance)}，按行金额分摊到单价`">
            <a-input-number v-model:value="form.rebateAmount" :min="0" :max="rebateBalance" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">
          明细（{{ form.items.length }} 行，数量 {{ formatQty(totalQty) }}，价税合计 {{ symbol }}{{ formatMoney(totalAmount) }}）
          <span v-if="foreign && Number(form.exchangeRate)" class="sub-text">≈ ¥{{ formatMoney(totalAmount * Number(form.exchangeRate)) }}</span>
        </span>
        <a-button type="primary" ghost @click="addRow">
          <template #icon><PlusOutlined /></template>添加物料
        </a-button>
      </div>
      <a-alert v-if="promo && (Number(promo.discountAmount) > 0 || promo.gifts.length || Number(form.rebateAmount) > 0)" type="success" show-icon class="promo-tip">
        <template #message>
          促销优惠 {{ symbol }}{{ formatMoney(promo.discountAmount) }}<span v-if="Number(form.rebateAmount) > 0">，返利抵扣 {{ symbol }}{{ formatMoney(form.rebateAmount) }}</span>，
          成交价合计约 {{ symbol }}{{ formatMoney(promoTotal) }}
          <div v-for="(n, i) in promo.notes" :key="i" class="sub-text">{{ n }}</div>
          <div class="sub-text">保存时按订单日期重新计算，单价按原价录入；赠品自动加入订单</div>
        </template>
      </a-alert>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: 1150 }"
        :locale="{ emptyText: '点击「添加物料」录入明细' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule('请选择物料')" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'unitQty']" :rules="requiredRule('请填写数量')" class="cell-item">
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
              低于最小起订量 {{ formatQty(record.minQty) }} {{ record.unit }}
            </div>
            <a v-if="kind === 'sales' && record.materialId" class="atp-link" @click="openAtp(record)">可承诺量</a>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-form-item :name="['items', index, 'unitPrice']" :rules="requiredRule('请填写单价')" class="cell-item">
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
            <div v-if="contractPrices.has(record.materialId)" class="sub-text">合同价</div>
            <div v-else-if="form.contractId && record.materialId" class="warn-text">不在合同中</div>
            <div v-if="record.unitName" class="sub-text">每{{ record.unitName }}</div>
            <PriceHint
              v-if="kind === 'sales'"
              :price="record.unitPrice"
              :list-price="record.listPrice != null ? fromBase(record.listPrice * ratioOf(record)) : undefined"
              :symbol="symbol"
            />
            <div v-if="promoLine(index)" class="promo-text">
              促销价 {{ symbol }}{{ formatQty(Number(promoLine(index).price) * ratioOf(record)) }}
              <span v-if="promoLine(index).promotionName">（{{ promoLine(index).promotionName }}）</span>
            </div>
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount">{{ symbol }}{{ formatMoney(lineAmount(record.unitQty, record.unitPrice)) }}</div>
            <div class="sub-text">税额 {{ formatMoney(lineTax(lineAmount(record.unitQty, record.unitPrice), record.taxRate)) }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.deliveryDate" value-format="YYYY-MM-DD" placeholder="同订单" class="full" />
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
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import AtpModal from './AtpModal.vue'
import PriceHint from './PriceHint.vue'
import { addOrder, getOrder, updateOrder } from '@/api/scm/order'
import { materialUnits } from '@/api/wms/material'
import { supplierPrices } from '@/api/scm/partner'
import { previewPromotion, quotePrice, rebateBalance as fetchRebateBalance } from '@/api/scm/sourcing'
import { activeContracts } from '@/api/scm/srm'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, formatMoney, lineAmount, lineTax, qty4 } from './scm'
import { BASE_CURRENCY, currencyOptions, currencySymbol, fetchRate, isForeign, useCurrencies } from '@/components/fin/currency'

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

const conf = computed(() => ORDER_KINDS[props.kind])

const columns = [
  { title: '物料', key: 'material', width: 280 },
  { title: '数量', key: 'qty', width: 150 },
  { title: '含税单价', key: 'price', width: 140 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '价税合计', key: 'amount', width: 130, align: 'right' },
  { title: '交货日期', key: 'deliveryDate', width: 140 },
  { title: '备注', key: 'remark', width: 150 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = computed(() => ({
  partnerId: requiredRule(`请选择${conf.value.partnerLabel}`),
  orderDate: requiredRule('请选择订单日期'),
  exchangeRate: foreign.value ? [{ required: true, type: 'number', min: 0.000001, message: '请填写汇率' }] : []
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
  { value: '', label: row.unit || '基本单位' },
  ...row.units.map((u) => ({ value: u.unit, label: `${u.unit}（${formatQty(u.ratio)}${row.unit || ''}）` }))
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
    list.push({ id: order.contractId, contractNo: order.contractNo, name: '（已失效）', items: [] })
  }
  contracts.value = list
}
const contractOptions = computed(() =>
  contracts.value.map((c) => ({ value: c.id, label: `${c.contractNo} ${c.name}${c.endDate ? `（至 ${c.endDate}）` : ''}` }))
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

const drawer = useFormDrawer({
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
  create: async (data) => emit('success', await addOrder(props.kind, data)),
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
  if (form.value.exchangeRate == null) message.warning(`未维护 ${code} 的汇率，请手工填写`)
}

function onRateChange() {
  refillAutoPrices()
}

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + baseQty(i), 0))
const totalAmount = computed(() => form.value.items.reduce((sum, i) => sum + lineAmount(i.unitQty, i.unitPrice), 0))

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
    fetchSalesPrice(row)
    return
  }
  fillPrice(row, true)
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
