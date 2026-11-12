<template>
  <FormDrawer
    page
    :drawer="drawer"
    :create-title="$t('scm.trade.addKind', { kind: conf.label })"
    :edit-title="$t('scm.trade.editDraft', { kind: conf.label, no: form.docNo || '' })"
    width="min(1320px, 100vw)"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="orderConf.partnerLabel" name="partnerId">
            <PartnerSelect
              v-model:value="form.partnerId"
              :label="form.partnerLabel"
              :type="orderConf.partner"
              :disabled="!!form.orderId"
              :placeholder="$t('scm.trade.selectPartner', { partner: orderConf.partnerLabel })"
              @select="(p) => (form.partnerLabel = `${p.code} ${p.name}`)"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="5">
          <a-form-item :label="$t('scm.trade.warehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.trade.bizDate')" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="kind === 'salesOut' && direct" :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.trade.salesman')" name="salesmanId">
            <IdentityUserSelect v-model:value="form.salesmanId" :placeholder="$t('scm.trade.salesmanDefault')" />
          </a-form-item>
        </a-col>
        <!-- 发票类型：直接开单时选择后明细税率统一调整；引用订单时显示订单的发票类型（税率取订单） -->
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.taxRate.invoiceKind')" name="invoiceKind" :tooltip="direct ? $t('scm.taxRate.invoiceKindExtra') : undefined">
            <InvoiceKindSelect v-model:value="form.invoiceKind" :disabled="!direct" @change="onInvoiceKindChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="kind === 'salesOut' && direct ? 24 : 5">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('scm.trade.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item name="orderId" class="source-item" :rules="allowDirect ? [] : [{ required: true, message: sourceRequired }]">
        <div class="source-bar">
          <span class="source-label">{{ sourceByDoc ? $t('scm.trade.sourceDoc', { kind: sourceConf.label }) : $t('scm.trade.refOrder') }}</span>
          <a-select
            :value="sourceByDoc ? form.sourceDocId : form.orderId"
            show-search
            allow-clear
            class="source-select"
            :filter-option="false"
            :options="sourceOptions"
            :loading="sourceLoading"
            :disabled="fixedOrder"
            :placeholder="sourceByDoc ? $t('scm.trade.searchSourceDoc') : $t('scm.trade.searchOrderNo')"
            :not-found-content="sourceLoading ? $t('common.msg.loading') : $t('scm.trade.noSource')"
            @search="searchSources"
            @focus="() => !sources.length && searchSources('')"
            @change="selectSource"
          />
          <span class="sub-text">{{ allowDirect ? (form.orderId ? $t('scm.trade.orderModeHint') : $t('scm.trade.directModeHint')) : $t('scm.trade.orderRequiredHint') }}</span>
        </div>
      </a-form-item>

      <a-form-item v-if="isReturn && form.orderId && !directOrder" name="replenish" class="replenish">
        <a-checkbox v-model:checked="form.replenish">
          {{ conf.orderKind === 'sales' ? $t('scm.trade.replenishSales') : $t('scm.trade.replenishPurchase') }}
          <span class="sub-text">{{ conf.orderKind === 'sales' ? $t('scm.trade.replenishSalesHint') : $t('scm.trade.replenishPurchaseHint') }}</span>
        </a-checkbox>
      </a-form-item>

      <TradeDocItems :kind="kind" :form="form" :direct="direct" :line-warehouse="lineWarehouse" />

      <!-- 运费：采购入库生成运费应付单；销售出库、退货的运费见 TradeFreight（issue #72、#73） -->
      <TradeFreight :kind="kind" :form="form" :purchase-freight-cost="bizRules['scm.purchaseFreight.cost']" />
      <div class="tip sub-text">{{ tip }}</div>
      <div v-if="showDraftActions" class="tip sub-text">{{ $t('scm.trade.draftHint') }}</div>
    </a-form>
    <!-- 先存草稿（业务参数 scm.tradeDoc.draft）或修改草稿：「保存草稿」不出入库，「审核」出入库并生成应收应付 -->
    <template v-if="showDraftActions" #actions="{ saving, loading }">
      <a-button v-perm="`scm:${kind}:add`" :loading="saving && mode === 'draft'" :disabled="loading || saving" @click="submit('draft')">
        {{ $t('scm.trade.saveDraft') }}
      </a-button>
      <a-button v-perm="`scm:${kind}:post`" type="primary" :loading="saving && mode === 'post'" :disabled="loading || saving" @click="submit('post')">
        {{ $t('scm.trade.post') }}
      </a-button>
    </template>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import TradeDocItems from './TradeDocItems.vue'
