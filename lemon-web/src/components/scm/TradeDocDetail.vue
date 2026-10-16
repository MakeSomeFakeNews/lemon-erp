<template>
  <SubPage
    :open="open"
    :title="detail ? `${conf.label} ${detail.docNo}` : conf.label"
    width="min(1000px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-button v-if="detail" size="small" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="单据号">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ detail.orderNo }}</a-descriptions-item>
          <a-descriptions-item :label="ORDER_KINDS[conf.orderKind].partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="业务日期">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item label="仓库单据">{{ detail.wmsDocNo }}</a-descriptions-item>
          <a-descriptions-item v-if="conf.returns" label="退货方式">
            {{ detail.replenish === 1 ? (conf.orderKind === 'sales' ? '退货补发' : '退货补收') : '退货退款' }}
          </a-descriptions-item>
          <a-descriptions-item label="价税合计">
            <b>¥{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">（税额 {{ formatMoney(detail.taxAmount) }}）</span>
            <div v-if="isForeign(detail.currency)" class="sub-text">原币 {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
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
              <div class="sub-text">税率 {{ Number(record.taxRate) }}%</div>
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

const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  docId: Number
})
const emit = defineEmits(['update:open'])

const conf = computed(() => TRADE_KINDS[props.kind])
const columns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '库位', key: 'location', width: 100 },
  { title: '批次', key: 'batchNo', width: 160 },
  { title: '数量', key: 'qty', width: 100, align: 'right' },
  { title: '含税单价', key: 'price', width: 110, align: 'right' },
  { title: '价税合计', key: 'amount', width: 120, align: 'right' },
  { title: '序列号', key: 'serialNos', width: 240 }
]

const detail = ref()

function print() {
  const d = detail.value
  const c = conf.value
  const partnerLabel = ORDER_KINDS[c.orderKind].partnerLabel
  printDocs({
    title: c.label + '单',
    no: d.docNo,
    fields: [
      ['订单号', d.orderNo],
      [partnerLabel, d.partnerName],
      ['仓库', d.warehouseName],
      ['业务日期', d.bizDate],
      ['仓库单据', d.wmsDocNo],
      ['价税合计', `¥${formatMoney(d.totalAmount)}`],
      ...(isForeign(d.currency) ? [['原币', fcText(d.currency, d.amountFc, d.exchangeRate)]] : [])
    ],
    columns: [
      ...materialColumns(),
      { title: '库位', value: (r) => locationText(r.locationCode) },
      { title: '批次', key: 'batchNo' },
      { title: '数量', value: (r) => formatQty(r.qty), align: 'right' },
      { title: '含税单价', value: (r) => formatMoney(r.price), align: 'right' },
      { title: '价税合计', key: 'amount', value: (r) => formatMoney(r.amount), align: 'right' },
      { title: '序列号', key: 'serialNos' }
    ],
    rows: d.items,
    totals: { amount: formatMoney(d.totalAmount) },
    note: d.remark,
    signs: [['制单', d.createByName], '仓管', partnerLabel + '签收']
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
