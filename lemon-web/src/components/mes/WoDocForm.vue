<template>
  <SubPage :open="open" :title="title" width="min(1150px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <a-alert type="info" show-icon class="tip" :message="tip" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('mes.woDocForm.warehouse')" name="warehouseId" :rules="[{ required: true, message: $t('mes.woDocForm.warehouseRequired') }]">
            <WarehouseSelect
              :value="form.warehouseId"
              :allow-clear="false"
              :placeholder="kind === 'scrap' ? $t('mes.woDocForm.scrapWarehousePh') : $t('mes.woDocForm.warehouseRequired')"
              :types="kind === 'scrap' ? ['DEFECT'] : undefined"
              @update:value="onWarehouseChange"
            />
            <div v-if="kind === 'scrap'" class="sub-text">{{ $t('mes.woDocForm.scrapWarehouseTip') }}</div>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="6">
          <a-form-item :label="$t('mes.woDocForm.bizDate')" name="bizDate" :rules="[{ required: true, message: $t('mes.woDocForm.bizDateRequired') }]">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="10">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="200" :placeholder="$t('mes.woDocForm.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-table row-key="key" size="small" class="items-table" :columns="columns" :data-source="form.items" :pagination="false" :scroll="{ x: scrollX }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="row-tags">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">{{ $t('mes.woDocForm.batch') }}</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">{{ $t('mes.woDocForm.serialNo') }}</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'limit'">
            <div>{{ formatQty(record.limit) }} {{ record.unit }}</div>
            <div v-if="record.hint" class="sub-text">{{ record.hint }}</div>
          </template>
          <template v-else-if="column.key === 'slot'">
            <a-form-item :name="['items', index, 'slot']" :rules="[{ required: true, message: $t('mes.woDocForm.slotRequired') }]" class="cell-item">
              <StockSlotSelect
                v-model:value="record.slot"
                :label="record.slotLabel"
                :warehouse-id="form.warehouseId"
                :material-id="record.materialId"
                @select="(s) => Object.assign(record, { locationId: s.locationId, batchNo: s.batchNo, slotLabel: s.label, available: Number(s.qty), serialNos: undefined })"
              />
            </a-form-item>
            <div v-if="record.available != null" class="sub-text">{{ $t('mes.woDocForm.available', { v: formatQty(record.available) }) }}</div>
          </template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-form-item
              :name="['items', index, 'batchNo']"
              :rules="record.batchEnabled === 1 ? [{ required: true, message: $t('mes.woDocForm.batchNoRequired') }] : []"
              class="cell-item"
            >
              <a-input v-model:value="record.batchNo" :disabled="record.batchEnabled !== 1" :maxlength="64" :placeholder="$t('mes.woDocForm.batchNo')" />
            </a-form-item>
            <template v-if="(kind === 'finish' || kind === 'scrap') && record.batchEnabled === 1">
              <a-date-picker v-model:value="record.productionDate" value-format="YYYY-MM-DD" :placeholder="$t('mes.woDocForm.productionDate')" class="full date-row" />
              <a-date-picker v-model:value="record.expiryDate" value-format="YYYY-MM-DD" :placeholder="$t('mes.woDocForm.expiryDate')" class="full date-row" />
            </template>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="[{ required: true, message: $t('mes.woDocForm.qtyRequired') }]" class="cell-item">
              <a-input-number
                v-model:value="record.qty"
                :min="0"
                :precision="record.serialEnabled === 1 ? 0 : 4"
                :addon-after="record.unit || undefined"
                class="full"
              />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'serialNos'">
            <SerialInput
              v-if="record.serialEnabled === 1"
              v-model:value="record.serialNos"
              :expected="record.qty ?? 0"
              :source="outbound && record.slot ? { warehouseId: form.warehouseId, locationId: record.locationId || undefined, materialId: record.materialId, batchNo: record.batchNo || undefined } : undefined"
            />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" :title="$t('mes.woDocForm.split')" @click="split(index)"><SplitCellsOutlined /></a-button>
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>
    </a-form>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="emit('update:open', false)">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" :disabled="!form.items.length" @click="submit">{{ $t('mes.woDocForm.confirmKind', { label: KINDS[kind].label }) }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, SplitCellsOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import StockSlotSelect from '@/components/scm/StockSlotSelect.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addWoDoc, issueSupplement } from '@/api/mes/wo'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { qty4 } from '@/components/scm/scm'

