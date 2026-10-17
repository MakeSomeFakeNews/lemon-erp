<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('wms.common.docNo')" name="docNo">
          <a-input v-model:value="query.docNo" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('wms.assembly.kind')" name="bizType">
          <a-select v-model:value="query.bizType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.wms_assembly_kind)" />
        </a-form-item>
        <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
          <WarehouseSelect v-model:value="query.warehouseId" :placeholder="$t('common.placeholder.all')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="STATUS_OPTIONS" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:show-search="showSearch" @refresh="load">
        <a-button v-perm="'wms:assembly:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('wms.assembly.createTitle') }}
        </a-button>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'assemblyNo'">
            <a @click="openDetail(record.id)">{{ record.assemblyNo }}</a>
          </template>
          <template v-else-if="column.key === 'kind'"><DictTag type="wms_assembly_kind" :value="record.kind" /></template>
          <template v-else-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'DONE' ? 'green' : 'default'" :bordered="false">{{ record.status === 'DONE' ? $t('wms.assembly.done') : $t('wms.assembly.draft') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <span v-if="record.status === 'DRAFT'" class="table-actions">
              <a-button v-perm="'wms:assembly:add'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
              <span v-perm="'wms:assembly:confirm'">
                <a-popconfirm :title="$t('wms.assembly.confirmTitle', { no: record.assemblyNo })" @confirm="confirm(record.id)">
                  <a-button type="link" size="small">{{ $t('wms.doc.confirm') }}</a-button>
                </a-popconfirm>
              </span>
              <span v-perm="'wms:assembly:add'">
                <a-popconfirm :title="$t('wms.assembly.confirmDelete', { no: record.assemblyNo })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
            <a-button v-else type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('wms.assembly.createTitle')" :edit-title="$t('wms.assembly.editTitle')" width="min(1100px, 100vw)">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="$t('wms.assembly.tip')"
      />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.assembly.kind')" name="kind">
              <a-radio-group v-model:value="form.kind" button-style="solid">
                <a-radio-button value="ASSEMBLE">{{ $t('wms.assembly.assemble') }}</a-radio-button>
                <a-radio-button value="DISASSEMBLE">{{ $t('wms.assembly.disassemble') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.common.warehouse')" name="warehouseId">
              <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" @update:value="onWarehouseChange" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('wms.common.bizDate')" name="bizDate">
              <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="6">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('wms.common.optional')" />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="section-title">{{ form.kind === 'ASSEMBLE' ? $t('wms.assembly.kitIn') : $t('wms.assembly.kitOut') }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item :label="$t('wms.common.material')" name="materialId">
              <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" @select="onProduct" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('common.field.quantity')" name="qty">
              <a-input-number v-model:value="form.qty" :min="0" :precision="form.serialEnabled === 1 ? 0 : 4" :addon-after="form.unit || undefined" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6" :lg="6">
            <a-form-item :label="$t('wms.common.location')" name="locationId">
              <LocationSelect v-model:value="form.locationId" :warehouse-id="form.warehouseId" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.batchEnabled === 1" :xs="12" :sm="6" :lg="4">
            <a-form-item :label="$t('wms.common.batchNo')" name="batchNo">
              <a-input v-model:value="form.batchNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col v-if="form.serialEnabled === 1" :span="24">
            <a-form-item :label="$t('wms.common.serialNo')" name="serialNos" :extra="$t('wms.assembly.serialTip')">
              <a-textarea v-model:value="form.serialNos" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">{{ form.kind === 'ASSEMBLE' ? $t('wms.assembly.compOut', { n: form.items.length }) : $t('wms.assembly.compIn', { n: form.items.length }) }}</span>
          <a-space>
            <a-button :disabled="!form.materialId || !form.qty" @click="fromBom">{{ $t('wms.assembly.fromBom') }}</a-button>
            <a-button type="primary" ghost @click="addRow"><template #icon><PlusOutlined /></template>{{ $t('wms.assembly.addComp') }}</a-button>
          </a-space>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: itemScrollX }"
          :locale="{ emptyText: $t('wms.assembly.empty') }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: $t('wms.common.selectMaterial') }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'qty'">
              <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: $t('wms.common.inputQty') }]" class="cell-item">
                <a-input-number v-model:value="record.qty" :min="0" :precision="record.serialEnabled === 1 ? 0 : 4" :addon-after="record.unit || undefined" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'location'">
              <LocationSelect v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
            </template>
            <template v-else-if="column.key === 'batchNo'">
              <a-input v-if="record.batchEnabled === 1" v-model:value="record.batchNo" :maxlength="64" :placeholder="$t('wms.common.batchNo')" />
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'serialNos'">
              <a-input v-if="record.serialEnabled === 1" v-model:value="record.serialNos" :placeholder="$t('wms.assembly.commaSep')" />
              <span v-else class="sub-text">-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? `${$t('wms.assembly.title')} ${detail.assemblyNo}` : ''" width="min(900px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('wms.assembly.kind')"><DictTag type="wms_assembly_kind" :value="detail.kind" /></a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.warehouse')">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.bizDate')">{{ detail.bizDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.assembly.kit')">{{ detail.materialCode }} {{ detail.materialName }} {{ detail.spec || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.quantity')">{{ formatQty(detail.qty) }} {{ detail.unit }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.assembly.locationBatch')">{{ detail.locationCode || $t('wms.common.unspecified') }} {{ detail.batchNo || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">{{ detail.status === 'DONE' ? $t('wms.assembly.done') : $t('wms.assembly.draft') }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.outDocNo" :label="$t('wms.assembly.docs')">{{ $t('wms.assembly.docsValue', { out: detail.outDocNo, in: detail.inDocNo }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.common.createBy')">{{ detail.createByName }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.serialNos" :label="$t('wms.common.serialNo')">{{ detail.serialNos }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="detailColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }} <span class="sub-text">{{ record.spec }}</span></template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addAssembly, confirmAssembly, deleteAssembly, getAssembly, pageAssembly, updateAssembly } from '@/api/wms/assembly'
import { bomsOfMaterial, getBom } from '@/api/mes/base'
import { getMaterial } from '@/api/wms/material'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

const { t } = useI18n()

/** 组装拆卸单：组装消耗子件得到成套件，拆卸相反；确认后生成出入库单 */
const dict = useDict('wms_assembly_kind')
const STATUS_OPTIONS = [
  { value: 'DRAFT', label: t('wms.assembly.draft') },
  { value: 'DONE', label: t('wms.assembly.done') }
]
const columns = [
  { title: t('wms.common.docNo'), key: 'assemblyNo', width: 160 },
  { title: t('wms.assembly.kind'), key: 'kind', width: 110 },
  { title: t('wms.assembly.kit'), key: 'material', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 110, align: 'right' },
  { title: t('wms.common.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('wms.common.bizDate'), dataIndex: 'bizDate', width: 110 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('wms.common.createBy'), dataIndex: 'createByName', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 180, fixed: 'right' }
]
const showSearch = ref(true)
const query = reactive({ docNo: undefined, bizType: undefined, warehouseId: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageAssembly, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, { docNo: undefined, bizType: undefined, warehouseId: undefined, status: undefined })
  search()
}

const itemColumns = [
  { title: t('wms.assembly.component'), key: 'material', width: 300 },
  { title: t('common.field.quantity'), key: 'qty', width: 160 },
  { title: t('wms.common.location'), key: 'location', width: 200 },
  { title: t('wms.common.batchNo'), key: 'batchNo', width: 140 },
  { title: t('wms.common.serialNo'), key: 'serialNos', width: 180 },
  { title: '', key: 'action', width: 50 }
]
const itemScrollX = itemColumns.reduce((sum, c) => sum + c.width, 0)
const detailColumns = [
  { title: t('wms.assembly.component'), key: 'material' },
  { title: t('common.field.quantity'), key: 'qty', width: 130, align: 'right' },
  { title: t('wms.common.location'), dataIndex: 'locationCode', width: 120 },
  { title: t('wms.common.batchNo'), dataIndex: 'batchNo', width: 120 },
  { title: t('wms.common.serialNo'), dataIndex: 'serialNos', width: 180, ellipsis: true }
]
const rules = {
  warehouseId: [{ required: true, message: t('wms.common.selectWarehouse') }],
  bizDate: [{ required: true, message: t('wms.doc.selectBizDate') }],
  materialId: [{ required: true, message: t('wms.assembly.selectKit') }],
  qty: [{ required: true, message: t('wms.common.inputQty') }]
}
const label = (m) => `${m.code} ${m.name}${m.spec ? `（${m.spec}）` : ''}`
const toItem = (i) => ({
  key: rowKey(),
  materialId: i.materialId,
  materialLabel: `${i.materialCode} ${i.materialName}${i.spec ? `（${i.spec}）` : ''}`,
  unit: i.unit,
  batchEnabled: i.batchEnabled,
  serialEnabled: i.serialEnabled,
  qty: Number(i.qty),
  locationId: i.locationId || null,
  batchNo: i.batchNo || '',
  serialNos: i.serialNos || ''
})

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, kind: 'ASSEMBLE', warehouseId: undefined, bizDate: today(), remark: '', materialId: undefined, qty: 1, locationId: null, batchNo: '', serialNos: '', items: [] }),
  fetch: async (record) => {
    const a = await getAssembly(record.id)
    return {
      ...a,
      qty: Number(a.qty),
      locationId: a.locationId || null,
      materialLabel: `${a.materialCode} ${a.materialName}${a.spec ? `（${a.spec}）` : ''}`,
      serialNos: a.serialNos || '',
      items: a.items.map(toItem)
    }
  },
  create: addAssembly,
  update: updateAssembly,
  toPayload: (f) => ({
    kind: f.kind,
    warehouseId: f.warehouseId,
    bizDate: f.bizDate,
    remark: f.remark || null,
    materialId: f.materialId,
    qty: f.qty,
    locationId: f.locationId || null,
    batchNo: f.batchNo || null,
    serialNos: f.serialNos || null,
    items: f.items.map((i) => ({ materialId: i.materialId, qty: i.qty, locationId: i.locationId || null, batchNo: i.batchNo || null, serialNos: i.serialNos || null }))
  }),
  onSuccess: load
})
const { formRef, form } = drawer

