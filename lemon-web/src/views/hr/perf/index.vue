<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('hr.perf.plan')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('hr.perf.namePh0')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.hr_perf_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:perf:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('hr.perf.add') }}
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openResult(record)">{{ record.name }}</a>
            <div class="sub-text">{{ periodTypeText(record.periodType) }} · {{ record.startDate }} ~ {{ record.endDate }}</div>
          </template>
          <template v-else-if="column.key === 'payPeriod'">
            {{ record.payPeriod ? periodText(record.payPeriod) : $t('hr.perf.noPay') }}
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress
              v-if="record.total"
              :percent="Math.round((record.doneCount / record.total) * 100)"
              size="small"
              :format="() => `${record.doneCount}/${record.total}`"
            />
            <span v-else class="sub-text">{{ $t('hr.perf.notStarted') }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_perf_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status !== 'DRAFT'" type="link" size="small" @click="openResult(record)">{{ $t('hr.perf.result') }}</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-perm="'hr:perf:edit'" type="link" size="small" @click="openStart(record)">{{ $t('hr.perf.start') }}</a-button>
              <span v-if="record.status === 'SCORING'" v-perm="'hr:perf:edit'">
                <a-popconfirm :title="$t('hr.perf.confirmFinish')" @confirm="finish(record.id)">
                  <a-button type="link" size="small">{{ $t('hr.perf.archive') }}</a-button>
                </a-popconfirm>
              </span>
              <a-button v-if="record.status !== 'FINISHED'" v-perm="'hr:perf:edit'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
              <span v-if="record.status === 'DRAFT'" v-perm="'hr:perf:remove'">
                <a-popconfirm :title="$t('hr.perf.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 考核方案 -->
    <FormDrawer :drawer="drawer" :create-title="$t('hr.perf.add')" :edit-title="$t('hr.perf.edit')" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-alert v-if="isEdit && form.status !== 'DRAFT'" type="info" show-icon class="mb" :message="$t('hr.perf.lockedTip')" />
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item :label="$t('hr.perf.name')" name="name"><a-input v-model:value="form.name" :maxlength="128" :placeholder="$t('hr.perf.namePh')" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('hr.perf.periodType')" name="periodType">
              <a-select v-model:value="form.periodType" :options="PERIOD_TYPES" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.beginDate')" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.endDate')" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.perf.payPeriod')" name="payPeriod" :extra="$t('hr.perf.payPeriodExtra')">
              <PeriodPicker v-model:value="form.payPeriod" :placeholder="$t('hr.perf.noPay')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('hr.perf.description')" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
        <div class="indicator-head">
          <span>{{ $t('hr.perf.indicators') }} <span :class="['sub-text', { danger: weightSum !== 100 }]">{{ $t('hr.perf.weightSum', { n: weightSum }) }}</span></span>
          <a-button v-if="!locked" size="small" @click="form.indicators.push({ name: '', weight: undefined, description: '' })">
            <template #icon><PlusOutlined /></template>{{ $t('hr.perf.addIndicator') }}
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="indicatorColumns" :data-source="indicatorRows" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'name'">
              <a-input v-model:value="form.indicators[index].name" :maxlength="64" :disabled="locked" :placeholder="$t('hr.perf.indicatorPh')" />
            </template>
            <template v-else-if="column.key === 'weight'">
              <a-input-number v-model:value="form.indicators[index].weight" :min="1" :max="100" addon-after="%" class="full" :disabled="locked" />
            </template>
            <template v-else-if="column.key === 'description'">
              <a-input v-model:value="form.indicators[index].description" :maxlength="255" :disabled="locked" :placeholder="$t('hr.perf.criteria')" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button v-if="!locked" type="link" danger size="small" @click="form.indicators.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <!-- 发起考核 -->
    <a-modal v-model:open="startOpen" :title="$t('hr.perf.startTitle', { name: startTarget?.name || '' })" :confirm-loading="saving" @ok="start">
      <a-alert
        type="info"
        show-icon
        class="mb"
        :message="$t('hr.perf.startTip')"
      />
      <a-form layout="vertical">
        <a-form-item :label="$t('hr.perf.scope')" :extra="$t('hr.perf.scopeExtra')">
          <a-tree-select
            v-model:value="startDepts"
            multiple
            allow-clear
            tree-default-expand-all
            :tree-data="deptList"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            :placeholder="$t('hr.perf.allDepts')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 考核结果 -->
    <SubPage v-model:open="resultOpen" :title="$t('hr.perf.resultTitle', { name: resultPlan?.name || '' })" width="min(1000px, 100vw)" destroy-on-close>
      <template v-if="resultPlan">
        <a-row :gutter="16" class="mb">
          <a-col :xs="24" :md="10">
            <EChart v-if="gradeOption" :option="gradeOption" height="220px" />
            <a-empty v-else :description="$t('hr.perf.noResult')" />
          </a-col>
          <a-col :xs="24" :md="14">
            <a-descriptions size="small" :column="1" bordered>
              <a-descriptions-item :label="$t('common.field.status')"><DictTag type="hr_perf_status" :value="resultPlan.status" /></a-descriptions-item>
              <a-descriptions-item :label="$t('hr.perf.progress')">{{ resultPlan.doneCount }} / {{ resultPlan.total }}</a-descriptions-item>
              <a-descriptions-item :label="$t('hr.perf.indicator')">
                <span v-for="i in resultPlan.indicators || []" :key="i.id" class="indicator">{{ i.name }} {{ Number(i.weight) }}%</span>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('hr.perf.bonus')">{{ resultPlan.payPeriod ? periodText(resultPlan.payPeriod) : $t('hr.perf.noPay') }}</a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <div class="result-filter">
          <a-select v-model:value="aq.status" :placeholder="$t('hr.perf.allStatus')" allow-clear class="w140" :options="toOptions(dict.hr_appraisal_status)" @change="aSearch" />
          <a-input-search v-model:value="aq.keyword" :placeholder="$t('hr.perf.keywordPh')" allow-clear class="w200" @search="aSearch" />
        </div>
        <a-table
          row-key="id"
          size="small"
          :columns="appraisalColumns"
          :data-source="aList"
          :loading="aLoading"
          :pagination="aPagination"
          @change="aChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'">
              <div>{{ record.employeeName }}</div>
              <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'status'"><DictTag type="hr_appraisal_status" :value="record.status" /></template>
            <template v-else-if="column.key === 'grade'"><DictTag type="hr_perf_grade" :value="record.grade" /></template>
            <template v-else-if="column.key === 'comment'">
              <div v-if="record.reviewComment">{{ record.reviewComment }}</div>
              <div v-if="record.selfComment" class="sub-text">{{ $t('hr.perf.selfComment', { text: record.selfComment }) }}</div>
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-if="resultPlan.status === 'SCORING'" class="table-actions">
                <a-button
                  v-if="record.status !== 'DONE' && (record.reviewerId === userStore.user?.id || userStore.hasPerm('hr:perf:score'))"
                  type="link"
                  size="small"
                  @click="openScore(record)"
                >
                  {{ $t('hr.perf.score') }}
                </a-button>
                <a-popconfirm v-if="record.status === 'DONE' && userStore.hasPerm('hr:perf:score')" :title="$t('hr.perf.confirmReopen')" @confirm="reopen(record.id)">
                  <a-button type="link" size="small">{{ $t('hr.perf.reopen') }}</a-button>
                </a-popconfirm>
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <ScoreModal v-model:open="scoreOpen" :appraisal-id="scoreId" mode="review" @success="afterScore" />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import EChart from '@/components/charts/EChart.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import ScoreModal from '@/components/hr/ScoreModal.vue'
import { PERIOD_TYPES, periodText } from '@/components/hr/hr'
import { addPerf, deletePerf, finishPerf, getPerf, pageAppraisal, pagePerf, perfGrades, reopenAppraisal, startPerf, updatePerf } from '@/api/hr'
import { deptTree } from '@/api/system/dept'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'

