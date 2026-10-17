<template>
  <a-popover v-model:open="open" trigger="click" placement="bottomRight" :overlay-inner-style="{ padding: 0 }" @open-change="onOpen">
    <!-- 面板打开时不显示提示，避免遮住「全部已读」 -->
    <a-tooltip :title="open ? null : $t('message.message')">
      <a-button type="text" class="header-btn">
        <template #icon>
          <a-badge :count="count" :overflow-count="99" size="small" :offset="[2, -2]">
            <BellOutlined class="bell" />
          </a-badge>
        </template>
      </a-button>
    </a-tooltip>
    <template #content>
      <div class="panel">
        <div class="panel-head">
          <span>{{ $t('components.layout.unreadMessages') }}<span v-if="count" class="head-count">{{ count }}</span></span>
          <a-button type="link" size="small" :disabled="!count" @click="readAll">{{ $t('message.markAllRead') }}</a-button>
        </div>
        <a-spin :spinning="loading">
          <div v-if="!list.length" class="empty">{{ $t('components.layout.noUnread') }}</div>
          <!-- 列表区域固定高度（约 4-5 条），更多消息上下滚动 -->
          <div v-else class="list">
            <div v-for="m in list" :key="m.id" class="item" @click="go(m)">
              <div class="item-title">
                <DictTag type="sys_message_category" :value="m.category" />
                <span class="title-text">{{ m.title }}</span>
              </div>
              <div v-if="m.content && m.content !== m.title" class="item-content">{{ m.content }}</div>
              <div class="item-time">{{ m.createTime }}</div>
            </div>
            <div v-if="count > list.length" class="more" @click="goAll">{{ $t('components.layout.moreUnread', { n: count - list.length }) }}</div>
          </div>
        </a-spin>
        <div class="panel-foot">
          <a @click="goAll">{{ $t('components.layout.viewAll') }}</a>
        </div>
      </div>
    </template>
  </a-popover>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { pageMessage, readMessage, unreadCount } from '@/api/system/message'
import { useDict } from '@/composables/useDict'

/** 顶栏消息铃铛：每分钟刷新未读数，点开显示最近 20 条未读（列表区域可滚动） */
useDict('sys_message_category')
const router = useRouter()
const open = ref(false)
const count = ref(0)
const list = ref([])
const loading = ref(false)
let timer

async function refreshCount() {
  count.value = await unreadCount().catch(() => count.value)
}

async function onOpen(visible) {
  if (!visible) return
  loading.value = true
  try {
    list.value = (await pageMessage({ current: 1, size: 20, unread: true })).records
  } finally {
    loading.value = false
  }
}

async function go(m) {
  await readMessage([m.id])
  open.value = false
  refreshCount()
  if (m.link) router.push(m.link).catch(() => {})
}

async function readAll() {
  await readMessage([])
  list.value = []
  count.value = 0
}

function goAll() {
  open.value = false
  router.push('/messages')
}

onMounted(() => {
  refreshCount()
  timer = setInterval(refreshCount, 60000)
})
onBeforeUnmount(() => clearInterval(timer))

defineExpose({ refreshCount })
</script>

<style scoped>
.bell {
  font-size: 16px;
}
.panel {
  width: 340px;
}
.panel-head,
.panel-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
}
.panel-head {
  border-bottom: 1px solid var(--app-border);
  font-weight: 500;
}
.panel-foot {
  justify-content: center;
  border-top: 1px solid var(--app-border);
}
.head-count {
  margin-left: 6px;
  color: var(--app-text-3);
  font-weight: normal;
}
.list {
  max-height: 380px;
  overflow-y: auto;
  overscroll-behavior: contain;
}
.list .item:last-child {
  border-bottom: none;
}
.more {
  padding: 8px 12px;
  text-align: center;
  font-size: 12px;
  color: var(--app-text-3);
  cursor: pointer;
}
.more:hover {
  color: var(--app-primary);
}
.empty {
  padding: 32px 0;
  text-align: center;
  color: var(--app-text-3);
}
.item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--app-border);
}
.item:hover {
  background: var(--app-hover);
}
.item-title {
  display: flex;
  align-items: center;
  gap: 6px;
}
.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-content {
  margin-top: 4px;
  color: var(--app-text-2);
  font-size: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-time {
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
