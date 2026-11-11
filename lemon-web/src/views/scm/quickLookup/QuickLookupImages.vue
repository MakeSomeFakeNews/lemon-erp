<template>
  <a-modal v-model:open="visible" :title="$t('pricing.lookup.imageTitle', { name: material?.name || '' })" width="min(760px, 100vw)" :footer="null">
    <a-spin :spinning="loading">
      <a-empty v-if="!loading && !urls.length" :description="$t('pricing.lookup.imageEmpty')" />
      <a-image-preview-group v-else>
        <div class="ql-images">
          <a-image v-for="u in urls" :key="u" :src="u" :width="160" :height="160" class="ql-image" />
        </div>
      </a-image-preview-group>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { materialImageUrls } from '@/api/scm/quickLookup'

/** 商品图片：物料资料中的商品图片（issue #94，第一张为主图，公开文件地址），点击放大。用法：ref.open(row) */
const visible = ref(false)
const loading = ref(false)
const material = ref()
const urls = ref([])

let seq = 0
async function open(m) {
  const current = ++seq
  material.value = m
  urls.value = m.imageUrl ? [m.imageUrl] : []
  visible.value = true
  loading.value = !urls.value.length
  try {
    const map = (await materialImageUrls([m.materialId])) || {}
    const list = map[m.materialId] || []
    if (current === seq && list.length) urls.value = list
  } finally {
    if (current === seq) loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.ql-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.ql-image :deep(img) {
  object-fit: cover;
  border-radius: 6px;
}
</style>
