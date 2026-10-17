<template>
  <a-modal
    :open="open"
    :title="mode === 'self' ? $t('hr.scoreModal.selfTitle') : $t('hr.scoreModal.reviewTitle')"
    :confirm-loading="saving"
    width="min(720px, 100vw)"
    destroy-on-close
    @cancel="emit('update:open', false)"
    @ok="submit"
  >
    <a-spin :spinning="loading">
      <template v-if="appraisal">
        <a-descriptions size="small" :column="3" class="head">
          <a-descriptions-item :label="$t('hr.scoreModal.plan')">{{ appraisal.planName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('hr.scoreModal.appraisee')">{{ appraisal.empNo }} {{ appraisal.employeeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.dept')">{{ appraisal.deptName || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="indicatorId" size="small" :columns="columns" :data-source="rows" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div>{{ record.indicatorName }}</div>
              <div v-if="record.description" class="sub-text">{{ record.description }}</div>
            </template>
            <template v-else-if="column.key === 'weight'">{{ Number(record.weight) }}%</template>
            <template v-else-if="column.key === 'selfScore'">{{ record.selfScore ?? '-' }}</template>
            <template v-else-if="column.key === 'input'">
              <a-input-number v-model:value="record.input" :min="0" :max="100" :precision="1" class="full" />
            </template>
          </template>
        </a-table>
        <div class="total">
          {{ $t('hr.scoreModal.weightedScore') }}<b>{{ total }}</b>
          <a-tag v-if="mode === 'review'" :color="gradeColor" :bordered="false">{{ grade }}</a-tag>
        </div>
        <div v-if="mode === 'review' && appraisal.selfComment" class="self-comment">
          <span class="sub-text">{{ $t('hr.scoreModal.selfSummaryLabel') }}</span>{{ appraisal.selfComment }}
        </div>
        <a-textarea v-model:value="comment" :rows="3" :maxlength="500" :placeholder="mode === 'self' ? $t('hr.scoreModal.selfSummaryPh') : $t('hr.scoreModal.commentPh')" />
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { getAppraisal, reviewAppraisal, selfEvaluate } from '@/api/hr'

/**
 * 绩效自评 / 评分：每个指标 0-100 分，按权重加权；评分时显示员工的自评分
 * - mode：self 自评 / review 评分
 */
const props = defineProps({
  open: Boolean,
  appraisalId: Number,
  mode: { type: String, default: 'review' }
})
const emit = defineEmits(['update:open', 'success'])
const { t } = useI18n()

const appraisal = ref(null)
const rows = ref([])
const comment = ref('')
const loading = ref(false)
const saving = ref(false)

const columns = computed(() => [
  { title: t('hr.scoreModal.indicator'), key: 'name' },
  { title: t('hr.scoreModal.weight'), key: 'weight', width: 90 },
  ...(props.mode === 'review' ? [{ title: t('hr.scoreModal.self'), key: 'selfScore', width: 100 }] : []),
  { title: props.mode === 'self' ? t('hr.scoreModal.selfScore') : t('hr.scoreModal.score'), key: 'input', width: 130 }
])

const total = computed(() => {
  const sum = rows.value.reduce((s, r) => s + (Number(r.input) || 0) * Number(r.weight), 0)
  return (sum / 100).toFixed(1)
})
const grade = computed(() => {
  const s = Number(total.value)
  return s >= 90 ? 'S' : s >= 80 ? 'A' : s >= 70 ? 'B' : s >= 60 ? 'C' : 'D'
})
const gradeColor = computed(() => ({ S: 'gold', A: 'green', B: 'blue', C: 'orange', D: 'red' })[grade.value])

watch(
  () => props.open,
  async (open) => {
    if (!open || !props.appraisalId) return
    loading.value = true
    appraisal.value = null
    try {
      const a = await getAppraisal(props.appraisalId)
      appraisal.value = a
      comment.value = (props.mode === 'self' ? a.selfComment : a.reviewComment) || ''
      rows.value = a.scores.map((s) => ({
        ...s,
        input: props.mode === 'self' ? s.selfScore ?? undefined : s.score ?? s.selfScore ?? undefined
      }))
    } finally {
      loading.value = false
    }
  }
)

async function submit() {
  if (rows.value.some((r) => r.input === undefined || r.input === null)) {
    message.warning(t('hr.scoreModal.scoreAll'))
    return
  }
  saving.value = true
  try {
    const data = { scores: rows.value.map((r) => ({ indicatorId: r.indicatorId, score: r.input })), comment: comment.value || null }
    if (props.mode === 'self') {
      await selfEvaluate(props.appraisalId, data)
    } else {
      await reviewAppraisal(props.appraisalId, data)
    }
    message.success(props.mode === 'self' ? t('hr.scoreModal.selfSubmitted') : t('hr.scoreModal.reviewSaved'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.head {
  margin-bottom: 12px;
}
.total {
  margin: 12px 0;
}
.self-comment {
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
</style>
