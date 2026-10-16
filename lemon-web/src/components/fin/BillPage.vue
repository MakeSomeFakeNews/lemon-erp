<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <template #rightExtra>
          <a-space v-if="tab === 'bills'">
            <a-button v-perm="`fin:${kind}:voucher`" :disabled="!selectedKeys.length" @click="generate">
              <template #icon><FileAddOutlined /></template>生成凭证（{{ selectedKeys.length }}）
            </a-button>
            <a-button v-perm="`fin:${kind}:add`" type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>期初 / 手工录入
            </a-button>
          </a-space>
        </template>

        <a-tab-pane key="bills" :tab="`${conf.label}单`">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="conf.partner" allow-clear :placeholder="`全部${conf.partnerLabel}`" class="filter" />
            <a-select v-model:value="query.status" placeholder="核销状态" allow-clear :options="toOptions(dict.fin_bill_status)" class="filter-s" @change="search" />
            <a-input-search v-model:value="query.keyword" placeholder="单号或来源单号" allow-clear class="filter" @search="search" />
            <a-checkbox v-model:checked="query.overdue" @change="search">只看逾期</a-checkbox>
          </div>
          <div class="stats">
            <span v-if="list.some((b) => isForeign(b.currency))" class="sub-text">外币单据的金额为按记账汇率折算的人民币</span>
            <span>本页合计 <b>¥{{ formatMoney(pageTotal.amount) }}</b></span>
            <span>已核销 ¥{{ formatMoney(pageTotal.settled) }}</span>
            <span>未核销 <b class="warn">¥{{ formatMoney(pageTotal.amount - pageTotal.settled) }}</b></span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: !!r.voucherId || r.sourceType === 'MANUAL' }) }"
            :scroll="{ x: 1480 }"
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
                <div class="sub-text">税额 {{ formatMoney(record.taxAmount) }}</div>
                <div v-if="isForeign(record.currency)" class="sub-text">原币 {{ record.currency }} {{ formatMoney(record.amountFc) }}</div>
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
                <div class="sub-text">余 {{ formatMoney(remaining(record)) }}</div>
                <div v-if="isForeign(record.currency)" class="sub-text">
                  原币 {{ formatMoney(record.settledFc) }}，余 {{ formatMoney(Number(record.amountFc || 0) - Number(record.settledFc || 0)) }}
                </div>
                <div v-if="Number(record.badDebtAmount)" class="sub-text neg">坏账 {{ formatMoney(record.badDebtAmount) }}</div>
              </template>
              <template v-else-if="column.key === 'invoiced'">
                <div>¥{{ formatMoney(record.invoicedAmount ?? 0) }}</div>
                <div v-if="Number(record.amount) > 0" class="sub-text">
                  {{ Number(record.invoicedAmount) >= Number(record.amount) ? `全部${conf.invoiceLabel}` : `未${conf.invoiceLabel.slice(1)} ${formatMoney(Number(record.amount) - Number(record.invoicedAmount || 0))}` }}
                </div>
              </template>
              <template v-else-if="column.key === 'dueDate'">
                <div>{{ record.billDate }}</div>
                <div :class="['sub-text', { neg: overdue(record) }]">
                  {{ overdue(record) ? `逾期 ${overdue(record)} 天` : `到期 ${record.dueDate}` }}
                </div>
              </template>
              <template v-else-if="column.key === 'status'"><DictTag type="fin_bill_status" :value="record.status" /></template>
              <template v-else-if="column.key === 'voucher'">
                <a v-if="record.voucherId" @click="openVoucher(record.voucherId)">{{ record.voucherNo }}</a>
                <span v-else class="sub-text">{{ record.sourceType === 'MANUAL' ? '期初不生成' : '未生成' }}</span>
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
                    坏账核销
                  </a-button>
                  <span v-if="record.sourceType === 'MANUAL' && Number(record.settledAmount) === 0" v-perm="`fin:${kind}:remove`">
                    <a-popconfirm title="确定删除该单据？" @confirm="remove(record.id)">
                      <a-button type="link" danger size="small">删除</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="aging" tab="账龄分析">
          <div class="filters">
            <span>截止日期</span>
            <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" @change="aging.search" />
            <span class="sub-text">按到期日计算逾期天数，只统计未核销余额</span>
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

    <a-modal v-model:open="createOpen" :title="`录入${conf.label}单（期初余额等）`" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: `请选择${conf.partnerLabel}` }]">
          <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="单据日期" name="billDate" :rules="[{ required: true, message: '请选择日期' }]">
              <a-date-picker v-model:value="form.billDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="到期日" name="dueDate" extra="不填按账期计算">
              <a-date-picker v-model:value="form.dueDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="金额" name="amount" :rules="[{ required: true, message: '请填写金额' }]" extra="负数为红字（冲减）">
          <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" :maxlength="255" placeholder="如：2025 年末余额" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="badDebtOpen" :title="`坏账核销 ${badDebtTarget?.billNo || ''}`" :confirm-loading="badDebtSaving" @ok="submitBadDebt">
      <a-alert
        type="info"
        show-icon
        class="tip"
        message="直接转销法：确认无法收回的应收款计入已核销，生成凭证 借 营业外支出 / 贷 应收账款。业务单据须先生成应收凭证。"
      />
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="核销金额" required :extra="badDebtTarget ? `未核销余额 ${formatMoney(remaining(badDebtTarget))}` : ''">
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
            <a-form-item label="核销日期" required>
              <a-date-picker v-model:value="badDebtForm.date" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="核销原因">
          <a-textarea v-model:value="badDebtForm.reason" :rows="2" :maxlength="255" placeholder="如：客户破产清算，确认无法收回" />
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

