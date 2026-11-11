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
          <a-descriptions-item v-if="detail.invoiceKind" :label="$t('scm.taxRate.invoiceKind')">
            <DictTag type="scm_invoice_kind" :value="detail.invoiceKind" />
          </a-descriptions-item>
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
              <a-tag :bordered="false" class="freight-tag">{{ bearerLabel }}</a-tag>
              <DictTag v-if="detail.freightSettle" type="scm_freight_settle" :value="detail.freightSettle" />
              <div class="sub-text">
                {{ [detail.carrierSupplierName, accountName, detail.freightCost ? $t(`scm.freight.cost.${detail.freightCost}`) : null].filter(Boolean).join(' · ') }}
              </div>
              <div v-if="detail.freightIoId" class="sub-text">
                <a v-if="userStore.hasPerm('fin:io:list')" @click="openIo">{{ $t('scm.freight.ioDoc') }}</a>
                <span v-else>{{ $t('scm.freight.ioDoc') }}</span>
              </div>
            </div>
          </a-descriptions-item>
          <a-descriptions-item v-if="advanceTotal != null" :label="$t(`scm.freight.advanceTotal.${kind}`)">
            <b>¥{{ formatMoney(advanceTotal) }}</b>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.postTime" :label="$t('scm.trade.postedBy')">{{ detail.postByName || '-' }} {{ detail.postTime }}</a-descriptions-item>
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
              <div v-if="batchDates(record)" class="sub-text">{{ batchDates(record) }}</div>
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
import { useRouter } from 'vue-router'
import { getTrade } from '@/api/scm/trade'
import { batchDates, formatQty, locationText } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, grossAmount, grossPrice } from './scm'
import DictTag from '@/components/DictTag.vue'
import { accountOptions } from '@/api/fin'
import { cached } from '@/utils/cache'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { useUserStore } from '@/store/user'
import { round2 } from '@/utils/number'
import { fcText, isForeign } from '@/components/fin/currency'
import { materialColumns, printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'
import { FreightBearer, OrderSource } from '@/constants/enums'

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

const dict = useDict('scm_freight_settle', 'scm_invoice_kind')
const userStore = useUserStore()
const router = useRouter()
/** 运费承担的名称按单据种类（退货中往来单位一方表示往来单位代垫） */
const bearerLabel = computed(() => {
  const b = detail.value?.freightBearer
  return b ? t(`scm.freight.bearers.${props.kind}.${b}`) : ''
})
/**
 * 往来合计（货款 + 代垫运费，代垫运费单独一张应收应付单）：销售出库代垫运费为客户应收合计，
 * 销售退货客户代垫为应退合计，采购退货代垫运费为应付冲减合计
 */
const advanceTotal = computed(() => {
  const d = detail.value
  if (!d || !Number(d.freight) || props.kind === 'purchaseIn' || d.freightBearer === FreightBearer.COMPANY) return null
  return round2(Number(d.totalAmount) + Number(d.freight))
})
/** 现付的付款账户名称（会计科目，登录即可读取） */
const accounts = ref([])
const accountName = computed(() => {
  const a = accounts.value.find((x) => x.id === detail.value?.freightAccountId)
  return a ? `${a.code} ${a.name}` : ''
})
/** 打开现付运费生成的费用支出单 */
function openIo() {
  emit('update:open', false)
  router.push({ path: '/fin/income-expense', query: { id: detail.value.freightIoId } })
}

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
      ...(d.invoiceKind ? [[t('print.name.invoiceKind'), findDict(dict.scm_invoice_kind, d.invoiceKind)?.label || d.invoiceKind]] : []),
      ...(discount.value ? [[t('print.name.grossTotal'), `¥${formatMoney(grossTotal.value)}`],
        [c.returns ? t('print.name.discountShare') : t('scm.wholeDiscount.label'), `¥${formatMoney(discount.value)}`]] : []),
      [discount.value ? t('print.name.discountedTotal') : t('scm.trade.totalAmount'), `¥${formatMoney(d.totalAmount)}`],
      ...(Number(d.freight) ? [[t('print.name.freight'), `¥${formatMoney(d.freight)}`], [t('print.name.freightBearer'), bearerLabel.value]] : []),
      ...(advanceTotal.value != null ? [[t('print.name.advanceTotal'), `¥${formatMoney(advanceTotal.value)}`]] : []),
      ...(isForeign(d.currency) ? [[t('scm.trade.originalCurrency'), fcText(d.currency, d.amountFc, d.exchangeRate)]] : [])
    ],
    columns: [
      ...materialColumns(),
      ...(lineWarehouse.value ? [{ title: t('scm.trade.warehouse'), key: 'warehouseName' }] : []),
      { title: t('scm.trade.location'), value: (r) => locationText(r.locationCode) },
      { title: t('scm.trade.batch'), key: 'batchNo' },
      { title: t('common.field.quantity'), value: (r) => formatQty(r.qty), align: 'right' },
      { title: t('scm.trade.taxPrice'), value: (r) => formatMoney(grossPrice(r)), align: 'right' },
      { title: t('print.name.taxRate'), value: (r) => `${Number(r.taxRate)}%`, align: 'right' },
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
      if (detail.value.freightAccountId && !accounts.value.length) {
        accounts.value = await cached('accounts-leaf', accountOptions)
      }
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
