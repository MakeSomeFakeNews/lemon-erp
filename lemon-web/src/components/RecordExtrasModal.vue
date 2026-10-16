<template>
  <a-modal :open="open" :title="title" :footer="null" width="min(820px, 100vw)" destroy-on-close @cancel="emit('update:open', false)">
    <a-tabs>
      <a-tab-pane key="attachment" tab="附件">
        <AttachmentPanel :biz-type="bizType" :biz-id="bizId" :editable="editable" />
      </a-tab-pane>
      <a-tab-pane key="history" tab="变更记录">
        <ChangeLogPanel :biz-type="bizType" :biz-id="bizId" />
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup>
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel.vue'

/** 列表页的「附件 / 变更记录」弹窗，用于没有详情页的主数据（物料、客户、供应商） */
defineProps({
  open: Boolean,
  title: String,
  bizType: { type: String, required: true },
  bizId: [Number, String],
  editable: Boolean
})
const emit = defineEmits(['update:open'])
</script>
