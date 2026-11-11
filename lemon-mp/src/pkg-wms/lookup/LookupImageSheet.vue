<template>
  <view v-if="visible" class="ss-mask" @click.self="visible = false">
    <view class="ss-panel">
      <view class="ss-title">{{ t('lookup.image.title', { name: material?.name || '' }) }}</view>
      <view v-if="material?.spec || material?.code" class="lk-sheet-sub l-muted">{{ [material.code, material.spec].filter(Boolean).join(' · ') }}</view>
      <scroll-view scroll-y class="ss-list">
        <view v-if="loading" class="ss-loading l-muted">{{ t('common.loading') }}</view>
        <view v-else-if="!urls.length" class="ss-loading l-muted">{{ t('lookup.image.empty') }}</view>
        <view v-else class="li-grid">
          <image v-for="(u, i) in urls" :key="u" :src="u" mode="aspectFill" class="li-img" @click="preview(i)" />
        </view>
      </scroll-view>
      <view class="ss-cancel" @click="visible = false">{{ t('common.cancel') }}</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from '@/locales'
import { materialImageUrls } from '@/api/lookup'
import { fileUrl } from '@/utils/request'

/**
 * 商品图片：物料资料中的商品图片（issue #94，第一张为主图，公开文件地址，登录即可读取），点击放大。用法：ref.open(row)
 */
const { t } = useI18n()
const visible = ref(false)
const loading = ref(false)
const material = ref(null)
const urls = ref([])

let seq = 0
async function load() {
  const current = ++seq
  const m = material.value
  urls.value = m.imageUrl ? [fileUrl(m.imageUrl)] : []
  loading.value = !urls.value.length
  try {
    const map = (await materialImageUrls([m.materialId]).catch(() => null)) || {}
    const list = (map[m.materialId] || map[String(m.materialId)] || []).map(fileUrl)
    if (current === seq && list.length) urls.value = list
  } finally {
    if (current === seq) loading.value = false
  }
}

function preview(i) {
  uni.previewImage({ urls: urls.value, current: urls.value[i] })
}

defineExpose({
  open(m) {
    material.value = m
    visible.value = true
    load()
  }
})
</script>

<style lang="scss">
@import '../sheet.scss';
@import './lookup.scss';

.li-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 0 24rpx 24rpx;
}
.li-img {
  width: calc((100% - 32rpx) / 3);
  height: 220rpx;
  border-radius: $l-radius;
  background: #fff;
}
</style>
