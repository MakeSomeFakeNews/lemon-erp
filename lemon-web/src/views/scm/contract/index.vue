<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('scm.contract.allStatus')" allow-clear :options="toOptions(dict.scm_contract_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear :placeholder="$t('scm.contract.supplier')" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('scm.contract.keyword')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'scm:contract:add'" type="primary" @click="drawer.openCreate()">
            <template #icon><PlusOutlined /></template>{{ $t('scm.contract.add') }}
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1220, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'contractNo'">
            <a @click="openDetail(record.id)">{{ record.contractNo }}</a>
            <div class="sub-text">{{ record.name }}</div>
          </template>
          <template v-else-if="column.key === 'type'"><DictTag type="scm_contract_type" :value="record.contractType" /></template>
          <template v-else-if="column.key === 'period'">
            <div>{{ record.startDate }} ~ {{ record.endDate }}</div>
            <div v-if="record.status === 'ACTIVE' && expired(record)" class="sub-text danger">{{ $t('scm.contract.expired') }}</div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <div>{{ $t('scm.contract.orderedAmount', { amount: formatMoney(record.orderedAmount) }) }}</div>
            <div class="sub-text">{{ record.amountLimit != null ? $t('scm.contract.limitAmount', { amount: formatMoney(record.amountLimit) }) : $t('scm.contract.amountUnlimited') }}</div>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="scm_contract_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openDetail(record.id)">{{ $t('common.action.view') }}</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <FormDrawer :drawer="drawer" :create-title="$t('scm.contract.createTitle')" :edit-title="$t('scm.contract.editTitle')" width="min(1100px, 100vw)" page>
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.contract.name')" name="name">
              <a-input v-model:value="form.name" :maxlength="128" :placeholder="$t('scm.contract.namePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.contract.supplier')" name="supplierId">
              <PartnerSelect v-model:value="form.supplierId" type="supplier" :label="form.supplierName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('scm.contract.contractType')" name="contractType" :extra="TYPE_TIPS[form.contractType]">
              <a-radio-group v-model:value="form.contractType" :options="toOptions(dict.scm_contract_type)" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('scm.contract.signDate')" name="signDate">
              <a-date-picker v-model:value="form.signDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('scm.contract.amountLimit')" name="amountLimit" :extra="$t('scm.contract.amountLimitTip')">
              <a-input-number v-model:value="form.amountLimit" :min="0.01" :precision="2" prefix="¥" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('scm.contract.startDate')" name="startDate">
              <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="6">
            <a-form-item :label="$t('scm.contract.endDate')" name="endDate">
              <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="4">
            <a-form-item :label="$t('scm.contract.paymentDays')" name="paymentDays" :extra="$t('scm.contract.paymentDaysTip')">
              <a-input-number v-model:value="form.paymentDays" :min="0" :precision="0" :addon-after="$t('scm.contract.day')" class="full" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :sm="8">
            <a-form-item :label="$t('scm.contract.paymentTerms')" name="paymentTerms">
              <a-input v-model:value="form.paymentTerms" :maxlength="255" :placeholder="$t('scm.contract.paymentTermsPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item :label="$t('common.field.remark')" name="remark">
              <a-input v-model:value="form.remark" :maxlength="500" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="items-head">
          <span class="section-title">{{ $t('scm.contract.itemsTitle') }}</span>
          <a-button type="primary" ghost size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>{{ $t('scm.contract.addMaterial') }}
          </a-button>
        </div>
        <a-table row-key="key" size="small" :columns="itemColumns" :data-source="form.items" :pagination="false" :scroll="{ x: itemScrollX }">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'material'">
              <a-form-item :name="['items', index, 'materialId']" :rules="[{ required: true, message: $t('scm.contract.materialRequired') }]" class="cell-item">
                <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'price'">
              <a-form-item :name="['items', index, 'price']" :rules="[{ required: true, message: $t('scm.contract.priceRequired') }]" class="cell-item">
                <a-input-number v-model:value="record.price" :min="0" :precision="4" prefix="¥" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'taxRate'">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" addon-after="%" class="full" />
            </template>
            <template v-else-if="column.key === 'qtyLimit'">
              <a-form-item
                :name="['items', index, 'qtyLimit']"
                :rules="form.contractType === 'FIXED' ? [{ required: true, message: $t('scm.contract.qtyRequired') }] : []"
                class="cell-item"
              >
                <a-input-number v-model:value="record.qtyLimit" :min="0" :placeholder="form.contractType === 'FIXED' ? '' : $t('scm.contract.unlimited')" class="full" />
              </a-form-item>
            </template>
            <template v-else-if="column.key === 'remark'">
              <a-input v-model:value="record.remark" :maxlength="255" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)"><DeleteOutlined /></a-button>
            </template>
          </template>
        </a-table>
      </a-form>
    </FormDrawer>

    <SubPage :open="detailOpen" :title="detail ? $t('scm.contract.detailTitle', { no: detail.contractNo }) : ''" width="min(1000px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button v-if="detail.flowInstanceId" @click="flowOpen = true">{{ $t('scm.contract.flowRecord') }}</a-button>
          <template v-if="detail.status === 'DRAFT'">
            <a-button v-perm="'scm:contract:edit'" @click="drawer.openEdit(detail)">{{ $t('common.action.edit') }}</a-button>
            <span v-perm="'scm:contract:remove'">
              <a-popconfirm :title="$t('scm.contract.confirmDelete')" @confirm="remove">
                <a-button danger>{{ $t('common.action.delete') }}</a-button>
              </a-popconfirm>
            </span>
            <span v-perm="'scm:contract:approve'">
              <a-popconfirm :title="$t('scm.contract.confirmSubmit')" @confirm="submit">
                <a-button type="primary">{{ $t('common.action.submit') }}</a-button>
              </a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === 'ACTIVE'" v-perm="'scm:contract:close'" @click="openClose">{{ $t('scm.contract.closeContract') }}</a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('scm.contract.name')" :span="2">{{ detail.name }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')">
            <DictTag type="scm_contract_status" :value="detail.status" />
            <span v-if="detail.status === 'ACTIVE' && expired(detail)" class="danger">{{ $t('scm.contract.expired') }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.contract.supplier')">{{ detail.supplierCode }} {{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.contract.type')"><DictTag type="scm_contract_type" :value="detail.contractType" /></a-descriptions-item>
          <a-descriptions-item :label="$t('scm.contract.signDate')">{{ detail.signDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('scm.contract.period')">{{ detail.startDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.amount')">
            {{ $t('scm.contract.orderedAmount', { amount: formatMoney(detail.orderedAmount) }) }}
            <span class="sub-text">{{ detail.amountLimit != null ? ' / ' + $t('scm.contract.limitAmount', { amount: formatMoney(detail.amountLimit) }) : $t('scm.contract.unlimitedParen') }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('scm.contract.payment')">{{ detail.paymentDays != null ? $t('scm.contract.days', { n: detail.paymentDays }) : $t('scm.contract.supplierTerms') }} {{ detail.paymentTerms || '' }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.tenderNo" :label="$t('scm.contract.sourceTender')">{{ detail.tenderNo }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.closeReason" :label="$t('scm.contract.closeReason')">{{ detail.closeReason }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-tabs>
          <a-tab-pane key="items" :tab="$t('scm.contract.itemsTab')">
            <a-table row-key="id" size="small" :columns="usageColumns" :data-source="detail.items" :pagination="false" :scroll="{ x: 860 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'material'">
                  {{ record.materialCode }} {{ record.materialName }}
                  <div class="sub-text">{{ record.spec }}</div>
                </template>
                <template v-else-if="column.key === 'price'">¥{{ Number(record.price) }}<div class="sub-text">{{ $t('scm.contract.taxRateOf', { rate: Number(record.taxRate) }) }}</div></template>
                <template v-else-if="column.key === 'progress'">
                  <template v-if="record.qtyLimit != null">
                    <a-progress :percent="pct(record.orderedQty, record.qtyLimit)" size="small" />
                    <div class="sub-text">{{ $t('scm.contract.orderedOfLimit', { qty: formatQty(record.orderedQty), limit: formatQty(record.qtyLimit), unit: record.unit || '' }) }}</div>
                  </template>
                  <span v-else>{{ $t('scm.contract.orderedQty', { qty: formatQty(record.orderedQty), unit: record.unit || '' }) }}<span class="sub-text">{{ $t('scm.contract.unlimitedParen') }}</span></span>
                </template>
                <template v-else-if="column.key === 'received'">{{ formatQty(record.receivedQty) }}</template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.orderedAmount) }}</template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="orders" :tab="$t('scm.contract.ordersTab', { n: orders.length })">
            <a-table row-key="id" size="small" :columns="orderColumns" :data-source="orders" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'"><DictTag type="scm_order_status" :value="record.status" /></template>
                <template v-else-if="column.key === 'amount'">¥{{ formatMoney(record.totalAmount) }}</template>
                <template v-else-if="column.key === 'qty'">{{ formatQty(record.doneQty) }} / {{ formatQty(record.totalQty) }}</template>
                <template v-else-if="column.key === 'confirm'">
                  <DictTag v-if="record.supplierConfirm" type="scm_supplier_confirm" :value="record.supplierConfirm" />
                  <span v-else class="sub-text">-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="files" :tab="$t('scm.contract.attachments')">
            <AttachmentPanel biz-type="scm_contract" :biz-id="detail.id" :editable="userStore.hasPerm('scm:contract:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </SubPage>

    <a-modal v-model:open="closeOpen" :title="$t('scm.contract.closeContract')" @ok="submitClose">
      <a-alert type="info" show-icon class="block" :message="$t('scm.contract.closeTip')" />
      <a-textarea v-model:value="closeReason" :rows="3" :maxlength="255" :placeholder="$t('scm.contract.closeReason')" />
    </a-modal>

    <InstanceDetailDrawer v-model:open="flowOpen" :instance-id="detail?.flowInstanceId" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import SubPage from '@/components/SubPage.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import InstanceDetailDrawer from '@/components/workflow/InstanceDetailDrawer.vue'
import { addContract, closeContract, contractOrders, deleteContract, getContract, pageContract, submitContract, updateContract } from '@/api/scm/srm'
import { formatMoney } from '@/components/scm/scm'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

/** 采购合同 / 框架协议：生效后采购订单可引用，按合同价下单并校验数量、金额上限 */
const { t } = useI18n()
const dict = useDict('scm_contract_type', 'scm_contract_status')
const userStore = useUserStore()
const TYPE_TIPS = {
  FRAMEWORK: t('scm.contract.frameworkTip'),
  FIXED: t('scm.contract.fixedTip')
}

const columns = [
  { title: t('scm.contract.noName'), key: 'contractNo', width: 240 },
  { title: t('scm.contract.supplier'), dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: t('scm.contract.type'), key: 'type', width: 130 },
  { title: t('scm.contract.period'), key: 'period', width: 200 },
  { title: t('scm.contract.execAmount'), key: 'amount', width: 190 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.action.operation'), key: 'action', width: 90, fixed: 'right' }
]
const { checked, visibleColumns } = useColumns(columns, 'scm-contract')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageContract, query)
onMounted(load)

const expired = (c) => dayjs(c.endDate).isBefore(dayjs(), 'day')
const pct = (a, b) => (Number(b) ? Math.min(100, Math.round((Number(a) / Number(b)) * 100)) : 0)

// ---------------------------------------------------------------- 表单
const newItem = () => ({ key: rowKey(), materialId: undefined, price: undefined, taxRate: 13, qtyLimit: undefined, remark: '' })
const rules = {
  name: [{ required: true, whitespace: true, message: t('scm.contract.nameRequired') }],
  supplierId: [{ required: true, message: t('scm.contract.supplierRequired') }],
  contractType: [{ required: true, message: t('scm.contract.typeRequired') }],
  signDate: [{ required: true, message: t('scm.contract.signDateRequired') }],
  startDate: [{ required: true, message: t('scm.contract.startDateRequired') }],
  endDate: [{ required: true, message: t('scm.contract.endDateRequired') }]
}
const num = (v) => (v == null ? undefined : Number(v))
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined, name: '', supplierId: undefined, contractType: 'FRAMEWORK', signDate: today(), startDate: today(),
    endDate: dayjs().add(1, 'year').subtract(1, 'day').format('YYYY-MM-DD'), amountLimit: undefined, paymentDays: undefined,
    paymentTerms: '', remark: '', items: [newItem()]
  }),
  fetch: async (record) => {
    const c = await getContract(record.id)
    return {
      ...c,
      supplierName: `${c.supplierCode} ${c.supplierName}`,
      amountLimit: num(c.amountLimit),
      items: c.items.map((i) => ({
        key: rowKey(), materialId: i.materialId, materialLabel: `${i.materialCode} ${i.materialName}`, price: num(i.price),
        taxRate: num(i.taxRate), qtyLimit: num(i.qtyLimit), remark: i.remark
      }))
    }
  },
  create: addContract,
  update: updateContract,
  toPayload: (f) => ({
    name: f.name, supplierId: f.supplierId, contractType: f.contractType, signDate: f.signDate, startDate: f.startDate, endDate: f.endDate,
    amountLimit: f.amountLimit ?? null, paymentDays: f.paymentDays ?? null, paymentTerms: f.paymentTerms || null, remark: f.remark || null,
    items: f.items.map((i) => ({ materialId: i.materialId, price: i.price, taxRate: i.taxRate ?? 13, qtyLimit: i.qtyLimit ?? null, remark: i.remark || null }))
  }),
  onSuccess: () => {
    load()
    if (detailOpen.value) refreshDetail()
  }
})
const { formRef, form } = drawer
const addItem = () => form.value.items.push(newItem())