/**
 * 工单领料 / 退料 / 完工入库 / 补料发料：按工单用料（或产品、补料申请）生成明细，保存即生成仓库单据并更新库存
 * - wo：工单详情（含 materials），领料 / 退料 / 完工入库使用
 * - supplement：补料申请详情（含 items），kind 为 supplement 时使用
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, default: 'issue' },
  wo: Object,
  supplement: Object,
  /** 报废入库：已报废入库的数量 */
  scrapIn: { type: Number, default: 0 }
})
const emit = defineEmits(['update:open', 'success'])

const { t } = useI18n()
const KINDS = {
  issue: { label: t('mes.woDocForm.kind.issue'), tip: t('mes.woDocForm.tip.issue') },
  return: { label: t('mes.woDocForm.kind.return'), tip: t('mes.woDocForm.tip.return') },
  finish: { label: t('mes.woDocForm.kind.finish'), tip: t('mes.woDocForm.tip.finish') },
  scrap: { label: t('mes.woDocForm.kind.scrap'), tip: t('mes.woDocForm.tip.scrap') },
  supplement: { label: t('mes.woDocForm.kind.supplement'), tip: t('mes.woDocForm.tip.supplement') }
}
/** 从仓库现有库存中选择库位 / 批次 */
const outbound = computed(() => props.kind === 'issue' || props.kind === 'supplement')
const title = computed(() => {
  const label = KINDS[props.kind].label
  if (props.kind === 'supplement') return props.supplement ? t('mes.woDocForm.supplementTitle', { label, no: props.supplement.suppNo, wo: props.supplement.woNo }) : label
  return props.wo ? t('mes.woDocForm.woTitle', { label, wo: props.wo.woNo }) : label
})
const tip = computed(() => KINDS[props.kind].tip)

const columns = computed(() => [
  { title: props.kind === 'finish' || props.kind === 'scrap' ? t('mes.woDocForm.product') : t('mes.woDocForm.material'), key: 'material', width: 220 },
  { title: t('mes.woDocForm.limit.' + props.kind), key: 'limit', width: 150 },
  ...(outbound.value
    ? [{ title: t('mes.woDocForm.slot'), key: 'slot', width: 230 }]
    : [
        { title: t('mes.woDocForm.location'), key: 'location', width: 200 },
        { title: t('mes.woDocForm.batchNo'), key: 'batchNo', width: 160 }
      ]),
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('mes.woDocForm.serialNo'), key: 'serialNos', width: 240 },
  { title: '', key: 'action', width: 80, fixed: 'right' }
])
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))

const formRef = ref()
const form = ref({ warehouseId: undefined, bizDate: today(), remark: '', items: [] })
const saving = ref(false)

