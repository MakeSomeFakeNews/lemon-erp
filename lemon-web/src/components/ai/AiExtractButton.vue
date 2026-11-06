<template>
  <template v-if="visible">
    <a-button @click="openModal">
      <template #icon><ScanOutlined /></template>{{ $t('ai.extract.button') }}
    </a-button>
    <a-modal
      v-model:open="open"
      :title="$t('ai.extract.modalTitle', { name: title || '' })"
      width="min(760px, 100vw)"
      :mask-closable="false"
      destroy-on-close
    >
      <template v-if="!result">
        <a-alert type="info" show-icon class="tip" :message="$t('ai.extract.modalTip')" />
        <AiExtractInput ref="inputRef" :disabled="running" />
        <AiExtractProgress v-if="running" :started="startedAt" :vision="hasImages" class="progress" />
      </template>
      <AiExtractPreview v-else :result="result" />
      <template #footer>
        <a-button v-if="result" @click="result = null">{{ $t('ai.extract.again') }}</a-button>
        <a-button v-else @click="open = false">{{ $t('common.action.cancel') }}</a-button>
        <a-button v-if="result" type="primary" @click="fill">{{ $t('ai.extract.fill') }}</a-button>
        <a-button v-else type="primary" :loading="running" :disabled="inputRef?.parsing" @click="run">{{ $t('ai.extract.start') }}</a-button>
      </template>
    </a-modal>
  </template>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ScanOutlined } from '@ant-design/icons-vue'
import AiExtractInput from './AiExtractInput.vue'
import AiExtractPreview from './AiExtractPreview.vue'
import AiExtractProgress from './AiExtractProgress.vue'
import { runExtract } from '@/api/ai/extract'
import { useAiExtractEnabled } from '@/composables/useAiPrefill'
import { useUserStore } from '@/store/user'

/**
 * 业务页面工具栏上的「AI 识别」：在当前页面上传 / 粘贴单据，识别后触发 result（页面用 useAiPrefill 的 apply 打开新增表单预填）。
 * 有 ai:extract:use 和 perm（目标单据的新增权限）且租户开启了智能录入时才显示
 */
const props = defineProps({
  target: { type: String, required: true },
  perm: { type: String, required: true },
  title: String
})
const emit = defineEmits(['result'])
const { t } = useI18n()
const userStore = useUserStore()
const enabled = useAiExtractEnabled()
const visible = computed(() => enabled.value && userStore.hasPerm(props.perm))

const open = ref(false)
const inputRef = ref()
const running = ref(false)
const startedAt = ref(0)
const hasImages = ref(false)
const result = ref(null)

function openModal() {
  result.value = null
  open.value = true
}

async function run() {
  const input = inputRef.value
  if (!input || input.isEmpty) {
    message.warning(t('ai.extract.empty'))
    return
  }
  const { text, files } = input.collect()
  hasImages.value = files.some((f) => f.type.startsWith('image/'))
  running.value = true
  startedAt.value = Date.now()
  try {
    const res = await runExtract({ target: props.target, text, files })
    if (open.value) result.value = res
  } finally {
    running.value = false
  }
}

function fill() {
  emit('result', result.value)
  open.value = false
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.progress {
  margin-top: 12px;
}
</style>
