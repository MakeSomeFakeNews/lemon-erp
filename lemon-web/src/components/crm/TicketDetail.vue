<template>
  <SubPage :open="open" :title="detail ? `${detail.ticketNo} ${detail.title}` : '工单详情'" width="min(900px, 100vw)" destroy-on-close @close="emit('update:open', false)">
    <template #extra>
      <a-space v-if="detail">
        <DictTag type="crm_ticket_status" :value="detail.status" />
        <template v-if="userStore.hasPerm('crm:ticket:handle')">
          <template v-if="active">
            <a-button size="small" @click="openAction('assign')">指派</a-button>
            <a-button size="small" @click="openAction('process')">处理记录</a-button>
            <a-button size="small" type="primary" @click="openAction('resolve')">解决</a-button>
          </template>
          <a-button v-if="detail.status === 'RESOLVED'" size="small" type="primary" @click="openAction('close')">关闭</a-button>
          <a-button v-if="!active" size="small" @click="openAction('reopen')">重新打开</a-button>
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
          :message="detail.overdue ? `已${overdueText(detail.dueTime)}，处理时限 ${detail.dueTime}` : `剩余 ${remaining(detail.dueTime)}，处理时限 ${detail.dueTime}`"
        />
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" bordered class="block">
          <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ detail.contactName || '-' }} {{ detail.contactPhone || '' }}</a-descriptions-item>
          <a-descriptions-item label="类型"><DictTag type="crm_ticket_type" :value="detail.type" /></a-descriptions-item>
          <a-descriptions-item label="优先级"><DictTag type="crm_ticket_priority" :value="detail.priority" /></a-descriptions-item>
          <a-descriptions-item label="受理渠道"><DictTag v-if="detail.channel" type="crm_ticket_channel" :value="detail.channel" /><span v-else>-</span></a-descriptions-item>
          <a-descriptions-item label="处理人">{{ detail.assigneeName || '未指派' }}</a-descriptions-item>
          <a-descriptions-item label="相关产品">{{ detail.materialCode ? `${detail.materialCode} ${detail.materialName}` : '-' }}</a-descriptions-item>
          <a-descriptions-item label="序列号">{{ detail.serialNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="受理">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="解决时间">{{ detail.resolvedTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="问题描述" :span="2"><div class="pre">{{ detail.description || '-' }}</div></a-descriptions-item>
          <a-descriptions-item v-if="detail.solution" label="解决方案" :span="2"><div class="pre">{{ detail.solution }}</div></a-descriptions-item>
          <a-descriptions-item v-if="detail.satisfaction" label="客户满意度" :span="2">
            <a-rate :value="detail.satisfaction" disabled />
          </a-descriptions-item>
        </a-descriptions>

        <div class="title">
          备件与收费
          <span class="title-actions">
            <a-button v-if="detail.status !== 'CLOSED'" v-perm="'crm:ticket:handle'" size="small" @click="openParts">领用备件</a-button>
            <a-button v-if="!detail.feeBillId" v-perm="'crm:ticket:handle'" size="small" @click="openCharge">服务收费</a-button>
          </span>
        </div>
        <div v-if="detail.feeBillId" class="sub-text fee">已收费 ¥{{ formatMoney(detail.feeAmount) }}（已生成应收单，在「应收管理」中生成凭证、收款核销）</div>
        <a-table v-if="parts.length" row-key="id" size="small" :columns="partColumns" :data-source="parts" :pagination="false" class="block" />

        <div class="title">处理记录</div>
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

    <a-modal v-model:open="partsOpen" title="领用备件" width="min(760px, 100vw)" :confirm-loading="saving" destroy-on-close @ok="submitParts">
      <a-form layout="inline" class="parts-head">
        <a-form-item label="仓库" required>
          <WarehouseSelect v-model:value="partsForm.warehouseId" style="width: 180px" @change="partsForm.items = []" />
        </a-form-item>
        <a-form-item label="日期" required>
          <a-date-picker v-model:value="partsForm.bizDate" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-button :disabled="!partsForm.warehouseId" @click="pickerOpen = true">选择库存</a-button>
      </a-form>
      <a-table row-key="key" size="small" :columns="pickColumns" :data-source="partsForm.items" :pagination="false">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">{{ record.materialCode }} {{ record.materialName }}</template>
          <template v-else-if="column.key === 'slot'">{{ record.locationCode || '未指定' }}{{ record.batchNo ? ' · ' + record.batchNo : '' }}</template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :max="record.available" size="small" />
            <span class="sub-text"> / {{ record.available }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="partsForm.items.splice(index, 1)">删除</a-button>
          </template>
        </template>
      </a-table>
      <a-input v-model:value="partsForm.remark" placeholder="备注" :maxlength="255" class="parts-remark" />
      <StockPicker v-model:open="pickerOpen" :warehouse-id="partsForm.warehouseId" @pick="onPick" />
    </a-modal>

    <a-modal v-model:open="chargeOpen" title="服务收费" :confirm-loading="saving" destroy-on-close @ok="submitCharge">
      <a-form layout="vertical">
        <a-form-item label="收费金额（含税）" required>
          <a-input-number v-model:value="chargeForm.amount" :min="0.01" :precision="2" addon-after="元" class="full" />
        </a-form-item>
        <a-form-item label="税率">
          <a-input-number v-model:value="chargeForm.taxRate" :min="0" :max="100" addon-after="%" class="full" />
        </a-form-item>
        <a-form-item label="收费日期" required>
          <a-date-picker v-model:value="chargeForm.billDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item label="说明">
          <a-input v-model:value="chargeForm.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
      <div class="sub-text">生成客户应收单，凭证计入其他业务收入。</div>
    </a-modal>

    <a-modal v-model:open="actionOpen" :title="ACTION_TITLES[action]" :confirm-loading="saving" destroy-on-close @ok="submitAction">
      <a-form layout="vertical">
        <a-form-item v-if="action === 'assign'" label="处理人" required>
          <IdentityUserSelect v-model:value="actionForm.assigneeId" :label="detail?.assigneeName" />
        </a-form-item>
        <a-form-item v-if="action === 'close'" label="客户满意度">
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

const ACTIONS = { CREATE: '受理', ASSIGN: '指派', PROCESS: '处理', RESOLVE: '解决', CLOSE: '关闭', REOPEN: '重新打开' }
const ACTION_COLORS = { CREATE: 'blue', ASSIGN: 'blue', PROCESS: 'blue', RESOLVE: 'green', CLOSE: 'gray', REOPEN: 'red' }
const ACTION_TITLES = { assign: '指派处理人', process: '添加处理记录', resolve: '解决工单', close: '关闭工单', reopen: '重新打开' }
const CONTENT_LABELS = { assign: '说明', process: '处理记录', resolve: '解决方案', close: '客户反馈', reopen: '重新打开的原因' }

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
  { title: '出库单', dataIndex: 'docNo', width: 160 },
  { title: '仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '日期', dataIndex: 'bizDate', width: 110 },
  { title: '数量', dataIndex: 'totalQty', width: 90, align: 'right' },
  { title: '领用人', dataIndex: 'createByName', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]
const pickColumns = [
  { title: '物料', key: 'material', width: 220 },
  { title: '库位 / 批次', key: 'slot', width: 160 },
  { title: '数量 / 可用', key: 'qty', width: 180 },
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
    message.warning('请选择仓库和备件')
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
    message.success('已领用，库存已扣减')
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
    message.warning('请填写收费金额')
    return
  }
  saving.value = true
  try {
    await chargeTicket(props.ticketId, { ...chargeForm.value, remark: chargeForm.value.remark || null })
    message.success('已生成应收单')
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
    message.warning('请选择处理人')
    return
  }
  if (!['assign', 'close'].includes(action.value) && !actionForm.value.content?.trim()) {
    message.warning(`请填写${CONTENT_LABELS[action.value]}`)
    return
  }
  saving.value = true
  try {
    await ticketAction(detail.value.id, action.value, actionForm.value)
    message.success('操作成功')
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
