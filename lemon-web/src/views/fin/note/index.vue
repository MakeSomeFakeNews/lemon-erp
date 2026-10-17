<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-segmented v-model:value="kind" :options="KIND_OPTIONS" />
        <a-select v-model:value="query.status" :placeholder="$t('fin.note.allStatus')" allow-clear :options="statusOptions" class="filter" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.note.keywordPlaceholder')" allow-clear class="filter-wide" @search="search" />
        <a-checkbox v-model:checked="dueSoon">{{ $t('fin.note.dueSoon') }}</a-checkbox>
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
            <div class="sub-text">{{ record.electronic ? $t('fin.note.electronicNote') : $t('fin.note.paperNote') }}</div>
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
              <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
              <span v-if="actionsOf(record).length" v-perm="'fin:note:edit'">
                <a-dropdown>
                  <a-button type="link" size="small">{{ $t('fin.note.handle') }} <DownOutlined /></a-button>
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
            <a-form-item :label="$t('fin.note.noteType')" name="noteType">
              <a-select v-model:value="reg.noteType" :options="toOptions(dict.fin_note_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="10">
            <a-form-item :label="$t('fin.note.noteNo')" name="noteNo">
              <a-input v-model:value="reg.noteNo" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('fin.note.electronicNote')" name="electronic">
              <a-switch v-model:checked="reg.electronic" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="conf.partnerLabel" name="partnerId">
              <PartnerSelect v-model:value="reg.partnerId" :type="conf.partner" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.note.faceAmount')" name="amount">
              <a-input-number v-model:value="reg.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="conf.actionDateLabel" name="actionDate">
              <a-date-picker v-model:value="reg.actionDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('fin.note.issueDate')" name="issueDate">
              <a-date-picker v-model:value="reg.issueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('fin.note.dueDate')" name="dueDate">
              <a-date-picker v-model:value="reg.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('fin.note.acceptor')" name="acceptor">
              <a-input v-model:value="reg.acceptor" :maxlength="128" :placeholder="$t('fin.note.acceptorPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col v-if="kind === NoteKind.RECEIVABLE" :xs="24" :sm="8">
            <a-form-item :label="$t('fin.note.drawer')" name="drawer">
              <a-input v-model:value="reg.drawer" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="kind === NoteKind.RECEIVABLE ? 16 : 24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="reg.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <BillSettleTable v-model="reg.items" :kind="conf.billKind" :partner-id="reg.partnerId" :target="reg.amount" />
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="registerOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="submitRegister">{{ conf.confirmLabel }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 背书转让 -->
    <SubPage :open="endorseOpen" :title="$t('fin.note.endorseTitle', { no: current?.noteNo || '' })" width="min(900px, 100vw)" destroy-on-close @close="endorseOpen = false">
      <a-alert type="info" show-icon class="block" :message="$t('fin.note.endorseTip')" />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.note.supplier')" required>
              <PartnerSelect v-model:value="endorse.supplierId" type="supplier" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.note.endorseDate')" required>
              <a-date-picker v-model:value="endorse.actionDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.note.faceAmount')"><b>¥{{ formatMoney(current?.amount) }}</b></a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="endorse.remark" :maxlength="255" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <BillSettleTable v-model="endorse.items" kind="ap" :partner-id="endorse.supplierId" :target="Number(current?.amount)" />
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="endorseOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" :disabled="!endorse.supplierId" @click="submitEndorse">{{ $t('fin.note.confirmEndorse') }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 贴现 / 托收 / 拒付 / 兑付 -->
    <a-modal v-model:open="settleOpen" :title="`${actionLabel} - ${current?.noteNo || ''}`" :confirm-loading="saving" @ok="submitSettle">
      <a-alert type="info" show-icon class="block" :message="SETTLE_TIPS[action]" />
      <a-form layout="vertical">
        <a-form-item :label="$t('common.field.date')" required>
          <a-date-picker v-model:value="settle.actionDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item v-if="action !== 'dishonor'" :label="action === 'pay' ? $t('fin.note.payAccount') : $t('fin.note.inAccount')" required>
          <AccountSelect v-model:value="settle.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.note.bankDeposit')" />
        </a-form-item>
        <template v-if="action === 'discount'">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item :label="$t('fin.note.discountRate')" :extra="$t('fin.note.discountRateExtra')">
                <a-input-number v-model:value="discountRate" :min="0" :max="100" :precision="4" class="full" @change="calcInterest" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="$t('fin.note.interest')">
                <a-input-number v-model:value="settle.interest" :min="0" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </a-col>
          </a-row>
          <div class="sub-text discount-net">{{ $t('fin.note.discountNet', { amount: formatMoney(Number(current?.amount || 0) - Number(settle.interest || 0)), days: discountDays }) }}</div>
        </template>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="settle.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? $t('fin.note.detailTitle', { no: detail.noteNo }) : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <span v-perm="'fin:note:edit'">
          <a-space>
            <a-popconfirm v-if="canUndo" :title="$t('fin.note.confirmUndo')" @confirm="undo">
              <a-button><template #icon><UndoOutlined /></template>{{ $t('fin.note.undoAction', { action: lastActionLabel }) }}</a-button>
            </a-popconfirm>
            <a-popconfirm v-if="canDelete" :title="$t('fin.note.confirmDelete')" @confirm="remove">
              <a-button danger><template #icon><DeleteOutlined /></template>{{ $t('common.action.delete') }}</a-button>
            </a-popconfirm>
          </a-space>
        </span>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.note.noteNo')">{{ detail.noteNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.type')"><DictTag type="fin_note_type" :value="detail.noteType" /> {{ detail.electronic ? $t('fin.note.electronic') : $t('fin.note.paper') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_note_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="detail.kind === NoteKind.RECEIVABLE ? $t('fin.note.fromCustomer') : $t('fin.note.toSupplier')">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.note.faceAmount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.note.acceptor')">{{ detail.acceptor || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.note.issueDate')">{{ detail.issueDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.note.dueDate')">{{ detail.dueDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.note.drawer')">{{ detail.drawer || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('fin.note.logs') }}</div>
        <a-table row-key="id" size="small" :columns="logColumns" :data-source="detail.logs" :pagination="false" :scroll="{ x: logScrollX }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'"><DictTag type="fin_note_action" :value="record.action" /></template>
            <template v-else-if="column.key === 'target'">{{ record.partnerName || record.accountName || '-' }}</template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ formatMoney(record.amount) }}
              <div v-if="Number(record.interest)" class="sub-text">{{ $t('fin.note.interest') }} ¥{{ formatMoney(record.interest) }}</div>
            </template>
            <template v-else-if="column.key === 'ref'">
              <div v-if="record.settleNo">{{ $t('fin.note.settleNo', { no: record.settleNo }) }}</div>
              <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ $t('fin.note.voucherNo', { no: record.voucherNo }) }}</a>
              <div v-if="record.billNo" class="sub-text">{{ $t('fin.note.billBack', { no: record.billNo }) }}</div>
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
import { useI18n } from 'vue-i18n'
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