import TradeFreight from './TradeFreight.vue'
import InvoiceKindSelect from './InvoiceKindSelect.vue'
import { applyTaxRate, useTaxRates } from './taxRate'
import { getOrder, pageOrder } from '@/api/scm/order'
import { addTrade, addTradeDraft, getTrade, getTradeDraft, pageTrade, postTradeDraft, updateTradeDraft } from '@/api/scm/trade'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useBizRules } from '@/composables/useBizRules'
import { useUserStore } from '@/store/user'
import { locationText, rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, freightForm, freightPayload, grossPrice, netQty, newTradeRow, remainingQty } from './scm'
import { OrderSource, OrderStatus, PurchaseFreightCost } from '@/constants/enums'

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，保存即审核（更新库存）。
 * 默认直接开单（业务参数 scm.directDoc.enabled）：选往来单位、仓库，明细直接选商品、数量、单价；
 * 也可引用订单（退货为引用原出入库单），按订单明细录入。参数关闭时必须引用。
 * 业务参数 scm.tradeDoc.draft 开启时底部为「保存草稿」「审核」：草稿不影响库存、订单、应收应付，可再修改（openDraft）、审核。
 * 通过 ref 调用 openCreate(order?)：传入订单时订单不可更换；openDraft(draft) 修改草稿；保存成功后触发 success({ posted })
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const emit = defineEmits(['success'])
const { t } = useI18n()
const userStore = useUserStore()

const conf = computed(() => TRADE_KINDS[props.kind])
const orderConf = computed(() => ORDER_KINDS[conf.value.orderKind])
const tip = computed(() => t(`scm.trade.tip.${props.kind}`))
const isReturn = computed(() => conf.value.returns)

const { rules: bizRules, ready: rulesReady } = useBizRules({
  'scm.directDoc.enabled': true,
  'scm.salesOut.lineWarehouse': true,
  'scm.purchaseFreight.cost': PurchaseFreightCost.INVENTORY,
  'scm.tradeDoc.draft': false
})
const allowDirect = computed(() => bizRules['scm.directDoc.enabled'] === true)
/** 先存草稿：底部按钮为「保存草稿」「审核」 */
const draftMode = computed(() => bizRules['scm.tradeDoc.draft'] === true)
const lineWarehouse = computed(() => props.kind === 'salesOut' && bizRules['scm.salesOut.lineWarehouse'] === true)

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = computed(() => ({
  partnerId: direct.value ? requiredRule(t('scm.trade.selectPartner', { partner: orderConf.value.partnerLabel })) : [],
  warehouseId: requiredRule(t('scm.trade.selectWarehouse')),
  bizDate: requiredRule(t('scm.trade.selectBizDate'))
}))

const emptyForm = () => ({
  id: undefined, docNo: undefined, orderId: undefined, sourceDocId: undefined, partnerId: undefined, partnerLabel: undefined, salesmanId: undefined,
  warehouseId: undefined, bizDate: today(), remark: '', replenish: false, wholeDiscount: undefined, orderDiscount: undefined,
  invoiceKind: undefined, ...freightForm(props.kind), items: []
})

// 税率：直接开单的新明细为发票类型的税率，没有时为业务参数「默认税率」；选择发票类型后明细税率统一调整（issue #82）
const tax = useTaxRates()
const newRow = () => ({ ...newTradeRow(tax.fallback(form.value.invoiceKind)), key: rowKey() })
function onInvoiceKindChange(kind, rate) {
  if (rate == null || !direct.value) return
  message.success(t('scm.taxRate.invoiceKindApplied', { n: applyTaxRate(form.value.items, rate), rate }))
}

/**
 * 保存方式：save 保存即审核（未开启草稿）；draft 保存草稿；post 审核（新单直接保存并审核，草稿先按表单修改再审核）。
 * Ctrl / ⌘ + Enter 在先存草稿或修改草稿时保存草稿
 */
const mode = ref('save')
const posted = () => mode.value !== 'draft'

