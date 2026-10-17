<template>
  <SubPage :open="visible" :title="detail ? $t('fin.invoice.detailTitle', { no: detail.invoiceNo }) : ''" width="min(820px, 100vw)" @close="visible = false">
    <template v-if="detail">
      <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
        <a-descriptions-item :label="$t('fin.invoice.kind')">{{ KINDS[detail.kind]?.label }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.invoiceType')"><DictTag type="fin_invoice_type" :value="detail.invoiceType" /></a-descriptions-item>
        <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.invoiceDate')">{{ detail.invoiceDate }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.taxAmount')">¥{{ formatMoney(detail.taxAmount) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.field.status')">
          <a-tag :color="detail.status === InvoiceStatus.VOID ? 'default' : 'green'" :bordered="false">{{ detail.status === InvoiceStatus.VOID ? $t('fin.invoice.statusVoid') : $t('fin.invoice.statusValid') }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.kind === InvoiceKind.INPUT" :label="$t('fin.invoice.certifyPeriod')">{{ detail.certifyPeriod ? periodText(detail.certifyPeriod) : $t('fin.invoice.uncertified') }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.registeredBy')">{{ detail.createByName || '-' }} {{ detail.createTime || '' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('fin.invoice.source')">{{ SOURCES[detail.source] || SOURCES[InvoiceSource.MANUAL] }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.sellerName || detail.sellerTaxNo" :label="$t('fin.invoice.seller')">{{ detail.sellerName }} {{ detail.sellerTaxNo }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.buyerName || detail.buyerTaxNo" :label="$t('fin.invoice.buyer')">{{ detail.buyerName }} {{ detail.buyerTaxNo }}</a-descriptions-item>
        <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
      </a-descriptions>
      <template v-if="detail.lines?.length">
        <div class="section-title block-title">{{ $t('fin.invoice.lines') }}</div>
        <a-table row-key="id" size="small" :columns="lineColumns" :data-source="detail.lines" :pagination="false" :scroll="{ x: 760 }" class="block">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'qty'">{{ record.qty != null ? Number(record.qty) : '' }} {{ record.unit || '' }}</template>
            <template v-else-if="column.key === 'price'">{{ record.price != null ? Number(record.price) : '' }}</template>
            <template v-else-if="column.key === 'taxRate'">{{ record.taxRate != null ? `${Number(record.taxRate)}%` : '' }}</template>
            <template v-else-if="['amount', 'taxAmount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
      </template>
      <div class="bills-head">
        <span class="section-title">{{ $t('fin.invoice.matchedBills', { amount: formatMoney(detail.matchedAmount) }) }}</span>
        <a-popconfirm
          v-if="detail.status === InvoiceStatus.VALID && detail.items?.length"
          :title="$t('fin.invoice.confirmUnmatch')"
          @confirm="unmatch(detail.id)"
        >
          <a-button v-perm="'fin:invoice:edit'" size="small">{{ $t('fin.invoice.unmatch') }}</a-button>
        </a-popconfirm>
      </div>
      <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="['billAmount', 'amount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
        </template>
      </a-table>
      <div class="section-title block-title attach-title">{{ $t('fin.invoice.attachments') }}</div>
      <AttachmentPanel biz-type="fin_invoice" :biz-id="detail.id" :editable="userStore.hasPerm('fin:invoice:edit')" />
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import { getInvoice, unmatchInvoice } from '@/api/fin'
import { useAction } from '@/composables/useAction'
import { useUserStore } from '@/store/user'
import { formatMoney, periodText } from '@/components/fin/fin'
import { KINDS, SOURCES } from './invoice'
import { InvoiceKind, InvoiceSource, InvoiceStatus } from '@/constants/enums'

/** 发票详情：明细行、勾稽的单据、附件。通过 ref 调用 open(发票)，取消勾稽后触发 changed */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()

const itemColumns = [
  { title: t('fin.invoice.bill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.invoice.sourceNo'), dataIndex: 'sourceNo', width: 170 },
  { title: t('fin.invoice.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.invoice.matchAmount'), key: 'amount', width: 140, align: 'right' }
]
const lineColumns = [
  { title: t('fin.invoice.itemName'), dataIndex: 'itemName', width: 200, ellipsis: true },
  { title: t('fin.invoice.spec'), dataIndex: 'spec', width: 110, ellipsis: true },
  { title: t('common.field.quantity'), key: 'qty', width: 100, align: 'right' },
  { title: t('common.field.price'), key: 'price', width: 100, align: 'right' },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.invoice.taxRate'), key: 'taxRate', width: 80, align: 'right' },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', width: 110, align: 'right' }
]

const visible = ref(false)
const detail = ref()
async function open(record) {
  detail.value = undefined
  visible.value = true
  detail.value = await getInvoice(record.id)
}

const unmatch = useAction(unmatchInvoice, {
  successKey: 'fin.invoice.unmatched2',
  reload: async (res, id) => {
    detail.value = await getInvoice(id)
    emit('changed')
  }
})

defineExpose({ open })
</script>

<style scoped>
.bills-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.block-title {
  margin-bottom: 8px;
}
.block {
  margin-bottom: 16px;
}
.attach-title {
  margin-top: 16px;
}
</style>
