<template>
  <SubPage :open="open" :title="detail ? `${detail.contractNo} ${detail.name}` : $t('crm.contractDetail.title')" width="min(960px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail" wrap>
        <DictTag type="crm_contract_status" :value="detail.status" />
        <template v-if="detail.status === 'DRAFT' && userStore.hasPerm('crm:contract:edit')">
          <a-button size="small" @click="emit('edit', detail)">{{ $t('common.action.edit') }}</a-button>
          <a-popconfirm :title="$t('crm.contractDetail.confirmSubmit')" @confirm="submit">
            <a-button size="small" type="primary">{{ $t('common.action.submit') }}</a-button>
          </a-popconfirm>
        </template>
        <template v-if="detail.status === 'ACTIVE' && userStore.hasPerm('crm:contract:edit')">
          <template v-if="!detail.orderId">
            <a-button v-if="detail.quotationId && userStore.hasPerm('scm:sales:add')" size="small" type="primary" @click="openToOrder">{{ $t('crm.common.toOrder') }}</a-button>
            <a-button size="small" @click="openLink">{{ $t('crm.contractDetail.link') }}</a-button>
          </template>
          <a-popconfirm :title="$t('crm.contractDetail.confirmComplete')" @confirm="complete">
            <a-button size="small">{{ $t('crm.contractDetail.complete') }}</a-button>
          </a-popconfirm>
          <a-button size="small" danger @click="openTerminate">{{ $t('crm.contractDetail.terminate') }}</a-button>
        </template>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.status === 'APPROVING'" type="info" show-icon class="block" :message="$t('crm.contractDetail.approving')" />
        <a-alert v-if="detail.status === 'TERMINATED'" type="error" show-icon class="block" :message="$t('crm.contractDetail.terminateReasonText', { reason: detail.terminateReason || '-' })" />

        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('crm.common.f.customer')">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.contractAmount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.owner')">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.signDate')">{{ detail.signDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.contractDetail.term')">{{ detail.startDate || '-' }} ~ {{ detail.endDate || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.opportunity')">{{ detail.opportunityName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.quotation')">{{ detail.quoteNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.salesOrder')">
            <router-link v-if="detail.orderId" :to="{ path: '/scm/sales', query: { id: detail.orderId } }">{{ detail.orderNo }}</router-link>
            <span v-else class="sub-text">{{ $t('crm.contractDetail.notLinked') }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('crm.contractDetail.terms')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="progress-head">
          <span class="section-title">{{ $t('crm.contractDetail.progress') }}</span>
          <span>{{ $t('crm.contractDetail.received') }} <b>¥{{ formatMoney(detail.receivedAmount || 0) }}</b> / ¥{{ formatMoney(detail.amount) }}</span>
        </div>
        <a-progress :percent="percent" :status="percent >= 100 ? 'success' : 'active'" />
        <div class="sub-text block">{{ $t('crm.contractDetail.progressTip') }}</div>

        <a-table row-key="id" size="small" :columns="planColumns" :data-source="detail.plans || []" :pagination="false" :scroll="{ x: 640 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'seq'">{{ $t('crm.contractDetail.seqText', { seq: record.seq }) }}</template>
            <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'receivedAmount'">¥{{ formatMoney(record.receivedAmount || 0) }}</template>
            <template v-else-if="column.key === 'collectStatus'">
              <a-tag :color="COLLECT_STATUS[record.collectStatus]?.color" :bordered="false">
                {{ COLLECT_STATUS[record.collectStatus]?.label || record.collectStatus || '-' }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <div v-if="!detail.plans?.length" class="sub-text empty">{{ $t('crm.contractDetail.noPlans') }}</div>
      </template>
    </a-spin>

    <a-modal v-model:open="linkOpen" :title="$t('crm.contractDetail.link')" :confirm-loading="saving" destroy-on-close @ok="submitLink">
      <a-alert type="info" show-icon class="block" :message="$t('crm.contractDetail.linkTip')" />
      <a-select
        v-model:value="linkOrderId"
        show-search
        option-filter-prop="label"
        :options="orderOptions"
        :loading="orderLoading"
        :placeholder="$t('crm.contractDetail.selectOrder')"
        :not-found-content="orderLoading ? $t('crm.common.loading') : $t('crm.contractDetail.noOrders')"
        class="full"
      />
    </a-modal>

    <a-modal v-model:open="toOrderOpen" :title="$t('crm.common.toOrder')" :confirm-loading="saving" destroy-on-close @ok="submitToOrder">
      <a-alert type="info" show-icon class="block" :message="$t('crm.contractDetail.toOrderTip', { no: detail?.quoteNo || '' })" />
      <a-form layout="vertical">
        <a-form-item :label="$t('crm.common.f.warehouse')"><WarehouseSelect v-model:value="toOrderWarehouseId" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="terminateOpen" :title="$t('crm.contractDetail.terminateTitle')" :confirm-loading="saving" destroy-on-close @ok="submitTerminate">
      <a-textarea v-model:value="terminateReason" :rows="3" :maxlength="255" :placeholder="$t('crm.contractDetail.terminateReason')" />
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

const detail = ref(null)
const loading = ref(false)
const saving = ref(false)

const percent = computed(() => {
  const amount = Number(detail.value?.amount) || 0
  return amount ? Math.min(Math.round(((Number(detail.value.receivedAmount) || 0) / amount) * 1000) / 10, 100) : 0
})

const planColumns = [
  { title: t('crm.contractDetail.col.seq'), key: 'seq', width: 100 },
  { title: t('crm.common.f.planDate'), dataIndex: 'planDate', width: 110 },
  { title: t('crm.contractDetail.col.planAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('crm.common.f.received'), key: 'receivedAmount', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'collectStatus', width: 140 },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true }
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
  message.success(submitted ? t('crm.contractDetail.submittedApproval') : t('crm.contractDetail.activated'))
  changed()
}
async function complete() {
  await completeCrmContract(detail.value.id)
  message.success(t('crm.contractDetail.completed'))
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
    message.warning(t('crm.contractDetail.orderRequired'))
    return
  }
  saving.value = true
  try {
    await linkContractOrder(detail.value.id, linkOrderId.value)
    message.success(t('crm.contractDetail.linked'))
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
      content: t('crm.contractDetail.orderCreated'),
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
    message.warning(t('crm.contractDetail.reasonRequired'))
    return
  }
  saving.value = true
  try {
    await terminateCrmContract(detail.value.id, terminateReason.value.trim())
    message.success(t('crm.contractDetail.terminated'))
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