import { NoteAction, NoteKind, NoteStatus, NoteType } from '@/constants/enums'
/** 票据管理：应收票据（收票 → 背书 / 贴现 / 托收 / 拒付）、应付票据（开票 → 兑付） */
const dict = useDict('fin_note_type', 'fin_note_status', 'fin_note_action')
const { t } = useI18n()

const KIND_OPTIONS = [
  { label: t('fin.note.receivable'), value: 'RECEIVABLE' },
  { label: t('fin.note.payable'), value: 'PAYABLE' }
]
const CONF = {
  RECEIVABLE: {
    registerLabel: t('fin.note.conf.RECEIVABLE.register'), confirmLabel: t('fin.note.conf.RECEIVABLE.confirm'), successMsg: t('fin.note.conf.RECEIVABLE.success'),
    partner: 'customer', partnerLabel: t('fin.note.customer'), billKind: 'ar', actionDateLabel: t('fin.note.conf.RECEIVABLE.actionDate'),
    registerTip: t('fin.note.conf.RECEIVABLE.tip'),
    statuses: ['HELD', 'ENDORSING', 'ENDORSED', 'DISCOUNTED', 'COLLECTED', 'DISHONORED']
  },
  PAYABLE: {
    registerLabel: t('fin.note.conf.PAYABLE.register'), confirmLabel: t('fin.note.conf.PAYABLE.confirm'), successMsg: t('fin.note.conf.PAYABLE.success'),
    partner: 'supplier', partnerLabel: t('fin.note.supplier'), billKind: 'ap', actionDateLabel: t('fin.note.conf.PAYABLE.actionDate'),
    registerTip: t('fin.note.conf.PAYABLE.tip'),
    statuses: ['ISSUING', 'ISSUED', 'PAID', 'CANCELLED']
  }
}
/** 各状态可以办理的操作 */
const ACTIONS = {
  HELD: [
    { key: 'endorse', label: t('fin.note.actions.endorse') },
    { key: 'discount', label: t('fin.note.actions.discount') },
    { key: 'collect', label: t('fin.note.actions.collect') },
    { key: 'dishonor', label: t('fin.note.actions.dishonor') }
  ],
  ISSUED: [{ key: 'pay', label: t('fin.note.actions.pay') }]
}
const SETTLE_TIPS = {
  discount: t('fin.note.tips.discount'),
  collect: t('fin.note.tips.collect'),
  dishonor: t('fin.note.tips.dishonor'),
  pay: t('fin.note.tips.pay')
}

