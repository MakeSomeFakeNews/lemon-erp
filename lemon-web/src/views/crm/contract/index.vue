<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('crm.common.f.contract')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('crm.contract.kwPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('crm.common.all')" allow-clear :options="toOptions(dict.crm_contract_status)" />
        </a-form-item>
        <a-form-item :label="$t('crm.common.f.customer')" name="customerId">
          <PartnerSelect v-model:value="query.customerId" type="customer" :placeholder="$t('crm.common.all')" allow-clear />
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
        <a-button v-perm="'crm:contract:add'" type="primary" @click="drawer.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('crm.contract.add') }}
        </a-button>
        <a-alert
          type="info"
          show-icon
          :message="$t('crm.contract.tip')"
        />
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1600, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub-text">{{ record.contractNo }}</div>
          </template>
          <template v-else-if="column.key === 'source'">
            <div>{{ record.opportunityName || '-' }}</div>
            <div v-if="record.quoteNo" class="sub-text">{{ $t('crm.contract.quoteText', { no: record.quoteNo }) }}</div>
          </template>
          <template v-else-if="column.key === 'term'">
            {{ record.startDate || '-' }} ~ {{ record.endDate || '-' }}
            <div class="sub-text">{{ $t('crm.contract.signText', { date: record.signDate }) }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.amount) }}</template>
          <template v-else-if="column.key === 'received'">
            <div>¥{{ formatMoney(record.receivedAmount || 0) }}</div>
            <a-progress :percent="percentOf(record)" size="small" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'orderNo'">
            <a v-if="record.orderId" @click="router.push({ path: '/scm/sales', query: { id: record.orderId } })">{{ record.orderNo }}</a>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="crm_contract_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <span class="table-actions">
              <a-button type="link" size="small" @click="openDetail(record)">
                <template #icon><EyeOutlined /></template>{{ $t('common.action.detail') }}
              </a-button>
              <a-button v-if="record.status === 'DRAFT'" v-perm="'crm:contract:edit'" type="link" size="small" @click="drawer.openEdit(record)">
                <template #icon><EditOutlined /></template>{{ $t('common.action.edit') }}
              </a-button>
              <span v-if="record.status === 'DRAFT'" v-perm="'crm:contract:remove'">
                <a-popconfirm :title="$t('crm.contract.confirmDelete', { name: record.name })" @confirm="remove(record.id)">
                  <a-button type="link" danger size="small"><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer page :drawer="drawer" :create-title="$t('crm.contract.add')" :edit-title="$t('crm.contract.editTitle')" width="min(900px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.contractName')" name="name"><a-input v-model:value="form.name" :maxlength="128" /></a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.common.f.customer')" name="customerId">
              <PartnerSelect v-model:value="form.customerId" type="customer" :label="form.customerName" @update:value="onCustomer" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.contract.opportunity')" name="opportunityId">
              <a-select
                v-model:value="form.opportunityId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="oppOptions"
                :placeholder="form.customerId ? $t('crm.common.optional') : $t('crm.contract.contactPh')"
                @change="onOpportunity"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('crm.contract.quotation')" name="quotationId" :extra="$t('crm.contract.quotationExtra')">
              <a-select
                v-model:value="form.quotationId"
                allow-clear
                show-search
                option-filter-prop="label"
                :options="quoteOptions"
                :placeholder="form.customerId ? $t('crm.common.optional') : $t('crm.contract.contactPh')"
                @change="onQuotation"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('crm.common.f.signDate')" name="signDate">
              <a-date-picker v-model:value="form.signDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.beginDate')" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('common.field.endDate')" name="endDate">
              <a-date-picker
                v-model:value="form.endDate"
                value-format="YYYY-MM-DD"
                class="full"
                :disabled-date="(d) => form.startDate && d.isBefore(dayjs(form.startDate))"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('crm.contract.amountTax')" name="amount">
              <a-input-number v-model:value="form.amount" :min="0" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-form-item :label="$t('crm.common.f.owner')" name="ownerId" :extra="$t('crm.common.ownerExtra')">
              <IdentityUserSelect v-model:value="form.ownerId" :label="form.ownerName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('crm.contractDetail.terms')" name="remark">
              <a-textarea v-model:value="form.remark" :rows="2" :maxlength="500" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="plans-head">
          <span>{{ $t('crm.contract.plans') }}</span>
          <span class="sub-text">
            {{ $t('crm.contract.planTotal', { amount: formatMoney(planTotal) }) }}
            <template v-if="form.amount != null">{{ $t('crm.contract.unplanned', { amount: formatMoney(Math.max(Number(form.amount) - planTotal, 0)) }) }}</template>
          </span>
          <a-button size="small" @click="addPlan"><template #icon><PlusOutlined /></template>{{ $t('crm.contract.addPlan') }}</a-button>
        </div>
        <a-table row-key="key" size="small" :columns="planColumns" :data-source="form.plans" :pagination="false" :scroll="{ x: 640 }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'seq'">{{ $t('crm.contractDetail.seqText', { seq: index + 1 }) }}</template>
            <template v-else-if="column.key === 'planDate'">
              <a-form-item :name="['plans', index, 'planDate']" :rules="[{ required: true, message: $t('crm.contract.dateRequired') }]" class="cell">
                <a-date-picker v-model:value="record.planDate" value-format="YYYY-MM-DD" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'amount'">
              <a-form-item :name="['plans', index, 'amount']" :rules="[{ required: true, message: $t('crm.contract.amountRequired') }]" class="cell">
                <a-input-number v-model:value="record.amount" :min="0.01" :precision="2" prefix="¥" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-form-item class="cell">
                <a-input v-model:value="record.remark" :maxlength="255" :placeholder="$t('crm.contract.planRemarkPh')" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.plans.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
        <a-form-item name="plans" :rules="[{ validator: validatePlans }]" class="cell" />
      </a-form>
    </FormDrawer>

    <ContractDetail ref="detailRef" v-model:open="detailOpen" :contract-id="detailId" @changed="load" @edit="onEditFromDetail" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import ContractDetail from '@/components/crm/ContractDetail.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { formatMoney } from '@/components/scm/scm'
