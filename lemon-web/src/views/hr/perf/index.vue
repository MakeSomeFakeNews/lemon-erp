<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="考核" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="考核名称" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.hr_perf_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:perf:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新建考核
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
            {{ record.payPeriod ? periodText(record.payPeriod) : '不发放' }}
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress
              v-if="record.total"
              :percent="Math.round((record.doneCount / record.total) * 100)"
              size="small"
              :format="() => `${record.doneCount}/${record.total}`"
            />
            <span v-else class="sub-text">未发起</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_perf_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status !== 'DRAFT'" type="link" size="small" @click="openResult(record)">考核结果</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-perm="'hr:perf:edit'" type="link" size="small" @click="openStart(record)">发起</a-button>
              <span v-if="record.status === 'SCORING'" v-perm="'hr:perf:edit'">
                <a-popconfirm title="归档后不能再评分，绩效奖金按发放月份计入工资，确定归档？" @confirm="finish(record.id)">
                  <a-button type="link" size="small">归档</a-button>
                </a-popconfirm>
              </span>
              <a-button v-if="record.status !== 'FINISHED'" v-perm="'hr:perf:edit'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
              <span v-if="record.status === 'DRAFT'" v-perm="'hr:perf:remove'">
                <a-popconfirm :title="`确定删除「${record.name}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 考核方案 -->
    <FormDrawer :drawer="drawer" create-title="新建考核" edit-title="编辑考核" width="min(760px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-alert v-if="isEdit && form.status !== 'DRAFT'" type="info" show-icon class="mb" message="考核已发起，只能修改名称、说明和奖金发放月份。" />
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="考核名称" name="name"><a-input v-model:value="form.name" :maxlength="128" placeholder="如：2026 年第三季度绩效考核" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="考核周期" name="periodType">
              <a-select v-model:value="form.periodType" :options="PERIOD_TYPES" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" :disabled="locked" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="奖金发放月份" name="payPeriod" extra="归档后绩效奖金计入该月工资，不选不发放">
              <PeriodPicker v-model:value="form.payPeriod" placeholder="不发放" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="说明" name="remark"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
        <div class="indicator-head">
          <span>考核指标 <span :class="['sub-text', { danger: weightSum !== 100 }]">权重合计 {{ weightSum }}%</span></span>
          <a-button v-if="!locked" size="small" @click="form.indicators.push({ name: '', weight: undefined, description: '' })">
            <template #icon><PlusOutlined /></template>添加指标
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="indicatorColumns" :data-source="indicatorRows" :pagination="false">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'name'">
              <a-input v-model:value="form.indicators[index].name" :maxlength="64" :disabled="locked" placeholder="如：工作业绩" />
            </template>
            <template v-else-if="column.key === 'weight'">
              <a-input-number v-model:value="form.indicators[index].weight" :min="1" :max="100" addon-after="%" class="full" :disabled="locked" />
            </template>
            <template v-else-if="column.key === 'description'">
              <a-input v-model:value="form.indicators[index].description" :maxlength="255" :disabled="locked" placeholder="评分标准" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button v-if="!locked" type="link" danger size="small" @click="form.indicators.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <!-- 发起考核 -->
    <a-modal v-model:open="startOpen" :title="`发起考核：${startTarget?.name || ''}`" :confirm-loading="saving" @ok="start">
      <a-alert
        type="info"
        show-icon
        class="mb"
        message="为所选部门（含下级）的在职员工生成考核表，评分人为员工所在部门的负责人；关联了登录账号的员工先自评。"
      />
      <a-form layout="vertical">
        <a-form-item label="考核范围" extra="不选为全部在职员工">
          <a-tree-select
            v-model:value="startDepts"
            multiple
            allow-clear
            tree-default-expand-all
            :tree-data="deptList"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="全部部门"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 考核结果 -->
    <SubPage v-model:open="resultOpen" :title="`考核结果 - ${resultPlan?.name || ''}`" width="min(1000px, 100vw)" destroy-on-close>
      <template v-if="resultPlan">
        <a-row :gutter="16" class="mb">
          <a-col :xs="24" :md="10">
            <EChart v-if="gradeOption" :option="gradeOption" height="220px" />
            <a-empty v-else description="还没有完成评分的考核表" />
          </a-col>
          <a-col :xs="24" :md="14">
            <a-descriptions size="small" :column="1" bordered>
              <a-descriptions-item label="状态"><DictTag type="hr_perf_status" :value="resultPlan.status" /></a-descriptions-item>
              <a-descriptions-item label="完成进度">{{ resultPlan.doneCount }} / {{ resultPlan.total }}</a-descriptions-item>
              <a-descriptions-item label="指标">
                <span v-for="i in resultPlan.indicators || []" :key="i.id" class="indicator">{{ i.name }} {{ Number(i.weight) }}%</span>
              </a-descriptions-item>
              <a-descriptions-item label="奖金发放">{{ resultPlan.payPeriod ? periodText(resultPlan.payPeriod) : '不发放' }}</a-descriptions-item>
            </a-descriptions>
          </a-col>
        </a-row>
        <div class="result-filter">
          <a-select v-model:value="aq.status" placeholder="全部状态" allow-clear class="w140" :options="toOptions(dict.hr_appraisal_status)" @change="aSearch" />
          <a-input-search v-model:value="aq.keyword" placeholder="工号或姓名" allow-clear class="w200" @search="aSearch" />
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
              <div v-if="record.selfComment" class="sub-text">自评：{{ record.selfComment }}</div>
            </template>
            <template v-else-if="column.key === 'action'">
              <span v-if="resultPlan.status === 'SCORING'" class="table-actions">
                <a-button
                  v-if="record.status !== 'DONE' && (record.reviewerId === userStore.user?.id || userStore.hasPerm('hr:perf:score'))"
                  type="link"
                  size="small"
                  @click="openScore(record)"
                >
                  评分
                </a-button>
                <a-popconfirm v-if="record.status === 'DONE' && userStore.hasPerm('hr:perf:score')" title="退回重新评分？" @confirm="reopen(record.id)">
                  <a-button type="link" size="small">退回重评</a-button>
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

