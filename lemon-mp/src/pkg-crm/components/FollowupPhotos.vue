<template>
  <view v-if="paths.length" class="fp">
    <image v-for="(p, i) in paths" :key="i" :src="p" class="fp__img" mode="aspectFill" @click.stop="preview(i)" />
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { followupPhotos } from '../photos'

/** 跟进记录的照片缩略图，点击预览 */
const props = defineProps({ followupId: { type: [Number, String], required: true } })
const paths = ref([])

watch(
  () => props.followupId,
  async (id) => {
    paths.value = id ? await followupPhotos(id) : []
  },
  { immediate: true }
)

function preview(i) {
  uni.previewImage({ urls: paths.value, current: paths.value[i] })
}
</script>

<style lang="scss">
.fp {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 12rpx;
  &__img {
    width: 150rpx;
    height: 150rpx;
    border-radius: 8rpx;
    background: #f5f5f5;
  }
}
</style>
