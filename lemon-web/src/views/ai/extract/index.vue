<template>
  <div class="ai-extract">
    <!-- 未配置 / 未开启时的引导 -->
    <a-card v-if="status && !status.features?.EXTRACT" :bordered="false" class="page-card">
      <a-result :title="$t('ai.extract.notAvailable')" :sub-title="status.configured ? $t('ai.extract.disabledDesc') : $t('ai.extract.notConfiguredDesc')">
        <template #icon><ScanOutlined class="guide-icon" /></template>
        <template #extra>
          <a-button v-if="canConfig" type="primary" @click="router.push('/ai/settings')">{{ $t('ai.chat.goSettings') }}</a-button>
          <span v-else class="sub-text">{{ $t('ai.chat.contactAdmin') }}</span>
        </template>
      </a-result>
    </a-card>

    <a-row v-else-if="status" :gutter="16">
      <a-col :xs="24" :lg="10">
        <a-card :bordered="false" class="page-card" :title="$t('ai.extract.inputTitle')">
          <a-alert v-if="!status.vision" type="info" show-icon class="block" :message="$t('ai.extract.noVision')" />
          <a-empty v-if="targetsLoaded && !targets.length" :description="$t('ai.extract.noTargets')" />
          <template v-else>
            <div class="field-label">{{ $t('ai.extract.target') }}</div>
            <a-select v-model:value="target" :options="targetOptions" :disabled="running" class="target-select" />
            <AiExtractInput ref="inputRef" :rows="6" :disabled="running" />
            <AiExtractProgress v-if="running" :started="startedAt" :vision="hasImages" class="block-top" />
            <div class="actions">
              <a-button :disabled="running" @click="resetAll">{{ $t('common.action.reset') }}</a-button>
              <a-button type="primary" :loading="running" :disabled="inputRef?.parsing" @click="start">
                <template #icon><ScanOutlined /></template>{{ $t('ai.extract.start') }}
              </a-button>
            </div>
            <div class="sub-text privacy">{{ $t('ai.extract.privacy') }}</div>
          </template>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="14">
        <a-card :bordered="false" class="page-card result-card" :title="$t('ai.extract.resultTitle')">
          <!-- 自动判断的候选 -->
          <template v-if="candidates.length && !result">
            <div class="field-label">{{ $t('ai.extract.candidatesTitle') }}</div>
            <a-radio-group v-model:value="chosen" class="candidates">
              <a-radio v-for="c in candidates" :key="c.key" :value="c.key" class="candidate">
                <b>{{ c.label }}</b>
                <a-tag :bordered="false" class="conf">{{ $t('ai.extract.confidence', { p: Math.round(Number(c.confidence) * 100) }) }}</a-tag>
                <div v-if="c.reason" class="sub-text">{{ c.reason }}</div>
              </a-radio>
            </a-radio-group>
            <div class="actions">
              <a-button type="primary" :loading="running" :disabled="!chosen" @click="extract(chosen)">{{ $t('ai.extract.confirmTarget') }}</a-button>
            </div>
          </template>

          <template v-else-if="result">
            <AiExtractPreview :result="result" />
            <div class="actions">
              <a-button type="primary" @click="toForm">
                <template #icon><FormOutlined /></template>{{ $t('ai.extract.toForm', { name: result.label }) }}
              </a-button>
            </div>
            <div class="sub-text">{{ $t('ai.extract.toFormTip') }}</div>
          </template>

          <a-empty v-else :description="$t('ai.extract.resultEmpty')">
            <template #image><ScanOutlined class="empty-icon" /></template>
          </a-empty>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { FormOutlined, ScanOutlined } from '@ant-design/icons-vue'
import AiExtractInput from '@/components/ai/AiExtractInput.vue'
import AiExtractPreview from '@/components/ai/AiExtractPreview.vue'
import AiExtractProgress from '@/components/ai/AiExtractProgress.vue'
import { detectExtractTarget, listExtractTargets, runExtract } from '@/api/ai/extract'
import { getAiStatus } from '@/api/ai/config'
import { setPendingPrefill } from '@/composables/useAiPrefill'
import { useUserStore } from '@/store/user'

/**
 * 智能录入：选择要生成的单据（或自动识别），上传 / 粘贴单据，识别结果预览后「生成表单」跳转到业务页面打开新增表单预填，
 * 用户核对后在原表单保存（AI 不保存单据）。只列出有新增权限的单据
 */
const AUTO = '__auto__'
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const canConfig = computed(() => userStore.hasPerm('ai:config:list'))

const status = ref(null)
const targets = ref([])
const targetsLoaded = ref(false)
const target = ref(AUTO)
const targetOptions = computed(() => [{ value: AUTO, label: t('ai.extract.auto') }, ...targets.value.map((x) => ({ value: x.key, label: x.label }))])

onMounted(async () => {
  status.value = await getAiStatus()
  if (status.value.features?.EXTRACT) {
    targets.value = await listExtractTargets()
    targetsLoaded.value = true
  }
})

const inputRef = ref()
const running = ref(false)
const startedAt = ref(0)
const hasImages = ref(false)
const candidates = ref([])
const chosen = ref()
const result = ref(null)
let lastInput = null

async function withProgress(fn) {
  running.value = true
  startedAt.value = Date.now()
  try {
    return await fn()
  } finally {
    running.value = false
  }
}

async function start() {
  const input = inputRef.value
  if (!input || input.isEmpty) {
    message.warning(t('ai.extract.empty'))
    return
  }
  lastInput = input.collect()
  hasImages.value = lastInput.files.some((f) => f.type.startsWith('image/'))
  result.value = null
  candidates.value = []
  if (target.value !== AUTO) {
    await extract(target.value)
    return
  }
  const list = await withProgress(() => detectExtractTarget(lastInput))
  candidates.value = list
  chosen.value = list[0]?.key
}

async function extract(key) {
  if (!lastInput) return
  result.value = await withProgress(() => runExtract({ ...lastInput, target: key }))
  target.value = key
}

function resetAll() {
  inputRef.value?.reset()
  result.value = null
  candidates.value = []
  lastInput = null
}

/** 跳转到业务页面并打开新增表单（识别结果只在内存中传递） */
function toForm() {
  const r = result.value
  if (router.resolve(r.route).name === 'NotFound') {
    message.warning(t('ai.extract.noPage', { name: r.label }))
    return
  }
  setPendingPrefill(r)
  router.push({ path: r.route, query: { ai: r.form } })
}
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.block-top {
  margin-top: 12px;
}
.field-label {
  margin-bottom: 6px;
  font-weight: 500;
}
.target-select {
  width: 100%;
  margin-bottom: 12px;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
.privacy {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.candidates {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.candidate {
  padding: 8px 12px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  margin: 0;
}
.conf {
  margin-left: 8px;
}
.guide-icon,
.empty-icon {
  font-size: 56px;
  color: var(--ant-color-primary, #1677ff);
}
.empty-icon {
  font-size: 48px;
  color: var(--app-text-3);
}
@media (max-width: 991px) {
  .result-card {
    margin-top: 16px;
  }
}
</style>