const { t } = useI18n()

const userStore = useUserStore()
const dict = useDict('hr_perf_status', 'hr_appraisal_status', 'hr_perf_grade')
const periodTypeText = (t) => PERIOD_TYPES.find((p) => p.value === t)?.label || t
const showSearch = ref(true)

const columns = [
  { title: t('hr.perf.plan'), key: 'name', width: 280 },
  { title: t('hr.perf.payPeriod'), key: 'payPeriod', width: 160 },
  { title: t('hr.perf.col.scoreProgress'), key: 'progress', width: 180 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('common.field.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 260, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'hr-perf')
const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePerf, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
const deptList = ref([])
onMounted(async () => {
  load()
  deptList.value = await deptTree().catch(() => [])
})

// 考核方案
const DEFAULT_INDICATORS = () => [
  { name: t('hr.perf.ind.result'), weight: 60, description: t('hr.perf.ind.resultDesc') },
  { name: t('hr.perf.ind.ability'), weight: 20, description: t('hr.perf.ind.abilityDesc') },
  { name: t('hr.perf.ind.attitude'), weight: 20, description: t('hr.perf.ind.attitudeDesc') }
]
const rules = {
  name: [{ required: true, whitespace: true, message: t('hr.perf.rule.name') }],
  periodType: [{ required: true, message: t('hr.perf.rule.periodType') }],
  startDate: [{ required: true, message: t('hr.perf.rule.startDate') }],
  endDate: [{ required: true, message: t('hr.perf.rule.endDate') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', periodType: 'QUARTER', startDate: dayjs().startOf('quarter').format('YYYY-MM-DD'),
    endDate: dayjs().endOf('quarter').format('YYYY-MM-DD'), payPeriod: undefined, remark: '', status: 'DRAFT', indicators: DEFAULT_INDICATORS() }),
  fetch: (r) => getPerf(r.id),
  create: addPerf,
  update: updatePerf,
  toPayload: (f) => ({ name: f.name, periodType: f.periodType, startDate: f.startDate, endDate: f.endDate, payPeriod: f.payPeriod || null,
    remark: f.remark || null, indicators: f.indicators.map((i) => ({ name: i.name, weight: i.weight, description: i.description || null })) }),
  onSuccess: load
})
const { formRef, form, isEdit } = drawer
const locked = computed(() => isEdit.value && form.value.status !== 'DRAFT')
const weightSum = computed(() => (form.value.indicators || []).reduce((s, i) => s + (Number(i.weight) || 0), 0))
const indicatorRows = computed(() => (form.value.indicators || []).map((i, idx) => ({ ...i, key: idx })))
const indicatorColumns = [
  { title: t('hr.perf.indicator'), key: 'name', width: 200 },
  { title: t('hr.perf.col.weight'), key: 'weight', width: 130 },
  { title: t('hr.perf.criteria'), key: 'description' },
  { title: '', key: 'action', width: 50 }
]

async function remove(id) {
  await deletePerf(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}
async function finish(id) {
  await finishPerf(id)
  message.success(t('hr.perf.archived'))
  load()
}

// 发起
const startOpen = ref(false)
const startTarget = ref()
const startDepts = ref([])
const saving = ref(false)
function openStart(record) {
  startTarget.value = record
  startDepts.value = []
  startOpen.value = true
}
async function start() {
  saving.value = true
  try {
    const n = await startPerf(startTarget.value.id, startDepts.value)
    message.success(t('hr.perf.started', { n }))
    startOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 考核结果
const resultOpen = ref(false)
const resultPlan = ref()
const grades = ref([])
const aq = reactive({ planId: undefined, status: undefined, keyword: undefined })
const { list: aList, loading: aLoading, pagination: aPagination, load: aLoad, search: aSearch, onTableChange: aChange } = useTable(pageAppraisal, aq)
const appraisalColumns = [
  { title: t('hr.perf.col.employee'), key: 'employee', width: 170 },
  { title: t('hr.perf.col.reviewer'), dataIndex: 'reviewerName', width: 100 },
  { title: t('hr.perf.col.self'), dataIndex: 'selfScore', width: 70 },
  { title: t('hr.perf.col.score'), dataIndex: 'score', width: 70 },
  { title: t('hr.perf.col.grade'), key: 'grade', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 110 },
  { title: t('hr.perf.col.comment'), key: 'comment' },
  { title: '', key: 'action', width: 100 }
]
const GRADE_COLORS = { S: '#faad14', A: '#52c41a', B: '#1677ff', C: '#fa8c16', D: '#f5222d' }
const gradeOption = computed(() => {
  if (!grades.value.length) return null
  return {
    tooltip: { trigger: 'item', formatter: (p) => t('hr.perf.gradeTip', { name: p.name, value: p.value, percent: p.percent }) },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      data: grades.value.map((g) => ({ name: findDict(dict.hr_perf_grade, g.grade)?.label || g.grade, value: Number(g.count),
        itemStyle: { color: GRADE_COLORS[g.grade] } }))
    }]
  }
})
async function openResult(record) {
  resultOpen.value = true
  resultPlan.value = record
  aq.planId = record.id
  aq.status = undefined
  aq.keyword = undefined
  aSearch()
  const [plan, g] = await Promise.all([getPerf(record.id), perfGrades(record.id)])
  resultPlan.value = plan
  grades.value = g
}

const scoreOpen = ref(false)
const scoreId = ref()
function openScore(record) {
  scoreId.value = record.id
  scoreOpen.value = true
}
async function afterScore() {
  aLoad()
  load()
  grades.value = await perfGrades(resultPlan.value.id)
  resultPlan.value = await getPerf(resultPlan.value.id)
}
async function reopen(id) {
  await reopenAppraisal(id)
  message.success(t('hr.perf.reopened'))
  afterScore()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.full {
  width: 100%;
}
.mb {
  margin-bottom: 16px;
}
.indicator-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 12px;
  font-weight: 600;
}
.indicator {
  margin-right: 12px;
}
.result-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.w140 {
  width: 140px;
}
.w200 {
  width: 200px;
}
</style>
