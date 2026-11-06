<template>
  <div class="conv-panel">
    <a-button type="primary" ghost block class="new-btn" @click="emit('new')">
      <template #icon><PlusOutlined /></template>{{ $t('ai.chat.newChat') }}
    </a-button>
    <div class="conv-list">
      <div
        v-for="c in conversations"
        :key="c.id"
        class="conv"
        :class="{ active: c.id === currentId }"
        @click="emit('select', c.id)"
      >
        <MessageOutlined class="conv-icon" />
        <div class="conv-main">
          <div class="conv-title">{{ c.title || $t('ai.chat.untitled') }}</div>
          <div class="sub-text">{{ c.updateTime }}</div>
        </div>
        <a-dropdown :trigger="['click']">
          <EllipsisOutlined class="conv-more" @click.stop />
          <template #overlay>
            <a-menu>
              <a-menu-item key="rename" @click="emit('rename', c)"><EditOutlined /> {{ $t('ai.chat.rename') }}</a-menu-item>
              <a-menu-item key="delete" danger @click="emit('remove', c)"><DeleteOutlined /> {{ $t('common.action.delete') }}</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <a-empty v-if="!conversations.length" :image="simpleImage" :description="$t('ai.chat.noConversations')" />
    </div>
  </div>
</template>

<script setup>
import { Empty } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, EllipsisOutlined, MessageOutlined, PlusOutlined } from '@ant-design/icons-vue'

/** AI 问数的会话列表：宽屏显示在左侧，窄屏放在抽屉中（新建、切换、重命名、删除由父组件处理） */
defineProps({
  conversations: { type: Array, required: true },
  currentId: { type: Number, default: null }
})
const emit = defineEmits(['new', 'select', 'rename', 'remove'])
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
</script>

<style scoped>
.conv-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.new-btn {
  margin-bottom: 8px;
}
.conv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.conv {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
}
.conv:hover {
  background: var(--app-hover);
}
.conv.active {
  background: var(--app-primary-bg);
}
.conv-icon {
  color: var(--app-text-3);
}
.conv-main {
  flex: 1;
  min-width: 0;
}
.conv-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.conv-more {
  visibility: hidden;
  padding: 4px;
  color: var(--app-text-3);
}
.conv:hover .conv-more,
.conv.active .conv-more {
  visibility: visible;
}
/* 触屏没有悬停：一直显示操作按钮 */
@media (hover: none) {
  .conv-more {
    visibility: visible;
  }
}
</style>