function onWarehouseChange() {
  form.value.locationId = null
  form.value.items.forEach((i) => (i.locationId = null))
}
function onProduct(m) {
  Object.assign(form.value, { materialLabel: label(m), unit: m.unit, batchEnabled: m.batchEnabled, serialEnabled: m.serialEnabled })
}
function addRow() {
  form.value.items.push({ key: rowKey(), materialId: undefined, qty: undefined, locationId: null, batchNo: '', serialNos: '' })
}
function applyMaterial(row, m) {
  Object.assign(row, { materialLabel: label(m), unit: m.unit, batchEnabled: m.batchEnabled, serialEnabled: m.serialEnabled })
}

/** 按成套件的默认 BOM（没有默认时取第一个启用的）带出子件：用量 × 成套件数量 */
async function fromBom() {
  const boms = await bomsOfMaterial(form.value.materialId).catch(() => null)
  if (!boms?.length) {
    message.warning(t('wms.assembly.noBom'))
    return
  }
  const bom = await getBom((boms.find((b) => b.isDefault === 1) || boms[0]).id)
  // BOM 子件不带批次 / 序列号管理标识，逐个读取物料
  const flags = await Promise.all(bom.items.map((i) => getMaterial(i.materialId).catch(() => ({}))))
  form.value.items = bom.items.map((i, idx) =>
    toItem({
      ...i,
      qty: Math.round(Number(i.qty) * Number(form.value.qty) * 10000) / 10000,
      batchEnabled: flags[idx].batchEnabled,
      serialEnabled: flags[idx].serialEnabled
    })
  )
  message.success(t('wms.assembly.bomLoaded', { version: bom.version || '', n: bom.items.length }))
}

async function confirm(id) {
  await confirmAssembly(id)
  message.success(t('wms.assembly.confirmed'))
  load()
}
async function remove(id) {
  await deleteAssembly(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getAssembly(id)
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.cell-item {
  margin-bottom: 0;
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
</style>
