<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('system.notice.createTitle')" :edit-title="$t('system.notice.editTitle')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="notice-form">
      <a-row :gutter="16">
        <a-col :xs="24" :md="16">
          <a-form-item :label="$t('system.notice.titleLabel')" name="title">
            <a-input v-model:value="form.title" :maxlength="128" show-count :placeholder="$t('system.notice.titlePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="8">
          <a-form-item :label="$t('system.notice.type')" name="type">
            <a-select v-model:value="form.type" :options="toOptions(dict.sys_notice_type)" :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="8">
          <a-form-item :label="$t('system.notice.scope')" name="scopeType">
            <a-radio-group v-model:value="form.scopeType" button-style="solid" @change="form.targetIds = []">
              <a-radio-button v-for="o in dict.sys_notice_scope" :key="o.value" :value="o.value">{{ o.label }}</a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="16">
          <a-form-item v-if="form.scopeType === NoticeScope.DEPT" :label="$t('system.notice.depts')" name="targetIds" :extra="$t('system.notice.deptsTip')">
            <a-tree-select
              v-model:value="form.targetIds"
              :tree-data="options.depts"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              :placeholder="$t('system.notice.selectDepts')"
              multiple
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              allow-clear
            />
          </a-form-item>
          <a-form-item v-else-if="form.scopeType === NoticeScope.ROLE" :label="$t('system.notice.roles')" name="targetIds">
            <a-select
              v-model:value="form.targetIds"
              mode="multiple"
              :options="options.roles.map((r) => ({ label: r.name, value: r.id }))"
              :placeholder="$t('system.notice.selectRoles')"
              option-filter-prop="label"
              allow-clear
            />
          </a-form-item>
          <a-form-item v-else :label="$t('system.notice.audience')">
            <span class="hint">{{ $t('system.notice.allTip') }}</span>
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="8">
          <a-form-item :label="$t('system.notice.expireDate')" name="expireDate" :extra="$t('system.notice.expireTip')">
            <a-date-picker v-model:value="form.expireDate" value-format="YYYY-MM-DD" :disabled-date="beforeToday" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :md="8">
          <a-form-item :label="$t('system.notice.pinned')" name="pinned" :extra="$t('system.notice.pinnedTip')">
            <a-switch v-model:checked="form.pinned" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item name="content">
            <template #label>
              <span>{{ $t('system.notice.content') }}</span>
              <span class="label-hint">{{ $t('system.notice.markdownTip') }}</span>
            </template>
            <a-tabs v-model:active-key="editorTab" size="small" class="editor-tabs">
              <a-tab-pane key="edit" :tab="$t('common.action.edit')">
                <a-textarea
                  v-model:value="form.content"
                  :auto-size="{ minRows: 12, maxRows: 30 }"
                  :maxlength="50000"
                  :placeholder="$t('system.notice.contentPh')"
                  class="editor"
                />
              </a-tab-pane>
              <a-tab-pane key="preview" :tab="$t('common.action.preview')">
                <div class="preview">
                  <MarkdownView v-if="form.content" :content="form.content" />
                  <span v-else class="hint">{{ $t('system.notice.noContent') }}</span>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('system.notice.attachments')">
            <AttachmentPanel v-if="form.id" biz-type="sys_notice" :biz-id="form.id" editable />
            <span v-else class="hint">{{ $t('system.notice.attachAfterSave') }}</span>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import dayjs from 'dayjs'
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MarkdownView from '@/components/ai/MarkdownView.vue'
import { addNotice, getNotice, noticeOptions, updateNotice } from '@/api/system/notice'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { NoticeScope } from '@/constants/enums'

/** 新增 / 编辑公告（草稿或已撤回的）：保存后为草稿，在列表中发布；附件在保存后上传 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('sys_notice_type', 'sys_notice_scope')
const editorTab = ref('edit')
const options = reactive({ depts: [], roles: [] })

const beforeToday = (d) => d && d.isBefore(dayjs().startOf('day'))

const rules = {
  title: [{ required: true, whitespace: true, message: t('system.notice.inputTitle') }],
  type: [{ required: true, message: t('system.notice.selectType') }],
  targetIds: [{ required: true, type: 'array', min: 1, message: t('system.notice.selectTargets') }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    title: '',
    type: dict.sys_notice_type?.[0]?.value,
    content: '',
    scopeType: NoticeScope.ALL,
    targetIds: [],
    pinned: false,
    expireDate: undefined
  }),
  beforeOpen: async () => {
    editorTab.value = 'edit'
    Object.assign(options, await noticeOptions())
  },
  fetch: (record) => getNotice(record.id),
  create: addNotice,
  update: updateNotice,
  toPayload: ({ title, type, content, scopeType, targetIds, pinned, expireDate }) => ({
    title,
    type,
    content,
    scopeType,
    targetIds: scopeType === NoticeScope.ALL ? [] : targetIds,
    pinned,
    expireDate: expireDate || null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: (record) => drawer.openEdit(record) })
</script>

<style scoped>
.full {
  width: 100%;
}
.hint {
  color: var(--app-text-3);
}
.label-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--app-text-3);
}
.editor-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.editor {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
}
.preview {
  min-height: 260px;
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
}
</style>
