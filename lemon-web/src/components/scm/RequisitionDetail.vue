<template>
  <SubPage
    :open="open"
    :title="detail ? `请购单 ${detail.reqNo}` : '请购单'"
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
          <a-descriptions-item label="请购单号">{{ detail.reqNo }}</a-descriptions-item>
          <a-descriptions-item label="请购日期">{{ detail.reqDate }}</a-descriptions-item>
          <a-descriptions-item label="请购部门">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.approveTime" label="批准时间">{{ detail.approveTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="说明">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          message="请购单审批中，可在「流程中心 → 我的申请」查看审批进度，审批通过后自动变为已批准"
        />
        <a-alert
          v-else-if="detail.status === 'APPROVED'"
          type="info"
          show-icon
          class="doc-desc"
          message="生成采购订单时按建议供应商拆分，未指定建议供应商的明细使用物料的默认供应商"
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

    <a-modal v-model:open="orderOpen" title="生成采购订单" :confirm-loading="ordering" @ok="submitOrder">
      <p>按供应商拆分生成草稿采购订单，单价按供货价格带出，生成后请到采购订单审核。</p>
      <a-form layout="vertical">
        <a-form-item label="收货仓库">
          <WarehouseSelect v-model:value="warehouseId" placeholder="选填，入库时默认" />
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
import { EditOutlined, SendOutlined, ShoppingCartOutlined, StopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { closeRequisition, getRequisition, requisitionToOrder, submitRequisition } from '@/api/scm/sourcing'
import { useUserStore } from '@/store/user'
import { formatQty } from '@/components/wms/wms'

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
  { title: '物料', key: 'material', width: 240 },
  { title: '数量', key: 'qty', width: 120, align: 'right' },
  { title: '需求日期', dataIndex: 'requiredDate', width: 110 },
  { title: '建议供应商', key: 'supplier', width: 180, ellipsis: true },
  { title: '采购订单', key: 'order', width: 170 },
  { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true }
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
    message.success(`已生成 ${ids.length} 张采购订单`)
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
    list.push({ key: 'edit', label: '编辑', icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (['DRAFT', 'APPROVED'].includes(d.status) && can('edit')) {
    list.push({
      key: 'close',
      label: '关闭',
      icon: StopOutlined,
      danger: true,
      run: () => confirmAction('关闭请购单？', '关闭后不能再生成采购订单。', closeRequisition, '已关闭')
    })
  }
  if (d.status === 'DRAFT' && can('edit')) {
    list.push({
      key: 'submit',
      label: '提交',
      icon: SendOutlined,
      primary: true,
      run: () =>
        confirmAction('提交请购单？', '发布了审批流程时将发起审批，否则直接批准。', submitRequisition, (flow) =>
          flow ? '已提交审批' : '已批准'
        )
    })
  }
  if (d.status === 'APPROVED' && can('order')) {
    list.push({
      key: 'order',
      label: '生成采购订单',
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
