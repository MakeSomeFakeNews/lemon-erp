<template>
  <template v-if="employee">
    <div class="section-title">{{ $t('hr.self.myAppraisals') }}</div>
    <a-table row-key="id" size="small" :columns="myAppraisalColumns" :data-source="myAppraisals" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'"><DictTag type="hr_appraisal_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'grade'"><DictTag type="hr_perf_grade" :value="record.grade" /></template>
        <template v-else-if="column.key === 'action'">
          <a-button v-if="record.status === AppraisalStatus.SELF && record.planStatus === PerfStatus.SCORING" type="link" size="small" @click="openScore(record, 'self')">
            {{ $t('hr.scoreModal.self') }}
          </a-button>
          <a-button v-else-if="record.status === AppraisalStatus.DONE" type="link" size="small" @click="viewResult(record)">{{ $t('common.action.view') }}</a-button>
        </template>
      </template>
    </a-table>
  </template>
  <div class="section-title">{{ $t('hr.self.pendingReview') }}</div>
  <a-table row-key="id" size="small" :columns="reviewColumns" :data-source="reviews" :pagination="false">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'employee'">{{ record.empNo }} {{ record.employeeName }}</template>
      <template v-else-if="column.key === 'status'"><DictTag type="hr_appraisal_status" :value="record.status" /></template>
      <template v-else-if="column.key === 'grade'"><DictTag type="hr_perf_grade" :value="record.grade" /></template>
      <template v-else-if="column.key === 'action'">
        <a-button v-if="record.status !== AppraisalStatus.DONE && record.planStatus === PerfStatus.SCORING" type="link" size="small" @click="openScore(record, 'review')">
          {{ $t('hr.scoreModal.score') }}
        </a-button>
      </template>
    </template>
  </a-table>

  <ScoreModal v-model:open="scoreOpen" :appraisal-id="scoreId" :mode="scoreMode" @success="emit('changed')" />

  <a-modal v-model:open="resultOpen" :title="$t('hr.self.resultTitle')" :footer="null" destroy-on-close>
    <template v-if="result">
      <a-descriptions size="small" :column="1" bordered>
        <a-descriptions-item :label="$t('hr.scoreModal.plan')">{{ result.planName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.score')">{{ $t('hr.self.scoreWithSelf', { score: result.score, self: result.selfScore ?? '-' }) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.grade')"><DictTag type="hr_perf_grade" :value="result.grade" /></a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.reviewer')">{{ result.reviewerName }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.self.comment')">{{ result.reviewComment || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-table row-key="id" size="small" :columns="scoreColumns" :data-source="result.scores" :pagination="false" class="score-table" />
    </template>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import ScoreModal from '@/components/hr/ScoreModal.vue'
import { getAppraisal } from '@/api/hr'
import { t } from '@/locales'
import { AppraisalStatus, PerfStatus } from '@/constants/enums'

/** 员工自助 - 绩效：我的考核（employee 为是否关联员工档案）和待我评分，评分后触发 changed */
defineProps({
  employee: { type: Boolean, default: false },
  myAppraisals: { type: Array, default: () => [] },
  reviews: { type: Array, default: () => [] }
})
const emit = defineEmits(['changed'])

const myAppraisalColumns = [
  { title: t('hr.scoreModal.plan'), dataIndex: 'planName' },
  { title: t('hr.self.reviewer'), dataIndex: 'reviewerName', width: 100 },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.self.score'), dataIndex: 'score', width: 80 },
  { title: t('hr.self.grade'), key: 'grade', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: '', key: 'action', width: 80 }
]
const reviewColumns = [
  { title: t('hr.scoreModal.plan'), dataIndex: 'planName' },
  { title: t('hr.scoreModal.appraisee'), key: 'employee', width: 160 },
  { title: t('common.field.dept'), dataIndex: 'deptName', width: 120 },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.self.score'), dataIndex: 'score', width: 80 },
  { title: t('hr.self.grade'), key: 'grade', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: '', key: 'action', width: 80 }
]
const scoreOpen = ref(false)
const scoreId = ref()
const scoreMode = ref('self')
function openScore(record, mode) {
  scoreId.value = record.id
  scoreMode.value = mode
  scoreOpen.value = true
}
const resultOpen = ref(false)
const result = ref()
const scoreColumns = [
  { title: t('hr.scoreModal.indicator'), dataIndex: 'indicatorName' },
  { title: t('hr.scoreModal.weight'), dataIndex: 'weight', width: 80, customRender: ({ text }) => `${Number(text)}%` },
  { title: t('hr.scoreModal.self'), dataIndex: 'selfScore', width: 80 },
  { title: t('hr.scoreModal.score'), dataIndex: 'score', width: 80 }
]
async function viewResult(record) {
  result.value = await getAppraisal(record.id)
  resultOpen.value = true
}
</script>

<style scoped>
.section-title {
  font-weight: 600;
  margin: 8px 0 12px;
}
.score-table {
  margin-top: 12px;
}
</style>
