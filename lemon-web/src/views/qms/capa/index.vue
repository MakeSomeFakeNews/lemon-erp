<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.qms_capa_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="单号、标题" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:capa:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增 8D
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
            <div v-if="overdue(record)" class="sub-text danger">已逾期</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="qms_capa_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ record.status === 'OPEN' ? '处理' : '查看' }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增纠正预防（8D）" edit-title="编辑纠正预防">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="来源" name="source">
          <a-radio-group v-model:value="form.source" :options="toOptions(dict.qms_capa_source)" />
        </a-form-item>
        <a-form-item v-if="form.source === 'NCR'" label="不合格品处理单" name="ncrId">
          <a-select
            v-model:value="form.ncrId"
            show-search
            :filter-option="false"
            :options="ncrOptions"
            placeholder="搜索处理单号、物料"
            @search="searchNcr"
            @focus="searchNcr('')"
          />
        </a-form-item>
        <a-form-item label="问题标题" name="title">
          <a-input v-model:value="form.title" :maxlength="128" />
        </a-form-item>
        <a-form-item label="问题描述" name="description">
          <a-textarea v-model:value="form.description" :rows="3" :maxlength="1000" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="负责人" name="ownerId" extra="默认为各步骤的负责人">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="要求完成日期" name="dueDate">
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
            <a-button v-perm="'qms:capa:add'" @click="drawer.openEdit(detail)">编辑</a-button>
            <span v-perm="'qms:capa:remove'">
              <a-popconfirm title="删除该纠正预防单？" @confirm="remove">
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </span>
            <span v-perm="'qms:capa:edit'">
              <a-popconfirm title="D1 ~ D8 已全部完成，提交效果验证？" :disabled="doneCount < 8" @confirm="submit">
                <a-tooltip :title="doneCount < 8 ? `还有 ${8 - doneCount} 个步骤未完成` : ''">
                  <a-button type="primary" :disabled="doneCount < 8">提交验证</a-button>
                </a-tooltip>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'VERIFYING'" v-perm="'qms:capa:verify'" type="primary" @click="openVerify">效果验证</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="来源"><DictTag type="qms_capa_source" :value="detail.source" /> {{ detail.ncrNo || '' }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detail.ownerName }}</a-descriptions-item>
          <a-descriptions-item label="要求完成">
            <span :class="{ danger: overdue(detail) }">{{ detail.dueDate }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="qms_capa_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="登记">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="进度">{{ doneCount }} / 8</a-descriptions-item>
          <a-descriptions-item v-if="detail.description" label="问题描述" :span="3">{{ detail.description }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.verifyResult" label="效果验证" :span="3">
            {{ detail.verifyResult }} <span class="sub-text">{{ detail.verifiedByName }} {{ detail.verifyTime }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-timeline class="steps">
          <a-timeline-item v-for="s in detail.steps" :key="s.id" :color="s.done === 1 ? 'green' : 'gray'" class="step">
            <div>
              <span class="step-title">{{ s.step }} {{ STEP_NAMES[s.step] }}</span>
              <span class="sub-text step-meta">{{ s.ownerName || '' }}{{ s.doneDate ? ` · ${s.doneDate} 完成` : '' }}</span>
            </div>
            <div class="step-hint sub-text">{{ STEP_HINTS[s.step] }}</div>
            <template v-if="editable && editing[s.id]">
              <a-textarea v-model:value="editing[s.id].content" :rows="3" :maxlength="2000" class="step-input" />
              <div class="step-actions">
                <IdentityUserSelect v-model:value="editing[s.id].ownerId" :label="s.ownerName" class="owner" />
                <a-checkbox v-model:checked="editing[s.id].done">已完成</a-checkbox>
                <a-button size="small" @click="delete editing[s.id]">取消</a-button>
                <a-button size="small" type="primary" :loading="stepSaving" @click="saveStep(s)">保存</a-button>
              </div>
            </template>
            <template v-else>
              <div class="step-content">{{ s.content || '未填写' }}</div>
              <a-button v-if="editable" v-perm="'qms:capa:edit'" type="link" size="small" class="edit-link" @click="editStep(s)">填写</a-button>
            </template>
          </a-timeline-item>
        </a-timeline>
      </template>
    </SubPage>

    <a-modal v-model:open="verifyOpen" title="效果验证" :confirm-loading="saving" @ok="submitVerify">
      <a-form layout="vertical">
        <a-form-item label="验证结论" required>
          <a-radio-group v-model:value="verify.effective">
            <a-radio :value="true">有效，关闭</a-radio>
            <a-radio :value="false">无效，退回继续处理</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="验证说明" required>
          <a-textarea v-model:value="verify.verifyResult" :rows="3" :maxlength="500" placeholder="如：连续 3 批来料检验无同类不良" />
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

const STEP_NAMES = {
  D1: '组建团队', D2: '问题描述', D3: '临时措施', D4: '根本原因', D5: '永久措施', D6: '实施验证', D7: '预防再发', D8: '总结表彰'
}
const STEP_HINTS = {
  D1: '成立跨部门小组，明确组长与成员分工',
  D2: '用 5W2H 描述问题：什么产品、什么不良、数量、何时何地发现',
  D3: '围堵措施：隔离、挑选在库与在途产品，防止不良流出',
  D4: '用鱼骨图、5Why 分析产生原因与流出原因，并验证',
  D5: '针对根本原因制定永久纠正措施，评估风险',
  D6: '实施永久措施并跟踪效果，撤销临时措施',
  D7: '更新作业指导书、FMEA、控制计划，横向展开到类似产品与工序',
  D8: '总结经验，认可团队贡献，关闭问题'
}

const columns = [
  { title: '单号', key: 'capaNo', width: 170 },
  { title: '问题标题', dataIndex: 'title', width: 260, ellipsis: true },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '进度', key: 'progress', width: 160 },
  { title: '要求完成', key: 'dueDate', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '登记时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-capa')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageCapa, query)

const overdue = (r) => r.status !== 'CLOSED' && dayjs(r.dueDate).isBefore(dayjs(), 'day')

// ---------------------------------------------------------------- 新增 / 编辑
const rules = {
  source: [{ required: true, message: '请选择来源' }],
  ncrId: [{ required: true, message: '请选择不合格品处理单' }],
  title: [{ required: true, whitespace: true, message: '请填写问题标题' }],
  ownerId: [{ required: true, message: '请选择负责人' }],
  dueDate: [{ required: true, message: '请选择要求完成日期' }]
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
    message.success('已保存')
    await refreshDetail()
    load()
  } finally {
    stepSaving.value = false
  }
}
async function submit() {
  await submitCapa(detail.value.id)
  message.success('已提交效果验证')
  refreshDetail()
  load()
}
async function remove() {
  await deleteCapa(detail.value.id)
  message.success('已删除')
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
  if (!verify.value.verifyResult?.trim()) return message.warning('请填写验证说明')
  saving.value = true
  try {
    await verifyCapa(detail.value.id, verify.value)
    message.success(verify.value.effective ? '已关闭' : '已退回继续处理')
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
