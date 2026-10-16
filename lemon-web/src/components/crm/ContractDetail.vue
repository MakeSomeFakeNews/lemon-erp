<template>
  <SubPage :open="open" :title="detail ? `${detail.contractNo} ${detail.name}` : '合同详情'" width="min(960px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail" wrap>
        <DictTag type="crm_contract_status" :value="detail.status" />
        <template v-if="detail.status === 'DRAFT' && userStore.hasPerm('crm:contract:edit')">
          <a-button size="small" @click="emit('edit', detail)">编辑</a-button>
          <a-popconfirm title="提交后合同不能再修改，确定提交？" @confirm="submit">
            <a-button size="small" type="primary">提交</a-button>
          </a-popconfirm>
        </template>
        <template v-if="detail.status === 'ACTIVE' && userStore.hasPerm('crm:contract:edit')">
          <template v-if="!detail.orderId">
            <a-button v-if="detail.quotationId && userStore.hasPerm('scm:sales:add')" size="small" type="primary" @click="openToOrder">生成销售订单</a-button>
            <a-button size="small" @click="openLink">关联销售订单</a-button>
          </template>
          <a-popconfirm title="确定合同已履行完成？" @confirm="complete">
            <a-button size="small">完成</a-button>
          </a-popconfirm>
          <a-button size="small" danger @click="openTerminate">终止</a-button>
        </template>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.status === 'APPROVING'" type="info" show-icon class="block" message="合同审批中，审批通过后生效（执行中）" />
        <a-alert v-if="detail.status === 'TERMINATED'" type="error" show-icon class="block" :message="`终止原因：${detail.terminateReason || '-'}`" />

        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="合同金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="签订日期">{{ detail.signDate }}</a-descriptions-item>
          <a-descriptions-item label="合同期限">{{ detail.startDate || '-' }} ~ {{ detail.endDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="商机">{{ detail.opportunityName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="报价单">{{ detail.quoteNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="销售订单">
            <router-link v-if="detail.orderId" :to="{ path: '/scm/sales', query: { id: detail.orderId } }">{{ detail.orderNo }}</router-link>
            <span v-else class="sub-text">未关联</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="主要条款 / 备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="progress-head">
          <span class="section-title">回款进度</span>
          <span>已回款 <b>¥{{ formatMoney(detail.receivedAmount || 0) }}</b> / ¥{{ formatMoney(detail.amount) }}</span>
        </div>
        <a-progress :percent="percent" :status="percent >= 100 ? 'success' : 'active'" />
        <div class="sub-text block">回款按关联销售订单出库形成的应收单的核销金额计算，按计划日期先后分摊到各期回款计划。</div>

        <a-table row-key="id" size="small" :columns="planColumns" :data-source="detail.plans || []" :pagination="false" :scroll="{ x: 640 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'seq'">第 {{ record.seq }} 期</template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'receivedAmount'">¥{{ formatMoney(record.receivedAmount || 0) }}</template>
            <template v-else-if="column.key === 'collectStatus'">
              <a-tag :color="COLLECT_STATUS[record.collectStatus]?.color" :bordered="false">
                {{ COLLECT_STATUS[record.collectStatus]?.label || record.collectStatus || '-' }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <div v-if="!detail.plans?.length" class="sub-text empty">没有回款计划</div>
      </template>
    </a-spin>

    <a-modal v-model:open="linkOpen" title="关联销售订单" :confirm-loading="saving" destroy-on-close @ok="submitLink">
      <a-alert type="info" show-icon class="block" message="选择该客户已审核的销售订单；关联后按订单出库形成的应收单计算回款" />
      <a-select
        v-model:value="linkOrderId"
        show-search
        option-filter-prop="label"
        :options="orderOptions"
        :loading="orderLoading"
        placeholder="选择销售订单"
        :not-found-content="orderLoading ? '加载中…' : '该客户没有已审核的销售订单'"
        class="full"
      />
    </a-modal>

    <a-modal v-model:open="toOrderOpen" title="生成销售订单" :confirm-loading="saving" destroy-on-close @ok="submitToOrder">
      <a-alert type="info" show-icon class="block" :message="`按关联的报价单 ${detail?.quoteNo || ''} 生成销售订单草稿，并与合同关联`" />
      <a-form layout="vertical">
        <a-form-item label="发货仓库"><WarehouseSelect v-model:value="toOrderWarehouseId" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="terminateOpen" title="终止合同" :confirm-loading="saving" destroy-on-close @ok="submitTerminate">
      <a-textarea v-model:value="terminateReason" :rows="3" :maxlength="255" placeholder="终止原因" />
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import { completeCrmContract, contractToOrder, getCrmContract, linkContractOrder, submitCrmContract, terminateCrmContract } from '@/api/crm'
import { pageOrder } from '@/api/scm/order'
import { useUserStore } from '@/store/user'
import { COLLECT_STATUS } from './crm'

/** 销售合同详情：回款进度、回款计划；提交、关联 / 生成销售订单、完成、终止 */
const props = defineProps({
  open: Boolean,
  contractId: Number
})
const emit = defineEmits(['update:open', 'changed', 'edit'])
const userStore = useUserStore()
const router = useRouter()

const detail = ref(null)
const loading = ref(false)
const saving = ref(false)

const percent = computed(() => {
  const amount = Number(detail.value?.amount) || 0
  return amount ? Math.min(Math.round(((Number(detail.value.receivedAmount) || 0) / amount) * 1000) / 10, 100) : 0
})

const planColumns = [
  { title: '期次', key: 'seq', width: 80 },
  { title: '计划日期', dataIndex: 'planDate', width: 110 },
  { title: '计划金额', key: 'amount', width: 130, align: 'right' },
  { title: '已回款', key: 'receivedAmount', width: 130, align: 'right' },
  { title: '状态', key: 'collectStatus', width: 100 },
  { title: '说明', dataIndex: 'remark', ellipsis: true }
]

async function load() {
  loading.value = true
  try {
    detail.value = await getCrmContract(props.contractId)
  } finally {
    loading.value = false
  }
}
watch(
  () => props.open,
  (open) => {
    if (open && props.contractId) {
      detail.value = null
      load()
    }
  }
)
function changed() {
  load()
  emit('changed')
}

async function submit() {
  const submitted = await submitCrmContract(detail.value.id)
  message.success(submitted ? '已提交审批，审批通过后合同生效' : '合同已生效')
  changed()
}
async function complete() {
  await completeCrmContract(detail.value.id)
  message.success('合同已完成')
  changed()
}

// 关联销售订单：同一客户已审核的订单
const linkOpen = ref(false)
const linkOrderId = ref()
const orders = ref([])
const orderLoading = ref(false)
const orderOptions = computed(() =>
  orders.value.map((o) => ({ value: o.id, label: `${o.orderNo}　${o.orderDate || ''}　¥${formatMoney(o.totalAmount)}` }))
)
async function openLink() {
  linkOrderId.value = undefined
  linkOpen.value = true
  orderLoading.value = true
  try {
    const res = await pageOrder('sales', { partnerId: detail.value.customerId, current: 1, size: 100 })
    orders.value = res.records.filter((o) => ['APPROVED', 'PARTIAL', 'COMPLETED'].includes(o.status))
  } finally {
    orderLoading.value = false
  }
}
async function submitLink() {
  if (!linkOrderId.value) {
    message.warning('请选择销售订单')
    return
  }
  saving.value = true
  try {
    await linkContractOrder(detail.value.id, linkOrderId.value)
    message.success('已关联销售订单')
    linkOpen.value = false
    changed()
  } finally {
    saving.value = false
  }
}

// 由报价单生成销售订单
const toOrderOpen = ref(false)
const toOrderWarehouseId = ref()
function openToOrder() {
  toOrderWarehouseId.value = undefined
  toOrderOpen.value = true
}
async function submitToOrder() {
  saving.value = true
  try {
    const orderId = await contractToOrder(detail.value.id, toOrderWarehouseId.value || undefined)
    toOrderOpen.value = false
    changed()
    message.success({
      content: '已生成销售订单草稿，点击查看',
      onClick: () => router.push({ path: '/scm/sales', query: { id: orderId } })
    })
  } finally {
    saving.value = false
  }
}

// 终止
const terminateOpen = ref(false)
const terminateReason = ref('')
function openTerminate() {
  terminateReason.value = ''
  terminateOpen.value = true
}
async function submitTerminate() {
  if (!terminateReason.value.trim()) {
    message.warning('请填写终止原因')
    return
  }
  saving.value = true
  try {
    await terminateCrmContract(detail.value.id, terminateReason.value.trim())
    message.success('合同已终止')
    terminateOpen.value = false
    changed()
  } finally {
    saving.value = false
  }
}

defineExpose({ reload: load })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.empty {
  padding: 12px 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.section-title {
  font-weight: 600;
}
.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
