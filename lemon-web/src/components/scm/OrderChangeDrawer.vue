<template>
  <SubPage
    :open="open"
    :title="order ? `变更${conf.label} ${order.orderNo}` : '订单变更'"
    width="min(1200px, 100vw)"
    destroy-on-close
    @close="close"
  >
    <template v-if="order">
      <a-alert
        type="info"
        show-icon
        class="block"
        message="修改未完成明细的数量、单价、交货日期，删除未执行的明细或新增明细。发布了「订单变更审批」流程时审批通过后生效，生效后订单版本加 1；销售订单数量减少的明细会释放库存预留。数量不能小于已完成数量（已完成 − 退货补发）。"
      />
      <a-form layout="vertical">
        <a-form-item label="变更原因" required>
          <a-textarea v-model:value="reason" :rows="2" :maxlength="255" show-count placeholder="如：客户追加数量、供应商调价" />
        </a-form-item>
      </a-form>

      <div class="items-head">
        <span class="section-title">
          现有明细
          <span class="sub-text">（{{ changedCount }} 行修改 / 删除，{{ adds.length }} 行新增）</span>
        </span>
        <span>
          价税合计 {{ symbol }}{{ formatMoney(order.totalAmount) }}
          <ArrowRightOutlined class="arrow" />
          <b :class="{ diff: newTotal !== Number(order.totalAmount) }">{{ symbol }}{{ formatMoney(newTotal) }}</b>
        </span>
      </div>
      <a-table row-key="key" size="small" class="items-table" :columns="lineColumns" :data-source="lines" :pagination="false" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <div :class="{ removed: record.remove }">{{ record.materialCode }} {{ record.materialName }}</div>
            <div class="sub-text">{{ record.spec }}</div>
          </template>
          <template v-else-if="column.key === 'done'">
            <div>{{ formatQty(record.doneQty) }} {{ record.unit }}</div>
            <div v-if="Number(record.returnQty)" class="sub-text">退货 {{ formatQty(record.returnQty) }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number
              v-model:value="record.newQty"
              :min="minQty(record)"
              :precision="4"
              :disabled="record.remove"
              :addon-after="record.unit || undefined"
              class="full"
            />
            <div class="sub-text">
              原 {{ formatQty(record.qty) }}<template v-if="minQty(record) > 0">，不少于 {{ formatQty(minQty(record)) }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.newPrice" :min="0" :precision="4" :prefix="symbol" :disabled="record.remove" class="full" />
            <div class="sub-text">原 {{ symbol }}{{ formatMoney(record.price) }}，税率 {{ Number(record.taxRate) }}%</div>
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.newDeliveryDate" value-format="YYYY-MM-DD" placeholder="同订单" :disabled="record.remove" class="full" />
            <div class="sub-text">原 {{ record.deliveryDate || '同订单' }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount" :class="{ removed: record.remove }">{{ symbol }}{{ formatMoney(lineAmount(record.newQty, record.newPrice)) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-tooltip :title="removable(record) ? '' : '已执行（入库 / 出库或退货）的明细不能删除'">
              <a-checkbox v-model:checked="record.remove" :disabled="!removable(record)">删除</a-checkbox>
            </a-tooltip>
            <div><a-tag v-if="lineAction(record)" :color="lineAction(record) === 'REMOVE' ? 'red' : 'orange'" :bordered="false">{{ lineAction(record) === 'REMOVE' ? '删除' : '已修改' }}</a-tag></div>
          </template>
        </template>
      </a-table>

      <div class="items-head">
        <span class="section-title">新增明细</span>
        <a-button type="primary" ghost size="small" @click="addRow">
          <template #icon><PlusOutlined /></template>新增明细
        </a-button>
      </div>
      <a-table
        v-if="adds.length"
        row-key="key"
        size="small"
        class="items-table"
        :columns="addColumns"
        :data-source="adds"
        :pagination="false"
        :scroll="{ x: 1000 }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0" :precision="4" :addon-after="record.unit || undefined" class="full" />
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.price" :min="0" :precision="4" :prefix="symbol" class="full" />
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.deliveryDate" value-format="YYYY-MM-DD" placeholder="同订单" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount">{{ symbol }}{{ formatMoney(lineAmount(record.qty, record.price)) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="adds.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </template>

    <template #footer>
      <div class="drawer-footer">
        <a-button @click="close">取消</a-button>
        <a-button type="primary" :loading="saving" @click="submit">提交变更</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { changeOrder, orderChanges } from '@/api/scm/order'
import { formatQty, rowKey } from '@/components/wms/wms'
import { currencySymbol } from '@/components/fin/currency'
import { ORDER_KINDS, formatMoney, lineAmount, qty4 } from './scm'

/**
 * 订单变更：已审核 / 部分完成的订单修改明细数量、单价、交货日期，删除未执行的明细，新增明细。
 * 只提交有变化的明细；数量、单价按基本单位。提交成功后触发 success
 */
const props = defineProps({
  open: Boolean,
  kind: { type: String, required: true },
  /** 订单详情（含 items） */
  order: Object
})
const emit = defineEmits(['update:open', 'success'])

const conf = computed(() => ORDER_KINDS[props.kind])
const symbol = computed(() => currencySymbol(props.order?.currency))

const lineColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '已完成', key: 'done', width: 110, align: 'right' },
  { title: '新数量', key: 'qty', width: 180 },
  { title: '新含税单价', key: 'price', width: 170 },
  { title: '新交货日期', key: 'deliveryDate', width: 150 },
  { title: '价税合计', key: 'amount', width: 120, align: 'right' },
  { title: '', key: 'action', width: 90 }
]
const addColumns = [
  { title: '物料', key: 'material', width: 260 },
  { title: '数量', key: 'qty', width: 150 },
  { title: '含税单价', key: 'price', width: 140 },
  { title: '税率', key: 'taxRate', width: 110 },
  { title: '交货日期', key: 'deliveryDate', width: 140 },
  { title: '价税合计', key: 'amount', width: 120, align: 'right' },
  { title: '', key: 'action', width: 50 }
]

const reason = ref('')
const lines = ref([])
const adds = ref([])
const saving = ref(false)

watch(
  () => props.open && props.order,
  (o) => {
    if (!o) return
    reason.value = ''
    adds.value = []
    lines.value = o.items.map((i) => ({
      ...i,
      key: `item-${i.id}`,
      newQty: Number(i.qty),
      newPrice: Number(i.price),
      newDeliveryDate: i.deliveryDate || undefined,
      remove: false
    }))
  },
  { immediate: true }
)

/** 数量下限：已完成 − 退货补发 */
const minQty = (i) => Math.max(qty4(Number(i.doneQty || 0) - Number(i.replenishQty || 0)), 0)
const removable = (i) => !Number(i.doneQty) && !Number(i.returnQty)

/** 明细的变更动作：REMOVE / MODIFY / 无变化 */
function lineAction(i) {
  if (i.remove) return 'REMOVE'
  if (
    Number(i.newQty) !== Number(i.qty) ||
    Number(i.newPrice) !== Number(i.price) ||
    (i.newDeliveryDate || null) !== (i.deliveryDate || null)
  ) return 'MODIFY'
  return null
}
const changedCount = computed(() => lines.value.filter(lineAction).length)

const newTotal = computed(() => {
  const kept = lines.value.filter((i) => !i.remove).reduce((s, i) => s + lineAmount(i.newQty, i.newPrice), 0)
  const added = adds.value.reduce((s, i) => s + lineAmount(i.qty, i.price), 0)
  return Math.round((kept + added) * 100) / 100
})

function addRow() {
  adds.value.push({ key: rowKey(), materialId: undefined, qty: undefined, price: undefined, taxRate: 13, deliveryDate: undefined })
}

function applyMaterial(row, m) {
  Object.assign(row, { materialLabel: m.label, materialCode: m.code, materialName: m.name, unit: m.unit })
}

function close() {
  emit('update:open', false)
}

async function submit() {
  if (!reason.value.trim()) {
    message.warning('请填写变更原因')
    return
  }
  const changed = []
  for (const i of lines.value) {
    const action = lineAction(i)
    if (!action) continue
    if (action === 'MODIFY') {
      if (i.newQty == null || Number(i.newQty) <= 0) return message.warning(`${i.materialCode} 的数量必须大于 0，不需要的明细请勾选删除`)
      if (Number(i.newQty) < minQty(i)) return message.warning(`${i.materialCode} 的数量不能小于已完成数量 ${formatQty(minQty(i))}`)
      if (i.newPrice == null) return message.warning(`请填写 ${i.materialCode} 的单价`)
    }
    changed.push(
      action === 'REMOVE'
        ? { action, orderItemId: i.id }
        : {
            action,
            orderItemId: i.id,
            materialId: i.materialId,
            qty: i.newQty,
            price: i.newPrice,
            taxRate: i.taxRate,
            deliveryDate: i.newDeliveryDate || null
          }
    )
  }
  for (const [n, a] of adds.value.entries()) {
    if (!a.materialId || !(Number(a.qty) > 0) || a.price == null) return message.warning(`请完整填写第 ${n + 1} 行新增明细的物料、数量、单价`)
    changed.push({
      action: 'ADD',
      materialId: a.materialId,
      qty: a.qty,
      price: a.price,
      taxRate: a.taxRate ?? 0,
      deliveryDate: a.deliveryDate || null
    })
  }
  if (!changed.length) {
    message.warning('没有修改任何明细')
    return
  }
  if (lines.value.every((i) => i.remove) && !adds.value.length) {
    message.warning('不能删除全部明细，请改为关闭订单')
    return
  }
  saving.value = true
  try {
    const id = await changeOrder(props.kind, props.order.id, { reason: reason.value.trim(), lines: changed })
    const list = await orderChanges(props.kind, props.order.id).catch(() => [])
    const saved = list.find((c) => c.id === id)
    message.success(saved?.status === 'APPROVING' ? '变更已提交审批，审批通过后生效' : '变更已生效，订单版本加 1')
    close()
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.items-table {
  margin-bottom: 16px;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.full {
  width: 100%;
}
.amount {
  line-height: 32px;
}
.removed {
  text-decoration: line-through;
  color: var(--app-text-3);
}
.diff {
  color: #fa8c16;
}
.arrow {
  color: var(--app-text-3);
  font-size: 12px;
  margin: 0 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
