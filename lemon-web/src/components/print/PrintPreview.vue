<template>
  <a-modal
    v-model:open="printPreview.open"
    :title="printPreview.title || '打印预览'"
    :width="960"
    centered
    destroy-on-close
    wrap-class-name="print-preview-modal"
    @after-close="release"
  >
    <iframe ref="frame" class="print-frame" :src="printPreview.url" title="打印预览" />
    <template #footer>
      <a-button @click="download"><template #icon><DownloadOutlined /></template>下载 PDF</a-button>
      <a-button type="primary" @click="print"><template #icon><PrinterOutlined /></template>打印</a-button>
    </template>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import { printPreview } from '@/utils/print'

/** 打印预览：显示后端生成的 PDF（浏览器内置的 PDF 阅读器），可直接打印或下载 */
const frame = ref()

function print() {
  const w = frame.value?.contentWindow
  if (!w) return
  w.focus()
  w.print()
}

function download() {
  const a = document.createElement('a')
  a.href = printPreview.url
  a.download = `${printPreview.title || '打印'}.pdf`
  a.click()
}

function release() {
  if (printPreview.url) URL.revokeObjectURL(printPreview.url)
  printPreview.url = ''
}
</script>

<style scoped>
.print-frame {
  display: block;
  width: 100%;
  height: 72vh;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  background: #fff;
}
</style>
