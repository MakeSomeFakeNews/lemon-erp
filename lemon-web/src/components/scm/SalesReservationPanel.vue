<template>
  <div>
    <div class="panel-head">
      <span class="sub-text">
        {{ $t('scm.reservation.tip') }}
      </span>
      <a-space v-if="editable">
        <a-button size="small" @click="openReserve">
          <template #icon><PushpinOutlined /></template>{{ $t('scm.reservation.reserve') }}
        </a-button>
        <a-button size="small" @click="openAuto">
          <template #icon><ThunderboltOutlined /></template>{{ $t('scm.reservation.auto') }}
        </a-button>
        <a-popconfirm v-if="list.length" :title="$t('scm.reservation.confirmReleaseAll')" @confirm="releaseAll">
          <a-button size="small" danger>
            <template #icon><UnlockOutlined /></template>{{ $t('scm.reservation.releaseAll') }}
          </a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="false"
      :scroll="{ x: 800 }"
      :locale="{ emptyText: $t('scm.reservation.empty') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
        <template v-else-if="column.key === 'create'">{{ record.createByName || '-' }} {{ record.createTime }}</template>
      </template>
    </a-table>

    <a-modal v-model:open="reserveOpen" :title="$t('scm.reservation.reserve')" width="min(960px, 100vw)" :confirm-loading="saving" @ok="submitReserve">
      <p class="sub-text">{{ $t('scm.reservation.reserveTip') }}</p>
      <a-table
        row-key="key"
        size="small"
        class="reserve-table"
        :columns="reserveColumns"
        :data-source="reserveRows"
        :pagination="false"
        :scroll="{ x: 820 }"
        :locale="{ emptyText: $t('scm.reservation.allDone') }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'need'">
            <div>{{ formatQty(record.need) }} {{ record.unit }}</div>
            <div v-if="record.reserved" class="sub-text">{{ $t('scm.reservation.reservedValue', { qty: formatQty(record.reserved) }) }}</div>
          </template>
          <template v-else-if="column.key === 'warehouse'">
            <WarehouseSelect :value="record.warehouseId" :allow-clear="false" @update:value="(v) => changeWarehouse(record, v)" />
          </template>
          <template v-else-if="column.key === 'free'">
            <span v-if="record.free == null" class="sub-text">-</span>
            <span v-else :class="{ short: record.free < record.need }">{{ formatQty(record.free) }}</span>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number
              v-model:value="record.qty"
              :min="0"
              :max="record.need"
              :precision="record.serialEnabled === 1 ? 0 : 4"
              :addon-after="record.unit || undefined"
              class="full"
            />
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="autoOpen" :title="$t('scm.reservation.auto')" :confirm-loading="saving" @ok="submitAuto">
      <p>{{ $t('scm.reservation.autoTip') }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.order.warehouse')">
          <WarehouseSelect v-model:value="autoWarehouseId" :placeholder="$t('scm.reservation.defaultWarehouse')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PushpinOutlined, ThunderboltOutlined, UnlockOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { autoReserveOrder, orderReservations, releaseOrderReservations, reserveOrder } from '@/api/scm/planning'
import { freeStock } from '@/api/wms/reservation'
import { formatQty, rowKey } from '@/components/wms/wms'
import { openQty, qty4 } from './scm'
import { useAction } from '@/composables/useAction'

/** 销售订单的库存预留：查看、手工预留、一键预留、全部释放 */
const props = defineProps({
  /** 订单详情（含 items） */
  order: { type: Object, required: true },
  editable: Boolean
})

const { t } = useI18n()
const columns = [
  { title: t('scm.order.warehouse'), dataIndex: 'warehouseName', width: 140 },
  { title: t('scm.order.material'), key: 'material', width: 240 },
  { title: t('scm.reservation.qty'), key: 'qty', width: 130, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('scm.reservation.reservedBy'), key: 'create', width: 220 }
]
const reserveColumns = [
  { title: t('scm.order.material'), key: 'material', width: 220 },
  { title: t('scm.reservation.need'), key: 'need', width: 130 },
  { title: t('scm.order.warehouse'), key: 'warehouse', width: 190 },
  { title: t('scm.reservation.free'), key: 'free', width: 110, align: 'right' },
  { title: t('scm.reservation.qty'), key: 'qty', width: 170 }
]

const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await orderReservations(props.order.id)
  } finally {
    loading.value = false
  }
}
onMounted(load)

