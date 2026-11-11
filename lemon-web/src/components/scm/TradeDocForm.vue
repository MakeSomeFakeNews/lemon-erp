<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.trade.addKind', { kind: conf.label })" width="min(1320px, 100vw)">
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
        <a-col :xs="24" :sm="12" :lg="kind === 'salesOut' && direct ? 5 : 9">
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

      <!-- 采购入库的运费：生成运费应付单；供应商代垫的计入存货成本，本公司承担的按业务参数计入存货成本或当期费用 -->
      <a-row v-if="kind === 'purchaseIn'" :gutter="16" class="freight-row">
        <a-col :xs="24" :sm="8" :lg="5">
          <a-form-item :label="$t('scm.freight.label')" name="freight">
            <a-input-number v-model:value="form.freight" :min="0" :precision="2" prefix="¥" class="full" :placeholder="$t('scm.trade.optional')" />
          </a-form-item>
        </a-col>
        <template v-if="Number(form.freight) > 0">
          <a-col :xs="24" :sm="8" :lg="5">
            <a-form-item :label="$t('scm.freight.payTo')" name="freightBearer">
              <a-select v-model:value="form.freightBearer" :options="purchaseBearers" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.freightBearer === FreightBearer.COMPANY" :xs="24" :sm="8" :lg="6">
            <a-form-item :label="$t('scm.freight.carrier')" name="carrierSupplierId" :rules="requiredRule($t('scm.freight.selectCarrier'))">
              <PartnerSelect v-model:value="form.carrierSupplierId" type="supplier" carrier :placeholder="$t('scm.freight.selectCarrier')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <div class="sub-text freight-hint">{{ freightHint }}</div>
          </a-col>
        </template>
      </a-row>
      <div class="tip sub-text">{{ tip }}</div>
    </a-form>
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
import { getOrder, pageOrder } from '@/api/scm/order'
import { addTrade, getTrade, pageTrade } from '@/api/scm/trade'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useBizRules } from '@/composables/useBizRules'
import { rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, grossPrice, netQty, newTradeRow, remainingQty } from './scm'
import { FreightBearer, OrderSource, OrderStatus, PurchaseFreightCost } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货，保存即确认（更新库存）。
 * 默认直接开单（业务参数 scm.directDoc.enabled）：选往来单位、仓库，明细直接选商品、数量、单价；
 * 也可引用订单（退货为引用原出入库单），按订单明细录入。参数关闭时必须引用。
 * 通过 ref 调用 openCreate(order?)：传入订单时订单不可更换；保存成功后触发 success
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const emit = defineEmits(['success'])
const { t } = useI18n()

const conf = computed(() => TRADE_KINDS[props.kind])
const orderConf = computed(() => ORDER_KINDS[conf.value.orderKind])
const tip = computed(() => t(`scm.trade.tip.${props.kind}`))
const isReturn = computed(() => conf.value.returns)

const { rules: bizRules, ready: rulesReady } = useBizRules({
  'scm.directDoc.enabled': true,
  'scm.salesOut.lineWarehouse': true,
  'scm.purchaseFreight.cost': PurchaseFreightCost.INVENTORY
})
const allowDirect = computed(() => bizRules['scm.directDoc.enabled'] === true)
const lineWarehouse = computed(() => props.kind === 'salesOut' && bizRules['scm.salesOut.lineWarehouse'] === true)

const requiredRule = (msg) => [{ required: true, message: msg }]
/** 采购运费付给：供应商代垫（计入该供应商应付）/ 本公司承担（付承运商） */
const dict = useDict('scm_freight_bearer')
const purchaseBearers = computed(() =>
  toOptions(dict.scm_freight_bearer).filter((d) => d.value === FreightBearer.SUPPLIER || d.value === FreightBearer.COMPANY)
)
/** 运费的实际入账方式：供应商代垫计入存货成本；本公司承担按业务参数「本公司承担的采购运费计入」 */
const freightHint = computed(() => {
  if (form.value.freightBearer !== FreightBearer.COMPANY) return t('scm.freight.supplierHint')
  return bizRules['scm.purchaseFreight.cost'] === PurchaseFreightCost.EXPENSE ? t('scm.freight.expenseHint') : t('scm.freight.inventoryHint')
})
const rules = computed(() => ({
  partnerId: direct.value ? requiredRule(t('scm.trade.selectPartner', { partner: orderConf.value.partnerLabel })) : [],
  warehouseId: requiredRule(t('scm.trade.selectWarehouse')),
  bizDate: requiredRule(t('scm.trade.selectBizDate'))
}))

const emptyForm = () => ({
  id: undefined, orderId: undefined, sourceDocId: undefined, partnerId: undefined, partnerLabel: undefined, salesmanId: undefined,
  warehouseId: undefined, bizDate: today(), remark: '', replenish: false, wholeDiscount: undefined, orderDiscount: undefined,
  freight: undefined, freightBearer: FreightBearer.SUPPLIER, carrierSupplierId: undefined, items: []
})

const drawer = useFormDrawer({
  emptyForm,
  create: async (data) => emit('success', await addTrade(props.kind, data)),
  update: () => Promise.reject(new Error(t('scm.trade.cannotModify'))),
  successMessage: t('scm.trade.kindSuccess', { kind: TRADE_KINDS[props.kind].label }),
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
      wholeDiscount: byOrder ? null : f.wholeDiscount || null,
      // 引用订单的采购入库修改了订单优惠（为空按订单分摊）
      orderDiscount: byOrder && props.kind === 'purchaseIn' && f.orderDiscount != null ? f.orderDiscount : null,
      ...(props.kind === 'purchaseIn' && Number(f.freight) > 0
        ? { freight: f.freight, freightBearer: f.freightBearer, carrierSupplierId: f.freightBearer === FreightBearer.COMPANY ? f.carrierSupplierId : null }
        : {}),
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
        // 买赠赠品行（直接开单的销售出库）：数量须与促销一致，后端校验
        gift: !byOrder && i.gift ? true : null
      }))
    }
  }
})
const { form, formRef } = drawer

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
function orderRows(order, doc) {
  const items = new Map(order.items.map((i) => [i.id, i]))
  const base = (i) => ({
    key: rowKey(), orderItemId: i.id, materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName,
    spec: i.spec, unit: i.unit, batchEnabled: i.batchEnabled, serialEnabled: i.serialEnabled, price: Number(i.price), grossPrice: grossPrice(i),
    warehouseId: null, locationId: null, batchNo: undefined, slot: undefined, slotLabel: undefined, available: undefined, serialNos: undefined
  })
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
  Object.assign(form.value, { orderId: undefined, sourceDocId: undefined, replenish: false, wholeDiscount: undefined, orderDiscount: undefined })
  form.value.items = allowDirect.value ? [{ ...newTradeRow(), key: rowKey() }] : []
}

async function openCreate(order) {
  fixedOrder.value = !!order
  currentOrder.value = undefined
  sources.value = []
  await rulesReady
  if (!order) return drawer.openCreate(allowDirect.value ? { items: [{ ...newTradeRow(), key: rowKey() }] } : {})
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
    warehouseId: full.warehouseId || undefined, items
  })
}

defineExpose({ openCreate })
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
.freight-row {
  margin-top: 12px;
}
.freight-hint {
  margin-top: -16px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
