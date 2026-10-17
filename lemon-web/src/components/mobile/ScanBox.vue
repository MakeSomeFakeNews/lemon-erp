<template>
  <div class="scan-box">
    <a-input
      ref="inputRef"
      v-model:value="code"
      size="large"
      :placeholder="placeholder"
      allow-clear
      autocomplete="off"
      enterkeyhint="done"
      @press-enter="submit"
    >
      <template #prefix><ScanOutlined /></template>
    </a-input>
    <a-button size="large" type="primary" @click="submit">{{ $t('common.action.confirm') }}</a-button>
  </div>
  <div v-if="tip" class="scan-tip" :class="tipType">{{ tip }}</div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ScanOutlined } from '@ant-design/icons-vue'
import { feedback } from './mobile'
import { t } from '@/locales'

/**
 * 扫码输入框：PDA 扫码枪按键盘输入并回车，手机可手工输入。触发 scan(code)，处理后保持焦点便于连续扫码。
 * 页面通过 ref 调用 show(text, ok) 显示处理结果
 */
defineProps({ placeholder: { type: String, default: () => t('mobile.scan.placeholder') } })
const emit = defineEmits(['scan'])

const inputRef = ref()
const code = ref('')
const tip = ref('')
const tipType = ref('ok')

function focus() {
  nextTick(() => inputRef.value?.focus())
}
function submit() {
  const value = (code.value || '').trim()
  code.value = ''
  focus()
  if (value) emit('scan', value)
}
function show(text, ok = true) {
  tip.value = text
  tipType.value = ok ? 'ok' : 'error'
  feedback(ok)
}
onMounted(focus)
defineExpose({ focus, show })
</script>

<style scoped>
.scan-box {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.scan-tip {
  margin-top: 6px;
  font-size: 13px;
}
.scan-tip.ok {
  color: #52c41a;
}
.scan-tip.error {
  color: #ff4d4f;
}
</style>