async function saveNew(data) {
  if (mode.value === 'draft') await addTradeDraft(props.kind, data)
  else await addTrade(props.kind, data)
  emit('success', { posted: posted() })
}

async function saveDraft(id, data) {
  // 只有审核权限（如会计）时按已保存的内容审核，修改草稿需要新增权限
  if (mode.value === 'post') await postTradeDraft(props.kind, id, userStore.hasPerm(`scm:${props.kind}:add`) ? data : undefined)
  else await updateTradeDraft(props.kind, id, data)
  emit('success', { posted: posted() })
}

const drawer = useFormDrawer({
  emptyForm,
  fetch: (record) => loadDraft(record.id),
  create: saveNew,
  update: saveDraft,
  successMessage: () => (posted() ? t('scm.trade.kindSuccess', { kind: TRADE_KINDS[props.kind].label }) : t('scm.trade.draftSaved')),
  toPayload: (f) => {
    const byOrder = !!f.orderId
    return {
      orderId: f.orderId || null,
      partnerId: byOrder ? null : f.partnerId,
      salesmanId: !byOrder && props.kind === 'salesOut' ? f.salesmanId || null : null,
      warehouseId: f.warehouseId,
      bizDate: f.bizDate,
      remark: f.remark,
      replenish: isReturn.value && byOrder ? f.replenish : null,
      sourceDocId: sourceByDoc.value && byOrder ? f.sourceDocId || null : null,
      wholeDiscount: byOrder ? null : f.wholeDiscount || null,
      invoiceKind: byOrder ? null : f.invoiceKind || null,
      // 引用订单的采购入库修改了订单优惠（为空按订单分摊）
      orderDiscount: byOrder && props.kind === 'purchaseIn' && f.orderDiscount != null ? f.orderDiscount : null,
      ...freightPayload(props.kind, f),
      items: f.items.map((i) => ({
        orderItemId: byOrder ? i.orderItemId : null,
        materialId: byOrder ? null : i.materialId,
        price: byOrder ? null : i.price,
        taxRate: byOrder ? null : i.taxRate,
        warehouseId: lineWarehouse.value ? i.warehouseId || null : null,
        locationId: i.locationId || null,
        batchNo: i.batchNo || null,
        productionDate: i.productionDate || null,
        expiryDate: i.expiryDate || null,
        qty: i.qty,
        serialNos: i.serialNos || null,
        unitName: !byOrder ? i.unitName || null : null,
        // 买赠赠品行（直接开单的销售出库）：数量须与促销一致，后端校验
        gift: !byOrder && i.gift ? true : null
      }))
    }
  }
})
const { form, formRef, isEdit } = drawer
const showDraftActions = computed(() => draftMode.value || isEdit.value)

/** 底部按钮：先确定保存方式再提交（校验、保存、提示、关闭由 useFormDrawer 处理） */
async function submit(m) {
  mode.value = m
  try {
    await drawer.submit()
  } finally {
    mode.value = showDraftActions.value ? 'draft' : 'save'
  }
}

/** 直接开单：参数开启且没有引用订单 */
const direct = computed(() => allowDirect.value && !form.value.orderId)

// ---------------------------------------------------------------- 引用订单 / 原单
const fixedOrder = ref(false)
/** 当前引用的订单（判断是否为直接开单生成的订单） */
const currentOrder = ref()
const directOrder = computed(() => currentOrder.value?.source === OrderSource.DIRECT)
/** 退货引用原出入库单（采购退货 ← 采购入库，销售退货 ← 销售出库）；从订单详情进入时引用订单 */
const sourceByDoc = computed(() => isReturn.value && !fixedOrder.value)
const sourceKind = computed(() => (props.kind === 'purchaseReturn' ? 'purchaseIn' : 'salesOut'))
const sourceConf = computed(() => TRADE_KINDS[sourceKind.value])
const sourceRequired = computed(() => (sourceByDoc.value ? t('scm.trade.selectSourceDoc') : t('scm.trade.selectOrder')))

