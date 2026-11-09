<template>
  <a-tooltip v-if="list.length > 1">
    <template #title>
      <div v-for="n in list" :key="n">{{ n }}</div>
    </template>
    <a-tag color="blue" :bordered="false">{{ $t('wms.common.multiWarehouse', { n: list.length }) }}</a-tag>
  </a-tooltip>
  <span v-else>{{ list[0] || name }}</span>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 单据列表的仓库列：明细可另选仓库时，涉及多个仓库显示「多仓（N）」，悬停查看仓库列表；只有一个时显示该仓库。
 * names 为后端列表返回的 warehouseNames（表头仓库在前），name 为表头仓库
 */
const props = defineProps({
  name: String,
  names: Array
})
const list = computed(() => props.names || [])
</script>
