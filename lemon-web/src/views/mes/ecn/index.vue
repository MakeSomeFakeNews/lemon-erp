<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="toOptions(dict.mes_ecn_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="变更单号、标题、产品" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'mes:ecn:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增工程变更
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
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ecnNo'">
            <a @click="openDetail(record.id)">{{ record.ecnNo }}</a>
            <div class="sub-text">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'product'">
            {{ record.materialCode }} {{ record.materialName }}
            <div class="sub-text">{{ record.fromVersion }} → {{ record.toVersion || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'reason'"><DictTag type="mes_ecn_reason" :value="record.reason" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="mes_ecn_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增工程变更" edit-title="编辑工程变更">
      <a-alert v-if="!form.id" type="info" show-icon class="block" message="保存后把基准 BOM 复制为新版本（停用），在物料清单中修改新版本的子件和工序，再回到这里提交。" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="变更标题" name="title">
          <a-input v-model:value="form.title" :maxlength="128" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="基准 BOM（变更前）" name="fromBomId">
              <a-select
                v-model:value="form.fromBomId"
                show-search
                :filter-option="false"
                :options="bomOptions"
                :disabled="!!form.id"
                placeholder="搜索产品编码、名称"
                @search="searchBoms"
                @focus="searchBoms('')"
                @change="onBomChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="新版本号" name="newVersion">
              <a-input v-model:value="form.newVersion" :maxlength="16" :disabled="!!form.id" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="变更原因" name="reason">
              <a-select v-model:value="form.reason" :options="toOptions(dict.mes_ecn_reason)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生效日期" name="effectiveDate" extra="批准后到这一天自动切换默认 BOM">
              <a-date-picker v-model:value="form.effectiveDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="syncDraftWo">
          <a-checkbox v-model:checked="form.syncDraftWo">生效时同步使用原版本的草稿工单（按新版本重新生成用料和工序）</a-checkbox>
        </a-form-item>
        <a-form-item label="变更说明" name="description">
          <a-textarea v-model:value="form.description" :rows="4" :maxlength="1000" placeholder="变更内容、原因、对库存和在制品的处理意见" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `工程变更 ${detail.ecnNo} ${detail.title}` : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">审批记录</a-button>
          <template v-if="detail.status === 'DRAFT'">
            <a-button v-perm="'mes:bom:edit'" @click="router.push({ path: '/mes/bom', query: { edit: detail.toBomId } })">修改变更后的 BOM</a-button>
            <a-button v-perm="'mes:ecn:edit'" @click="drawer.openEdit(detail)">编辑</a-button>
            <span v-perm="'mes:ecn:remove'">
              <a-popconfirm title="删除该变更？变更后的 BOM 版本一并删除" @confirm="remove"><a-button danger>删除</a-button></a-popconfirm>
            </span>
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm title="提交后变更后的 BOM 不能再修改，确定？" @confirm="submit"><a-button type="primary">提交</a-button></a-popconfirm>
            </span>
          </template>
          <template v-if="detail.status === 'APPROVED'">
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm title="取消该变更？变更后的 BOM 版本删除" @confirm="cancel"><a-button>取消变更</a-button></a-popconfirm>
            </span>
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm title="不等生效日期，立即切换默认 BOM？" @confirm="effect"><a-button type="primary">立即生效</a-button></a-popconfirm>
            </span>
          </template>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="产品">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ detail.fromVersion }} → {{ detail.toVersion }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="mes_ecn_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="原因"><DictTag type="mes_ecn_reason" :value="detail.reason" /></a-descriptions-item>
          <a-descriptions-item label="生效日期">{{ detail.effectiveDate }}</a-descriptions-item>
          <a-descriptions-item label="同步草稿工单">{{ detail.syncDraftWo === 1 ? '是' : '否' }}</a-descriptions-item>
          <a-descriptions-item label="发起">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="批准">{{ detail.approveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="生效">{{ detail.effectTime || '-' }} <span class="sub-text">{{ detail.resultNote || '' }}</span></a-descriptions-item>
          <a-descriptions-item v-if="detail.description" label="变更说明" :span="3"><span class="pre">{{ detail.description }}</span></a-descriptions-item>
        </a-descriptions>

        <div class="section-title">子件变化</div>
        <a-table row-key="materialId" size="small" :columns="itemDiffColumns" :data-source="diff.items" :pagination="false" class="block"
          :locale="{ emptyText: '子件没有变化' }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><a-tag :color="ACTIONS[record.action].color" :bordered="false">{{ ACTIONS[record.action].label }}</a-tag></template>
            <template v-else-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
            <template v-else-if="column.key === 'qty'"><Change :from="qty(record.oldQty)" :to="qty(record.newQty)" :unit="record.unit" /></template>
            <template v-else-if="column.key === 'loss'"><Change :from="pct(record.oldLossRate)" :to="pct(record.newLossRate)" /></template>
            <template v-else-if="column.key === 'refDes'"><Change :from="record.oldRefDes" :to="record.newRefDes" /></template>
          </template>
        </a-table>
        <div class="section-title">工序变化</div>
        <a-table row-key="seq" size="small" :columns="opDiffColumns" :data-source="diff.ops" :pagination="false" class="block"
          :locale="{ emptyText: '工序没有变化' }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><a-tag :color="ACTIONS[record.action].color" :bordered="false">{{ ACTIONS[record.action].label }}</a-tag></template>
            <template v-else-if="column.key === 'name'"><Change :from="record.oldName" :to="record.newName" /></template>
            <template v-else-if="column.key === 'wc'"><Change :from="record.oldWorkCenter" :to="record.newWorkCenter" /></template>
            <template v-else-if="column.key === 'hours'"><Change :from="qty(record.oldStdHours)" :to="qty(record.newStdHours)" /></template>
            <template v-else-if="column.key === 'rate'"><Change :from="qty(record.oldPieceRate)" :to="qty(record.newPieceRate)" /></template>
            <template v-else-if="column.key === 'fa'"><Change :from="fa(record.oldFirstArticle)" :to="fa(record.newFirstArticle)" /></template>
          </template>
        </a-table>
        <div class="section-title">附件（变更通知、图纸等）</div>
        <AttachmentPanel biz-type="mes_ecn" :biz-id="detail.id" :editable="userStore.hasPerm('mes:ecn:edit')" />
      </template>
    </SubPage>

    <InstanceDetailDrawer v-model:open="flowOpen" :instance-id="detail?.flowInstanceId" />
  </div>
</template>

<script setup>
import { defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import { getBom, pageBom } from '@/api/mes/base'
import { addEcn, cancelEcn, deleteEcn, ecnDiff, effectEcn, getEcn, pageEcn, submitEcn, updateEcn } from '@/api/mes/plm'
import { formatQty } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 工程变更（ECN）：复制基准 BOM 为新版本修改，审批后按生效日期切换默认 BOM */
const dict = useDict('mes_ecn_reason', 'mes_ecn_status')
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const ACTIONS = {
  ADDED: { label: '新增', color: 'green' },
  REMOVED: { label: '删除', color: 'red' },
  CHANGED: { label: '修改', color: 'blue' }
}

/** 变更前 → 变更后，相同时只显示一个 */
const Change = defineComponent({
  props: { from: [String, Number], to: [String, Number], unit: String },
  setup(props) {
    return () => {
      const f = props.from ?? '-'
      const t = props.to ?? '-'
      const u = props.unit ? ` ${props.unit}` : ''
      if (f === t) return h('span', `${f}${u}`)
      return h('span', [h('span', { class: 'old-val' }, `${f}`), ' → ', h('b', `${t}${u}`)])
    }
  }
})
const qty = (v) => (v == null ? null : formatQty(v))
const pct = (v) => (v == null ? null : `${Number(v)}%`)
const fa = (v) => (v == null ? null : v === 1 ? '首件检验' : '否')

const columns = [
  { title: '变更单号 / 标题', key: 'ecnNo', width: 240 },
  { title: '产品 / 版本', key: 'product', width: 240 },
  { title: '原因', key: 'reason', width: 100 },
  { title: '生效日期', dataIndex: 'effectiveDate', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '发起人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-ecn')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageEcn, query)

// ---------------------------------------------------------------- 表单
const rules = {
  title: [{ required: true, whitespace: true, message: '请填写变更标题' }],
  fromBomId: [{ required: true, message: '请选择基准 BOM' }],
  newVersion: [{ required: true, whitespace: true, message: '请填写新版本号' }],
  reason: [{ required: true, message: '请选择变更原因' }],
  effectiveDate: [{ required: true, message: '请选择生效日期' }]
}
const bomOptions = ref([])
const bomLabel = (b) => `${b.materialCode} ${b.materialName} ${b.version}${b.isDefault === 1 ? '（默认）' : ''}`
async function searchBoms(keyword) {
  const page = await pageBom({ keyword: keyword || undefined, status: 1, size: 20 })
  bomOptions.value = page.records.map((b) => ({ value: b.id, label: bomLabel(b), version: b.version }))
}
/** 建议的新版本号：末尾数字加 1（V1.0 → V1.1，V2 → V3），否则加后缀 */
function nextVersion(v) {
  const m = /^(.*?)(\d+)$/.exec(v || '')
  return m ? `${m[1]}${Number(m[2]) + 1}` : `${v || 'V'}-1`
}
function onBomChange(id) {
  const o = bomOptions.value.find((x) => x.value === id)
  if (o) form.value.newVersion = nextVersion(o.version)
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, title: '', fromBomId: undefined, newVersion: '', reason: 'DESIGN', effectiveDate: dayjs().format('YYYY-MM-DD'),
    syncDraftWo: true, description: ''
  }),
  fetch: async (record) => {
    const e = await getEcn(record.id)
    bomOptions.value = [{ value: e.fromBomId, label: `${e.materialCode} ${e.materialName} ${e.fromVersion}` }]
    return { ...e, newVersion: e.toVersion, syncDraftWo: e.syncDraftWo === 1 }
  },
  create: async (data) => {
    const id = await addEcn(data)
    openDetail(id)
  },
  update: updateEcn,
  toPayload: (f) => ({
    title: f.title, fromBomId: f.fromBomId, newVersion: f.newVersion, reason: f.reason, description: f.description || null,
    effectiveDate: f.effectiveDate, syncDraftWo: !!f.syncDraftWo
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer

onMounted(async () => {
  load()
  // 从物料清单「发起变更」进入
  const bomId = Number(route.query.bomId)
  if (bomId) {
    const b = await getBom(bomId)
    bomOptions.value = [{ value: b.id, label: bomLabel(b), version: b.version }]
    drawer.openCreate({ fromBomId: b.id, newVersion: nextVersion(b.version), title: `${b.materialName} 变更` })
  }
})

// ---------------------------------------------------------------- 详情
const itemDiffColumns = [
  { title: '变化', key: 'action', width: 80 },
  { title: '子件', key: 'material', width: 240 },
  { title: '用量', key: 'qty', width: 160 },
  { title: '损耗率', key: 'loss', width: 120 },
  { title: '位号', key: 'refDes' }
]
const opDiffColumns = [
  { title: '变化', key: 'action', width: 80 },
  { title: '工序号', dataIndex: 'seq', width: 80 },
  { title: '工序名称', key: 'name', width: 160 },
  { title: '工作中心', key: 'wc', width: 180 },
  { title: '标准工时', key: 'hours', width: 120 },
  { title: '计件单价', key: 'rate', width: 120 },
  { title: '首件检验', key: 'fa', width: 140 }
]
const detailOpen = ref(false)
const detail = ref()
const diff = ref({ items: [], ops: [] })
const flowOpen = ref(false)
async function openDetail(id) {
  const [e, d] = await Promise.all([getEcn(id), ecnDiff(id)])
  detail.value = e
  diff.value = d
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) await openDetail(detail.value.id)
}
async function submit() {
  const approving = await submitEcn(detail.value.id)
  message.success(approving ? '已提交审批' : '已批准')
  refreshDetail()
  load()
}
async function effect() {
  await effectEcn(detail.value.id)
  message.success('已生效，新版本已接替默认 BOM')
  refreshDetail()
  load()
}
async function cancel() {
  await cancelEcn(detail.value.id)
  message.success('已取消')
  refreshDetail()
  load()
}
async function remove() {
  await deleteEcn(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 240px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.section-title {
  margin: 8px 0;
  font-weight: 600;
}
.pre {
  white-space: pre-wrap;
}
:deep(.old-val) {
  color: var(--app-text-3);
  text-decoration: line-through;
}
</style>
