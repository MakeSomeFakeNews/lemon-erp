<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.type" placeholder="检验类型" allow-clear :options="toOptions(dict.mes_qc_type)" class="filter" @change="search" />
        <a-select v-model:value="query.result" placeholder="检验结论" allow-clear :options="toOptions(dict.mes_qc_result)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="单号、来源单号、物料" allow-clear class="filter-wide" @search="search" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" @change="search" />
        <template #extra>
          <a-button v-perm="'mes:qc:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增检验
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
            <div class="sub-text">{{ record.supplierName || (record.opName ? '工序 ' + record.opName : record.woNo ? '生产工单' : '') }}</div>
          </template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.batchNo ? '批次 ' + record.batchNo : '' }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            送检 {{ formatQty(record.inspectQty) }} · 抽检 {{ formatQty(record.sampleQty) }}
            <div :class="['sub-text', { danger: Number(record.defectQty) > 0 }]">
              不良 {{ formatQty(record.defectQty) }}<template v-if="record.rejectQty != null"> · Ac {{ record.acceptQty }} / Re {{ record.rejectQty }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'result'">
            <DictTag type="mes_qc_result" :value="record.result" />
            <a-tag v-if="record.status !== 'CONFIRMED'" color="orange" :bordered="false">草稿</a-tag>
            <div v-if="record.defectType" class="sub-text"><DictTag type="mes_defect_type" :value="record.defectType" /></div>
          </template>
          <template v-else-if="column.key === 'inspectDate'">
            {{ record.inspectDate }}
            <div class="sub-text">{{ record.createByName }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-if="record.status === 'CONFIRMED'" type="link" size="small" @click="openDetail(record)">查看</a-button>
              <template v-else>
                <a-button v-perm="'mes:qc:edit'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                <span v-perm="'mes:qc:edit'">
                  <a-popconfirm :title="confirmTitle(record)" @confirm="confirm(record.id)">
                    <a-button type="link" size="small">确认</a-button>
                  </a-popconfirm>
                </span>
                <span v-perm="'mes:qc:remove'">
                  <a-popconfirm title="确定删除该检验单？" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增检验单" edit-title="编辑检验单" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="检验类型" name="type">
              <a-radio-group v-model:value="form.type" :options="toOptions(dict.mes_qc_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="form.type === 'IQC' ? '采购入库单号' : '生产工单号'" name="sourceNo" :extra="sourceExtra">
              <a-input v-model:value="form.sourceNo" :maxlength="32" :placeholder="form.type === 'FAI' ? '' : '选填'" @blur="loadOps" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.type === 'FAI'" :xs="24" :sm="6">
            <a-form-item label="工序" name="woOpId">
              <a-select v-model:value="form.woOpId" :options="opOptions" placeholder="先填写工单号" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="物料" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="批次号" name="batchNo">
              <a-input v-model:value="form.batchNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="送检数量" name="inspectQty">
              <a-input-number v-model:value="form.inspectQty" :min="0" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="检验日期" name="inspectDate">
              <a-date-picker v-model:value="form.inspectDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="主要不良类型" name="defectType">
              <a-select v-model:value="form.defectType" allow-clear :options="toOptions(dict.mes_defect_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="不良描述" name="defectDesc">
              <a-input v-model:value="form.defectDesc" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>

        <!-- 按检验方案逐项录入 -->
        <template v-if="usePlan">
          <a-alert type="info" show-icon class="block">
            <template #message>
              检验方案「{{ match.plan.name }}」：{{ samplingText(match.plan) }}
              <template v-if="match.sampleCode">，样本量字码 {{ match.sampleCode }}</template>
              ，抽样 <b>{{ formatQty(match.sampleQty) }}</b> 件，Ac {{ match.acceptQty }} / Re {{ match.rejectQty }}
              <a class="switch-link" @click="form.manual = true">不按方案，手工判定</a>
            </template>
          </a-alert>
          <a-table row-key="planItemId" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 900 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                {{ record.name }}
                <a-tag v-if="record.critical" color="red" :bordered="false">关键</a-tag>
              </template>
              <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
              <template v-else-if="column.key === 'input'">
                <a-input
                  v-if="record.method === 'QUANTITATIVE'"
                  v-model:value="record.measuredText"
                  :placeholder="`${formatQty(match.sampleQty)} 个实测值，逗号或空格分隔`"
                />
                <a-input-number v-else v-model:value="record.defectCount" :min="0" :max="Number(match.sampleQty)" addon-before="不合格" addon-after="件" class="full" />
              </template>
              <template v-else-if="column.key === 'judge'">
                <template v-if="record.method === 'QUANTITATIVE'">
                  <span class="sub-text">{{ parseMeasured(record.measuredText).length }} 个，</span>
                  <span :class="{ danger: defectsOf(record) > 0 }">超差 {{ defectsOf(record) }}</span>
                </template>
                <a-tag v-else :color="defectsOf(record) > 0 ? 'red' : 'green'" :bordered="false">{{ defectsOf(record) > 0 ? '不合格' : '合格' }}</a-tag>
              </template>
            </template>
          </a-table>
          <div class="judge">
            不合格合计 <b>{{ planDefects }}</b>，判定：
            <a-tag :color="planResult === 'FAIL' ? 'red' : 'green'">{{ planResult === 'FAIL' ? '不合格' : '合格' }}</a-tag>
            <span v-if="planResult === 'FAIL'" class="sub-text">确认后生成不合格品处理单{{ form.type === 'IQC' ? '，冻结库存等待评审处置' : '' }}</span>
          </div>
        </template>

        <!-- 手工判定 -->
        <template v-else>
          <a-alert v-if="match?.plan" type="warning" show-icon class="block">
            <template #message>
              已有检验方案「{{ match.plan.name }}」，当前为手工判定 <a class="switch-link" @click="form.manual = false">按方案逐项录入</a>
            </template>
          </a-alert>
          <a-row :gutter="16">
            <a-col :xs="12" :sm="6">
              <a-form-item label="抽检数量" name="sampleQty">
                <a-input-number v-model:value="form.sampleQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="6">
              <a-form-item label="不良数量" name="defectQty">
                <a-input-number v-model:value="form.defectQty" :min="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12">
              <a-form-item label="检验结论" name="result">
                <a-radio-group v-model:value="form.result" :options="toOptions(dict.mes_qc_result)" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `检验单 ${detail.inspNo}` : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="检验类型"><DictTag type="mes_qc_type" :value="detail.type" /></a-descriptions-item>
          <a-descriptions-item label="来源">{{ detail.sourceNo || '-' }} {{ detail.supplierName || '' }}</a-descriptions-item>
          <a-descriptions-item label="结论">
            <DictTag type="mes_qc_result" :value="detail.result" />
            <a-tag v-if="detail.status !== 'CONFIRMED'" color="orange" :bordered="false">草稿</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="物料">{{ detail.materialCode }} {{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item label="批次">{{ detail.batchNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="工序">{{ detail.opName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数量">送检 {{ formatQty(detail.inspectQty) }} · 抽检 {{ formatQty(detail.sampleQty) }} · 不良 {{ formatQty(detail.defectQty) }}</a-descriptions-item>
          <a-descriptions-item label="检验方案">
            {{ detail.planName || '手工判定' }}
            <span v-if="detail.rejectQty != null" class="sub-text">{{ detail.sampleCode ? `字码 ${detail.sampleCode}，` : '' }}Ac {{ detail.acceptQty }} / Re {{ detail.rejectQty }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="检验日期">{{ detail.inspectDate }} {{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.defectType || detail.defectDesc" label="不良" :span="3">
            <DictTag v-if="detail.defectType" type="mes_defect_type" :value="detail.defectType" /> {{ detail.defectDesc }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table v-if="detail.items?.length" row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              {{ record.name }}
              <a-tag v-if="record.critical === 1" color="red" :bordered="false">关键</a-tag>
            </template>
            <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
            <template v-else-if="column.key === 'measured'">
              <template v-if="record.measured">
                <span v-for="(v, i) in parseMeasured(record.measured)" :key="i" :class="['measured', { danger: outOfSpec(v, record) }]">{{ v }}</span>
              </template>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'result'">
              <a-tag :color="record.result === 'FAIL' ? 'red' : 'green'" :bordered="false">不合格 {{ record.defectCount }}</a-tag>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
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

/** 质量检验：来料、过程、成品、首件；有检验方案时按方案抽样并逐项录入，自动判定 */
const dict = useDict('mes_qc_type', 'mes_qc_result', 'mes_defect_type')

const columns = [
  { title: '检验单号 / 类型', key: 'inspNo', width: 190 },
  { title: '来源', key: 'source', width: 180 },
  { title: '物料', key: 'material', width: 210 },
  { title: '数量', key: 'qty', width: 190 },
  { title: '结论', key: 'result', width: 110 },
  { title: '检验日期', key: 'inspectDate', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'mes-inspection')
const query = reactive({ type: undefined, result: undefined, keyword: undefined, range: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageInspection, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})
onMounted(load)

const confirmTitle = (r) =>
  r.result === 'FAIL' ? '确认后生成不合格品处理单，检验单不能再修改或删除，确定？' : '确认后检验结论生效，不能再修改或删除，确定？'

// ---------------------------------------------------------------- 表单
const rules = computed(() => ({
  type: [{ required: true, message: '请选择检验类型' }],
  sourceNo: form.value.type === 'FAI' ? [{ required: true, whitespace: true, message: '请填写生产工单号' }] : [],
  woOpId: [{ required: true, message: '请选择工序' }],
  materialId: [{ required: true, message: '请选择物料' }],
  inspectQty: [{ required: true, message: '请填写送检数量' }],
  sampleQty: [{ required: true, message: '请填写抽检数量' }],
  result: [{ required: true, message: '请选择检验结论' }],
  inspectDate: [{ required: true, message: '请选择检验日期' }]
}))
const BASE_FIELDS = ['type', 'sourceNo', 'woOpId', 'materialId', 'batchNo', 'inspectQty', 'defectType', 'defectDesc', 'inspectDate', 'remark']
const blank = (v) => (v === '' || v === undefined ? null : v)
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, type: 'IQC', sourceNo: '', woOpId: undefined, materialId: undefined, batchNo: '', inspectQty: undefined,
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
    if (f.type !== 'FAI') payload.woOpId = null
    if (usePlan.value) {
      return {
        ...payload,
        planId: match.value.plan.id,
        items: f.items.map((i) => ({
          planItemId: i.planItemId,
          measured: i.method === 'QUANTITATIVE' ? parseMeasured(i.measuredText) : null,
          defectCount: i.method === 'QUALITATIVE' ? i.defectCount ?? 0 : null
        }))
      }
    }
    return { ...payload, sampleQty: f.sampleQty, defectQty: f.defectQty ?? 0, result: f.result }
  },
  onSuccess: load
})
const { formRef, form } = drawer

const sourceExtra = computed(() =>
  form.value.type === 'IQC' ? '自动带出供应商，不合格时冻结库存' : form.value.type === 'FAI' ? '工序要求首件检验时，合格前只能报工 1 件' : '关联工单'
)

// 首件检验：按工单号加载工序
const opOptions = ref([])
async function loadOps() {
  if (form.value.type !== 'FAI' || !form.value.sourceNo?.trim()) return
  const page = await pageWo({ woNo: form.value.sourceNo.trim(), size: 5 })
  const wo = page.records.find((w) => w.woNo === form.value.sourceNo.trim())
  if (!wo) {
    opOptions.value = []
    return
  }
  const d = await getWo(wo.id)
  opOptions.value = d.ops.map((o) => ({ value: o.id, label: `${o.seq} ${o.name}${o.firstArticle === 1 ? '（要求首件）' : ''}` }))
  if (!form.value.materialId) {
    form.value.materialId = d.materialId
    form.value.materialLabel = `${d.materialCode} ${d.materialName}`
  }
}
watch(() => form.value.type, loadOps)
watch(() => drawer.open.value, (open) => open && form.value.type === 'FAI' && loadOps())

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
  if (item.method === 'QUANTITATIVE') return parseMeasured(item.measuredText).filter((v) => outOfSpec(v, item)).length
  return Number(item.defectCount || 0)
}
const planDefects = computed(() => (form.value.items || []).reduce((s, i) => s + defectsOf(i), 0))
const planResult = computed(() => {
  const criticalFail = (form.value.items || []).some((i) => i.critical && defectsOf(i) > 0)
  return criticalFail || planDefects.value >= Number(match.value?.rejectQty ?? 1) ? 'FAIL' : 'PASS'
})

const itemColumns = [
  { title: '检验项目', key: 'name', width: 160 },
  { title: '规格 / 标准', key: 'spec', width: 180 },
  { title: '检验结果', key: 'input', width: 380 },
  { title: '判定', key: 'judge', width: 140 }
]
const itemViewColumns = [
  { title: '检验项目', key: 'name', width: 160 },
  { title: '规格 / 标准', key: 'spec', width: 160 },
  { title: '实测值', key: 'measured' },
  { title: '结果', key: 'result', width: 120 }
]

async function confirm(id) {
  await confirmInspection(id)
  message.success('已确认')
  load()
}

async function remove(id) {
  await deleteInspection(id)
  message.success('删除成功')
  load()
}

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
