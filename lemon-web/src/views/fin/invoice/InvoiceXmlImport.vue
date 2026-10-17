<template>
  <a-modal v-model:open="visible" :title="$t('fin.invoice.xmlTitle', { label: conf.label })" :confirm-loading="xmlImporting" :ok-text="$t('common.action.import')" :ok-button-props="{ disabled: !xmlFiles.length }" @ok="submitXml">
    <a-space direction="vertical" class="full">
      <a-alert type="info" show-icon :message="xmlTip" />
      <a-upload-dragger :file-list="[]" accept=".xml" multiple :before-upload="addXml">
        <p class="ant-upload-text">{{ $t('fin.invoice.xmlDrop') }}</p>
        <p v-if="xmlFiles.length" class="ant-upload-hint">{{ $t('fin.invoice.filesSelected', { n: xmlFiles.length }) }}</p>
      </a-upload-dragger>
      <a-checkbox v-model:checked="autoMatch">{{ conf.autoMatchTip }}</a-checkbox>
      <a-alert v-if="importResult && !importResult.errors.length" type="success" show-icon :message="importResultText" />
      <a-table v-if="importResult?.errors.length" size="small" row-key="rowNum" :columns="errorColumns" :data-source="importResult.errors" :pagination="false" />
    </a-space>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { importInvoiceXml } from '@/api/fin'
import { KINDS } from './invoice'
import { InvoiceKind } from '@/constants/enums'

/** 数电票 XML 导入：通过 ref 调用 open()，导入成功后触发 success */
const props = defineProps({ kind: { type: String, required: true } })
const emit = defineEmits(['success'])
const { t } = useI18n()
const conf = computed(() => KINDS[props.kind])

const autoMatch = ref(true)
const importResult = ref()
const errorColumns = [
  { title: t('common.table.index'), dataIndex: 'rowNum', width: 70 },
  { title: t('fin.invoice.error'), dataIndex: 'message' }
]
const importResultText = computed(() => {
  const r = importResult.value
  if (!r) return ''
  return [
    t('fin.invoice.importedN', { n: r.imported }),
    ...(r.skipped ? [t('fin.invoice.skippedN', { n: r.skipped })] : []),
    ...(r.matched ? [t('fin.invoice.autoMatchedN', { n: r.matched })] : [])
  ].join(t('fin.invoice.sep'))
})
const xmlTip = computed(() =>
  props.kind === InvoiceKind.INPUT
    ? t('fin.invoice.xmlTipInput')
    : t('fin.invoice.xmlTipOutput')
)
const visible = ref(false)
const xmlImporting = ref(false)
const xmlFiles = ref([])
function open() {
  xmlFiles.value = []
  importResult.value = undefined
  visible.value = true
}
function addXml(file) {
  if (!xmlFiles.value.some((f) => f.name === file.name && f.size === file.size)) xmlFiles.value.push(file)
  return false
}
async function submitXml() {
  xmlImporting.value = true
  try {
    importResult.value = await importInvoiceXml(props.kind, xmlFiles.value, autoMatch.value)
    if (!importResult.value.errors.length) {
      message.success(importResultText.value)
      xmlFiles.value = []
      emit('success')
    }
  } finally {
    xmlImporting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
