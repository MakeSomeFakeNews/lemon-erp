<template>
  <SubPage :open="open" :title="report?.title" @close="close">
    <template #extra>
      <a-space v-if="report?.status === AiReportStatus.DONE">
        <a-button @click="copy"><template #icon><CopyOutlined /></template>{{ $t('ai.report.copy') }}</a-button>
        <a-button @click="download"><template #icon><DownloadOutlined /></template>{{ $t('ai.report.download') }}</a-button>
        <a-button @click="print"><template #icon><PrinterOutlined /></template>{{ $t('ai.report.print') }}</a-button>
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <div v-if="report" class="report">
        <div class="meta">
          <a-tag :color="report.periodType === AiReportPeriodType.MONTH ? 'purple' : 'blue'">{{ $t(`ai.report.type.${report.periodType}`) }}</a-tag>
          <span>{{ report.periodStart }} ~ {{ report.periodEnd }}</span>
          <a-divider type="vertical" />
          <a-tooltip v-if="report.auto" :title="$t('ai.report.autoTip')">
            <a-tag color="green">{{ $t('ai.report.auto') }}</a-tag>
          </a-tooltip>
          <span v-else>{{ $t('ai.report.generatedByName', { name: report.userName || '-' }) }}</span>
          <a-divider type="vertical" />
          <span>{{ report.createTime }}</span>
          <template v-if="report.promptTokens != null">
            <a-divider type="vertical" />
            <span>{{ $t('ai.report.tokens', { n: report.promptTokens + (report.completionTokens || 0) }) }}</span>
          </template>
        </div>
        <div class="meta">
          <span class="sub-text">{{ $t('ai.report.sourcesPrefix') }}</span>
          <a-tag v-for="s in report.sourceLabels" :key="s" :bordered="false">{{ s }}</a-tag>
        </div>

        <a-result v-if="report.status === AiReportStatus.GENERATING" :title="$t('ai.report.generating')" :sub-title="$t('ai.report.generatingDesc')">
          <template #icon><LoadingOutlined class="generating-icon" /></template>
        </a-result>
        <a-result v-else-if="report.status === AiReportStatus.FAILED" status="error" :title="$t('ai.report.failed')" :sub-title="report.error" />
        <template v-else>
          <a-alert type="info" show-icon :message="$t('ai.report.disclaimer')" class="disclaimer" />
          <MarkdownView :content="report.content" class="content" />
        </template>

        <a-collapse v-if="report.status !== AiReportStatus.GENERATING" ghost class="snapshot" @change="loadSnapshot">
          <a-collapse-panel key="snapshot" :header="$t('ai.report.snapshot')">
            <div class="sub-text">{{ $t('ai.report.snapshotTip') }}</div>
            <a-spin :spinning="snapshotLoading">
              <pre class="snapshot-json">{{ snapshotText }}</pre>
            </a-spin>
          </a-collapse-panel>
        </a-collapse>
      </div>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CopyOutlined, DownloadOutlined, LoadingOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import MarkdownView from '@/components/ai/MarkdownView.vue'
import { getReport, getReportSnapshot } from '@/api/ai/report'
import { AiReportPeriodType, AiReportStatus } from '@/constants/enums'
import { saveBlob } from '@/utils/excel'
import { renderMarkdown } from '@/utils/markdown'
import { printHtml } from '@/utils/print'

/** 生成中时轮询的间隔 */
const POLL_MS = 3000

const emit = defineEmits(['changed'])
const { t } = useI18n()

const open = ref(false)
const loading = ref(false)
const report = ref(null)
const snapshot = ref(null)
const snapshotLoading = ref(false)
let timer = null

const snapshotText = computed(() => (snapshot.value ? JSON.stringify(snapshot.value, null, 2) : ''))

async function show(id) {
  open.value = true
  report.value = null
  snapshot.value = null
  loading.value = true
  try {
    await reload(id)
  } finally {
    loading.value = false
  }
}

/** 重新读取；生成中时定时刷新，完成后通知列表 */
async function reload(id = report.value?.id) {
  clearTimeout(timer)
  const wasGenerating = report.value?.status === AiReportStatus.GENERATING
  report.value = await getReport(id)
  if (report.value.status === AiReportStatus.GENERATING) {
    timer = setTimeout(() => open.value && reload(id), POLL_MS)
  } else if (wasGenerating) {
    emit('changed')
  }
}

async function loadSnapshot(keys) {
  if (!keys.length || snapshot.value) return
  snapshotLoading.value = true
  try {
    snapshot.value = await getReportSnapshot(report.value.id)
  } finally {
    snapshotLoading.value = false
  }
}

function close() {
  clearTimeout(timer)
  open.value = false
}

/** 导出的 Markdown：标题 + 正文 */
const markdownText = () => `# ${report.value.title}\n\n${report.value.content || ''}\n`

async function copy() {
  await navigator.clipboard.writeText(markdownText())
  message.success(t('ai.report.copied'))
}

function download() {
  saveBlob(new Blob([markdownText()], { type: 'text/markdown;charset=utf-8' }), `${report.value.title}.md`)
}

const escapeHtml = (s) => s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])

function print() {
  const r = report.value
  printHtml(`<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(r.title)}</title><style>
body{font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;line-height:1.8;color:#222;margin:24px 32px}
h1{font-size:22px;margin:0 0 4px}h2{font-size:17px;border-bottom:1px solid #ddd;padding-bottom:4px;margin-top:20px}
h3{font-size:15px}table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:3px 8px}.meta{color:#888;font-size:12px}
</style></head><body><h1>${escapeHtml(r.title)}</h1><div class="meta">${escapeHtml(`${r.periodStart} ~ ${r.periodEnd} · ${r.createTime || ''}`)}</div>
${renderMarkdown(r.content)}<p class="meta">${escapeHtml(t('ai.report.disclaimer'))}</p></body></html>`)
}

onBeforeUnmount(() => clearTimeout(timer))

defineExpose({ open: show, reload })
</script>

<style scoped>
.report {
  max-width: 960px;
}
.meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 0;
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.disclaimer {
  margin: 8px 0 16px;
}
.content {
  font-size: 14px;
}
.generating-icon {
  color: var(--app-primary);
  font-size: 48px;
}
.snapshot {
  margin-top: 24px;
  border-top: 1px solid var(--app-border);
}
.snapshot-json {
  max-height: 480px;
  overflow: auto;
  margin-top: 8px;
  padding: 10px 12px;
  background: var(--app-code-bg);
  border-radius: 6px;
  font-size: 12px;
}
</style>
