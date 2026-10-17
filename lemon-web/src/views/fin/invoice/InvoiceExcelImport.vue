<template>
  <ImportModal
    :open="open"
    :title="conf.listTitle"
    :columns="excelColumns"
    :importer="importExcel"
    :sample="{ invoiceNo: '25322000000123456789', invoiceDate: '2026-10-08', sellerTaxNo: '91320000MA1XXXXX0X', sellerName: $t('fin.invoice.sampleSeller'), amount: 1130, taxAmount: 130 }"
    @update:open="(v) => emit('update:open', v)"
    @success="emit('success')"
  />
</template>

<script setup>
import { computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import ImportModal from '@/components/ImportModal.vue'
import { importInvoiceRows } from '@/api/fin'
import { KINDS } from './invoice'

/** 税务数字账户导出的发票清单（Excel）导入，用法同 ImportModal（v-model:open、success） */
const props = defineProps({ open: Boolean, kind: { type: String, required: true } })
const emit = defineEmits(['update:open', 'success'])
const { t } = useI18n()
const conf = computed(() => KINDS[props.kind])

const excelColumns = [
  { title: t('fin.invoice.invoiceNo'), key: 'invoiceNo', required: true },
  { title: t('fin.invoice.invoiceType'), key: 'invoiceType', dict: 'fin_invoice_type' },
  { title: t('fin.invoice.invoiceDate'), key: 'invoiceDate', type: 'date', required: true },
  { title: t('fin.invoice.sellerTaxNo'), key: 'sellerTaxNo' },
  { title: t('fin.invoice.sellerName'), key: 'sellerName' },
  { title: t('fin.invoice.buyerTaxNo'), key: 'buyerTaxNo' },
  { title: t('fin.invoice.buyerName'), key: 'buyerName' },
  { title: t('fin.invoice.amount'), key: 'amount', type: 'number', required: true },
  { title: t('fin.invoice.taxAmount'), key: 'taxAmount', type: 'number', required: true }
]
async function importExcel(data) {
  const r = await importInvoiceRows(props.kind, data, true)
  if (!r.errors.length && (r.skipped || r.matched)) {
    message.info([...(r.skipped ? [t('fin.invoice.skippedN', { n: r.skipped })] : []), t('fin.invoice.autoMatchedN', { n: r.matched })].join(t('fin.invoice.sep')))
  }
  return { total: r.total, imported: r.imported, errors: r.errors }
}
</script>
