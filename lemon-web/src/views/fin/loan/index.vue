<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('fin.loan.keywordPlaceholder')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.fin_loan_status)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :exporter="exportAll"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'fin:loan:add'" type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>{{ $t('fin.loan.addLoan') }}
        </a-button>
        <a-alert
          type="info"
          show-icon
          :message="$t('fin.loan.tip')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1300, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'loanNo'">
            <a @click="openDetail(record)">{{ record.loanNo }}</a>
          </template>
          <template v-else-if="column.key === 'user'">
            <div>{{ record.userName }}</div>
            <div class="sub-text">{{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'repaid'">
            <div>{{ formatMoney(record.repaidAmount) }}</div>
            <div v-if="record.status === LoanStatus.PAID" class="sub-text">{{ $t('fin.loan.outstanding') }} <b class="warn">{{ formatMoney(outstanding(record)) }}</b></div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_loan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === LoanStatus.DRAFT">
                <a-button v-perm="'fin:loan:add'" type="link" size="small" @click="submit(record)">{{ $t('common.action.submit') }}</a-button>
                <span v-perm="'fin:loan:add'">
                  <a-popconfirm :title="$t('fin.loan.confirmDelete', { no: record.loanNo })" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <a-button v-if="record.status === LoanStatus.APPROVED" v-perm="'fin:loan:pay'" type="link" size="small" @click="openPay(record, 'pay')">{{ $t('fin.loan.pay') }}</a-button>
              <a-button v-if="record.status === LoanStatus.PAID" v-perm="'fin:loan:pay'" type="link" size="small" @click="openPay(record, 'repay')">{{ $t('fin.loan.repay') }}</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 -->
    <a-modal v-model:open="createOpen" :title="$t('fin.loan.addLoan')" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('fin.loan.borrower')" name="userId" :extra="$t('fin.loan.borrowerExtra')">
              <IdentityUserSelect v-model:value="form.userId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.dept')" name="deptId" :extra="$t('fin.loan.deptExtra')">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('fin.loan.loanDate')" name="loanDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('fin.loan.loanDate') }) }]">
              <a-date-picker v-model:value="form.loanDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.amount')" name="amount" :rules="[{ required: true, message: $t('common.rule.required', { name: $t('common.field.amount') }) }]">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="$t('fin.loan.purpose')" name="purpose">
          <a-textarea v-model:value="form.purpose" :rows="2" :maxlength="255" :placeholder="$t('fin.loan.purposePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 付款 / 还款 -->
    <a-modal
      v-model:open="payOpen"
      :title="$t(payMode === 'pay' ? 'fin.loan.payTitle' : 'fin.loan.repayTitle', { no: payTarget?.loanNo || '' })"
      :confirm-loading="paying"
      @ok="submitPay"
    >
      <p v-if="payTarget" class="sub-text">
        {{ $t('fin.loan.paySummary', { name: payTarget.userName, amount: formatMoney(payTarget.amount) }) }}
        <template v-if="payMode === 'repay'">{{ $t('fin.loan.repaySummary', { repaid: formatMoney(payTarget.repaidAmount), outstanding: formatMoney(outstanding(payTarget)) }) }}</template>
      </p>
      <a-form layout="vertical">
        <a-form-item :label="payMode === 'pay' ? $t('fin.loan.payAccount') : $t('fin.loan.receiveAccount')" required>
          <AccountSelect v-model:value="payForm.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.loan.accountPlaceholder')" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col v-if="payMode === 'repay'" :span="12">
            <a-form-item :label="$t('fin.loan.repayAmount')" required>
              <a-input-number v-model:value="payForm.amount" :min="0.01" :max="outstanding(payTarget || {})" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('common.field.date')" required>
              <a-date-picker v-model:value="payForm.date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="sub-text">
        {{ payMode === 'pay' ? $t('fin.loan.payVoucherHint') : $t('fin.loan.repayVoucherHint') }}
      </div>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? $t('fin.loan.detailTitle', { no: detail.loanNo }) : ''" width="min(760px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="$t('fin.loan.borrower')">{{ detail.userName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.dept')">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.loan.loanDate')">{{ detail.loanDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_loan_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.loan.repaid')">{{ $t('fin.loan.repaidDetail', { repaid: formatMoney(detail.repaidAmount), outstanding: formatMoney(outstanding(detail)) }) }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.loan.payVoucher')">{{ detail.voucherNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.loan.purpose')">{{ detail.purpose || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('fin.loan.repayRecords') }}</div>
        <a-table row-key="id" size="small" :columns="repayColumns" :data-source="detail.repays" :pagination="false" :locale="{ emptyText: $t('fin.loan.noRepay') }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'method'">
              <a-tag :color="record.method === LoanRepayMethod.CLAIM ? 'blue' : 'green'" :bordered="false">{{ record.method === LoanRepayMethod.CLAIM ? $t('fin.loan.claimOffset') : $t('fin.loan.cashRepay') }}</a-tag>
              <span v-if="record.claimNo" class="sub-text">{{ record.claimNo }}</span>
            </template>
          </template>
        </a-table>
      </template>
    </SubPage>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import { addLoan, deleteLoan, getLoan, pageLoan, payLoan, repayLoan, submitLoan } from '@/api/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { formatMoney } from '@/components/fin/fin'
import { useI18n } from 'vue-i18n'
import { useAction } from '@/composables/useAction'

import { LoanRepayMethod, LoanStatus } from '@/constants/enums'
const { t } = useI18n()
const dict = useDict('fin_loan_status')
const outstanding = (l) => Math.round((Number(l.amount || 0) - Number(l.repaidAmount || 0)) * 100) / 100

const columns = [
  { title: t('fin.loan.loanNo'), key: 'loanNo', dataIndex: 'loanNo', width: 160 },
  { title: t('fin.loan.borrower'), key: 'user', dataIndex: 'userName', width: 150 },
  { title: t('fin.loan.loanDate'), dataIndex: 'loanDate', width: 120 },
  { title: t('common.field.amount'), key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: t('fin.loan.repaid'), key: 'repaid', dataIndex: 'repaidAmount', width: 130, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'fin_loan_status' },
  { title: t('fin.loan.payVoucher'), dataIndex: 'voucherNo', width: 130 },
  { title: t('fin.loan.purpose'), dataIndex: 'purpose', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 140, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-loan')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageLoan, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

async function submit(record) {
  const flow = await submitLoan(record.id)
  message.success(flow ? t('fin.loan.submittedFlow') : t('fin.loan.submittedPay'))
  load()
}

const remove = useAction(deleteLoan, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 新增
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function openCreate() {
  form.value = { userId: undefined, deptId: undefined, loanDate: today(), amount: undefined, purpose: '' }
  createOpen.value = true
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    await addLoan({ ...f, userId: f.userId || null, deptId: f.deptId || null, purpose: f.purpose || null })
    message.success(t('fin.loan.savedSubmit'))
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 付款 / 还款
const payOpen = ref(false)
const paying = ref(false)
const payMode = ref('pay')
const payTarget = ref()
const payForm = reactive({ accountId: undefined, amount: undefined, date: undefined })
function openPay(record, mode) {
  payMode.value = mode
  payTarget.value = record
  Object.assign(payForm, { accountId: undefined, amount: mode === 'repay' ? outstanding(record) : undefined, date: today() })
  payOpen.value = true
}
async function submitPay() {
  if (!payForm.accountId) {
    message.warning(t('fin.loan.accountRequired'))
    return
  }
  if (payMode.value === 'repay' && !payForm.amount) {
    message.warning(t('fin.loan.repayAmountRequired'))
    return
  }
  paying.value = true
  try {
    if (payMode.value === 'pay') {
      await payLoan(payTarget.value.id, { accountId: payForm.accountId, date: payForm.date })
      message.success(t('fin.loan.paid'))
    } else {
      await repayLoan(payTarget.value.id, { ...payForm })
      message.success(t('fin.loan.repaidDone'))
    }
    payOpen.value = false
    load()
  } finally {
    paying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const repayColumns = [
  { title: t('common.field.date'), dataIndex: 'repayDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('fin.loan.method'), key: 'method', width: 200 },
  { title: t('fin.loan.voucher'), dataIndex: 'voucherNo', width: 110 }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getLoan(record.id)
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.warn {
  color: #fa8c16;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
