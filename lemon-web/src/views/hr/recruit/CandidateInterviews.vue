<template>
  <a-drawer v-model:open="visible" :title="$t('hr.recruit.interviewsTitle', { name: current?.name || '' })" width="min(720px, 100vw)" destroy-on-close>
    <template v-if="current">
      <a-descriptions size="small" :column="2" bordered>
        <a-descriptions-item :label="$t('hr.recruit.appliedPosition')">{{ current.recruitTitle }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.recruit.stage')"><DictTag type="hr_candidate_stage" :value="current.stage" /></a-descriptions-item>
        <a-descriptions-item :label="$t('hr.employee.education')">{{ dictLabel(dict.hr_education, current.education) }} {{ current.school || '' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.recruit.expectedSalary')">{{ current.expectedSalary ? formatMoney(current.expectedSalary) : '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('hr.recruit.resume')" :span="2"><div class="pre">{{ resumeText || '-' }}</div></a-descriptions-item>
      </a-descriptions>
      <div class="section-head"><span>{{ $t('hr.recruit.resumeAttachments') }}</span></div>
      <AttachmentPanel biz-type="hr_candidate" :biz-id="current.id" :editable="userStore.hasPerm('hr:recruit:candidate')" />
      <div class="section-head">
        <span>{{ $t('hr.recruit.interviewSchedule') }}</span>
        <a-button
          v-if="[CandidateStage.NEW, CandidateStage.INTERVIEW].includes(current.stage)"
          v-perm="'hr:recruit:candidate'"
          size="small"
          type="primary"
          @click="openInterviewForm()"
        >
          {{ $t('hr.recruit.scheduleInterview') }}
        </a-button>
      </div>
      <a-empty v-if="!interviews.length" :description="$t('hr.recruit.noInterviews')" />
      <a-timeline v-else>
        <a-timeline-item v-for="i in interviews" :key="i.id" :color="i.result === InterviewResult.PASS ? 'green' : i.result === InterviewResult.FAIL ? 'red' : 'blue'">
          <div class="iv-head">
            {{ $t('hr.recruit.interviewHead', { round: i.round, time: i.interviewTime, method: methodText(i.method), interviewer: i.interviewerName || '-' }) }}
            <DictTag type="hr_interview_result" :value="i.result" />
            <span v-if="i.score != null" class="sub-text">{{ $t('hr.recruit.scoreN', { n: i.score }) }}</span>
            <a v-if="[CandidateStage.NEW, CandidateStage.INTERVIEW].includes(current.stage) && userStore.hasPerm('hr:recruit:candidate')" @click="openInterviewForm(i)">{{ $t('hr.recruit.fillResult') }}</a>
          </div>
          <div v-if="i.feedback" class="sub-text">{{ i.feedback }}</div>
        </a-timeline-item>
      </a-timeline>
    </template>
  </a-drawer>

  <a-modal v-model:open="ivOpen" :title="ivForm.id ? $t('hr.recruit.interviewResult') : $t('hr.recruit.scheduleInterview')" :confirm-loading="ivSaving" destroy-on-close @ok="saveInterview">
    <a-form ref="ivRef" :model="ivForm" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('hr.recruit.interviewTime')" name="interviewTime" :rules="[{ required: true, message: $t('hr.recruit.interviewTimeRequired') }]">
            <a-date-picker v-model:value="ivForm.interviewTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('hr.recruit.method')" name="method"><a-select v-model:value="ivForm.method" :options="INTERVIEW_METHODS" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.interviewer')" name="interviewerId"><IdentityUserSelect v-model:value="ivForm.interviewerId" :label="ivForm.interviewerName" /></a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('hr.employee.result')" name="result">
            <a-radio-group v-model:value="ivForm.result" :options="toOptions(dict.hr_interview_result)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('hr.recruit.score')" name="score"><a-input-number v-model:value="ivForm.score" :min="0" :max="100" :addon-after="$t('hr.recruit.points')" class="full" /></a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('hr.recruit.feedback')" name="feedback"><a-textarea v-model:value="ivForm.feedback" :rows="3" :maxlength="1000" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { INTERVIEW_METHODS } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import { addInterview, candidateInterviews, getCandidate, updateInterview } from '@/api/hr'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { CandidateStage, InterviewMethod, InterviewResult } from '@/constants/enums'

/** 应聘者的简历、附件和面试安排：通过 ref 调用 open(应聘者)，保存面试后触发 changed */
const emit = defineEmits(['changed'])
const userStore = useUserStore()
const dict = useDict('hr_interview_result', 'hr_education')
const dictLabel = (list, v) => findDict(list, v)?.label || ''
const methodText = (m) => INTERVIEW_METHODS.find((x) => x.value === m)?.label || '-'

const visible = ref(false)
const current = ref()
const interviews = ref([])
const resumeText = ref('')
async function open(record) {
  current.value = record
  visible.value = true
  const [c, list] = await Promise.all([getCandidate(record.id), candidateInterviews(record.id)])
  current.value = { ...record, ...c }
  resumeText.value = c.resume
  interviews.value = list
}

const ivOpen = ref(false)
const ivSaving = ref(false)
const ivRef = ref()
const ivForm = reactive({})
function openInterviewForm(i) {
  Object.assign(ivForm, i
    ? { id: i.id, interviewTime: i.interviewTime, method: i.method, interviewerId: i.interviewerId, interviewerName: i.interviewerName,
        result: i.result, score: i.score, feedback: i.feedback }
    : { id: undefined, interviewTime: dayjs().add(1, 'day').hour(10).minute(0).second(0).format('YYYY-MM-DD HH:mm:ss'),
        method: InterviewMethod.ONSITE, interviewerId: userStore.user?.id, interviewerName: userStore.displayName, result: InterviewResult.PENDING, score: undefined,
        feedback: '' })
  ivOpen.value = true
}
async function saveInterview() {
  await ivRef.value.validate()
  ivSaving.value = true
  try {
    const { id, interviewerName, ...data } = ivForm
    if (id) await updateInterview(id, data)
    else await addInterview(current.value.id, data)
    message.success(t('common.msg.saved'))
    ivOpen.value = false
    await open(current.value)
    emit('changed')
  } finally {
    ivSaving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.pre {
  white-space: pre-wrap;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 12px;
  font-weight: 600;
}
.iv-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>
