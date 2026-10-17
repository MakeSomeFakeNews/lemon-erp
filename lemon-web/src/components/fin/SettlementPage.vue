<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="conf.allPartners" class="filter" />
          <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.settlementPage.no')" allow-clear class="filter" @search="search" />
        </a-space>
        <a-space>
          <a-button v-perm="`fin:${kind}:voucher`" :disabled="!selectedKeys.length" @click="generate">
            <template #icon><FileAddOutlined /></template>{{ $t('fin.settlementPage.generateN', { n: selectedKeys.length }) }}
          </a-button>
          <a-button v-perm="`fin:${kind}:add`" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>{{ conf.newTitle }}
          </a-button>
        </a-space>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !!r.voucherId || r.status !== ApproveStatus.APPROVED }) }"
        :scroll="{ x: 1260 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'settleNo'">
            <a @click="openDetail(record)">{{ record.settleNo }}</a>
          </template>
          <template v-else-if="column.key === 'amount'">
            ¥{{ formatMoney(record.amount) }}
            <div v-if="isForeign(record.currency)" class="sub-text">{{ fcText(record.currency, record.amountFc, record.exchangeRate) }}</div>
          </template>
          <template v-else-if="column.key === 'method'"><DictTag type="fin_settle_method" :value="record.method" /></template>
          <template v-else-if="column.key === 'voucher'">
            <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
            <DictTag v-else-if="record.status && record.status !== ApproveStatus.APPROVED" type="sys_approve_status" :value="record.status" />
            <span v-else class="sub-text">{{ $t('fin.settlementPage.notGenerated') }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">{{ $t('common.action.view') }}</a-button>
              <span v-if="!record.voucherId && record.status !== ApproveStatus.APPROVING" v-perm="`fin:${kind}:remove`">
                <a-popconfirm :title="record.status === ApproveStatus.APPROVED ? $t('fin.settlementPage.confirmDeleteApproved', { no: record.settleNo }) : $t('fin.settlementPage.confirmDelete', { no: record.settleNo })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增：选择往来单位后列出未核销单据，填写本次核销金额 -->
    <SubPage :open="createOpen" :title="conf.newTitle" width="min(980px, 100vw)" destroy-on-close @close="createOpen = false">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.placeholder.selectName', { name: conf.partnerLabel }) }]">
              <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="conf.dateLabel" name="settleDate" :rules="[{ required: true, message: $t('fin.settlementPage.dateRequired') }]">
              <a-date-picker v-model:value="form.settleDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('fin.settlementPage.method')" name="method">
              <a-select v-model:value="form.method" :options="methodOptions" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('fin.settlementPage.account')" name="accountId" :rules="[{ required: true, message: $t('fin.settlementPage.accountRequired') }]">
              <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.settlementPage.accountPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col v-if="foreign" :xs="24" :sm="12">
            <a-form-item :label="$t('fin.settlementPage.rateLabel', { currency: settleCurrency })" required>
              <a-input-number v-model:value="exchangeRate" :min="0" :precision="6" class="full" :placeholder="$t('fin.settlementPage.ratePlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert
        v-if="foreign"
        type="info"
        show-icon
        class="block"
        :message="conf.foreignTip(settleCurrency)"
      />
      <a-alert
        v-else-if="currencies.length > 1"
        type="warning"
        show-icon
        class="block"
        :message="conf.multiCurrency(currencies.join(', '))"
      />
      <div class="bills-head">
        <span class="section-title">{{ conf.billsTitle }}</span>
        <a-space>
          <a-button size="small" :disabled="!bills.length" @click="fillAll">{{ $t('fin.settlementPage.fillAll') }}</a-button>
          <span v-if="foreign">
            {{ conf.foreignTotal(settleCurrency, formatMoney(total)) }} <b class="total">¥{{ formatMoney(baseTotal) }}</b>
          </span>
          <span v-else>{{ conf.thisTotal }} <b class="total">¥{{ formatMoney(total) }}</b></span>
        </a-space>
      </div>
      <a-table row-key="id" size="small" :columns="billColumns" :data-source="bills" :loading="billLoading" :pagination="false" :scroll="{ x: billScrollX }"
        :locale="{ emptyText: form.partnerId ? $t('fin.settlementPage.noBills') : $t('fin.settlementPage.selectPartnerFirst', { name: conf.partnerLabel }) }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <DictTag type="fin_bill_source" :value="record.sourceType" />
            <span class="sub-text"> {{ record.sourceNo || '' }}</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            <template v-if="isForeign(record.currency)">
              <div>{{ record.currency }} {{ formatMoney(record.amountFc) }}</div>
              <div class="sub-text">¥{{ formatMoney(record.amount) }} @{{ Number(record.exchangeRate) }}</div>
            </template>
            <template v-else>{{ formatMoney(record.amount) }}</template>
          </template>
          <template v-else-if="column.key === 'remaining'">
            {{ isForeign(record.currency) ? `${record.currency} ` : '' }}{{ formatMoney(remaining(record)) }}
          </template>
          <template v-else-if="column.key === 'settle'">
            <a-tooltip :title="blocked(record) ? $t('fin.settlementPage.blocked', { currency: settleCurrency }) : ''">
              <a-input-number
                v-model:value="record.settle"
                :precision="2"
                :min="Math.min(0, remaining(record))"
                :max="Math.max(0, remaining(record))"
                :disabled="blocked(record)"
                :prefix="isForeign(record.currency) ? currencySymbol(record.currency) : undefined"
                class="full"
              />
            </a-tooltip>
          </template>
        </template>
      </a-table>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="createOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" :disabled="total <= 0 || (foreign && !(Number(exchangeRate) > 0))" @click="submit">{{ conf.confirm }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? conf.detailTitle(detail.settleNo) : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.date')">{{ detail.settleDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item v-if="isForeign(detail.currency)" :label="$t('fin.settlementPage.fcRate')">
            {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.account')">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.method')"><DictTag type="fin_settle_method" :value="detail.method" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.voucher')">{{ detail.voucherNo || $t('fin.settlementPage.notGenerated') }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.settlementPage.preparedBy')">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'billAmount'">{{ formatMoney(record.billAmount) }}</template>
            <template v-else-if="column.key === 'amount'">
              {{ formatMoney(record.amount) }}
              <div v-if="isForeign(detail.currency) && record.amountFc != null" class="sub-text">{{ $t('fin.settlementPage.originalN', { amount: `${detail.currency} ${formatMoney(record.amountFc)}` }) }}</div>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { FileAddOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import AccountSelect from './AccountSelect.vue'
import VoucherDetail from './VoucherDetail.vue'
import { addSettlement, deleteSettlement, getSettlement, pageSettlement, settlementVoucher, unsettledBills } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { formatMoney } from './fin'
import { BASE_CURRENCY, currencySymbol, fcText, fetchRate, isForeign, useCurrencies } from './currency'
import { t } from '@/locales'
import { useAction } from '@/composables/useAction'

import { ApproveStatus } from '@/constants/enums'
/** 收款单 / 付款单，kind = receipt | payment */
const props = defineProps({
  kind: { type: String, required: true }
})

const CONF = {
  receipt: {
    partner: 'customer',
    billKind: 'ar',
    partnerLabel: t('fin.settlementPage.receipt.partner'),
    allPartners: t('fin.settlementPage.receipt.allPartners'),
    newTitle: t('fin.settlementPage.receipt.newTitle'),
    dateLabel: t('fin.settlementPage.receipt.date'),
    billsTitle: t('fin.settlementPage.receipt.billsTitle'),
    thisTotal: t('fin.settlementPage.receipt.thisTotal'),
    confirm: t('fin.settlementPage.receipt.confirm'),
    generateTip: t('fin.settlementPage.receipt.generateTip'),
    submitted: t('fin.settlementPage.receipt.submitted'),
    success: t('fin.settlementPage.receipt.success'),
    foreignTip: (currency) => t('fin.settlementPage.receipt.foreignTip', { currency }),
    multiCurrency: (currencies) => t('fin.settlementPage.receipt.multiCurrency', { currencies }),
    foreignTotal: (currency, amount) => t('fin.settlementPage.receipt.foreignTotal', { currency, amount }),
    detailTitle: (no) => t('fin.settlementPage.receipt.detailTitle', { no }),
    generateConfirm: (n) => t('fin.settlementPage.receipt.generateConfirm', { n }),
    filledTip: (currency) => t('fin.settlementPage.receipt.filledTip', { currency })
  },
  payment: {
    partner: 'supplier',
    billKind: 'ap',
    partnerLabel: t('fin.settlementPage.payment.partner'),
    allPartners: t('fin.settlementPage.payment.allPartners'),
    newTitle: t('fin.settlementPage.payment.newTitle'),
    dateLabel: t('fin.settlementPage.payment.date'),
    billsTitle: t('fin.settlementPage.payment.billsTitle'),
    thisTotal: t('fin.settlementPage.payment.thisTotal'),
    confirm: t('fin.settlementPage.payment.confirm'),
    generateTip: t('fin.settlementPage.payment.generateTip'),
    submitted: t('fin.settlementPage.payment.submitted'),
    success: t('fin.settlementPage.payment.success'),
    foreignTip: (currency) => t('fin.settlementPage.payment.foreignTip', { currency }),
    multiCurrency: (currencies) => t('fin.settlementPage.payment.multiCurrency', { currencies }),
    foreignTotal: (currency, amount) => t('fin.settlementPage.payment.foreignTotal', { currency, amount }),
    detailTitle: (no) => t('fin.settlementPage.payment.detailTitle', { no }),
    generateConfirm: (n) => t('fin.settlementPage.payment.generateConfirm', { n }),
    filledTip: (currency) => t('fin.settlementPage.payment.filledTip', { currency })
  }
}
const conf = CONF[props.kind]
const dict = useDict('fin_settle_method', 'fin_bill_source', 'sys_approve_status')
// 票据结算由票据业务（收票、背书、开票）自动生成，手工收付款时显示为禁用并说明
const methodOptions = computed(() =>
  toOptions(dict.fin_settle_method).map((o) => (o.value !== 'NOTE' ? o : { ...o, label: t('fin.settlementPage.noteMethodHint', { label: o.label }), disabled: true }))
)

const columns = [
  { title: t('fin.settlementPage.no'), key: 'settleNo', width: 170 },
  { title: conf.partnerLabel, dataIndex: 'partnerName', width: 220, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'settleDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 170, align: 'right' },
  { title: t('fin.settlementPage.account'), dataIndex: 'accountName', width: 150 },
  { title: t('fin.settlementPage.methodShort'), key: 'method', width: 110 },
  { title: t('fin.settlementPage.voucher'), key: 'voucher', width: 120 },
  { title: t('fin.settlementPage.createBy'), dataIndex: 'createByName', width: 120 },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const query = reactive({ partnerId: undefined, keyword: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(
  (p) => pageSettlement(props.kind, p),
  query
)
watch(() => query.partnerId, () => search())
onMounted(load)

function generate() {
  Modal.confirm({
    title: conf.generateConfirm(selectedKeys.value.length),
    content: conf.generateTip,
    async onOk() {
      const count = await settlementVoucher(props.kind, selectedKeys.value)
      message.success(t('fin.settlementPage.generated', { n: count }))
      selectedKeys.value = []
      load()
    }
  })
}

const remove = useAction((id) => deleteSettlement(props.kind, id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 新增
const billColumns = [
  { title: t('fin.settlementPage.no'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.settlementPage.source'), key: 'source', width: 200 },
  { title: t('common.field.date'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.settlementPage.dueDate'), dataIndex: 'dueDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 140, align: 'right' },
  { title: t('fin.settlementPage.remaining'), key: 'remaining', width: 120, align: 'right' },
  { title: t('fin.settlementPage.settle'), key: 'settle', width: 150 }
]
const billScrollX = billColumns.reduce((sum, c) => sum + c.width, 0)
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const bills = ref([])
const billLoading = ref(false)
/** 未核销余额：外币单据为原币 */
const remaining = (b) =>
  isForeign(b.currency)
    ? Math.round((Number(b.amountFc || 0) - Number(b.settledFc || 0)) * 100) / 100
    : Math.round((Number(b.amount) - Number(b.settledAmount)) * 100) / 100
/** 本次核销合计：外币为原币 */
const total = computed(() => Math.round(bills.value.reduce((s, b) => s + (Number(b.settle) || 0), 0) * 100) / 100)

// ---------------------------------------------------------------- 外币核销
useCurrencies()
const curOf = (b) => b.currency || BASE_CURRENCY
/** 未核销单据的币种 */
const currencies = computed(() => [...new Set(bills.value.map(curOf))])
/** 本次核销的币种：已填金额的单据的币种；只有一种币种的单据时即为该币种 */
const settleCurrency = computed(() => {
  const picked = bills.value.find((b) => Number(b.settle))
  if (picked) return curOf(picked)
  return currencies.value.length === 1 ? currencies.value[0] : BASE_CURRENCY
})
const foreign = computed(() => isForeign(settleCurrency.value))
/** 已选了其他币种的单据时不能再填 */
const blocked = (b) => !!bills.value.find((x) => Number(x.settle)) && curOf(b) !== settleCurrency.value
const exchangeRate = ref()
/** 实收 / 实付本位币 = 原币合计 × 汇率 */
const baseTotal = computed(() => Math.round(total.value * (Number(exchangeRate.value) || 0) * 100) / 100)

let rateSeq = 0
async function refreshRate() {
  if (!isForeign(settleCurrency.value)) return
  const seq = ++rateSeq
  const rate = await fetchRate(settleCurrency.value, form.value.settleDate)
  if (seq === rateSeq && rate != null) exchangeRate.value = rate
}
watch(settleCurrency, (c, old) => {
  if (c === old) return
  exchangeRate.value = undefined
  refreshRate()
})

function openCreate() {
  form.value = { partnerId: undefined, settleDate: today(), method: 'TRANSFER', accountId: undefined, remark: '' }
  bills.value = []
  exchangeRate.value = undefined
  createOpen.value = true
}

async function loadBills(partnerId) {
  bills.value = []
  if (!partnerId) return
  billLoading.value = true
  try {
    bills.value = (await unsettledBills(conf.billKind, partnerId)).map((b) => ({ ...b, settle: undefined }))
  } finally {
    billLoading.value = false
  }
}

/** 全部核销：只填与本次核销币种相同的单据 */
function fillAll() {
  const cur = settleCurrency.value
  bills.value.forEach((b) => {
    if (curOf(b) === cur) b.settle = remaining(b)
  })
  if (currencies.value.length > 1) message.info(conf.filledTip(cur))
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const items = bills.value.filter((b) => Number(b.settle)).map((b) => ({ billId: b.id, amount: b.settle }))
  if (new Set(bills.value.filter((b) => Number(b.settle)).map(curOf)).size > 1) {
    message.warning(t('fin.settlementPage.mixedCurrency'))
    return
  }
  if (foreign.value && !(Number(exchangeRate.value) > 0)) {
    message.warning(t('fin.settlementPage.rateRequired'))
    return
  }
  saving.value = true
  try {
    const id = await addSettlement(props.kind, {
      kind: props.kind.toUpperCase(),
      ...form.value,
      items,
      exchangeRate: foreign.value ? exchangeRate.value : null
    })
    const saved = await getSettlement(props.kind, id).catch(() => null)
    message.success(saved?.status === ApproveStatus.APPROVING ? conf.submitted : conf.success)
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: t('fin.settlementPage.settledBill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.settlementPage.sourceNo'), dataIndex: 'sourceNo', width: 170 },
  { title: t('fin.settlementPage.billDate'), dataIndex: 'billDate', width: 110 },
  { title: t('fin.settlementPage.billAmount'), key: 'billAmount', width: 130, align: 'right' },
  { title: t('fin.settlementPage.settle'), key: 'amount', width: 130, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getSettlement(props.kind, record.id)
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.filter {
  width: 240px;
}
.bills-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
</style>
