<template>
  <div class="material-images">
    <a-spin :spinning="loading">
      <a-image-preview-group>
        <div class="grid">
          <div v-for="(img, i) in list" :key="img.id" class="cell">
            <a-image :src="img.url" :width="size" :height="size" class="thumb" />
            <a-tag v-if="i === 0" color="blue" :bordered="false" class="main-tag">{{ $t('wms.materialImage.main') }}</a-tag>
            <div v-if="editable" class="ops">
              <a-tooltip :title="$t('wms.materialImage.moveLeft')">
                <a-button type="text" size="small" :disabled="i === 0 || busy" @click="move(i, -1)"><LeftOutlined /></a-button>
              </a-tooltip>
              <a-tooltip :title="$t('wms.materialImage.setMain')">
                <a-button type="text" size="small" :disabled="i === 0 || busy" @click="setMain(img)"><StarOutlined /></a-button>
              </a-tooltip>
              <a-tooltip :title="$t('wms.materialImage.moveRight')">
                <a-button type="text" size="small" :disabled="i === list.length - 1 || busy" @click="move(i, 1)"><RightOutlined /></a-button>
              </a-tooltip>
              <a-popconfirm :title="$t('wms.materialImage.confirmDelete')" @confirm="remove(img)">
                <a-button type="text" size="small" danger :disabled="busy"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </div>
          </div>
          <a-upload
            v-if="editable && list.length < MAX_IMAGES"
            :file-list="[]"
            multiple
            accept=".png,.jpg,.jpeg,.gif,.webp"
            :custom-request="upload"
            :before-upload="check"
            :show-upload-list="false"
          >
            <div class="cell add" :style="{ width: `${size}px`, height: `${size}px` }">
              <LoadingOutlined v-if="uploading" />
              <PlusOutlined v-else />
              <div class="add-text">{{ $t('wms.materialImage.upload') }}</div>
            </div>
          </a-upload>
        </div>
      </a-image-preview-group>
      <div v-if="!list.length && !editable" class="empty">{{ $t('wms.materialImage.empty') }}</div>
      <div v-if="editable" class="hint">{{ $t('wms.materialImage.hint', { n: MAX_IMAGES, mb: MAX_MB }) }}</div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, LeftOutlined, LoadingOutlined, PlusOutlined, RightOutlined, StarOutlined } from '@ant-design/icons-vue'
import { deleteMaterialImage, materialImages, reorderMaterialImages, setMainMaterialImage, uploadMaterialImage } from '@/api/wms/material'

/**
 * 物料（商品）图片：第一张为主图（列表、下拉、小程序显示的缩略图），点击查看大图；
 * editable 时可上传（png / jpg / gif / webp，单张不超过 5 MB，最多 10 张）、左右移动排序、设为主图、删除。
 * 主图变化时触发 changed(主图 URL)
 */
const props = defineProps({
  materialId: Number,
  editable: Boolean,
  /** 缩略图边长（px） */
  size: { type: Number, default: 96 }
})
const emit = defineEmits(['changed'])
const { t } = useI18n()

const MAX_IMAGES = 10
const MAX_MB = 5
const TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']

const list = ref([])
const loading = ref(false)
const busy = ref(false)
const uploading = ref(0)

async function load() {
  if (!props.materialId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await materialImages(props.materialId)
  } finally {
    loading.value = false
  }
}
watch(() => props.materialId, load, { immediate: true })

const mainUrl = () => list.value[0]?.url || null

async function reload() {
  const before = mainUrl()
  await load()
  if (mainUrl() !== before) emit('changed', mainUrl())
}

function check(file) {
  if (!TYPES.includes(file.type)) {
    message.warning(t('wms.materialImage.badType', { name: file.name }))
    return false
  }
  if (file.size > MAX_MB * 1024 * 1024) {
    message.warning(t('wms.materialImage.tooLarge', { name: file.name, mb: MAX_MB }))
    return false
  }
  return true
}

async function upload({ file, onSuccess, onError }) {
  uploading.value++
  try {
    await uploadMaterialImage(props.materialId, file)
    onSuccess()
  } catch (e) {
    onError(e)
  } finally {
    uploading.value--
    if (!uploading.value) await reload()
  }
}

async function run(fn) {
  busy.value = true
  try {
    await fn()
    await reload()
  } finally {
    busy.value = false
  }
}

const move = (i, step) =>
  run(() => {
    const ids = list.value.map((img) => img.id)
    ;[ids[i], ids[i + step]] = [ids[i + step], ids[i]]
    return reorderMaterialImages(props.materialId, ids)
  })
const setMain = (img) => run(() => setMainMaterialImage(props.materialId, img.id))
const remove = (img) => run(() => deleteMaterialImage(props.materialId, img.id))

defineExpose({ reload })
</script>

<style scoped>
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.cell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.thumb :deep(img),
:deep(.thumb) {
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--app-border);
}
.main-tag {
  position: absolute;
  top: 4px;
  left: 4px;
}
.ops {
  display: flex;
  gap: 2px;
  margin-top: 4px;
}
.add {
  justify-content: center;
  border: 1px dashed var(--app-border);
  border-radius: 6px;
  cursor: pointer;
  color: var(--app-text-3);
}
.add:hover {
  border-color: var(--app-primary);
  color: var(--app-primary);
}
.add-text {
  margin-top: 6px;
  font-size: 12px;
}
.hint,
.empty {
  margin-top: 8px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