function buildRows() {
  if (props.kind === 'supplement') {
    return props.supplement.items.filter((i) => Number(i.qty) > Number(i.issuedQty || 0)).map((i) => ({
      key: rowKey(),
      materialId: i.materialId,
      materialCode: i.materialCode,
      materialName: i.materialName,
      unit: i.unit,
      batchEnabled: i.batchEnabled,
      serialEnabled: i.serialEnabled,
      limit: qty4(Number(i.qty) - Number(i.issuedQty || 0)),
      qty: qty4(Number(i.qty) - Number(i.issuedQty || 0)),
      slot: undefined,
      slotLabel: undefined,
      locationId: null,
      batchNo: undefined,
      available: undefined,
      serialNos: undefined
    }))
  }
  const wo = props.wo
  if (props.kind === 'finish' || props.kind === 'scrap') {
    // 报废入库：各工序报工报废数合计 − 已报废入库
    const scrapped = (wo.ops || []).reduce((sum, o) => sum + Number(o.scrapQty || 0), 0)
    const remaining = props.kind === 'scrap' ? qty4(scrapped - Number(props.scrapIn || 0)) : qty4(Number(wo.qty) - Number(wo.finishedQty))
    return remaining > 0
      ? [
          {
            key: rowKey(),
            materialId: wo.materialId,
            materialCode: wo.materialCode,
            materialName: wo.materialName,
            unit: wo.unit,
            batchEnabled: wo.batchEnabled,
            serialEnabled: wo.serialEnabled,
            limit: remaining,
            hint: props.kind === 'scrap' ? t('mes.woDocForm.scrapHint', { scrapped: formatQty(scrapped), stored: formatQty(props.scrapIn || 0) }) : undefined,
            qty: remaining
          }
        ]
      : []
  }
  return wo.materials
    .map((m) => {
      const issued = Number(m.issuedQty)
      const supplement = Number(m.supplementQty || 0)
      // 待领 = 需求 + 已补料 - 已领；超过需求的部分按物料的允许超领比例由后端校验
      const limit = props.kind === 'issue' ? Math.max(qty4(Number(m.requiredQty) + supplement - issued), 0) : issued
      return {
        key: rowKey(),
        materialId: m.materialId,
        materialCode: m.materialCode,
        materialName: m.materialName,
        unit: m.unit,
        batchEnabled: m.batchEnabled,
        serialEnabled: m.serialEnabled,
        limit,
        hint: props.kind === 'issue' ? (supplement ? t('mes.woDocForm.issueHintSupp', { req: formatQty(m.requiredQty), supp: formatQty(supplement), issued: formatQty(issued) }) : t('mes.woDocForm.issueHint', { req: formatQty(m.requiredQty), issued: formatQty(issued) })) : undefined,
        qty: props.kind === 'issue' && limit > 0 ? limit : undefined,
        slot: undefined,
        slotLabel: undefined,
        locationId: null,
        batchNo: undefined,
        available: undefined,
        serialNos: undefined
      }
    })
    .filter((r) => (props.kind === 'issue' ? r.limit > 0 : r.limit > 0))
}

watch(
  () => props.open,
  (open) => {
    if (!open || !(props.kind === 'supplement' ? props.supplement : props.wo)) return
    form.value = {
      warehouseId: props.kind === 'finish' ? props.wo.warehouseId || undefined : undefined,
      bizDate: today(),
      remark: '',
      items: buildRows()
    }
  }
)

function onWarehouseChange(id) {
  form.value.warehouseId = id
  form.value.items.forEach((i) => Object.assign(i, { slot: undefined, locationId: null, available: undefined, serialNos: undefined }))
}

function split(index) {
  const row = form.value.items[index]
  form.value.items.splice(index + 1, 0, { ...row, key: rowKey(), qty: undefined, slot: undefined, locationId: null, batchNo: undefined, serialNos: undefined, available: undefined })
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      warehouseId: form.value.warehouseId,
      bizDate: form.value.bizDate,
      remark: form.value.remark || null,
      items: form.value.items.map((i) => ({
        materialId: i.materialId,
        locationId: i.locationId || null,
        batchNo: i.batchNo || null,
        productionDate: i.productionDate || null,
        expiryDate: i.expiryDate || null,
        qty: i.qty,
        serialNos: i.serialNos || null
      }))
    }
    if (props.kind === 'supplement') {
      const docNo = await issueSupplement(props.supplement.id, payload)
      message.success(t('mes.woDocForm.issuedSupp', { no: docNo }))
    } else {
      await addWoDoc(props.wo.id, props.kind, payload)
      message.success(t('mes.woDocForm.kindSuccess', { label: KINDS[props.kind].label }))
    }
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.date-row {
  margin-top: 4px;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.row-tags:empty {
  display: none;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
