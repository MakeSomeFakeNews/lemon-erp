<template>
  <a-modal
    v-model:open="open"
    :title="$t('ai.report.generateTitle')"
    :ok-text="$t('ai.report.generate')"
    :ok-button-props="{ disabled: !sources.length }"
    :confirm-loading="submitting"
    destroy-on-close
    @ok="submit"
  >
    <a-form layout="vertical" class="generate-form">
      <a-form-item :label="$t('ai.report.periodType')">
        <a-radio-group v-model:value="form.periodType" button-style="solid" @change="resetDate">
          <a-radio-button :value="AiReportPeriodType.WEEK">{{ $t('ai.report.type.WEEK') }}</a-radio-button>
          <a-radio-button :value="AiReportPeriodType.MONTH">{{ $t('ai.report.type.MONTH') }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item :label="$t('ai.report.period')" :extra="$t('ai.report.periodRange', { range: rangeText })">
        <a-date-picker
          v-model:value="form.date"
          :picker="form.periodType === AiReportPeriodType.WEEK ? 'week' : 'month'"
          :disabled-date="disabledDate"
          :allow-clear="false"
          class="date-picker"
        />
      </a-form-item>
      <a-form-item :label="$t('ai.report.sources')">
        <a-spin :spinning="loadingSources">
          <div v-if="sources.length" class="sources">
            <a-tooltip v-for="s in sources" :key="s.key" :title="s.allData ? undefined : $t('ai.report.limitedScopeTip')">
              <a-tag :color="s.allData ? 'blue' : 'orange'">
                {{ s.allData ? s.label : $t('ai.report.limitedTag', { label: s.label }) }}
              </a-tag>
            </a-tooltip>
          </div>
          <a-alert v-else-if="!loadingSources" type="warning" show-icon :message="$t('ai.report.sourcesEmpty')" />
        </a-spin>
      </a-form-item>
    </a-form>
    <a-alert type="info" show-icon :message="$t('ai.report.generateTip')" />
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { generateReport, listReportSources } from '@/api/ai/report'
import { AiReportPeriodType } from '@/constants/enums'

const emit = defineEmits(['success'])
const { t } = useI18n()

const open = ref(false)
const submitting = ref(false)
const loadingSources = ref(false)
const sources = ref([])
const form = reactive({ periodType: AiReportPeriodType.WEEK, date: null })

/** 选中日期所在的周（周一至周日，与后端一致）或月，周选择器按界面语言可能从周日开始，取所选周的中间一天 */
const period = computed(() => {
  const d = form.date
  if (!d) return null
  if (form.periodType === AiReportPeriodType.MONTH) {
    return { start: d.startOf('month'), end: d.endOf('month') }
  }
  const mid = d.startOf('week').add(3, 'day')
  const monday = mid.subtract((mid.day() + 6) % 7, 'day')
  return { start: monday, end: monday.add(6, 'day'), mid }
})
const rangeText = computed(() => (period.value ? `${period.value.start.format('YYYY-MM-DD')} ~ ${period.value.end.format('YYYY-MM-DD')}` : ''))

/** 默认上一个完整的周 / 月 */
function resetDate() {
  form.date = form.periodType === AiReportPeriodType.WEEK ? dayjs().subtract(1, 'week') : dayjs().subtract(1, 'month')
}

const disabledDate = (d) => d.isAfter(dayjs(), 'day')

async function show() {
  form.periodType = AiReportPeriodType.WEEK
  resetDate()
  open.value = true
  loadingSources.value = true
  try {
    sources.value = await listReportSources()
  } finally {
    loadingSources.value = false
  }
}

async function submit() {
  const p = period.value
  if (!p) return
  submitting.value = true
  try {
    const date = (p.mid || p.start).format('YYYY-MM-DD')
    const id = await generateReport({ periodType: form.periodType, date })
    message.success(t('ai.report.started'))
    open.value = false
    emit('success', id)
  } finally {
    submitting.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.generate-form {
  margin-top: 8px;
}
.date-picker {
  width: 240px;
}
.sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
}
</style>
