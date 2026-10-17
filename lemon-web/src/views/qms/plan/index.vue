<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.type" :placeholder="$t('mes.inspection.type')" allow-clear :options="toOptions(dict.mes_qc_type)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('qms.plan.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:plan:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('qms.plan.add') }}
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
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record.id)">{{ record.name }}</a>
            <div v-if="record.remark" class="sub-text">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="mes_qc_type" :value="record.inspType" /></template>
          <template v-else-if="column.key === 'scope'">
            <span v-if="record.materialId">{{ record.materialCode }} {{ record.materialName }}</span>
            <span v-else>{{ $t('qms.plan.categoryPrefix') }}<DictTag type="wms_material_category" :value="record.materialCategory" /></span>
          </template>
          <template v-else-if="column.key === 'sampling'">
            <DictTag type="qms_sampling_mode" :value="record.samplingMode" />
            <div class="sub-text">{{ samplingText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? $t('common.status.enabled') : $t('common.status.disabled') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'qms:plan:edit'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
              <span v-perm="'qms:plan:remove'">
                <a-popconfirm :title="$t('qms.plan.deleteConfirm')" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('qms.plan.addTitle')" :edit-title="$t('qms.plan.editTitle')" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('qms.plan.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('qms.plan.namePh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('mes.inspection.type')" name="inspType">
              <a-radio-group v-model:value="form.inspType" :options="toOptions(dict.mes_qc_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="4">
            <a-form-item :label="$t('common.action.enable')" name="status">
              <a-switch v-model:checked="form.enabled" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('qms.plan.scope')" :extra="$t('qms.plan.scopeExtra')">
              <a-radio-group v-model:value="form.scope" button-style="solid">
                <a-radio-button value="material">{{ $t('qms.plan.scopeMaterial') }}</a-radio-button>
                <a-radio-button value="category">{{ $t('qms.plan.category') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item v-if="form.scope === 'material'" :label="$t('mes.wo.material')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
            <a-form-item v-else :label="$t('qms.plan.category')" name="materialCategory">
              <a-select v-model:value="form.materialCategory" :options="toOptions(dict.wms_material_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('qms.plan.samplingMode')" name="samplingMode">
              <a-radio-group v-model:value="form.samplingMode" :options="toOptions(dict.qms_sampling_mode)" />
            </a-form-item>
          </a-col>
          <template v-if="form.samplingMode === SamplingMode.FIXED">
            <a-col :xs="12" :sm="5">
              <a-form-item :label="$t('qms.plan.fixedQty')" name="fixedQty" :extra="$t('qms.plan.fixedQtyExtra')">
                <a-input-number v-model:value="form.fixedQty" :min="1" :precision="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="5">
              <a-form-item :label="$t('qms.plan.acceptQty')" name="acceptQty">
                <a-input-number v-model:value="form.acceptQty" :min="0" :precision="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else-if="form.samplingMode === SamplingMode.AQL">
            <a-col :xs="12" :sm="5">
              <a-form-item label="AQL" name="aql">
                <a-select v-model:value="form.aql" :options="AQL_OPTIONS" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="5">
              <a-form-item :label="$t('qms.plan.level')" name="inspectionLevel" :extra="$t('qms.plan.levelExtra')">
                <a-select v-model:value="form.inspectionLevel" :options="INSPECTION_LEVELS" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-if="form.samplingMode === SamplingMode.AQL" :xs="24" :sm="6">
            <a-form-item :label="$t('qms.plan.gbLabel')">
              <span class="sub-text">{{ $t('qms.plan.gbTip') }}</span>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">{{ $t('qms.plan.itemsTitle') }}</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>{{ $t('qms.plan.addItem') }}
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: itemScrollX }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'name'">
              <a-form-item :name="['items', index, 'name']" :rules="[{ required: true, whitespace: true, message: $t('qms.plan.itemRequired') }]" class="cell-item">
                <a-input v-model:value="record.name" :maxlength="64" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'method'">
              <a-select v-model:value="record.method" :options="METHODS" class="full" />
            </template>
            <template v-else-if="column.key === 'standard'">
              <a-input v-model:value="record.standard" :maxlength="255" :placeholder="record.method === ItemMethod.QUALITATIVE ? $t('qms.plan.standardPhQual') : $t('qms.plan.standardPh')" />
            </template>
            <template v-else-if="column.key === 'limits'">
              <a-space v-if="record.method === ItemMethod.QUANTITATIVE" :size="4">
                <a-input-number v-model:value="record.lowerLimit" :placeholder="$t('qms.plan.lower')" class="limit" />
                <span>~</span>
                <a-input-number v-model:value="record.upperLimit" :placeholder="$t('qms.plan.upper')" class="limit" />
              </a-space>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'unit'">
              <a-input v-if="record.method === ItemMethod.QUANTITATIVE" v-model:value="record.unit" :maxlength="16" placeholder="mm" />
            </template>
            <template v-else-if="column.key === 'critical'">
              <a-checkbox v-model:checked="record.critical" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('qms.plan.detailTitle', { name: detail.name }) : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('mes.inspection.type')"><DictTag type="mes_qc_type" :value="detail.inspType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('qms.plan.scope')">
            <span v-if="detail.materialId">{{ detail.materialCode }} {{ detail.materialName }}</span>
            <span v-else>{{ $t('qms.plan.categoryPrefix') }}<DictTag type="wms_material_category" :value="detail.materialCategory" /></span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('qms.plan.sampling')">{{ samplingText(detail) }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              {{ record.name }}
              <a-tag v-if="record.critical === 1" color="red" :bordered="false">{{ $t('mes.inspection.critical') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'method'">{{ record.method === ItemMethod.QUANTITATIVE ? $t('qms.plan.quantitative') : $t('qms.plan.qualitative') }}</template>
            <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { AQL_OPTIONS, INSPECTION_LEVELS, samplingText, specText } from '@/components/qms/qms'
import { addPlan, deletePlan, getPlan, pagePlan, updatePlan } from '@/api/qms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

import { InspectionLevel, ItemMethod, QcType, SamplingMode } from '@/constants/enums'
/** 检验方案：按物料或物料分类定义检验项目与抽样方案，新建检验单时自动匹配 */
const dict = useDict('mes_qc_type', 'qms_sampling_mode', 'wms_material_category')
const { t } = useI18n()
const METHODS = [
  { value: 'QUANTITATIVE', label: t('qms.plan.quantitative') },
  { value: 'QUALITATIVE', label: t('qms.plan.qualitative') }
]

const columns = [
  { title: t('qms.plan.name'), key: 'name', width: 220 },
  { title: t('mes.inspection.type'), key: 'type', width: 130 },
  { title: t('qms.plan.scope'), key: 'scope', width: 240 },
  { title: t('qms.plan.sampling'), key: 'sampling', width: 240 },
  { title: t('mes.inspection.item'), dataIndex: 'itemCount', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-plan')
const query = reactive({ type: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePlan, query)
onMounted(load)

let seq = 0
const rowKey = () => `k${++seq}`
const newItem = () => ({ key: rowKey(), name: '', method: 'QUANTITATIVE', standard: '', lowerLimit: undefined, upperLimit: undefined, unit: '', critical: false })

const rules = {
  name: [{ required: true, whitespace: true, message: t('qms.plan.nameRequired') }],
  inspType: [{ required: true, message: t('mes.inspection.typeRequired') }],
  materialId: [{ required: true, message: t('mes.supplementForm.materialRequired') }],
  materialCategory: [{ required: true, message: t('qms.plan.categoryRequired') }],
  samplingMode: [{ required: true, message: t('qms.plan.samplingModeRequired') }],
  fixedQty: [{ required: true, message: t('qms.plan.fixedQtyRequired') }],
  aql: [{ required: true, message: t('qms.plan.aqlRequired') }],
  inspectionLevel: [{ required: true, message: t('qms.plan.levelRequired') }]
}
const num = (v) => (v == null ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, name: '', inspType: QcType.IQC, enabled: true, scope: 'material', materialId: undefined, materialCategory: undefined,
    samplingMode: SamplingMode.AQL, fixedQty: undefined, acceptQty: 0, aql: 1, inspectionLevel: InspectionLevel.II, remark: '', items: [newItem()]
  }),
  fetch: async (record) => {
    const p = await getPlan(record.id)
    return {
      ...p,
      enabled: p.status === 1,
      scope: p.materialId ? 'material' : 'category',
      materialLabel: p.materialId ? `${p.materialCode} ${p.materialName}` : undefined,
      fixedQty: num(p.fixedQty),
      aql: num(p.aql),
      items: p.items.map((i) => ({ ...i, key: rowKey(), lowerLimit: num(i.lowerLimit), upperLimit: num(i.upperLimit), critical: i.critical === 1 }))
    }
  },
  create: addPlan,
  update: updatePlan,
  toPayload: (f) => ({
    name: f.name,
    inspType: f.inspType,
    materialId: f.scope === 'material' ? f.materialId : null,
    materialCategory: f.scope === 'category' ? f.materialCategory : null,
    samplingMode: f.samplingMode,
    fixedQty: f.samplingMode === SamplingMode.FIXED ? f.fixedQty : null,
    acceptQty: f.samplingMode === SamplingMode.FIXED ? f.acceptQty ?? 0 : null,
    aql: f.samplingMode === SamplingMode.AQL ? f.aql : null,
    inspectionLevel: f.samplingMode === SamplingMode.AQL ? f.inspectionLevel : null,
    status: f.enabled ? 1 : 0,
    remark: f.remark || null,
    items: f.items.map((i) => ({
      name: i.name,
      method: i.method,
      standard: i.standard || null,
      lowerLimit: i.method === ItemMethod.QUANTITATIVE ? i.lowerLimit ?? null : null,
      upperLimit: i.method === ItemMethod.QUANTITATIVE ? i.upperLimit ?? null : null,
      unit: i.method === ItemMethod.QUANTITATIVE ? i.unit || null : null,
      critical: !!i.critical
    }))
  }),
  onSuccess: load
})
const { formRef, form } = drawer

function addItem() {
  form.value.items.push(newItem())
}

const itemColumns = [
  { title: t('mes.inspection.item'), key: 'name', width: 180 },
  { title: t('qms.plan.method'), key: 'method', width: 150 },
  { title: t('qms.plan.standard'), key: 'standard', width: 220 },
  { title: t('qms.plan.limits'), key: 'limits', width: 240 },
  { title: t('common.field.unit'), key: 'unit', width: 90 },
  { title: t('mes.inspection.critical'), key: 'critical', width: 70, align: 'center' },
  { title: '', key: 'action', width: 50 }
]
const itemScrollX = itemColumns.reduce((sum, c) => sum + c.width, 0)
const itemViewColumns = [
  { title: t('mes.inspection.item'), key: 'name', width: 180 },
  { title: t('qms.plan.method'), key: 'method', width: 120 },
  { title: t('mes.inspection.spec'), key: 'spec', width: 200 },
  { title: t('common.field.description'), dataIndex: 'standard', ellipsis: true }
]

const remove = useAction(deletePlan, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getPlan(id)
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
.full {
  width: 100%;
}
.limit {
  width: 100px;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  font-weight: 600;
}
.cell-item {
  margin-bottom: 0;
}
</style>
