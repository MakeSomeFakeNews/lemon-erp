<template>
  <span v-if="list.length" class="serial-tags">
    <a-tag v-for="sn in shown" :key="sn" class="serial-tag">{{ sn }}</a-tag>
    <a-popover v-if="list.length > limit" trigger="click" placement="left" :title="$t('wms.common.serialAll', { n: list.length })">
      <template #content>
        <div class="serial-all">
          <a-tag v-for="sn in list" :key="sn" class="serial-tag">{{ sn }}</a-tag>
        </div>
      </template>
      <a class="serial-more">{{ $t('wms.common.serialTotal', { n: list.length }) }}</a>
    </a-popover>
  </span>
  <span v-else>-</span>
</template>

<script setup>
import { computed } from 'vue'
import { splitSerials } from '@/components/wms/wms'

/**
 * 明细行的序列号：只显示前 limit 个，更多时显示「共 N 个」，点击在弹出框中查看全部（上千个序列号时行高不被撑开）
 * - value：序列号文本（逗号等分隔）
 * - limit：直接显示的个数，默认 3
 */
const props = defineProps({
  value: String,
  limit: { type: Number, default: 3 }
})
const list = computed(() => splitSerials(props.value))
const shown = computed(() => list.value.slice(0, props.limit))
</script>

<style scoped>
.serial-tag {
  margin: 2px 4px 2px 0;
}
.serial-more {
  white-space: nowrap;
}
.serial-all {
  max-width: min(520px, 80vw);
  max-height: 320px;
  overflow-y: auto;
}
</style>
