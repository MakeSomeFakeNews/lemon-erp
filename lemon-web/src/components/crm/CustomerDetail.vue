<template>
  <SubPage :open="open" :title="detail ? detail.name : '客户详情'" width="min(1000px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail">
        <DictTag type="crm_customer_stage" :value="detail.stage" />
        <a-button v-perm="'crm:followup:add'" size="small" @click="followOpen = true">
          <template #icon><MessageOutlined /></template>写跟进
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item label="客户编码">{{ detail.code }}</a-descriptions-item>
          <a-descriptions-item label="客户等级"><DictTag type="scm_customer_level" :value="detail.level" /></a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detail.ownerName || '公海' }}</a-descriptions-item>
          <a-descriptions-item label="行业"><DictTag type="crm_industry" :value="detail.industry" /><span v-if="!detail.industry">-</span></a-descriptions-item>
          <a-descriptions-item label="来源"><DictTag type="crm_source" :value="detail.source" /><span v-if="!detail.source">-</span></a-descriptions-item>
          <a-descriptions-item label="地区">{{ detail.region || '-' }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ detail.contact || '-' }} {{ detail.phone || '' }}</a-descriptions-item>
          <a-descriptions-item label="网站">{{ detail.website || '-' }}</a-descriptions-item>
          <a-descriptions-item label="地址">{{ detail.address || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最近跟进">{{ detail.lastFollowTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="下次跟进">
            <span :class="{ danger: isPast(detail.nextFollowTime) }">{{ detail.nextFollowTime || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="信用额度">{{ detail.creditLimit != null ? '¥' + formatMoney(detail.creditLimit) : '不限' }}</a-descriptions-item>
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
          <a-tab-pane key="contact" tab="联系人">
            <ContactPanel :customer-id="detail.customerId" :editable="canEdit" @changed="loadDetail" />
          </a-tab-pane>
          <a-tab-pane key="followup" tab="跟进记录">
            <FollowupTimeline ref="timelineRef" :customer-id="detail.customerId" show-target @saved="reload" />
          </a-tab-pane>
          <a-tab-pane key="opp" tab="商机">
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
          <a-tab-pane key="ticket" tab="服务工单">
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
          <a-tab-pane v-for="t in tradeTabs" :key="t.key" :tab="t.label">
            <CustomerTrade v-if="tab === t.key" :customer-id="customerId" :kind="t.key" />
          </a-tab-pane>
          <a-tab-pane key="attachment" tab="附件">
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
    { key: 'contracts', label: '合同', perm: 'crm:contract:list' },
    { key: 'orders', label: '销售订单', perm: 'scm:sales:list' },
    { key: 'shipments', label: '发运', perm: 'scm:shipment:list' },
    { key: 'bills', label: '应收', perm: 'fin:ar:list' },
    { key: 'receipts', label: '收款', perm: 'fin:receipt:list' },
    { key: 'projects', label: '项目', perm: 'pm:project:list' }
  ].filter((t) => userStore.hasPerm(t.perm))
)
const credit = ref(null)

const stats = computed(() => {
  const s = summary.value
  const c = credit.value
  // 可用额度 = 信用额度 - 应收余额 - 未完成订单
  const available = c && c.creditLimit != null ? Number(c.creditLimit) - Number(c.receivable || 0) - Number(c.openOrders || 0) : null
  const creditItem = c ? [{ label: '可用信用额度', value: c.creditLimit == null ? '不限' : `¥${formatMoney(available)}` }] : []
  return [
    ...creditItem,
    { label: '近一年销售额', value: `¥${formatMoney(s.salesAmount)}` },
    { label: '销售订单', value: `${s.orderCount} 张` },
    { label: '最近下单', value: s.lastOrderDate || '-' },
    { label: '应收余额', value: `¥${formatMoney(s.arBalance)}` },
    { label: '赢单', value: `${s.wonCount} 个 · ¥${formatMoney(s.wonAmount)}` },
    { label: '未关闭工单', value: `${s.openTickets} 张` }
  ]
})

const oppColumns = [
  { title: '商机', key: 'name', width: 220 },
  { title: '阶段', key: 'stage', width: 100 },
  { title: '金额', key: 'amount', width: 130, align: 'right' },
  { title: '预计成交', dataIndex: 'expectedDate', width: 110 },
  { title: '负责人', dataIndex: 'ownerName', width: 110 }
]
const ticketColumns = [
  { title: '工单', key: 'title', width: 240 },
  { title: '类型', key: 'type', width: 90 },
  { title: '状态', key: 'status', width: 90 },
  { title: '处理人', dataIndex: 'assigneeName', width: 110 },
  { title: '受理时间', dataIndex: 'createTime', width: 160 }
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
