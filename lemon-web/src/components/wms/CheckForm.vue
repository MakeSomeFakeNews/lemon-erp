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
        :message="showCost ? `${$t('wms.checkDoc.tip')}${$t('wms.checkDoc.costPriceTip')}` : $t('wms.checkDoc.tip')"
      />

      <div class="items-head">
        <span class="section-title">
          {{ $t('wms.checkDoc.itemsTitle', { n: form.items.length, counted: stats.counted, diff: stats.diff }) }}
        </span>
        <a-space wrap>
          <a-tooltip :title="$t('wms.checkDoc.pickMaterialsTip')">
            <a-button type="primary" ghost :loading="adding" :disabled="!form.warehouseId" @click="pickerOpen = true">
              <template #icon><SearchOutlined /></template>{{ $t('wms.checkDoc.pickMaterials') }}
            </a-button>
          </a-tooltip>
          <a-button :disabled="!form.warehouseId" @click="addRow">
            <template #icon><PlusOutlined /></template>{{ $t('wms.doc.addMaterial') }}
          </a-button>
          <a-tooltip :title="$t('wms.checkDoc.fillBookTip')">
            <a-button :disabled="!form.items.length" @click="fillBook">{{ $t('wms.checkDoc.fillBook') }}</a-button>
          </a-tooltip>
        </a-space>
      </div>

      <LineItemsTable
        class="items-table"
        :items="form.items"
        :columns="columns"
        :pagination="form.items.length > 50 ? { pageSize: 50, showSizeChanger: false } : false"
        :empty-text="$t('wms.checkDoc.empty')"
        :confirm-remove="(r) => (r.actualQty == null ? undefined : $t('wms.checkDoc.confirmRemoveRow'))"
      >
        <template #cell="{ column, record }">
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
          <template v-else-if="column.key === 'costPrice'">
            <a-input-number
              v-if="(diffOf(record) ?? 0) > 0"
              v-model:value="record.costPrice"
              :min="0"
              :max="999999999999"
              :precision="4"
              :placeholder="record.avgCost == null ? $t('wms.checkDoc.costPriceDefault') : $t('wms.checkDoc.costPricePh', { v: price(record.avgCost) })"
              class="full"
            />
            <span v-else class="sub-text">-</span>
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
        </template>
      </LineItemsTable>
    </a-form>
    <CheckMaterialPicker v-model:open="pickerOpen" :warehouse-id="form.warehouseId" :existing-ids="existingMaterialIds" @pick="addMaterials" />
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import CheckMaterialPicker from './CheckMaterialPicker.vue'
import LocationSelect from './LocationSelect.vue'
import MaterialSelect from './MaterialSelect.vue'
import QtyDiff from './QtyDiff.vue'
import SerialInput from './SerialInput.vue'
import WarehouseSelect from './WarehouseSelect.vue'
import { addCheck, getCheck, updateCheck } from '@/api/wms/check'
import { unitCosts } from '@/api/wms/stock'
import { useUserStore } from '@/store/user'
import { price } from '@/utils/number'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { lineKey, useLineItems } from '@/composables/useLineItems'
import { formatQty, locationText, splitSerials, today } from './wms'
import { materialItems, MAX_CHECK_LINES } from './checkMaterials'

/**
 * 新增 / 编辑盘点单（草稿）：「选择商品」按条件（仓库范围、变动日期即动态盘点、分类、货位等）勾选本次盘点的物料，
 * 或「添加物料」逐个添加，录入实盘数量。不再一次按整仓库存或全部变动商品生成明细（issue #123）：一次盘点一部分，大盘点拆成多张盘点单
 * 通过 ref 调用 openCreate() / openEdit(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const userStore = useUserStore()
/** 盘盈成本价：有「查看成本金额」或盘点单新增 / 修改权限时显示（默认当前均价） */
const showCost = computed(() => userStore.hasPerm(['wms:stock:cost', 'wms:check:add', 'wms:check:edit']))

