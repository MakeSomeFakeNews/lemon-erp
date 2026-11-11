<template>
  <SubPage
    :open="open"
    :title="detail ? `${conf.label} ${detail.docNo}` : conf.label"
    width="min(1000px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>{{ $t('common.action.print') }}</a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('scm.trade.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.orderNo')">
            {{ detail.orderNo }}
            <a-tag v-if="detail.orderSource === OrderSource.DIRECT" color="blue" :bordered="false" class="direct-tag">{{ $t('scm.trade.direct') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="ORDER_KINDS[conf.orderKind].partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.wmsDocNo')">{{ detail.wmsDocNo }}</a-descriptions-item>
          <a-descriptions-item v-if="conf.returns" :label="$t('scm.trade.returnMode')">
            {{ detail.replenish === 1 ? (conf.orderKind === 'sales' ? $t('scm.trade.returnReship') : $t('scm.trade.returnRereceive')) : $t('scm.trade.returnRefund') }}
          </a-descriptions-item>
          <a-descriptions-item v-if="discount || discountAdjust" :label="$t('print.name.grossTotal')">¥{{ formatMoney(grossTotal) }}</a-descriptions-item>
          <a-descriptions-item v-if="discount || discountAdjust" :label="discountLabel">
            <a-tooltip :title="conf.returns ? $t('scm.wholeDiscount.returnShareHint') : kind === 'purchaseIn' ? $t('scm.wholeDiscount.orderSharePurchaseHint') : $t('scm.wholeDiscount.hint')">
              <span class="dashed">−¥{{ formatMoney(discount) }}</span>
            </a-tooltip>
            <div v-if="discountAdjust" class="sub-text">
              {{ $t('scm.wholeDiscount.orderDiscountAdjusted', { diff: discountAdjust > 0 ? $t('scm.wholeDiscount.more') : $t('scm.wholeDiscount.less'), amount: formatMoney(Math.abs(discountAdjust)) }) }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item :label="discount ? (conf.returns ? $t('scm.wholeDiscount.refundAfter') : $t('scm.wholeDiscount.after')) : $t('scm.trade.totalAmount')">
            <b>¥{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">{{ $t('scm.trade.taxPart', { amount: formatMoney(detail.taxAmount) }) }}</span>
            <div v-if="isForeign(detail.currency)" class="sub-text">{{ $t('scm.trade.originalCurrency') }} {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item v-if="Number(detail.freight)" :label="$t('scm.freight.label')">
            <div>
              <span class="nowrap">¥{{ formatMoney(detail.freight) }}</span>
              <DictTag type="scm_freight_bearer" :value="detail.freightBearer" class="freight-tag" />
              <div class="sub-text">{{ [detail.carrierSupplierName, detail.freightCost ? $t(`scm.freight.cost.${detail.freightCost}`) : null].filter(Boolean).join(' · ') }}</div>
            </div>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}<a-tag v-if="record.gift === 1" color="green" :bordered="false" class="gift-tag">{{ $t('scm.trade.giftTag') }}</a-tag></div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
            <template v-else-if="column.key === 'batchNo'">
              <span>{{ record.batchNo || '-' }}</span>
              <div v-if="record.expiryDate" class="sub-text">{{ record.productionDate || '?' }} ~ {{ record.expiryDate }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'price'">¥{{ formatMoney(grossPrice(record)) }}</template>
            <template v-else-if="column.key === 'amount'">
              <div>¥{{ formatMoney(grossAmount(record)) }}</div>
              <div class="sub-text">{{ $t('scm.trade.taxRateN', { rate: Number(record.taxRate) }) }}</div>
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <SerialTags :value="record.serialNos" />
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import SerialTags from '@/components/wms/SerialTags.vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { computed, ref, watch } from 'vue'
import { getTrade } from '@/api/scm/trade'
import { formatQty, locationText } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, grossAmount, grossPrice } from './scm'
import DictTag from '@/components/DictTag.vue'
import { fcText, isForeign } from '@/components/fin/currency'
import { materialColumns, printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'
import { OrderSource } from '@/constants/enums'

const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  docId: Number
})
const emit = defineEmits(['update:open'])
const { t } = useI18n()

const conf = computed(() => TRADE_KINDS[props.kind])
/** 明细带仓库（销售出库从多个仓库出库）时显示仓库列 */
const lineWarehouse = computed(() => (detail.value?.items || []).some((i) => i.warehouseId))
const columns = computed(() => [
  { title: t('scm.trade.material'), key: 'material', width: 220 },
  ...(lineWarehouse.value ? [{ title: t('scm.trade.warehouse'), dataIndex: 'warehouseName', width: 120 }] : []),
  { title: t('scm.trade.location'), key: 'location', width: 100 },
  { title: t('scm.trade.batch'), key: 'batchNo', width: 160 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('scm.trade.taxPrice'), key: 'price', width: 130, align: 'right' },
  { title: t('scm.trade.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.trade.serialNo'), key: 'serialNos', width: 240 }
])

const detail = ref()
/** 本单分摊的整单优惠（退货为对应的原单优惠分摊）；明细显示优惠前的单价、金额，合计处扣减 */
const discount = computed(() => Number(detail.value?.discountAmount) || 0)
/** 优惠的名称：退货为原单优惠分摊，引用订单的为订单优惠，直接开单为整单优惠 */
const discountLabel = computed(() =>
  conf.value.returns
    ? t('scm.wholeDiscount.returnShare')
    : detail.value?.orderSource === OrderSource.DIRECT ? t('scm.wholeDiscount.label') : t('scm.wholeDiscount.orderShare')
)
/** 采购入库修改了订单优惠时与按订单分摊之差（正数为多优惠） */
const discountAdjust = computed(() => Number(detail.value?.discountAdjust) || 0)
const grossTotal = computed(() => Math.round((Number(detail.value?.totalAmount || 0) + discount.value) * 100) / 100)

function print() {
  const d = detail.value
  const c = conf.value
  const partnerLabel = ORDER_KINDS[c.orderKind].partnerLabel
  printDocs({
    title: t('scm.trade.printTitle', { kind: c.label }),
    no: d.docNo,
    fields: [
      [t('scm.trade.orderNo'), d.orderSource === OrderSource.DIRECT ? `${d.orderNo} (${t('scm.trade.direct')})` : d.orderNo],
      [partnerLabel, d.partnerName],
      [t('scm.trade.warehouse'), d.warehouseName],
      [t('scm.trade.bizDate'), d.bizDate],
      [t('scm.trade.wmsDocNo'), d.wmsDocNo],
      ...(discount.value ? [[t('print.name.grossTotal'), `¥${formatMoney(grossTotal.value)}`],
        [c.returns ? t('print.name.discountShare') : t('scm.wholeDiscount.label'), `¥${formatMoney(discount.value)}`]] : []),
      [discount.value ? t('print.name.discountedTotal') : t('scm.trade.totalAmount'), `¥${formatMoney(d.totalAmount)}`],
      ...(Number(d.freight) ? [[t('print.name.freight'), `¥${formatMoney(d.freight)}`]] : []),
      ...(isForeign(d.currency) ? [[t('scm.trade.originalCurrency'), fcText(d.currency, d.amountFc, d.exchangeRate)]] : [])
    ],
    columns: [
      ...materialColumns(),
      ...(lineWarehouse.value ? [{ title: t('scm.trade.warehouse'), key: 'warehouseName' }] : []),
      { title: t('scm.trade.location'), value: (r) => locationText(r.locationCode) },
      { title: t('scm.trade.batch'), key: 'batchNo' },
      { title: t('common.field.quantity'), value: (r) => formatQty(r.qty), align: 'right' },
      { title: t('scm.trade.taxPrice'), value: (r) => formatMoney(grossPrice(r)), align: 'right' },
      { title: t('scm.trade.totalAmount'), key: 'amount', value: (r) => formatMoney(grossAmount(r)), align: 'right' },
      { title: t('scm.trade.serialNo'), key: 'serialNos' }
    ],
    rows: d.items,
    totals: { amount: formatMoney(grossTotal.value) },
    note: d.remark,
    signs: [[t('scm.trade.prepared'), d.createByName], t('scm.trade.storekeeper'), t('scm.trade.partnerSign', { partner: partnerLabel })]
  }, `${c.label} ${d.docNo}`, `scm.${props.kind}`)
}
const loading = ref(false)

watch(
  () => props.open && props.docId,
  async (v) => {
    detail.value = undefined
    if (!v) return
    loading.value = true
    try {
      detail.value = await getTrade(props.kind, props.docId)
    } finally {
      loading.value = false
    }
  }
)
</script>

<style scoped>
.gift-tag {
  margin-left: 6px;
}
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.direct-tag {
  margin-left: 6px;
}
.freight-tag {
  margin-left: 6px;
}
.nowrap {
  white-space: nowrap;
}
.dashed {
  border-bottom: 1px dashed var(--app-text-3);
  cursor: help;
}
</style>
