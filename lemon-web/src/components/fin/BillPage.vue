<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-space v-if="tab === 'bills'">
            <a-button v-perm="`fin:${kind}:voucher`" :disabled="!selectedKeys.length" @click="generate">
              <template #icon><FileAddOutlined /></template>{{ $t('fin.billPage.generateN', { n: selectedKeys.length }) }}
            </a-button>
            <a-button v-perm="`fin:${kind}:add`" type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>{{ $t('fin.billPage.manualEntry') }}
            </a-button>
          </a-space>
        </template>

        <a-tab-pane key="bills" :tab="conf.billTab">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
            <a-select v-model:value="query.status" :placeholder="$t('fin.billPage.settleStatus')" allow-clear :options="toOptions(dict.fin_bill_status)" class="filter-s" @change="search" />
            <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.billPage.keywordPlaceholder')" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.overdue" @change="search">{{ $t('fin.billPage.overdueOnly') }}</a-checkbox>
          </div>
          <div class="stats">
            <span v-if="list.some((b) => isForeign(b.currency))" class="sub-text">{{ $t('fin.billPage.foreignTip') }}</span>
            <span>{{ $t('fin.billPage.pageTotal') }} <b>¥{{ formatMoney(pageTotal.amount) }}</b></span>
            <span>{{ $t('fin.billPage.settled') }} ¥{{ formatMoney(pageTotal.settled) }}</span>
            <span>{{ $t('fin.billPage.unsettled') }} <b class="warn">¥{{ formatMoney(pageTotal.amount - pageTotal.settled) }}</b></span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !!r.voucherId || r.sourceType === 'MANUAL' }) }"
            :scroll="{ x: 1560 }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'partner'">
                <div>{{ record.partnerName }}</div>
                <div class="sub-text">{{ record.partnerCode }}</div>
              </template>
              <template v-else-if="column.key === 'source'">
                <DictTag type="fin_bill_source" :value="record.sourceType" />
                <div class="sub-text">{{ record.sourceNo || record.remark || '' }}</div>
              </template>
              <template v-else-if="column.key === 'amount'">
                <span :class="{ neg: Number(record.amount) < 0 }">¥{{ formatMoney(record.amount) }}</span>
                <div class="sub-text">{{ $t('fin.billPage.taxN', { amount: formatMoney(record.taxAmount) }) }}</div>
                <div v-if="isForeign(record.currency)" class="sub-text">{{ $t('fin.billPage.originalN', { amount: `${record.currency} ${formatMoney(record.amountFc)}` }) }}</div>
              </template>
              <template v-else-if="column.key === 'currency'">
                <template v-if="isForeign(record.currency)">
                  <div>{{ record.currency }}</div>
                  <div class="sub-text">@{{ Number(record.exchangeRate) }}</div>
                </template>
                <span v-else class="sub-text">{{ record.currency || 'CNY' }}</span>
              </template>
              <template v-else-if="column.key === 'settled'">
                <div>¥{{ formatMoney(record.settledAmount) }}</div>
                <div class="sub-text">{{ $t('fin.billPage.remainingN', { amount: formatMoney(remaining(record)) }) }}</div>
                <div v-if="isForeign(record.currency)" class="sub-text">
                  {{ $t('fin.billPage.originalSettled', { settled: formatMoney(record.settledFc), remaining: formatMoney(Number(record.amountFc || 0) - Number(record.settledFc || 0)) }) }}
                </div>
                <div v-if="Number(record.badDebtAmount)" class="sub-text neg">{{ $t('fin.billPage.badDebtN', { amount: formatMoney(record.badDebtAmount) }) }}</div>
              </template>
              <template v-else-if="column.key === 'invoiced'">
                <div>¥{{ formatMoney(record.invoicedAmount ?? 0) }}</div>
                <div v-if="Number(record.amount) > 0" class="sub-text">
                  {{ Number(record.invoicedAmount) >= Number(record.amount) ? conf.invoicedAll : conf.notInvoiced(formatMoney(Number(record.amount) - Number(record.invoicedAmount || 0))) }}
                </div>
              </template>
              <template v-else-if="column.key === 'dueDate'">
                <div>{{ record.billDate }}</div>
                <div :class="['sub-text', { neg: overdue(record) }]">
                  {{ overdue(record) ? $t('fin.billPage.overdueDays', { n: overdue(record) }) : $t('fin.billPage.dueOn', { date: record.dueDate }) }}
                </div>
              </template>
              <template v-else-if="column.key === 'status'"><DictTag type="fin_bill_status" :value="record.status" /></template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <span v-else class="sub-text">{{ record.sourceType === 'MANUAL' ? $t('fin.billPage.noVoucherOpening') : $t('fin.billPage.notGenerated') }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button
                    v-if="kind === 'ar' && Number(record.amount) > 0 && remaining(record) > 0"
                    v-perm="'fin:ar:baddebt'"
                    type="link"
                    size="small"
                    @click="openBadDebt(record)"
                  >
                    {{ $t('fin.billPage.badDebt') }}
                  </a-button>
                  <span v-if="record.sourceType === 'MANUAL' && Number(record.settledAmount) === 0" v-perm="`fin:${kind}:remove`">
                    <a-popconfirm :title="$t('fin.billPage.confirmDelete')" @confirm="remove(record.id)">
                      <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="aging" :tab="$t('fin.billPage.aging')">
          <div class="filters">
            <span>{{ $t('fin.billPage.asOf') }}</span>
            <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" @change="aging.search" />
            <span class="sub-text">{{ $t('fin.billPage.agingTip') }}</span>
          </div>
          <a-table
            row-key="partnerId"
            size="middle"
            :columns="agingColumns"
            :data-source="aging.list.value"
            :loading="aging.loading.value"
            :pagination="aging.pagination"
            @change="aging.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'partner'">{{ record.partnerName }} <span class="sub-text">{{ record.partnerCode }}</span></template>
              <template v-else-if="column.key !== 'partner'">
                <span :class="{ neg: column.key !== 'notDue' && column.key !== 'balance' && Number(record[column.key]) > 0, bold: column.key === 'balance' }">
                  {{ moneyOrBlank(record[column.key]) }}
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="createOpen" :title="conf.createTitle" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.placeholder.selectName', { name: conf.partnerLabel }) }]">
          <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.billPage.billDate')" name="billDate" :rules="[{ required: true, message: $t('fin.billPage.dateRequired') }]">
              <a-date-picker v-model:value="form.billDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.billPage.dueDate')" name="dueDate" :extra="$t('fin.billPage.dueDateExtra')">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.amount')" name="amount" :rules="[{ required: true, message: $t('fin.billPage.amountRequired') }]" :extra="$t('fin.billPage.amountExtra')">
          <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('fin.billPage.remarkPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="badDebtOpen" :title="$t('fin.billPage.badDebtTitle', { no: badDebtTarget?.billNo || '' })" :confirm-loading="badDebtSaving" @ok="submitBadDebt">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="$t('fin.billPage.badDebtTip')"
      />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.billPage.badDebtAmount')" required :extra="badDebtTarget ? $t('fin.billPage.unsettledBalanceN', { amount: formatMoney(remaining(badDebtTarget)) }) : ''">
              <a-input-number
                v-model:value="badDebtForm.amount"
                :min="0.01"
                :max="badDebtTarget ? remaining(badDebtTarget) : undefined"
                :precision="2"
                prefix="¥"
                class="full"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.billPage.badDebtDate')" required>
              <a-date-picker v-model:value="badDebtForm.date" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('fin.billPage.badDebtReason')">
          <a-textarea v-model:value="badDebtForm.reason" :rows="2" :maxlength="255" :placeholder="$t('fin.billPage.badDebtReasonPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { FileAddOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import VoucherDetail from './VoucherDetail.vue'
import { addBill, badDebt, billAging, billVoucher, deleteBill, pageBill } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { formatMoney, moneyOrBlank } from './fin'
import { isForeign } from './currency'
import { t } from '@/locales'

/** 应收管理 / 应付管理，kind = ar | ap */
const props = defineProps({
  kind: { type: String, required: true }
})

const CONF = {
  ar: {
    partner: 'customer',
    billTab: t('fin.billPage.ar.billTab'),
    partnerLabel: t('fin.billPage.ar.partner'),
    allPartners: t('fin.billPage.ar.allPartners'),
    invoiceLabel: t('fin.billPage.ar.invoiced'),
    invoicedAll: t('fin.billPage.ar.invoicedAll'),
    notInvoiced: (amount) => t('fin.billPage.ar.notInvoiced', { amount }),
    createTitle: t('fin.billPage.ar.createTitle')
  },
  ap: {
    partner: 'supplier',
    billTab: t('fin.billPage.ap.billTab'),
    partnerLabel: t('fin.billPage.ap.partner'),
    allPartners: t('fin.billPage.ap.allPartners'),
    invoiceLabel: t('fin.billPage.ap.invoiced'),
    invoicedAll: t('fin.billPage.ap.invoicedAll'),
    notInvoiced: (amount) => t('fin.billPage.ap.notInvoiced', { amount }),
    createTitle: t('fin.billPage.ap.createTitle')
  }
}
const conf = CONF[props.kind]
const dict = useDict('fin_bill_status', 'fin_bill_source')
const tab = ref('bills')

const columns = [
  { title: t('fin.billPage.billNo'), dataIndex: 'billNo', width: 160 },
  { title: conf.partnerLabel, key: 'partner', width: 200 },
  { title: t('fin.billPage.source'), key: 'source', width: 160 },
  { title: t('common.field.amount'), key: 'amount', width: 150, align: 'right' },
  { title: t('fin.billPage.currency'), key: 'currency', width: 90 },
  { title: t('fin.billPage.settled'), key: 'settled', width: 170, align: 'right' },
  { title: conf.invoiceLabel, key: 'invoiced', width: 150, align: 'right' },
  { title: t('fin.billPage.dateDue'), key: 'dueDate', width: 130 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('fin.billPage.voucher'), key: 'voucher', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: props.kind === 'ar' ? 150 : 80 }
]
const query = reactive({ partnerId: undefined, status: undefined, keyword: undefined, overdue: false })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable((p) => pageBill(props.kind, p), query)
watch(() => query.partnerId, () => search())
onMounted(load)

const pageTotal = computed(() =>
  list.value.reduce((t, b) => ({ amount: t.amount + Number(b.amount), settled: t.settled + Number(b.settledAmount) }), { amount: 0, settled: 0 })
)
const remaining = (b) => Math.round((Number(b.amount) - Number(b.settledAmount)) * 100) / 100
const overdue = (b) => (b.status === 'SETTLED' ? 0 : Math.max(dayjs().startOf('day').diff(dayjs(b.dueDate), 'day'), 0))

function generate() {
  Modal.confirm({
    title: t('fin.billPage.generateConfirm', { n: selectedKeys.value.length }),
    content: props.kind === 'ar' ? t('fin.billPage.generateArTip') : t('fin.billPage.generateApTip'),
    async onOk() {
      const count = await billVoucher(props.kind, selectedKeys.value)
      message.success(t('fin.billPage.generated', { n: count }))
      selectedKeys.value = []
      load()
    }
  })
}

// ---------------------------------------------------------------- 手工录入
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function openCreate() {
  form.value = { partnerId: undefined, billDate: today(), dueDate: undefined, amount: undefined, remark: '' }
  createOpen.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await addBill(props.kind, { kind: props.kind.toUpperCase(), ...form.value, dueDate: form.value.dueDate || null })
    message.success(t('common.msg.saveSuccess'))
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteBill(props.kind, id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

// ---------------------------------------------------------------- 坏账核销
const badDebtOpen = ref(false)
const badDebtSaving = ref(false)
const badDebtTarget = ref()
const badDebtForm = reactive({ amount: undefined, date: undefined, reason: '' })
function openBadDebt(record) {
  badDebtTarget.value = record
  Object.assign(badDebtForm, { amount: remaining(record), date: today(), reason: '' })
  badDebtOpen.value = true
}
async function submitBadDebt() {
  if (!badDebtForm.amount || badDebtForm.amount <= 0) {
    message.warning(t('fin.billPage.badDebtAmountRequired'))
    return
  }
  badDebtSaving.value = true
  try {
    const id = await badDebt(badDebtTarget.value.id, { ...badDebtForm, reason: badDebtForm.reason || null })
    message.success(t('fin.billPage.badDebtDone'))
    badDebtOpen.value = false
    load()
    if (id) openVoucher(id)
  } finally {
    badDebtSaving.value = false
  }
}

// ---------------------------------------------------------------- 账龄
const asOf = ref(today())
const agingColumns = [
  { title: conf.partnerLabel, key: 'partner', width: 240 },
  { title: t('fin.billPage.unsettledBalance'), key: 'balance', width: 150, align: 'right' },
  { title: t('fin.billPage.notDue'), key: 'notDue', width: 130, align: 'right' },
  { title: t('fin.billPage.d30'), key: 'd30', width: 150, align: 'right' },
  { title: t('fin.billPage.d60'), key: 'd60', width: 130, align: 'right' },
  { title: t('fin.billPage.d90'), key: 'd90', width: 130, align: 'right' },
  { title: t('fin.billPage.over90'), key: 'over90', width: 130, align: 'right' }
]
const aging = useTable((p) => billAging(props.kind, { ...p, asOf: asOf.value }), reactive({}))
watch(tab, (t) => t === 'aging' && aging.load())

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.filter-s {
  width: 140px;
}
.stats {
  display: flex;
  gap: 24px;
  margin-bottom: 8px;
  color: var(--app-text-2);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg,
.warn {
  color: #cf1322;
}
.bold {
  font-weight: 600;
}
.full {
  width: 100%;
}
.tip {
  margin-bottom: 16px;
}
</style>
