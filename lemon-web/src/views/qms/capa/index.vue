<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('mes.doc.allStatus')" allow-clear :options="toOptions(dict.qms_capa_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('qms.capa.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:capa:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('qms.capa.add') }}
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'capaNo'">
            <a @click="openDetail(record.id)">{{ record.capaNo }}</a>
            <div class="sub-text"><DictTag type="qms_capa_source" :value="record.source" />{{ record.ncrNo ? ' ' + record.ncrNo : '' }}</div>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress :percent="Math.round(((record.doneSteps || 0) / 8) * 100)" size="small" :format="() => `${record.doneSteps || 0}/8`" />
          </template>
          <template v-else-if="column.key === 'dueDate'">
            <div :class="{ danger: overdue(record) }">{{ record.dueDate }}</div>
            <div v-if="overdue(record)" class="sub-text danger">{{ $t('qms.capa.overdue') }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="qms_capa_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === 'OPEN' ? $t('qms.capa.handle') : $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('qms.capa.addTitle')" :edit-title="$t('qms.capa.editTitle')">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('qms.capa.source')" name="source">
          <a-radio-group v-model:value="form.source" :options="toOptions(dict.qms_capa_source)" />
        </a-form-item>
        <a-form-item v-if="form.source === 'NCR'" :label="$t('qms.capa.ncr')" name="ncrId">
          <a-select
            v-model:value="form.ncrId"
            show-search
            :filter-option="false"
            :options="ncrOptions"
            :placeholder="$t('qms.capa.ncrSearchPh')"
            @search="searchNcr"
            @focus="searchNcr('')"
          />
        </a-form-item>
        <a-form-item :label="$t('qms.capa.title')" name="title">
          <a-input v-model:value="form.title" :maxlength="128" />
        </a-form-item>
        <a-form-item :label="$t('qms.capa.description')" name="description">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('qms.capa.owner')" name="ownerId" :extra="$t('qms.capa.ownerExtra')">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('qms.capa.dueDate')" name="dueDate">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `8D ${detail.capaNo} ${detail.title}` : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <template v-if="detail.status === 'OPEN'">
            <a-button v-perm="'qms:capa:add'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'qms:capa:remove'">
              <a-popconfirm :title="$t('qms.capa.deleteConfirm')" @confirm="remove">
                <a-button danger>{{ $t('common.action.delete') }}</a-button>
              </a-popconfirm>
            </span>
            <span v-perm="'qms:capa:edit'">
              <a-popconfirm :title="$t('qms.capa.submitConfirm')" :disabled="doneCount < 8" @confirm="submit">
                <a-tooltip :title="doneCount < 8 ? $t('qms.capa.stepsLeft', { n: 8 - doneCount }) : ''">
                  <a-button type="primary" :disabled="doneCount < 8">{{ $t('qms.capa.submitVerify') }}</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'VERIFYING'" v-perm="'qms:capa:verify'" type="primary" @click="openVerify">{{ $t('qms.capa.verify') }}</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('qms.capa.source')"><DictTag type="qms_capa_source" :value="detail.source" /> {{ detail.ncrNo || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.capa.owner')">{{ detail.ownerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('qms.capa.due')">
            <span :class="{ danger: overdue(detail) }">{{ detail.dueDate }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="qms_capa_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('qms.capa.registered')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.opProgress')">{{ doneCount }} / 8</a-descriptions-item>
          <a-descriptions-item v-if="detail.description" :label="$t('qms.capa.description')" :span="3">{{ detail.description }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.verifyResult" :label="$t('qms.capa.verify')" :span="3">
            {{ detail.verifyResult }} <span class="sub-text">{{ detail.verifiedByName }} {{ detail.verifyTime }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-timeline class="steps">
          <a-timeline-item v-for="s in detail.steps" :key="s.id" :color="s.done === 1 ? 'green' : 'gray'" class="step">
            <div>
              <span class="step-title">{{ s.step }} {{ STEP_NAMES[s.step] }}</span>
              <span class="sub-text step-meta">{{ s.ownerName || '' }}{{ s.doneDate ? $t('qms.capa.doneOn', { d: s.doneDate }) : '' }}</span>
            </div>
            <div class="step-hint sub-text">{{ STEP_HINTS[s.step] }}</div>
            <template v-if="editable && editing[s.id]">
              <a-textarea v-model:value="editing[s.id].content" :rows="3" :maxlength="2000" class="step-input" />
              <div class="step-actions">
                <IdentityUserSelect v-model:value="editing[s.id].ownerId" :label="s.ownerName" class="owner" />
                <a-checkbox v-model:checked="editing[s.id].done">{{ $t('qms.capa.done') }}</a-checkbox>
                <a-button size="small" @click="delete editing[s.id]">{{ $t('common.action.cancel') }}</a-button>
                <a-button size="small" type="primary" :loading="stepSaving" @click="saveStep(s)">{{ $t('common.action.save') }}</a-button>
              </div>
            </template>
            <template v-else>
              <div class="step-content">{{ s.content || $t('qms.capa.notFilled') }}</div>
              <a-button v-if="editable" v-perm="'qms:capa:edit'" type="link" size="small" class="edit-link" @click="editStep(s)">{{ $t('qms.capa.fill') }}</a-button>
            </template>
          </a-timeline-item>
        </a-timeline>
      </template>
    </SubPage>

    <a-modal v-model:open="verifyOpen" :title="$t('qms.capa.verify')" :confirm-loading="saving" @ok="submitVerify">
      <a-form layout="vertical">
        <a-form-item :label="$t('qms.capa.verifyConclusion')" required>
          <a-radio-group v-model:value="verify.effective">
            <a-radio :value="true">{{ $t('qms.capa.effective') }}</a-radio>
            <a-radio :value="false">{{ $t('qms.capa.ineffective') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('qms.capa.verifyDesc')" required>
          <a-textarea v-model:value="verify.verifyResult" :rows="3" :maxlength="500" :placeholder="$t('qms.capa.verifyPh')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { addCapa, deleteCapa, getCapa, pageCapa, pageNcr, saveCapaStep, submitCapa, updateCapa, verifyCapa } from '@/api/qms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 纠正预防（8D）：D1 ~ D8 逐步填写，全部完成后效果验证 */
const dict = useDict('qms_capa_source', 'qms_capa_status')
const route = useRoute()
const userStore = useUserStore()
const { t } = useI18n()

const STEP_NAMES = {
  D1: t('qms.capa.step.D1'), D2: t('qms.capa.step.D2'), D3: t('qms.capa.step.D3'), D4: t('qms.capa.step.D4'), D5: t('qms.capa.step.D5'), D6: t('qms.capa.step.D6'), D7: t('qms.capa.step.D7'), D8: t('qms.capa.step.D8')
}
const STEP_HINTS = {
  D1: t('qms.capa.hint.D1'),
  D2: t('qms.capa.hint.D2'),
  D3: t('qms.capa.hint.D3'),
  D4: t('qms.capa.hint.D4'),
  D5: t('qms.capa.hint.D5'),
  D6: t('qms.capa.hint.D6'),
  D7: t('qms.capa.hint.D7'),
  D8: t('qms.capa.hint.D8')
}

const columns = [
  { title: t('qms.capa.no'), key: 'capaNo', width: 170 },
  { title: t('qms.capa.title'), dataIndex: 'title', width: 260, ellipsis: true },
  { title: t('qms.capa.owner'), dataIndex: 'ownerName', width: 100 },
  { title: t('mes.wo.opProgress'), key: 'progress', width: 160 },
  { title: t('qms.capa.due'), key: 'dueDate', width: 110 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('qms.capa.registerTime'), dataIndex: 'createTime', width: 160 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-capa')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageCapa, query)

const overdue = (r) => r.status !== 'CLOSED' && dayjs(r.dueDate).isBefore(dayjs(), 'day')

// ---------------------------------------------------------------- 新增 / 编辑
const rules = {
  source: [{ required: true, message: t('qms.capa.sourceRequired') }],
  ncrId: [{ required: true, message: t('qms.capa.ncrRequired') }],
  title: [{ required: true, whitespace: true, message: t('qms.capa.titleRequired') }],
  ownerId: [{ required: true, message: t('qms.capa.ownerRequired') }],
  dueDate: [{ required: true, message: t('qms.capa.dueDateRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, source: 'INTERNAL', ncrId: undefined, title: '', description: '', ownerId: userStore.user?.id,
    ownerName: userStore.user?.nickname, dueDate: dayjs().add(30, 'day').format('YYYY-MM-DD')
  }),
  fetch: async (record) => {
    const c = await getCapa(record.id)
    if (c.ncrId) ncrOptions.value = [{ value: c.ncrId, label: c.ncrNo }]
    return c
  },
  create: addCapa,
  update: updateCapa,
  toPayload: (f) => ({
    source: f.source,
    ncrId: f.source === 'NCR' ? f.ncrId : null,
    title: f.title,
    description: f.description || null,
    ownerId: f.ownerId,
    dueDate: f.dueDate
  }),
  onSuccess: () => {
    load()
    refreshDetail()
  }
})
const { formRef, form } = drawer

const ncrOptions = ref([])
async function searchNcr(keyword) {
  const page = await pageNcr({ keyword: keyword || undefined, size: 20 })
  ncrOptions.value = page.records.map((n) => ({ value: n.id, label: `${n.ncrNo} ${n.materialName} ${n.description || ''}`.trim() }))
}

onMounted(async () => {
  await load()
  // 从不合格品处理单「发起 8D」进入
  const ncrId = Number(route.query.ncrId)
  if (ncrId) {
    await searchNcr('')
    drawer.openCreate({ source: 'NCR', ncrId })
  }
})

// ---------------------------------------------------------------- 详情与步骤
const detailOpen = ref(false)
const detail = ref()
const editable = computed(() => detail.value?.status === 'OPEN')
const doneCount = computed(() => (detail.value?.steps || []).filter((s) => s.done === 1).length)
const editing = reactive({})
const stepSaving = ref(false)
async function openDetail(id) {
  Object.keys(editing).forEach((k) => delete editing[k])
  detail.value = await getCapa(id)
  detailOpen.value = true
}
async function refreshDetail() {
  if (detailOpen.value && detail.value) detail.value = await getCapa(detail.value.id)
}
function editStep(s) {
  editing[s.id] = { content: s.content || '', ownerId: s.ownerId, done: s.done === 1 }
}
async function saveStep(s) {
  stepSaving.value = true
  try {
    await saveCapaStep(detail.value.id, s.id, editing[s.id])
    delete editing[s.id]
    message.success(t('common.msg.saved'))
    await refreshDetail()
    load()
  } finally {
    stepSaving.value = false
  }
}
async function submit() {
  await submitCapa(detail.value.id)
  message.success(t('qms.capa.submitted'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteCapa(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}

const verifyOpen = ref(false)
const verify = ref({})
const saving = ref(false)
function openVerify() {
  verify.value = { effective: true, verifyResult: '' }
  verifyOpen.value = true
}
async function submitVerify() {
  if (!verify.value.verifyResult?.trim()) return message.warning(t('qms.capa.verifyDescRequired'))
  saving.value = true
  try {
    await verifyCapa(detail.value.id, verify.value)
    message.success(verify.value.effective ? t('mes.wo.closed') : t('qms.capa.returned'))
    verifyOpen.value = false
    refreshDetail()
    load()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 240px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--status-critical);
}
.steps {
  padding: 4px 4px 0;
}
.step-title {
  font-weight: 600;
}
.step-meta {
  margin-left: 8px;
  font-weight: normal;
}
.step-hint {
  margin-bottom: 4px;
}
.step-content {
  white-space: pre-wrap;
  color: var(--app-text-1, inherit);
}
.step-input {
  margin: 4px 0 8px;
}
.step-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.owner {
  width: 200px;
}
.edit-link {
  padding: 0;
}
</style>
