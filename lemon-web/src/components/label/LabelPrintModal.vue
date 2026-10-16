<template>
  <a-modal :open="open" :title="`打印${type?.name || '标签'}`" ok-text="打印" :confirm-loading="printing" @ok="print" @cancel="emit('update:open', false)">
    <a-form layout="inline" class="block">
      <a-form-item label="每个打印">
        <a-input-number v-model:value="copies" :min="1" :max="100" addon-after="张" />
      </a-form-item>
    </a-form>
    <div class="sub-text block">共 {{ items.length }} 个，打印 {{ items.length * (copies || 1) }} 张。标签纸尺寸、条码类型、显示字段在「系统管理 - 打印模板 - 标签」中设置</div>
    <iframe v-if="previewHtml" class="preview" :srcdoc="previewHtml" title="标签预览" />
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
