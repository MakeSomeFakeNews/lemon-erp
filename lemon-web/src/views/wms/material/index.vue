<template>
  <div class="workspace">
    <!-- 物料分类树：点击分类列出该分类及其下级分类的物料，有权限时可维护分类 -->
    <a-card v-show="showCategory" :bordered="false" class="page-card workspace-side">
      <MaterialCategoryTree ref="categoryTreeRef" v-model="query.category" @change="search" />
    </a-card>

    <div class="table-page workspace-main">
      <a-card v-show="showSearch" :bordered="false" class="page-card">
        <SearchForm :model="query" @search="search" @reset="resetQuery">
          <a-form-item :label="$t('wms.common.material')" name="keyword">
            <a-input v-model:value="query.keyword" :placeholder="$t('wms.flow.codeOrName')" allow-clear />
          </a-form-item>
          <a-form-item :label="$t('pricing.goods.brand')" name="brand">
            <a-select v-model:value="query.brand" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_material_brand)" />
          </a-form-item>
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
          <a-form-item :label="$t('wms.material.goodsType')" name="virtual">
            <a-select v-model:value="query.virtual" :placeholder="$t('common.placeholder.all')" allow-clear :options="goodsTypeOptions" />
          </a-form-item>
        </SearchForm>
      </a-card>

      <a-card :bordered="false" class="page-card table-card">
        <TableToolbar
          :exporter="exportAll"
          v-model:size="size"
          v-model:checked="checked"
          v-model:show-search="showSearch"
          :columns="toolbarColumns"
          @refresh="load"
        >
          <a-button v-perm="'wms:material:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('wms.material.createTitle') }}
          </a-button>
          <AiExtractButton target="wms.material" perm="wms:material:add" :title="$t('wms.material.createTitle')" @result="ai.apply" />
          <a-button v-perm="'wms:material:add'" @click="importOpen = true">
            <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
          </a-button>
          <span v-perm="'wms:material:remove'">
            <a-popconfirm
              :title="$t('wms.material.confirmDeleteN', { n: selectedKeys.length })"
              :disabled="!selectedKeys.length"
              @confirm="remove(selectedKeys)"
            >
              <a-button danger :disabled="!selectedKeys.length">
                <template #icon><DeleteOutlined /></template>{{ $t('common.action.batchDelete') }}
              </a-button>
            </a-popconfirm>
          </span>
          <a-button v-perm="'wms:material:priceLevel'" @click="priceLevelRef.open()">
            <template #icon><DollarOutlined /></template>{{ $t('pricing.level.title') }}
          </a-button>
          <a-button :disabled="!selectedKeys.length" @click="openLabels">
            <template #icon><BarcodeOutlined /></template>{{ $t('wms.material.printLabel') }}{{ selectedKeys.length ? ` (${selectedKeys.length})` : '' }}
          </a-button>
          <template #extra>
            <a-tooltip :title="showCategory ? $t('wms.material.hideCategory') : $t('wms.material.showCategory')">
              <a-button type="text" @click="showCategory = !showCategory">
                <template #icon><MenuFoldOutlined v-if="showCategory" /><MenuUnfoldOutlined v-else /></template>
              </a-button>
            </a-tooltip>
          </template>
        </TableToolbar>

        <a-table
          row-key="id"
          :size="size"
          :columns="tableColumns"
          :data-source="list"
          :loading="loading"
          :pagination="pagination"
          :row-selection="rowSelection"
          :scroll="{ x: sumWidth(tableColumns) + 32, y: '100%' }"
          @change="onTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <a-image v-if="record.imageUrl" :src="record.imageUrl" :width="36" :height="36" class="thumb" />
                <div>
                  <a @click="detailRef.open(record.id)">{{ record.name }}</a>
                  <a-tag v-if="record.isTemplate === 1" color="magenta" :bordered="false" class="ml">{{ $t('wms.material.style') }}</a-tag>
                  <a-tag v-if="record.isVirtual === 1" color="cyan" :bordered="false" class="ml">{{ $t('wms.material.virtualTag') }}</a-tag>
                  <div class="sub-text">
                    {{ record.code }}
                    <template v-if="record.variantText"> · {{ record.variantText }}</template>
                  </div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'category'">
              <DictTag type="wms_material_category" :value="record.category" />
            </template>
            <template v-else-if="column.key === 'brand'">
              <DictTag type="wms_material_brand" :value="record.brand" />
            </template>
            <template v-else-if="column.priceLevel != null">{{ record[column.key] == null ? '-' : price(record[column.key]) }}</template>
            <template v-else-if="column.key === 'tracking'">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">{{ $t('wms.common.batch') }}</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('wms.common.serialNo') }}</a-tag>
              <a-tag v-if="record.iqcRequired === 1" color="orange" :bordered="false">{{ $t('wms.material.iqc') }}</a-tag>
              <span v-if="record.batchEnabled !== 1 && record.serialEnabled !== 1 && record.iqcRequired !== 1" class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'supplyType'">
              <template v-if="record.plan">
                <DictTag type="mes_supply_type" :value="record.plan.effectiveSupplyType" />
                <div v-if="!record.plan.supplyType" class="sub-text">{{ $t('wms.material.auto') }}</div>
              </template>
            </template>
            <template v-else-if="column.key === 'planRule'">
              <template v-if="record.plan">
                <div v-for="t in planRule(record.plan)" :key="t" class="rule-line">{{ t }}</div>
                <span v-if="!planRule(record.plan).length" class="sub-text">-</span>
              </template>
            </template>
            <template v-else-if="column.key === 'safetyStock'">{{ formatQty(record.safetyStock) }}</template>
            <template v-else-if="column.key === 'barcode'">{{ record.barcode || '-' }}</template>
            <template v-else-if="column.key === 'status'">
              <StatusSwitch
                :value="record.status"
                :name="record.name"
                :api="(s) => changeMaterialStatus(record.id, s)"
                :disabled="!userStore.hasPerm('wms:material:edit')"
                @changed="(s) => (record.status = s)"
              />
            </template>
            <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
            <template v-else-if="column.key === 'action'">
              <TableActions :actions="rowActions(record)" />
            </template>
          </template>
        </a-table>
      </a-card>

      <FormDrawer page :drawer="drawer" :create-title="$t('wms.material.createTitle')" :edit-title="$t('wms.material.editTitle')" :width="canEditPlan ? 'min(860px, 100vw)' : undefined">
        <AiPrefillAlert :ai="ai" />
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
          <div v-if="canEditPlan" class="section-title">{{ $t('wms.material.basic') }}</div>
          <a-alert v-if="!canEdit && form.id" type="info" show-icon class="tip" :message="$t('wms.material.planOnly')" />
          <MaterialBasicFields :form="form" :col-span="colSpan" :readonly="basicReadonly" :ai="ai" :enabled-levels="enabledLevels" />
          <MaterialPlanFields v-if="canEditPlan && form.isVirtual !== 1" :form="form" />
          <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('wms.material.customFields')" />
        </a-form>
      </FormDrawer>
      <VariantDrawer v-model:open="variantOpen" :material="variantMaterial" @generated="load" />

      <MaterialDetail ref="detailRef" :can-edit="canEdit" :can-edit-plan="canEditPlan" @edit="(id) => drawer.openEdit({ id })" @changed="load" />
      <ImportModal
        v-model:open="importOpen"
        :title="$t('wms.common.material')"
        :columns="importColumns"
        :importer="importMaterial"
        :sample="{ code: 'M0001', name: $t('wms.material.sampleName'), spec: 'M3*8', unit: $t('wms.material.sampleUnit'), batchEnabled: $t('wms.material.sampleNo'), serialEnabled: $t('wms.material.sampleNo'), safetyStock: 100 }"
        @success="onImported"
      />
      <MaterialUnitsModal v-model:open="unitsOpen" :material="unitsMaterial" :editable="canEdit" />
      <RecordExtrasModal
        v-model:open="extrasOpen"
        :title="extrasTitle"
        biz-type="wms_material"
        :biz-id="extrasId"
        :editable="userStore.hasPerm('wms:material:edit')"
      />
      <LabelPrintModal v-model:open="labelOpen" type-key="label.material" :items="labelItems" />
      <PriceLevelModal ref="priceLevelRef" @saved="load" />
      <StockHistory ref="historyRef" />
      <MaterialImagesModal ref="imagesRef" :editable="canEdit" @changed="load" />
      <MaterialTrackingModal ref="trackingRef" @saved="load" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { AppstoreAddOutlined, BarcodeOutlined, DeleteOutlined, DollarOutlined, ImportOutlined, MenuFoldOutlined, MenuUnfoldOutlined, PaperClipOutlined, PictureOutlined, PlusOutlined, SafetyCertificateOutlined, SwapOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LabelPrintModal from '@/components/label/LabelPrintModal.vue'
import MaterialCategoryTree from '@/components/wms/MaterialCategoryTree.vue'
import MaterialUnitsModal from '@/components/wms/MaterialUnitsModal.vue'
import StockHistory from '@/components/wms/StockHistory.vue'
import VariantDrawer from '@/components/wms/VariantDrawer.vue'
import { addMaterial, changeMaterialStatus, deleteMaterial, getMaterial, pageMaterial, updateMaterial, importMaterial } from '@/api/wms/material'
import { getMaterialPlan, materialPlans, saveMaterialPlan } from '@/api/mes/base'
import { formatQty } from '@/components/wms/wms'
import { sumWidth, useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { asPrefilled, useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import { useCustomColumns, useCustomFields } from '@/composables/useCustomFields'
import { PRICE_FIELDS, usePriceLevels } from '@/components/wms/priceLevels'
import { price } from '@/utils/number'
import PriceLevelModal from './PriceLevelModal.vue'
import MaterialImagesModal from './MaterialImagesModal.vue'
import MaterialTrackingModal from './MaterialTrackingModal.vue'
import MaterialBasicFields from './MaterialBasicFields.vue'
import MaterialPlanFields from './MaterialPlanFields.vue'
import MaterialDetail from './MaterialDetail.vue'
import { emptyMaterialForm, planRule, toForm, toPayload } from './material'
import AiExtractButton from '@/components/ai/AiExtractButton.vue'
import AiPrefillAlert from '@/components/ai/AiPrefillAlert.vue'
import { useAiPrefill } from '@/composables/useAiPrefill'
import { load as loadPref, save as savePref } from '@/utils/storage'
const { t } = useI18n()

/**
 * 物料管理：物料主数据只有这一份，仓库、采购、销售、生产共用。
 * 有 mes:material:edit 权限时，编辑抽屉中可维护生产计划属性和替代料（只有该权限时基础信息只读）
 */
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
const canEditPlan = computed(() => userStore.hasPerm('mes:material:edit'))
// 左侧分类树：显示 / 隐藏记在本机
const showCategory = ref(loadPref('material-page:show-category', true))
watch(showCategory, (v) => savePref('material-page:show-category', v))
const categoryTreeRef = ref()
/** 物料有增删改、导入后刷新分类树上的物料数 */
const refreshCounts = () => categoryTreeRef.value?.refresh()
const dict = useDict('sys_common_status', 'wms_material_category', 'wms_material_brand', 'mes_supply_type')

const importOpen = ref(false)
// 价格列：停用的列在表单、列表、导入导出中隐藏
const { enabledLevels } = usePriceLevels()
const priceLevelRef = ref()
// 辅助单位
const unitsOpen = ref(false)
const unitsMaterial = ref()
// 多规格：生成 SKU、查看库存矩阵
const variantOpen = ref(false)
const variantMaterial = ref()
function openVariants(record) {
  variantMaterial.value = { ...record }
  variantOpen.value = true
}

// 经营历史（库存流水的进出、单价与结余）
const historyRef = ref()
function openHistory(record) {
  historyRef.value.open({ materialId: record.id, materialCode: record.code, materialName: record.name })
}

function openUnits(record) {
  unitsMaterial.value = record
  unitsOpen.value = true
}
// 附件、变更记录
const extrasOpen = ref(false)
const extrasId = ref()
const extrasTitle = ref('')
function openExtras(record) {
  extrasId.value = record.id
  extrasTitle.value = `${record.code} ${record.name}`
  extrasOpen.value = true
}
const importColumns = computed(() => [
  { title: t('wms.material.code'), key: 'code', required: true },
  { title: t('wms.material.name'), key: 'name', required: true },
  { title: t('wms.inout.spec'), key: 'spec' },
  { title: t('wms.material.unit'), key: 'unit' },
  // 分类可填编码、完整路径（如「电子元器件/电阻」）或不重名的分类名称，由后端匹配
  { title: t('wms.inout.category'), key: 'category' },
  { title: t('pricing.goods.brand'), key: 'brand', dict: 'wms_material_brand' },
  { title: t('wms.material.batchManaged'), key: 'batchEnabled', type: 'bool' },
  { title: t('wms.material.serialManaged'), key: 'serialEnabled', type: 'bool' },
  { title: t('wms.material.iqc'), key: 'iqcRequired', type: 'bool' },
  { title: t('wms.material.virtual'), key: 'isVirtual', type: 'bool' },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', type: 'number' },
  ...enabledLevels.value.map((l) => ({ title: l.name, key: l.field, type: 'number' })),
  { title: t('common.field.remark'), key: 'remark' }
])

const columns = [
  { title: t('wms.common.material'), key: 'name', width: 200 },
  { title: t('wms.inout.spec'), dataIndex: 'spec', width: 150, ellipsis: true },
  { title: t('common.field.unit'), dataIndex: 'unit', width: 70 },
  { title: t('wms.units.barcode'), key: 'barcode', width: 140, ellipsis: true },
  { title: t('wms.inout.category'), key: 'category', width: 160, dict: 'wms_material_category' },
  { title: t('pricing.goods.brand'), key: 'brand', width: 100, dict: 'wms_material_brand' },
  ...PRICE_FIELDS.map((key, level) => ({ title: key, key, dataIndex: key, priceLevel: level, width: 100, align: 'right' })),
  { title: t('wms.material.tracking'), key: 'tracking', width: 120 },
  { title: t('wms.material.supplyType'), key: 'supplyType', width: 110 },
  { title: t('wms.material.planParams'), key: 'planRule', width: 150 },
  { title: t('wms.stock.safetyStock'), key: 'safetyStock', width: 120, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  // 编辑、删除直接显示；资料、单位、变更记录、款式变体在「更多」中
  actionColumn(3)
]
const { checked } = useColumns(columns, 'wms-material')
/** 列设置、导出用的列：只有启用的价格列，标题为价格列名称 */
const toolbarColumns = computed(() => {
  const enabled = new Map(enabledLevels.value.map((l) => [l.level, l.name]))
  return columns.filter((c) => c.priceLevel == null || enabled.has(c.priceLevel))
    .map((c) => (c.priceLevel == null ? c : { ...c, title: enabled.get(c.priceLevel) }))
})
const visibleColumns = computed(() => toolbarColumns.value.filter((c) => checked.value.includes(c.key || c.dataIndex)))
const showSearch = ref(true)

/** 分页后再查一次本页物料的计划属性 */
async function fetchPage(params) {
  const page = await pageMaterial(params)
  if (page.records.length) {
    const plans = await materialPlans(page.records.map((r) => r.id)).catch(() => [])
    const byId = Object.fromEntries(plans.map((p) => [p.id, p]))
    page.records.forEach((r) => (r.plan = byId[r.id]))
  }
  return page
}

const query = reactive({ keyword: undefined, category: undefined, brand: undefined, status: undefined, virtual: undefined })
/** 商品类型筛选：实物 / 虚拟商品（服务项目） */
const goodsTypeOptions = computed(() => [
  { value: false, label: t('wms.material.physical') },
  { value: true, label: t('wms.material.virtual') }
])
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(fetchPage, query)
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const cf = useCustomFields('wms_material')
const cfFields = cf.fields
const { columns: cfColumns, cell: cfCell } = useCustomColumns('wms_material', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, category: undefined, brand: undefined, status: undefined, virtual: undefined })
  search()
}

onMounted(load)

// ---------------------------------------------------------------- 新增 / 编辑
const rules = {
  code: [
    { required: true, message: t('wms.material.inputCode') },
    { pattern: /^[A-Za-z0-9_.-]{1,64}$/, message: t('wms.material.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('wms.material.inputName') }]
}
const colSpan = computed(() => (canEditPlan.value ? 8 : 12))
const basicReadonly = computed(() => !!form.value.id && !canEdit.value)

const drawerOptions = cf.wrap({
  emptyForm: emptyMaterialForm,
  fetch: async (record) => {
    const [m, p] = await Promise.all([getMaterial(record.id), canEditPlan.value ? getMaterialPlan(record.id) : null])
    return toForm(m, p)
  },
  create: async ({ material, plan, copyFrom }) => {
    const id = await addMaterial(material, copyFrom)
    if (plan) await saveMaterialPlan(id, plan)
    return id
  },
  update: async (id, { material, plan }) => {
    if (canEdit.value) await updateMaterial(id, material)
    if (plan) await saveMaterialPlan(id, plan)
  },
  toPayload: (f) => toPayload(f, canEditPlan.value),
  onSuccess: () => {
    load()
    refreshCounts()
    detailRef.value?.reload()
  }
})
const drawer = useFormDrawer(drawerOptions)
const { formRef, form } = drawer
// 智能录入：规格书、商品标签等识别结果打开新增物料并预填
const ai = useAiPrefill('material', (data) => drawer.openCreate(data), () => drawer.open.value)

const rowActions = (record) => [
  { type: 'edit', show: canEdit.value || canEditPlan.value, onClick: () => drawer.openEdit(record) },
  { type: 'delete', perm: 'wms:material:remove', confirm: t('wms.material.confirmDelete', { name: record.name }), onClick: () => remove([record.id]) },
  { key: 'copy', type: 'copy', perm: 'wms:material:add', more: true, onClick: () => copyMaterial(record) },
  { key: 'images', label: t('wms.materialImage.action'), icon: PictureOutlined, more: true, onClick: () => imagesRef.value.open(record) },
  { key: 'tracking', label: t('wms.tracking.action'), icon: SafetyCertificateOutlined, perm: 'wms:material:edit', show: record.isTemplate !== 1 && record.isVirtual !== 1, more: true, onClick: () => trackingRef.value.open(record) },
  { key: 'extras', label: t('wms.material.extras'), icon: PaperClipOutlined, more: true, onClick: () => openExtras(record) },
  { key: 'units', label: t('common.field.unit'), icon: SwapOutlined, more: true, onClick: () => openUnits(record) },
  { key: 'history', type: 'history', label: t('wms.history.action'), show: record.isTemplate !== 1 && record.isVirtual !== 1, more: true, onClick: () => openHistory(record) },
  { key: 'variants', label: t('wms.variant.title'), icon: AppstoreAddOutlined, show: !record.templateId && record.isVirtual !== 1, more: true, onClick: () => openVariants(record) }
]

// 复制：带出原物料的资料、计划属性、替代料和自定义字段，编码留空（按编码规则生成）、条码不带；保存时后端复制辅助单位（不含包装条码）和图片
const imagesRef = ref()
const detailRef = ref()
const trackingRef = ref()
async function copyMaterial(record) {
  const src = await drawerOptions.fetch(record)
  const data = omitKeys(src, ['id', 'code', 'barcode', 'createTime', 'updateTime', 'createBy', 'status', 'isTemplate', 'templateId', 'variantText', 'imageUrl', 'searchKey'])
  if (record.isTemplate === 1 || record.templateId) message.info(t('wms.material.copyVariantTip'))
  asPrefilled(() => drawer.openCreate({ ...data, code: '', barcode: '', copyFrom: record.id }))
}
const omitKeys = (obj, keys) => Object.fromEntries(Object.entries(obj).filter(([k]) => !keys.includes(k)))

async function remove(ids) {
  await deleteMaterial(ids)
  message.success(t('common.msg.deleteSuccess'))
  selectedKeys.value = selectedKeys.value.filter((k) => !ids.includes(k))
  load()
  refreshCounts()
}

function onImported() {
  search()
  refreshCounts()
}

// 条码标签：选中的行（当前页）
const labelOpen = ref(false)
const labelItems = ref([])
function openLabels() {
  labelItems.value = list.value.filter((r) => selectedKeys.value.includes(r.id)).map((r) => ({ code: r.code, name: r.name, spec: r.spec, unit: r.unit, barcode: r.barcode }))
  labelOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tip {
  margin-bottom: 16px;
}
.rule-line {
  font-size: 12px;
  line-height: 18px;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--app-primary);
}
.ml {
  margin-left: 6px;
}
.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.name-cell :deep(.thumb) {
  object-fit: cover;
  border-radius: 4px;
}
</style>