// ---------------------------------------------------------------- 手工预留
const reserveOpen = ref(false)
const reserveRows = ref([])
const saving = ref(false)

const reservedOf = (itemId) => qty4(list.value.filter((r) => r.sourceItemId === itemId).reduce((sum, r) => sum + Number(r.qty), 0))

/** 按仓库查询空闲库存，填到对应的行 */
async function loadFree(rows) {
  const byWarehouse = new Map()
  rows.filter((r) => r.warehouseId).forEach((r) => byWarehouse.set(r.warehouseId, [...(byWarehouse.get(r.warehouseId) || []), r]))
  await Promise.all(
    [...byWarehouse].map(async ([warehouseId, group]) => {
      const free = await freeStock(warehouseId, [...new Set(group.map((r) => r.materialId))])
      group.forEach((r) => {
        r.free = Number(free[r.materialId] ?? 0)
        if (r.qty === undefined) r.qty = Math.min(r.need, Math.max(r.free, 0)) || undefined
      })
    })
  )
}

async function openReserve() {
  await load()
  reserveRows.value = props.order.items
    .map((i) => {
      const reserved = reservedOf(i.id)
      return {
        key: rowKey(),
        orderItemId: i.id,
        materialId: i.materialId,
        materialCode: i.materialCode,
        materialName: i.materialName,
        spec: i.spec,
        unit: i.unit,
        serialEnabled: i.serialEnabled,
        reserved,
        need: Math.max(qty4(openQty(i) - reserved), 0),
        warehouseId: props.order.warehouseId || undefined,
        free: undefined,
        qty: undefined
      }
    })
    .filter((r) => r.need > 0)
  reserveOpen.value = true
  await loadFree(reserveRows.value)
}

async function changeWarehouse(row, warehouseId) {
  Object.assign(row, { warehouseId, free: undefined, qty: undefined })
  if (warehouseId) await loadFree([row])
}

async function submitReserve() {
  const lines = reserveRows.value.filter((r) => r.qty > 0)
  if (!lines.length) return message.warning(t('scm.reservation.inputQty'))
  if (lines.some((r) => !r.warehouseId)) return message.warning(t('scm.reservation.selectWarehouse'))
  const over = lines.find((r) => r.free != null && r.qty > r.free)
  if (over) return message.warning(t('scm.reservation.overFree', { material: `${over.materialCode} ${over.materialName}`, qty: formatQty(over.free) }))
  saving.value = true
  try {
    await reserveOrder(props.order.id, { lines: lines.map((r) => ({ orderItemId: r.orderItemId, warehouseId: r.warehouseId, qty: r.qty })) })
    message.success(t('scm.reservation.success'))
    reserveOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 一键预留
const autoOpen = ref(false)
const autoWarehouseId = ref()
function openAuto() {
  autoWarehouseId.value = props.order.warehouseId || undefined
  autoOpen.value = true
}
async function submitAuto() {
  saving.value = true
  try {
    const n = await autoReserveOrder(props.order.id, autoWarehouseId.value || undefined)
    n ? message.success(t('scm.reservation.autoDone', { n })) : message.warning(t('scm.reservation.noFree'))
    autoOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const releaseAll = useAction(() => releaseOrderReservations(props.order.id), { successKey: 'scm.reservation.released', reload: () => load() })

defineExpose({ reload: load })
</script>

<style scoped>
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.reserve-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.full {
  width: 100%;
}
.short {
  color: #ff4d4f;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
