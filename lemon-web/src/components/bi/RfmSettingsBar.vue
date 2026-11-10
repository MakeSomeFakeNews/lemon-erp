<template>
  <div class="rfm-settings">
    <a-segmented v-model:value="form.mode" :options="modeOptions" @change="apply" />
    <template v-if="form.mode === RfmMode.THRESHOLD">
      <span class="item">
        <span class="label">{{ $t('jxc.rfm.settings.rLabel') }}</span>
        <a-input-number v-model:value="form.recencyDays" :min="1" :max="3650" :precision="0" size="small" class="num" @change="applyLater" />
        <span class="unit">{{ $t('jxc.rfm.settings.rUnit') }}</span>
      </span>
      <span class="item">
        <span class="label">{{ $t('jxc.rfm.settings.fLabel') }}</span>
        <a-input-number v-model:value="form.frequency" :min="0" :max="1000" :precision="2" :step="0.5" size="small" class="num" @change="applyLater" />
        <span class="unit">{{ $t('jxc.rfm.settings.fUnit') }}</span>
      </span>
      <span class="item">
        <span class="label">{{ $t('jxc.rfm.settings.mLabel') }}</span>
        <a-input-number v-model:value="form.monetary" :min="0" :max="100000000" :precision="2" :step="100" size="small" class="num-l" @change="applyLater" />
        <span class="unit">{{ $t('jxc.rfm.settings.mUnit') }}</span>
      </span>
    </template>
    <span v-else class="hint">{{ $t('jxc.rfm.settings.averageHint') }}</span>
    <span class="actions">
      <a-tooltip v-if="canSave" :title="$t('jxc.rfm.settings.saveDefaultTip')">
        <a-button size="small" :loading="saving" @click="saveDefault">{{ $t('jxc.rfm.settings.saveDefault') }}</a-button>
      </a-tooltip>
      <a @click="emit('apply', {})">{{ $t('jxc.rfm.settings.reset') }}</a>
    </span>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { saveBizRules } from '@/api/system/customField'
import { clearBizRules } from '@/composables/useBizRules'
import { RfmMode } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { t } from '@/locales'

/**
 * RFM 分群参数（进销存报表、客户关系的客户分析共用）：分群方式、R 天数、F 次 / 月、M 元 / 次，修改后立即重新查询。
 * - settings：后端返回的实际使用的参数（{ mode, recencyDays, frequency, monetary }），用于回显
 * - @apply(params)：参数变化时触发，父页面带上 params 重新查询；「恢复默认」传 {}（取本公司默认值）
 * 有业务参数编辑权限时可「保存为默认」（写入业务参数 scm.rfm.*，按租户）
 */
const props = defineProps({
  settings: { type: Object, default: null }
})
const emit = defineEmits(['apply'])

const modeOptions = [
  { value: RfmMode.THRESHOLD, label: t('jxc.rfm.settings.threshold') },
  { value: RfmMode.AVERAGE, label: t('jxc.rfm.settings.average') }
]
const canSave = useUserStore().hasPerm('sys:bizRule:edit')
const form = reactive({ mode: RfmMode.THRESHOLD, recencyDays: null, frequency: null, monetary: null })

watch(
  () => props.settings,
  (s) => {
    if (!s) return
    Object.assign(form, { mode: s.mode, recencyDays: s.recencyDays, frequency: Number(s.frequency), monetary: Number(s.monetary) })
  },
  { immediate: true }
)

/** 只传填写了的项，清空的项由后端取默认值 */
function params() {
  const p = { mode: form.mode }
  for (const k of ['recencyDays', 'frequency', 'monetary']) {
    if (form[k] != null && form[k] !== '') p[k] = form[k]
  }
  return p
}

let timer
function applyLater() {
  clearTimeout(timer)
  timer = setTimeout(apply, 500)
}
function apply() {
  clearTimeout(timer)
  emit('apply', params())
}

const saving = ref(false)
async function saveDefault() {
  saving.value = true
  try {
    const p = { 'scm.rfm.mode': form.mode }
    if (form.recencyDays != null) p['scm.rfm.recencyDays'] = String(form.recencyDays)
    if (form.frequency != null) p['scm.rfm.frequency'] = String(form.frequency)
    if (form.monetary != null) p['scm.rfm.monetary'] = String(form.monetary)
    await saveBizRules(p)
    clearBizRules()
    message.success(t('jxc.rfm.settings.savedDefault'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.rfm-settings {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
}
.item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.label {
  font-weight: 500;
}
.unit,
.hint {
  color: var(--app-text-3);
  font-size: 12px;
}
.num {
  width: 84px;
}
.num-l {
  width: 120px;
}
.actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}
</style>
