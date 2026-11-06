<template>
  <div class="ai-progress">
    <a-steps size="small" :current="step" :items="steps" />
    <div class="line">
      <LoadingOutlined />
      <span>{{ $t(step < 1 ? 'ai.extract.stepUploading' : 'ai.extract.stepRecognizing') }}</span>
      <span class="sub">{{ $t('ai.extract.elapsed', { s: seconds }) }}{{ vision ? $t('ai.extract.visionSlow') : '' }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { LoadingOutlined } from '@ant-design/icons-vue'

/**
 * 识别进度：后端是一次请求，按耗时显示「上传 → 识别 → 匹配」的大致阶段和已用时间
 */
const props = defineProps({
  started: { type: Number, required: true },
  vision: Boolean
})
const { t } = useI18n()
const now = ref(Date.now())
let timer
onMounted(() => (timer = setInterval(() => (now.value = Date.now()), 500)))
onBeforeUnmount(() => clearInterval(timer))

const seconds = computed(() => Math.max(0, Math.floor((now.value - props.started) / 1000)))
const step = computed(() => (seconds.value < 1 ? 0 : 1))
const steps = [{ title: t('ai.extract.stepUpload') }, { title: t('ai.extract.stepRecognize') }, { title: t('ai.extract.stepMatch') }]
</script>

<style scoped>
.ai-progress {
  padding: 12px;
  border-radius: 6px;
  background: var(--app-bg-2, rgba(0, 0, 0, 0.02));
}
.line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
