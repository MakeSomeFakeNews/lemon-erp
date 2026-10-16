<template>
  <SubPage
    :open="open"
    :title="detail ? `发货通知 ${detail.deliveryNo}` : '发货通知'"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="close"
  >
    <template #extra>
      <a-space>
        <a-button v-if="detail" size="small" :disabled="!detail.picks?.length" @click="print">
          <template #icon><PrinterOutlined /></template>打印拣货单
        </a-button>
        <DictTag v-if="detail" type="scm_delivery_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="通知单号">{{ detail.deliveryNo }}</a-descriptions-item>
          <a-descriptions-item label="销售订单">
            <a @click="toOrder">{{ detail.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="客户">{{ detail.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发货仓库">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发货日期">{{ detail.shipDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.pickTime" label="拣货">{{ detail.pickByName || '-' }} {{ detail.pickTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.tradeDocId" label="销售出库单">
            <a @click="toTradeDoc(detail.tradeDocId)">{{ detail.tradeDocNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert v-if="editing" type="info" show-icon class="doc-desc" :message="editTip" />
        <a-alert
          v-else-if="detail.status === 'PENDING'"
          type="info"
          show-icon
          class="doc-desc"
          message="已按先到期先出生成拣货建议（库存不足的部分没有建议），请按实际拣货调整后确认拣货；确认后才能出库。"
        />

        <div v-for="item in detail.items" :key="item.id" class="pick-block">
          <div class="pick-head">
            <div>
              <b>{{ item.materialCode }} {{ item.materialName }}</b>
              <span class="sub-text"> {{ item.spec }}</span>
              <a-tag v-if="item.batchEnabled === 1" color="blue" :bordered="false" class="head-tag">批次</a-tag>
              <a-tag v-if="item.serialEnabled === 1" color="purple" :bordered="false" class="head-tag">序列号</a-tag>
            </div>
            <a-space>
              <span>
                通知 {{ formatQty(item.qty) }} {{ item.unit }}，已拣
                <b :class="{ mismatch: pickedQty(item) !== Number(item.qty) }">{{ formatQty(pickedQty(item)) }}</b>
              </span>
              <a-button v-if="editing" size="small" @click="addRow(item)">
                <template #icon><PlusOutlined /></template>添加库位
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
            :locale="{ emptyText: editing ? '点击「添加库位」选择从哪里拣货' : '没有拣货明细（库存不足）' }"
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
                  <div v-if="record.available != null" class="sub-text">可用 {{ formatQty(record.available) }}</div>
                </template>
                <template v-else>
                  {{ locationText(record.locationCode) }}
                  <span v-if="record.batchNo" class="sub-text"> · 批次 {{ record.batchNo }}</span>
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
                    placeholder="选择或扫码"
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
          <a-button @click="editing = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submitPick">
            <template #icon><CheckOutlined /></template>确认拣货
          </a-button>
        </template>
        <template v-else>
          <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
            <template #icon><component :is="a.icon" /></template>{{ a.label }}
          </a-button>
        </template>
      </div>
    </template>

    <a-modal v-model:open="shipOpen" title="出库" :confirm-loading="shipping" @ok="submitShip">
      <p>按拣货明细生成销售出库单并扣减库存，同时生成应收单，出库后不能修改。</p>
      <a-form layout="vertical">
        <a-form-item label="业务日期" required>
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

/** 发货通知详情：拣货建议、确认拣货、出库、取消、打印拣货单。触发 changed */
const props = defineProps({
  open: Boolean,
  deliveryId: Number
})
const emit = defineEmits(['update:open', 'changed'])

const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const router = useRouter()

const viewColumns = [
  { title: '库位 / 批次', key: 'slot', width: 240 },
  { title: '有效期', key: 'expiryDate', width: 180 },
  { title: '数量', key: 'qty', width: 130, align: 'right' },
  { title: '序列号', key: 'serialNos', ellipsis: true }
]
const editColumns = [
  { title: '库位 / 批次', key: 'slot', width: 280 },
  { title: '数量', key: 'qty', width: 160 },
  { title: '序列号', key: 'serialNos', width: 300 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]

const detail = ref()
const loading = ref(false)
const editing = ref(false)
/** 当前显示的拣货行：查看时为已保存的拣货，编辑时为可修改的副本 */
const rows = ref([])

const editTip = '每个物料的拣货数量合计须等于通知数量；同一物料可从多个库位、批次拣货，序列号管理的物料需选择序列号。'

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
    title: '放弃未确认的拣货？',
    okText: '放弃',
    okType: 'danger',
    cancelText: '继续拣货',
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
    if (list.some((r) => !r.slot)) return message.warning(`${name}：请选择库位 / 批次`)
    if (list.some((r) => !r.qty || r.qty <= 0)) return message.warning(`${name}：请填写拣货数量`)
    if (pickedQty(item) !== qty4(Number(item.qty))) {
      return message.warning(`${name}：拣货数量 ${formatQty(pickedQty(item))} 与通知数量 ${formatQty(item.qty)} 不一致`)
    }
    if (item.serialEnabled === 1) {
      const bad = list.find((r) => splitSerials(r.serialNos).length !== Number(r.qty))
      if (bad) return message.warning(`${name}：序列号个数与拣货数量不一致`)
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
    message.success('已确认拣货，可以出库')
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
  if (!bizDate.value) return message.warning('请选择业务日期')
  shipping.value = true
  try {
    const docId = await shipDelivery(props.deliveryId, bizDate.value)
    shipOpen.value = false
    await load()
    emit('changed')
    Modal.confirm({
      title: '已出库',
      content: '已生成销售出库单并扣减库存。',
      okText: '查看出库单',
      cancelText: '关闭',
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
      label: '取消通知',
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction('取消发货通知？', '取消后不能恢复，订单的未出库数量可以重新通知。', cancelDelivery, '已取消')
    })
  }
  if (d.status === 'PENDING' && can('scm:delivery:pick')) {
    list.push({
      key: 'suggest',
      label: '重新建议',
      icon: SyncOutlined,
      run: () => confirmAction('重新生成拣货建议？', '按当前库存以先到期先出重新分配，覆盖现有的拣货明细。', suggestDelivery, '已重新生成拣货建议')
    })
  }
  if (['PENDING', 'PICKED'].includes(d.status) && can('scm:delivery:pick')) {
    list.push({
      key: 'pick',
      label: d.status === 'PICKED' ? '修改拣货' : '确认拣货',
      icon: UnorderedListOutlined,
      primary: d.status === 'PENDING',
      run: startPick
    })
  }
  if (d.status === 'PICKED' && can('scm:salesOut:add')) {
    list.push({
      key: 'ship',
      label: '出库',
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
      title: '拣货单',
      no: d.deliveryNo,
      fields: [
        ['销售订单', d.orderNo],
        ['客户', d.customerName],
        ['发货仓库', d.warehouseName],
        ['发货日期', d.shipDate],
        ['制单', d.createByName],
        ['状态', { PENDING: '待拣货', PICKED: '已拣货', SHIPPED: '已出库', CANCELLED: '已取消' }[d.status] || d.status]
      ],
      columns: [
        { title: '序号', value: (r, i) => i + 1 },
        { title: '物料', value: (r) => `${r.materialCode} ${r.materialName}${items.get(r.deliveryItemId)?.spec ? ' ' + items.get(r.deliveryItemId).spec : ''}` },
        { title: '库位', value: (r) => locationText(r.locationCode) },
        { title: '批次', value: (r) => r.batchNo || '' },
        { title: '有效期', value: (r) => r.expiryDate || '' },
        { title: '数量', key: 'qty', value: (r) => `${formatQty(r.qty)} ${r.unit || ''}`, align: 'right' },
        { title: '序列号', value: (r) => r.serialNos || '' }
      ],
      rows: picks,
      note: d.remark,
      signs: [['制单', d.createByName], '拣货', '复核']
    },
    `拣货单 ${d.deliveryNo}`,
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
