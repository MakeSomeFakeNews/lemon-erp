<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('wms.checkDoc.createTitle')" :edit-title="$t('wms.checkDoc.editTitle')" width="min(1280px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('wms.checkDoc.warehouse')" name="warehouseId">
            <WarehouseSelect :value="form.warehouseId" :allow-clear="false" @update:value="onWarehouseChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('wms.checkDoc.date')" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('wms.checkDoc.freeze')" name="freeze" :extra="$t('wms.checkDoc.freezeTip')">
            <a-switch v-model:checked="form.freeze" :checked-children="$t('wms.checkDoc.frozen')" :un-checked-children="$t('wms.checkDoc.notFrozen')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="6">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('wms.common.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-alert
        type="info"
        show-icon
        class="check-tip"
        :message="$t('wms.checkDoc.tip')"
      />

      <div class="items-head">
        <span class="section-title">
          {{ $t('wms.checkDoc.itemsTitle', { n: form.items.length, counted: stats.counted, diff: stats.diff }) }}
        </span>
        <a-space wrap>
          <LocationSelect v-model:value="gen.locationId" :warehouse-id="form.warehouseId" :placeholder="$t('wms.checkDoc.allLocations')" class="gen-input" />
          <a-input v-model:value="gen.keyword" :placeholder="$t('wms.common.materialKeyword')" allow-clear class="gen-input" />
          <a-button type="primary" ghost :loading="generating" :disabled="!form.warehouseId" @click="generate">
            <template #icon><ThunderboltOutlined /></template>{{ $t('wms.checkDoc.generate') }}
          </a-button>
          <a-button :disabled="!form.warehouseId" @click="addRow">
            <template #icon><PlusOutlined /></template>{{ $t('wms.doc.addMaterial') }}
          </a-button>
          <a-tooltip :title="$t('wms.checkDoc.fillBookTip')">
            <a-button :disabled="!form.items.length" @click="fillBook">{{ $t('wms.checkDoc.fillBook') }}</a-button>
          </a-tooltip>
        </a-space>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="form.items.length > 50 ? { pageSize: 50, showSizeChanger: false } : false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: $t('wms.checkDoc.empty') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <a-form-item v-if="record.manual" :name="itemName(record, 'materialId')" :rules="requiredRule($t('wms.common.selectMaterial'))" class="cell-item">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
            <template v-else>
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
          </template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-if="record.manual" v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
            <span v-else>{{ locationText(record.locationCode) }}</span>
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-form-item
              v-if="record.manual"
              :name="itemName(record, 'batchNo')"
              :rules="record.batchEnabled === 1 ? requiredRule($t('wms.common.inputBatchNo')) : []"
              class="cell-item"
            >
              <a-input v-model:value="record.batchNo" :maxlength="64" :disabled="record.batchEnabled !== 1" :placeholder="$t('wms.common.batchNo')" />
            </a-form-item>
            <span v-else>{{ record.batchNo || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'bookQty'">
            {{ record.bookQty == null ? '-' : formatQty(record.bookQty) }}
          </template>
          <template v-else-if="column.key === 'actualQty'">
            <a-input-number
              v-if="record.serialEnabled !== 1"
              v-model:value="record.actualQty"
              :min="0"
              :precision="4"
              :addon-after="record.unit || undefined"
              :placeholder="$t('wms.checkDoc.notCounted')"
              class="full"
            />
            <span v-else>{{ record.actualQty == null ? $t('wms.checkDoc.notCounted') : formatQty(record.actualQty) }}</span>
          </template>
          <template v-else-if="column.key === 'diff'">
            <QtyDiff :value="diffOf(record)" />
          </template>
          <template v-else-if="column.key === 'serialNos'">
            <SerialInput
              v-if="record.serialEnabled === 1"
              :value="record.serialNos"
              :placeholder="$t('wms.checkDoc.scanSerials')"
              @update:value="(v) => setSerials(record, v)"
            />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="removeRow(record)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LocationSelect from './LocationSelect.vue'
import MaterialSelect from './MaterialSelect.vue'
import QtyDiff from './QtyDiff.vue'
import SerialInput from './SerialInput.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { addCheck, checkSnapshot, getCheck, updateCheck } from '@/api/wms/check'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { formatQty, locationText, rowKey, splitSerials, today } from './wms'

/**
 * 新增 / 编辑盘点单（草稿）：按库存生成明细后录入实盘数量；盘盈的物料可手工添加。
 * 通过 ref 调用 openCreate() / openEdit(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const columns = [
  { title: t('wms.common.material'), key: 'material', width: 290 },
  { title: t('wms.common.location'), key: 'location', width: 200 },
  { title: t('wms.common.batch'), key: 'batchNo', width: 140 },
  { title: t('wms.checkDoc.bookQty'), key: 'bookQty', width: 110, align: 'right' },
  { title: t('wms.checkDoc.actualQty'), key: 'actualQty', width: 160 },
  { title: t('wms.checkDoc.diff'), key: 'diff', width: 100, align: 'right' },
  { title: t('wms.checkDoc.actualSerials'), key: 'serialNos', width: 260 },
  { title: t('common.field.remark'), key: 'remark', width: 140 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  warehouseId: requiredRule(t('wms.checkDoc.selectWarehouse')),
  bizDate: requiredRule(t('wms.checkDoc.selectDate'))
}

const toRow = (item) => ({
  ...item,
  key: item.id ? `item-${item.id}` : rowKey(),
  locationId: item.locationId || null,
  actualQty: item.actualQty ?? null
})

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, warehouseId: undefined, bizDate: today(), freeze: false, remark: '', items: [] }),
  fetch: async (record) => {
    const check = await getCheck(record.id)
    return { ...check, freeze: check.freeze === true || check.freeze === 1, items: check.items.map(toRow) }
  },
  create: addCheck,
  update: updateCheck,
  toPayload: (f) => ({
    warehouseId: f.warehouseId,
    bizDate: f.bizDate,
    freeze: !!f.freeze,
    remark: f.remark,
    items: f.items.map((i) => ({
      materialId: i.materialId,
      locationId: i.locationId || null,
      batchNo: i.batchNo || null,
      actualQty: i.actualQty ?? null,
      serialNos: i.serialNos || null,
      remark: i.remark || null
    }))
  }),
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

// 表格分页时 index 不是在 items 中的位置，按 key 找
const itemName = (record, field) => ['items', form.value.items.indexOf(record), field]

const diffOf = (r) => (r.actualQty == null || r.bookQty == null ? null : Number(r.actualQty) - Number(r.bookQty))
const stats = computed(() => ({
  counted: form.value.items.filter((i) => i.actualQty != null).length,
  diff: form.value.items.filter((i) => (diffOf(i) ?? 0) !== 0).length
}))

const slotKey = (i) => `${i.locationId || 0}|${i.materialId}|${i.batchNo || ''}`

function onWarehouseChange(id) {
  if (id === form.value.warehouseId) return
  form.value.warehouseId = id
  gen.locationId = undefined
  if (form.value.items.length) {
    form.value.items = []
    message.info(t('wms.checkDoc.warehouseChanged'))
  }
}

// 按库存生成：已在明细中的库存位置保留已录入的数据
const gen = reactive({ locationId: undefined, keyword: undefined })
const generating = ref(false)
async function generate() {
  generating.value = true
  try {
    const rows = await checkSnapshot({ warehouseId: form.value.warehouseId, locationId: gen.locationId, keyword: gen.keyword })
    const exists = new Set(form.value.items.map(slotKey))
    const added = rows.filter((r) => !exists.has(slotKey(r))).map(toRow)
    form.value.items.push(...added)
    message.success(added.length ? t('wms.checkDoc.added', { n: added.length }) : t('wms.checkDoc.nothingNew'))
  } finally {
    generating.value = false
  }
}

function addRow() {
  form.value.items.unshift({ key: rowKey(), manual: true, materialId: undefined, locationId: null, bookQty: null, actualQty: null })
}

function applyMaterial(row, m) {
  Object.assign(row, {
    materialLabel: m.label,
    materialCode: m.code,
    materialName: m.name,
    spec: m.spec,
    unit: m.unit,
    batchEnabled: m.batchEnabled,
    serialEnabled: m.serialEnabled,
    serialNos: undefined,
    actualQty: null
  })
  if (m.batchEnabled !== 1) row.batchNo = undefined
}

/** 序列号物料的实盘数量 = 实盘序列号个数 */
function setSerials(row, value) {
  row.serialNos = value
  row.actualQty = splitSerials(value).length
}

function fillBook() {
  let n = 0
  form.value.items.forEach((i) => {
    if (i.actualQty == null && i.bookQty != null && i.serialEnabled !== 1) {
      i.actualQty = i.bookQty
      n++
    }
  })
  message.success(n ? t('wms.checkDoc.filled', { n }) : t('wms.checkDoc.nothingToFill'))
}

function removeRow(record) {
  const remove = () => form.value.items.splice(form.value.items.indexOf(record), 1)
  if (record.actualQty == null) return remove()
  Modal.confirm({ title: t('wms.checkDoc.confirmRemoveRow'), okType: 'danger', onOk: remove })
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.check-tip {
  margin-bottom: 12px;
}
.items-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}
.section-title {
  font-weight: 500;
}
.gen-input {
  width: 160px;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
