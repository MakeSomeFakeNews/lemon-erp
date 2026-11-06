<template>
  <div class="ai-settings">
    <a-alert type="warning" show-icon class="notice" :message="$t('ai.settings.dataNotice')" :description="$t('ai.settings.dataNoticeDesc')" />

    <div class="layout">
      <a-card :bordered="false" class="page-card form-card">
        <div v-if="loading" class="page-spin"><a-spin /></div>
        <a-form v-else :model="form" layout="vertical" :disabled="!canEdit">
          <div class="section-title">{{ $t('ai.settings.model') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('ai.settings.provider')" required>
                <a-select v-model:value="form.provider" :options="providerOptions" @change="onProviderChange" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('ai.settings.apiKey')" :required="form.enabled" :extra="$t('ai.settings.apiKeyTip')">
                <a-input-password
                  v-model:value="form.apiKey"
                  :placeholder="saved.apiKeySet ? $t('ai.settings.apiKeySet', { mask: saved.apiKeyMask }) : 'sk-...'"
                  autocomplete="new-password"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item :label="$t('ai.settings.baseUrl')" :required="form.provider === AiProvider.CUSTOM" :extra="$t('ai.settings.baseUrlTip')">
            <a-input v-model:value="form.baseUrl" :placeholder="defaultBaseUrl || 'https://'" allow-clear />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('ai.settings.chatModel')" :required="form.enabled" :extra="$t('ai.settings.chatModelTip')">
                <a-input v-model:value="form.chatModel" :placeholder="example('chat')" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('ai.settings.visionModel')" :extra="$t('ai.settings.visionModelTip')">
                <a-input v-model:value="form.visionModel" :placeholder="example('vision')" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="section-title">{{ $t('ai.settings.params') }}</div>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ai.settings.temperature')" :extra="$t('ai.settings.temperatureTip')">
                <a-input-number v-model:value="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ai.settings.maxTokens')" :extra="$t('ai.settings.maxTokensTip')">
                <a-input-number v-model:value="form.maxTokens" :min="1" :max="65536" :precision="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ai.settings.monthlyLimit')" :extra="$t('ai.settings.monthlyLimitTip')">
                <a-input-number
                  v-model:value="form.monthlyTokenLimit"
                  :min="1"
                  :precision="0"
                  :placeholder="$t('ai.settings.unlimited')"
                  class="full"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="section-title">{{ $t('ai.settings.features') }}</div>
          <div class="feature master">
            <a-switch :checked="form.enabled" @change="(v) => toggle('enabled', v)" />
            <div>
              <div class="feature-name">{{ $t('ai.settings.enabled') }}</div>
              <div class="sub-text">{{ $t('ai.settings.enabledTip') }}</div>
            </div>
          </div>
          <div v-for="f in FEATURES" :key="f.field" class="feature">
            <a-tooltip :title="form.enabled ? '' : $t('ai.settings.featureOffWhenDisabled')">
              <a-switch size="small" :checked="form[f.field]" :disabled="!form.enabled || !canEdit" @change="(v) => toggle(f.field, v)" />
            </a-tooltip>
            <div>
              <div class="feature-name">
                {{ $t(`ai.feature.${f.feature}`) }}
              </div>
              <div class="sub-text">{{ $t(`ai.featureDesc.${f.feature}`) }}</div>
            </div>
          </div>

          <div v-if="canEdit" class="actions">
            <a-button type="primary" :loading="saving" @click="save">{{ $t('common.action.save') }}</a-button>
            <a-tooltip :title="$t('ai.settings.saveFirst')">
              <a-button :loading="testing" @click="test">{{ $t('ai.settings.testConnection') }}</a-button>
            </a-tooltip>
          </div>
          <a-alert
            v-if="testResult"
            class="test-result"
            show-icon
            :type="testResult.ok ? 'success' : 'error'"
            :message="testResult.ok ? $t('ai.settings.testOk') : $t('ai.settings.testFail')"
          >
            <template #description>
              <template v-if="testResult.ok">
                <div>{{ $t('ai.settings.testReply', { reply: testResult.reply }) }}</div>
                <div class="sub-text">{{ $t('ai.settings.testMeta', { model: testResult.model, ms: testResult.latencyMs, tokens: testResult.tokens }) }}</div>
              </template>
              <template v-else>{{ testResult.error }}</template>
            </template>
          </a-alert>
        </a-form>
      </a-card>

      <UsagePanel ref="usageRef" class="usage" />
    </div>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { getAiConfig, listAiProviders, saveAiConfig, testAiConfig } from '@/api/ai/config'
import { useUserStore } from '@/store/user'
import { AiFeature, AiProvider } from '@/constants/enums'
import UsagePanel from './UsagePanel.vue'

/** AI 设置：按租户配置国产大模型（OpenAI 兼容协议）、功能开关、月度上限；API Key 只写不读 */
const { t } = useI18n()
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('ai:config:edit'))

/** 常见模型名称，只作为输入框的占位提示（以服务商控制台为准） */
const MODEL_EXAMPLES = {
  DEEPSEEK: { chat: 'deepseek-chat', vision: '' },
  QWEN: { chat: 'qwen-plus', vision: 'qwen-vl-plus' },
  ZHIPU: { chat: 'glm-4-plus', vision: 'glm-4v-plus' },
  MOONSHOT: { chat: 'kimi-k2-turbo-preview', vision: 'moonshot-v1-8k-vision-preview' },
  DOUBAO: { chat: 'doubao-seed-1-6', vision: 'doubao-1-5-vision-pro-32k' },
  CUSTOM: { chat: 'qwen2.5-72b-instruct', vision: 'qwen2.5-vl-72b-instruct' }
}
const FEATURES = [
  { field: 'chatEnabled', feature: AiFeature.CHAT },
  { field: 'extractEnabled', feature: AiFeature.EXTRACT },
  { field: 'cleanseEnabled', feature: AiFeature.CLEANSE },
  { field: 'reportEnabled', feature: AiFeature.REPORT }
]

const loading = ref(true)
const providers = ref([])
const saved = ref({})
const form = reactive({})
const providerOptions = computed(() => providers.value.map((p) => ({ value: p.value, label: t(`ai.provider.${p.value}`) })))
const defaultBaseUrl = computed(() => providers.value.find((p) => p.value === form.provider)?.defaultBaseUrl)

function example(kind) {
  const name = MODEL_EXAMPLES[form.provider]?.[kind]
  return name ? t('ai.settings.example', { example: name }) : ''
}

function resetForm(c) {
  Object.assign(form, {
    provider: c.provider || AiProvider.DEEPSEEK,
    baseUrl: c.baseUrl,
    apiKey: '',
    chatModel: c.chatModel,
    visionModel: c.visionModel,
    enabled: !!c.enabled,
    chatEnabled: !!c.chatEnabled,
    extractEnabled: !!c.extractEnabled,
    cleanseEnabled: !!c.cleanseEnabled,
    reportEnabled: !!c.reportEnabled,
    temperature: c.temperature ?? 0.3,
    maxTokens: c.maxTokens,
    monthlyTokenLimit: c.monthlyTokenLimit
  })
}

async function load() {
  loading.value = true
  try {
    const [list, config] = await Promise.all([listAiProviders(), getAiConfig()])
    providers.value = list
    saved.value = config
    resetForm(config)
  } finally {
    loading.value = false
  }
}

function onProviderChange() {
  // 换服务商时清空自定义的接口地址，使用新服务商的默认地址
  form.baseUrl = ''
}

/** 打开开关前醒目提示业务数据会发送给第三方模型服务，确认后才打开 */
function toggle(field, value) {
  if (!value) {
    form[field] = false
    return
  }
  const provider = t(`ai.provider.${form.provider}`)
  const content =
    field === 'enabled'
      ? t('ai.settings.confirmContentAll', { provider })
      : t('ai.settings.confirmContent', { feature: t(`ai.feature.${FEATURES.find((f) => f.field === field).feature}`), provider })
  Modal.confirm({
    title: t('ai.settings.confirmTitle'),
    content: h('div', [
      h('p', { class: 'confirm-warn' }, t('ai.settings.dataNotice')),
      h('p', content)
    ]),
    okText: t('ai.settings.confirmOk'),
    okType: 'danger',
    onOk: () => {
      form[field] = true
    }
  })
}

const saving = ref(false)
async function save() {
  saving.value = true
  try {
    await saveAiConfig({ ...form })
    message.success(t('common.msg.saved'))
    await load()
  } finally {
    saving.value = false
  }
}

const testing = ref(false)
const testResult = ref(null)
const usageRef = ref()
async function test() {
  testing.value = true
  testResult.value = null
  try {
    const r = await testAiConfig({ ...form })
    testResult.value = { ok: true, ...r, tokens: r.promptTokens + r.completionTokens }
    usageRef.value?.reload()
  } catch (e) {
    testResult.value = { ok: false, error: e.message }
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ai-settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.notice {
  flex: none;
}
.layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.form-card {
  flex: 1;
  min-width: 0;
  margin: 0;
}
.form-card :deep(.ant-card-body) {
  max-width: 820px;
}
.usage {
  width: 400px;
  flex: none;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
  line-height: 1.2;
}
.full {
  width: 100%;
}
.feature {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0 10px 4px;
}
.feature + .feature {
  border-top: 1px dashed var(--app-border);
}
.feature.master {
  padding-left: 0;
}
.feature-name {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
.test-result {
  margin-top: 16px;
}
.page-spin {
  padding: 48px 0;
  text-align: center;
}
@media (max-width: 1200px) {
  .layout {
    flex-direction: column;
    align-items: stretch;
  }
  .usage {
    width: 100%;
  }
}
</style>

<style>
.confirm-warn {
  color: var(--ant-color-error, #ff4d4f);
  font-weight: 600;
}
</style>
