<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-button v-perm="'fin:advance:add'" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>登记{{ conf.label }}
          </a-button>
        </template>

        <a-tab-pane v-for="k in ['RECEIPT', 'PAYMENT']" :key="k" :tab="KINDS[k].tab">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="KINDS[k].partner" allow-clear :placeholder="`全部${KINDS[k].partnerLabel}`" class="filter" />
            <a-input-search v-model:value="query.keyword" placeholder="单号或备注" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.onlyOpen" @change="search">只看有余额的</a-checkbox>
          </div>
          <a-alert
            type="info"
            show-icon
            class="tip"
            :message="k === 'RECEIPT'
              ? '收到客户预付的款项登记为预收（借 银行存款 / 贷 预收账款）；开出应收单后在这里冲抵（借 预收账款 / 贷 应收账款）。'
              : '预先支付给供应商的款项登记为预付（借 预付账款 / 贷 银行存款）；收到应付单后在这里冲抵（借 应付账款 / 贷 预付账款）。'"
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
                  余 <b>{{ formatMoney(remaining(record)) }}</b>
                </div>
              </template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button v-if="remaining(record) > 0" v-perm="'fin:advance:add'" type="link" size="small" @click="openApply(record)">冲抵</a-button>
                  <span v-if="!Number(record.usedAmount)" v-perm="'fin:advance:add'">
                    <a-popconfirm :title="`删除 ${record.advanceNo}？同时删除生成的凭证（须未审核）。`" @confirm="remove(record.id)">
                      <a-button type="link" danger size="small">删除</a-button>
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
    <a-modal v-model:open="createOpen" :title="`登记${conf.label}`" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: `请选择${conf.partnerLabel}` }]">
          <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="日期" name="advanceDate" :rules="[{ required: true, message: '请选择日期' }]">
              <a-date-picker v-model:value="form.advanceDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="金额" name="amount" :rules="[{ required: true, message: '请填写金额' }]">
              <a-input-number v-model:value="form.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结算账户" name="accountId" :rules="[{ required: true, message: '请选择结算账户' }]">
              <AccountSelect v-model:value="form.accountId" :prefix="['1001', '1002']" placeholder="库存现金 / 银行存款" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结算方式" name="method">
              <a-select v-model:value="form.method" :options="toOptions(dict.fin_settle_method)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" placeholder="如：合同 HT-2026-001 定金" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 冲抵 -->
    <SubPage :open="applyOpen" :title="`冲抵${conf.label} ${applyTarget?.advanceNo || ''}`" width="min(900px, 100vw)" destroy-on-close @close="applyOpen = false">
      <template v-if="applyTarget">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ applyTarget.partnerName }}</a-descriptions-item>
          <a-descriptions-item :label="`${conf.label}金额`">¥{{ formatMoney(applyTarget.amount) }}</a-descriptions-item>
          <a-descriptions-item label="可冲抵余额"><b>¥{{ formatMoney(remaining(applyTarget)) }}</b></a-descriptions-item>
        </a-descriptions>
        <div class="bills-head">
          <a-space>
            <span>冲抵日期</span>
            <a-date-picker v-model:value="applyDate" value-format="YYYY-MM-DD" :allow-clear="false" />
          </a-space>
          <a-space>
            <a-button size="small" :disabled="!bills.length" @click="autoFill">按到期日自动分配</a-button>
            <span>本次冲抵 <b :class="['total', { warn: applyTotal > remaining(applyTarget) }]">¥{{ formatMoney(applyTotal) }}</b></span>
          </a-space>
        </div>
        <a-table row-key="id" size="small" :columns="billColumns" :data-source="bills" :loading="billLoading" :pagination="false"
          :locale="{ emptyText: `没有未核销的${conf.billLabel}` }">
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
          <a-button @click="applyOpen = false">取消</a-button>
          <a-button type="primary" :loading="applying" :disabled="applyTotal <= 0" @click="submitApply">确认冲抵</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `${KINDS[detail.kind]?.label} ${detail.advanceNo}` : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="日期">{{ detail.advanceDate }}</a-descriptions-item>
          <a-descriptions-item label="金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="已冲抵">¥{{ formatMoney(detail.usedAmount) }}（余 {{ formatMoney(remaining(detail)) }}）</a-descriptions-item>
          <a-descriptions-item label="结算账户">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item label="结算方式"><DictTag type="fin_settle_method" :value="detail.method" /></a-descriptions-item>
          <a-descriptions-item label="凭证">{{ detail.voucherNo || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title block-title">冲抵记录</div>
        <a-table row-key="id" size="small" :columns="applyColumns" :data-source="detail.applies" :pagination="false" :locale="{ emptyText: '还没有冲抵' }">
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

const KINDS = {
  RECEIPT: { tab: '预收（客户）', label: '预收', partner: 'customer', partnerLabel: '客户', billKind: 'ar', billLabel: '应收单' },
  PAYMENT: { tab: '预付（供应商）', label: '预付', partner: 'supplier', partnerLabel: '供应商', billKind: 'ap', billLabel: '应付单' }
}
const dict = useDict('fin_settle_method', 'fin_bill_source')
const tab = ref('RECEIPT')
const conf = computed(() => KINDS[tab.value])
const round2 = (n) => Math.round(n * 100) / 100
const remaining = (a) => round2(Number(a.amount) - Number(a.usedAmount || 0))

const columns = computed(() => [
  { title: '单号', key: 'advanceNo', width: 160 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: '日期', dataIndex: 'advanceDate', width: 110 },
  { title: '结算账户 / 方式', key: 'account', width: 150 },
  { title: '金额', key: 'amount', width: 130, align: 'right' },
  { title: '已冲抵', key: 'used', width: 130, align: 'right' },
  { title: '凭证', key: 'voucher', width: 110 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
])
const query = reactive({ kind: 'RECEIPT', partnerId: undefined, keyword: undefined, onlyOpen: false })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageAdvance, query)
onMounted(load)
watch(() => query.partnerId, () => search())
watch(tab, (t) => {
  Object.assign(query, { kind: t, partnerId: undefined, keyword: undefined })
  search()
})

async function remove(id) {
  await deleteAdvance(id)
  message.success('删除成功')
  load()
}

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
    message.success('登记成功，凭证已生成')
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 冲抵
const billColumns = [
  { title: '单号', dataIndex: 'billNo', width: 150 },
  { title: '来源', key: 'source', width: 200 },
  { title: '日期', dataIndex: 'billDate', width: 100 },
  { title: '到期日', dataIndex: 'dueDate', width: 100 },
  { title: '金额', key: 'amount', width: 110, align: 'right' },
  { title: '未核销', key: 'remaining', width: 110, align: 'right' },
  { title: '本次冲抵', key: 'apply', width: 150 }
]
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
    message.warning(`冲抵合计超过可冲抵余额 ¥${formatMoney(remaining(applyTarget.value))}`)
    return
  }
  const items = bills.value.filter((b) => Number(b.apply) > 0).map((b) => ({ billId: b.id, amount: b.apply }))
  applying.value = true
  try {
    await applyAdvance(applyTarget.value.id, { date: applyDate.value, items })
    message.success('冲抵成功，凭证已生成')
    applyOpen.value = false
    load()
  } finally {
    applying.value = false
  }
}

// ---------------------------------------------------------------- 详情
const applyColumns = [
  { title: '冲抵单据', dataIndex: 'billNo', width: 160 },
  { title: '冲抵日期', dataIndex: 'applyDate', width: 120 },
  { title: '金额', key: 'amount', width: 130, align: 'right' },
  { title: '凭证', dataIndex: 'voucherNo', width: 120 }
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
