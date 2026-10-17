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
          <a-descriptions-item :label="$t('scm.trade.orderNo')">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item :label="ORDER_KINDS[conf.orderKind].partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.wmsDocNo')">{{ detail.wmsDocNo }}</a-descriptions-item>
          <a-descriptions-item v-if="conf.returns" :label="$t('scm.trade.returnMode')">
            {{ detail.replenish === 1 ? (conf.orderKind === 'sales' ? $t('scm.trade.returnReship') : $t('scm.trade.returnRereceive')) : $t('scm.trade.returnRefund') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.totalAmount')">
            <b>¥{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">{{ $t('scm.trade.taxPart', { amount: formatMoney(detail.taxAmount) }) }}</span>
            <div v-if="isForeign(detail.currency)" class="sub-text">{{ $t('scm.trade.originalCurrency') }} {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.trade.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-table row-key="id" size="small" :columns="columns" :data-source="detail.items" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'location'">{{ locationText(record.locationCode) }}</template>
            <template v-else-if="column.key === 'batchNo'">
              <span>{{ record.batchNo || '-' }}</span>
              <div v-if="record.expiryDate" class="sub-text">{{ record.productionDate || '?' }} ~ {{ record.expiryDate }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'price'">¥{{ formatMoney(record.price) }}</template>
            <template v-else-if="column.key === 'amount'">
              <div>¥{{ formatMoney(record.amount) }}</div>
              <div class="sub-text">{{ $t('scm.trade.taxRateN', { rate: Number(record.taxRate) }) }}</div>
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <template v-if="record.serialNos">
                <a-tag v-for="sn in splitSerials(record.serialNos)" :key="sn" class="serial-tag">{{ sn }}</a-tag>
              </template>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { PrinterOutlined } from '@ant-design/icons-vue'
import { computed, ref, watch } from 'vue'
import { getTrade } from '@/api/scm/trade'
import { formatQty, locationText, splitSerials } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney } from './scm'
import { fcText, isForeign } from '@/components/fin/currency'
import { materialColumns, printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  docId: Number
})
const emit = defineEmits(['update:open'])
const { t } = useI18n()

const conf = computed(() => TRADE_KINDS[props.kind])
const columns = [
  { title: t('scm.trade.material'), key: 'material', width: 220 },
  { title: t('scm.trade.location'), key: 'location', width: 100 },
  { title: t('scm.trade.batch'), key: 'batchNo', width: 160 },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('scm.trade.taxPrice'), key: 'price', width: 130, align: 'right' },
  { title: t('scm.trade.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.trade.serialNo'), key: 'serialNos', width: 240 }
]

const detail = ref()

function print() {
  const d = detail.value
  const c = conf.value
  const partnerLabel = ORDER_KINDS[c.orderKind].partnerLabel
  printDocs({
    title: t('scm.trade.printTitle', { kind: c.label }),
    no: d.docNo,
    fields: [
      [t('scm.trade.orderNo'), d.orderNo],
      [partnerLabel, d.partnerName],
      [t('scm.trade.warehouse'), d.warehouseName],
      [t('scm.trade.bizDate'), d.bizDate],
      [t('scm.trade.wmsDocNo'), d.wmsDocNo],
      [t('scm.trade.totalAmount'), `¥${formatMoney(d.totalAmount)}`],
      ...(isForeign(d.currency) ? [[t('scm.trade.originalCurrency'), fcText(d.currency, d.amountFc, d.exchangeRate)]] : [])
    ],
    columns: [
      ...materialColumns(),
      { title: t('scm.trade.location'), value: (r) => locationText(r.locationCode) },
      { title: t('scm.trade.batch'), key: 'batchNo' },
      { title: t('common.field.quantity'), value: (r) => formatQty(r.qty), align: 'right' },
      { title: t('scm.trade.taxPrice'), value: (r) => formatMoney(r.price), align: 'right' },
      { title: t('scm.trade.totalAmount'), key: 'amount', value: (r) => formatMoney(r.amount), align: 'right' },
      { title: t('scm.trade.serialNo'), key: 'serialNos' }
    ],
    rows: d.items,
    totals: { amount: formatMoney(d.totalAmount) },
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
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.serial-tag {
  margin: 2px 4px 2px 0;
}
</style>
