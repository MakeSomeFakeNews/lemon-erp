<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <div class="toolbar">
        <a-space wrap>
          <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="`全部${conf.partnerLabel}`" class="filter" />
          <a-input-search v-model:value="query.keyword" placeholder="单号" allow-clear class="filter" @search="search" />
        </a-space>
        <a-space>
          <a-button v-perm="`fin:${kind}:voucher`" :disabled="!selectedKeys.length" @click="generate">
            <template #icon><FileAddOutlined /></template>生成凭证（{{ selectedKeys.length }}）
          </a-button>
          <a-button v-perm="`fin:${kind}:add`" type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>新增{{ conf.label }}单
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
        :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !!r.voucherId || r.status !== 'APPROVED' }) }"
        :scroll="{ x: 1200 }"
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
            <DictTag v-else-if="record.status && record.status !== 'APPROVED'" type="sys_approve_status" :value="record.status" />
            <span v-else class="sub-text">未生成</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
              <span v-if="!record.voucherId && record.status !== 'APPROVING'" v-perm="`fin:${kind}:remove`">
                <a-popconfirm :title="record.status === 'APPROVED' ? `删除 ${record.settleNo}，恢复单据的核销金额？` : `删除 ${record.settleNo}？`" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small">删除</a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增：选择往来单位后列出未核销单据，填写本次核销金额 -->
    <SubPage :open="createOpen" :title="`新增${conf.label}单`" width="min(980px, 100vw)" destroy-on-close @close="createOpen = false">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: `请选择${conf.partnerLabel}` }]">
              <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="`${conf.label}日期`" name="settleDate" :rules="[{ required: true, message: '请选择日期' }]">
              <a-date-picker v-model:value="form.settleDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="结算方式" name="method">
              <a-select v-model:value="form.method" :options="toOptions(dict.fin_settle_method)" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="结算账户" name="accountId" :rules="[{ required: true, message: '请选择结算账户' }]">
              <AccountSelect v-model:value="form.accountId" :prefix="['1001', '1002']" placeholder="库存现金 / 银行存款" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
          <a-col v-if="foreign" :xs="24" :sm="12">
            <a-form-item :label="`汇率（1 ${settleCurrency} = ? 人民币）`" required>
              <a-input-number v-model:value="exchangeRate" :min="0" :precision="6" class="full" placeholder="未维护汇率，请填写" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <a-alert
        v-if="foreign"
        type="info"
        show-icon
        class="block"
        :message="`本次核销的是 ${settleCurrency} 外币单据，核销金额为原币。外币核销按单据记账汇率冲销往来，与实${conf.label.slice(0, 1)}本位币的差额生成凭证时计入财务费用（汇兑损益）。`"
      />
      <a-alert
        v-else-if="currencies.length > 1"
        type="warning"
        show-icon
        class="block"
        :message="`该${conf.partnerLabel}有不同币种的单据（${currencies.join('、')}），一张${conf.label}单只能核销同一币种的单据`"
      />
      <div class="bills-head">
        <span class="section-title">未核销的{{ conf.billLabel }}（红字单据为负数，可冲抵）</span>
        <a-space>
          <a-button size="small" :disabled="!bills.length" @click="fillAll">全部核销</a-button>
          <span v-if="foreign">
            原币 {{ settleCurrency }} {{ formatMoney(total) }}，实{{ conf.label.slice(0, 1) }}本位币 <b class="total">¥{{ formatMoney(baseTotal) }}</b>
          </span>
          <span v-else>本次{{ conf.label }} <b class="total">¥{{ formatMoney(total) }}</b></span>
        </a-space>
      </div>
      <a-table row-key="id" size="small" :columns="billColumns" :data-source="bills" :loading="billLoading" :pagination="false"
        :locale="{ emptyText: form.partnerId ? '没有未核销的单据' : `请先选择${conf.partnerLabel}` }">
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
            <a-tooltip :title="blocked(record) ? `已选择 ${settleCurrency} 单据，不能混合核销不同币种` : ''">
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
          <a-button @click="createOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" :disabled="total <= 0 || (foreign && !(Number(exchangeRate) > 0))" @click="submit">确认{{ conf.label }}</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `${conf.label}单 ${detail.settleNo}` : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item :label="conf.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="日期">{{ detail.settleDate }}</a-descriptions-item>
          <a-descriptions-item label="金额"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item v-if="isForeign(detail.currency)" label="原币 / 汇率">
            {{ fcText(detail.currency, detail.amountFc, detail.exchangeRate) }}
          </a-descriptions-item>
          <a-descriptions-item label="结算账户">{{ detail.accountName }}</a-descriptions-item>
          <a-descriptions-item label="结算方式"><DictTag type="fin_settle_method" :value="detail.method" /></a-descriptions-item>
          <a-descriptions-item label="凭证">{{ detail.voucherNo || '未生成' }}</a-descriptions-item>
          <a-descriptions-item label="制单">{{ detail.createByName }} {{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'billAmount'">{{ formatMoney(record.billAmount) }}</template>
            <template v-else-if="column.key === 'amount'">
              {{ formatMoney(record.amount) }}
              <div v-if="isForeign(detail.currency) && record.amountFc != null" class="sub-text">原币 {{ detail.currency }} {{ formatMoney(record.amountFc) }}</div>
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

/** 收款单 / 付款单，kind = receipt | payment */
const props = defineProps({
  kind: { type: String, required: true }
})

const CONF = {
  receipt: { label: '收款', partner: 'customer', partnerLabel: '客户', billKind: 'ar', billLabel: '应收单' },
  payment: { label: '付款', partner: 'supplier', partnerLabel: '供应商', billKind: 'ap', billLabel: '应付单' }
}
const conf = CONF[props.kind]
const dict = useDict('fin_settle_method', 'fin_bill_source', 'sys_approve_status')

const columns = [
  { title: '单号', key: 'settleNo', width: 170 },
  { title: conf.partnerLabel, dataIndex: 'partnerName', width: 220, ellipsis: true },
  { title: '日期', dataIndex: 'settleDate', width: 110 },
  { title: '金额', key: 'amount', width: 170, align: 'right' },
  { title: '结算账户', dataIndex: 'accountName', width: 130 },
  { title: '方式', key: 'method', width: 110 },
  { title: '凭证', key: 'voucher', width: 110 },
  { title: '制单人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
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
    title: `为 ${selectedKeys.value.length} 张${conf.label}单生成凭证？`,
    content: props.kind === 'receipt' ? '借 银行存款 / 贷 应收账款' : '借 应付账款 / 贷 银行存款',
    async onOk() {
      const count = await settlementVoucher(props.kind, selectedKeys.value)
      message.success(`已生成 ${count} 张凭证（未审核）`)
      selectedKeys.value = []
      load()
    }
  })
}

async function remove(id) {
  await deleteSettlement(props.kind, id)
  message.success('删除成功')
  load()
}

// ---------------------------------------------------------------- 新增
const billColumns = [
  { title: '单号', dataIndex: 'billNo', width: 150 },
  { title: '来源', key: 'source', width: 200 },
  { title: '日期', dataIndex: 'billDate', width: 100 },
  { title: '到期日', dataIndex: 'dueDate', width: 100 },
  { title: '金额', key: 'amount', width: 140, align: 'right' },
  { title: '未核销', key: 'remaining', width: 120, align: 'right' },
  { title: '本次核销', key: 'settle', width: 150 }
]
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
  if (currencies.value.length > 1) message.info(`已填写 ${cur} 单据，其他币种的单据请另开${conf.label}单`)
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const items = bills.value.filter((b) => Number(b.settle)).map((b) => ({ billId: b.id, amount: b.settle }))
  if (new Set(bills.value.filter((b) => Number(b.settle)).map(curOf)).size > 1) {
    message.warning('不能混合核销不同币种的单据')
    return
  }
  if (foreign.value && !(Number(exchangeRate.value) > 0)) {
    message.warning('请填写汇率')
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
    message.success(saved?.status === 'APPROVING' ? `${conf.label}单已提交审批，审批通过后核销` : `${conf.label}成功`)
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: '核销单据', dataIndex: 'billNo', width: 160 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 170 },
  { title: '单据日期', dataIndex: 'billDate', width: 110 },
  { title: '单据金额', key: 'billAmount', width: 130, align: 'right' },
  { title: '本次核销', key: 'amount', width: 130, align: 'right' }
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
