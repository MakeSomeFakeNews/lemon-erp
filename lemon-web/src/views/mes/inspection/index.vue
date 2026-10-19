<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.type" :placeholder="$t('mes.inspection.type')" allow-clear :options="toOptions(dict.mes_qc_type)" class="filter" @change="search" />
        <a-select v-model:value="query.result" :placeholder="$t('mes.inspection.result')" allow-clear :options="toOptions(dict.mes_qc_result)" class="filter" @change="search" />
        <a-select v-model:value="query.status" :placeholder="$t('common.field.status')" allow-clear :options="statusOptions" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('mes.inspection.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="search" />
        <template #extra>
          <a-button v-perm="'mes:qc:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('mes.inspection.add') }}
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
        :scroll="{ x: 1130, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'inspNo'">
            <a @click="openDetail(record)">{{ record.inspNo }}</a>
            <div><DictTag type="mes_qc_type" :value="record.type" /><span v-if="record.planName" class="sub-text">{{ record.planName }}</span></div>
          </template>
          <template v-else-if="column.key === 'source'">
            <div>{{ record.sourceNo || '-' }}</div>
            <div class="sub-text">{{ record.supplierName || (record.opName ? $t('mes.inspection.opName', { name: record.opName }) : record.woNo ? $t('mes.inspection.wo') : '') }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.batchNo ? $t('mes.inspection.batchNo', { no: record.batchNo }) : '' }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            {{ $t('mes.inspection.inspectSample', { inspect: formatQty(record.inspectQty), sample: formatQty(record.sampleQty) }) }}
            <div :class="['sub-text', { danger: Number(record.defectQty) > 0 }]">
              {{ $t('mes.inspection.defectN', { n: formatQty(record.defectQty) }) }}<template v-if="record.rejectQty != null"> · Ac {{ record.acceptQty }} / Re {{ record.rejectQty }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'result'">
            <DictTag type="mes_qc_result" :value="record.result" />
            <a-tag v-if="record.status !== InspectionStatus.CONFIRMED" color="orange" :bordered="false">{{ $t('mes.inspection.draft') }}</a-tag>
            <div v-if="record.defectType" class="sub-text"><DictTag type="mes_defect_type" :value="record.defectType" /></div>
          </template>
          <template v-else-if="column.key === 'inspectDate'">
            {{ record.inspectDate }}
            <div class="sub-text">{{ record.createByName }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status === InspectionStatus.CONFIRMED" type="link" size="small" @click="openDetail(record)">{{ $t('common.action.view') }}</a-button>
              <template v-else>
                <a-button v-perm="'mes:qc:edit'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                <span v-perm="'mes:qc:edit'">
                  <a-popconfirm :title="confirmTitle(record)" @confirm="confirm(record.id)">
                    <a-button type="link" size="small">{{ $t('mes.plan.confirm') }}</a-button>
                  </a-popconfirm>
                </span>
                <span v-perm="'mes:qc:remove'">
                  <a-popconfirm :title="$t('mes.inspection.confirmDelete')" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('mes.inspection.addTitle')" :edit-title="$t('mes.inspection.editTitle')" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.inspection.type')" name="type">
              <a-radio-group v-model:value="form.type" :options="toOptions(dict.mes_qc_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="form.type === QcType.IQC ? $t('mes.inspection.receiptNo') : $t('mes.inspection.woNo')" name="sourceNo" :extra="sourceExtra">
              <a-input v-model:value="form.sourceNo" :maxlength="32" :placeholder="form.type === QcType.FAI ? '' : $t('mes.woDocForm.optional')" @blur="loadOps" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type === QcType.FAI" :xs="24" :sm="6">
            <a-form-item :label="$t('mes.wo.op')" name="woOpId">
              <a-select v-model:value="form.woOpId" :options="opOptions" :placeholder="$t('mes.inspection.woFirst')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.wo.material')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('mes.woDocForm.batchNo')" name="batchNo">
              <a-input v-model:value="form.batchNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('mes.inspection.inspectQty')" name="inspectQty">
              <a-input-number v-model:value="form.inspectQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('mes.inspection.inspectDate')" name="inspectDate">
              <a-date-picker v-model:value="form.inspectDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('mes.inspection.defectType')" name="defectType">
              <a-select v-model:value="form.defectType" allow-clear :options="toOptions(dict.mes_defect_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('mes.inspection.defectDesc')" name="defectDesc">
              <a-input v-model:value="form.defectDesc" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 按检验方案逐项录入 -->
        <template v-if="usePlan">
          <a-alert type="info" show-icon class="block">
            <template #message>
              {{ $t('mes.inspection.planHead', { name: match.plan.name, sampling: samplingText(match.plan) }) }}
              <template v-if="match.sampleCode">{{ $t('mes.inspection.sampleCodePart', { code: match.sampleCode }) }}</template>
              {{ $t('mes.inspection.sampleLabel') }} <b>{{ formatQty(match.sampleQty) }}</b> {{ $t('mes.inspection.sampleTail', { ac: match.acceptQty, re: match.rejectQty }) }}
              <a class="switch-link" @click="form.manual = true">{{ $t('mes.inspection.toManual') }}</a>
            </template>
          </a-alert>
          <a-table row-key="planItemId" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 900 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                {{ record.name }}
                <a-tag v-if="record.critical" color="red" :bordered="false">{{ $t('mes.inspection.critical') }}</a-tag>
              </template>
              <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
              <template v-else-if="column.key === 'input'">
                <a-input
                  v-if="record.method === ItemMethod.QUANTITATIVE"
                  v-model:value="record.measuredText"
                  :placeholder="$t('mes.inspection.measuredPh', { n: formatQty(match.sampleQty) })"
                />
                <a-input-number v-else v-model:value="record.defectCount" :min="0" :max="Number(match.sampleQty)" :addon-before="$t('mes.inspection.fail')" :addon-after="$t('mes.inspection.pcs')" class="full" />
              </template>
              <template v-else-if="column.key === 'judge'">
                <template v-if="record.method === ItemMethod.QUANTITATIVE">
                  <span class="sub-text">{{ $t('mes.inspection.measuredCount', { n: parseMeasured(record.measuredText).length }) }}</span>
                  <span :class="{ danger: defectsOf(record) > 0 }">{{ $t('mes.inspection.outOfSpec', { n: defectsOf(record) }) }}</span>
                </template>
                <a-tag v-else :color="defectsOf(record) > 0 ? 'red' : 'green'" :bordered="false">{{ defectsOf(record) > 0 ? $t('mes.inspection.fail') : $t('mes.inspection.pass') }}</a-tag>
              </template>
            </template>
          </a-table>
          <div class="judge">
            {{ $t('mes.inspection.defectTotal') }} <b>{{ planDefects }}</b>{{ $t('mes.inspection.judgeLabel') }}
            <a-tag :color="planResult === QcResult.FAIL ? 'red' : 'green'">{{ planResult === QcResult.FAIL ? $t('mes.inspection.fail') : $t('mes.inspection.pass') }}</a-tag>
            <span v-if="planResult === QcResult.FAIL" class="sub-text">{{ form.type === QcType.IQC ? $t('mes.inspection.failTipIqc') : $t('mes.inspection.failTip') }}</span>
          </div>
        </template>

        <!-- 手工判定 -->
        <template v-else>
          <a-alert v-if="match?.plan" type="warning" show-icon class="block">
            <template #message>
              {{ $t('mes.inspection.manualHead', { name: match.plan.name }) }} <a class="switch-link" @click="form.manual = false">{{ $t('mes.inspection.toPlan') }}</a>
            </template>
          </a-alert>
          <a-row :gutter="16">
            <a-col :xs="12" :sm="6">
              <a-form-item :label="$t('mes.inspection.sampleQty')" name="sampleQty">
                <a-input-number v-model:value="form.sampleQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="6">
              <a-form-item :label="$t('mes.inspection.defectQty')" name="defectQty">
                <a-input-number v-model:value="form.defectQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item :label="$t('mes.inspection.result')" name="result">
                <a-radio-group v-model:value="form.result" :options="toOptions(dict.mes_qc_result)" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('mes.inspection.detailTitle', { no: detail.inspNo }) : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('mes.inspection.type')"><DictTag type="mes_qc_type" :value="detail.type" /></a-descriptions-item>
          <a-descriptions-item :label="$t('mes.inspection.source')">{{ detail.sourceNo || '-' }} {{ detail.supplierName || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.inspection.conclusion')">
            <DictTag type="mes_qc_result" :value="detail.result" />
            <a-tag v-if="detail.status !== InspectionStatus.CONFIRMED" color="orange" :bordered="false">{{ $t('mes.inspection.draft') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.material')">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.woDocForm.batch')">{{ detail.batchNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.wo.op')">{{ detail.opName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.quantity')">{{ $t('mes.inspection.inspectSampleDefect', { inspect: formatQty(detail.inspectQty), sample: formatQty(detail.sampleQty), defect: formatQty(detail.defectQty) }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('mes.inspection.plan')">
            {{ detail.planName || $t('mes.inspection.manual') }}
            <span v-if="detail.rejectQty != null" class="sub-text">{{ detail.sampleCode ? $t('mes.inspection.codePrefix', { code: detail.sampleCode }) : '' }}Ac {{ detail.acceptQty }} / Re {{ detail.rejectQty }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('mes.inspection.inspectDate')">{{ detail.inspectDate }} {{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.defectType || detail.defectDesc" :label="$t('mes.inspection.defect')" :span="3">
            <DictTag v-if="detail.defectType" type="mes_defect_type" :value="detail.defectType" /> {{ detail.defectDesc }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table v-if="detail.items?.length" row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              {{ record.name }}
              <a-tag v-if="record.critical === 1" color="red" :bordered="false">{{ $t('mes.inspection.critical') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
            <template v-else-if="column.key === 'measured'">
              <template v-if="record.measured">
                <span v-for="(v, i) in parseMeasured(record.measured)" :key="i" :class="['measured', { danger: outOfSpec(v, record) }]">{{ v }}</span>
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'result'">
              <a-tag :color="record.result === QcResult.FAIL ? 'red' : 'green'" :bordered="false">{{ $t('mes.inspection.failN', { n: record.defectCount }) }}</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { outOfSpec, parseMeasured, samplingText, specText } from '@/components/qms/qms'
import { addInspection, confirmInspection, deleteInspection, getInspection, pageInspection, updateInspection } from '@/api/mes/base'
import { getWo, pageWo } from '@/api/mes/wo'
import { matchPlan } from '@/api/qms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { formatQty, today } from '@/components/wms/wms'
import { useAction } from '@/composables/useAction'

import { InspectionStatus, ItemMethod, QcResult, QcType } from '@/constants/enums'
/** 质量检验：来料、过程、成品、首件；有检验方案时按方案抽样并逐项录入，自动判定 */
const dict = useDict('mes_qc_type', 'mes_qc_result', 'mes_defect_type')
const { t } = useI18n()

const columns = [
  { title: t('mes.inspection.noType'), key: 'inspNo', width: 190 },
  { title: t('mes.inspection.source'), key: 'source', width: 180 },
  { title: t('mes.wo.material'), key: 'material', width: 210 },
  { title: t('common.field.quantity'), key: 'qty', width: 220 },
  { title: t('mes.inspection.conclusion'), key: 'result', width: 110 },
  { title: t('mes.inspection.inspectDate'), key: 'inspectDate', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-inspection')
const statusOptions = [
  { value: InspectionStatus.DRAFT, label: t('mes.inspection.draft') },
  { value: InspectionStatus.CONFIRMED, label: t('mes.inspection.confirmed') }
]
const route = useRoute()
const query = reactive({ type: undefined, result: undefined, status: undefined, keyword: undefined, range: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageInspection, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})
onMounted(() => {
  // 首页待办「检验单待确认」链接带 status=DRAFT
  if (route.query.status) query.status = route.query.status
  load()
})

const confirmTitle = (r) =>
  r.result === QcResult.FAIL ? t('mes.inspection.confirmFail') : t('mes.inspection.confirmPass')

// ---------------------------------------------------------------- 表单
const rules = computed(() => ({
  type: [{ required: true, message: t('mes.inspection.typeRequired') }],
  sourceNo: form.value.type === QcType.FAI ? [{ required: true, whitespace: true, message: t('mes.inspection.woNoRequired') }] : [],
  woOpId: [{ required: true, message: t('mes.inspection.opRequired') }],
  materialId: [{ required: true, message: t('mes.supplementForm.materialRequired') }],
  inspectQty: [{ required: true, message: t('mes.inspection.inspectQtyRequired') }],
  sampleQty: [{ required: true, message: t('mes.inspection.sampleQtyRequired') }],
  result: [{ required: true, message: t('mes.inspection.resultRequired') }],
  inspectDate: [{ required: true, message: t('mes.inspection.inspectDateRequired') }]
}))
const BASE_FIELDS = ['type', 'sourceNo', 'woOpId', 'materialId', 'batchNo', 'inspectQty', 'defectType', 'defectDesc', 'inspectDate', 'remark']
const blank = (v) => (v === '' || v === undefined ? null : v)
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, type: QcType.IQC, sourceNo: '', woOpId: undefined, materialId: undefined, batchNo: '', inspectQty: undefined,
    sampleQty: undefined, defectQty: 0, result: 'PASS', defectType: undefined, defectDesc: '', inspectDate: today(), remark: '',
    manual: false, items: []
  }),
  fetch: async (record) => {
    const r = await getInspection(record.id)
    const saved = Object.fromEntries((r.items || []).map((i) => [i.planItemId, i]))
    pendingItems = saved
    return {
      ...r,
      materialLabel: `${record.materialCode} ${record.materialName}`,
      inspectQty: Number(r.inspectQty),
      sampleQty: Number(r.sampleQty),
      defectQty: Number(r.defectQty),
      manual: !r.planId,
      items: []
    }
  },
  create: addInspection,
  update: updateInspection,
  toPayload: (f) => {
    const payload = Object.fromEntries(BASE_FIELDS.map((k) => [k, blank(f[k])]))
    if (f.type !== QcType.FAI) payload.woOpId = null
    if (usePlan.value) {
      return {
        ...payload,
        planId: match.value.plan.id,
        items: f.items.map((i) => ({
          planItemId: i.planItemId,
          measured: i.method === ItemMethod.QUANTITATIVE ? parseMeasured(i.measuredText) : null,
          defectCount: i.method === ItemMethod.QUALITATIVE ? i.defectCount ?? 0 : null
        }))
      }
    }
    return { ...payload, sampleQty: f.sampleQty, defectQty: f.defectQty ?? 0, result: f.result }
  },
  onSuccess: load
})
const { formRef, form } = drawer

const sourceExtra = computed(() =>
  form.value.type === QcType.IQC ? t('mes.inspection.extraIqc') : form.value.type === QcType.FAI ? t('mes.inspection.extraFai') : t('mes.inspection.extraWo')
)

// 首件检验：按工单号加载工序
const opOptions = ref([])
async function loadOps() {
  if (form.value.type !== QcType.FAI || !form.value.sourceNo?.trim()) return
  const page = await pageWo({ woNo: form.value.sourceNo.trim(), size: 5 })
  const wo = page.records.find((w) => w.woNo === form.value.sourceNo.trim())
  if (!wo) {
    opOptions.value = []
    return
  }
  const d = await getWo(wo.id)
  opOptions.value = d.ops.map((o) => ({ value: o.id, label: o.firstArticle === 1 ? t('mes.inspection.opFai', { seq: o.seq, name: o.name }) : `${o.seq} ${o.name}` }))
  if (!form.value.materialId) {
    form.value.materialId = d.materialId
    form.value.materialLabel = `${d.materialCode} ${d.materialName}`
  }
}
watch(() => form.value.type, loadOps)
watch(() => drawer.open.value, (open) => open && form.value.type === QcType.FAI && loadOps())

// 检验方案匹配：类型、物料、送检数量变化时重新匹配
const match = ref(null)
let pendingItems = null
let matchSeq = 0
async function rematch() {
  const f = form.value
  if (!drawer.open.value || !f.type || !f.materialId || !(Number(f.inspectQty) > 0)) {
    match.value = null
    return
  }
  const seqNo = ++matchSeq
  let m = null
  try {
    m = await matchPlan({ type: f.type, materialId: f.materialId, lotQty: f.inspectQty })
  } catch {
    m = null
  }
  if (seqNo !== matchSeq) return
  const prev = Object.fromEntries((f.items || []).map((i) => [i.planItemId, i]))
  const saved = pendingItems || {}
  pendingItems = null
  match.value = m
  f.items = (m?.plan?.items || []).map((p) => {
    const old = prev[p.id] || saved[p.id]
    return {
      planItemId: p.id,
      name: p.name,
      method: p.method,
      standard: p.standard,
      lowerLimit: p.lowerLimit,
      upperLimit: p.upperLimit,
      unit: p.unit,
      critical: p.critical === 1,
      measuredText: old?.measuredText ?? old?.measured ?? '',
      defectCount: old?.defectCount ?? 0
    }
  })
}
let timer
watch(
  () => [drawer.open.value, form.value.type, form.value.materialId, form.value.inspectQty],
  () => {
    clearTimeout(timer)
    timer = setTimeout(rematch, 300)
  }
)
const usePlan = computed(() => !!match.value?.plan && !form.value.manual)

function defectsOf(item) {
  if (item.method === ItemMethod.QUANTITATIVE) return parseMeasured(item.measuredText).filter((v) => outOfSpec(v, item)).length
  return Number(item.defectCount || 0)
}
const planDefects = computed(() => (form.value.items || []).reduce((s, i) => s + defectsOf(i), 0))
const planResult = computed(() => {
  const criticalFail = (form.value.items || []).some((i) => i.critical && defectsOf(i) > 0)
  return criticalFail || planDefects.value >= Number(match.value?.rejectQty ?? 1) ? 'FAIL' : 'PASS'
})

const itemColumns = [
  { title: t('mes.inspection.item'), key: 'name', width: 160 },
  { title: t('mes.inspection.spec'), key: 'spec', width: 180 },
  { title: t('mes.inspection.inputResult'), key: 'input', width: 380 },
  { title: t('mes.inspection.judge'), key: 'judge', width: 160 }
]
const itemViewColumns = [
  { title: t('mes.inspection.item'), key: 'name', width: 160 },
  { title: t('mes.inspection.spec'), key: 'spec', width: 160 },
  { title: t('mes.inspection.measured'), key: 'measured' },
  { title: t('mes.inspection.itemResult'), key: 'result', width: 120 }
]

const confirm = useAction(confirmInspection, { successKey: 'mes.plan.confirmed', reload: () => load() })

const remove = useAction(deleteInspection, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 详情
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  // 列表行已有关联的名称，详情只取逐项结果
  const d = await getInspection(record.id)
  detail.value = { ...record, items: d.items }
  detailOpen.value = true
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
.switch-link {
  margin-left: 12px;
}
.judge {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.measured {
  display: inline-block;
  margin-right: 8px;
  font-variant-numeric: tabular-nums;
}
</style>
