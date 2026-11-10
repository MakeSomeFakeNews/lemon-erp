<template>
  <a-popconfirm :title="$t('wms.doc.confirmReceive', { name: warehouseName })" @open-change="onOpen" @confirm="emit('confirm', receiveDate)">
    <template #description>
      <div>{{ $t('wms.doc.receiveTip') }}</div>
      <div class="receive-date">
        <span>{{ $t('wms.doc.receiveDate') }}</span>
        <a-date-picker
          v-model:value="receiveDate"
          value-format="YYYY-MM-DD"
          size="small"
          :allow-clear="false"
          :get-popup-container="(trigger) => trigger.parentNode"
        />
      </div>
    </template>
    <slot />
  </a-popconfirm>
</template>

<script setup>
// 在途调拨收货确认：可选择收货日期（默认当天），确认时把日期随 confirm 事件传出
import dayjs from 'dayjs'
import { ref } from 'vue'

defineProps({ warehouseName: { type: String, default: '' } })
const emit = defineEmits(['confirm'])
const receiveDate = ref(dayjs().format('YYYY-MM-DD'))

function onOpen(open) {
  if (open) {
    receiveDate.value = dayjs().format('YYYY-MM-DD')
  }
}
</script>

<style scoped>
.receive-date {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
</style>