const columns = computed(() =>
  [
    { title: t('wms.common.material'), key: 'material', width: 290 },
    { title: t('wms.common.location'), key: 'location', width: 200 },
    { title: t('wms.common.batch'), key: 'batchNo', width: 140 },
    { title: t('wms.checkDoc.bookQty'), key: 'bookQty', width: 110, align: 'right' },
    { title: t('wms.checkDoc.actualQty'), key: 'actualQty', width: 160 },
    { title: t('wms.checkDoc.diff'), key: 'diff', width: 100, align: 'right' },
    showCost.value ? { title: t('wms.checkDoc.costPrice'), key: 'costPrice', width: 150 } : null,
    { title: t('wms.checkDoc.actualSerials'), key: 'serialNos', width: 260 },
    { title: t('common.field.remark'), key: 'remark', width: 140 },
    { title: '', key: 'action', width: 50, fixed: 'right' }
  ].filter(Boolean)
)

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  warehouseId: requiredRule(t('wms.checkDoc.selectWarehouse')),
  bizDate: requiredRule(t('wms.checkDoc.selectDate'))
}

const toRow = (item) => ({
  ...item,
  key: lineKey(item),
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
      remark: i.remark || null,
      costPrice: showCost.value && i.costPrice != null && i.costPrice !== '' ? i.costPrice : null
    }))
  }),
  onSuccess: () => emit('success')
})
const { form, formRef } = drawer

const lines = useLineItems(() => form.value.items, {
  newRow: () => ({ manual: true, materialId: undefined, locationId: null, bookQty: null, actualQty: null })
})
// 表格分页时 index 不是在 items 中的位置，按行对象找
const itemName = lines.itemName

const diffOf = (r) => (r.actualQty == null || r.bookQty == null ? null : Number(r.actualQty) - Number(r.bookQty))
const stats = computed(() => ({
  counted: form.value.items.filter((i) => i.actualQty != null).length,
  diff: form.value.items.filter((i) => (diffOf(i) ?? 0) !== 0).length
}))

const slotKey = (i) => `${i.locationId || 0}|${i.materialId}|${i.batchNo || ''}`

function onWarehouseChange(id) {
  if (id === form.value.warehouseId) return
  form.value.warehouseId = id
  if (form.value.items.length) {
    form.value.items = []
    message.info(t('wms.checkDoc.warehouseChanged'))
  }
}

const adding = ref(false)

// 选择商品：有库存的按库存位置各一行；盘点仓库没有库存的一行账面 0（可改库位、批次），已在明细中的不重复添加
const pickerOpen = ref(false)
const existingMaterialIds = computed(() => [...new Set(form.value.items.map((i) => i.materialId).filter(Boolean))])

async function addMaterials(ids) {
  if (!ids.length) return
  // 已在明细中的物料也要请求：可能有明细中还没有的库存位置
  const materials = new Set(existingMaterialIds.value)
  adding.value = true
  try {
    const rows = await materialItems(form.value.warehouseId, ids)
    const slots = new Set(form.value.items.map(slotKey))
    const added = []
    rows.forEach((r) => {
      const noStock = Number(r.bookQty) === 0
      if (noStock ? materials.has(r.materialId) : slots.has(slotKey(r))) return
      slots.add(slotKey(r))
      added.push(noStock ? { ...toRow(r), manual: true, materialLabel: `${r.materialCode} ${r.materialName}` } : toRow(r))
    })
    form.value.items.push(...added)
    message.success(added.length ? t('wms.checkDoc.added', { n: added.length }) : t('wms.checkDoc.nothingNewPicked'))
    if (form.value.items.length > MAX_CHECK_LINES) message.warning(t('wms.checkDoc.tooManyLines', { max: MAX_CHECK_LINES }))
  } finally {
    adding.value = false
  }
}

const addRow = () => lines.addRow({}, { first: true })

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
    actualQty: null,
    costPrice: null,
    avgCost: null
  })
  if (m.batchEnabled !== 1) row.batchNo = undefined
  // 当前均价只给有「查看成本金额」权限的人，其他人不填时按确认时的均价
  if (userStore.hasPerm('wms:stock:cost')) {
    unitCosts([m.id]).then((costs) => {
      if (row.materialId === m.id && costs[m.id] != null) row.avgCost = Number(costs[m.id])
    })
  }
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
