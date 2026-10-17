<template>
  <div class="check-list">
    <div v-for="(item, i) in items" :key="i" :class="['check-item', item.level]">
      <CheckCircleFilled v-if="item.level === 'OK'" class="icon ok" />
      <ExclamationCircleFilled v-else-if="item.level === 'WARN'" class="icon warn" />
      <CloseCircleFilled v-else class="icon error" />
      <div class="body">
        <div class="name">{{ item.name }}</div>
        <div v-if="item.message" class="message">{{ item.message }}</div>
      </div>
    </div>
    <a-empty v-if="!items.length" :image="simpleImage" :description="$t('fin.closeCheckList.empty')" />
  </div>
</template>

<script setup>
import { Empty } from 'ant-design-vue'
import { CheckCircleFilled, CloseCircleFilled, ExclamationCircleFilled } from '@ant-design/icons-vue'

/** 结账检查结果：items [{ name, level: OK | WARN | ERROR, message }] */
defineProps({
  items: { type: Array, default: () => [] }
})
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
</script>

<style scoped>
.check-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.check-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--app-bg-2, #fafafa);
}
.check-item.WARN {
  background: rgba(250, 140, 22, 0.08);
}
.check-item.ERROR {
  background: rgba(207, 19, 34, 0.08);
}
.icon {
  font-size: 16px;
  margin-top: 3px;
}
.ok {
  color: #52c41a;
}
.warn {
  color: #fa8c16;
}
.error {
  color: #cf1322;
}
.name {
  font-weight: 500;
}
.message {
  color: var(--app-text-2);
  font-size: 13px;
}
</style>
