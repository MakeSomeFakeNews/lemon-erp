<template>
  <SubPage
    :open="open"
    :title="detail ? $t('scm.requisition.titleNo', { no: detail.reqNo }) : $t('scm.requisition.title')"
    width="min(1000px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <DictTag v-if="detail" type="scm_requisition_status" :value="detail.status" />
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('scm.requisition.reqNo')">{{ detail.reqNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.requisition.reqDate')">{{ detail.reqDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.requisition.dept')">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.requisition.creator')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.approveTime" :label="$t('scm.requisition.approveTime')">{{ detail.approveTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('scm.requisition.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('scm.requisition.approvingTip')"
        />
        <a-alert
          v-else-if="detail.status === 'APPROVED'"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('scm.requisition.splitTip')"
        />

        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'supplier'">{{ record.supplierName || '-' }}</template>
            <template v-else-if="column.key === 'order'">
              <a v-if="record.orderId" @click="toOrder(record.orderId)">{{ record.orderNo }}</a>
              <span v-else class="sub-text">-</span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>

    <a-modal v-model:open="orderOpen" :title="$t('scm.requisition.toOrder')" :confirm-loading="ordering" @ok="submitOrder">
      <p>{{ $t('scm.requisition.toOrderTip') }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.requisition.warehouse')">
          <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('scm.requisition.warehousePh')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { EditOutlined, SendOutlined, ShoppingCartOutlined, StopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { closeRequisition, getRequisition, requisitionToOrder, submitRequisition } from '@/api/scm/sourcing'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'

const { t } = useI18n()

/** 请购单详情：提交审批、生成采购订单、关闭。触发 edit(record)、changed */
const props = defineProps({
  open: Boolean,
  reqId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])

const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(`scm:requisition:${perm}`)
const router = useRouter()

const itemColumns = [
  { title: t('scm.requisition.material'), key: 'material', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 120, align: 'right' },
  { title: t('scm.requisition.requiredDate'), dataIndex: 'requiredDate', width: 110 },
  { title: t('scm.requisition.suggestSupplier'), key: 'supplier', width: 180, ellipsis: true },
  { title: t('scm.requisition.purchaseOrder'), key: 'order', width: 170 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true }
]

const detail = ref()
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getRequisition(props.reqId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.reqId,
  (v) => {
    detail.value = undefined
    if (v) load()
  }
)

function toOrder(id) {
  router.push({ path: '/scm/purchase', query: { id } })
}

function confirmAction(title, content, api, success) {
  Modal.confirm({
    title,
    content,
    async onOk() {
      const res = await api(props.reqId)
      message.success(typeof success === 'function' ? success(res) : success)
      await load()
      emit('changed')
    }
  })
}

const orderOpen = ref(false)
const ordering = ref(false)
const warehouseId = ref()
async function submitOrder() {
  ordering.value = true
  try {
    const ids = await requisitionToOrder(props.reqId, warehouseId.value || undefined)
    message.success(t('scm.requisition.ordersCreated', { n: ids.length }))
    orderOpen.value = false
    await load()
    emit('changed')
  } finally {
    ordering.value = false
  }
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (d.status === 'DRAFT' && can('edit')) {
    list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (['DRAFT', 'APPROVED'].includes(d.status) && can('edit')) {
    list.push({
      key: 'close',
      label: t('scm.requisition.close'),
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction(t('scm.requisition.confirmClose'), t('scm.requisition.confirmCloseTip'), closeRequisition, t('scm.requisition.closed'))
    })
  }
  if (d.status === 'DRAFT' && can('edit')) {
    list.push({
      key: 'submit',
      label: t('common.action.submit'),
      icon: SendOutlined,
      primary: true,
      run: () =>
        confirmAction(t('scm.requisition.confirmSubmit'), t('scm.requisition.confirmSubmitTip'), submitRequisition, (flow) =>
          flow ? t('scm.requisition.submittedFlow') : t('scm.requisition.approved')
        )
    })
  }
  if (d.status === 'APPROVED' && can('order')) {
    list.push({
      key: 'order',
      label: t('scm.requisition.toOrder'),
      icon: ShoppingCartOutlined,
      primary: true,
      run: () => {
        warehouseId.value = undefined
        orderOpen.value = true
      }
    })
  }
  return list
})

defineExpose({ reload: load })
</script>

<style scoped>
.doc-desc {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
