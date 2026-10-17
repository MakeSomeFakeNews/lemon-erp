<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('mes.doc.allStatus')" allow-clear :options="toOptions(dict.mes_ecn_status)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('mes.ecn.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'mes:ecn:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('mes.ecn.add') }}
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
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.ecn.add')" :edit-title="$t('mes.ecn.edit')">
      <a-alert v-if="!form.id" type="info" show-icon class="block" :message="$t('mes.ecn.saveTip')" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('mes.ecn.title')" name="title">
          <a-input v-model:value="form.title" :maxlength="128" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item :label="$t('mes.ecn.fromBom')" name="fromBomId">
              <a-select
                v-model:value="form.fromBomId"
                show-search
                :filter-option="false"
                :options="bomOptions"
                :disabled="!!form.id"
                :placeholder="$t('mes.ecn.bomSearchPh')"
                @search="searchBoms"
                @focus="searchBoms('')"
                @change="onBomChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('mes.ecn.newVersion')" name="newVersion">
              <a-input v-model:value="form.newVersion" :maxlength="16" :disabled="!!form.id" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.ecn.reason')" name="reason">
              <a-select v-model:value="form.reason" :options="toOptions(dict.mes_ecn_reason)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('mes.ecn.effectiveDate')" name="effectiveDate" :extra="$t('mes.ecn.effectiveDateExtra')">
              <a-date-picker v-model:value="form.effectiveDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item name="syncDraftWo">
          <a-checkbox v-model:checked="form.syncDraftWo">{{ $t('mes.ecn.syncDraftWoLong') }}</a-checkbox>
        </a-form-item>
        <a-form-item :label="$t('mes.ecn.description')" name="description">
          <a-textarea v-model:value="form.description" :rows="4" :maxlength="1000" :placeholder="$t('mes.ecn.descriptionPh')" />
        </a-form-item>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('mes.ecn.detailTitle', { no: detail.ecnNo, title: detail.title }) : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">{{ $t('mes.ecn.flowRecord') }}</a-button>
          <template v-if="detail.status === EcnStatus.DRAFT">
            <a-button v-perm="'mes:bom:edit'" @click="router.push({ path: '/mes/bom', query: { edit: detail.toBomId } })">{{ $t('mes.ecn.editToBom') }}</a-button>
            <a-button v-perm="'mes:ecn:edit'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'mes:ecn:remove'">
              <a-popconfirm :title="$t('mes.ecn.deleteConfirm')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm :title="$t('mes.ecn.submitConfirm')" @confirm="submit"><a-button type="primary">{{ $t('common.action.submit') }}</a-button></a-popconfirm>
            </span>
          </template>
          <template v-if="detail.status === EcnStatus.APPROVED">
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm :title="$t('mes.ecn.cancelConfirm')" @confirm="cancel"><a-button>{{ $t('mes.ecn.cancel') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'mes:ecn:approve'">
              <a-popconfirm :title="$t('mes.ecn.effectConfirm')" @confirm="effect"><a-button type="primary">{{ $t('mes.ecn.effectNow') }}</a-button></a-popconfirm>
            </span>
          </template>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('mes.bom.product')">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.bom.version')">{{ detail.fromVersion }} → {{ detail.toVersion }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="mes_ecn_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.reasonShort')"><DictTag type="mes_ecn_reason" :value="detail.reason" /></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.effectiveDate')">{{ detail.effectiveDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.syncDraftWo')">{{ detail.syncDraftWo === 1 ? $t('common.status.yes') : $t('common.status.no') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.initiated')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.approved')">{{ detail.approveTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.ecn.effected')">{{ detail.effectTime || '-' }} <span class="sub-text">{{ detail.resultNote || '' }}</span></a-descriptions-item>
          <a-descriptions-item v-if="detail.description" :label="$t('mes.ecn.description')" :span="3"><span class="pre">{{ detail.description }}</span></a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('mes.ecn.itemChanges') }}</div>
        <a-table row-key="materialId" size="small" :columns="itemDiffColumns" :data-source="diff.items" :pagination="false" class="block"
          :locale="{ emptyText: $t('mes.ecn.noItemChanges') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><a-tag :color="ACTIONS[record.action].color" :bordered="false">{{ ACTIONS[record.action].label }}</a-tag></template>
            <template v-else-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
            <template v-else-if="column.key === 'qty'"><Change :from="qty(record.oldQty)" :to="qty(record.newQty)" :unit="record.unit" /></template>
            <template v-else-if="column.key === 'loss'"><Change :from="pct(record.oldLossRate)" :to="pct(record.newLossRate)" /></template>
            <template v-else-if="column.key === 'refDes'"><Change :from="record.oldRefDes" :to="record.newRefDes" /></template>
          </template>
        </a-table>
        <div class="section-title">{{ $t('mes.ecn.opChanges') }}</div>
        <a-table row-key="seq" size="small" :columns="opDiffColumns" :data-source="diff.ops" :pagination="false" class="block"
          :locale="{ emptyText: $t('mes.ecn.noOpChanges') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><a-tag :color="ACTIONS[record.action].color" :bordered="false">{{ ACTIONS[record.action].label }}</a-tag></template>
            <template v-else-if="column.key === 'name'"><Change :from="record.oldName" :to="record.newName" /></template>
            <template v-else-if="column.key === 'wc'"><Change :from="record.oldWorkCenter" :to="record.newWorkCenter" /></template>
            <template v-else-if="column.key === 'hours'"><Change :from="qty(record.oldStdHours)" :to="qty(record.newStdHours)" /></template>
            <template v-else-if="column.key === 'rate'"><Change :from="qty(record.oldPieceRate)" :to="qty(record.newPieceRate)" /></template>
            <template v-else-if="column.key === 'fa'"><Change :from="fa(record.oldFirstArticle)" :to="fa(record.newFirstArticle)" /></template>
          </template>
        </a-table>
        <div class="section-title">{{ $t('mes.ecn.attachments') }}</div>
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
import { useI18n } from 'vue-i18n'
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

import { EcnStatus } from '@/constants/enums'
/** 工程变更（ECN）：复制基准 BOM 为新版本修改，审批后按生效日期切换默认 BOM */
const dict = useDict('mes_ecn_reason', 'mes_ecn_status')
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const ACTIONS = {
  ADDED: { label: t('mes.ecn.action.ADDED'), color: 'green' },
  REMOVED: { label: t('mes.ecn.action.REMOVED'), color: 'red' },
  CHANGED: { label: t('mes.ecn.action.CHANGED'), color: 'blue' }
}

/** 变更前 → 变更后，相同时只显示一个 */
const Change = defineComponent({
  props: { from: [String, Number], to: [String, Number], unit: String },
  setup(props) {
    return () => {
      const f = props.from ?? '-'
      const to = props.to ?? '-'
      const u = props.unit ? ` ${props.unit}` : ''
      if (f === to) return h('span', `${f}${u}`)
      return h('span', [h('span', { class: 'old-val' }, `${f}`), ' → ', h('b', `${to}${u}`)])
    }
  }
})
const qty = (v) => (v == null ? null : formatQty(v))
const pct = (v) => (v == null ? null : `${Number(v)}%`)
const fa = (v) => (v == null ? null : v === 1 ? t('mes.bom.firstArticle') : t('common.status.no'))

const columns = [
  { title: t('mes.ecn.noTitle'), key: 'ecnNo', width: 240 },
  { title: t('mes.ecn.productVersion'), key: 'product', width: 240 },
  { title: t('mes.ecn.reasonShort'), key: 'reason', width: 120 },
  { title: t('mes.ecn.effectiveDate'), dataIndex: 'effectiveDate', width: 120 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('mes.ecn.initiator'), dataIndex: 'createByName', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 80, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-ecn')
const query = reactive({ status: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageEcn, query)

// ---------------------------------------------------------------- 表单
const rules = {
  title: [{ required: true, whitespace: true, message: t('mes.ecn.titleRequired') }],
  fromBomId: [{ required: true, message: t('mes.ecn.fromBomRequired') }],
  newVersion: [{ required: true, whitespace: true, message: t('mes.ecn.newVersionRequired') }],
  reason: [{ required: true, message: t('mes.ecn.reasonRequired') }],
  effectiveDate: [{ required: true, message: t('mes.ecn.effectiveDateRequired') }]
}
const bomOptions = ref([])
const bomLabel = (b) => `${b.materialCode} ${b.materialName} ${b.isDefault === 1 ? t('mes.wo.bomDefault', { v: b.version }) : b.version}`
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
    drawer.openCreate({ fromBomId: b.id, newVersion: nextVersion(b.version), title: t('mes.ecn.defaultTitle', { name: b.materialName }) })
  }
})

// ---------------------------------------------------------------- 详情
const itemDiffColumns = [
  { title: t('mes.ecn.change'), key: 'action', width: 90 },
  { title: t('mes.bom.item'), key: 'material', width: 240 },
  { title: t('mes.ecn.qty'), key: 'qty', width: 160 },
  { title: t('mes.bom.lossRate'), key: 'loss', width: 120 },
  { title: t('mes.bom.refDes'), key: 'refDes' }
]
const opDiffColumns = [
  { title: t('mes.ecn.change'), key: 'action', width: 90 },
  { title: t('mes.wo.opSeq'), dataIndex: 'seq', width: 80 },
  { title: t('mes.bom.opName'), key: 'name', width: 160 },
  { title: t('mes.wo.workCenter'), key: 'wc', width: 180 },
  { title: t('mes.bom.stdHours'), key: 'hours', width: 130 },
  { title: t('mes.bom.pieceRate'), key: 'rate', width: 120 },
  { title: t('mes.bom.firstArticle'), key: 'fa', width: 170 }
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
  message.success(approving ? t('mes.ecn.submittedForApproval') : t('mes.ecn.approvedMsg'))
  refreshDetail()
  load()
}
async function effect() {
  await effectEcn(detail.value.id)
  message.success(t('mes.ecn.effectedMsg'))
  refreshDetail()
  load()
}
async function cancel() {
  await cancelEcn(detail.value.id)
  message.success(t('mes.ecn.cancelled'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteEcn(detail.value.id)
  message.success(t('common.msg.deleted'))
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
