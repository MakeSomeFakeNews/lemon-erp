<template>
  <SubPage
    :open="open"
    :title="order ? $t('scm.orderChange.title', { kind: conf.label, no: order.orderNo }) : $t('scm.orderChange.defaultTitle')"
    width="min(1200px, 100vw)"
    destroy-on-close
    @close="close"
  >
    <template v-if="order">
      <a-alert
        type="info"
        show-icon
        class="block"
        :message="$t('scm.orderChange.tip')"
      />
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.orderChange.reason')" required>
          <a-textarea v-model:value="reason" :rows="2" :maxlength="255" show-count :placeholder="$t('scm.orderChange.reasonPlaceholder')" />
        </a-form-item>
      </a-form>

      <div class="items-head">
        <span class="section-title">
          {{ $t('scm.orderChange.existing') }}
          <span class="sub-text">{{ $t('scm.orderChange.counts', { changed: changedCount, added: adds.length }) }}</span>
        </span>
        <span>
          {{ $t('scm.order.totalAmount') }} {{ symbol }}{{ formatMoney(order.totalAmount) }}
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
            <div v-if="Number(record.returnQty)" class="sub-text">{{ $t('scm.orderChange.returned', { qty: formatQty(record.returnQty) }) }}</div>
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
              {{ $t('scm.orderChange.orig', { value: formatQty(record.qty) }) }}<template v-if="minQty(record) > 0">{{ $t('scm.orderChange.notLess', { qty: formatQty(minQty(record)) }) }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.newPrice" :min="0" :precision="4" :prefix="symbol" :disabled="record.remove" class="full" />
            <div class="sub-text">{{ $t('scm.orderChange.origPrice', { price: symbol + formatMoney(record.price), rate: Number(record.taxRate) }) }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.newDeliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.orderForm.sameAsOrder')" :disabled="record.remove" class="full" />
            <div class="sub-text">{{ $t('scm.orderChange.orig', { value: record.deliveryDate || $t('scm.orderForm.sameAsOrder') }) }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount" :class="{ removed: record.remove }">{{ symbol }}{{ formatMoney(lineAmount(record.newQty, record.newPrice)) }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-tooltip :title="removable(record) ? '' : $t('scm.orderChange.cannotRemove')">
              <a-checkbox v-model:checked="record.remove" :disabled="!removable(record)">{{ $t('common.action.delete') }}</a-checkbox>
            </a-tooltip>
            <div><a-tag v-if="lineAction(record)" :color="lineAction(record) === OrderChangeAction.REMOVE ? 'red' : 'orange'" :bordered="false">{{ lineAction(record) === OrderChangeAction.REMOVE ? $t('common.action.delete') : $t('scm.orderChange.modified') }}</a-tag></div>
          </template>
        </template>
      </a-table>

      <div class="items-head">
        <span class="section-title">{{ $t('scm.orderChange.addLines') }}</span>
        <a-button type="primary" ghost size="small" @click="addRow">
          <template #icon><PlusOutlined /></template>{{ $t('scm.orderChange.addLines') }}
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
        :scroll="{ x: addScrollX }"
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
            <a-date-picker v-model:value="record.deliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.orderForm.sameAsOrder')" class="full" />
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
        <a-button @click="close">{{ $t('common.action.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="submit">{{ $t('scm.orderChange.submit') }}</a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ArrowRightOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import { changeOrder, orderChanges } from '@/api/scm/order'
import { formatQty, rowKey } from '@/components/wms/wms'
import { currencySymbol } from '@/components/fin/currency'
import { ORDER_KINDS, formatMoney, lineAmount, qty4 } from './scm'

import { OrderChangeAction, OrderChangeStatus } from '@/constants/enums'
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

const { t } = useI18n()
const conf = computed(() => ORDER_KINDS[props.kind])
const symbol = computed(() => currencySymbol(props.order?.currency))

const lineColumns = [
  { title: t('scm.order.material'), key: 'material', width: 220 },
  { title: t('scm.orderChange.done'), key: 'done', width: 110, align: 'right' },
  { title: t('scm.orderChange.newQty'), key: 'qty', width: 180 },
  { title: t('scm.orderChange.newPrice'), key: 'price', width: 170 },
  { title: t('scm.orderChange.newDeliveryDate'), key: 'deliveryDate', width: 150 },
  { title: t('scm.order.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: '', key: 'action', width: 90 }
]
const addColumns = [
  { title: t('scm.order.material'), key: 'material', width: 290 },
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('scm.order.taxPrice'), key: 'price', width: 150 },
  { title: t('scm.order.taxRate'), key: 'taxRate', width: 130 },
  { title: t('scm.order.deliveryDate'), key: 'deliveryDate', width: 140 },
  { title: t('scm.order.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: '', key: 'action', width: 50 }
]
const addScrollX = addColumns.reduce((sum, c) => sum + c.width, 0)

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
  if (i.remove) return OrderChangeAction.REMOVE
  if (
    Number(i.newQty) !== Number(i.qty) ||
    Number(i.newPrice) !== Number(i.price) ||
    (i.newDeliveryDate || null) !== (i.deliveryDate || null)
  ) return OrderChangeAction.MODIFY
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
    message.warning(t('scm.orderChange.inputReason'))
    return
  }
  const changed = []
  for (const i of lines.value) {
    const action = lineAction(i)
    if (!action) continue
    if (action === OrderChangeAction.MODIFY) {
      if (i.newQty == null || Number(i.newQty) <= 0) return message.warning(t('scm.orderChange.qtyPositive', { code: i.materialCode }))
      if (Number(i.newQty) < minQty(i)) return message.warning(t('scm.orderChange.qtyBelowDone', { code: i.materialCode, qty: formatQty(minQty(i)) }))
      if (i.newPrice == null) return message.warning(t('scm.orderChange.inputPrice', { code: i.materialCode }))
    }
    changed.push(
      action === OrderChangeAction.REMOVE
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
    if (!a.materialId || !(Number(a.qty) > 0) || a.price == null) return message.warning(t('scm.orderChange.incompleteAdd', { n: n + 1 }))
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
    message.warning(t('scm.orderChange.noChange'))
    return
  }
  if (lines.value.every((i) => i.remove) && !adds.value.length) {
    message.warning(t('scm.orderChange.cannotRemoveAll'))
    return
  }
  saving.value = true
  try {
    const id = await changeOrder(props.kind, props.order.id, { reason: reason.value.trim(), lines: changed })
    const list = await orderChanges(props.kind, props.order.id).catch(() => [])
    const saved = list.find((c) => c.id === id)
    message.success(saved?.status === OrderChangeStatus.APPROVING ? t('scm.orderChange.submittedApproval') : t('scm.orderChange.effective'))
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
