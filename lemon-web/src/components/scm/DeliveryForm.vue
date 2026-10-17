<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.delivery.addTitle')" width="min(1100px, 100vw)">
    <a-alert
      type="info"
      show-icon
      class="tip"
      :message="$t('scm.delivery.formTip')"
    />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('scm.delivery.salesOrder')" name="orderId">
            <a-select
              :value="form.orderId"
              show-search
              :filter-option="false"
              :options="orderOptions"
              :loading="orderLoading"
              :disabled="fixedOrder"
              :placeholder="$t('scm.delivery.searchOrderNo')"
              :not-found-content="orderLoading ? $t('common.msg.loading') : $t('scm.delivery.noOrder')"
              @search="searchOrders"
              @focus="() => !orders.length && searchOrders('')"
              @change="selectOrder"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('scm.delivery.shipWarehouse')" name="warehouseId">
            <WarehouseSelect v-model:value="form.warehouseId" :allow-clear="false" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.delivery.shipDate')" name="shipDate">
            <a-date-picker v-model:value="form.shipDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('scm.delivery.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">{{ $t('scm.delivery.itemsSummary', { rows: form.items.length, qty: formatQty(totalQty) }) }}</span>
        <span v-if="partnerName" class="sub-text">{{ $t('scm.delivery.customerIs', { name: partnerName }) }}</span>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: 860 }"
        :locale="{ emptyText: form.orderId ? $t('scm.delivery.noOpenItems') : $t('scm.delivery.selectOrderFirst') }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'limit'">
            <div>{{ formatQty(record.limit) }} {{ record.unit }}</div>
            <div v-if="record.notified" class="sub-text">{{ $t('scm.delivery.notifiedN', { qty: formatQty(record.notified) }) }}</div>
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
import { useI18n } from 'vue-i18n'

/**
 * 新增发货通知：选择销售订单，按未出库且未通知的数量带出明细，可减少数量或删除行。
 * 通过 ref 调用 openCreate(orderId?)：传入订单时订单不可更换；保存成功后触发 success(id)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const columns = [
  { title: t('scm.delivery.material'), key: 'material', width: 260 },
  { title: t('scm.delivery.notifiableQty'), key: 'limit', width: 150 },
  { title: t('scm.delivery.noticeQty'), key: 'qty', width: 170 },
  { title: t('common.field.remark'), key: 'remark', width: 200 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  orderId: requiredRule(t('scm.delivery.selectOrder')),
  warehouseId: requiredRule(t('scm.delivery.selectWarehouse'))
}
const qtyRule = (row) => [
  {
    validator: (_, v) => {
      if (!v || v <= 0) return Promise.reject(new Error(t('scm.delivery.inputQty')))
      if (v > row.limit) return Promise.reject(new Error(t('scm.delivery.maxQty', { max: formatQty(row.limit) })))
      return Promise.resolve()
    }
  }
]

const emptyForm = () => ({ id: undefined, orderId: undefined, warehouseId: undefined, shipDate: today(), remark: '', items: [] })

const drawer = useFormDrawer({
  emptyForm,
  create: async (data) => {
    if (!data.items.length) {
      message.warning(t('scm.delivery.keepOneRow'))
      throw Object.assign(new Error(t('scm.delivery.keepOneRow')), { handled: true })
    }
    emit('success', await addDelivery(data))
  },
  update: () => Promise.reject(new Error(t('scm.delivery.cannotModify'))),
  successMessage: t('scm.delivery.saved'),
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
    message.warning(t('scm.delivery.orderNotShippable', { no: order.orderNo }))
    return
  }
  if (!items.length) {
    message.warning(t('scm.delivery.noOpenItems'))
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
