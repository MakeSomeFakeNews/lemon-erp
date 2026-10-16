<template>
  <FormDrawer page :drawer="drawer" :create-title="`新增${conf.label}`" width="min(1280px, 100vw)">
    <a-alert type="info" show-icon class="tip" :message="tip" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="ORDER_KINDS[conf.orderKind].label" name="orderId">
            <a-select
              :value="form.orderId"
              show-search
              :filter-option="false"
              :options="orderOptions"
              :loading="orderLoading"
              :disabled="fixedOrder"
              placeholder="搜索订单号"
              :not-found-content="orderLoading ? '加载中…' : `没有可${conf.action}的订单`"
              @search="searchOrders"
              @focus="() => !orders.length && searchOrders('')"
              @change="selectOrder"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="仓库" name="warehouseId">
            <WarehouseSelect :value="form.warehouseId" :allow-clear="false" @update:value="onWarehouseChange" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item label="业务日期" name="bizDate">
            <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" placeholder="选填" />
          </a-form-item>
        </a-col>
        <a-col v-if="isReturn" :span="24">
          <a-form-item name="replenish" class="replenish">
            <a-checkbox v-model:checked="form.replenish">
              退货后需要{{ conf.orderKind === 'sales' ? '补发' : '补收' }}
              <span class="sub-text">：勾选后订单恢复相应的未完成数量，可继续{{ conf.orderKind === 'sales' ? '出库' : '入库' }}；不勾选视为退款，订单不再补发</span>
            </a-checkbox>
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">
          明细（{{ form.items.length }} 行，数量 {{ formatQty(totalQty) }}，价税合计 ¥{{ formatMoney(totalAmount) }}）
        </span>
        <a-space>
          <span v-if="partnerName" class="sub-text">{{ ORDER_KINDS[conf.orderKind].partnerLabel }}：{{ partnerName }}</span>
          <a-button v-if="inbound" size="small" :disabled="!form.warehouseId || !form.items.length" @click="putaway">推荐库位</a-button>
        </a-space>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: form.orderId ? `订单没有可${conf.action}的明细` : '请先选择订单' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <div>{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
            <div class="row-tags">
              <a-tag v-if="record.batchEnabled === 1" color="blue" :bordered="false">批次</a-tag>
              <a-tag v-if="record.serialEnabled === 1" color="purple" :bordered="false">序列号</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'limit'">
            <div>{{ formatQty(record.limit) }} {{ record.unit }}</div>
            <div class="sub-text">¥{{ formatMoney(record.price) }} / {{ record.unit || '单位' }}</div>
          </template>
          <template v-else-if="column.key === 'slot'">
            <a-form-item :name="['items', index, 'slot']" :rules="requiredRule('请选择库位 / 批次')" class="cell-item">
              <StockSlotSelect
                v-model:value="record.slot"
                :label="record.slotLabel"
                :warehouse-id="form.warehouseId"
                :material-id="record.materialId"
                :include-frozen="kind === 'purchaseReturn'"
                @select="(s) => applySlot(record, s)"
              />
            </a-form-item>
            <div v-if="record.available != null" class="sub-text">可用 {{ formatQty(record.available) }}</div>
          </template>
          <template v-else-if="column.key === 'location'">
            <LocationSelect v-model:value="record.locationId" :warehouse-id="form.warehouseId" />
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-form-item
              :name="['items', index, 'batchNo']"
              :rules="record.batchEnabled === 1 ? requiredRule('请填写批次号') : []"
              class="cell-item"
            >
              <a-input v-model:value="record.batchNo" :maxlength="64" :disabled="record.batchEnabled !== 1" placeholder="批次号" />
            </a-form-item>
            <template v-if="kind === 'purchaseIn' && record.batchEnabled === 1">
              <a-date-picker v-model:value="record.productionDate" value-format="YYYY-MM-DD" placeholder="生产日期" class="full date-row" />
              <a-date-picker v-model:value="record.expiryDate" value-format="YYYY-MM-DD" placeholder="有效期至" class="full date-row" />
            </template>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'qty']" :rules="requiredRule('请填写数量')" class="cell-item">
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
              :source="conf.fromStock ? serialSource(record) : undefined"
              :placeholder="conf.fromStock ? '选择或扫码' : '扫码或输入序列号'"
            />
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-tooltip title="拆分：同一物料从多个库位 / 批次出入">
              <a-button type="link" size="small" @click="splitRow(index)">
                <template #icon><SplitCellsOutlined /></template>
              </a-button>
            </a-tooltip>
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
import { DeleteOutlined, SplitCellsOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LocationSelect from '@/components/wms/LocationSelect.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import StockSlotSelect from './StockSlotSelect.vue'
import { getOrder, pageOrder } from '@/api/scm/order'
import { addTrade } from '@/api/scm/trade'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { fillPutaway, formatQty, rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, TRADE_KINDS, formatMoney, lineAmount, netQty, remainingQty } from './scm'

/**
 * 采购入库 / 采购退货 / 销售出库 / 销售退货：选择订单后按订单明细录入，保存即确认（更新库存）。
 * 通过 ref 调用 openCreate(order?)：传入订单时订单不可更换；保存成功后触发 success
 */
const props = defineProps({
  kind: { type: String, required: true }
})
const emit = defineEmits(['success'])

const conf = computed(() => TRADE_KINDS[props.kind])
const tip = computed(() => {
  const c = conf.value
  const stock = c.fromStock ? '从所选仓库的现有库存中选择库位、批次（序列号）' : '录入入库的库位、批次（序列号）'
  return `${stock}。保存后立即生成仓库${c.fromStock ? '出库单' : '入库单'}并更新库存，${
    c.orderKind === 'purchase' ? '同时生成应付单' : '同时生成应收单'
  }，单据不能修改，错误时请${c.returns ? '重新入库 / 出库' : '做退货'}冲回。`
})

const columns = computed(() => {
  const c = conf.value
  const list = [
    { title: '物料', key: 'material', width: 230 },
    { title: c.returns ? '可退数量' : '未完成数量', key: 'limit', width: 130 },
    ...(c.fromStock
      ? [{ title: '库位 / 批次', key: 'slot', width: 230 }]
      : [
          { title: '库位', key: 'location', width: 150 },
          { title: '批次号', key: 'batchNo', width: 160 }
        ]),
    { title: '数量', key: 'qty', width: 150 },
    { title: '序列号', key: 'serialNos', width: 260 },
    { title: '', key: 'action', width: 80, fixed: 'right' }
  ]
  return list
})
const scrollX = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0))

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = {
  orderId: requiredRule('请选择订单'),
  warehouseId: requiredRule('请选择仓库'),
  bizDate: requiredRule('请选择业务日期')
}

