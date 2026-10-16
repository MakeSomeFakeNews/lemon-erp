<template>
  <div>
    <div class="panel-head">
      <span class="sub-text">
        预留不锁定库位批次，只保证其他订单、出库和调拨不能占用；销售出库时核销本订单的预留，订单完成、关闭或反审核时自动释放。
      </span>
      <a-space v-if="editable">
        <a-button size="small" @click="openReserve">
          <template #icon><PushpinOutlined /></template>预留库存
        </a-button>
        <a-button size="small" @click="openAuto">
          <template #icon><ThunderboltOutlined /></template>一键预留
        </a-button>
        <a-popconfirm v-if="list.length" title="确定释放本订单的全部预留？" @confirm="releaseAll">
          <a-button size="small" danger>
            <template #icon><UnlockOutlined /></template>全部释放
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
      :locale="{ emptyText: '没有预留库存' }"
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

    <a-modal v-model:open="reserveOpen" title="预留库存" width="min(960px, 100vw)" :confirm-loading="saving" @ok="submitReserve">
      <p class="sub-text">从所选仓库的空闲库存（可用 − 已预留）中为订单明细预留，数量不填的行不预留。</p>
      <a-table
        row-key="key"
        size="small"
        class="reserve-table"
        :columns="reserveColumns"
        :data-source="reserveRows"
        :pagination="false"
        :scroll="{ x: 820 }"
        :locale="{ emptyText: '订单明细都已出库或已预留' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'need'">
            <div>{{ formatQty(record.need) }} {{ record.unit }}</div>
            <div v-if="record.reserved" class="sub-text">已预留 {{ formatQty(record.reserved) }}</div>
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

    <a-modal v-model:open="autoOpen" title="一键预留" :confirm-loading="saving" @ok="submitAuto">
      <p>按订单明细顺序，从仓库的空闲库存中预留未出库且未预留的数量，库存不足的部分不预留。</p>
      <a-form layout="vertical">
        <a-form-item label="仓库">
          <WarehouseSelect v-model:value="autoWarehouseId" placeholder="默认订单发货仓库" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PushpinOutlined, ThunderboltOutlined, UnlockOutlined } from '@ant-design/icons-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { autoReserveOrder, orderReservations, releaseOrderReservations, reserveOrder } from '@/api/scm/planning'
import { freeStock } from '@/api/wms/reservation'
import { formatQty, rowKey } from '@/components/wms/wms'
import { openQty, qty4 } from './scm'

/** 销售订单的库存预留：查看、手工预留、一键预留、全部释放 */
const props = defineProps({
  /** 订单详情（含 items） */
  order: { type: Object, required: true },
  editable: Boolean
})

const columns = [
  { title: '仓库', dataIndex: 'warehouseName', width: 140 },
  { title: '物料', key: 'material', width: 240 },
  { title: '预留数量', key: 'qty', width: 130, align: 'right' },
  { title: '说明', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '预留', key: 'create', width: 220 }
]
const reserveColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '待预留', key: 'need', width: 130 },
  { title: '仓库', key: 'warehouse', width: 190 },
  { title: '空闲库存', key: 'free', width: 110, align: 'right' },
  { title: '预留数量', key: 'qty', width: 170 }
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
  if (!lines.length) return message.warning('请填写预留数量')
  if (lines.some((r) => !r.warehouseId)) return message.warning('请选择仓库')
  const over = lines.find((r) => r.free != null && r.qty > r.free)
  if (over) return message.warning(`${over.materialCode} ${over.materialName}：预留数量超过空闲库存 ${formatQty(over.free)}`)
  saving.value = true
  try {
    await reserveOrder(props.order.id, { lines: lines.map((r) => ({ orderItemId: r.orderItemId, warehouseId: r.warehouseId, qty: r.qty })) })
    message.success('预留成功')
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
    n ? message.success(`已预留 ${n} 行`) : message.warning('没有可预留的空闲库存')
    autoOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function releaseAll() {
  await releaseOrderReservations(props.order.id)
  message.success('已释放')
  load()
}

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
