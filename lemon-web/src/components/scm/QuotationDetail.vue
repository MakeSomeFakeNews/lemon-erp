<template>
  <SubPage
    :open="open"
    :title="detail ? `销售报价 ${detail.quoteNo}` : '销售报价'"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-space v-if="detail">
        <a-tag v-if="expired" color="red" :bordered="false">已过期</a-tag>
        <DictTag type="scm_quotation_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item label="报价单号">{{ detail.quoteNo }}</a-descriptions-item>
          <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="报价日期">{{ detail.quoteDate }}</a-descriptions-item>
          <a-descriptions-item label="有效期至">{{ detail.validTo || '不限' }}</a-descriptions-item>
          <a-descriptions-item label="价税合计">
            <b>¥{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">（税额 {{ formatMoney(detail.taxAmount) }}）</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.opportunityId" label="关联商机">#{{ detail.opportunityId }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.orderId" label="销售订单">
            <a @click="toOrder(detail.orderId)">{{ detail.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.lostReason" label="失效原因">{{ detail.lostReason }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          message="报价单审批中，可在「流程中心 → 我的申请」查看审批进度，审批通过后自动变为已批准"
        />
        <a-alert
          v-else-if="detail.status === 'APPROVED' && expired"
          type="warning"
          show-icon
          class="doc-desc"
          message="报价已过有效期，不能转为销售订单，请重新报价"
        />

        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 850 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <div>{{ record.materialCode }} {{ record.materialName }}</div>
              <div class="sub-text">{{ record.spec }}</div>
            </template>
            <template v-else-if="column.key === 'qty'">{{ formatQty(record.qty) }} {{ record.unit }}</template>
            <template v-else-if="column.key === 'price'">
              <div>¥{{ formatMoney(record.price) }}</div>
              <div class="sub-text">税率 {{ Number(record.taxRate) }}%</div>
              <PriceHint :price="record.price" :list-price="record.listPrice" />
            </template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
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

    <a-modal v-model:open="orderOpen" title="转销售订单" :confirm-loading="saving" @ok="submitOrder">
      <p>按报价明细生成草稿销售订单，生成后请到销售订单审核。</p>
      <a-form layout="vertical">
        <a-form-item label="发货仓库">
          <WarehouseSelect v-model:value="orderForm.warehouseId" placeholder="选填，出库时默认" />
        </a-form-item>
        <a-form-item label="交货日期">
          <a-date-picker v-model:value="orderForm.deliveryDate" value-format="YYYY-MM-DD" placeholder="选填" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="loseOpen" title="标记失效" :confirm-loading="saving" @ok="submitLose">
      <p>报价未成交或已作废，标记后不能再转销售订单。</p>
      <a-textarea v-model:value="loseReason" :rows="3" :maxlength="255" placeholder="失效原因，如：客户选择了其他供应商、价格未达成一致" />
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { CloseCircleOutlined, EditOutlined, SendOutlined, ShoppingOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PriceHint from './PriceHint.vue'
import { getQuotation, loseQuotation, quotationToOrder, submitQuotation } from '@/api/scm/sourcing'
import { useUserStore } from '@/store/user'
import { formatQty, today } from '@/components/wms/wms'
import { formatMoney } from './scm'

/** 销售报价详情：提交审批、转销售订单、标记失效。触发 edit(record)、changed */
const props = defineProps({
  open: Boolean,
  quotationId: Number
})
const emit = defineEmits(['update:open', 'edit', 'changed'])

const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(`scm:quotation:${perm}`)
const router = useRouter()

const itemColumns = [
  { title: '物料', key: 'material', width: 240 },
  { title: '数量', key: 'qty', width: 120, align: 'right' },
  { title: '含税单价', key: 'price', width: 170, align: 'right' },
  { title: '价税合计', key: 'amount', width: 130, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true }
]

const detail = ref()
const loading = ref(false)
const expired = computed(() => {
  const d = detail.value
  return !!d?.validTo && d.validTo < today() && ['DRAFT', 'APPROVING', 'APPROVED'].includes(d.status)
})

async function load() {
  loading.value = true
  try {
    detail.value = await getQuotation(props.quotationId)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open && props.quotationId,
  (v) => {
    detail.value = undefined
    if (v) load()
  }
)

async function afterChange(msg) {
  message.success(msg)
  await load()
  emit('changed')
}

function toOrder(id) {
  router.push({ path: '/scm/sales', query: { id } })
}

const saving = ref(false)
const orderOpen = ref(false)
const orderForm = reactive({ warehouseId: undefined, deliveryDate: undefined })
async function submitOrder() {
  saving.value = true
  try {
    const orderId = await quotationToOrder(props.quotationId, {
      warehouseId: orderForm.warehouseId || undefined,
      deliveryDate: orderForm.deliveryDate || undefined
    })
    orderOpen.value = false
    await afterChange('已生成销售订单')
    if (userStore.hasPerm('scm:sales:list')) {
      Modal.confirm({
        title: '已生成销售订单',
        content: `销售订单 ${detail.value?.orderNo || ''} 为草稿状态，是否前往查看并审核？`,
        okText: '前往查看',
        cancelText: '留在此页',
        onOk: () => toOrder(orderId)
      })
    }
  } finally {
    saving.value = false
  }
}

const loseOpen = ref(false)
const loseReason = ref('')
async function submitLose() {
  saving.value = true
  try {
    await loseQuotation(props.quotationId, loseReason.value || undefined)
    loseOpen.value = false
    await afterChange('已标记失效')
  } finally {
    saving.value = false
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
      key: 'lose',
      label: '标记失效',
      icon: CloseCircleOutlined,
      danger: true,
      run: () => {
        loseReason.value = ''
        loseOpen.value = true
      }
    })
  }
  if (d.status === 'DRAFT' && can('edit')) {
    list.push({
      key: 'submit',
      label: '提交',
      icon: SendOutlined,
      primary: true,
      run: () =>
        Modal.confirm({
          title: '提交报价单？',
          content: '发布了审批流程时将发起审批，否则直接批准。',
          async onOk() {
            const flow = await submitQuotation(props.quotationId)
            await afterChange(flow ? '已提交审批' : '已批准')
          }
        })
    })
  }
  if (d.status === 'APPROVED' && !expired.value && can('order')) {
    list.push({
      key: 'order',
      label: '转销售订单',
      icon: ShoppingOutlined,
      primary: true,
      run: () => {
        Object.assign(orderForm, { warehouseId: undefined, deliveryDate: undefined })
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
.full {
  width: 100%;
}
</style>
