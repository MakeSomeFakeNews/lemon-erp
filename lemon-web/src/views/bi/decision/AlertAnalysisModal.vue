<template>
  <a-modal v-model:open="open" :title="$t('ai.report.alertTitle', { name: alert?.name })" :width="720" :footer="null" destroy-on-close>
    <div class="analysis">
      <div v-if="loading" class="loading">
        <a-spin />
        <span>{{ $t('ai.report.alertLoading') }}</span>
      </div>
      <template v-else-if="result">
        <MarkdownView :content="result.content" />
        <div class="foot">
          <span class="sub-text">
            {{ result.sourceLabels.length ? $t('ai.report.alertSources', { sources: result.sourceLabels.join(' / ') }) : $t('ai.report.alertNoSources') }}
          </span>
          <a-button size="small" @click="analyze"><template #icon><ReloadOutlined /></template>{{ $t('ai.report.reanalyze') }}</a-button>
        </div>
        <div class="sub-text disclaimer">{{ $t('ai.report.alertDisclaimer') }}</div>
      </template>
    </div>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import MarkdownView from '@/components/ai/MarkdownView.vue'
import { analyzeAlert } from '@/api/ai/report'

/** 经营预警的 AI 分析（结果不入库，同一条预警在页面打开期间保留上次的分析） */
const open = ref(false)
const loading = ref(false)
const alert = ref(null)
const result = ref(null)
const cache = new Map()

async function analyze() {
  loading.value = true
  try {
    result.value = await analyzeAlert(alert.value.ruleId)
    cache.set(alert.value.ruleId, result.value)
  } catch {
    open.value = false
  } finally {
    loading.value = false
  }
}

function show(a) {
  alert.value = a
  result.value = cache.get(a.ruleId) || null
  open.value = true
  if (!result.value) analyze()
}

defineExpose({ open: show })
</script>

<style scoped>
.analysis {
  min-height: 160px;
}
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: var(--app-text-2);
}
.foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 10px;
  border-top: 1px solid var(--app-border);
}
.disclaimer {
  margin-top: 6px;
  font-size: 12px;
}
</style>
