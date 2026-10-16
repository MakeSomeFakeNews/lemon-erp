<template>
  <a-modal
    :open="open"
    :title="mode === 'self' ? '绩效自评' : '绩效评分'"
    :confirm-loading="saving"
    width="min(720px, 100vw)"
    destroy-on-close
    @cancel="emit('update:open', false)"
    @ok="submit"
  >
    <a-spin :spinning="loading">
      <template v-if="appraisal">
        <a-descriptions size="small" :column="3" class="head">
          <a-descriptions-item label="考核">{{ appraisal.planName }}</a-descriptions-item>
          <a-descriptions-item label="被考核人">{{ appraisal.empNo }} {{ appraisal.employeeName }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ appraisal.deptName || '-' }}</a-descriptions-item>
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
          加权得分：<b>{{ total }}</b>
          <a-tag v-if="mode === 'review'" :color="gradeColor" :bordered="false">{{ grade }}</a-tag>
        </div>
        <div v-if="mode === 'review' && appraisal.selfComment" class="self-comment">
          <span class="sub-text">自评总结：</span>{{ appraisal.selfComment }}
        </div>
        <a-textarea v-model:value="comment" :rows="3" :maxlength="500" :placeholder="mode === 'self' ? '自评总结（选填）' : '评语（选填）'" />
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
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

const appraisal = ref(null)
const rows = ref([])
const comment = ref('')
const loading = ref(false)
const saving = ref(false)

const columns = computed(() => [
  { title: '指标', key: 'name' },
  { title: '权重', key: 'weight', width: 80 },
  ...(props.mode === 'review' ? [{ title: '自评', key: 'selfScore', width: 80 }] : []),
  { title: props.mode === 'self' ? '自评分' : '评分', key: 'input', width: 130 }
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
    message.warning('请为每个指标评分')
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
    message.success(props.mode === 'self' ? '自评已提交' : '评分已保存')
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
