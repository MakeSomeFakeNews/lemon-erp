<template>
  <span v-if="value == null" class="muted">-</span>
  <span v-else-if="value === 0" class="muted">0</span>
  <span v-else :class="value > 0 ? 'gain' : 'loss'">{{ value > 0 ? '+' : '' }}{{ formatQty(value) }}</span>
</template>

<script setup>
import { formatQty } from './wms'

/** 带正负号和颜色的数量（盘点差异、库存流水的变动数量） */
defineProps({ value: Number })
</script>

<style scoped>
.muted {
  color: var(--app-text-3);
}
.gain {
  color: #52c41a;
}
.loss {
  color: #ff4d4f;
}
</style>