import {
  addCrmContract,
  deleteCrmContract,
  getCrmContract,
  getCrmCustomer,
  getOpportunity,
  pageCrmContract,
  pageOpportunity,
  updateCrmContract
} from '@/api/crm'
import { pageQuotation } from '@/api/scm/sourcing'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

const dict = useDict('crm_contract_status', 'scm_quotation_status')
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const percentOf = (r) => (Number(r.amount) ? Math.min(Math.round(((Number(r.receivedAmount) || 0) / Number(r.amount)) * 100), 100) : 0)

const columns = [
  { title: t('crm.common.f.contract'), key: 'name', width: 220, fixed: 'left', exportValue: (r) => `${r.contractNo} ${r.name}` },
  { title: t('crm.common.f.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('crm.contract.col.source'), key: 'source', width: 210, exportValue: (r) => [r.opportunityName, r.quoteNo].filter(Boolean).join(' / ') },
  { title: t('crm.contractDetail.term'), key: 'term', width: 210, exportValue: (r) => `${r.startDate || ''} ~ ${r.endDate || ''}` },
  { title: t('crm.common.f.contractAmount'), key: 'amount', dataIndex: 'amount', width: 130, align: 'right' },
  { title: t('crm.common.f.received'), key: 'received', dataIndex: 'receivedAmount', width: 150, align: 'right' },
  { title: t('crm.common.f.salesOrder'), key: 'orderNo', dataIndex: 'orderNo', width: 160 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'crm_contract_status' },
  { title: t('crm.common.f.owner'), dataIndex: 'ownerName', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 190, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'crm-contract')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined, customerId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCrmContract, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

// 详情
const detailRef = ref()
const detailOpen = ref(false)
const detailId = ref()
function openDetail(record) {
  detailId.value = record.id
  detailOpen.value = true
}
function onEditFromDetail(record) {
  drawer.openEdit(record)
}

// 新增 / 编辑
let planSeq = 0
const planRow = (p = {}) => ({ key: ++planSeq, planDate: p.planDate, amount: p.amount == null ? undefined : Number(p.amount), remark: p.remark })
const planColumns = [
  { title: t('crm.contractDetail.col.seq'), key: 'seq', width: 110 },
  { title: t('crm.contract.col.planDate'), key: 'planDate', width: 190 },
  { title: t('common.field.amount'), key: 'amount', width: 170 },
  { title: t('common.field.description'), key: 'remark' },
  { title: '', key: 'action', width: 50 }
]
const rules = {
  name: [{ required: true, whitespace: true, message: t('crm.contract.nameRequired') }],
  customerId: [{ required: true, message: t('crm.common.customerRequired') }],
  signDate: [{ required: true, message: t('crm.contract.signRequired') }],
  amount: [{ required: true, message: t('crm.contract.contractAmountRequired') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: undefined,
    customerId: undefined,
    customerName: undefined,
    opportunityId: undefined,
    opportunityName: undefined,
    quotationId: undefined,
    quoteNo: undefined,
    signDate: dayjs().format('YYYY-MM-DD'),
    startDate: dayjs().format('YYYY-MM-DD'),
    endDate: undefined,
    amount: undefined,
    ownerId: undefined,
    ownerName: undefined,
    remark: undefined,
    plans: []
  }),
  beforeOpen: (record) => loadRelated(record ? record.customerId : form.value.customerId),
  fetch: async (record) => {
    const d = await getCrmContract(record.id)
    return { ...d, plans: (d.plans || []).map(planRow) }
  },
  create: addCrmContract,
  update: updateCrmContract,
  toPayload: (f) => ({
    name: f.name,
    customerId: f.customerId,
    opportunityId: f.opportunityId || null,
    quotationId: f.quotationId || null,
    signDate: f.signDate,
    startDate: f.startDate || null,
    endDate: f.endDate || null,
    amount: f.amount,
    ownerId: f.ownerId || null,
    remark: f.remark || null,
    plans: f.plans.map((p) => ({ planDate: p.planDate, amount: p.amount, remark: p.remark || null }))
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value) detailRef.value?.reload()
  }
})
const { formRef, form } = drawer

const planTotal = computed(() => form.value.plans.reduce((s, p) => s + (Number(p.amount) || 0), 0))
function validatePlans() {
  const amount = Number(form.value.amount) || 0
  return Math.round(planTotal.value * 100) > Math.round(amount * 100)
    ? Promise.reject(new Error(t('crm.contract.planExceed', { amount: formatMoney(planTotal.value) })))
    : Promise.resolve()
}
function addPlan() {
  const remaining = Math.max((Number(form.value.amount) || 0) - planTotal.value, 0)
  const last = form.value.plans[form.value.plans.length - 1]?.planDate
  form.value.plans.push(
    planRow({
      planDate: last ? dayjs(last).add(1, 'month').format('YYYY-MM-DD') : form.value.signDate,
      amount: remaining ? Math.round(remaining * 100) / 100 : undefined
    })
  )
}

// 客户的商机、报价单
const opps = ref([])
const quotes = ref([])
const quoteStatus = (v) => dict.scm_quotation_status.find((d) => d.value === v)?.label || v
const oppOptions = computed(() => {
  const opts = opps.value.map((o) => ({ value: o.id, label: `${o.oppNo} ${o.name}` }))
  const f = form.value
  if (f.opportunityId && !opts.some((o) => o.value === f.opportunityId)) opts.unshift({ value: f.opportunityId, label: f.opportunityName || t('crm.contract.oppLabel', { id: f.opportunityId }) })
  return opts
})
const quoteOptions = computed(() => {
  const opts = quotes.value.map((q) => ({ value: q.id, label: `${q.quoteNo}　¥${formatMoney(q.totalAmount)}　${quoteStatus(q.status)}` }))
  const f = form.value
  if (f.quotationId && !opts.some((o) => o.value === f.quotationId)) opts.unshift({ value: f.quotationId, label: f.quoteNo || t('crm.contract.quoteLabel', { id: f.quotationId }) })
  return opts
})
let loadedFor
async function loadRelated(customerId) {
  if (customerId && customerId === loadedFor) return
  loadedFor = customerId
  opps.value = []
  quotes.value = []
  if (!customerId) return
  const [o, q] = await Promise.all([
    userStore.hasPerm('crm:opp:list') ? pageOpportunity({ customerId, current: 1, size: 100 }).catch(() => ({ records: [] })) : { records: [] },
    userStore.hasPerm('scm:quotation:list') ? pageQuotation({ customerId, current: 1, size: 100 }).catch(() => ({ records: [] })) : { records: [] }
  ])
  opps.value = o.records
  quotes.value = q.records.filter((r) => r.status !== 'LOST')
}
function onCustomer(customerId) {
  Object.assign(form.value, { opportunityId: undefined, opportunityName: undefined, quotationId: undefined, quoteNo: undefined })
  loadRelated(customerId).catch(() => {})
}
function onOpportunity(id) {
  const o = opps.value.find((x) => x.id === id)
  if (!o) return
  if (!form.value.name) form.value.name = o.name
  if (form.value.amount == null) form.value.amount = Number(o.amount) || undefined
  // 商机下的报价单优先
  const q = quotes.value.find((x) => x.opportunityId === id && ['APPROVED', 'ORDERED'].includes(x.status))
  if (q && !form.value.quotationId) onQuotation((form.value.quotationId = q.id))
}
function onQuotation(id) {
  const q = quotes.value.find((x) => x.id === id)
  if (q && form.value.amount == null) form.value.amount = Number(q.totalAmount)
}

async function remove(id) {
  await deleteCrmContract(id)
  message.success(t('common.msg.deleteSuccess'))
  load()
}

onMounted(async () => {
  load()
  const q = route.query
  const num = (v) => (v == null || v === '' ? undefined : Number(v))
  if (num(q.id)) {
    openDetail({ id: num(q.id) })
    router.replace({ query: {} })
  } else if (q.create) {
    // 从商机、客户「新建合同」跳转过来：带入客户和商机
    router.replace({ query: {} })
    const opportunityId = num(q.opportunityId)
    let customerId = num(q.customerId)
    const defaults = { customerId, opportunityId }
    if (opportunityId) {
      const opp = await getOpportunity(opportunityId).catch(() => null)
      if (opp) {
        customerId = customerId || opp.customerId
        Object.assign(defaults, {
          customerId,
          customerName: opp.customerName,
          opportunityName: `${opp.oppNo} ${opp.name}`,
          name: opp.name,
          amount: Number(opp.amount) || undefined
        })
      }
    } else if (customerId) {
      const c = await getCrmCustomer(customerId).catch(() => null)
      if (c) defaults.customerName = c.name
    }
    // 先加载客户的商机和报价单，商机下已批准的报价单默认关联
    await loadRelated(customerId)
    const quote = opportunityId && quotes.value.find((x) => x.opportunityId === opportunityId && ['APPROVED', 'ORDERED'].includes(x.status))
    if (quote) Object.assign(defaults, { quotationId: quote.id, amount: Number(quote.totalAmount) })
    drawer.openCreate(defaults)
  }
})
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.plans-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0;
  font-weight: 500;
}
.plans-head .ant-btn {
  margin-left: auto;
}
.cell {
  margin-bottom: 0;
}
</style>
