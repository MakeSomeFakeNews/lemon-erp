<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="培训" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="主题或讲师" allow-clear />
        </a-form-item>
        <a-form-item label="类别" name="category">
          <a-select v-model:value="query.category" placeholder="全部" allow-clear :options="toOptions(dict.hr_training_category)" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.hr_training_status)" />
        </a-form-item>
        <a-form-item label="年度" name="year">
          <a-select v-model:value="query.year" placeholder="全部" allow-clear :options="years" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'hr:training:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>新增培训
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1250, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a @click="openRecords(record)">{{ record.title }}</a>
            <div class="sub-text">讲师 {{ record.trainer || '-' }} · {{ record.location || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'category'">
            <DictTag type="hr_training_category" :value="record.category" />
          </template>
          <template v-else-if="column.key === 'date'">
            {{ record.startDate }}<span v-if="record.endDate !== record.startDate"> ~ {{ record.endDate }}</span>
          </template>
          <template v-else-if="column.key === 'trainees'">
            <div>{{ record.traineeCount }} 人</div>
            <div v-if="record.status === 'FINISHED'" class="sub-text">出勤 {{ record.attendedCount }} · 通过 {{ record.passedCount }}</div>
          </template>
          <template v-else-if="column.key === 'cost'">{{ formatMoney(record.cost) }}</template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="hr_training_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openRecords(record)">学员与成绩</a-button>
              <template v-if="userStore.hasPerm('hr:training:edit')">
                <a-popconfirm v-if="record.status === 'PLANNED'" title="开始培训？" @confirm="setStatus(record, 'ONGOING')">
                  <a-button type="link" size="small">开始</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="record.status === 'ONGOING'" title="结束培训？结束前请录入出勤和成绩" @confirm="setStatus(record, 'FINISHED')">
                  <a-button type="link" size="small">结束</a-button>
                </a-popconfirm>
                <a-dropdown v-if="['PLANNED', 'ONGOING'].includes(record.status)">
                  <a-button type="link" size="small">更多<DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => (key === 'edit' ? drawer.openEdit(record) : setStatus(record, 'CANCELED'))">
                      <a-menu-item key="edit">编辑</a-menu-item>
                      <a-menu-item key="cancel" danger>取消培训</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
              <span v-if="['PLANNED', 'CANCELED'].includes(record.status)" v-perm="'hr:training:remove'">
                <a-popconfirm :title="`确定删除「${record.title}」？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增培训" edit-title="编辑培训">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="培训主题" name="title"><a-input v-model:value="form.title" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="类别" name="category"><a-select v-model:value="form.category" :options="toOptions(dict.hr_training_category)" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="讲师" name="trainer"><a-input v-model:value="form.trainer" :maxlength="64" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="开始日期" name="startDate"><a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="结束日期" name="endDate"><a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="课时" name="hours"><a-input-number v-model:value="form.hours" :min="0" :precision="1" addon-after="小时" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="费用" name="cost"><a-input-number v-model:value="form.cost" :min="0" :precision="2" addon-after="元" class="full" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="地点" name="location"><a-input v-model:value="form.location" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="培训内容" name="content"><a-textarea v-model:value="form.content" :rows="4" :maxlength="1000" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <!-- 学员与成绩 -->
    <SubPage v-model:open="recordOpen" :title="`学员与成绩 - ${current?.title || ''}`" width="min(900px, 100vw)" destroy-on-close>
      <template v-if="current">
        <div class="record-head">
          <span>
            <DictTag type="hr_training_status" :value="current.status" />
            {{ current.startDate }} · {{ Number(current.hours) }} 课时 · {{ records.length }} 名学员
          </span>
          <span v-if="editableTrainees" v-perm="'hr:training:edit'" class="add-trainee">
            <EmployeeSelect v-model:value="addIds" multiple placeholder="搜索员工添加学员" class="trainee-select" />
            <a-button type="primary" :disabled="!addIds?.length" @click="doAddTrainees">添加</a-button>
          </span>
        </div>
        <a-alert v-if="current.status === 'PLANNED'" type="info" show-icon message="培训开始后可以录入出勤、成绩和评价。" class="mb" />
        <a-table row-key="id" size="small" :columns="recordColumns" :data-source="records" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'">
              <div>{{ record.employeeName }}</div>
              <div class="sub-text">{{ record.empNo }} · {{ record.deptName || '-' }}</div>
            </template>
            <template v-else-if="column.key === 'attended'">
              <a-switch v-model:checked="record.attendedBool" :disabled="!editableScores" checked-children="出勤" un-checked-children="缺勤" />
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
                :options="[{ value: 1, label: '通过' }, { value: 0, label: '未通过' }]"
              />
            </template>
            <template v-else-if="column.key === 'feedback'">
              <a-input v-model:value="record.feedback" :maxlength="255" :disabled="!editableScores" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm v-if="editableTrainees && userStore.hasPerm('hr:training:edit')" title="移除该学员？" @confirm="doRemove(record.id)">
                <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </template>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="recordOpen = false">关闭</a-button>
          <a-button v-if="editableScores" v-perm="'hr:training:edit'" type="primary" :loading="savingRecords" @click="saveRecords">保存出勤与成绩</a-button>
        </div>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
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

const userStore = useUserStore()
const dict = useDict('hr_training_category', 'hr_training_status')
const showSearch = ref(true)
const thisYear = dayjs().year()
const years = [thisYear + 1, thisYear, thisYear - 1, thisYear - 2].map((y) => ({ value: y, label: `${y} 年` }))

const columns = [
  { title: '培训', key: 'title', width: 260, fixed: 'left' },
  { title: '类别', key: 'category', width: 100, dict: 'hr_training_category' },
  { title: '日期', key: 'date', width: 200 },
  { title: '课时', dataIndex: 'hours', width: 70 },
  { title: '学员', key: 'trainees', width: 140 },
  { title: '费用', key: 'cost', width: 110, align: 'right' },
  { title: '状态', key: 'status', width: 90, dict: 'hr_training_status' },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
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
  title: [{ required: true, whitespace: true, message: '请输入培训主题' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }]
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
  message.success('已更新')
  load()
  if (current.value?.id === record.id) current.value = { ...current.value, status }
}

async function remove(id) {
  await deleteTraining(id)
  message.success('删除成功')
  load()
}

// 学员与成绩
const recordOpen = ref(false)
const current = ref()
const records = ref([])
const addIds = ref([])
const savingRecords = ref(false)
const editableTrainees = computed(() => ['PLANNED', 'ONGOING'].includes(current.value?.status))
const editableScores = computed(() => ['ONGOING', 'FINISHED'].includes(current.value?.status))
const recordColumns = [
  { title: '学员', key: 'employee', width: 180 },
  { title: '出勤', key: 'attended', width: 90 },
  { title: '成绩', key: 'score', width: 110 },
  { title: '结果', key: 'passed', width: 110 },
  { title: '评价', key: 'feedback' },
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
  message.success(n ? `已添加 ${n} 名学员` : '所选员工都已在名单中')
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
    message.success('已保存')
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