/** 应收管理 / 应付管理，kind = ar | ap */
const props = defineProps({
  kind: { type: String, required: true }
})

const CONF = {
  ar: { label: '应收', partner: 'customer', partnerLabel: '客户', invoiceLabel: '已开票' },
  ap: { label: '应付', partner: 'supplier', partnerLabel: '供应商', invoiceLabel: '已收票' }
}
const conf = CONF[props.kind]
const dict = useDict('fin_bill_status', 'fin_bill_source')
const tab = ref('bills')

const columns = [
  { title: '单号', dataIndex: 'billNo', width: 160 },
  { title: conf.partnerLabel, key: 'partner', width: 200 },
  { title: '来源', key: 'source', width: 160 },
  { title: '金额', key: 'amount', width: 150, align: 'right' },
  { title: '币种', key: 'currency', width: 80 },
  { title: '已核销', key: 'settled', width: 160, align: 'right' },
  { title: conf.invoiceLabel, key: 'invoiced', width: 130, align: 'right' },
  { title: '日期 / 到期日', key: 'dueDate', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '凭证', key: 'voucher', width: 100 },
  { title: '操作', key: 'action', width: props.kind === 'ar' ? 130 : 70 }
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
    title: `为 ${selectedKeys.value.length} 张单据生成凭证？`,
    content: props.kind === 'ar' ? '借 应收账款 / 贷 主营业务收入、应交增值税（销项税额）' : '借 原材料 / 库存商品、应交增值税（进项税额） / 贷 应付账款',
    async onOk() {
      const count = await billVoucher(props.kind, selectedKeys.value)
      message.success(`已生成 ${count} 张凭证（未审核）`)
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
    message.success('保存成功')
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await deleteBill(props.kind, id)
  message.success('删除成功')
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
    message.warning('请填写核销金额')
    return
  }
  badDebtSaving.value = true
  try {
    const id = await badDebt(badDebtTarget.value.id, { ...badDebtForm, reason: badDebtForm.reason || null })
    message.success('坏账已核销，凭证已生成')
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
  { title: '未核销余额', key: 'balance', width: 140, align: 'right' },
  { title: '未到期', key: 'notDue', width: 130, align: 'right' },
  { title: '逾期 1-30 天', key: 'd30', width: 130, align: 'right' },
  { title: '31-60 天', key: 'd60', width: 130, align: 'right' },
  { title: '61-90 天', key: 'd90', width: 130, align: 'right' },
  { title: '90 天以上', key: 'over90', width: 130, align: 'right' }
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
