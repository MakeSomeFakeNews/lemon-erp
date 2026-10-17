<template>
  <SubPage :open="open" :title="detail ? detail.name : $t('crm.customerDetail.title')" width="min(1000px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail">
        <DictTag type="crm_customer_stage" :value="detail.stage" />
        <a-button v-perm="'crm:followup:add'" size="small" @click="followOpen = true">
          <template #icon><MessageOutlined /></template>{{ $t('crm.common.writeFollowup') }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('crm.common.f.customerCode')">{{ detail.code }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.customerLevel')"><DictTag type="scm_customer_level" :value="detail.level" /></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.owner')">{{ detail.ownerName || $t('crm.common.publicPool') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.industry')"><DictTag type="crm_industry" :value="detail.industry" /><span v-if="!detail.industry">-</span></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.source')"><DictTag type="crm_source" :value="detail.source" /><span v-if="!detail.source">-</span></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.region')">{{ detail.region || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.contact')">{{ detail.contact || '-' }} {{ detail.phone || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.website')">{{ detail.website || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.address')">{{ detail.address || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.lastFollow')">{{ detail.lastFollowTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.nextFollow')">
            <span :class="{ danger: isPast(detail.nextFollowTime) }">{{ detail.nextFollowTime || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.creditLimit')">{{ detail.creditLimit != null ? '¥' + formatMoney(detail.creditLimit) : $t('crm.common.unlimited') }}</a-descriptions-item>
        </a-descriptions>

        <a-row v-if="summary" :gutter="[12, 12]" class="block">
          <a-col v-for="s in stats" :key="s.label" :xs="12" :sm="8" :lg="4">
            <div class="stat">
              <div class="stat-label">{{ s.label }}</div>
              <div class="stat-value">{{ s.value }}</div>
            </div>
          </a-col>
        </a-row>

        <a-tabs v-model:active-key="tab" size="small">
          <a-tab-pane key="contact" :tab="$t('crm.customerDetail.tab.contact')">
            <ContactPanel :customer-id="detail.customerId" :editable="canEdit" @changed="loadDetail" />
          </a-tab-pane>
          <a-tab-pane key="followup" :tab="$t('crm.customerDetail.tab.followup')">
            <FollowupTimeline ref="timelineRef" :customer-id="detail.customerId" show-target @saved="reload" />
          </a-tab-pane>
          <a-tab-pane key="opp" :tab="$t('crm.customerDetail.tab.opp')">
            <a-table row-key="id" size="small" :columns="oppColumns" :data-source="opps" :pagination="false" :scroll="{ x: 760 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <div>{{ record.name }}</div>
                  <div class="sub-text">{{ record.oppNo }}</div>
                </template>
                <template v-else-if="column.key === 'stage'"><DictTag type="crm_opp_stage" :value="record.stage" /></template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="ticket" :tab="$t('crm.customerDetail.tab.ticket')">
            <a-table row-key="id" size="small" :columns="ticketColumns" :data-source="tickets" :pagination="false" :scroll="{ x: 760 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'title'">
                  <div>{{ record.title }}</div>
                  <div class="sub-text">{{ record.ticketNo }}</div>
                </template>
                <template v-else-if="column.key === 'type'"><DictTag type="crm_ticket_type" :value="record.type" /></template>
                <template v-else-if="column.key === 'status'"><DictTag type="crm_ticket_status" :value="record.status" /></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-for="x in tradeTabs" :key="x.key" :tab="x.label">
            <CustomerTrade v-if="tab === x.key" :customer-id="customerId" :kind="x.key" />
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('crm.customerDetail.tab.attachment')">
            <AttachmentPanel biz-type="crm_customer" :biz-id="customerId" :editable="userStore.hasPerm('crm:customer:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>
    <FollowupModal v-model:open="followOpen" target-type="CUSTOMER" :target-id="customerId" @success="reload" />
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { MessageOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import ContactPanel from './ContactPanel.vue'
import FollowupModal from './FollowupModal.vue'
import FollowupTimeline from './FollowupTimeline.vue'
import CustomerTrade from './CustomerTrade.vue'
import { crmCustomerSummary, getCrmCustomer, pageOpportunity, pageTicket } from '@/api/crm'
import { customerCredit } from '@/api/scm/partner'
import { formatMoney } from '@/components/scm/scm'
import { useUserStore } from '@/store/user'
import { isPast } from './crm'

/** 客户详情（客户 360）：经营信息、信用占用、近一年交易概况、联系人、跟进、商机、服务工单、订单、发运、应收、收款、项目 */
const props = defineProps({
  open: Boolean,
  customerId: Number
})
const emit = defineEmits(['update:open', 'changed'])
const userStore = useUserStore()
const { t } = useI18n()

const detail = ref(null)
const summary = ref(null)
const opps = ref([])
const tickets = ref([])
const loading = ref(false)
const tab = ref('contact')
const followOpen = ref(false)
const timelineRef = ref()

const canEdit = computed(() => !!detail.value && (detail.value.ownerId === userStore.user?.id || userStore.hasPerm('crm:customer:edit')))

/** 客户 360：各模块的单据，按当前用户的权限显示 */
const tradeTabs = computed(() =>
  [
    { key: 'contracts', label: t('crm.customerDetail.tab.contracts'), perm: 'crm:contract:list' },
    { key: 'orders', label: t('crm.customerDetail.tab.orders'), perm: 'scm:sales:list' },
    { key: 'shipments', label: t('crm.customerDetail.tab.shipments'), perm: 'scm:shipment:list' },
    { key: 'bills', label: t('crm.customerDetail.tab.bills'), perm: 'fin:ar:list' },
    { key: 'receipts', label: t('crm.customerDetail.tab.receipts'), perm: 'fin:receipt:list' },
    { key: 'projects', label: t('crm.customerDetail.tab.projects'), perm: 'pm:project:list' }
  ].filter((x) => userStore.hasPerm(x.perm))
)
const credit = ref(null)

const stats = computed(() => {
  const s = summary.value
  const c = credit.value
  // 可用额度 = 信用额度 - 应收余额 - 未完成订单
  const available = c && c.creditLimit != null ? Number(c.creditLimit) - Number(c.receivable || 0) - Number(c.openOrders || 0) : null
  const creditItem = c ? [{ label: t('crm.customerDetail.stat.credit'), value: c.creditLimit == null ? t('crm.common.unlimited') : `¥${formatMoney(available)}` }] : []
  return [
    ...creditItem,
    { label: t('crm.customerDetail.stat.sales'), value: `¥${formatMoney(s.salesAmount)}` },
    { label: t('crm.customerDetail.stat.orders'), value: t('crm.customerDetail.stat.ordersValue', { n: s.orderCount }) },
    { label: t('crm.customerDetail.stat.lastOrder'), value: s.lastOrderDate || '-' },
    { label: t('crm.customerDetail.stat.ar'), value: `¥${formatMoney(s.arBalance)}` },
    { label: t('crm.customerDetail.stat.won'), value: t('crm.customerDetail.stat.wonValue', { n: s.wonCount, amount: formatMoney(s.wonAmount) }) },
    { label: t('crm.customerDetail.stat.openTickets'), value: t('crm.customerDetail.stat.ordersValue', { n: s.openTickets }) }
  ]
})

const oppColumns = [
  { title: t('crm.common.f.opportunity'), key: 'name', width: 220 },
  { title: t('crm.common.f.stage'), key: 'stage', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('crm.common.f.expectedDate'), dataIndex: 'expectedDate', width: 120 },
  { title: t('crm.common.f.owner'), dataIndex: 'ownerName', width: 110 }
]
const ticketColumns = [
  { title: t('crm.common.f.ticket'), key: 'title', width: 240 },
  { title: t('common.field.type'), key: 'type', width: 90 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('crm.common.f.assignee'), dataIndex: 'assigneeName', width: 110 },
  { title: t('crm.common.f.acceptTime'), dataIndex: 'createTime', width: 160 }
]

async function loadDetail() {
  const [d, s, c] = await Promise.all([
    getCrmCustomer(props.customerId),
    crmCustomerSummary(props.customerId),
    customerCredit(props.customerId).catch(() => null)
  ])
  credit.value = c
  detail.value = d
  summary.value = s
}

async function load() {
  loading.value = true
  try {
    await loadDetail()
    const [o, t] = await Promise.all([
      pageOpportunity({ current: 1, size: 100, customerId: props.customerId }),
      pageTicket({ current: 1, size: 100, customerId: props.customerId })
    ])
    opps.value = o.records
    tickets.value = t.records
  } finally {
    loading.value = false
  }
}

function reload() {
  loadDetail()
  timelineRef.value?.load()
  emit('changed')
}

watch(
  () => props.open,
  (open) => {
    if (open && props.customerId) {
      tab.value = 'contact'
      detail.value = null
      load()
    }
  }
)
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.stat {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-hover);
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  margin-top: 4px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.danger {
  color: #f5222d;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
