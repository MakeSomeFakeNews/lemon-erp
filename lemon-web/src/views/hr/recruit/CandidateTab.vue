<template>
  <TableToolbar v-model:size="size" :columns="candidateColumns" @refresh="load">
    <a-button v-perm="'hr:recruit:candidate'" type="primary" @click="formRef.openCreate({ recruitId: query.recruitId })">
      <template #icon><PlusOutlined /></template>{{ $t('hr.recruit.addCandidate') }}
    </a-button>
  </TableToolbar>
  <a-table
    row-key="id"
    :size="size"
    :columns="candidateColumns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: 1250, y: '100%' }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <a @click="interviewsRef.open(record)">{{ record.name }}</a>
        <div class="sub-text">{{ genderText(record.gender) }} · {{ record.phone || '-' }}</div>
      </template>
      <template v-else-if="column.key === 'recruit'">
        <div>{{ record.recruitTitle }}</div>
        <div class="sub-text">{{ record.recruitNo }}</div>
      </template>
      <template v-else-if="column.key === 'education'">
        <div>{{ dictLabel(dict.hr_education, record.education) }} {{ record.school || '' }}</div>
        <div class="sub-text">{{ record.workYears != null ? $t('hr.recruit.yearsExp', { n: record.workYears }) : '' }}</div>
      </template>
      <template v-else-if="column.key === 'source'">
        <DictTag type="hr_candidate_source" :value="record.source" />
      </template>
      <template v-else-if="column.key === 'expectedSalary'">
        {{ record.expectedSalary ? formatMoney(record.expectedSalary) : '-' }}
      </template>
      <template v-else-if="column.key === 'stage'">
        <DictTag type="hr_candidate_stage" :value="record.stage" />
        <div v-if="record.empNo" class="sub-text">{{ $t('hr.recruit.empNoText', { no: record.empNo }) }}</div>
      </template>
      <template v-else-if="column.key === 'interview'">
        <span v-if="record.interviewCount">
          {{ $t('hr.recruit.roundsN', { n: record.interviewCount }) }} <DictTag type="hr_interview_result" :value="record.lastResult" />
        </span>
        <span v-else class="sub-text">-</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <span class="table-actions">
          <a-button type="link" size="small" @click="interviewsRef.open(record)">{{ $t('hr.recruit.interview') }}</a-button>
          <template v-if="[CandidateStage.NEW, CandidateStage.INTERVIEW, CandidateStage.OFFER].includes(record.stage)">
            <span v-if="record.stage === CandidateStage.INTERVIEW" v-perm="'hr:recruit:candidate'">
              <a-popconfirm :title="$t('hr.recruit.confirmOffer')" @confirm="offer(record.id)">
                <a-button type="link" size="small">{{ $t('hr.recruit.offer') }}</a-button>
              </a-popconfirm>
            </span>
            <a-button v-if="record.stage === CandidateStage.OFFER" v-perm="'hr:recruit:hire'" type="link" size="small" @click="hireRef.open(record)">{{ $t('hr.recruit.hire') }}</a-button>
            <a-dropdown>
              <a-button type="link" size="small">{{ $t('common.action.more') }}<DownOutlined /></a-button>
              <template #overlay>
                <a-menu @click="({ key }) => onMore(key, record)">
                  <a-menu-item key="edit" :disabled="!userStore.hasPerm('hr:recruit:candidate')">{{ $t('common.action.edit') }}</a-menu-item>
                  <a-menu-item key="reject" danger :disabled="!userStore.hasPerm('hr:recruit:candidate')">{{ $t('hr.recruit.reject') }}</a-menu-item>
                  <a-menu-item key="delete" danger :disabled="!userStore.hasPerm('hr:recruit:candidate')">{{ $t('common.action.delete') }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </span>
      </template>
    </template>
  </a-table>

  <CandidateForm ref="formRef" :open-options="openOptions" @success="onSaved" />
  <CandidateInterviews ref="interviewsRef" @changed="load" />
  <HireModal ref="hireRef" @success="onHired" />
</template>

<script setup>
import { h, ref } from 'vue'
import { Modal } from 'ant-design-vue'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { genderText } from '@/components/hr/hr'
import { formatMoney } from '@/components/scm/scm'
import { deleteCandidate, offerCandidate, pageCandidate, rejectCandidate } from '@/api/hr'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'
import CandidateForm from './CandidateForm.vue'
import CandidateInterviews from './CandidateInterviews.vue'
import HireModal from './HireModal.vue'
import { CandidateStage } from '@/constants/enums'

/**
 * 应聘者列表（放在 table-card 中）：query 为页面上的筛选条件，通过 ref 调用 load() / search() / openCreate(defaults)。
 * 影响需求的招聘人数时触发 recruit-changed，录用后另触发 options-changed（刷新需求下拉）
 */
const props = defineProps({
  query: { type: Object, required: true },
  openOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['recruit-changed', 'options-changed'])
const userStore = useUserStore()
const dict = useDict('hr_education')
const dictLabel = (list, v) => findDict(list, v)?.label || ''

const candidateColumns = [
  { title: t('hr.employee.name'), key: 'name', width: 150, fixed: 'left' },
  { title: t('hr.recruit.appliedPosition'), key: 'recruit', width: 190 },
  { title: t('hr.recruit.educationExp'), key: 'education', width: 200 },
  { title: t('hr.recruit.sourceShort'), key: 'source', width: 110 },
  { title: t('hr.recruit.expectedSalary'), key: 'expectedSalary', width: 140, align: 'right' },
  { title: t('hr.recruit.interview'), key: 'interview', width: 150 },
  { title: t('hr.recruit.stage'), key: 'stage', width: 120 },
  { title: t('hr.recruit.createTime'), dataIndex: 'createTime', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 230, fixed: 'right' }
]
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageCandidate, props.query)
const formRef = ref()
const interviewsRef = ref()
const hireRef = ref()

function onSaved() {
  load()
  emit('recruit-changed')
}
function onHired() {
  load()
  emit('recruit-changed')
  emit('options-changed')
}

const offer = useAction(offerCandidate, { successKey: 'hr.recruit.offered', reload: () => load() })
const reject = useAction((record, reason) => rejectCandidate(record.id, reason), { successKey: 'hr.recruit.rejected', reload: () => load() })
const remove = useAction((record) => deleteCandidate(record.id), {
  confirm: (record) => ({ title: t('hr.recruit.confirmDeleteCandidate', { name: record.name }), okType: 'danger' }),
  successKey: 'common.msg.deleteSuccess',
  reload: onSaved
})

function onMore(key, record) {
  if (key === 'edit') {
    formRef.value.openEdit(record)
  } else if (key === 'reject') {
    let reason = ''
    Modal.confirm({
      title: t('hr.recruit.rejectTitle', { name: record.name }),
      content: () => h('textarea', { class: 'ant-input', rows: 2, placeholder: t('hr.recruit.rejectReasonPh'), onInput: (e) => (reason = e.target.value) }),
      okType: 'danger',
      onOk: () => reject(record, reason)
    })
  } else if (key === 'delete') {
    remove(record)
  }
}

defineExpose({ load, search, openCreate: (defaults) => formRef.value.openCreate(defaults) })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
