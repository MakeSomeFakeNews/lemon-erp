<template>
  <a-drawer :open="open" :title="$t('system.notice.detailTitle')" width="min(760px, 100vw)" destroy-on-close @close="open = false">
    <a-spin :spinning="loading">
      <NoticeView v-if="notice" :notice="notice" />
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { ref } from 'vue'
import NoticeView from './NoticeView.vue'
import { getMyNotice, readMyNotice } from '@/api/system/notice'

/**
 * 员工端公告详情：open(id) 打开，未读的打开即标记已读（对应的站内消息一并已读），触发 read 事件让列表刷新未读标记
 */
const emit = defineEmits(['read'])
const open = ref(false)
const loading = ref(false)
const notice = ref(null)

async function show(id) {
  notice.value = null
  open.value = true
  loading.value = true
  try {
    notice.value = await getMyNotice(id)
  } catch {
    // 不存在、已撤回或已过期：错误提示由请求拦截器显示
    open.value = false
    return
  } finally {
    loading.value = false
  }
  if (!notice.value.read) {
    await readMyNotice(id)
    notice.value.read = true
    emit('read', id)
  }
}

defineExpose({ open: show })
</script>
