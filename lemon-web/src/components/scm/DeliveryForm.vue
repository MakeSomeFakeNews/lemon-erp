<template>
  <FormDrawer page :drawer="drawer" create-title="新增发货通知" width="min(1100px, 100vw)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      message="发货通知按销售订单的未出库数量生成，保存后按先到期先出自动生成拣货建议；仓库确认拣货后出库，自动生成销售出库单。"
    />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item label="销售订单" name="orderId">
            <a-select
              :value="form.orderId"
              show-search
              :filter-option="false"
              :options="orderOptions"
              :loading="orderLoading"
              :disabled="fixedOrder"
              placeholder="搜索订单号"
              :not-found-content="orderLoading ? '加载中…' : '没有可发货的订单'"
              @search="searchOrders"
              @focus="() => !orders.length && searchOrders('')"
              @change="selectOrder"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="发货仓库" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item label="发货日期" name="shipDate">
            <a-date-picker v-model:value="form.shipDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">明细（{{ form.items.length }} 行，数量 {{ formatQty(totalQty) }}）</span>
        <span v-if="partnerName" class="sub-text">客户：{{ partnerName }}</span>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: 860 }"
        :locale="{ emptyText: form.orderId ? '订单没有未出库且未通知的明细' : '请先选择销售订单' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'limit'">
            <div>{{ formatQty(record.limit) }} {{ record.unit }}</div>
            <div v-if="record.notified" class="sub-text">已通知未出库 {{ formatQty(record.notified) }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="qtyRule(record)" class="cell-item">
              <a-input-number
                v-model:value="record.qty"
                :min="0"
                :max="record.limit"
                :precision="record.serialEnabled === 1 ? 0 : 4"
                :addon-after="record.unit || undefined"
                class="full"
              />
            </a-form-item>
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { getOrder, pageOrder } from '@/api/scm/order'
import { addDelivery, getDelivery, pageDelivery } from '@/api/scm/planning'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { formatMoney, openQty, qty4 } from './scm'

/**
 * 新增发货通知：选择销售订单，按未出库且未通知的数量带出明细，可减少数量或删除行。
 * 通过 ref 调用 openCreate(orderId?)：传入订单时订单不可更换；保存成功后触发 success(id)
 */
const emit = defineEmits(['success'])

const columns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '可通知数量', key: 'limit', width: 150 },
  { title: '通知数量', key: 'qty', width: 170 },
  { title: '备注', key: 'remark', width: 200 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  orderId: requiredRule('请选择销售订单'),
  warehouseId: requiredRule('请选择发货仓库')
}
const qtyRule = (row) => [
  {
    validator: (_, v) => {
      if (!v || v <= 0) return Promise.reject(new Error('请填写数量'))
      if (v > row.limit) return Promise.reject(new Error(`不能超过 ${formatQty(row.limit)}`))
      return Promise.resolve()
    }
  }
]

const emptyForm = () => ({ id: undefined, orderId: undefined, warehouseId: undefined, shipDate: today(), remark: '', items: [] })

const drawer = useFormDrawer({
  emptyForm,
  create: async (data) => {
    if (!data.items.length) {
      message.warning('请保留至少一行明细')
      throw Object.assign(new Error('请保留至少一行明细'), { handled: true })
    }
    emit('success', await addDelivery(data))
  },
  update: () => Promise.reject(new Error('发货通知不能修改')),
  successMessage: '发货通知已保存',
  toPayload: (f) => ({
    orderId: f.orderId,
    warehouseId: f.warehouseId,
    shipDate: f.shipDate || null,
    remark: f.remark || null,
    items: f.items.map((i) => ({ orderItemId: i.orderItemId, qty: i.qty, remark: i.remark || null }))
  })
})
const { form, formRef } = drawer

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0))

// ---------------------------------------------------------------- 订单
const fixedOrder = ref(false)
const partnerName = ref('')
const orders = ref([])
const orderLoading = ref(false)
const orderOptions = computed(() =>
  orders.value.map((o) => ({ value: o.id, label: `${o.orderNo}　${o.partnerName}　¥${formatMoney(o.totalAmount)}` }))
)
let timer
function searchOrders(orderNo) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    orderLoading.value = true
    try {
      const res = await pageOrder('sales', { orderNo: orderNo || undefined, current: 1, size: 100 })
      orders.value = res.records.filter((o) => ['APPROVED', 'PARTIAL'].includes(o.status))
    } finally {
      orderLoading.value = false
    }
  }, 200)
}

/** 订单上待拣货、已拣货（未出库）的通知数量：orderItemId → 数量 */
async function notifiedQty(orderId) {
  const res = await pageDelivery({ orderId, current: 1, size: 100 })
  const active = res.records.filter((d) => ['PENDING', 'PICKED'].includes(d.status))
  const details = await Promise.all(active.map((d) => getDelivery(d.id)))
  const map = new Map()
  details.flatMap((d) => d.items || []).forEach((i) => map.set(i.orderItemId, (map.get(i.orderItemId) || 0) + Number(i.qty)))
  return map
}

async function loadOrder(orderId) {
  const [order, notified] = await Promise.all([getOrder('sales', orderId), notifiedQty(orderId)])
  partnerName.value = `${order.partnerCode} ${order.partnerName}`
  if (!orders.value.some((o) => o.id === order.id)) orders.value = [order, ...orders.value]
  const items = order.items
    .map((i) => {
      const done = notified.get(i.id) || 0
      return { i, notified: done, limit: Math.max(qty4(openQty(i) - done), 0) }
    })
    .filter(({ limit }) => limit > 0)
    .map(({ i, notified: n, limit }) => ({
      key: rowKey(),
      orderItemId: i.id,
      materialId: i.materialId,
      materialCode: i.materialCode,
      materialName: i.materialName,
      spec: i.spec,
      unit: i.unit,
      serialEnabled: i.serialEnabled,
      notified: n,
      limit,
      qty: limit,
      remark: undefined
    }))
  return { order, items }
}

async function selectOrder(orderId) {
  const { order, items } = await loadOrder(orderId)
  form.value.orderId = orderId
  if (order.warehouseId) form.value.warehouseId = order.warehouseId
  form.value.items = items
}

async function openCreate(orderId) {
  fixedOrder.value = !!orderId
  partnerName.value = ''
  orders.value = []
  if (!orderId) return drawer.openCreate()
  const { order, items } = await loadOrder(orderId)
  if (!['APPROVED', 'PARTIAL'].includes(order.status)) {
    message.warning(`订单 ${order.orderNo} 不是已审核或部分出库状态，不能发货`)
    return
  }
  if (!items.length) {
    message.warning('订单没有未出库且未通知的明细')
    return
  }
  return drawer.openCreate({ orderId: order.id, warehouseId: order.warehouseId || undefined, items })
}

defineExpose({ openCreate })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
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
