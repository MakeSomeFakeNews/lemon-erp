<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <template #rightExtra>
          <a-space v-if="tab !== 'report'">
            <template v-if="tab === 'INPUT'">
              <a-button v-perm="'fin:invoice:edit'" :disabled="!certifiable.length" @click="openCertify">
                <template #icon><SafetyCertificateOutlined /></template>认证（{{ certifiable.length }}）
              </a-button>
            </template>
            <a-button v-perm="'fin:invoice:edit'" type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>登记{{ conf.label }}
            </a-button>
          </a-space>
        </template>

        <a-tab-pane v-for="k in ['OUTPUT', 'INPUT']" :key="k" :tab="KINDS[k].label">
          <div class="filters">
            <PartnerSelect v-model:value="query.partnerId" :type="KINDS[k].partner" allow-clear :placeholder="`全部${KINDS[k].partnerLabel}`" class="filter" />
            <a-select v-model:value="query.status" placeholder="状态" allow-clear :options="STATUS_OPTIONS" class="filter-s" @change="search" />
            <a-input-search v-model:value="query.keyword" placeholder="发票号码或备注" allow-clear class="filter" @search="search" />
          </div>
          <div class="stats">
            <span>本页价税合计 <b>¥{{ formatMoney(pageTotal.amount) }}</b></span>
            <span>税额 ¥{{ formatMoney(pageTotal.tax) }}</span>
            <span class="sub-text">已作废的不计入</span>
          </div>
          <a-table
            row-key="id"
            size="middle"
            :columns="columns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            :row-selection="k === 'INPUT' ? { ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== 'VALID' }) } : undefined"
            :scroll="{ x: 1200 }"
            @change="onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'invoiceNo'">
                <a @click="openDetail(record)">{{ record.invoiceNo }}</a>
              </template>
              <template v-else-if="column.key === 'invoiceType'"><DictTag type="fin_invoice_type" :value="record.invoiceType" /></template>
              <template v-else-if="column.key === 'amount'">
                <span :class="{ neg: Number(record.amount) < 0, void: record.status === 'VOID' }">¥{{ formatMoney(record.amount) }}</span>
              </template>
              <template v-else-if="column.key === 'taxAmount'">
                <span :class="{ void: record.status === 'VOID' }">{{ formatMoney(record.taxAmount) }}</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ record.status === 'VOID' ? '已作废' : '有效' }}</a-tag>
              </template>
              <template v-else-if="column.key === 'certify'">
                <a-tag v-if="record.certifyPeriod" color="blue" :bordered="false">{{ periodText(record.certifyPeriod) }}</a-tag>
                <span v-else-if="record.status === 'VALID' && record.invoiceType === 'SPECIAL'" class="sub-text">未认证</span>
                <span v-else class="sub-text">-</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <span class="table-actions">
                  <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
                  <span v-if="record.status === 'VALID'" v-perm="'fin:invoice:edit'">
                    <a-popconfirm :title="`作废发票 ${record.invoiceNo}？作废后释放勾稽的单据开票金额。`" @confirm="cancel(record.id)">
                      <a-button type="link" danger size="small">作废</a-button>
                    </a-popconfirm>
                  </span>
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="report" tab="未开票 · 暂估">
          <div class="filters">
            <a-radio-group v-model:value="reportKind" button-style="solid" @change="loadReport">
              <a-radio-button value="OUTPUT">未开票收入（销项）</a-radio-button>
              <a-radio-button value="INPUT">暂估应付（进项）</a-radio-button>
            </a-radio-group>
            <span>截止日期</span>
            <a-date-picker v-model:value="asOf" value-format="YYYY-MM-DD" :allow-clear="false" @change="loadReport" />
            <span class="sub-text">
              {{ reportKind === 'OUTPUT' ? '已确认应收但尚未开具销项发票的金额' : '已入库形成应付但尚未收到进项发票的金额（月末暂估）' }}
            </span>
          </div>
          <div class="stats">
            <span>{{ reportKind === 'OUTPUT' ? '客户' : '供应商' }} {{ report.length }} 家</span>
            <span>单据金额 ¥{{ formatMoney(reportTotal.amount) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? '已开票' : '已收票' }} ¥{{ formatMoney(reportTotal.invoiced) }}</span>
            <span>{{ reportKind === 'OUTPUT' ? '未开票' : '暂估' }} <b class="neg">¥{{ formatMoney(reportTotal.uninvoiced) }}</b></span>
            <span>其中税额 ¥{{ formatMoney(reportTotal.uninvoicedTax) }}</span>
          </div>
          <a-table
            row-key="partnerId"
            size="middle"
            :columns="reportColumns"
            :data-source="report"
            :loading="reportLoading"
            :pagination="report.length > 20 ? { pageSize: 20, showTotal: (t) => `共 ${t} 条` } : false"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="['amount', 'invoiced', 'uninvoiced', 'uninvoicedTax'].includes(column.key)">
                <span :class="{ bold: column.key === 'uninvoiced' }">{{ formatMoney(record[column.key]) }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 登记发票：选择往来单位后列出未全部开票的单据，勾选并填写本次勾稽金额 -->
    <SubPage :open="createOpen" :title="`登记${conf.label}`" width="min(1000px, 100vw)" destroy-on-close @close="createOpen = false">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="conf.partnerLabel" name="partnerId" :rules="[{ required: true, message: `请选择${conf.partnerLabel}` }]">
              <PartnerSelect v-model:value="form.partnerId" :type="conf.partner" @update:value="loadBills" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item label="发票号码" name="invoiceNo" :rules="[{ required: true, whitespace: true, message: '请填写发票号码' }]">
              <a-input v-model:value="form.invoiceNo" :maxlength="32" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="发票类型" name="invoiceType">
              <a-select v-model:value="form.invoiceType" :options="toOptions(dict.fin_invoice_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="开票日期" name="invoiceDate" :rules="[{ required: true, message: '请选择开票日期' }]">
              <a-date-picker v-model:value="form.invoiceDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="价税合计" name="amount" :rules="[{ required: true, message: '请填写价税合计' }]" extra="默认为勾稽金额合计">
              <a-input-number v-model:value="form.amount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item label="税额" name="taxAmount" :rules="[{ required: true, message: '请填写税额' }]" extra="按单据税额比例计算，可修改">
              <a-input-number v-model:value="form.taxAmount" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="form.remark" :maxlength="255" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="bills-head">
        <span class="section-title">勾稽{{ conf.billLabel }}（未全部{{ conf.invoiceVerb }}的单据）</span>
        <a-space>
          <span>勾稽合计 <b class="total">¥{{ formatMoney(itemTotal) }}</b></span>
          <a-tag v-if="form.amount != null && Math.abs(itemTotal - Number(form.amount)) >= 0.005" color="red" :bordered="false">与价税合计不一致</a-tag>
        </a-space>
      </div>
      <a-table
        row-key="id"
        size="small"
        :columns="billColumns"
        :data-source="bills"
        :loading="billLoading"
        :pagination="false"
        :row-selection="{ selectedRowKeys: checkedBills, onChange: onCheckBills }"
        :scroll="{ x: 860 }"
        :locale="{ emptyText: form.partnerId ? `没有未${conf.invoiceVerb}的单据` : `请先选择${conf.partnerLabel}` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatMoney(record.amount) }}
            <div class="sub-text">税额 {{ formatMoney(record.taxAmount) }}</div>
          </template>
          <template v-else-if="column.key === 'invoiced'">{{ formatMoney(record.invoicedAmount ?? 0) }}</template>
          <template v-else-if="column.key === 'remaining'">{{ formatMoney(billRemaining(record)) }}</template>
          <template v-else-if="column.key === 'apply'">
            <a-input-number
              v-model:value="record.apply"
              :precision="2"
              :min="Math.min(0, billRemaining(record))"
              :max="Math.max(0, billRemaining(record))"
              :disabled="!checkedBills.includes(record.id)"
              class="full"
              @change="recalc"
            />
          </template>
        </template>
      </a-table>
      <template #footer>
        <div class="drawer-footer">
          <a-button @click="createOpen = false">取消</a-button>
          <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
        </div>
      </template>
    </SubPage>

    <!-- 详情 -->
    <SubPage :open="detailOpen" :title="detail ? `发票 ${detail.invoiceNo}` : ''" width="min(820px, 100vw)" @close="detailOpen = false">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2 }" size="small" class="block">
          <a-descriptions-item label="种类">{{ KINDS[detail.kind]?.label }}</a-descriptions-item>
          <a-descriptions-item label="发票类型"><DictTag type="fin_invoice_type" :value="detail.invoiceType" /></a-descriptions-item>
          <a-descriptions-item :label="KINDS[detail.kind]?.partnerLabel">{{ detail.partnerName }}</a-descriptions-item>
          <a-descriptions-item label="开票日期">{{ detail.invoiceDate }}</a-descriptions-item>
          <a-descriptions-item label="价税合计"><b>¥{{ formatMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item label="税额">¥{{ formatMoney(detail.taxAmount) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detail.status === 'VOID' ? 'default' : 'green'" :bordered="false">{{ detail.status === 'VOID' ? '已作废' : '有效' }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.kind === 'INPUT'" label="认证期间">{{ detail.certifyPeriod ? periodText(detail.certifyPeriod) : '未认证' }}</a-descriptions-item>
          <a-descriptions-item label="登记人">{{ detail.createByName || '-' }} {{ detail.createTime || '' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" label="备注">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <div class="section-title block-title">勾稽单据</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="['billAmount', 'amount'].includes(column.key)">{{ formatMoney(record[column.key]) }}</template>
          </template>
        </a-table>
      </template>
    </SubPage>

    <!-- 进项认证 -->
    <a-modal v-model:open="certifyOpen" title="进项发票认证" :confirm-loading="certifying" ok-text="认证" @ok="certify(certifyPeriod)">
      <p>已选择 {{ certifiable.length }} 张有效的进项发票，认证（勾选抵扣）后记入所选期间的进项税额抵扣。</p>
      <a-space>
        <span>认证期间</span>
        <PeriodPicker v-model:value="certifyPeriod" />
        <a-popconfirm title="取消所选发票的认证？" @confirm="certify(null)">
          <a-button :loading="certifying">取消认证</a-button>
        </a-popconfirm>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup>
import SubPage from '@/components/SubPage.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import { addInvoice, cancelInvoice, certifyInvoice, getInvoice, invoiceOpenBills, pageInvoice, uninvoiced } from '@/api/fin'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { today } from '@/components/wms/wms'
import { currentPeriod, formatMoney, periodText } from '@/components/fin/fin'

const KINDS = {
  OUTPUT: { label: '销项发票', partner: 'customer', partnerLabel: '客户', billLabel: '应收单', invoiceVerb: '开票' },
  INPUT: { label: '进项发票', partner: 'supplier', partnerLabel: '供应商', billLabel: '应付单', invoiceVerb: '收票' }
}
const STATUS_OPTIONS = [
  { value: 'VALID', label: '有效' },
  { value: 'VOID', label: '已作废' }
]
const dict = useDict('fin_invoice_type')
const tab = ref('OUTPUT')
// 登记、列表使用的种类：报表页签时沿用上一个发票页签
const kind = ref('OUTPUT')
const conf = computed(() => KINDS[kind.value])
const round2 = (n) => Math.round(n * 100) / 100

const columns = computed(() => [
  { title: '发票号码', key: 'invoiceNo', width: 160 },
  { title: '类型', key: 'invoiceType', width: 130 },
  { title: conf.value.partnerLabel, dataIndex: 'partnerName', width: 200, ellipsis: true },
  { title: '开票日期', dataIndex: 'invoiceDate', width: 110 },
  { title: '价税合计', key: 'amount', width: 130, align: 'right' },
  { title: '税额', key: 'taxAmount', width: 110, align: 'right' },
  { title: '状态', key: 'status', width: 90 },
  ...(kind.value === 'INPUT' ? [{ title: '认证期间', key: 'certify', width: 130 }] : []),
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '登记人', dataIndex: 'createByName', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
])
const query = reactive({ kind: 'OUTPUT', partnerId: undefined, status: undefined, keyword: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search, onTableChange } = useTable(pageInvoice, query)
onMounted(load)
watch(() => query.partnerId, () => search())
watch(tab, (t) => {
  if (t === 'report') {
    loadReport()
    return
  }
  kind.value = t
  Object.assign(query, { kind: t, partnerId: undefined, status: undefined, keyword: undefined })
  selectedKeys.value = []
  search()
})

const pageTotal = computed(() =>
  list.value
    .filter((r) => r.status !== 'VOID')
    .reduce((t, r) => ({ amount: t.amount + Number(r.amount), tax: t.tax + Number(r.taxAmount) }), { amount: 0, tax: 0 })
)

async function cancel(id) {
  await cancelInvoice(id)
  message.success('已作废')
  load()
}

// ---------------------------------------------------------------- 认证
const certifiable = computed(() => list.value.filter((r) => selectedKeys.value.includes(r.id) && r.status === 'VALID').map((r) => r.id))
const certifyOpen = ref(false)
const certifying = ref(false)
const certifyPeriod = ref(currentPeriod())
function openCertify() {
  certifyPeriod.value = currentPeriod()
  certifyOpen.value = true
}
async function certify(period) {
  certifying.value = true
  try {
    await certifyInvoice(certifiable.value, period)
    message.success(period ? `已认证到 ${periodText(period)}` : '已取消认证')
    certifyOpen.value = false
    selectedKeys.value = []
    load()
  } finally {
    certifying.value = false
  }
}

// ---------------------------------------------------------------- 登记
const billColumns = [
  { title: '单号', dataIndex: 'billNo', width: 150 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 160, ellipsis: true },
  { title: '日期', dataIndex: 'billDate', width: 100 },
  { title: '金额', key: 'amount', width: 120, align: 'right' },
  { title: '已勾稽', key: 'invoiced', width: 110, align: 'right' },
  { title: '未勾稽', key: 'remaining', width: 110, align: 'right' },
  { title: '本次勾稽', key: 'apply', width: 150 }
]
const createOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const bills = ref([])
const billLoading = ref(false)
const checkedBills = ref([])
const billRemaining = (b) => round2(Number(b.amount) - Number(b.invoicedAmount || 0))
const selectedBills = computed(() => bills.value.filter((b) => checkedBills.value.includes(b.id)))
const itemTotal = computed(() => round2(selectedBills.value.reduce((s, b) => s + (Number(b.apply) || 0), 0)))

function openCreate() {
  form.value = { kind: kind.value, partnerId: undefined, invoiceNo: '', invoiceType: 'SPECIAL', invoiceDate: today(), amount: undefined, taxAmount: undefined, remark: '' }
  bills.value = []
  checkedBills.value = []
  createOpen.value = true
}

async function loadBills(partnerId) {
  bills.value = []
  checkedBills.value = []
  recalc()
  if (!partnerId) return
  billLoading.value = true
  try {
    bills.value = (await invoiceOpenBills(form.value.kind, partnerId)).map((b) => ({ ...b, apply: billRemaining(b) }))
  } finally {
    billLoading.value = false
  }
}

function onCheckBills(keys) {
  // 新勾选的单据默认勾稽全部未开票金额
  bills.value.forEach((b) => {
    if (keys.includes(b.id) && !checkedBills.value.includes(b.id) && !Number(b.apply)) b.apply = billRemaining(b)
  })
  checkedBills.value = keys
  recalc()
}

/** 按勾稽金额重新计算价税合计、税额（税额 = 单据税额 × 勾稽金额 / 单据金额） */
function recalc() {
  const rows = selectedBills.value
  if (!rows.length) {
    form.value.amount = undefined
    form.value.taxAmount = undefined
    return
  }
  form.value.amount = itemTotal.value
  form.value.taxAmount = round2(
    rows.reduce((s, b) => s + (Number(b.amount) ? (Number(b.taxAmount || 0) * (Number(b.apply) || 0)) / Number(b.amount) : 0), 0)
  )
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const items = selectedBills.value.filter((b) => Number(b.apply)).map((b) => ({ billId: b.id, amount: b.apply }))
  if (!items.length) {
    message.warning('请勾选勾稽的单据并填写金额')
    return
  }
  if (Math.abs(itemTotal.value - Number(form.value.amount)) >= 0.005) {
    message.warning(`勾稽合计 ¥${formatMoney(itemTotal.value)} 与价税合计不一致`)
    return
  }
  saving.value = true
  try {
    await addInvoice({ ...form.value, invoiceNo: form.value.invoiceNo.trim(), remark: form.value.remark || null, items })
    message.success('发票已登记')
    createOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------- 详情
const itemColumns = [
  { title: '单据', dataIndex: 'billNo', width: 160 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 170 },
  { title: '单据金额', key: 'billAmount', width: 130, align: 'right' },
  { title: '勾稽金额', key: 'amount', width: 130, align: 'right' }
]
const detailOpen = ref(false)
const detail = ref()
async function openDetail(record) {
  detail.value = undefined
  detailOpen.value = true
  detail.value = await getInvoice(record.id)
}

// ---------------------------------------------------------------- 未开票 / 暂估
const reportKind = ref('OUTPUT')
const asOf = ref(today())
const report = ref([])
const reportLoading = ref(false)
const reportColumns = computed(() => [
  { title: reportKind.value === 'OUTPUT' ? '客户' : '供应商', dataIndex: 'partnerName', width: 240, ellipsis: true },
  { title: '单据数', dataIndex: 'billCount', width: 90, align: 'right' },
  { title: '单据金额', key: 'amount', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? '已开票' : '已收票', key: 'invoiced', width: 140, align: 'right' },
  { title: reportKind.value === 'OUTPUT' ? '未开票' : '暂估金额', key: 'uninvoiced', width: 140, align: 'right' },
  { title: '其中税额', key: 'uninvoicedTax', width: 130, align: 'right' }
])
const reportTotal = computed(() =>
  report.value.reduce(
    (t, r) => ({
      amount: t.amount + Number(r.amount || 0),
      invoiced: t.invoiced + Number(r.invoiced || 0),
      uninvoiced: t.uninvoiced + Number(r.uninvoiced || 0),
      uninvoicedTax: t.uninvoicedTax + Number(r.uninvoicedTax || 0)
    }),
    { amount: 0, invoiced: 0, uninvoiced: 0, uninvoicedTax: 0 }
  )
)
async function loadReport() {
  reportLoading.value = true
  try {
    report.value = await uninvoiced(reportKind.value, asOf.value)
  } finally {
    reportLoading.value = false
  }
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
  width: 130px;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 8px;
  color: var(--app-text-2);
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
.block-title {
  margin-bottom: 8px;
}
.total {
  font-size: 16px;
  color: var(--ant-color-primary, #1677ff);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.neg {
  color: #cf1322;
}
.void {
  color: var(--app-text-3);
  text-decoration: line-through;
}
.bold {
  font-weight: 600;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
</style>
