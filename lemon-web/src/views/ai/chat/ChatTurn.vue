<template>
  <div class="turn">
    <div class="avatar"><RobotOutlined /></div>
    <div class="body">
      <!-- 查询过程 -->
      <a-collapse v-if="turn.steps.length" v-model:active-key="processOpen" ghost size="small" class="process">
        <a-collapse-panel key="p" :header="$t('ai.chat.process', { n: turn.steps.length })">
          <div v-for="s in turn.steps" :key="s.key" class="step">
            <div class="step-line">
              <LoadingOutlined v-if="s.running" class="icon running" />
              <CloseCircleOutlined v-else-if="s.error" class="icon failed" />
              <CheckCircleOutlined v-else class="icon ok" />
              <span class="step-label">{{ s.label || s.tool }}</span>
              <a v-if="!s.running && s.messageId" class="toggle" @click="$emit('toggle-data', s)">
                {{ s.open ? $t('ai.chat.hideData') : $t('ai.chat.viewData') }}
              </a>
            </div>
            <div v-if="s.arguments && s.arguments !== '{}'" class="args">
              <span class="sub-text">{{ $t('ai.chat.arguments') }}</span>
              <code>{{ s.arguments }}</code>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>

      <!-- 工具数据 -->
      <template v-for="s in turn.steps" :key="`d-${s.key}`">
        <div v-if="s.open" class="data-card">
          <div class="data-title">{{ s.label || s.tool }}</div>
          <a-spin :spinning="!!s.loading">
            <ToolDataTable :data="s.data" :total="s.total" :truncated="s.truncated" :error="s.error" />
          </a-spin>
        </div>
      </template>

      <!-- 回答 -->
      <div v-if="turn.content" class="markdown" v-html="html"></div>
      <div v-else-if="turn.streaming && !runningStep" class="thinking"><LoadingOutlined /> {{ $t('ai.chat.thinking') }}</div>
      <div v-else-if="runningStep" class="thinking"><LoadingOutlined /> {{ runningStep.status }}</div>
      <a-alert v-if="turn.error" type="error" show-icon :message="turn.error" class="error" />
      <div v-if="turn.cancelled" class="sub-text">{{ $t('ai.chat.stopped') }}</div>
      <div v-if="!turn.streaming && turn.tokens" class="meta sub-text">
        {{ $t('ai.chat.tokens', { n: turn.tokens }) }}<span v-if="turn.estimated">{{ $t('ai.chat.estimated') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import ToolDataTable from './ToolDataTable.vue'

/**
 * 一次回答：查询过程（可折叠）、工具数据表格、Markdown 渲染的回答（经 DOMPurify 清理）
 *
 * turn: { content, steps: [{ key, tool, label, status, arguments, messageId, data, total, truncated, error, running, open, loading }],
 *         streaming, error, cancelled, tokens, estimated }
 */
const props = defineProps({ turn: { type: Object, required: true } })
defineEmits(['toggle-data'])

const html = computed(() => renderMarkdown(props.turn.content))
const runningStep = computed(() => props.turn.steps.find((s) => s.running))
// 回答过程中展开查询过程，结束后默认收起
const processOpen = ref(props.turn.streaming ? ['p'] : [])
watch(
  () => props.turn.streaming,
  (v) => {
    if (!v) processOpen.value = []
  }
)
</script>

<style scoped>
.turn {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.avatar {
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-bg);
  color: var(--app-primary);
  font-size: 16px;
}
.body {
  flex: 1;
  min-width: 0;
  padding-top: 4px;
}
.process {
  margin-bottom: 4px;
  background: var(--app-hover);
  border-radius: 8px;
}
.process :deep(.ant-collapse-header) {
  padding: 6px 12px !important;
  color: var(--app-text-2);
  font-size: 13px;
}
.process :deep(.ant-collapse-content-box) {
  padding: 0 12px 8px !important;
}
.step + .step {
  margin-top: 6px;
}
.step-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.icon.ok {
  color: var(--ant-color-success, #52c41a);
}
.icon.failed {
  color: var(--ant-color-error, #ff4d4f);
}
.icon.running {
  color: var(--app-primary);
}
.toggle {
  margin-left: auto;
  font-size: 12px;
}
.args {
  margin: 2px 0 0 20px;
  font-size: 12px;
  word-break: break-all;
}
.args code {
  margin-left: 6px;
  padding: 1px 4px;
  background: var(--app-code-bg);
  border-radius: 4px;
}
.data-card {
  margin: 8px 0;
  padding: 8px 12px 10px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.data-title {
  font-weight: 500;
  font-size: 13px;
}
.thinking {
  color: var(--app-text-3);
  padding: 4px 0;
}
.error {
  margin-top: 8px;
}
.meta {
  margin-top: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.markdown {
  line-height: 1.75;
  word-break: break-word;
}
.markdown :deep(p) {
  margin: 0 0 8px;
}
.markdown :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 22px;
  margin: 0 0 8px;
}
.markdown :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  display: block;
  overflow-x: auto;
}
.markdown :deep(th),
.markdown :deep(td) {
  border: 1px solid var(--app-border);
  padding: 4px 10px;
}
.markdown :deep(th) {
  background: var(--app-hover);
}
.markdown :deep(code) {
  padding: 1px 4px;
  background: var(--app-code-bg);
  border-radius: 4px;
  font-size: 13px;
}
.markdown :deep(pre) {
  padding: 10px 12px;
  background: var(--app-code-bg);
  border-radius: 6px;
  overflow-x: auto;
}
.markdown :deep(pre code) {
  padding: 0;
  background: none;
}
.markdown :deep(blockquote) {
  margin: 0 0 8px;
  padding-left: 10px;
  border-left: 3px solid var(--app-border);
  color: var(--app-text-2);
}
</style>
