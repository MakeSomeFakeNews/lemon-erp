<template>
  <a-modal
    :open="open"
    :title="material ? $t('wms.materialImage.title', { name: `${material.code} ${material.name}` }) : ''"
    :footer="null"
    width="min(720px, 100vw)"
    destroy-on-close
    @cancel="open = false"
  >
    <MaterialImages v-if="material" :material-id="material.id" :editable="editable" @changed="(url) => emit('changed', material.id, url)" />
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import MaterialImages from '@/components/wms/MaterialImages.vue'

/** 物料图片维护（列表「更多 → 图片」）：主图变化时触发 changed(物料ID, 主图URL) */
defineProps({ editable: Boolean })
const emit = defineEmits(['changed'])

const open = ref(false)
const material = ref()

function show(record) {
  material.value = record
  open.value = true
}

defineExpose({ open: show })
</script>
