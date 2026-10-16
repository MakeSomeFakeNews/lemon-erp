<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.type" placeholder="检验类型" allow-clear :options="toOptions(dict.mes_qc_type)" class="filter" @change="search" />
        <a-input-search v-model:value="query.keyword" placeholder="方案名称、物料" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'qms:plan:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>新增方案
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
            <span v-else>分类：<DictTag type="wms_material_category" :value="record.materialCategory" /></span>
          </template>
          <template v-else-if="column.key === 'sampling'">
            <DictTag type="qms_sampling_mode" :value="record.samplingMode" />
            <div class="sub-text">{{ samplingText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'" :bordered="false">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button v-perm="'qms:plan:edit'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
              <span v-perm="'qms:plan:remove'">
                <a-popconfirm title="删除检验方案？已使用该方案的检验单不受影响" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" create-title="新增检验方案" edit-title="编辑检验方案" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="10">
            <a-form-item label="方案名称" name="name">
              <a-input v-model:value="form.name" :maxlength="64" placeholder="如：外壳来料检验" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item label="检验类型" name="inspType">
              <a-radio-group v-model:value="form.inspType" :options="toOptions(dict.mes_qc_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="4">
            <a-form-item label="启用" name="status">
              <a-switch v-model:checked="form.enabled" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item label="适用范围" extra="新建检验单时先按物料匹配，没有再按物料分类">
              <a-radio-group v-model:value="form.scope" button-style="solid">
                <a-radio-button value="material">指定物料</a-radio-button>
                <a-radio-button value="category">物料分类</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item v-if="form.scope === 'material'" label="物料" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" />
            </a-form-item>
            <a-form-item v-else label="物料分类" name="materialCategory">
              <a-select v-model:value="form.materialCategory" :options="toOptions(dict.wms_material_category)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="抽样方式" name="samplingMode">
              <a-radio-group v-model:value="form.samplingMode" :options="toOptions(dict.qms_sampling_mode)" />
            </a-form-item>
          </a-col>
          <template v-if="form.samplingMode === 'FIXED'">
            <a-col :xs="12" :sm="5">
              <a-form-item label="抽样数量" name="fixedQty" extra="大于送检数量时全检">
                <a-input-number v-model:value="form.fixedQty" :min="1" :precision="0" class="full" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="5">
              <a-form-item label="允收不良数 Ac" name="acceptQty">
                <a-input-number v-model:value="form.acceptQty" :min="0" :precision="0" class="full" />
              </a-form-item>
            </a-col>
          </template>
          <template v-else-if="form.samplingMode === 'AQL'">
            <a-col :xs="12" :sm="5">
              <a-form-item label="AQL" name="aql">
                <a-select v-model:value="form.aql" :options="AQL_OPTIONS" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :sm="5">
              <a-form-item label="检验水平" name="inspectionLevel" extra="一般用 II，破坏性检验用 S 级">
                <a-select v-model:value="form.inspectionLevel" :options="INSPECTION_LEVELS" />
              </a-form-item>
            </a-col>
          </template>
          <a-col v-if="form.samplingMode === 'AQL'" :xs="24" :sm="6">
            <a-form-item label="按 GB/T 2828.1 正常检验一次抽样">
              <span class="sub-text">送检时按批量查样本量字码，计算样本量与 Ac / Re</span>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">检验项目（定量项目录入实测值，超出上下限计为不合格；关键项目不合格时整批判不合格）</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>添加项目
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 1000 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'name'">
              <a-form-item :name="['items', index, 'name']" :rules="[{ required: true, whitespace: true, message: '请填写项目' }]" class="cell-item">
                <a-input v-model:value="record.name" :maxlength="64" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'method'">
              <a-select v-model:value="record.method" :options="METHODS" class="full" />
            </template>
            <template v-else-if="column.key === 'standard'">
              <a-input v-model:value="record.standard" :maxlength="255" :placeholder="record.method === 'QUALITATIVE' ? '如：无划伤、无毛刺' : '说明（选填）'" />
            </template>
            <template v-else-if="column.key === 'limits'">
              <a-space v-if="record.method === 'QUANTITATIVE'" :size="4">
                <a-input-number v-model:value="record.lowerLimit" placeholder="下限" class="limit" />
                <span>~</span>
                <a-input-number v-model:value="record.upperLimit" placeholder="上限" class="limit" />
              </a-space>
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'unit'">
              <a-input v-if="record.method === 'QUANTITATIVE'" v-model:value="record.unit" :maxlength="16" placeholder="mm" />
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

    <SubPage :open="detailOpen" :title="detail ? `检验方案 ${detail.name}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="检验类型"><DictTag type="mes_qc_type" :value="detail.inspType" /></a-descriptions-item>
          <a-descriptions-item label="适用范围">
            <span v-if="detail.materialId">{{ detail.materialCode }} {{ detail.materialName }}</span>
            <span v-else>分类：<DictTag type="wms_material_category" :value="detail.materialCategory" /></span>
          </a-descriptions-item>
          <a-descriptions-item label="抽样">{{ samplingText(detail) }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemViewColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              {{ record.name }}
              <a-tag v-if="record.critical === 1" color="red" :bordered="false">关键</a-tag>
            </template>
            <template v-else-if="column.key === 'method'">{{ record.method === 'QUANTITATIVE' ? '定量' : '定性' }}</template>
            <template v-else-if="column.key === 'spec'">{{ specText(record) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
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

/** 检验方案：按物料或物料分类定义检验项目与抽样方案，新建检验单时自动匹配 */
const dict = useDict('mes_qc_type', 'qms_sampling_mode', 'wms_material_category')
const METHODS = [
  { value: 'QUANTITATIVE', label: '定量' },
  { value: 'QUALITATIVE', label: '定性' }
]

const columns = [
  { title: '方案名称', key: 'name', width: 220 },
  { title: '检验类型', key: 'type', width: 110 },
  { title: '适用范围', key: 'scope', width: 240 },
  { title: '抽样', key: 'sampling', width: 220 },
  { title: '检验项目', dataIndex: 'itemCount', width: 90, align: 'right' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'qms-plan')
const query = reactive({ type: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pagePlan, query)
onMounted(load)

let seq = 0
const rowKey = () => `k${++seq}`
const newItem = () => ({ key: rowKey(), name: '', method: 'QUANTITATIVE', standard: '', lowerLimit: undefined, upperLimit: undefined, unit: '', critical: false })

const rules = {
  name: [{ required: true, whitespace: true, message: '请填写方案名称' }],
  inspType: [{ required: true, message: '请选择检验类型' }],
  materialId: [{ required: true, message: '请选择物料' }],
  materialCategory: [{ required: true, message: '请选择物料分类' }],
  samplingMode: [{ required: true, message: '请选择抽样方式' }],
  fixedQty: [{ required: true, message: '请填写抽样数量' }],
  aql: [{ required: true, message: '请选择 AQL' }],
  inspectionLevel: [{ required: true, message: '请选择检验水平' }]
}
const num = (v) => (v == null ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, name: '', inspType: 'IQC', enabled: true, scope: 'material', materialId: undefined, materialCategory: undefined,
    samplingMode: 'AQL', fixedQty: undefined, acceptQty: 0, aql: 1, inspectionLevel: 'II', remark: '', items: [newItem()]
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
    fixedQty: f.samplingMode === 'FIXED' ? f.fixedQty : null,
    acceptQty: f.samplingMode === 'FIXED' ? f.acceptQty ?? 0 : null,
    aql: f.samplingMode === 'AQL' ? f.aql : null,
    inspectionLevel: f.samplingMode === 'AQL' ? f.inspectionLevel : null,
    status: f.enabled ? 1 : 0,
    remark: f.remark || null,
    items: f.items.map((i) => ({
      name: i.name,
      method: i.method,
      standard: i.standard || null,
      lowerLimit: i.method === 'QUANTITATIVE' ? i.lowerLimit ?? null : null,
      upperLimit: i.method === 'QUANTITATIVE' ? i.upperLimit ?? null : null,
      unit: i.method === 'QUANTITATIVE' ? i.unit || null : null,
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
  { title: '检验项目', key: 'name', width: 180 },
  { title: '判定方式', key: 'method', width: 100 },
  { title: '标准 / 说明', key: 'standard', width: 220 },
  { title: '规格限', key: 'limits', width: 240 },
  { title: '单位', key: 'unit', width: 90 },
  { title: '关键', key: 'critical', width: 60, align: 'center' },
  { title: '', key: 'action', width: 50 }
]
const itemViewColumns = [
  { title: '检验项目', key: 'name', width: 180 },
  { title: '判定方式', key: 'method', width: 90 },
  { title: '规格 / 标准', key: 'spec', width: 200 },
  { title: '说明', dataIndex: 'standard', ellipsis: true }
]

async function remove(id) {
  await deletePlan(id)
  message.success('删除成功')
  load()
}

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