const itemColumns = [
  { title: t('scm.contract.material'), key: 'material', width: 280 },
  { title: t('scm.contract.contractPrice'), key: 'price', width: 190 },
  { title: t('scm.contract.taxRate'), key: 'taxRate', width: 130 },
  { title: t('scm.contract.qtyLimit'), key: 'qtyLimit', width: 140 },
  { title: t('common.field.remark'), key: 'remark', width: 160 },
  { title: '', key: 'action', width: 50 }
]
const itemScrollX = itemColumns.reduce((sum, c) => sum + c.width, 0)

// ---------------------------------------------------------------- 详情
const usageColumns = [
  { title: t('scm.contract.material'), key: 'material', width: 240 },
  { title: t('scm.contract.contractPriceShort'), key: 'price', width: 130 },
  { title: t('scm.contract.progress'), key: 'progress', width: 260 },
  { title: t('scm.contract.received'), key: 'received', width: 110, align: 'right' },
  { title: t('scm.contract.orderedAmountCol'), key: 'amount', width: 140, align: 'right' }
]
const orderColumns = [
  { title: t('scm.contract.orderNo'), dataIndex: 'orderNo', width: 170 },
  { title: t('scm.contract.orderDate'), dataIndex: 'orderDate', width: 110 },
  { title: t('common.field.amount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.contract.receivedOfOrdered'), key: 'qty', width: 170 },
  { title: t('scm.contract.supplierConfirm'), key: 'confirm', width: 140 },
  { title: t('common.field.status'), key: 'status', width: 100 }
]
const detailOpen = ref(false)
const detail = ref()
const orders = ref([])
const flowOpen = ref(false)
async function openDetail(id) {
  const [c, o] = await Promise.all([getContract(id), contractOrders(id)])
  detail.value = c
  orders.value = o
  detailOpen.value = true
}
async function refreshDetail() {
  if (detail.value) await openDetail(detail.value.id)
}
async function submit() {
  const approving = await submitContract(detail.value.id)
  message.success(approving ? t('scm.contract.submitted') : t('scm.contract.activated'))
  refreshDetail()
  load()
}
async function remove() {
  await deleteContract(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
const closeOpen = ref(false)
const closeReason = ref('')
function openClose() {
  closeReason.value = ''
  closeOpen.value = true
}
async function submitClose() {
  if (!closeReason.value.trim()) return message.warning(t('scm.contract.closeReasonRequired'))
  await closeContract(detail.value.id, closeReason.value)
  message.success(t('scm.contract.closed'))
  closeOpen.value = false
  refreshDetail()
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--status-critical);
  margin-left: 4px;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.section-title {
  font-weight: 600;
}
.cell-item {
  margin-bottom: 0;
}
</style>
