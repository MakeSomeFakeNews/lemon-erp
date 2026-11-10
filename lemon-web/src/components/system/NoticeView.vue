<template>
  <div class="notice-view">
    <h2 class="notice-title">
      <PushpinFilled v-if="notice.pinned" class="pin" />
      {{ notice.title }}
    </h2>
    <div class="notice-meta">
      <DictTag type="sys_notice_type" :value="notice.type" />
      <span v-if="notice.publishTime">{{ notice.publishTime }}</span>
      <span v-if="notice.publishByName">{{ $t('system.notice.publishBy', { name: notice.publishByName }) }}</span>
      <span v-if="notice.expireDate" class="expire">{{ $t('system.notice.expireUntil', { date: notice.expireDate }) }}</span>
    </div>
    <a-divider class="divider" />
    <MarkdownView v-if="notice.content" :content="notice.content" class="notice-body" />
    <a-empty v-else :image="simpleImage" :description="$t('system.notice.noContent')" />
    <template v-if="notice.id && showAttachments">
      <div class="section-title">{{ $t('system.notice.attachments') }}</div>
      <AttachmentPanel biz-type="sys_notice" :biz-id="notice.id" />
    </template>
  </div>
</template>

<script setup>
import { Empty } from 'ant-design-vue'
import { PushpinFilled } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import MarkdownView from '@/components/ai/MarkdownView.vue'

/** 公告内容：标题、类型、发布信息、正文（Markdown 安全渲染）、附件；员工端详情和管理端预览共用 */
defineProps({
  notice: { type: Object, required: true },
  showAttachments: { type: Boolean, default: true }
})
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
</script>

<style scoped>
.notice-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--app-text);
  word-break: break-word;
}
.pin {
  color: var(--status-critical);
  font-size: 16px;
  margin-right: 4px;
}
.notice-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
  color: var(--app-text-3);
  font-size: 13px;
}
.divider {
  margin: 14px 0;
}
.notice-body {
  font-size: 14px;
}
.section-title {
  margin: 20px 0 8px;
  font-weight: 600;
  color: var(--app-text);
}
</style>
