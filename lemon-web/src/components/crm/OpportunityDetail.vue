<template>
  <SubPage :open="open" :title="detail ? `${detail.oppNo} ${detail.name}` : '商机详情'" width="min(1000px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail">
        <DictTag type="crm_opp_stage" :value="detail.stage" />
        <template v-if="detail.stage !== 'WON'">
          <a-button v-perm="'crm:opp:edit'" size="small" type="primary" @click="openStage">推进阶段</a-button>
        </template>
        <a-button
          v-if="detail.stage === 'WON' && !detail.salesOrderId"
          v-perm="'crm:opp:order'"
          size="small"
          type="primary"
          @click="openOrder"
        >
          生成销售订单
        </a-button>
        <a-button v-if="detail.stage === 'WON'" v-perm="'pm:project:add'" size="small" @click="createProject">创建项目</a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-steps :current="stepIndex" :status="detail.stage === 'LOST' ? 'error' : stepIndex === steps.length - 1 ? 'finish' : 'process'" size="small" class="block">
          <a-step v-for="s in steps" :key="s.value" :title="s.label" :description="reachedAt(s.value)" />
        </a-steps>

        <a-alert v-if="detail.stage === 'LOST'" type="error" show-icon class="block" :message="`输单原因：${detail.lostReason || '-'}`" />
        <a-alert v-if="detail.salesOrderNo" type="success" show-icon class="block">
          <template #message>
            已生成销售订单 <b>{{ detail.salesOrderNo }}</b>，
            <router-link to="/scm/sales">到「销售订单」中审核</router-link>
          </template>
        </a-alert>

        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ detail.contactName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预计金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="赢单概率">{{ detail.probability }}%</a-descriptions-item>
          <a-descriptions-item label="预计成交">{{ detail.expectedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源">
            <DictTag v-if="detail.source" type="crm_source" :value="detail.source" /><span v-else>-</span>
            <span v-if="detail.campaignName" class="sub-text"> · {{ detail.campaignName }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="成交 / 输单日期">{{ detail.closeDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <a-tabs size="small">
          <a-tab-pane key="items" tab="产品明细">
            <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 700 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
                <template v-else-if="column.key === 'qty'">{{ Number(record.qty) }} {{ record.unit }}</template>
                <template v-else-if="column.key === 'price'">¥{{ formatMoney(record.price) }}</template>
                <template v-else-if="column.key === 'taxRate'">{{ Number(record.taxRate) }}%</template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
              </template>
            </a-table>
            <div v-if="!detail.items.length" class="sub-text empty">没有产品明细；赢单后生成销售订单需要先维护产品明细</div>
          </a-tab-pane>
          <a-tab-pane v-if="canQuote" key="quotation" :tab="`报价单${quotations.length ? `（${quotations.length}）` : ''}`">
            <div class="tab-toolbar">
              <a-button v-perm="'scm:quotation:add'" size="small" type="primary" @click="createQuotation">
                <template #icon><PlusOutlined /></template>新建报价
              </a-button>
              <router-link to="/scm/quotation" class="sub-text">到「销售报价」中审批、生成订单</router-link>
            </div>
            <a-table row-key="id" size="small" :columns="quotationColumns" :data-source="quotations" :loading="relatedLoading" :pagination="false" :scroll="{ x: 640 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalAmount'">¥{{ formatMoney(record.totalAmount) }}</template>
                <template v-else-if="column.key === 'status'"><DictTag type="scm_quotation_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'orderNo'">
                  <router-link v-if="record.orderId" :to="{ path: '/scm/sales', query: { id: record.orderId } }">{{ record.orderNo }}</router-link>
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane v-if="canContract" key="contract" :tab="`合同${contracts.length ? `（${contracts.length}）` : ''}`">
            <div class="tab-toolbar">
              <a-button v-perm="'crm:contract:add'" size="small" type="primary" @click="createContract">
                <template #icon><PlusOutlined /></template>新建合同
              </a-button>
            </div>
            <a-table row-key="id" size="small" :columns="contractColumns" :data-source="contracts" :loading="relatedLoading" :pagination="false" :scroll="{ x: 640 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <router-link :to="{ path: '/crm/contract', query: { id: record.id } }">{{ record.name }}</router-link>
                  <div class="sub-text">{{ record.contractNo }}</div>
                </template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
                <template v-else-if="column.key === 'received'">¥{{ formatMoney(record.receivedAmount || 0) }}</template>
                <template v-else-if="column.key === 'status'"><DictTag type="crm_contract_status" :value="record.status" /></template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="followup" tab="跟进记录">
            <FollowupTimeline target-type-value="OPPORTUNITY" :target-id="detail.id" @saved="emit('changed')" />
          </a-tab-pane>
          <a-tab-pane key="attachment" tab="附件">
            <AttachmentPanel biz-type="crm_opp" :biz-id="opportunityId" :editable="userStore.hasPerm('crm:opp:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <a-modal v-model:open="stageOpen" title="推进销售阶段" :confirm-loading="saving" destroy-on-close @ok="submitStage">
      <a-form layout="vertical">
        <a-form-item label="销售阶段">
          <a-radio-group v-model:value="stageForm.stage" button-style="solid">
            <a-radio-button v-for="s in dict.crm_opp_stage" :key="s.value" :value="s.value">{{ s.label }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="stageForm.stage === 'LOST'" label="输单原因" required>
          <a-textarea v-model:value="stageForm.lostReason" :rows="3" :maxlength="255" placeholder="如：价格过高、选择了竞品" />
        </a-form-item>
        <a-alert v-if="stageForm.stage === 'WON'" type="success" show-icon message="赢单后客户变为成交客户，可以按产品明细生成销售订单" />
      </a-form>
    </a-modal>

    <a-modal v-model:open="orderOpen" title="生成销售订单" :confirm-loading="saving" destroy-on-close @ok="submitOrder">
      <a-alert type="info" show-icon class="block" message="按商机的产品明细生成销售订单（草稿，来源「商机」），审核后即可出库" />
      <a-form layout="vertical">
        <a-form-item label="发货仓库">
          <WarehouseSelect v-model:value="orderForm.warehouseId" />
        </a-form-item>
        <a-form-item label="交货日期" extra="不填取商机的预计成交日期">
          <a-date-picker v-model:value="orderForm.deliveryDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import FollowupTimeline from './FollowupTimeline.vue'
import { changeOpportunityStage, getOpportunity, opportunityStages, opportunityToOrder, pageCrmContract } from '@/api/crm'
import { pageQuotation } from '@/api/scm/sourcing'
import { useDict } from '@/composables/useDict'
import { formatMoney } from '@/components/scm/scm'
import { OPEN_STAGES } from './crm'
import { useUserStore } from '@/store/user'

/** 商机详情：阶段进度、产品明细、跟进；推进阶段、赢单后生成销售订单 */
const props = defineProps({
  open: Boolean,
  opportunityId: Number
})
const userStore = useUserStore()
const router = useRouter()

/** 赢单后一键建项目：带入客户、商机、金额 */
function createProject() {
  const d = detail.value
  router.push({
    path: '/pm/project',
    query: { create: 1, opportunityId: d.id, customerId: d.customerId, contractAmount: d.amount, name: d.name }
  })
}
const emit = defineEmits(['update:open', 'changed'])
const dict = useDict('crm_opp_stage')

const detail = ref(null)
const logs = ref([])
const loading = ref(false)

const label = (v) => dict.crm_opp_stage.find((d) => d.value === v)?.label || v
const steps = computed(() => {
  const last = detail.value?.stage === 'LOST' ? 'LOST' : 'WON'
  return [...OPEN_STAGES, last].map((v) => ({ value: v, label: label(v) }))
})
const stepIndex = computed(() => {
  if (!detail.value) return 0
  if (detail.value.stage === 'LOST') return steps.value.length - 1
  return steps.value.findIndex((s) => s.value === detail.value.stage)
})
function reachedAt(stage) {
  const log = [...logs.value].reverse().find((l) => l.toStage === stage)
  return log ? `${log.createTime?.slice(0, 10)} ${log.userName || ''}` : ''
}

const itemColumns = [
  { title: '产品', key: 'material', width: 240 },
  { title: '数量', key: 'qty', width: 110, align: 'right' },
  { title: '含税单价', key: 'price', width: 120, align: 'right' },
  { title: '税率', key: 'taxRate', width: 80, align: 'right' },
  { title: '金额', key: 'amount', width: 130, align: 'right' }
]

async function load() {
  loading.value = true
  try {
    const [d, l] = await Promise.all([getOpportunity(props.opportunityId), opportunityStages(props.opportunityId)])
    detail.value = d
    logs.value = l
  } finally {
    loading.value = false
  }
  loadRelated()
}

// 报价单、合同：按当前用户的权限显示
const canQuote = computed(() => userStore.hasPerm('scm:quotation:list'))
const canContract = computed(() => userStore.hasPerm('crm:contract:list'))
const quotations = ref([])
const contracts = ref([])
const relatedLoading = ref(false)
const quotationColumns = [
  { title: '报价单号', dataIndex: 'quoteNo', width: 170 },
  { title: '报价日期', dataIndex: 'quoteDate', width: 110 },
  { title: '有效期至', dataIndex: 'validTo', width: 110 },
  { title: '价税合计', key: 'totalAmount', width: 130, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  { title: '销售订单', key: 'orderNo', width: 160 }
]
const contractColumns = [
  { title: '合同', key: 'name', width: 220 },
  { title: '签订日期', dataIndex: 'signDate', width: 110 },
  { title: '合同金额', key: 'amount', width: 130, align: 'right' },
  { title: '已回款', key: 'received', width: 130, align: 'right' },
  { title: '状态', key: 'status', width: 90 }
]
async function loadRelated() {
  const params = { opportunityId: props.opportunityId, current: 1, size: 100 }
  relatedLoading.value = true
  try {
    const [q, c] = await Promise.all([
      canQuote.value ? pageQuotation(params).catch(() => null) : null,
      canContract.value ? pageCrmContract(params).catch(() => null) : null
    ])
    quotations.value = q?.records || []
    contracts.value = c?.records || []
  } finally {
    relatedLoading.value = false
  }
}
function createQuotation() {
  const d = detail.value
  router.push({ path: '/scm/quotation', query: { create: 1, customerId: d.customerId, customerName: d.customerName, opportunityId: d.id } })
}
function createContract() {
  const d = detail.value
  router.push({ path: '/crm/contract', query: { create: 1, customerId: d.customerId, opportunityId: d.id } })
}

watch(
  () => props.open,
  (open) => {
    if (open && props.opportunityId) {
      detail.value = null
      load()
    }
  }
)

const saving = ref(false)
const stageOpen = ref(false)
const stageForm = ref({})
function openStage() {
  const i = OPEN_STAGES.indexOf(detail.value.stage)
  stageForm.value = { stage: i >= 0 && i < OPEN_STAGES.length - 1 ? OPEN_STAGES[i + 1] : 'WON', lostReason: '' }
  stageOpen.value = true
}
async function submitStage() {
  if (stageForm.value.stage === 'LOST' && !stageForm.value.lostReason?.trim()) {
    message.warning('请填写输单原因')
    return
  }
  saving.value = true
  try {
    await changeOpportunityStage(detail.value.id, stageForm.value)
    message.success('阶段已更新')
    stageOpen.value = false
    load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

const orderOpen = ref(false)
const orderForm = ref({})
function openOrder() {
  orderForm.value = { warehouseId: undefined, deliveryDate: detail.value.expectedDate || undefined }
  orderOpen.value = true
}
async function submitOrder() {
  saving.value = true
  try {
    await opportunityToOrder(detail.value.id, orderForm.value)
    message.success('已生成销售订单')
    orderOpen.value = false
    load()
    emit('changed')
  } finally {
    saving.value = false
  }
}
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
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
