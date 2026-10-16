<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item label="关键字" name="keyword">
          <a-input v-model:value="query.keyword" placeholder="单号、借款人或事由" allow-clear />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="query.status" placeholder="全部" allow-clear :options="toOptions(dict.fin_loan_status)" />
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
          <template #icon><PlusOutlined /></template>新增借款
        </a-button>
        <a-alert
          type="info"
          show-icon
          message="提交后发布了 fin_loan 流程时走审批，否则直接待付款；付款生成凭证（借 其他应收款 / 贷 银行存款），报销时可以冲抵借款，也可以现金还款。"
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
            <div v-if="record.status === 'PAID'" class="sub-text">未还 <b class="warn">{{ formatMoney(outstanding(record)) }}</b></div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_loan_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <template v-if="record.status === 'DRAFT'">
                <a-button v-perm="'fin:loan:add'" type="link" size="small" @click="submit(record)">提交</a-button>
                <span v-perm="'fin:loan:add'">
                  <a-popconfirm :title="`删除借款单 ${record.loanNo}？`" @confirm="remove(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </span>
              </template>
              <a-button v-if="record.status === 'APPROVED'" v-perm="'fin:loan:pay'" type="link" size="small" @click="openPay(record, 'pay')">付款</a-button>
              <a-button v-if="record.status === 'PAID'" v-perm="'fin:loan:pay'" type="link" size="small" @click="openPay(record, 'repay')">还款</a-button>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增 -->
    <a-modal v-model:open="createOpen" title="新增借款" :confirm-loading="saving" @ok="save">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="借款人" name="userId" extra="不选为本人">
              <IdentityUserSelect v-model:value="form.userId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门" name="deptId" extra="不选取借款人的部门">
              <DeptSelect v-model:value="form.deptId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="借款日期" name="loanDate" :rules="[{ required: true, message: '请选择借款日期' }]">
              <a-date-picker v-model:value="form.loanDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="金额" name="amount" :rules="[{ required: true, message: '请填写金额' }]">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="事由" name="purpose">
          <a-textarea v-model:value="form.purpose" :rows="2" :maxlength="255" placeholder="如：出差备用金" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 付款 / 还款 -->
    <a-modal
      v-model:open="payOpen"
      :title="`${payMode === 'pay' ? '借款付款' : '现金还款'} ${payTarget?.loanNo || ''}`"
      :confirm-loading="paying"
      @ok="submitPay"
    >
      <p v-if="payTarget" class="sub-text">
        {{ payTarget.userName }} 借款 ¥{{ formatMoney(payTarget.amount) }}
        <template v-if="payMode === 'repay'">，已还 ¥{{ formatMoney(payTarget.repaidAmount) }}，未还 ¥{{ formatMoney(outstanding(payTarget)) }}</template>
      </p>
      <a-form layout="vertical">
        <a-form-item :label="payMode === 'pay' ? '付款账户' : '收款账户'" required>
          <AccountSelect v-model:value="payForm.accountId" :prefix="['1001', '1002']" placeholder="库存现金 / 银行存款" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col v-if="payMode === 'repay'" :span="12">
            <a-form-item label="还款金额" required>
              <a-input-number v-model:value="payForm.amount" :min="0.01" :max="outstanding(payTarget || {})" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="日期" required>
              <a-date-picker v-model:value="payForm.date" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="sub-text">
        {{ payMode === 'pay' ? '生成凭证：借 其他应收款 / 贷 结算账户' : '生成凭证：借 结算账户 / 贷 其他应收款；用报销冲抵的请在「费用报销」中选择该借款' }}
      </div>
    </a-modal>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `借款单 ${detail.loanNo}` : ''" width="min(760px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item label="借款人">{{ detail.userName }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ detail.deptName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="借款日期">{{ detail.loanDate }}</a-descriptions-item>
          <a-descriptions-item label="状态"><DictTag type="fin_loan_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="已还">¥{{ formatMoney(detail.repaidAmount) }}（未还 {{ formatMoney(outstanding(detail)) }}）</a-descriptions-item>
          <a-descriptions-item label="付款凭证">{{ detail.voucherNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="事由">{{ detail.purpose || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title">还款记录</div>
        <a-table row-key="id" size="small" :columns="repayColumns" :data-source="detail.repays" :pagination="false" :locale="{ emptyText: '还没有还款' }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">{{ formatMoney(record.amount) }}</template>
            <template v-else-if="column.key === 'method'">
              <a-tag :color="record.method === 'CLAIM' ? 'blue' : 'green'" :bordered="false">{{ record.method === 'CLAIM' ? '报销冲抵' : '现金还款' }}</a-tag>
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

const dict = useDict('fin_loan_status')
const outstanding = (l) => Math.round((Number(l.amount || 0) - Number(l.repaidAmount || 0)) * 100) / 100

const columns = [
  { title: '单号', key: 'loanNo', dataIndex: 'loanNo', width: 160 },
  { title: '借款人', key: 'user', dataIndex: 'userName', width: 150 },
  { title: '借款日期', dataIndex: 'loanDate', width: 110 },
  { title: '金额', key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: '已还', key: 'repaid', dataIndex: 'repaidAmount', width: 130, align: 'right' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100, dict: 'fin_loan_status' },
  { title: '付款凭证', dataIndex: 'voucherNo', width: 110 },
  { title: '事由', dataIndex: 'purpose', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
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
  message.success(flow ? '已提交审批' : '已提交，待付款')
  load()
}

async function remove(id) {
  await deleteLoan(id)
  message.success('删除成功')
  load()
}

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
    message.success('保存成功，请提交')
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
    message.warning('请选择结算账户')
    return
  }
  if (payMode.value === 'repay' && !payForm.amount) {
    message.warning('请填写还款金额')
    return
  }
  paying.value = true
  try {
    if (payMode.value === 'pay') {
      await payLoan(payTarget.value.id, { accountId: payForm.accountId, date: payForm.date })
      message.success('已付款，凭证已生成')
    } else {
      await repayLoan(payTarget.value.id, { ...payForm })
      message.success('还款成功，凭证已生成')
    }
    payOpen.value = false
    load()
  } finally {
    paying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const repayColumns = [
  { title: '日期', dataIndex: 'repayDate', width: 110 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '方式', key: 'method', width: 200 },
  { title: '凭证', dataIndex: 'voucherNo', width: 110 }
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
