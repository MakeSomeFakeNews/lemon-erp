<template>
  <a-modal :open="open" :title="$t('wms.label.printTitle', { name: type?.name || $t('wms.label.title') })" :ok-text="$t('common.action.print')" :confirm-loading="printing" @ok="print" @cancel="emit('update:open', false)">
    <a-form layout="inline" class="block">
      <a-form-item :label="$t('wms.label.copiesEach')">
        <a-input-number v-model:value="copies" :min="1" :max="100" :addon-after="$t('wms.label.sheets')" />
      </a-form-item>
    </a-form>
    <div class="sub-text block">{{ $t('wms.label.summary', { n: items.length, total: items.length * (copies || 1) }) }}</div>
    <iframe v-if="previewHtml" class="preview" :srcdoc="previewHtml" :title="$t('wms.label.previewTitle')" />
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { LABEL_TYPE_MAP, labelConfig, printLabels, renderLabelsHtml } from '@/utils/labels'
import { loadTemplates } from '@/utils/printTemplates'

/** 打印标签：每个打印的张数，预览第一张。typeKey 为 LABEL_TYPES 的标识，items 为标签数据 */
const props = defineProps({
  open: Boolean,
  typeKey: { type: String, required: true },
  items: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open'])

const type = computed(() => LABEL_TYPE_MAP[props.typeKey])
const copies = ref(1)
const printing = ref(false)
const previewHtml = ref('')
watch(() => props.open, async (v) => {
  if (!v || !props.items.length) return
  copies.value = 1
  const saved = (await loadTemplates().catch(() => ({})))[props.typeKey]
  previewHtml.value = await renderLabelsHtml(props.typeKey, props.items.slice(0, 1), labelConfig(props.typeKey, saved))
})
async function print() {
  printing.value = true
  try {
    await printLabels(props.typeKey, props.items.flatMap((d) => Array.from({ length: copies.value || 1 }, () => d)))
    emit('update:open', false)
  } finally {
    printing.value = false
  }
}
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.preview {
  width: 100%;
  height: 220px;
  border: 1px solid var(--app-border, #eee);
  border-radius: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