const kind = ref('RECEIVABLE')
const conf = computed(() => CONF[kind.value])
const statusOptions = computed(() =>
  conf.value.statuses.map((s) => ({ value: s, label: dict.fin_note_status?.find((d) => d.value === s)?.label || s }))
)
const dueSoon = ref(false)

const columns = [
  { title: t('fin.note.noteNo'), key: 'noteNo', width: 200 },
  { title: t('common.field.type'), key: 'noteType', width: 150 },
  { title: t('fin.note.partner'), dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: t('fin.note.faceAmount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.note.issueDate'), dataIndex: 'issueDate', width: 110 },
  { title: t('fin.note.dueDate'), key: 'dueDate', width: 130 },
  { title: t('fin.note.acceptor'), dataIndex: 'acceptor', width: 160, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 160, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-note')
const query = reactive({ kind: NoteKind.RECEIVABLE, status: undefined, keyword: undefined, dueWithin: undefined })
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

const pending = (r) => r.status === NoteStatus.HELD || r.status === NoteStatus.ISSUED
const daysLeft = (r) => dayjs(r.dueDate).diff(dayjs().startOf('day'), 'day')
const dueText = (r) =>
  daysLeft(r) < 0 ? t('fin.note.overdueDays', { n: -daysLeft(r) }) : daysLeft(r) === 0 ? t('fin.note.dueToday') : t('fin.note.dueInDays', { n: daysLeft(r) })
const dueClass = (r) => (daysLeft(r) <= 0 ? 'danger' : daysLeft(r) <= 7 ? 'warn' : '')
const actionsOf = (r) => ACTIONS[r.status] || []

// ---------------------------------------------------------------- 收票 / 开票
const registerOpen = ref(false)
const registerRef = ref()
const reg = ref({})
const saving = ref(false)
const registerRules = {
  noteType: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.note.noteType') }) }],
  noteNo: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('fin.note.noteNo') }) }],
  partnerId: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.note.partner') }) }],
  amount: [{ required: true, message: t('common.rule.required', { name: t('fin.note.faceAmount') }) }],
  actionDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('common.field.date') }) }],
  issueDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.note.issueDate') }) }],
  dueDate: [{ required: true, message: t('common.rule.requiredSelect', { name: t('fin.note.dueDate') }) }]
}
function openRegister() {
  reg.value = {
    noteType: NoteType.BANK, noteNo: '', electronic: true, partnerId: undefined, amount: undefined, actionDate: today(),
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
  if (!reg.value.items.length) return message.warning(t('fin.note.fillBills'))
  saving.value = true
  try {
    await (kind.value === NoteKind.RECEIVABLE ? receiveNote : issueNote)(reg.value)
    message.success(conf.value.successMsg)
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
  if (!endorse.value.items.length) return message.warning(t('fin.note.fillApBills'))
  saving.value = true
  try {
    await noteAction(current.value.id, 'endorse', endorse.value)
    message.success(t('fin.note.endorseSubmitted'))
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
    message.success(t('fin.note.actionDone', { action: actionLabel.value }))
    settleOpen.value = false
    load()
    if (detailOpen.value) refreshDetail()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const logColumns = [
  { title: t('common.action.operation'), key: 'action', width: 110 },
  { title: t('common.field.date'), dataIndex: 'actionDate', width: 110 },
  { title: t('fin.note.partnerOrAccount'), key: 'target', width: 200 },
  { title: t('common.field.amount'), key: 'amount', width: 150, align: 'right' },
  { title: t('fin.note.documents'), key: 'ref', width: 240 },
  { title: t('fin.note.operator'), dataIndex: 'createByName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true }
]
const logScrollX = logColumns.reduce((sum, c) => sum + c.width, 0)
const detailOpen = ref(false)
const detail = ref()
const lastLog = computed(() => detail.value?.logs?.[detail.value.logs.length - 1])
const canUndo = computed(() => !!lastLog.value?.voucherId)
const lastActionLabel = computed(() => dict.fin_note_action?.find((d) => d.value === lastLog.value?.action)?.label || '')
const canDelete = computed(() => {
  const d = detail.value
  if (!d) return false
  const fresh = d.logs.every((l) => [NoteAction.RECEIVE, NoteAction.ISSUE, NoteAction.ENDORSE].includes(l.action))
  return fresh && (d.kind === NoteKind.RECEIVABLE ? d.status === NoteStatus.HELD : [NoteStatus.ISSUED, NoteStatus.CANCELLED].includes(d.status))
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
  message.success(t('fin.note.undone'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteNote(detail.value.id)
  message.success(t('common.msg.deleted'))
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