const sources = ref([])
const sourceLoading = ref(false)
const executable = [OrderStatus.APPROVED, OrderStatus.PARTIAL]
const sourceOptions = computed(() =>
  sources.value.map((s) =>
    sourceByDoc.value
      ? { value: s.id, label: `${s.docNo}　${s.partnerName}　${s.bizDate}　¥${formatMoney(s.totalAmount)}` }
      : { value: s.id, label: `${s.orderNo}　${s.partnerName}　¥${formatMoney(s.totalAmount)}` }
  )
)
let timer
function searchSources(keyword) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    sourceLoading.value = true
    try {
      const partnerId = form.value.partnerId || undefined
      if (sourceByDoc.value) {
        const res = await pageTrade(sourceKind.value, { docNo: keyword || undefined, partnerId, current: 1, size: 50 })
        sources.value = res.records
      } else {
        const res = await pageOrder(conf.value.orderKind, { orderNo: keyword || undefined, partnerId, current: 1, size: 100 })
        // 直接开单生成的订单已全部完成，不会出现在可出入库的订单中
        sources.value = res.records.filter((o) => executable.includes(o.status) && o.source !== OrderSource.DIRECT)
      }
    } finally {
      sourceLoading.value = false
    }
  }, 200)
}

/** 订单明细 → 表单行：入库 / 出库默认数量为未完成数量；退货默认不填。doc 为原出入库单时按其明细（批次、库位、序列号）带出 */
/** 订单明细 → 表单行的公共部分 */
const base = (i) => ({
  key: rowKey(), orderItemId: i.id, materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName,
  spec: i.spec, unit: i.unit, batchEnabled: i.batchEnabled, serialEnabled: i.serialEnabled, isVirtual: i.isVirtual, price: Number(i.price), grossPrice: grossPrice(i),
  warehouseId: null, locationId: null, batchNo: undefined, slot: undefined, slotLabel: undefined, available: undefined, serialNos: undefined
})

function orderRows(order, doc) {
  const items = new Map(order.items.map((i) => [i.id, i]))
  if (doc) {
    return doc.items
      .map((d) => ({ d, i: items.get(d.orderItemId) }))
      .filter(({ i }) => i && netQty(i) > 0)
      .map(({ d, i }) => ({ ...base(i), price: Number(d.price), grossPrice: grossPrice(d), limit: netQty(i), qty: undefined, batchNo: d.batchNo || undefined }))
  }
  return order.items
    .map((i) => ({ i, limit: isReturn.value ? netQty(i) : remainingQty(i) }))
    .filter(({ limit }) => limit > 0)
    .map(({ i, limit }) => ({ ...base(i), limit, qty: isReturn.value ? undefined : limit }))
}

function useOrder(order, doc) {
  currentOrder.value = order
  form.value.orderId = order.id
  form.value.partnerId = order.partnerId
  form.value.partnerLabel = `${order.partnerCode} ${order.partnerName}`
  form.value.orderDiscount = undefined
  // 引用订单时沿用订单的发票类型（只显示，税率取订单明细）
  form.value.invoiceKind = order.invoiceKind || undefined
  if (directOrder.value) form.value.replenish = false
  const warehouseId = doc?.warehouseId || order.warehouseId
  if (warehouseId && !form.value.warehouseId) form.value.warehouseId = warehouseId
  form.value.items = orderRows(order, doc)
}

async function selectSource(id) {
  if (!id) return clearSource()
  if (sourceByDoc.value) {
    const doc = await getTrade(sourceKind.value, id)
    const order = await getOrder(conf.value.orderKind, doc.orderId)
    form.value.sourceDocId = id
    useOrder(order, doc)
  } else {
    useOrder(await getOrder(conf.value.orderKind, id))
  }
  if (!form.value.items.length) message.warning(t('scm.trade.noItems', { action: conf.value.action }))
}

/** 取消引用：回到直接开单 */
function clearSource() {
  currentOrder.value = undefined
  Object.assign(form.value, { orderId: undefined, sourceDocId: undefined, replenish: false, wholeDiscount: undefined, orderDiscount: undefined, invoiceKind: undefined })
  form.value.items = allowDirect.value ? [newRow()] : []
}

async function openCreate(order) {
  fixedOrder.value = !!order
  currentOrder.value = undefined
  sources.value = []
  await Promise.all([rulesReady, tax.ready])
  mode.value = draftMode.value ? 'draft' : 'save'
  if (!order) return drawer.openCreate(allowDirect.value ? { items: [{ ...newTradeRow(tax.defaultRate.value), key: rowKey() }] } : {})
  const full = await getOrder(conf.value.orderKind, order.id)
  const items = orderRows(full)
  if (!items.length) {
    message.warning(t('scm.trade.noItems', { action: conf.value.action }))
    return
  }
  currentOrder.value = full
  sources.value = [full]
  return drawer.openCreate({
    orderId: full.id, partnerId: full.partnerId, partnerLabel: `${full.partnerCode} ${full.partnerName}`,
    warehouseId: full.warehouseId || undefined, invoiceKind: full.invoiceKind || undefined, items
  })
}

