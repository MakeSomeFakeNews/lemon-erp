<template>
  <SubPage
    :open="open"
    :title="detail ? `${$t('scm.delivery.title')} ${detail.deliveryNo}` : $t('scm.delivery.title')"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="close"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" :disabled="!detail.picks?.length" @click="print">
          <template #icon><PrinterOutlined /></template>{{ $t('scm.delivery.printPickList') }}
        </a-button>
        <DictTag v-if="detail" type="scm_delivery_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('scm.delivery.deliveryNo')">{{ detail.deliveryNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.delivery.salesOrder')">
            <a @click="toOrder">{{ detail.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.delivery.customer')">{{ detail.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.delivery.shipWarehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.delivery.shipDate')">{{ detail.shipDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.delivery.prepared')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.pickTime" :label="$t('scm.delivery.picked')">{{ detail.pickByName || '-' }} {{ detail.pickTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.tradeDocId" :label="$t('scm.delivery.salesOutDoc')">
            <a @click="toTradeDoc(detail.tradeDocId)">{{ detail.tradeDocNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert v-if="editing" type="info" show-icon class="doc-desc" :message="editTip" />
        <a-alert
          v-else-if="detail.status === 'PENDING'"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('scm.delivery.pendingTip')"
        />

        <div v-for="item in detail.items" :key="item.id" class="pick-block">
          <div class="pick-head">
            <div>
              <b>{{ item.materialCode }} {{ item.materialName }}</b>
              <span class="sub-text"> {{ item.spec }}</span>
              <a-tag v-if="item.batchEnabled === 1" color="blue" :bordered="false" class="head-tag">{{ $t('scm.delivery.batch') }}</a-tag>
              <a-tag v-if="item.serialEnabled === 1" color="purple" :bordered="false" class="head-tag">{{ $t('scm.delivery.serialNo') }}</a-tag>
            </div>
            <a-space>
              <span>
                {{ $t('scm.delivery.noticedPicked', { qty: formatQty(item.qty), unit: item.unit || '' }) }}
                <b :class="{ mismatch: pickedQty(item) !== Number(item.qty) }">{{ formatQty(pickedQty(item)) }}</b>
              </span>
              <a-button v-if="editing" size="small" @click="addRow(item)">
                <template #icon><PlusOutlined /></template>{{ $t('scm.delivery.addLocation') }}
              </a-button>
            </a-space>
          </div>
          <a-table
            row-key="key"
            size="small"
            class="pick-table"
            :columns="editing ? editColumns : viewColumns"
            :data-source="rowsOf(item)"
            :pagination="false"
            :scroll="{ x: editing ? 820 : 700 }"
            :locale="{ emptyText: editing ? $t('scm.delivery.emptyEdit') : $t('scm.delivery.emptyView') }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'slot'">
                <template v-if="editing">
                  <StockSlotSelect
                    v-model:value="record.slot"
                    :label="record.slotLabel"
                    :warehouse-id="detail.warehouseId"
                    :material-id="item.materialId"
                    @select="(s) => applySlot(record, s)"
                  />
                  <div v-if="record.available != null" class="sub-text">{{ $t('scm.delivery.availableN', { qty: formatQty(record.available) }) }}</div>
                </template>
                <template v-else>
                  {{ locationText(record.locationCode) }}
                  <span v-if="record.batchNo" class="sub-text"> · {{ $t('scm.delivery.batchN', { batch: record.batchNo }) }}</span>
                </template>
              </template>
              <template v-else-if="column.key === 'expiryDate'">
                {{ record.expiryDate || '-' }}
                <a-tag v-if="expiryStatus(record.expiryDate)" :color="expiryStatus(record.expiryDate).color" :bordered="false">
                  {{ expiryStatus(record.expiryDate).text }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'qty'">
                <a-input-number
                  v-if="editing"
                  v-model:value="record.qty"
                  :min="0"
                  :precision="item.serialEnabled === 1 ? 0 : 4"
                  :addon-after="item.unit || undefined"
                  class="full"
                />
                <template v-else>{{ formatQty(record.qty) }} {{ item.unit }}</template>
              </template>
              <template v-else-if="column.key === 'serialNos'">
                <template v-if="item.serialEnabled === 1">
                  <SerialInput
                    v-if="editing"
                    v-model:value="record.serialNos"
                    :expected="record.qty ?? 0"
                    :source="serialSource(record, item)"
                    :placeholder="$t('scm.delivery.selectOrScan')"
                  />
                  <span v-else>{{ record.serialNos || '-' }}</span>
                </template>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="removeRow(record)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
        <DeliveryPacking
          v-if="!editing && ['PICKED', 'SHIPPED'].includes(detail.status)"
          :delivery="detail"
          editable
        />
      </template>
    </a-spin>

    <template v-if="detail && (actions.length || editing)" #footer>
      <div class="drawer-footer">
        <template v-if="editing">
          <a-button @click="editing = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="submitPick">
            <template #icon><CheckOutlined /></template>{{ $t('scm.delivery.confirmPick') }}
          </a-button>
        </template>
        <template v-else>
          <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
            <template #icon><component :is="a.icon" /></template>{{ a.label }}
          </a-button>
        </template>
      </div>
    </template>

    <a-modal v-model:open="shipOpen" :title="$t('scm.delivery.ship')" :confirm-loading="shipping" @ok="submitShip">
      <p>{{ $t('scm.delivery.shipTip') }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.delivery.bizDate')" required>
          <a-date-picker v-model:value="bizDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  CheckOutlined,
  DeleteOutlined,
  ExportOutlined,
  PlusOutlined,
  PrinterOutlined,
  StopOutlined,
  SyncOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SerialInput from '@/components/wms/SerialInput.vue'
import DeliveryPacking from './DeliveryPacking.vue'
import StockSlotSelect from './StockSlotSelect.vue'
import { cancelDelivery, getDelivery, pickDelivery, shipDelivery, suggestDelivery } from '@/api/scm/planning'
import { useUserStore } from '@/store/user'
import { expiryStatus, formatQty, locationText, rowKey, splitSerials, today } from '@/components/wms/wms'
import { qty4 } from './scm'
import { printDocs } from '@/utils/print'
import { useI18n } from 'vue-i18n'

/** 发货通知详情：拣货建议、确认拣货、出库、取消、打印拣货单。触发 changed */
const props = defineProps({
  open: Boolean,
  deliveryId: Number
})
const emit = defineEmits(['update:open', 'changed'])
const { t } = useI18n()

const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const router = useRouter()

const viewColumns = [
  { title: t('scm.delivery.locationBatch'), key: 'slot', width: 240 },
  { title: t('scm.delivery.expiryDate'), key: 'expiryDate', width: 180 },
  { title: t('common.field.quantity'), key: 'qty', width: 130, align: 'right' },
  { title: t('scm.delivery.serialNo'), key: 'serialNos', ellipsis: true }
]
const editColumns = [
  { title: t('scm.delivery.locationBatch'), key: 'slot', width: 280 },
  { title: t('common.field.quantity'), key: 'qty', width: 160 },
  { title: t('scm.delivery.serialNo'), key: 'serialNos', width: 300 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const detail = ref()
const loading = ref(false)
const editing = ref(false)
/** 当前显示的拣货行：查看时为已保存的拣货，编辑时为可修改的副本 */
const rows = ref([])

const editTip = t('scm.delivery.editTip')

const toRow = (p) => ({
  ...p,
  key: p.id ? `pick-${p.id}` : rowKey(),
  qty: Number(p.qty),
  slot: `${p.locationId || 0}|${p.batchNo || ''}`,
  slotLabel: `${locationText(p.locationCode)}${p.batchNo ? ' · ' + p.batchNo : ''}`,
  available: undefined
})

async function load() {
  loading.value = true
  try {
    detail.value = await getDelivery(props.deliveryId)
    rows.value = (detail.value.picks || []).map(toRow)
    editing.value = false
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.deliveryId,
  (v) => {
    detail.value = undefined
    editing.value = false
    if (v) load()
  }
)

function close() {
  if (!editing.value) return emit('update:open', false)
  Modal.confirm({
    title: t('scm.delivery.discardTitle'),
    okText: t('scm.delivery.discard'),
    okType: 'danger',
    cancelText: t('scm.delivery.continuePick'),
    onOk: () => emit('update:open', false)
  })
}

const rowsOf = (item) => rows.value.filter((r) => r.deliveryItemId === item.id)
const pickedQty = (item) => qty4(rowsOf(item).reduce((sum, r) => sum + (Number(r.qty) || 0), 0))

// ---------------------------------------------------------------- 拣货
function startPick() {
  rows.value = (detail.value.picks || []).map(toRow)
  editing.value = true
}

function addRow(item) {
  const left = Math.max(qty4(Number(item.qty) - pickedQty(item)), 0)
  rows.value.push({
    key: rowKey(),
    deliveryItemId: item.id,
    materialId: item.materialId,
    locationId: null,
    batchNo: '',
    slot: undefined,
    slotLabel: undefined,
    qty: left || undefined,
    serialNos: undefined,
    available: undefined
  })
}

function removeRow(row) {
  rows.value = rows.value.filter((r) => r !== row)
}

function applySlot(row, slot) {
  Object.assign(row, {
    locationId: slot.locationId,
    locationCode: slot.locationCode,
    batchNo: slot.batchNo,
    slotLabel: slot.label,
    available: Number(slot.qty),
    serialNos: undefined
  })
}

const serialSource = (row, item) =>
  row.slot
    ? {
        warehouseId: detail.value.warehouseId,
        locationId: row.locationId || undefined,
        materialId: item.materialId,
        batchNo: row.batchNo || undefined
      }
    : undefined

const saving = ref(false)
async function submitPick() {
  for (const item of detail.value.items) {
    const name = `${item.materialCode} ${item.materialName}`
    const list = rowsOf(item)
    if (list.some((r) => !r.slot)) return message.warning(t('scm.delivery.errSlot', { name }))
    if (list.some((r) => !r.qty || r.qty <= 0)) return message.warning(t('scm.delivery.errQty', { name }))
    if (pickedQty(item) !== qty4(Number(item.qty))) {
      return message.warning(t('scm.delivery.errMismatch', { name, picked: formatQty(pickedQty(item)), qty: formatQty(item.qty) }))
    }
    if (item.serialEnabled === 1) {
      const bad = list.find((r) => splitSerials(r.serialNos).length !== Number(r.qty))
      if (bad) return message.warning(t('scm.delivery.errSerial', { name }))
    }
  }
  saving.value = true
  try {
    await pickDelivery(props.deliveryId, {
      picks: rows.value.map((r) => ({
        deliveryItemId: r.deliveryItemId,
        locationId: r.locationId || null,
        batchNo: r.batchNo || null,
        qty: r.qty,
        serialNos: r.serialNos || null
      }))
    })
    message.success(t('scm.delivery.pickConfirmed'))
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 出库
const shipOpen = ref(false)
const shipping = ref(false)
const bizDate = ref(today())
async function submitShip() {
  if (!bizDate.value) return message.warning(t('scm.delivery.selectBizDate'))
  shipping.value = true
  try {
    const docId = await shipDelivery(props.deliveryId, bizDate.value)
    shipOpen.value = false
    await load()
    emit('changed')
    Modal.confirm({
      title: t('scm.delivery.shipped'),
      content: t('scm.delivery.shippedContent'),
      okText: t('scm.delivery.viewOutDoc'),
      cancelText: t('common.action.close'),
      onOk: () => toTradeDoc(docId)
    })
  } finally {
    shipping.value = false
  }
}

const toOrder = () => router.push({ path: '/scm/sales', query: { id: detail.value.orderId } })
const toTradeDoc = (id) => router.push({ path: '/scm/sales-out', query: { id } })

function confirmAction(title, content, api, success) {
  Modal.confirm({
    title,
    content,
    async onOk() {
      await api(props.deliveryId)
      message.success(success)
      await load()
      emit('changed')
    }
  })
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (['PENDING', 'PICKED'].includes(d.status) && can('scm:delivery:add')) {
    list.push({
      key: 'cancel',
      label: t('scm.delivery.cancelNotice'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(t('scm.delivery.cancelTitle'), t('scm.delivery.cancelContent'), cancelDelivery, t('scm.delivery.cancelled'))
    })
  }
  if (d.status === 'PENDING' && can('scm:delivery:pick')) {
    list.push({
      key: 'suggest',
      label: t('scm.delivery.resuggest'),
      icon: SyncOutlined,
      run: () => confirmAction(t('scm.delivery.resuggestTitle'), t('scm.delivery.resuggestContent'), suggestDelivery, t('scm.delivery.resuggested'))
    })
  }
  if (['PENDING', 'PICKED'].includes(d.status) && can('scm:delivery:pick')) {
    list.push({
      key: 'pick',
      label: d.status === 'PICKED' ? t('scm.delivery.editPick') : t('scm.delivery.confirmPick'),
      icon: UnorderedListOutlined,
      primary: d.status === 'PENDING',
      run: startPick
    })
  }
  if (d.status === 'PICKED' && can('scm:salesOut:add')) {
    list.push({
      key: 'ship',
      label: t('scm.delivery.ship'),
      icon: ExportOutlined,
      primary: true,
      run: () => {
        bizDate.value = today()
        shipOpen.value = true
      }
    })
  }
  return list
})

function print() {
  const d = detail.value
  const items = new Map(d.items.map((i) => [i.id, i]))
  const picks = [...d.picks].sort(
    (a, b) => String(a.locationCode || '').localeCompare(String(b.locationCode || '')) || a.deliveryItemId - b.deliveryItemId
  )
  printDocs(
    {
      title: t('scm.delivery.pickList'),
      no: d.deliveryNo,
      fields: [
        [t('scm.delivery.salesOrder'), d.orderNo],
        [t('scm.delivery.customer'), d.customerName],
        [t('scm.delivery.shipWarehouse'), d.warehouseName],
        [t('scm.delivery.shipDate'), d.shipDate],
        [t('scm.delivery.prepared'), d.createByName],
        [t('common.field.status'), { PENDING: t('scm.delivery.status.PENDING'), PICKED: t('scm.delivery.status.PICKED'), SHIPPED: t('scm.delivery.status.SHIPPED'), CANCELLED: t('scm.delivery.status.CANCELLED') }[d.status] || d.status]
      ],
      columns: [
        { title: t('common.table.index'), value: (r, i) => i + 1 },
        { title: t('scm.delivery.material'), value: (r) => `${r.materialCode} ${r.materialName}${items.get(r.deliveryItemId)?.spec ? ' ' + items.get(r.deliveryItemId).spec : ''}` },
        { title: t('scm.delivery.location'), value: (r) => locationText(r.locationCode) },
        { title: t('scm.delivery.batch'), value: (r) => r.batchNo || '' },
        { title: t('scm.delivery.expiryDate'), value: (r) => r.expiryDate || '' },
        { title: t('common.field.quantity'), key: 'qty', value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: t('scm.delivery.serialNo'), value: (r) => r.serialNos || '' }
      ],
      rows: picks,
      note: d.remark,
      signs: [[t('scm.delivery.prepared'), d.createByName], t('scm.delivery.picker'), t('scm.delivery.reviewer')]
    },
    `${t('scm.delivery.pickList')} ${d.deliveryNo}`,
    'scm.pickList'
  )
}

defineExpose({ reload: load })
</script>

<style scoped>
.doc-desc {
  margin-bottom: 16px;
}
.pick-block {
  margin-bottom: 16px;
}
.pick-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 6px 0;
}
.head-tag {
  margin-left: 6px;
}
.pick-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.mismatch {
  color: #ff4d4f;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
