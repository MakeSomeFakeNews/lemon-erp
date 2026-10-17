<template>
  <SubPage :open="open" :title="detail ? `${detail.ticketNo} ${detail.title}` : $t('crm.ticketDetail.title')" width="min(900px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail">
        <DictTag type="crm_ticket_status" :value="detail.status" />
        <template v-if="userStore.hasPerm('crm:ticket:handle')">
          <template v-if="active">
            <a-button size="small" @click="openAction('assign')">{{ $t('crm.common.action.assign') }}</a-button>
            <a-button size="small" @click="openAction('process')">{{ $t('crm.common.action.process') }}</a-button>
            <a-button size="small" type="primary" @click="openAction('resolve')">{{ $t('crm.common.action.resolve') }}</a-button>
          </template>
          <a-button v-if="detail.status === 'RESOLVED'" size="small" type="primary" @click="openAction('close')">{{ $t('crm.common.action.close') }}</a-button>
          <a-button v-if="!active" size="small" @click="openAction('reopen')">{{ $t('crm.common.action.reopen') }}</a-button>
        </template>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert
          v-if="active"
          :type="detail.overdue ? 'error' : 'info'"
          show-icon
          class="block"
          :message="detail.overdue ? $t('crm.common.overdueMsg', { overdue: overdueText(detail.dueTime), due: detail.dueTime }) : $t('crm.common.remainMsg', { remain: remaining(detail.dueTime), due: detail.dueTime })"
        />
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('crm.common.f.customer')">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.contact')">{{ detail.contactName || '-' }} {{ detail.contactPhone || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="crm_ticket_type" :value="detail.type" /></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.priority')"><DictTag type="crm_ticket_priority" :value="detail.priority" /></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.channel')"><DictTag v-if="detail.channel" type="crm_ticket_channel" :value="detail.channel" /><span v-else>-</span></a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.assignee')">{{ detail.assigneeName || $t('crm.common.unassigned') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.product')">{{ detail.materialCode ? `${detail.materialCode} ${detail.materialName}` : '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.serialNo')">{{ detail.serialNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.ticketDetail.accepted')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.resolvedTime')">{{ detail.resolvedTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('crm.common.f.description')" :span="2"><div class="pre">{{ detail.description || '-' }}</div></a-descriptions-item>
          <a-descriptions-item v-if="detail.solution" :label="$t('crm.common.f.solution')" :span="2"><div class="pre">{{ detail.solution }}</div></a-descriptions-item>
          <a-descriptions-item v-if="detail.satisfaction" :label="$t('crm.common.f.satisfaction')" :span="2">
            <a-rate :value="detail.satisfaction" disabled />
          </a-descriptions-item>
        </a-descriptions>

        <div class="title">
          {{ $t('crm.ticketDetail.partsAndFee') }}
          <span class="title-actions">
            <a-button v-if="detail.status !== 'CLOSED'" v-perm="'crm:ticket:handle'" size="small" @click="openParts">{{ $t('crm.ticketDetail.issueParts') }}</a-button>
            <a-button v-if="!detail.feeBillId" v-perm="'crm:ticket:handle'" size="small" @click="openCharge">{{ $t('crm.ticketDetail.charge') }}</a-button>
          </span>
        </div>
        <div v-if="detail.feeBillId" class="sub-text fee">{{ $t('crm.ticketDetail.charged', { amount: formatMoney(detail.feeAmount) }) }}</div>
        <a-table v-if="parts.length" row-key="id" size="small" :columns="partColumns" :data-source="parts" :pagination="false" class="block" />

        <div class="title">{{ $t('crm.ticketDetail.logs') }}</div>
        <a-timeline>
          <a-timeline-item v-for="l in logs" :key="l.id" :color="ACTION_COLORS[l.action] || 'gray'">
            <div>
              <b>{{ ACTIONS[l.action] || l.action }}</b>
              <span class="sub-text"> {{ l.createTime }} · {{ l.userName }}</span>
            </div>
            <div v-if="l.content" class="pre">{{ l.content }}</div>
          </a-timeline-item>
        </a-timeline>
      </template>
    </a-spin>

    <a-modal v-model:open="partsOpen" :title="$t('crm.ticketDetail.issueParts')" width="min(760px, 100vw)" :confirm-loading="saving" destroy-on-close @ok="submitParts">
      <a-form layout="inline" class="parts-head">
        <a-form-item :label="$t('crm.ticketDetail.warehouse')" required>
          <WarehouseSelect v-model:value="partsForm.warehouseId" style="width: 180px" @change="partsForm.items = []" />
        </a-form-item>
        <a-form-item :label="$t('common.field.date')" required>
          <a-date-picker v-model:value="partsForm.bizDate" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-button :disabled="!partsForm.warehouseId" @click="pickerOpen = true">{{ $t('crm.ticketDetail.pickStock') }}</a-button>
      </a-form>
      <a-table row-key="key" size="small" :columns="pickColumns" :data-source="partsForm.items" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'slot'">{{ record.locationCode || $t('crm.ticketDetail.noLocation') }}{{ record.batchNo ? ' · ' + record.batchNo : '' }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :max="record.available" size="small" />
            <span class="sub-text"> / {{ record.available }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="partsForm.items.splice(index, 1)">{{ $t('common.action.delete') }}</a-button>
          </template>
        </template>
      </a-table>
      <a-input v-model:value="partsForm.remark" :placeholder="$t('common.field.remark')" :maxlength="255" class="parts-remark" />
      <StockPicker v-model:open="pickerOpen" :warehouse-id="partsForm.warehouseId" @pick="onPick" />
    </a-modal>

    <a-modal v-model:open="chargeOpen" :title="$t('crm.ticketDetail.charge')" :confirm-loading="saving" destroy-on-close @ok="submitCharge">
      <a-form layout="vertical">
        <a-form-item :label="$t('crm.ticketDetail.chargeAmount')" required>
          <a-input-number v-model:value="chargeForm.amount" :min="0.01" :precision="2" :addon-after="$t('crm.ticketDetail.yuan')" class="full" />
        </a-form-item>
        <a-form-item :label="$t('crm.common.f.taxRate')">
          <a-input-number v-model:value="chargeForm.taxRate" :min="0" :max="100" addon-after="%" class="full" />
        </a-form-item>
        <a-form-item :label="$t('crm.ticketDetail.chargeDate')" required>
          <a-date-picker v-model:value="chargeForm.billDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.description')">
          <a-input v-model:value="chargeForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
      <div class="sub-text">{{ $t('crm.ticketDetail.chargeTip') }}</div>
    </a-modal>

    <a-modal v-model:open="actionOpen" :title="ACTION_TITLES[action]" :confirm-loading="saving" destroy-on-close @ok="submitAction">
      <a-form layout="vertical">
        <a-form-item v-if="action === 'assign'" :label="$t('crm.common.f.assignee')" required>
          <IdentityUserSelect v-model:value="actionForm.assigneeId" :label="detail?.assigneeName" />
        </a-form-item>
        <a-form-item v-if="action === 'close'" :label="$t('crm.common.f.satisfaction')">
          <a-rate v-model:value="actionForm.satisfaction" />
        </a-form-item>
        <a-form-item :label="CONTENT_LABELS[action]" :required="action !== 'assign' && action !== 'close'">
          <a-textarea v-model:value="actionForm.content" :rows="4" :maxlength="2000" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </SubPage>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import StockPicker from '@/components/wms/StockPicker.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { chargeTicket, getTicket, issueTicketParts, ticketAction, ticketLogs, ticketParts } from '@/api/crm'
import { formatMoney } from '@/components/scm/scm'
import { availableQty, today } from '@/components/wms/wms'
import { useUserStore } from '@/store/user'
import { overdueText, remaining } from './crm'

/** 服务工单详情：处理时限、处理记录；指派、处理、解决、关闭、重新打开 */
const props = defineProps({
  open: Boolean,
  ticketId: Number
})
const emit = defineEmits(['update:open', 'changed'])
const userStore = useUserStore()
const { t } = useI18n()

const ACTIONS = Object.fromEntries(['CREATE', 'ASSIGN', 'PROCESS', 'RESOLVE', 'CLOSE', 'REOPEN'].map((k) => [k, t(`crm.common.log.${k}`)]))
const ACTION_COLORS = { CREATE: 'blue', ASSIGN: 'blue', PROCESS: 'blue', RESOLVE: 'green', CLOSE: 'gray', REOPEN: 'red' }
const ACTION_KEYS = ['assign', 'process', 'resolve', 'close', 'reopen']
const ACTION_TITLES = Object.fromEntries(ACTION_KEYS.map((k) => [k, t(`crm.common.actionTitle.${k}`)]))
const CONTENT_LABELS = Object.fromEntries(ACTION_KEYS.map((k) => [k, t(`crm.common.contentLabel.${k}`)]))

const detail = ref(null)
const logs = ref([])
const loading = ref(false)
const active = computed(() => detail.value && ['OPEN', 'PROCESSING'].includes(detail.value.status))

async function load() {
  loading.value = true
  try {
    const [d, l, p] = await Promise.all([getTicket(props.ticketId), ticketLogs(props.ticketId), ticketParts(props.ticketId)])
    detail.value = d
    logs.value = l
    parts.value = p
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open && props.ticketId) {
      detail.value = null
      load()
    }
  }
)

// 备件领用、服务收费
const parts = ref([])
const partColumns = [
  { title: t('crm.ticketDetail.col.outbound'), dataIndex: 'docNo', width: 160 },
  { title: t('crm.ticketDetail.warehouse'), dataIndex: 'warehouseName', width: 120 },
  { title: t('common.field.date'), dataIndex: 'bizDate', width: 110 },
  { title: t('common.field.quantity'), dataIndex: 'totalQty', width: 90, align: 'right' },
  { title: t('crm.ticketDetail.col.issuer'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true }
]
const pickColumns = [
  { title: t('crm.ticketDetail.col.material'), key: 'material', width: 220 },
  { title: t('crm.ticketDetail.col.slot'), key: 'slot', width: 160 },
  { title: t('crm.ticketDetail.col.qty'), key: 'qty', width: 180 },
  { title: '', key: 'action', width: 60 }
]
const partsOpen = ref(false)
const pickerOpen = ref(false)
const partsForm = ref({})
function openParts() {
  partsForm.value = { warehouseId: undefined, bizDate: today(), items: [], remark: '' }
  partsOpen.value = true
}
function onPick(stocks) {
  stocks.forEach((s) => {
    if (partsForm.value.items.some((i) => i.key === s.id)) return
    partsForm.value.items.push({
      key: s.id,
      materialId: s.materialId,
      materialCode: s.materialCode,
      materialName: s.materialName,
      locationId: s.locationId || null,
      locationCode: s.locationCode,
      batchNo: s.batchNo || null,
      available: availableQty(s),
      qty: 1
    })
  })
}
async function submitParts() {
  const f = partsForm.value
  if (!f.warehouseId || !f.items.length) {
    message.warning(t('crm.ticketDetail.partsRequired'))
    return
  }
  saving.value = true
  try {
    await issueTicketParts(props.ticketId, {
      warehouseId: f.warehouseId,
      bizDate: f.bizDate,
      remark: f.remark || null,
      items: f.items.map((i) => ({ materialId: i.materialId, locationId: i.locationId, batchNo: i.batchNo, qty: i.qty }))
    })
    message.success(t('crm.ticketDetail.partsIssued'))
    partsOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
const chargeOpen = ref(false)
const chargeForm = ref({})
function openCharge() {
  chargeForm.value = { amount: undefined, taxRate: 6, billDate: today(), remark: '' }
  chargeOpen.value = true
}
async function submitCharge() {
  if (!chargeForm.value.amount) {
    message.warning(t('crm.ticketDetail.amountRequired'))
    return
  }
  saving.value = true
  try {
    await chargeTicket(props.ticketId, { ...chargeForm.value, remark: chargeForm.value.remark || null })
    message.success(t('crm.ticketDetail.billCreated'))
    chargeOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const action = ref('process')
const actionOpen = ref(false)
const actionForm = ref({})
const saving = ref(false)

function openAction(a) {
  action.value = a
  actionForm.value = { assigneeId: a === 'assign' ? detail.value.assigneeId : undefined, content: '', satisfaction: a === 'close' ? 5 : undefined }
  actionOpen.value = true
}

async function submitAction() {
  if (action.value === 'assign' && !actionForm.value.assigneeId) {
    message.warning(t('crm.common.assigneeRequired'))
    return
  }
  if (!['assign', 'close'].includes(action.value) && !actionForm.value.content?.trim()) {
    message.warning(t('crm.common.fillName', { name: CONTENT_LABELS[action.value] }))
    return
  }
  saving.value = true
  try {
    await ticketAction(detail.value.id, action.value, actionForm.value)
    message.success(t('common.msg.operationSuccess'))
    actionOpen.value = false
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
.title {
  font-weight: 500;
  margin-bottom: 12px;
}
.pre {
  white-space: pre-wrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.title-actions {
  float: right;
  display: inline-flex;
  gap: 8px;
}
.fee {
  margin-bottom: 8px;
}
.parts-head {
  margin-bottom: 12px;
  row-gap: 8px;
}
.parts-remark {
  margin-top: 12px;
}
</style>
