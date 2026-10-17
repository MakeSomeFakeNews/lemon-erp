<template>
  <SubPage
    :open="open"
    :title="detail ? $t('scm.quotation.titleNo', { no: detail.quoteNo }) : $t('scm.quotation.title')"
    width="min(1100px, 100vw)"
    destroy-on-close
    @close="emit('update:open', false)"
  >
    <template #extra>
      <a-space v-if="detail">
        <a-tag v-if="expired" color="red" :bordered="false">{{ $t('scm.quotation.expired') }}</a-tag>
        <DictTag type="scm_quotation_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="doc-desc">
          <a-descriptions-item :label="$t('scm.quotation.quoteNo')">{{ detail.quoteNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.quotation.customer')">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.quotation.quoteDate')">{{ detail.quoteDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.quotation.validTo')">{{ detail.validTo || $t('scm.quotation.unlimited') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.quotation.amount')">
            <b>¥{{ formatMoney(detail.totalAmount) }}</b>
            <span class="sub-text">{{ $t('scm.quotation.taxAmountParen', { amount: formatMoney(detail.taxAmount) }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.opportunityId" :label="$t('scm.quotation.opportunity')">#{{ detail.opportunityId }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.orderId" :label="$t('scm.quotation.salesOrder')">
            <a @click="toOrder(detail.orderId)">{{ detail.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.quotation.creator')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.lostReason" :label="$t('scm.quotation.lostReason')">{{ detail.lostReason }}</a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="detail.status === 'APPROVING'"
          type="info"
          show-icon
          class="doc-desc"
          :message="$t('scm.quotation.approvingTip')"
        />
        <a-alert
          v-else-if="detail.status === 'APPROVED' && expired"
          type="warning"
          show-icon
          class="doc-desc"
          :message="$t('scm.quotation.expiredTip')"
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
              <div class="sub-text">{{ $t('scm.quotation.taxRateVal', { n: Number(record.taxRate) }) }}</div>
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

    <a-modal v-model:open="orderOpen" :title="$t('scm.quotation.toOrder')" :confirm-loading="saving" @ok="submitOrder">
      <p>{{ $t('scm.quotation.toOrderTip') }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('scm.quotation.shipWarehouse')">
          <WarehouseSelect v-model:value="orderForm.warehouseId" :placeholder="$t('scm.quotation.shipWarehousePh')" />
        </a-form-item>
        <a-form-item :label="$t('scm.quotation.deliveryDate')">
          <a-date-picker v-model:value="orderForm.deliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.quotation.optional')" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="loseOpen" :title="$t('scm.quotation.markLost')" :confirm-loading="saving" @ok="submitLose">
      <p>{{ $t('scm.quotation.markLostTip') }}</p>
      <a-textarea v-model:value="loseReason" :rows="3" :maxlength="255" :placeholder="$t('scm.quotation.lostReasonPh')" />
    </a-modal>
  </SubPage>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

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
  { title: t('scm.quotation.material'), key: 'material', width: 240 },
  { title: t('common.field.quantity'), key: 'qty', width: 120, align: 'right' },
  { title: t('scm.quotation.taxPrice'), key: 'price', width: 170, align: 'right' },
  { title: t('scm.quotation.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true }
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
    await afterChange(t('scm.quotation.orderCreated'))
    if (userStore.hasPerm('scm:sales:list')) {
      Modal.confirm({
        title: t('scm.quotation.orderCreated'),
        content: t('scm.quotation.orderCreatedTip', { no: detail.value?.orderNo || '' }),
        okText: t('scm.quotation.goView'),
        cancelText: t('scm.quotation.stay'),
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
    await afterChange(t('scm.quotation.markedLost'))
  } finally {
    saving.value = false
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
      key: 'lose',
      label: t('scm.quotation.markLost'),
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
      label: t('common.action.submit'),
      icon: SendOutlined,
      primary: true,
      run: () =>
        Modal.confirm({
          title: t('scm.quotation.confirmSubmit'),
          content: t('scm.quotation.confirmSubmitTip'),
          async onOk() {
            const flow = await submitQuotation(props.quotationId)
            await afterChange(flow ? t('scm.quotation.submittedFlow') : t('scm.quotation.approved'))
          }
        })
    })
  }
  if (d.status === 'APPROVED' && !expired.value && can('order')) {
    list.push({
      key: 'order',
      label: t('scm.quotation.toOrder'),
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
