<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-segmented v-model:value="kind" :options="KIND_OPTIONS" />
        <a-select v-model:value="query.status" placeholder="全部状态" allow-clear :options="statusOptions" class="filter" />
        <a-input-search v-model:value="query.keyword" placeholder="票据号码、往来单位、承兑人" allow-clear class="filter-wide" @search="search" />
        <a-checkbox v-model:checked="dueSoon">30 天内到期</a-checkbox>
        <template #extra>
          <a-button v-perm="'fin:note:edit'" type="primary" @click="openRegister">
            <template #icon><PlusOutlined /></template>{{ conf.registerLabel }}
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'noteNo'">
            <a @click="openDetail(record.id)">{{ record.noteNo }}</a>
            <div class="sub-text">{{ record.electronic ? '电子票据' : '纸质票据' }}</div>
          </template>
          <template v-else-if="column.key === 'noteType'"><DictTag type="fin_note_type" :value="record.noteType" /></template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'dueDate'">
            <div>{{ record.dueDate }}</div>
            <div v-if="pending(record)" :class="['sub-text', dueClass(record)]">{{ dueText(record) }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="fin_note_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record.id)">查看</a-button>
              <span v-if="actionsOf(record).length" v-perm="'fin:note:edit'">
                <a-dropdown>
                  <a-button type="link" size="small">办理 <DownOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => openAction(record, key)">
                      <a-menu-item v-for="a in actionsOf(record)" :key="a.key">{{ a.label }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 收票 / 开票 -->
    <SubPage :open="registerOpen" :title="conf.registerLabel" width="min(980px, 100vw)" destroy-on-close @close="registerOpen = false">
      <a-alert type="info" show-icon class="block" :message="conf.registerTip" />
      <a-form ref="registerRef" :model="reg" :rules="registerRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-form-item label="票据类型" name="noteType">
              <a-select v-model:value="reg.noteType" :options="toOptions(dict.fin_note_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item label="票据号码" name="noteNo">
              <a-input v-model:value="reg.noteNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item label="电子票据" name="electronic">
              <a-switch v-model:checked="reg.electronic" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="conf.partnerLabel" name="partnerId">
              <PartnerSelect v-model:value="reg.partnerId" :type="conf.partner" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="票面金额" name="amount">
              <a-input-number v-model:value="reg.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="conf.actionDateLabel" name="actionDate">
              <a-date-picker v-model:value="reg.actionDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="出票日期" name="issueDate">
              <a-date-picker v-model:value="reg.issueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item label="到期日期" name="dueDate">
              <a-date-picker v-model:value="reg.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item label="承兑人" name="acceptor">
              <a-input v-model:value="reg.acceptor" :maxlength="128" placeholder="承兑银行或企业" />
            </a-form-item>
          </a-col>
          <a-col v-if="kind === 'RECEIVABLE'" :xs="24" :sm="8">
            <a-form-item label="出票人" name="drawer">
              <a-input v-model:value="reg.drawer" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="kind === 'RECEIVABLE' ? 16 : 24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="reg.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <BillSettleTable v-model="reg.items" :kind="conf.billKind" :partner-id="reg.partnerId" :target="reg.amount" />
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="registerOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submitRegister">确认{{ conf.registerLabel }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 背书转让 -->
    <SubPage :open="endorseOpen" :title="`背书转让 - ${current?.noteNo || ''}`" width="min(900px, 100vw)" destroy-on-close @close="endorseOpen = false">
      <a-alert type="info" show-icon class="block" message="把持有的票据转给供应商支付货款：核销供应商的应付单，核销合计须等于票面金额。启用付款审批时需审批通过后生效。" />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item label="供应商" required>
              <PartnerSelect v-model:value="endorse.supplierId" type="supplier" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="背书日期" required>
              <a-date-picker v-model:value="endorse.actionDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="票面金额"><b>¥{{ formatMoney(current?.amount) }}</b></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注"><a-input v-model:value="endorse.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <BillSettleTable v-model="endorse.items" kind="ap" :partner-id="endorse.supplierId" :target="Number(current?.amount)" />
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="endorseOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" :disabled="!endorse.supplierId" @click="submitEndorse">确认背书</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 贴现 / 托收 / 拒付 / 兑付 -->
    <a-modal v-model:open="settleOpen" :title="`${actionLabel} - ${current?.noteNo || ''}`" :confirm-loading="saving" @ok="submitSettle">
      <a-alert type="info" show-icon class="block" :message="SETTLE_TIPS[action]" />
      <a-form layout="vertical">
        <a-form-item label="日期" required>
          <a-date-picker v-model:value="settle.actionDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item v-if="action !== 'dishonor'" :label="action === 'pay' ? '付款账户' : '入账账户'" required>
          <AccountSelect v-model:value="settle.accountId" :prefix="['1001', '1002']" placeholder="银行存款" />
        </a-form-item>
        <template v-if="action === 'discount'">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="年贴现率（%）" extra="按 360 天计息，可直接修改贴现息">
                <a-input-number v-model:value="discountRate" :min="0" :max="100" :precision="4" class="full" @change="calcInterest" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="贴现息">
                <a-input-number v-model:value="settle.interest" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
          <div class="sub-text discount-net">实收金额 ¥{{ formatMoney(Number(current?.amount || 0) - Number(settle.interest || 0)) }}，剩余 {{ discountDays }} 天到期</div>
        </template>
        <a-form-item label="备注">
          <a-input v-model:value="settle.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `票据 ${detail.noteNo}` : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <span v-perm="'fin:note:edit'">
          <a-space>
            <a-popconfirm v-if="canUndo" title="撤销最近一次操作？生成的凭证（须未审核）一并删除" @confirm="undo">
              <a-button><template #icon><UndoOutlined /></template>撤销{{ lastActionLabel }}</a-button>
            </a-popconfirm>
            <a-popconfirm v-if="canDelete" title="删除票据？登记时生成的收付款单一并删除，往来单据恢复未核销" @confirm="remove">
              <a-button danger><template #icon><DeleteOutlined /></template>删除</a-button>
            </a-popconfirm>
          </a-space>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item label="票据号码">{{ detail.noteNo }}</a-descriptions-item>
          <a-descriptions-item label="类型"><DictTag type="fin_note_type" :value="detail.noteType" /> {{ detail.electronic ? '电子' : '纸质' }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="fin_note_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="detail.kind === 'RECEIVABLE' ? '收票客户' : '收款供应商'">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="票面金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="承兑人">{{ detail.acceptor || '-' }}</a-descriptions-item>
          <a-descriptions-item label="出票日期">{{ detail.issueDate }}</a-descriptions-item>
          <a-descriptions-item label="到期日期">{{ detail.dueDate }}</a-descriptions-item>
          <a-descriptions-item label="出票人">{{ detail.drawer || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">流转记录</div>
        <a-table row-key="id" size="small" :columns="logColumns" :data-source="detail.logs" :pagination="false" :scroll="{ x: 900 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><DictTag type="fin_note_action" :value="record.action" /></template>
            <template v-else-if="column.key === 'target'">{{ record.partnerName || record.accountName || '-' }}</template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ formatMoney(record.amount) }}
              <div v-if="Number(record.interest)" class="sub-text">贴现息 ¥{{ formatMoney(record.interest) }}</div>
            </template>
            <template v-else-if="column.key === 'ref'">
              <div v-if="record.settleNo">收付款单 {{ record.settleNo }}</div>
              <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">凭证 {{ record.voucherNo }}</a>
              <div v-if="record.billNo" class="sub-text">转回应收单 {{ record.billNo }}</div>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="refreshDetail" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { DeleteOutlined, DownOutlined, PlusOutlined, UndoOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import BillSettleTable from '@/components/fin/BillSettleTable.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { deleteNote, getNote, issueNote, noteAction, pageNote, receiveNote, undoNote } from '@/api/fin'
import { formatMoney } from '@/components/fin/fin'
import { today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/** 票据管理：应收票据（收票 → 背书 / 贴现 / 托收 / 拒付）、应付票据（开票 → 兑付） */
const dict = useDict('fin_note_type', 'fin_note_status', 'fin_note_action')

const KIND_OPTIONS = [
  { label: '应收票据', value: 'RECEIVABLE' },
  { label: '应付票据', value: 'PAYABLE' }
]
const CONF = {
  RECEIVABLE: {
    registerLabel: '收票', partner: 'customer', partnerLabel: '客户', billKind: 'ar', actionDateLabel: '收票日期',
    registerTip: '客户以承兑汇票付款：登记票据并核销客户的应收单（核销合计须等于票面金额），生成的收款单可在收款单中生成凭证（借 应收票据 / 贷 应收账款）。',
    statuses: ['HELD', 'ENDORSING', 'ENDORSED', 'DISCOUNTED', 'COLLECTED', 'DISHONORED']
  },
  PAYABLE: {
    registerLabel: '开具票据', partner: 'supplier', partnerLabel: '供应商', billKind: 'ap', actionDateLabel: '开票日期',
    registerTip: '以应付票据支付供应商：登记票据并核销供应商的应付单（核销合计须等于票面金额），启用付款审批时需审批通过后生效；生成的付款单可生成凭证（借 应付账款 / 贷 应付票据）。',
    statuses: ['ISSUING', 'ISSUED', 'PAID', 'CANCELLED']
  }
}
/** 各状态可以办理的操作 */
const ACTIONS = {
  HELD: [
    { key: 'endorse', label: '背书转让' },
    { key: 'discount', label: '贴现' },
    { key: 'collect', label: '托收' },
    { key: 'dishonor', label: '拒付' }
  ],
  ISSUED: [{ key: 'pay', label: '到期兑付' }]
}
const SETTLE_TIPS = {
  discount: '持票向银行贴现：借 银行存款（实收）、财务费用（贴现息），贷 应收票据',
  collect: '票据到期托收：借 银行存款，贷 应收票据',
  dishonor: '票据被拒付：转回应收账款并生成应收单，客户需重新付款。借 应收账款，贷 应收票据',
  pay: '应付票据到期兑付：借 应付票据，贷 银行存款'
}

const kind = ref('RECEIVABLE')
const conf = computed(() => CONF[kind.value])
const statusOptions = computed(() =>
  conf.value.statuses.map((s) => ({ value: s, label: dict.fin_note_status?.find((d) => d.value === s)?.label || s }))
)
const dueSoon = ref(false)

const columns = [
  { title: '票据号码', key: 'noteNo', width: 200 },
  { title: '类型', key: 'noteType', width: 130 },
  { title: '往来单位', dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: '票面金额', key: 'amount', width: 140, align: 'right' },
  { title: '出票日期', dataIndex: 'issueDate', width: 110 },
  { title: '到期日期', key: 'dueDate', width: 120 },
  { title: '承兑人', dataIndex: 'acceptor', width: 160, ellipsis: true },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-note')
const query = reactive({ kind: 'RECEIVABLE', status: undefined, keyword: undefined, dueWithin: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageNote, query)
watch(kind, (k) => {
  query.kind = k
  query.status = undefined
  search()
})
watch(() => query.status, () => search())
watch(dueSoon, (v) => {
  query.dueWithin = v ? 30 : undefined
  search()
})
onMounted(load)

const pending = (r) => r.status === 'HELD' || r.status === 'ISSUED'
const daysLeft = (r) => dayjs(r.dueDate).diff(dayjs().startOf('day'), 'day')
const dueText = (r) => (daysLeft(r) < 0 ? `已到期 ${-daysLeft(r)} 天` : daysLeft(r) === 0 ? '今天到期' : `${daysLeft(r)} 天后到期`)
const dueClass = (r) => (daysLeft(r) <= 0 ? 'danger' : daysLeft(r) <= 7 ? 'warn' : '')
const actionsOf = (r) => ACTIONS[r.status] || []

// ---------------------------------------------------------------- 收票 / 开票
const registerOpen = ref(false)
const registerRef = ref()
const reg = ref({})
const saving = ref(false)
const registerRules = {
  noteType: [{ required: true, message: '请选择票据类型' }],
  noteNo: [{ required: true, whitespace: true, message: '请填写票据号码' }],
  partnerId: [{ required: true, message: '请选择往来单位' }],
  amount: [{ required: true, message: '请填写票面金额' }],
  actionDate: [{ required: true, message: '请选择日期' }],
  issueDate: [{ required: true, message: '请选择出票日期' }],
  dueDate: [{ required: true, message: '请选择到期日期' }]
}
function openRegister() {
  reg.value = {
    noteType: 'BANK', noteNo: '', electronic: true, partnerId: undefined, amount: undefined, actionDate: today(),
    issueDate: today(), dueDate: dayjs().add(6, 'month').format('YYYY-MM-DD'), acceptor: '', drawer: '', remark: '', items: []
  }
  registerOpen.value = true
}
async function submitRegister() {
  try {
    await registerRef.value.validate()
  } catch {
    return
  }
  if (!reg.value.items.length) return message.warning('请填写要核销的单据')
  saving.value = true
  try {
    await (kind.value === 'RECEIVABLE' ? receiveNote : issueNote)(reg.value)
    message.success(`${conf.value.registerLabel}成功`)
    registerOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 办理
const current = ref()
const action = ref()
const actionLabel = computed(() => Object.values(ACTIONS).flat().find((a) => a.key === action.value)?.label || '')
const endorseOpen = ref(false)
const endorse = ref({})
const settleOpen = ref(false)
const settle = ref({})
const discountRate = ref()
const discountDays = computed(() => Math.max(0, dayjs(current.value?.dueDate).diff(dayjs(settle.value.actionDate), 'day')))

function openAction(record, key) {
  current.value = record
  action.value = key
  if (key === 'endorse') {
    endorse.value = { supplierId: undefined, actionDate: today(), remark: '', items: [] }
    endorseOpen.value = true
  } else {
    settle.value = { actionDate: today(), accountId: undefined, interest: key === 'discount' ? 0 : undefined, remark: '' }
    discountRate.value = undefined
    settleOpen.value = true
  }
}
function calcInterest() {
  const amount = Number(current.value?.amount || 0)
  settle.value.interest = Math.round(((amount * Number(discountRate.value || 0)) / 100) * (discountDays.value / 360) * 100) / 100
}
async function submitEndorse() {
  if (!endorse.value.items.length) return message.warning('请填写要核销的应付单')
  saving.value = true
  try {
    await noteAction(current.value.id, 'endorse', endorse.value)
    message.success('背书已提交')
    endorseOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function submitSettle() {
  saving.value = true
  try {
    await noteAction(current.value.id, action.value, settle.value)
    message.success(`${actionLabel.value}成功，凭证已生成（未审核）`)
    settleOpen.value = false
    load()
    if (detailOpen.value) refreshDetail()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const logColumns = [
  { title: '操作', key: 'action', width: 100 },
  { title: '日期', dataIndex: 'actionDate', width: 110 },
  { title: '往来单位 / 账户', key: 'target', width: 200 },
  { title: '金额', key: 'amount', width: 150, align: 'right' },
  { title: '单据', key: 'ref', width: 230 },
  { title: '操作人', dataIndex: 'createByName', width: 90 },
  { title: '备注', dataIndex: 'remark', ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
const lastLog = computed(() => detail.value?.logs?.[detail.value.logs.length - 1])
const canUndo = computed(() => !!lastLog.value?.voucherId)
const lastActionLabel = computed(() => dict.fin_note_action?.find((d) => d.value === lastLog.value?.action)?.label || '')
const canDelete = computed(() => {
  const d = detail.value
  if (!d) return false
  const fresh = d.logs.every((l) => ['RECEIVE', 'ISSUE', 'ENDORSE'].includes(l.action))
  return fresh && (d.kind === 'RECEIVABLE' ? d.status === 'HELD' : ['ISSUED', 'CANCELLED'].includes(d.status))
})
async function openDetail(id) {
  detail.value = await getNote(id)
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) detail.value = await getNote(detail.value.id)
}
async function undo() {
  await undoNote(detail.value.id)
  message.success('已撤销')
  refreshDetail()
  load()
}
async function remove() {
  await deleteNote(detail.value.id)
  message.success('已删除')
  detailOpen.value = false
  load()
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filter {
  width: 150px;
}
.filter-wide {
  width: 240px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.sub-text.warn {
  color: var(--status-warning);
}
.sub-text.danger {
  color: var(--status-critical);
}
.discount-net {
  margin: -8px 0 16px;
}
.section-title {
  margin: 8px 0;
  font-weight: 600;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
