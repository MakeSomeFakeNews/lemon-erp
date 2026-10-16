<template>
  <div class="panel-actions">
    <span v-if="dirty" class="panel-dirty"><ExclamationCircleOutlined /> 有未保存的修改</span>
    <a-button :disabled="!dirty || saving" @click="emit('reset')">撤销修改</a-button>
    <a-button type="primary" :disabled="!dirty" :loading="saving" @click="emit('save')">保存</a-button>
  </div>
</template>

<script setup>
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'

/** 角色详情各面板底部的保存 / 撤销按钮 */
defineProps({
  dirty: Boolean,
  saving: Boolean
})
const emit = defineEmits(['save', 'reset'])
</script>

<style scoped>
.panel-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 0;
  border-top: 1px solid var(--app-border);
  /* 内容较长时（如菜单树）保存按钮始终可见 */
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: var(--app-card);
}
.panel-dirty {
  margin-right: auto;
  color: #fa8c16;
  font-size: 13px;
}
</style>
