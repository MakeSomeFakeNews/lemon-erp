<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-button v-perm="'fin:advance:add'" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>{{ conf.addLabel }}
          </a-button>
        </template>

        <a-tab-pane v-for="k in ['RECEIPT', 'PAYMENT']" :key="k" :tab="KINDS[k].tab">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="KINDS[k].partner" allow-clear :placeholder="KINDS[k].allPartner" class="filter" />
            <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.advance.keywordPh')" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.onlyOpen" @change="search">{{ $t('fin.advance.onlyOpen') }}</a-checkbox>
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="k === SettleKind.RECEIPT
              ? $t('fin.advance.tipReceipt')
              : $t('fin.advance.tipPayment')"
          />
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1250 }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'advanceNo'">
                <a @click="openDetail(record)">{{ record.advanceNo }}</a>
              </template>
              <template v-else-if="column.key === 'account'">
                {{ record.accountName }}
                <div><DictTag type="fin_settle_method" :value="record.method" /></div>
              </template>
              <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
              <template v-else-if="column.key === 'used'">
                <div>{{ formatMoney(record.usedAmount) }}</div>
                <div class="sub-text">
                  {{ $t('fin.advance.left') }} <b>{{ formatMoney(remaining(record)) }}</b>
                </div>
              </template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button v-if="remaining(record) > 0" v-perm="'fin:advance:add'" type="link" size="small" @click="openApply(record)">{{ $t('fin.advance.apply') }}</a-button>
                  <span v-if="!Number(record.usedAmount)" v-perm="'fin:advance:add'">
                    <a-popconfirm :title="$t('fin.advance.confirmDelete', { no: record.advanceNo })" @confirm="remove(record.id)">
                      <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 登记 -->
    <a-modal v-model:open="createOpen" :title="conf.addLabel" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: conf.partnerLabel }) }]">
          <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('common.field.date')" name="advanceDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('common.field.date') }) }]">
              <a-date-picker v-model:value="form.advanceDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.amount')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('common.field.amount') }) }]">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.advance.settleAccount')" name="accountId" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('fin.advance.settleAccount') }) }]">
              <AccountSelect v-model:value="form.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.advance.settleAccountPh')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.advance.method')" name="method">
              <a-select v-model:value="form.method" :options="toOptions(dict.fin_settle_method)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('fin.advance.remarkPh')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 冲抵 -->
    <SubPage :open="applyOpen" :title="conf.applyTitle + ' ' + (applyTarget?.advanceNo || '')" width="min(900px, 100vw)" destroy-on-close @close="applyOpen = false">
      <template v-if="applyTarget">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ applyTarget.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="conf.amountLabel">¥{{ formatMoney(applyTarget.amount) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.advance.available')"><b>¥{{ formatMoney(remaining(applyTarget)) }}</b></a-descriptions-item>
        </a-descriptions>
        <div class="bills-head">
          <a-space>
            <span>{{ $t('fin.advance.applyDate') }}</span>
            <a-date-picker v-model:value="applyDate" value-format="YYYY-MM-DD" :allow-clear="false" />
          </a-space>
          <a-space>
            <a-button size="small" :disabled="!bills.length" @click="autoFill">{{ $t('fin.advance.autoFill') }}</a-button>
            <span>{{ $t('fin.advance.thisApply') }} <b :class="['total', { warn: applyTotal > remaining(applyTarget) }]">¥{{ formatMoney(applyTotal) }}</b></span>
          </a-space>
        </div>
        <a-table row-key="id" size="small" :columns="billColumns" :data-source="bills" :loading="billLoading" :pagination="false" :scroll="{ x: billScrollX }"
          :locale="{ emptyText: conf.emptyBills }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'source'">
              <DictTag type="fin_bill_source" :value="record.sourceType" />
              <span class="sub-text"> {{ record.sourceNo || '' }}</span>
            </template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'remaining'">{{ formatMoney(billRemaining(record)) }}</template>
            <template v-else-if="column.key === 'apply'">
              <a-input-number v-model:value="record.apply" :precision="2" :min="0" :max="Math.max(0, billRemaining(record))" class="full" />
            </template>
          </template>
        </a-table>
      </template>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="applyOpen = false">{{ $t('common.action.cancel') }}</a-button>
          <a-button type="primary" :loading="applying" :disabled="applyTotal <= 0" @click="submitApply">{{ $t('fin.advance.confirmApply') }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `${KINDS[detail.kind]?.label} ${detail.advanceNo}` : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.date')">{{ detail.advanceDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.advance.used')">{{ $t('fin.advance.usedText', { used: formatMoney(detail.usedAmount), left: formatMoney(remaining(detail)) }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.advance.settleAccount')">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.advance.method')"><DictTag type="fin_settle_method" :value="detail.method" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.advance.voucher')">{{ detail.voucherNo || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title block-title">{{ $t('fin.advance.records') }}</div>
        <a-table row-key="id" size="small" :columns="applyColumns" :data-source="detail.applies" :pagination="false" :locale="{ emptyText: $t('fin.advance.noRecords') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
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
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { addAdvance, applyAdvance, deleteAdvance, getAdvance, pageAdvance, unsettledBills } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { formatMoney } from '@/components/fin/fin'
import { round2 } from '@/utils/number'
import { useAction } from '@/composables/useAction'

import { SettleKind } from '@/constants/enums'
const { t } = useI18n()
const KINDS = {
  RECEIPT: { tab: t('fin.advance.receiptTab'), label: t('fin.advance.receipt'), partner: 'customer', partnerLabel: t('fin.advance.customer'), billKind: 'ar',
    addLabel: t('fin.advance.addReceipt'), allPartner: t('fin.advance.allCustomers'), amountLabel: t('fin.advance.receiptAmount'),
    applyTitle: t('fin.advance.applyReceipt'), emptyBills: t('fin.advance.noArBills') },
  PAYMENT: { tab: t('fin.advance.paymentTab'), label: t('fin.advance.payment'), partner: 'supplier', partnerLabel: t('fin.advance.supplier'), billKind: 'ap',
    addLabel: t('fin.advance.addPayment'), allPartner: t('fin.advance.allSuppliers'), amountLabel: t('fin.advance.paymentAmount'),
    applyTitle: t('fin.advance.applyPayment'), emptyBills: t('fin.advance.noApBills') }
}
const dict = useDict('fin_settle_method', 'fin_bill_source')
const tab = ref('RECEIPT')
const conf = computed(() => KINDS[tab.value])
const remaining = (a) => round2(Number(a.amount) - Number(a.usedAmount || 0))

const columns = computed(() => [
  { title: t('fin.advance.docNo'), key: 'advanceNo', width: 160 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: t('common.field.date'), dataIndex: 'advanceDate', width: 110 },
  { title: t('fin.advance.accountMethod'), key: 'account', width: 170 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.advance.used'), key: 'used', width: 130, align: 'right' },
  { title: t('fin.advance.voucher'), key: 'voucher', width: 110 },
  { title: t('common.field.remark'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 130, fixed: 'right' }
])
const query = reactive({ kind: SettleKind.RECEIPT, partnerId: undefined, keyword: undefined, onlyOpen: false })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageAdvance, query)
onMounted(load)
watch(() => query.partnerId, () => search())
watch(tab, (v) => {
  Object.assign(query, { kind: v, partnerId: undefined, keyword: undefined })
  search()
})

const remove = useAction(deleteAdvance, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 登记
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function openCreate() {
  form.value = { kind: tab.value, partnerId: undefined, advanceDate: today(), accountId: undefined, method: 'TRANSFER', amount: undefined, remark: '' }
  createOpen.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await addAdvance({ ...form.value, remark: form.value.remark || null })
    message.success(t('fin.advance.created'))
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 冲抵
const billColumns = [
  { title: t('fin.advance.docNo'), dataIndex: 'billNo', width: 150 },
  { title: t('fin.advance.source'), key: 'source', width: 200 },
  { title: t('common.field.date'), dataIndex: 'billDate', width: 100 },
  { title: t('fin.advance.dueDate'), dataIndex: 'dueDate', width: 100 },
  { title: t('common.field.amount'), key: 'amount', width: 110, align: 'right' },
  { title: t('fin.advance.unsettled'), key: 'remaining', width: 120, align: 'right' },
  { title: t('fin.advance.thisApply'), key: 'apply', width: 150 }
]
const billScrollX = billColumns.reduce((sum, c) => sum + c.width, 0)
const applyOpen = ref(false)
const applying = ref(false)
const applyTarget = ref()
const applyDate = ref(today())
const bills = ref([])
const billLoading = ref(false)
const billRemaining = (b) => round2(Number(b.amount) - Number(b.settledAmount))
const applyTotal = computed(() => round2(bills.value.reduce((s, b) => s + (Number(b.apply) || 0), 0)))

async function openApply(record) {
  applyTarget.value = record
  applyDate.value = today()
  bills.value = []
  applyOpen.value = true
  billLoading.value = true
  try {
    // 只列出正数单据，红字单据不能用预收预付冲抵
    bills.value = (await unsettledBills(KINDS[record.kind].billKind, record.partnerId))
      .filter((b) => billRemaining(b) > 0)
      .map((b) => ({ ...b, apply: undefined }))
  } finally {
    billLoading.value = false
  }
}

function autoFill() {
  let left = remaining(applyTarget.value)
  ;[...bills.value]
    .sort((a, b) => String(a.dueDate || a.billDate).localeCompare(String(b.dueDate || b.billDate)))
    .forEach((b) => {
      const v = round2(Math.min(left, billRemaining(b)))
      b.apply = v > 0 ? v : undefined
      left = round2(left - Math.max(v, 0))
    })
}

async function submitApply() {
  if (applyTotal.value > remaining(applyTarget.value)) {
    message.warning(t('fin.advance.overApply', { amount: formatMoney(remaining(applyTarget.value)) }))
    return
  }
  const items = bills.value.filter((b) => Number(b.apply) > 0).map((b) => ({ billId: b.id, amount: b.apply }))
  applying.value = true
  try {
    await applyAdvance(applyTarget.value.id, { date: applyDate.value, items })
    message.success(t('fin.advance.applied'))
    applyOpen.value = false
    load()
  } finally {
    applying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const applyColumns = [
  { title: t('fin.advance.applyBill'), dataIndex: 'billNo', width: 160 },
  { title: t('fin.advance.applyDate'), dataIndex: 'applyDate', width: 120 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('fin.advance.voucher'), dataIndex: 'voucherNo', width: 120 }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getAdvance(record.id)
}

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
.tip {
  margin-bottom: 12px;
}
.bills-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.block-title {
  margin-bottom: 8px;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.total.warn,
.warn {
  color: #cf1322;
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
