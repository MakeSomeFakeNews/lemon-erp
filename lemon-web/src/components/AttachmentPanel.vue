<template>
  <div class="attachment-panel">
    <a-upload
      v-if="editable"
      :file-list="[]"
      :multiple="true"
      :custom-request="upload"
      :before-upload="check"
      :disabled="!bizId"
    >
      <a-button size="small" :loading="uploading > 0">
        <template #icon><UploadOutlined /></template>上传附件
      </a-button>
      <span class="hint">单个文件不超过 {{ configStore.attachmentMaxMb }}MB</span>
    </a-upload>
    <a-list :data-source="list" :loading="loading" size="small" :locale="{ emptyText: '暂无附件' }">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>
              <a @click="download(item)"><PaperClipOutlined /> {{ item.fileName }}</a>
            </template>
            <template #description>
              {{ formatSize(item.fileSize) }} · {{ item.createByName || '-' }} · {{ item.createTime }}
            </template>
          </a-list-item-meta>
          <template #actions>
            <a @click="download(item)"><DownloadOutlined /></a>
            <a-popconfirm v-if="editable" title="确定删除该附件？" @confirm="remove(item)">
              <a class="danger"><DeleteOutlined /></a>
            </a-popconfirm>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, DownloadOutlined, PaperClipOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { deleteAttachment, downloadAttachment, listAttachment, uploadAttachment } from '@/api/system/attachment'
import { useConfigStore } from '@/store/config'
import { saveBlob } from '@/utils/excel'

/**
 * 单据附件：bizType 为后端 AttachmentType.code（如 scm_purchase），editable 控制上传、删除
 */
const props = defineProps({
  bizType: { type: String, required: true },
  bizId: [Number, String],
  editable: Boolean
})

const configStore = useConfigStore()
const list = ref([])
const loading = ref(false)
const uploading = ref(0)

async function load() {
  if (!props.bizId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await listAttachment(props.bizType, props.bizId)
  } finally {
    loading.value = false
  }
}
watch(() => [props.bizType, props.bizId], load, { immediate: true })

function check(file) {
  if (file.size > configStore.attachmentMaxMb * 1024 * 1024) {
    message.warning(`${file.name} 超过 ${configStore.attachmentMaxMb}MB`)
    return false
  }
  return true
}

async function upload({ file, onSuccess, onError }) {
  uploading.value++
  try {
    const a = await uploadAttachment(props.bizType, props.bizId, file)
    list.value.push(a)
    onSuccess(a)
  } catch (e) {
    onError(e)
  } finally {
    uploading.value--
  }
}

async function download(item) {
  saveBlob(await downloadAttachment(item.id), item.fileName)
}

async function remove(item) {
  await deleteAttachment(item.id)
  list.value = list.value.filter((a) => a.id !== item.id)
  message.success('已删除')
}

function formatSize(n) {
  if (n == null) return '-'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.hint {
  margin-left: 8px;
  color: var(--ant-color-text-tertiary, #999);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
</style>
