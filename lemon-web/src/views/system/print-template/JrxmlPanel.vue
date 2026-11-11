<template>
  <div class="jrxml">
    <div class="jrxml-status">
      <a-tag v-if="custom" color="green" :bordered="false">{{ $t('print.template.jrxmlUploaded') }}</a-tag>
      <a-tag v-else :bordered="false">{{ $t('print.template.jrxmlNone') }}</a-tag>
    </div>
    <a-space wrap>
      <a-button size="small" :loading="exporting" @click="downloadDefault">
        <template #icon><DownloadOutlined /></template>{{ $t('print.template.downloadDefault') }}
      </a-button>
      <a-button v-if="custom" size="small" @click="downloadCustom"><template #icon><DownloadOutlined /></template>{{ $t('print.template.downloadUploaded') }}</a-button>
      <span v-perm="'sys:printTemplate:edit'">
        <a-upload accept=".jrxml,.xml" :show-upload-list="false" :before-upload="upload">
          <a-button size="small" :loading="uploading"><template #icon><UploadOutlined /></template>{{ $t('print.template.uploadJrxml') }}</a-button>
        </a-upload>
      </span>
      <span v-if="custom" v-perm="'sys:printTemplate:edit'">
        <a-popconfirm :title="$t('print.template.removeJrxmlConfirm')" @confirm="removeCustom">
          <a-button size="small" danger>{{ $t('common.action.delete') }}</a-button>
        </a-popconfirm>
      </span>
    </a-space>
    <!-- 名称为中文匹配键，示例中保持中文 -->
    <i18n-t keypath="print.template.jrxmlParams" tag="div" class="jrxml-hint">
      <template #fields><code>{{ jrxmlFieldExample }}</code></template>
      <template #column><code>{{ jrxmlColumnExample }}</code></template>
    </i18n-t>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { defaultJrxml } from '@/api/system/print'
import { downloadJrxml, removeJrxml, uploadJrxml } from '@/api/system/printTemplate'
import { printName } from '@/utils/printTemplates'

/** 打印模板页面的 JRXML：下载按当前配置生成的默认模板、上传 / 下载 / 删除当前格式的 JRXML；上传、删除后触发 changed */
const props = defineProps({
  /** 模板标识 */
  typeKey: { type: String, required: true },
  /** 当前编辑的格式，为空时为默认格式 */
  formatId: Number,
  /** 当前格式已上传 JRXML */
  custom: Boolean,
  /** () => 按当前配置生成的打印请求（下载默认模板用） */
  buildRequest: { type: Function, required: true }
})
const emit = defineEmits(['changed'])
const { t } = useI18n()

/** JRXML 中按中文名称取值的示例 */
const jrxmlFieldExample = `$P{FIELDS}.get("${printName('supplier')}")`
const jrxmlColumnExample = `$F{${printName('materialName')}}`

function saveBlob(blob, name) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

const exporting = ref(false)
async function downloadDefault() {
  exporting.value = true
  try {
    // 按当前配置生成，已上传 JRXML 时也下载按配置生成的默认版式
    saveBlob(await defaultJrxml(props.buildRequest()), `${props.typeKey}.jrxml`)
  } finally {
    exporting.value = false
  }
}

async function downloadCustom() {
  saveBlob(await downloadJrxml(props.typeKey, props.formatId), `${props.typeKey}.jrxml`)
}

const uploading = ref(false)
async function upload(file) {
  uploading.value = true
  try {
    await uploadJrxml(props.typeKey, file, props.formatId)
    emit('changed')
    message.success(t('print.template.uploaded'))
  } finally {
    uploading.value = false
  }
  return false
}

async function removeCustom() {
  await removeJrxml(props.typeKey, props.formatId)
  emit('changed')
  message.success(t('print.template.removed'))
}
</script>

<style scoped>
.jrxml {
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 10px 12px;
}
.jrxml-status {
  margin-bottom: 8px;
}
.jrxml-hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-3);
}
.jrxml-hint code {
  font-size: 12px;
}
</style>