const emptyForm = () => ({ id: undefined, orderId: undefined, warehouseId: undefined, bizDate: today(), remark: '', replenish: false, items: [] })
const isReturn = computed(() => props.kind === 'purchaseReturn' || props.kind === 'salesReturn')
/** 入库类单据（采购入库、销售退货）可按上架策略推荐库位 */
const inbound = computed(() => props.kind === 'purchaseIn' || props.kind === 'salesReturn')
async function putaway() {
  const n = await fillPutaway(form.value.warehouseId, form.value.items)
  message.info(n ? `已为 ${n} 行推荐库位` : '没有可推荐的库位（已选库位的行不变）')
}

const drawer = useFormDrawer({
  emptyForm,
  create: async (data) => emit('success', await addTrade(props.kind, data)),
  update: () => Promise.reject(new Error('单据不能修改')),
  successMessage: `${TRADE_KINDS[props.kind].label}成功`,
  toPayload: (f) => ({
    orderId: f.orderId,
    warehouseId: f.warehouseId,
    bizDate: f.bizDate,
    remark: f.remark,
    replenish: isReturn.value ? f.replenish : null,
    items: f.items.map((i) => ({
      orderItemId: i.orderItemId,
      locationId: i.locationId || null,
      batchNo: i.batchNo || null,
      productionDate: i.productionDate || null,
      expiryDate: i.expiryDate || null,
      qty: i.qty,
      serialNos: i.serialNos || null
    }))
  })
})
const { form, formRef } = drawer

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0))
const totalAmount = computed(() => form.value.items.reduce((sum, i) => sum + lineAmount(i.qty, i.price), 0))

