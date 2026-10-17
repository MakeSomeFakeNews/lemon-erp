<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('fin.claim.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.fin_claim_status)" />
        </a-form-item>
        <a-form-item name="mine">
          <a-checkbox v-model:checked="query.mine" @change="search">{{ $t('fin.claim.mine') }}</a-checkbox>
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
        <a-button v-perm="'fin:claim:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('fin.claim.add') }}
        </a-button>
        <a-alert
          type="info"
          show-icon
          :message="$t('fin.claim.hint')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'claimNo'">
            <a @click="openDetail(record)">{{ record.claimNo }}</a>
          </template>
          <template v-else-if="column.key === 'user'">
            <div>{{ record.userName }}</div>
            <div class="sub-text">{{ record.deptName || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'loan'">
            <template v-if="record.loanId">
              <div>{{ record.loanNo }}</div>
              <div v-if="Number(record.loanOffset)" class="sub-text">{{ $t('fin.claim.offsetText', { amount: formatMoney(record.loanOffset) }) }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_claim_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'pay'">
            <template v-if="record.payDate">
              <div>{{ record.payDate }}</div>
              <a v-if="record.voucherId" class="sub-link" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === ExpenseClaimStatus.DRAFT">
                <a-button v-perm="'fin:claim:add'" type="link" size="small" @click="drawer.openEdit(record)">{{ $t('common.action.edit') }}</a-button>
                <a-button v-perm="'fin:claim:add'" type="link" size="small" @click="submit(record)">{{ $t('common.action.submit') }}</a-button>
                <span v-perm="'fin:claim:add'">
                  <a-popconfirm :title="$t('fin.claim.confirmDelete', { no: record.claimNo })" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">{{ $t('common.action.delete') }}</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <a-button v-if="record.status === ExpenseClaimStatus.APPROVED" v-perm="'fin:claim:pay'" type="link" size="small" @click="openPay(record)">{{ $t('fin.claim.pay') }}</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer page :drawer="drawer" :create-title="$t('fin.claim.add')" :edit-title="$t('fin.claim.edit')" width="min(1080px, 100vw)">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('fin.claim.claimant')" name="userId" :extra="$t('fin.claim.claimantExtra')">
              <IdentityUserSelect v-model:value="form.userId" :label="form.userName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item :label="$t('fin.claim.expenseDept')" name="deptId" :extra="$t('fin.claim.expenseDeptExtra')">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="5">
            <a-form-item :label="$t('fin.claim.claimDate')" name="claimDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('fin.claim.claimDate') }) }]">
              <a-date-picker v-model:value="form.claimDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="7">
            <a-form-item :label="$t('fin.claim.offsetLoan')" name="loanId" :extra="$t('fin.claim.offsetLoanExtra')">
              <a-select
                v-model:value="form.loanId"
                allow-clear
                :placeholder="$t('fin.claim.loanPh')"
                :options="loanOptions"
                :loading="loanLoading"
                :not-found-content="loanLoading ? $t('common.msg.loading') : $t('fin.claim.noLoan')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('fin.claim.reason')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('fin.claim.reasonPh')" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="items-head">
        <span class="section-title">{{ $t('fin.claim.items') }}</span>
        <a-space>
          <span>{{ $t('common.field.total') }} <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
          <a-button size="small" @click="addItem"><template #icon><PlusOutlined /></template>{{ $t('common.action.addRow') }}</a-button>
        </a-space>
      </div>
      <LineItemsTable :items="form.items" :columns="itemColumns" :scroll-x="980" :min-rows="1">
        <template #cell="{ column, record }">
          <template v-if="column.key === 'expenseDate'">
            <a-date-picker v-model:value="record.expenseDate" value-format="YYYY-MM-DD" class="full" />
          </template>
          <template v-else-if="column.key === 'accountId'">
            <AccountSelect v-model:value="record.accountId" :prefix="EXPENSE_PREFIX" :placeholder="$t('fin.claim.expenseAccount')" />
          </template>
          <template v-else-if="column.key === 'projectId'">
            <ProjectSelect v-model:value="record.projectId" :placeholder="$t('fin.claim.noProject')" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <a-input-number v-model:value="record.amount" :min="0.01" :precision="2" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('fin.claim.itemRemarkPh')" />
          </template>
        </template>
      </LineItemsTable>
    </FormDrawer>

    <!-- 付款 -->
    <a-modal v-model:open="payOpen" :title="$t('fin.claim.payTitle', { no: payTarget?.claimNo || '' })" :confirm-loading="paying" @ok="submitPay">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="payTarget?.loanId
          ? $t('fin.claim.payTipLoan', { no: payTarget.loanNo })
          : $t('fin.claim.payTip')"
      />
      <p v-if="payTarget">{{ $t('fin.claim.payLine', { name: payTarget.userName }) }} <b>¥{{ formatMoney(payTarget.amount) }}</b></p>
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.claim.payAccount')" :required="!payTarget?.loanId" :extra="payTarget?.loanId ? $t('fin.claim.payAccountExtra') : ''">
          <AccountSelect v-model:value="payForm.accountId" :roles="['CASH', 'BANK']" :placeholder="$t('fin.claim.payAccountPh')" />
        </a-form-item>
        <a-form-item :label="$t('fin.claim.payDate')" required>
          <a-date-picker v-model:value="payForm.date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? $t('fin.claim.detailTitle', { no: detail.claimNo }) : ''" width="min(860px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="$t('fin.claim.claimant')">{{ detail.userName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.claim.expenseDept')">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.claim.claimDate')">{{ detail.claimDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_claim_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.claim.offsetLoan')">
            {{ detail.loanNo || '-' }}
            <span v-if="Number(detail.loanOffset)" class="sub-text">{{ $t('fin.claim.offsetParen', { amount: formatMoney(detail.loanOffset) }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.claim.pay')">{{ detail.payDate || '-' }} {{ detail.voucherNo || '' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.claim.reason')">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">{{ $t('fin.claim.items') }}</div>
        <a-table row-key="id" size="small" :columns="detailColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'account'">{{ record.accountCode }} {{ record.accountName }}</template>
            <template v-else-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="load" />
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import LineItemsTable from '@/components/common/LineItemsTable.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import AccountSelect from '@/components/fin/AccountSelect.vue'
import DeptSelect from '@/components/fin/DeptSelect.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import ProjectSelect from '@/components/pm/ProjectSelect.vue'
import { addClaim, deleteClaim, getClaim, pageClaim, pageLoan, payClaim, submitClaim, updateClaim } from '@/api/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { formatMoney } from '@/components/fin/fin'
import { useAction } from '@/composables/useAction'
import { rowKey, useLineItems } from '@/composables/useLineItems'

import { ExpenseClaimStatus, LoanStatus } from '@/constants/enums'
/** 费用科目：制造费用、销售费用、管理费用、财务费用下的末级科目 */
const EXPENSE_PREFIX = ['5101', '6601', '6602', '6603']

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('fin_claim_status')

const columns = [
  { title: t('fin.claim.claimNo'), key: 'claimNo', dataIndex: 'claimNo', width: 160 },
  { title: t('fin.claim.claimant'), key: 'user', dataIndex: 'userName', width: 150 },
  { title: t('fin.claim.claimDate'), dataIndex: 'claimDate', width: 120 },
  { title: t('common.field.amount'), key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: t('fin.claim.offsetLoan'), key: 'loan', dataIndex: 'loanNo', width: 150 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 120, dict: 'fin_claim_status' },
  { title: t('fin.claim.pay'), key: 'pay', dataIndex: 'payDate', width: 130 },
  { title: t('fin.claim.reason'), dataIndex: 'remark', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'fin-claim')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, mine: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageClaim, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

async function submit(record) {
  const res = await submitClaim(record.id)
  const text = res.flow ? t('fin.claim.submittedFlow') : t('fin.claim.submitted')
  if (res.warnings?.length) {
    Modal.warning({
      title: t('fin.claim.overBudget', { text }),
      content: h('ul', { class: 'warn-list' }, res.warnings.map((w) => h('li', w)))
    })
  } else {
    message.success(text)
  }
  load()
}

const remove = useAction(deleteClaim, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// ---------------------------------------------------------------- 新增 / 编辑
const emptyItem = (date) => ({ key: rowKey(), expenseDate: date || today(), accountId: undefined, projectId: undefined, amount: undefined, remark: '' })
const itemColumns = [
  { title: t('fin.claim.expenseDate'), key: 'expenseDate', width: 140 },
  { title: t('fin.claim.expenseAccount'), key: 'accountId', width: 220 },
  { title: t('fin.claim.project'), key: 'projectId', width: 180 },
  { title: t('common.field.amount'), key: 'amount', width: 130 },
  { title: t('common.field.description'), key: 'remark' },
  { title: '', key: 'action', width: 50 }
]
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, userId: undefined, userName: undefined, deptId: undefined, claimDate: today(), loanId: undefined, loanNo: undefined, remark: '', items: [emptyItem()] }),
  fetch: async (record) => {
    const c = await getClaim(record.id)
    return {
      id: c.id,
      userId: c.userId,
      userName: c.userName,
      deptId: c.deptId,
      claimDate: c.claimDate,
      loanId: c.loanId,
      loanNo: c.loanNo,
      remark: c.remark || '',
      items: (c.items || []).map((i) => ({
        key: rowKey(),
        expenseDate: i.expenseDate,
        accountId: i.accountId,
        projectId: i.projectId,
        amount: Number(i.amount),
        remark: i.remark || ''
      }))
    }
  },
  create: addClaim,
  update: updateClaim,
  toPayload: (f) => ({
    userId: f.userId || null,
    deptId: f.deptId || null,
    claimDate: f.claimDate,
    loanId: f.loanId || null,
    remark: f.remark || null,
    items: f.items
      .filter((i) => i.accountId || i.amount)
      .map((i) => ({ expenseDate: i.expenseDate || null, accountId: i.accountId, projectId: i.projectId || null, amount: i.amount, remark: i.remark || null }))
  }),
  onSuccess: load,
  successMessage: t('fin.claim.savedSubmit')
})
const { formRef, form } = drawer
const lines = useLineItems(() => form.value.items, { newRow: (items) => emptyItem(items[items.length - 1]?.expenseDate) })
const itemTotal = lines.total('amount')
const addItem = () => lines.addRow()

// 报销人未还清的借款
const loans = ref([])
const loanLoading = ref(false)
const loanOptions = computed(() => {
  const opts = loans.value.map((l) => ({
    value: l.id,
    label: t('fin.claim.loanOption', { no: l.loanNo, date: l.loanDate, amount: formatMoney(Number(l.amount) - Number(l.repaidAmount || 0)) })
  }))
  const f = form.value
  if (f.loanId && !opts.some((o) => o.value === f.loanId)) opts.unshift({ value: f.loanId, label: f.loanNo || String(f.loanId) })
  return opts
})
let loanSeq = 0
async function loadLoans() {
  const userId = form.value.userId || userStore.user?.id
  const current = ++loanSeq
  loans.value = []
  if (!userId) return
  loanLoading.value = true
  try {
    const res = await pageLoan({ current: 1, size: 100, status: LoanStatus.PAID, userId })
    if (current === loanSeq) loans.value = res.records
  } finally {
    if (current === loanSeq) loanLoading.value = false
  }
}
watch(
  () => drawer.open.value && !drawer.loading.value && (form.value.userId || 0),
  (v, old) => {
    if (v === false) return
    // 更换报销人时清除原来选择的借款
    if (old !== false && old !== undefined && v !== old) form.value.loanId = undefined
    loadLoans()
  }
)

// ---------------------------------------------------------------- 付款
const payOpen = ref(false)
const paying = ref(false)
const payTarget = ref()
const payForm = reactive({ accountId: undefined, date: undefined })
function openPay(record) {
  payTarget.value = record
  Object.assign(payForm, { accountId: undefined, date: today() })
  payOpen.value = true
}
async function submitPay() {
  if (!payForm.accountId && !payTarget.value.loanId) {
    message.warning(t('common.rule.requiredSelect', { name: t('fin.claim.payAccount') }))
    return
  }
  paying.value = true
  try {
    await payClaim(payTarget.value.id, { accountId: payForm.accountId || null, date: payForm.date })
    message.success(t('fin.claim.paid'))
    payOpen.value = false
    load()
  } finally {
    paying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const detailColumns = [
  { title: t('fin.claim.expenseDate'), dataIndex: 'expenseDate', width: 120 },
  { title: t('fin.claim.expenseAccount'), key: 'account', width: 200 },
  { title: t('common.field.amount'), key: 'amount', width: 120, align: 'right' },
  { title: t('common.field.description'), dataIndex: 'remark', ellipsis: true }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getClaim(record.id)
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.sub-link {
  font-size: 12px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.tip {
  margin-bottom: 12px;
}
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