// ---------------------------------------------------------------- 修改草稿
/** 草稿明细中录入的库位、批次等（出库类单据的库位批次选择框按「库位|批次」回显） */
const draftSlot = (d) => ({
  warehouseId: d.warehouseId || null,
  locationId: d.locationId || null,
  batchNo: d.batchNo || undefined,
  productionDate: d.productionDate || undefined,
  expiryDate: d.expiryDate || undefined,
  serialNos: d.serialNos || undefined,
  remark: d.remark || undefined,
  qty: Number(d.qty),
  ...(conf.value.fromStock
    ? { slot: `${d.locationId || 0}|${d.batchNo || ''}`, slotLabel: `${locationText(d.locationCode)}${d.batchNo ? ' · ' + d.batchNo : ''}` }
    : {})
})

/** 草稿 → 表单：引用订单时按订单的最新明细带出可入库 / 可退数量，直接开单按草稿的物料、单价 */
async function loadDraft(id) {
  const d = await getTradeDraft(props.kind, id)
  currentOrder.value = undefined
  sources.value = []
  // 退货草稿没有原单时是从订单进入的：订单不可更换
  fixedOrder.value = isReturn.value && !!d.orderId && !d.sourceDocId
  const head = {
    id: d.id, docNo: d.docNo, orderId: d.orderId || undefined, sourceDocId: d.sourceDocId || undefined,
    partnerId: d.partnerId, partnerLabel: [d.partnerCode, d.partnerName].filter(Boolean).join(' '), salesmanId: d.salesmanId || undefined,
    warehouseId: d.warehouseId, bizDate: d.bizDate, remark: d.remark || '', replenish: d.replenish === 1,
    wholeDiscount: d.wholeDiscount != null ? Number(d.wholeDiscount) : undefined,
    orderDiscount: d.orderDiscount != null ? Number(d.orderDiscount) : undefined,
    invoiceKind: d.invoiceKind || undefined,
    ...freightForm(props.kind, d)
  }
  if (!d.orderId) {
    const items = d.items.map((i) => ({
      ...newTradeRow(), key: rowKey(), materialId: i.materialId, materialLabel: [i.materialCode, i.materialName].filter(Boolean).join(' '),
      materialCode: i.materialCode, materialName: i.materialName, spec: i.spec, unit: i.unit, batchEnabled: i.batchEnabled,
      serialEnabled: i.serialEnabled, isVirtual: i.isVirtual, price: i.price != null ? Number(i.price) : undefined,
      taxRate: i.taxRate != null ? Number(i.taxRate) : i.gift === 1 ? undefined : tax.fallback(d.invoiceKind),
      priceAuto: false, gift: i.gift === 1, unitName: i.unitName || undefined, ...draftSlot(i)
    }))
    return { ...head, items }
  }
  const order = await getOrder(conf.value.orderKind, d.orderId)
  currentOrder.value = order
  sources.value = sourceByDoc.value && d.sourceDocId ? [await getTrade(sourceKind.value, d.sourceDocId)] : [order]
  const orderItems = new Map(order.items.map((i) => [i.id, i]))
  const items = d.items
    .filter((i) => orderItems.has(i.orderItemId))
    .map((i) => {
      const oi = orderItems.get(i.orderItemId)
      return { ...base(oi), limit: isReturn.value ? netQty(oi) : remainingQty(oi), ...draftSlot(i) }
    })
  return { ...head, items }
}

/** 修改草稿（列表中的「编辑」或点击草稿单号） */
async function openDraft(draft) {
  await Promise.all([rulesReady, tax.ready])
  mode.value = 'draft'
  return drawer.openEdit(draft)
}

defineExpose({ openCreate, openDraft })
</script>

<style scoped>
.tip {
  margin-top: 12px;
}
.full {
  width: 100%;
}
.source-item {
  margin-bottom: 8px;
}
.source-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.source-label {
  font-weight: 500;
}
.source-select {
  width: min(420px, 100%);
}
.replenish {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