// ---------------------------------------------------------------- 订单
const fixedOrder = ref(false)
const partnerName = ref('')
const orders = ref([])
const orderLoading = ref(false)
const allowedStatus = computed(() => (conf.value.returns ? ['PARTIAL', 'COMPLETED', 'CLOSED'] : ['APPROVED', 'PARTIAL']))
const orderOptions = computed(() =>
  orders.value.map((o) => ({ value: o.id, label: `${o.orderNo}　${o.partnerName}　¥${formatMoney(o.totalAmount)}` }))
)
let timer
function searchOrders(orderNo) {
  clearTimeout(timer)
  timer = setTimeout(async () => {
    orderLoading.value = true
    try {
      const res = await pageOrder(conf.value.orderKind, { orderNo: orderNo || undefined, current: 1, size: 100 })
      orders.value = res.records.filter((o) => allowedStatus.value.includes(o.status))
    } finally {
      orderLoading.value = false
    }
  }, 200)
}

/** 订单明细 → 表单行：入库 / 出库默认数量为未完成数量；退货默认不填 */
function toRows(order) {
  return order.items
    .map((i) => ({ i, limit: conf.value.returns ? netQty(i) : remainingQty(i) }))
    .filter(({ limit }) => limit > 0)
    .map(({ i, limit }) => ({
      key: rowKey(),
      orderItemId: i.id,
      materialId: i.materialId,
      materialCode: i.materialCode,
      materialName: i.materialName,
      spec: i.spec,
      unit: i.unit,
      batchEnabled: i.batchEnabled,
      serialEnabled: i.serialEnabled,
      price: Number(i.price),
      limit,
      qty: conf.value.returns ? undefined : limit,
      locationId: null,
      batchNo: undefined,
      slot: undefined,
      slotLabel: undefined,
      available: undefined,
      serialNos: undefined
    }))
}

async function loadOrder(orderId) {
  const order = await getOrder(conf.value.orderKind, orderId)
  partnerName.value = `${order.partnerCode} ${order.partnerName}`
  if (!orders.value.some((o) => o.id === order.id)) orders.value = [order, ...orders.value]
  return order
}

async function selectOrder(orderId) {
  const order = await loadOrder(orderId)
  form.value.orderId = orderId
  if (order.warehouseId && !form.value.warehouseId) form.value.warehouseId = order.warehouseId
  form.value.items = toRows(order)
}

async function openCreate(order) {
  fixedOrder.value = !!order
  partnerName.value = ''
  orders.value = []
  if (!order) return drawer.openCreate()
  const full = await loadOrder(order.id)
  const items = toRows(full)
  if (!items.length) {
    message.warning(`订单没有可${conf.value.action}的明细`)
    return
  }
  return drawer.openCreate({ orderId: full.id, warehouseId: full.warehouseId || undefined, items })
}

// ---------------------------------------------------------------- 明细
function onWarehouseChange(id) {
  if (id === form.value.warehouseId) return
  form.value.warehouseId = id
  form.value.items.forEach((i) => Object.assign(i, { locationId: null, slot: undefined, available: undefined, serialNos: undefined }))
}

function applySlot(row, slot) {
  Object.assign(row, { locationId: slot.locationId, batchNo: slot.batchNo, slotLabel: slot.label, available: Number(slot.qty), serialNos: undefined })
}

const serialSource = (row) =>
  row.slot
    ? { warehouseId: form.value.warehouseId, locationId: row.locationId || undefined, materialId: row.materialId, batchNo: row.batchNo || undefined }
    : undefined

function splitRow(index) {
  const row = form.value.items[index]
  form.value.items.splice(index + 1, 0, {
    ...row,
    key: rowKey(),
    qty: undefined,
    slot: undefined,
    locationId: null,
    batchNo: undefined,
    available: undefined,
    serialNos: undefined
  })
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
.date-row {
  margin-top: 4px;
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
.row-tags {
  margin-top: 4px;
}
.row-tags:empty {
  display: none;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
