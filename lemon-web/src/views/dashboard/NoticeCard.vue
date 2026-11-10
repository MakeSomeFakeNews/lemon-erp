<template>
  <a-card :bordered="false" class="page-card notice-card">
    <template #title>
      <span>{{ $t('dashboard.notices') }}</span>
      <a-badge v-if="unread" :count="unread" :overflow-count="99" class="unread-badge" />
    </template>
    <template #extra><router-link to="/notices">{{ $t('common.status.all') }}</router-link></template>
    <a-skeleton v-if="loading && !list.length" active :paragraph="{ rows: 2 }" />
    <div v-else-if="!list.length" class="empty">{{ $t('dashboard.noNotices') }}</div>
    <ul v-else class="notice-list">
      <li v-for="n in list" :key="n.id" class="notice-item" @click="drawer.open(n.id)">
        <span :class="['dot', { read: n.read }]" />
        <DictTag type="sys_notice_type" :value="n.type" />
        <span :class="['notice-title', { unread: !n.read }]" :title="n.title">
          <PushpinFilled v-if="n.pinned" class="pin" />{{ n.title }}
        </span>
        <span class="notice-time">{{ shortDate(n.publishTime) }}</span>
      </li>
    </ul>
    <NoticeDrawer ref="drawer" @read="onRead" />
  </a-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { PushpinFilled } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import NoticeDrawer from '@/components/system/NoticeDrawer.vue'
import { myNoticeUnreadCount, pageMyNotice } from '@/api/system/notice'

/** 工作台「通知公告」：发给本人的有效公告，置顶在前，未读加粗；点击打开详情并标记已读 */
const SIZE = 5
const list = ref([])
const unread = ref(0)
const loading = ref(false)
const drawer = ref()

const shortDate = (time) => (time ? String(time).slice(5, 10) : '')

async function load() {
  loading.value = true
  try {
    const [page, count] = await Promise.all([pageMyNotice({ current: 1, size: SIZE }), myNoticeUnreadCount()])
    list.value = page.records || []
    unread.value = count
  } finally {
    loading.value = false
  }
}

function onRead(id) {
  const n = list.value.find((x) => x.id === id)
  if (n) n.read = true
  unread.value = Math.max(0, unread.value - 1)
}

onMounted(load)
</script>

<style scoped>
.unread-badge {
  margin-left: 8px;
}
.empty {
  color: var(--app-text-3);
  padding: 4px 0;
}
.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--app-border);
  border-radius: 6px;
  cursor: pointer;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-item:hover {
  background: var(--app-hover);
}
.notice-item :deep(.ant-tag) {
  margin-inline-end: 0;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--app-primary);
}
.dot.read {
  background: var(--app-border);
}
.notice-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--app-text);
}
.notice-title.unread {
  font-weight: 600;
}
.pin {
  color: var(--status-critical);
  margin-right: 4px;
}
.notice-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--app-text-3);
}
</style>