const userStore = useUserStore()
const dict = useDict('hr_perf_status', 'hr_appraisal_status', 'hr_perf_grade')
const periodTypeText = (t) => PERIOD_TYPES.find((p) => p.value === t)?.label || t
const showSearch = ref(true)

const columns = [
  { title: '考核', key: 'name', width: 280 },
  { title: '奖金发放月份', key: 'payPeriod', width: 130 },
  { title: '评分进度', key: 'progress', width: 180 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
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
  { name: '工作业绩', weight: 60, description: '目标完成情况、工作质量与效率' },
  { name: '工作能力', weight: 20, description: '专业技能、解决问题的能力' },
  { name: '工作态度', weight: 20, description: '责任心、协作与纪律' }
]
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入考核名称' }],
  periodType: [{ required: true, message: '请选择考核周期' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }]
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
  { title: '指标', key: 'name', width: 200 },
  { title: '权重', key: 'weight', width: 130 },
  { title: '评分标准', key: 'description' },
  { title: '', key: 'action', width: 50 }
]

async function remove(id) {
  await deletePerf(id)
  message.success('删除成功')
  load()
}
async function finish(id) {
  await finishPerf(id)
  message.success('已归档')
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
    message.success(`已发起，生成 ${n} 张考核表`)
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
  { title: '员工', key: 'employee', width: 170 },
  { title: '评分人', dataIndex: 'reviewerName', width: 100 },
  { title: '自评', dataIndex: 'selfScore', width: 70 },
  { title: '得分', dataIndex: 'score', width: 70 },
  { title: '等级', key: 'grade', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '评语', key: 'comment' },
  { title: '', key: 'action', width: 100 }
]
const GRADE_COLORS = { S: '#faad14', A: '#52c41a', B: '#1677ff', C: '#fa8c16', D: '#f5222d' }
const gradeOption = computed(() => {
  if (!grades.value.length) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
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
  message.success('已退回')
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
