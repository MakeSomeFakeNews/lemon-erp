<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.training.training')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.training.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('hr.training.category')" name="category">
          <a-select v-model:value="query.category" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_training_category)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_training_status)" />
        </a-form-item>
        <a-form-item :label="$t('hr.training.year')" name="year">
          <a-select v-model:value="query.year" :placeholder="$t('common.placeholder.all')" allow-clear :options="years" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:training:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.training.add') }}
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="openRecords(record)">{{ record.title }}</a>
            <div class="sub-text">{{ $t('hr.training.trainerLine', { trainer: record.trainer || '-', location: record.location || '-' }) }}</div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="hr_training_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'date'">
            {{ record.startDate }}<span v-if="record.endDate !== record.startDate"> ~ {{ record.endDate }}</span>
          </template>
          <template v-else-if="column.key === 'trainees'">
            <div>{{ $t('hr.training.traineeCount', { n: record.traineeCount }) }}</div>
            <div v-if="record.status === TrainingStatus.FINISHED" class="sub-text">{{ $t('hr.training.attendSummary', { attended: record.attendedCount, passed: record.passedCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'cost'">{{ formatMoney(record.cost) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_training_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openRecords(record)">{{ $t('hr.training.records') }}</a-button>
              <template v-if="userStore.hasPerm('hr:training:edit')">
                <a-popconfirm v-if="record.status === TrainingStatus.PLANNED" :title="$t('hr.training.confirmStart')" @confirm="setStatus(record, 'ONGOING')">
                  <a-button type="link" size="small">{{ $t('hr.training.start') }}</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="record.status === TrainingStatus.ONGOING" :title="$t('hr.training.confirmFinish')" @confirm="setStatus(record, 'FINISHED')">
                  <a-button type="link" size="small">{{ $t('hr.training.finish') }}</a-button>
                </a-popconfirm>
                <a-dropdown v-if="[TrainingStatus.PLANNED, TrainingStatus.ONGOING].includes(record.status)">
                  <a-button type="link" size="small">{{ $t('common.action.more') }}<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => (key === 'edit' ? drawer.openEdit(record) : setStatus(record, 'CANCELED'))">
                      <a-menu-item key="edit">{{ $t('common.action.edit') }}</a-menu-item>
                      <a-menu-item key="cancel" danger>{{ $t('hr.training.cancelTraining') }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
              <span v-if="[TrainingStatus.PLANNED, TrainingStatus.CANCELED].includes(record.status)" v-perm="'hr:training:remove'">
                <a-popconfirm :title="$t('hr.training.confirmDelete', { name: record.title })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('hr.training.add')" :edit-title="$t('hr.training.edit')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('hr.training.title')" name="title"><a-input v-model:value="form.title" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.training.category')" name="category"><a-select v-model:value="form.category" :options="toOptions(dict.hr_training_category)" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.training.trainer')" name="trainer"><a-input v-model:value="form.trainer" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.beginDate')" name="startDate"><a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.endDate')" name="endDate"><a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.training.hours')" name="hours"><a-input-number v-model:value="form.hours" :min="0" :precision="1" :addon-after="$t('hr.training.hourUnit')" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.training.cost')" name="cost"><a-input-number v-model:value="form.cost" :min="0" :precision="2" :addon-after="$t('hr.training.yuan')" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.training.location')" name="location"><a-input v-model:value="form.location" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('hr.training.content')" name="content"><a-textarea v-model:value="form.content" :rows="4" :maxlength="1000" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 学员与成绩 -->
    <SubPage v-model:open="recordOpen" :title="$t('hr.training.recordsTitle', { name: current?.title || '' })" width="min(900px, 100vw)" destroy-on-close>
      <template v-if="current">
        <div class="record-head">
          <span>
            <DictTag type="hr_training_status" :value="current.status" />
            {{ $t('hr.training.recordHead', { date: current.startDate, hours: Number(current.hours), n: records.length }) }}
          </span>
          <span v-if="editableTrainees" v-perm="'hr:training:edit'" class="add-trainee">
            <EmployeeSelect v-model:value="addIds" multiple :placeholder="$t('hr.training.addTraineePh')" class="trainee-select" />
            <a-button type="primary" :disabled="!addIds?.length" @click="doAddTrainees">{{ $t('hr.training.addBtn') }}</a-button>
          </span>
        </div>
        <a-alert v-if="current.status === TrainingStatus.PLANNED" type="info" show-icon :message="$t('hr.training.plannedTip')" class="mb" />
        <a-table row-key="id" size="small" :columns="recordColumns" :data-source="records" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'">
              <div>{{ record.employeeName }}</div>
              <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'attended'">
              <a-switch v-model:checked="record.attendedBool" :disabled="!editableScores" :checked-children="$t('hr.training.attended')" :un-checked-children="$t('hr.training.absent')" />
            </template>
            <template v-else-if="column.key === 'score'">
              <a-input-number v-model:value="record.score" :min="0" :max="100" :precision="1" :disabled="!editableScores || !record.attendedBool" class="full" />
            </template>
            <template v-else-if="column.key === 'passed'">
              <a-select
                v-model:value="record.passed"
                :disabled="!editableScores || !record.attendedBool"
                allow-clear
                class="full"
                :options="[{ value: 1, label: $t('hr.training.passed') }, { value: 0, label: $t('hr.training.failed') }]"
              />
            </template>
            <template v-else-if="column.key === 'feedback'">
              <a-input v-model:value="record.feedback" :maxlength="255" :disabled="!editableScores" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm v-if="editableTrainees && userStore.hasPerm('hr:training:edit')" :title="$t('hr.training.confirmRemove')" @confirm="doRemove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </template>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="recordOpen = false">{{ $t('common.action.close') }}</a-button>
          <a-button v-if="editableScores" v-perm="'hr:training:edit'" type="primary" :loading="savingRecords" @click="saveRecords">{{ $t('hr.training.saveRecords') }}</a-button>
        </div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import EmployeeSelect from '@/components/hr/EmployeeSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import {
  addTraining,
  addTrainees,
  changeTrainingStatus,
  deleteTraining,
  getTraining,
  pageTraining,
  removeTrainees,
  saveTrainingRecords,
  trainingRecords,
  updateTraining
} from '@/api/hr'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

import { TrainingStatus } from '@/constants/enums'
const { t } = useI18n()

const userStore = useUserStore()
const dict = useDict('hr_training_category', 'hr_training_status')
const showSearch = ref(true)
const thisYear = dayjs().year()
const years = [thisYear + 1, thisYear, thisYear - 1, thisYear - 2].map((y) => ({ value: y, label: t('hr.training.yearOption', { year: y }) }))

const columns = [
  { title: t('hr.training.training'), key: 'title', width: 260, fixed: 'left' },
  { title: t('hr.training.category'), key: 'category', width: 110, dict: 'hr_training_category' },
  { title: t('common.field.date'), key: 'date', width: 200 },
  { title: t('hr.training.hours'), dataIndex: 'hours', width: 100 },
  { title: t('hr.training.col.trainees'), key: 'trainees', width: 170 },
  { title: t('hr.training.cost'), key: 'cost', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 100, dict: 'hr_training_status' },
  { title: t('common.action.operation'), key: 'action', width: 280, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-training')

const emptyQuery = () => ({ keyword: undefined, category: undefined, status: undefined, year: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTraining, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const FIELDS = ['title', 'category', 'trainer', 'startDate', 'endDate', 'hours', 'location', 'cost', 'content']
const rules = {
  title: [{ required: true, whitespace: true, message: t('hr.training.rule.title') }],
  startDate: [{ required: true, message: t('hr.training.rule.startDate') }],
  endDate: [{ required: true, message: t('hr.training.rule.endDate') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), category: 'SKILL', hours: 2, cost: 0 }),
  fetch: (r) => getTraining(r.id),
  create: addTraining,
  update: updateTraining,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: load
})
const { formRef, form } = drawer

async function setStatus(record, status) {
  await changeTrainingStatus(record.id, status)
  message.success(t('hr.training.updated'))
  load()
  if (current.value?.id === record.id) current.value = { ...current.value, status }
}

const remove = useAction(deleteTraining, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// 学员与成绩
const recordOpen = ref(false)
const current = ref()
const records = ref([])
const addIds = ref([])
const savingRecords = ref(false)
const editableTrainees = computed(() => [TrainingStatus.PLANNED, TrainingStatus.ONGOING].includes(current.value?.status))
const editableScores = computed(() => [TrainingStatus.ONGOING, TrainingStatus.FINISHED].includes(current.value?.status))
const recordColumns = [
  { title: t('hr.training.col.trainee'), key: 'employee', width: 180 },
  { title: t('hr.training.attended'), key: 'attended', width: 100 },
  { title: t('hr.training.col.score'), key: 'score', width: 110 },
  { title: t('hr.training.col.result'), key: 'passed', width: 110 },
  { title: t('hr.training.col.feedback'), key: 'feedback' },
  { title: '', key: 'action', width: 60 }
]

async function openRecords(record) {
  current.value = record
  recordOpen.value = true
  await loadRecords()
}
async function loadRecords() {
  const list = await trainingRecords(current.value.id)
  records.value = list.map((r) => ({ ...r, attendedBool: r.attended === 1 }))
}
async function doAddTrainees() {
  const n = await addTrainees(current.value.id, addIds.value)
  message.success(n ? t('hr.training.addedN', { n }) : t('hr.training.allExist'))
  addIds.value = []
  await loadRecords()
  load()
}
async function doRemove(id) {
  await removeTrainees(current.value.id, id)
  await loadRecords()
  load()
}
async function saveRecords() {
  savingRecords.value = true
  try {
    await saveTrainingRecords(current.value.id, records.value.map((r) => ({
      id: r.id,
      attended: r.attendedBool ? 1 : 0,
      score: r.attendedBool ? r.score ?? null : null,
      passed: r.attendedBool ? r.passed ?? null : 0,
      feedback: r.feedback || null
    })))
    message.success(t('common.msg.saved'))
    load()
  } finally {
    savingRecords.value = false
  }
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 12px;
}
.record-head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.add-trainee {
  display: flex;
  gap: 8px;
}
.trainee-select {
  width: 320px;
  max-width: 60vw;
}
</style>
