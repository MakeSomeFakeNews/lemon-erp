<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="单号、报销人或事由" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.fin_claim_status)" />
        </a-form-item>
        <a-form-item name="mine">
          <a-checkbox v-model:checked="query.mine" @change="search">只看我的</a-checkbox>
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
          <template #icon><PlusOutlined /></template>新增报销
        </a-button>
        <a-alert
          type="info"
          show-icon
          message="提交时检查费用预算；发布了 fin_expense 流程时走审批，否则直接待付款。付款时有关联借款的先冲抵借款，差额再付款。"
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
              <div v-if="Number(record.loanOffset)" class="sub-text">冲抵 {{ formatMoney(record.loanOffset) }}</div>
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
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'fin:claim:add'" type="link" size="small" @click="drawer.openEdit(record)">编辑</a-button>
                <a-button v-perm="'fin:claim:add'" type="link" size="small" @click="submit(record)">提交</a-button>
                <span v-perm="'fin:claim:add'">
                  <a-popconfirm :title="`删除报销单 ${record.claimNo}？`" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <a-button v-if="record.status === 'APPROVED'" v-perm="'fin:claim:pay'" type="link" size="small" @click="openPay(record)">付款</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 / 编辑 -->
    <FormDrawer page :drawer="drawer" create-title="新增报销" edit-title="编辑报销" width="min(1080px, 100vw)">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="6">
            <a-form-item label="报销人" name="userId" extra="不选为本人">
              <IdentityUserSelect v-model:value="form.userId" :label="form.userName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="6">
            <a-form-item label="费用部门" name="deptId" extra="不选取报销人的部门">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="5">
            <a-form-item label="报销日期" name="claimDate" :rules="[{ required: true, message: '请选择报销日期' }]">
              <a-date-picker v-model:value="form.claimDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="7">
            <a-form-item label="冲抵借款" name="loanId" extra="付款时先冲抵该借款的未还金额">
              <a-select
                v-model:value="form.loanId"
                allow-clear
                placeholder="报销人未还清的借款"
                :options="loanOptions"
                :loading="loanLoading"
                :not-found-content="loanLoading ? '加载中…' : '没有未还清的借款'"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="事由" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" placeholder="如：10 月上海出差" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="items-head">
        <span class="section-title">报销明细</span>
        <a-space>
          <span>合计 <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
          <a-button size="small" @click="addItem"><template #icon><PlusOutlined /></template>添加一行</a-button>
        </a-space>
      </div>
      <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: 980 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'expenseDate'">
            <a-date-picker v-model:value="record.expenseDate" value-format="YYYY-MM-DD" class="full" />
          </template>
          <template v-else-if="column.key === 'accountId'">
            <AccountSelect v-model:value="record.accountId" :prefix="EXPENSE_PREFIX" placeholder="费用科目" />
          </template>
          <template v-else-if="column.key === 'projectId'">
            <ProjectSelect v-model:value="record.projectId" placeholder="不归集到项目" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <a-input-number v-model:value="record.amount" :min="0.01" :precision="2" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" placeholder="如：高铁票" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" :disabled="form.items.length <= 1" @click="form.items.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
    </FormDrawer>

    <!-- 付款 -->
    <a-modal v-model:open="payOpen" :title="`报销付款 ${payTarget?.claimNo || ''}`" :confirm-loading="paying" @ok="submitPay">
      <a-alert
        type="info"
        show-icon
        class="tip"
        :message="payTarget?.loanId
          ? `该报销关联借款 ${payTarget.loanNo}：先冲抵借款的未还金额（贷 其他应收款），差额再从结算账户付款；借款已还清时全额付款。`
          : '生成凭证：借 费用科目（部门、项目）/ 贷 结算账户。'"
      />
      <p v-if="payTarget">{{ payTarget.userName }} 报销 <b>¥{{ formatMoney(payTarget.amount) }}</b></p>
      <a-form layout="vertical">
        <a-form-item label="付款账户" :required="!payTarget?.loanId" :extra="payTarget?.loanId ? '借款未还金额足以冲抵全部报销额时可以不选' : ''">
          <AccountSelect v-model:value="payForm.accountId" :prefix="['1001', '1002']" placeholder="库存现金 / 银行存款" />
        </a-form-item>
        <a-form-item label="付款日期" required>
          <a-date-picker v-model:value="payForm.date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `报销单 ${detail.claimNo}` : ''" width="min(860px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item label="报销人">{{ detail.userName }}</a-descriptions-item>
          <a-descriptions-item label="费用部门">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="报销日期">{{ detail.claimDate }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="fin_claim_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="冲抵借款">
            {{ detail.loanNo || '-' }}
            <span v-if="Number(detail.loanOffset)" class="sub-text">（冲抵 {{ formatMoney(detail.loanOffset) }}）</span>
          </a-descriptions-item>
          <a-descriptions-item label="付款">{{ detail.payDate || '-' }} {{ detail.voucherNo || '' }}</a-descriptions-item>
          <a-descriptions-item label="事由">{{ detail.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">报销明细</div>
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
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
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

/** 费用科目：制造费用、销售费用、管理费用、财务费用下的末级科目 */
const EXPENSE_PREFIX = ['5101', '6601', '6602', '6603']

const userStore = useUserStore()
const dict = useDict('fin_claim_status')

const columns = [
  { title: '单号', key: 'claimNo', dataIndex: 'claimNo', width: 160 },
  { title: '报销人', key: 'user', dataIndex: 'userName', width: 150 },
  { title: '报销日期', dataIndex: 'claimDate', width: 110 },
  { title: '金额', key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: '冲抵借款', key: 'loan', dataIndex: 'loanNo', width: 150 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100, dict: 'fin_claim_status' },
  { title: '付款', key: 'pay', dataIndex: 'payDate', width: 130 },
  { title: '事由', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
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
  const text = res.flow ? '已提交审批' : '已提交，待付款'
  if (res.warnings?.length) {
    Modal.warning({
      title: `${text}，但超出预算`,
      content: h('ul', { class: 'warn-list' }, res.warnings.map((w) => h('li', w)))
    })
  } else {
    message.success(text)
  }
  load()
}

async function remove(id) {
  await deleteClaim(id)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 新增 / 编辑
let seq = 0
const emptyItem = (date) => ({ key: ++seq, expenseDate: date || today(), accountId: undefined, projectId: undefined, amount: undefined, remark: '' })
const itemColumns = [
  { title: '费用日期', key: 'expenseDate', width: 140 },
  { title: '费用科目', key: 'accountId', width: 220 },
  { title: '项目', key: 'projectId', width: 180 },
  { title: '金额', key: 'amount', width: 130 },
  { title: '说明', key: 'remark' },
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
        key: ++seq,
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
  successMessage: '保存成功，请提交'
})
const { formRef, form } = drawer
const itemTotal = computed(() => Math.round((form.value.items || []).reduce((s, i) => s + (Number(i.amount) || 0), 0) * 100) / 100)
function addItem() {
  const last = form.value.items[form.value.items.length - 1]
  form.value.items.push(emptyItem(last?.expenseDate))
}

// 报销人未还清的借款
const loans = ref([])
const loanLoading = ref(false)
const loanOptions = computed(() => {
  const opts = loans.value.map((l) => ({
    value: l.id,
    label: `${l.loanNo} ${l.loanDate} 未还 ¥${formatMoney(Number(l.amount) - Number(l.repaidAmount || 0))}`
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
    const res = await pageLoan({ current: 1, size: 100, status: 'PAID', userId })
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
    message.warning('请选择付款账户')
    return
  }
  paying.value = true
  try {
    await payClaim(payTarget.value.id, { accountId: payForm.accountId || null, date: payForm.date })
    message.success('已付款，凭证已生成')
    payOpen.value = false
    load()
  } finally {
    paying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const detailColumns = [
  { title: '费用日期', dataIndex: 'expenseDate', width: 110 },
  { title: '费用科目', key: 'account', width: 200 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '说明', dataIndex: 'remark', ellipsis: true }
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
