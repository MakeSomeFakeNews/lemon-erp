<template>
  <div class="preview">
    <div class="preview-head">
      <span>{{ $t('print.template.preview') }}</span>
      <a-spin v-if="previewing" size="small" />
      <span class="spacer" />
      <a-button type="link" size="small" :disabled="!previewUrl" @click="zoom"><template #icon><ExpandOutlined /></template>{{ $t('print.template.zoom') }}</a-button>
    </div>
    <iframe v-if="previewUrl" class="preview-frame" :src="`${previewUrl}#toolbar=0&view=FitH`" :title="$t('print.preview.title')" />
    <div v-else class="preview-empty">{{ previewError || $t('print.template.generating') }}</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ExpandOutlined } from '@ant-design/icons-vue'
import { printPdf } from '@/api/system/print'
import { showPdf } from '@/utils/print'

/**
 * 打印模板的预览：用示例单据和当前（未保存的）配置请求后端生成 PDF。
 * 显示时立即生成，watchKey 变化后稍等再生成（避免输入时频繁请求），也可调用 render() 立即生成
 */
const props = defineProps({
  /** () => 打印请求，返回空时不生成 */
  buildRequest: { type: Function, required: true },
  /** 变化时重新生成（如配置的 JSON） */
  watchKey: { type: [String, Array, Object], default: '' },
  /** 放大预览窗口的标题 */
  zoomTitle: { type: String, default: '' }
})
const { t } = useI18n()

const previewUrl = ref('')
const previewing = ref(false)
const previewError = ref('')
let previewBlob = null
let previewTimer = null
let previewSeq = 0

async function render() {
  const req = props.buildRequest()
  if (!req) return
  const seq = ++previewSeq
  previewing.value = true
  try {
    const blob = await printPdf(req)
    if (seq !== previewSeq) return
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewBlob = blob
    previewUrl.value = URL.createObjectURL(blob)
    previewError.value = ''
  } catch (e) {
    if (seq === previewSeq) previewError.value = e?.message || t('print.template.previewFailed')
  } finally {
    if (seq === previewSeq) previewing.value = false
  }
}

watch(
  () => JSON.stringify(props.watchKey),
  () => {
    clearTimeout(previewTimer)
    previewTimer = setTimeout(render, 500)
  }
)

function zoom() {
  if (previewBlob) showPdf(previewBlob, props.zoomTitle)
}

onMounted(render)

onBeforeUnmount(() => {
  clearTimeout(previewTimer)
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})

defineExpose({ render })
</script>

<style scoped>
.preview {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  background: var(--app-hover);
  padding: 8px;
}
.preview-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-3);
  margin-bottom: 6px;
}
.spacer {
  flex: 1;
}
.preview-frame {
  flex: 1;
  min-height: 480px;
  width: 100%;
  border: 0;
  border-radius: 4px;
  background: #fff;
}
.preview-empty {
  flex: 1;
  min-height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-3);
}
</style>
