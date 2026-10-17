<template>
  <span class="im-icon" :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${Math.round(size * 0.48)}px`, background: info.color }">
    {{ info.short }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { imPlatform } from '@/utils/im'

/** 平台图标：品牌色圆角方块 + 平台名首字 */
const props = defineProps({
  platform: { type: String, required: true },
  size: { type: Number, default: 32 }
})
const info = computed(() => imPlatform(props.platform))
</script>

<style scoped>
.im-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 28%;
  color: #fff;
  font-weight: 600;
  line-height: 1;
  user-select: none;
}
</style>
