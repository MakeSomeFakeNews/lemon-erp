<template>
  <div class="ai-input" @paste="onPaste">
    <a-upload-dragger :file-list="[]" :accept="ACCEPT" multiple :before-upload="onFile" :disabled="disabled" class="drop">
      <p class="ant-upload-drag-icon"><InboxOutlined /></p>
      <p class="ant-upload-text">{{ $t('ai.extract.dropHint') }}</p>
      <p class="ant-upload-hint">{{ $t('ai.extract.dropTypes', { images: MAX_IMAGES }) }}</p>
    </a-upload-dragger>

    <div v-if="items.length" class="files">
      <div v-for="(it, i) in items" :key="it.id" class="file">
        <img v-if="it.url" :src="it.url" alt="" class="thumb" />
        <span v-else class="file-icon">
          <FilePdfOutlined v-if="it.kind === 'pdf'" />
          <FileExcelOutlined v-else-if="it.kind === 'sheet'" />
          <FileTextOutlined v-else />
        </span>
        <div class="file-meta">
          <div class="file-name" :title="it.file.name">{{ it.file.name }}</div>
          <div class="file-sub">
            <template v-if="it.parsing"><LoadingOutlined /> {{ $t('ai.extract.parsing') }}</template>
            <template v-else>{{ $t(`ai.extract.kind.${it.kind}`) }} · {{ size(it.file.size) }}</template>
          </div>
        </div>
        <a-button type="text" size="small" :disabled="disabled" @click="remove(i)"><template #icon><CloseOutlined /></template></a-button>
      </div>
    </div>

    <a-textarea
      :value="text"
      :rows="rows"
      :maxlength="20000"
      :disabled="disabled"
      :placeholder="$t('ai.extract.textPlaceholder')"
      class="text"
      @update:value="(v) => (text = v)"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CloseOutlined, FileExcelOutlined, FilePdfOutlined, FileTextOutlined, InboxOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { ACCEPT, MAX_FILES, MAX_IMAGES, fileKind, overLimit, sheetToText } from '@/utils/aiInput'

/**
 * 智能录入的输入：拖拽 / 选择文件（图片、PDF、Excel、CSV、文本），粘贴截图（Ctrl / ⌘ + V），粘贴文字。
 * Excel 在浏览器中解析成表格文字；图片、PDF、CSV、文本原样提交给后端（只在内存中处理，不保存）。
 * 通过 ref 调用 collect() 取得 { text, files }，reset() 清空；isEmpty 表示还没有内容
 */
defineProps({
  disabled: Boolean,
  rows: { type: Number, default: 4 }
})

const { t } = useI18n()
const items = ref([])
const text = ref('')
let seq = 0

function add(file) {
  const kind = fileKind(file)
  if (!kind) {
    message.warning(t('ai.extract.unsupported', { name: file.name }))
    return
  }
  const limit = overLimit(file, kind)
  if (limit) {
    message.warning(t('ai.extract.tooLarge', { name: file.name, mb: limit }))
    return
  }
  if (items.value.length >= MAX_FILES) {
    message.warning(t('ai.extract.tooManyFiles', { n: MAX_FILES }))
    return
  }
  if (kind === 'image' && items.value.filter((i) => i.kind === 'image').length >= MAX_IMAGES) {
    message.warning(t('ai.extract.tooManyImages', { n: MAX_IMAGES }))
    return
  }
  const item = { id: ++seq, file, kind, url: kind === 'image' ? URL.createObjectURL(file) : null, text: null, parsing: kind === 'sheet' }
  items.value.push(item)
  if (kind === 'sheet') {
    sheetToText(file)
      .then((s) => {
        const it = items.value.find((x) => x.id === item.id)
        if (it) Object.assign(it, { text: s, parsing: false })
        if (!s) message.warning(t('ai.extract.sheetEmpty', { name: file.name }))
      })
      .catch(() => {
        message.error(t('ai.extract.sheetFailed', { name: file.name }))
        remove(items.value.findIndex((x) => x.id === item.id))
      })
  }
}

function onFile(file) {
  add(file)
  return false
}

/** 粘贴截图：剪贴板中的图片作为文件加入（文字照常粘贴到输入框） */
function onPaste(e) {
  const files = [...(e.clipboardData?.items || [])].filter((i) => i.kind === 'file').map((i) => i.getAsFile()).filter(Boolean)
  if (!files.length) return
  e.preventDefault()
  const stamp = new Date().toTimeString().slice(0, 8).replace(/:/g, '')
  files.forEach((f, i) => {
    const ext = (f.type.split('/')[1] || 'png').replace('jpeg', 'jpg')
    add(f.name && f.name !== 'image.png' ? f : new File([f], `paste-${stamp}${i ? '-' + i : ''}.${ext}`, { type: f.type }))
  })
}

function remove(index) {
  if (index < 0) return
  const [it] = items.value.splice(index, 1)
  if (it?.url) URL.revokeObjectURL(it.url)
}

const size = (n) => (n >= 1024 * 1024 ? `${(n / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`)

const isEmpty = computed(() => !text.value.trim() && !items.value.length)
const parsing = computed(() => items.value.some((i) => i.parsing))

/** 提交的内容：Excel 解析出的文字（带文件名）拼在粘贴的文字前面，其余文件原样提交 */
function collect() {
  const sheets = items.value.filter((i) => i.kind === 'sheet' && i.text).map((i) => `## ${i.file.name}\n${i.text}`)
  const all = [...sheets, text.value.trim()].filter(Boolean).join('\n\n')
  return { text: all, files: items.value.filter((i) => i.kind !== 'sheet').map((i) => i.file) }
}

function reset() {
  items.value.forEach((i) => i.url && URL.revokeObjectURL(i.url))
  items.value = []
  text.value = ''
}

onBeforeUnmount(reset)

defineExpose({ collect, reset, isEmpty, parsing })
</script>

<style scoped>
.drop {
  display: block;
}
.files {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  min-width: 0;
}
.thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  flex: none;
}
.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--app-text-3);
  flex: none;
}
.file-meta {
  flex: 1;
  min-width: 0;
}
.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.text {
  margin-top: 12px;
}
</style>
